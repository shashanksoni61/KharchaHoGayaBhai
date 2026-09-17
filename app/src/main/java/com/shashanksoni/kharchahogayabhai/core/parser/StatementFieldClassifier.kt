package com.shashanksoni.kharchahogayabhai.core.parser

import com.shashanksoni.kharchahogayabhai.core.common.StatementDateParser
import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionReferenceNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * What a single piece of statement text appears to be, based on its content —
 * not on the column header above it.
 *
 * Statements disagree on column names and order; they almost always still print
 * a date and an amount. Classification lets us rebuild a transaction from those
 * facts even when headers are wrong, missing, or labelled differently.
 */
enum class StatementFieldKind {
    DATE,
    TIME,
    MONEY,
    REFERENCE,
    TYPE_SIGNAL,
    ACCOUNT_MASK,
    NARRATIVE,
    NOISE,
}

data class ClassifiedField(
    val raw: String,
    val kind: StatementFieldKind,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val money: Money? = null,
    val typeHint: TransactionType? = null,
)

/** Classifies raw statement cells / tokens by inspecting the text itself. */
object StatementFieldClassifier {

    private val TYPE_DEBIT = Regex(
        """^(DEBIT|DR|D|WITHDRAWAL|WITHDRAWN|DEBITED)$""",
        RegexOption.IGNORE_CASE,
    )
    private val TYPE_CREDIT = Regex(
        """^(CREDIT|CR|C|DEPOSIT|DEPOSITED|CREDITED)$""",
        RegexOption.IGNORE_CASE,
    )
    private val ACCOUNT_MASK = Regex("""[X*]{2,}\d{3,}""", RegexOption.IGNORE_CASE)
    private val PHONEPE_TXN = Regex("""^T\d{15,}$""")
    private val ALNUM_REF = Regex("""^[A-Z]{2,}\d{6,}$""", RegexOption.IGNORE_CASE)
    private val HEADERISH = Regex(
        """^(date|time|amount|debit|credit|balance|narration|description|particulars|""" +
            """transaction details|transaction id|transaction type|utr|reference|""" +
            """credit/debit instrument|type|ref|ref no|withdrawal|deposit)$""",
        RegexOption.IGNORE_CASE,
    )

    fun classify(raw: String): ClassifiedField {
        val value = raw.trim().trim('"')
        if (value.isEmpty()) {
            return ClassifiedField(raw = value, kind = StatementFieldKind.NOISE)
        }

        // Type tokens like DEBIT/CREDIT are also common header words — treat data
        // values first so a PhonePe "DEBIT" cell is not discarded as noise.
        when {
            TYPE_DEBIT.matches(value) ->
                return ClassifiedField(value, StatementFieldKind.TYPE_SIGNAL, typeHint = TransactionType.DEBIT)

            TYPE_CREDIT.matches(value) ->
                return ClassifiedField(value, StatementFieldKind.TYPE_SIGNAL, typeHint = TransactionType.CREDIT)
        }

        if (HEADERISH.matches(value)) {
            return ClassifiedField(raw = value, kind = StatementFieldKind.NOISE)
        }

        StatementDateParser.parseDate(value)?.let { date ->
            return ClassifiedField(raw = value, kind = StatementFieldKind.DATE, date = date)
        }

        if (looksLikeClock(value)) {
            StatementDateParser.parseTime(value)?.let { time ->
                return ClassifiedField(raw = value, kind = StatementFieldKind.TIME, time = time)
            }
        }

        if (ACCOUNT_MASK.containsMatchIn(value)) {
            return ClassifiedField(raw = value, kind = StatementFieldKind.ACCOUNT_MASK)
        }

        if (isReference(value)) {
            return ClassifiedField(raw = value, kind = StatementFieldKind.REFERENCE)
        }

        val money = Money.parseMajorUnitsOrNull(value)?.absoluteValue
        if (money != null && !isLongDigitString(value)) {
            return ClassifiedField(raw = value, kind = StatementFieldKind.MONEY, money = money)
        }

        if (value.any { it.isLetter() } && value.length >= 3) {
            return ClassifiedField(raw = value, kind = StatementFieldKind.NARRATIVE)
        }

        return ClassifiedField(raw = value, kind = StatementFieldKind.NOISE)
    }

    fun classifyAll(values: Iterable<String>): List<ClassifiedField> =
        values.map { classify(it) }

    private fun isReference(value: String): Boolean {
        if (PHONEPE_TXN.matches(value) || ALNUM_REF.matches(value)) return true
        if (isLongDigitString(value)) return true
        val normalized = TransactionReferenceNormalizer.normalize(value)
        return normalized != null && value.any { it.isLetter() }
    }

