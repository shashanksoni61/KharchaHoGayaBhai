package com.shashanksoni.kharchahogayabhai.sms

import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionReferenceNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.parser.TransactionParser
import java.util.Locale

/**
 * Extracts bank / UPI / card alerts from Indian SMS bodies.
 *
 * Dedup against CSV/PDF relies on the same pipeline as file import:
 * - [ParsedTransaction.sourceIdentifier] = `sms:{inboxId}` so the same SMS is
 *   not ingested twice
 * - [ParsedTransaction.referenceNumber] = UPI/UTR/RRN when present, so a later
 *   CSV/PDF row with the same id merges instead of double-counting
 */
class SmsTransactionParser : TransactionParser<SmsParseInput> {

    override val source: TransactionSource = TransactionSource.SMS
    override val name: String = "Indian bank / UPI SMS"

    override fun canParse(input: SmsParseInput): Boolean =
        input.messages.any { looksLikeTransactionAlert(it.body) }

    override fun parse(input: SmsParseInput): List<ParsedTransaction> =
        input.messages.mapNotNull { message -> parseOne(message) }

    private fun parseOne(message: SmsMessage): ParsedTransaction? {
        val body = message.body
        if (!looksLikeTransactionAlert(body)) return null

        val amount = extractAmount(body)
        val type = detectType(body)
        val reference = extractReference(body)
        val account = ACCOUNT_MASK.find(body)?.groupValues?.getOrNull(1)
        val paymentMethod = detectPaymentMethod(body)
        val merchant = extractMerchant(body)

        if (amount == null || type == null) {
            return ParsedTransaction(
                amount = amount ?: Money.zero(),
                type = type ?: TransactionType.DEBIT,
                transactionDate = message.receivedAt,
                description = body.take(240),
                merchantName = merchant,
                referenceNumber = reference,
                accountIdentifier = account,
                paymentMethod = paymentMethod,
                source = TransactionSource.SMS,
                sourceIdentifier = sourceId(message.id),
                originLabel = message.address,
                parseStatus = ParseStatus.FAILED,
                rawPayload = body,
            )
        }

        val status = when {
            reference.isNullOrBlank() && merchant.isNullOrBlank() -> ParseStatus.PARTIAL
            else -> ParseStatus.PARSED
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            transactionDate = message.receivedAt,
            description = body.take(240),
            merchantName = merchant,
            referenceNumber = reference,
            accountIdentifier = account,
            paymentMethod = paymentMethod,
            source = TransactionSource.SMS,
            sourceIdentifier = sourceId(message.id),
            originLabel = message.address,
            parseStatus = status,
            rawPayload = body,
        )
    }

    private fun looksLikeTransactionAlert(body: String): Boolean {
        if (!AMOUNT_HINT.containsMatchIn(body)) return false
        return DEBIT_HINT.containsMatchIn(body) ||
            CREDIT_HINT.containsMatchIn(body) ||
            SPENT_HINT.containsMatchIn(body) ||
            UPI_HINT.containsMatchIn(body)
    }

    private fun extractAmount(body: String): Money? {
        AMOUNT_CAPTURE.findAll(body).forEach { match ->
            val raw = match.groupValues[1]
            Money.parseMajorUnitsOrNull(raw)?.absoluteValue?.let { return it }
        }
        return null
    }

    private fun detectType(body: String): TransactionType? = when {
        CREDIT_HINT.containsMatchIn(body) && !DEBIT_HINT.containsMatchIn(body) ->
            TransactionType.CREDIT

        DEBIT_HINT.containsMatchIn(body) || SPENT_HINT.containsMatchIn(body) ->
            TransactionType.DEBIT

        CREDIT_HINT.containsMatchIn(body) -> TransactionType.CREDIT
        else -> null
    }

    private fun extractReference(body: String): String? {
        REFERENCE_CAPTURE.findAll(body).forEach { match ->
            val candidate = match.groupValues[1]
            TransactionReferenceNormalizer.normalize(candidate)?.let { return it }
            TransactionReferenceNormalizer.normalize("UPI Ref $candidate")?.let { return it }
        }
        // PhonePe / Paytm style bare txn ids
        PHONEPE_TXN.find(body)?.value?.let { return it }
        return null
    }

    private fun detectPaymentMethod(body: String): PaymentMethod {
        val upper = body.uppercase(Locale.ROOT)
        return when {
            "UPI" in upper -> PaymentMethod.UPI
            "IMPS" in upper -> PaymentMethod.IMPS
            "NEFT" in upper -> PaymentMethod.NEFT
            "RTGS" in upper -> PaymentMethod.RTGS
            "ATM" in upper || "CASH WDL" in upper -> PaymentMethod.ATM
            "CARD" in upper || "CREDIT CARD" in upper || "DEBIT CARD" in upper ->
                PaymentMethod.CARD

            else -> PaymentMethod.UNKNOWN
        }
    }

    private fun extractMerchant(body: String): String? {
        MERCHANT_PATTERNS.forEach { pattern ->
            pattern.find(body)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.length in 2..64 }
                ?.let { return cleanMerchant(it) }
        }
        return null
    }

    private fun cleanMerchant(raw: String): String =
        raw.trim(' ', '.', ',', ';', '-', '/', '\\')
            .replace(Regex("""\s+"""), " ")
            .take(64)

    private fun sourceId(smsId: Long): String = "sms:$smsId"

    companion object {
        private val AMOUNT_HINT = Regex(
            """(?:Rs\.?|INR|₹)\s*[\d,]+\.?\d*""",
            RegexOption.IGNORE_CASE,
        )
        private val AMOUNT_CAPTURE = Regex(
            """(?:(?:Rs\.?|INR|₹)\s*|(?:debited|credited|spent|paid|received)\s+(?:for\s+)?(?:Rs\.?|INR|₹)?\s*)([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE,
        )
        private val DEBIT_HINT = Regex(
            """\b(debited|debit|withdrawn|withdrawal|paid\s+to|sent\s+to|purchase)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val CREDIT_HINT = Regex(
            """\b(credited|credit|deposited|received|refund)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val SPENT_HINT = Regex("""\b(spent|purchase)\b""", RegexOption.IGNORE_CASE)
        private val UPI_HINT = Regex("""\bUPI\b""", RegexOption.IGNORE_CASE)
        private val ACCOUNT_MASK = Regex("""(?:a/?c|acct|account)[^\dX*]*([X*]{2,}\d{2,}|\d{4})""", RegexOption.IGNORE_CASE)
        private val REFERENCE_CAPTURE = Regex(
            """(?:UPI\s*(?:Ref(?:erence)?(?:\s*No\.?)?|Txn(?:n)?(?:\s*ID)?|ID)?|UTR|RRN|Ref(?:erence)?(?:\s*No\.?)?|Txn(?:n)?(?:\s*ID)?)\s*[:\-]?\s*([A-Z0-9]{6,})""",
            RegexOption.IGNORE_CASE,
        )
        private val PHONEPE_TXN = Regex("""\bT\d{15,}\b""")
        private val MERCHANT_PATTERNS = listOf(
            Regex("""(?:to|at|towards)\s+VPA\s+([^\s,]+@[^\s,]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|at|towards)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{1,40}?)(?:\s+on\s|\s+UPI|\s+Ref|\s+Info|\.|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:from)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{1,40}?)(?:\s+on\s|\s+UPI|\s+Ref|\.|$)""", RegexOption.IGNORE_CASE),
        )
    }
}
