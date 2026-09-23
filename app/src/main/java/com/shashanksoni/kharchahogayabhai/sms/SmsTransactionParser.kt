package com.shashanksoni.kharchahogayabhai.sms

import com.shashanksoni.kharchahogayabhai.core.common.StatementDateParser
import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionReferenceNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.parser.TransactionParser
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Extracts bank / UPI / card alerts from Indian SMS bodies.
 *
 * Dedup against CSV/PDF relies on the same pipeline as file import:
 * - [ParsedTransaction.sourceIdentifier] = `sms:{inboxId}` so the same SMS is
 *   not ingested twice
 * - [ParsedTransaction.referenceNumber] = UPI/UTR/RRN when present, so a later
 *   CSV/PDF row with the same id merges instead of double-counting
 *
 * Transaction date prefers a timestamp printed in the body (e.g. Axis
 * `22-09-26, 20:41:05`) over the SMS provider receive time.
 */
class SmsTransactionParser(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : TransactionParser<SmsParseInput> {

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
        val promotional = isPromotionalAlert(body, reference, account)
        val merchant = if (promotional) {
            extractMerchant(body) ?: message.address?.let(::cleanMerchant)
        } else {
            extractMerchant(body)
        }
        val transactionDate = extractBodyDateTime(body) ?: message.receivedAt

        if (amount == null || type == null) {
            return ParsedTransaction(
                amount = amount ?: Money.zero(),
                type = type ?: TransactionType.DEBIT,
                transactionDate = transactionDate,
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
                isPromotional = promotional,
            )
        }

        val status = when {
            reference.isNullOrBlank() && merchant.isNullOrBlank() -> ParseStatus.PARTIALLY_PARSED
            else -> ParseStatus.PARSED
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            transactionDate = transactionDate,
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
            isPromotional = promotional,
        )
    }

    /**
     * Offers, app-wallet credits with an expiry, and similar marketing SMS
     * often say "Rs X credited" without a bank account or UTR. Keep them
     * visible; do not treat them as money that moved.
     */
    private fun isPromotionalAlert(
        body: String,
        reference: String?,
        account: String?,
    ): Boolean {
        if (hasBankMoneyMovementEvidence(body, reference, account)) return false
        return PROMOTIONAL_HINT.containsMatchIn(body)
    }

    private fun hasBankMoneyMovementEvidence(
        body: String,
        reference: String?,
        account: String?,
    ): Boolean {
        if (!account.isNullOrBlank()) return true
        if (!reference.isNullOrBlank()) return true
        return BANK_MOVEMENT_HINT.containsMatchIn(body)
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
        UPI_PATH.find(body)?.groupValues?.getOrNull(1)?.let { ref ->
            TransactionReferenceNormalizer.normalize(ref)?.let { return it }
        }
        REFERENCE_CAPTURE.findAll(body).forEach { match ->
            val candidate = match.groupValues[1]
            TransactionReferenceNormalizer.normalize(candidate)?.let { return it }
            TransactionReferenceNormalizer.normalize("UPI Ref $candidate")?.let { return it }
        }
        UPI_PATH.find(body)?.value?.let { path ->
            TransactionReferenceNormalizer.normalize(path)?.let { return it }
        }
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
        UPI_PATH.find(body)?.groupValues?.getOrNull(2)?.trim()
            ?.takeIf { it.length in 2..64 }
            ?.let { return cleanMerchant(it) }
        MERCHANT_PATTERNS.forEach { pattern ->
            pattern.find(body)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.length in 2..64 }
                ?.let { return cleanMerchant(it) }
        }
        return null
    }

    private fun extractBodyDateTime(body: String): Instant? {
        BODY_DATE_TIME.find(body)?.value?.let { raw ->
            StatementDateParser.parseToInstant(raw, zone)?.let { return it }
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
        private val ACCOUNT_MASK = Regex(
            """(?:a/?c|acct|account)(?:\s*no\.?)?[^\dX*]*([X*]{2,}\d{2,}|\d{4})""",
            RegexOption.IGNORE_CASE,
        )
        /** Axis-style: `UPI/P2M/347484353597/SHREE MEDICAL STORE` */
        private val UPI_PATH = Regex(
            """\bUPI/[A-Za-z0-9]+/(\d{6,})/([^\n\r]+)""",
            RegexOption.IGNORE_CASE,
        )
        private val REFERENCE_CAPTURE = Regex(
            """(?:UPI\s*(?:Ref(?:erence)?(?:\s*No\.?)?|Txn(?:n)?(?:\s*ID)?|ID)?|UTR|RRN|Ref(?:erence)?(?:\s*No\.?)?|Txn(?:n)?(?:\s*ID)?)\s*[:\-]?\s*([A-Z0-9]{6,})""",
            RegexOption.IGNORE_CASE,
        )
        private val PHONEPE_TXN = Regex("""\bT\d{15,}\b""")
        /** Prefer body stamp over SMS receive time when banks print one. */
        private val BODY_DATE_TIME = Regex(
            """\b(\d{1,2}[-/]\d{1,2}[-/]\d{2,4}),?\s+(\d{1,2}:\d{2}(?::\d{2})?(?:\s*[AaPp][Mm])?)\b""",
        )
        private val BANK_MOVEMENT_HINT = Regex(
            """(?:debited\s+from|credited\s+to\s+your|withdrawn\s+from|avl(?:ailable)?\s+bal|not you\?|a/?c\s*(?:no\.?)?)""",
            RegexOption.IGNORE_CASE,
        )
        private val PROMOTIONAL_HINT = Regex(
            """(?:credited\s+(?:in|to)\s+your\s+wallet|wallet.{0,40}(?:till|until|valid|expir)|""" +
                """(?:till|until|valid(?:\s+(?:till|until|upto|up to))?|expir(?:es|y|ing)?)\b.{0,40}wallet|""" +
                """eager to serve|\b(?:voucher|coupon|promo(?:tion|tional)?|offer code|cashback)\b|""" +
                """\bwill be credited\b|\b(?:win|unlock|grab|flat)\s+(?:rs\.?|inr|₹)|""" +
                """up\s*to\s+(?:rs\.?|inr|₹))""",
            RegexOption.IGNORE_CASE,
        )
        private val MERCHANT_PATTERNS = listOf(
            Regex("""(?:to|at|towards)\s+VPA\s+([^\s,]+@[^\s,]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|at|towards)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{1,40}?)(?:\s+on\s|\s+UPI|\s+Ref|\s+Info|\.|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:from)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{1,40}?)(?:\s+on\s|\s+UPI|\s+Ref|\.|$)""", RegexOption.IGNORE_CASE),
        )
    }
}