    private fun isLongDigitString(value: String): Boolean {
        val compact = value.replace(",", "").replace(" ", "")
        return compact.all { it.isDigit() } && compact.length >= 8
    }

    private fun looksLikeClock(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("am") || lower.contains("pm") || value.contains(':')
    }
}

/**
 * Builds one transaction candidate from classified fields.
 *
 * Minimum viable row: a **date** and an **amount**. Headers are never required.
 */
object StatementRowAssembler {

    data class AssembledRow(
        val amount: Money,
        val type: TransactionType,
        val transactionDate: Instant,
        val description: String?,
        val merchantName: String?,
        val referenceNumber: String?,
        val accountIdentifier: String?,
        val paymentMethodIsUpi: Boolean,
        val phonePeTransactionId: String?,
        val confidence: Confidence,
    )

    enum class Confidence { HIGH, MEDIUM, LOW }

    fun assemble(fields: List<ClassifiedField>, zone: ZoneId): AssembledRow? {
        val date = fields.firstOrNull { it.kind == StatementFieldKind.DATE }?.date ?: return null
        val time = fields.firstOrNull { it.kind == StatementFieldKind.TIME }?.time
        val monies = fields.mapNotNull { field ->
            field.money?.takeIf { field.kind == StatementFieldKind.MONEY }
        }
        if (monies.isEmpty()) return null

        val type = fields.firstOrNull { it.typeHint != null }?.typeHint
            ?: inferTypeFromNarrative(fields)
            ?: TransactionType.DEBIT

        val amount = pickTransactionAmount(monies)
        val references = fields.filter { it.kind == StatementFieldKind.REFERENCE }.map { it.raw }
        val phonePeId = references.firstOrNull { it.startsWith("T") && it.length >= 16 }
        val utrLike = references.firstOrNull { ref ->
            val digits = ref.filter { it.isDigit() }
            !ref.startsWith("T") && (digits.length in 10..16 || TransactionReferenceNormalizer.normalize(ref) != null)
        }?.let { TransactionReferenceNormalizer.normalize(it) ?: it }

        val narratives = fields.filter { it.kind == StatementFieldKind.NARRATIVE }.map { it.raw }
        val description = narratives.maxByOrNull { it.length }
        val merchant = description?.let { extractMerchant(it) }
        val account = fields.firstOrNull { it.kind == StatementFieldKind.ACCOUNT_MASK }?.raw
            ?.filter { it.isDigit() }
            ?.takeLast(6)
            ?.takeIf { it.length >= 4 }

        val instant = if (time != null) {
            date.atTime(time).atZone(zone).toInstant()
        } else {
            date.atStartOfDay(zone).toInstant()
        }

        val confidence = when {
            utrLike != null && merchant != null -> Confidence.HIGH
            utrLike != null || merchant != null -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        return AssembledRow(
            amount = amount,
            type = type,
            transactionDate = instant,
            description = description,
            merchantName = merchant,
            referenceNumber = utrLike ?: phonePeId,
            accountIdentifier = account,
            paymentMethodIsUpi = utrLike != null || phonePeId != null ||
                description?.contains("UPI", ignoreCase = true) == true,
            phonePeTransactionId = phonePeId,
            confidence = confidence,
        )
    }

    /** If debit/credit/balance all look like money, drop the likely balance (largest). */
    private fun pickTransactionAmount(monies: List<Money>): Money {
        val positive = monies.filter { !it.isZero }.distinctBy { it.minorUnits }
        return when {
            positive.isEmpty() -> monies.first()
            positive.size == 1 -> positive.first()
            else -> positive.minBy { it.minorUnits }
        }
    }

    private fun inferTypeFromNarrative(fields: List<ClassifiedField>): TransactionType? {
        val text = fields.filter { it.kind == StatementFieldKind.NARRATIVE }
            .joinToString(" ") { it.raw }
            .lowercase()
        return when {
            text.startsWith("paid to") || text.contains("debited") ||
                text.contains("withdraw") || text.contains("spent") -> TransactionType.DEBIT

            text.startsWith("received from") || text.startsWith("refund from") ||
                text.contains("credited") -> TransactionType.CREDIT

            else -> null
        }
    }

    private fun extractMerchant(description: String): String? {
        Regex("""(?i)^(?:Paid to|Received from|Refund from)\s+(.+)$""")
            .find(description.trim())
            ?.let { return it.groupValues[1].trim() }

        Regex("""(?i)UPI[/\-]([^/\-\s]+)""")
            .find(description)
            ?.let { return it.groupValues[1].trim() }

        return description.trim().takeIf { it.isNotEmpty() }
    }
}
