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
        input.messages.any { looksLikeCandidate(it.body) }

    override fun parse(input: SmsParseInput): List<ParsedTransaction> =
        input.messages.mapNotNull { message -> parseOne(message) }

    private fun parseOne(message: SmsMessage): ParsedTransaction? {
        val body = message.body
        if (!looksLikeCandidate(body)) return null

        val reference = extractReference(body)
        val account = ACCOUNT_MASK.find(body)?.groupValues?.getOrNull(1)
        val spendAmount = extractSpendAmount(body)
        val informationalOnly = isInformationalOnly(body, spendAmount)
        val promotional = informationalOnly || isPromotionalAlert(body, reference, account)
        val amount = spendAmount ?: if (promotional) extractAnyAmount(body) else null
        val type = detectType(body) ?: if (promotional) TransactionType.DEBIT else null
        val paymentMethod = detectPaymentMethod(body)
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
            promotional -> ParseStatus.PARSED
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
     * Offers, wallet credits with an expiry, remaining card limits, dues and
     * similar alerts mention rupees but no money moved. They stay out of totals
     * and are hidden from the list by default.
     */
    private fun isPromotionalAlert(
        body: String,
        reference: String?,
        account: String?,
    ): Boolean {
        if (hasBankMoneyMovementEvidence(body, reference, account)) return false
        return PROMOTIONAL_HINT.containsMatchIn(body)
    }

    private fun isInformationalOnly(body: String, spendAmount: Money?): Boolean {
        if (!INFORMATIONAL_HINT.containsMatchIn(body)) return false
        return spendAmount == null || !hasMoneyMovementVerb(body)
    }

    private fun hasBankMoneyMovementEvidence(
        body: String,
        reference: String?,
        account: String?,
    ): Boolean {
        if (!hasMoneyMovementVerb(body)) return false
        if (!reference.isNullOrBlank()) return true
        if (!account.isNullOrBlank()) return true
        return BANK_MOVEMENT_HINT.containsMatchIn(body)
    }

    private fun hasMoneyMovementVerb(body: String): Boolean =
        DEBIT_HINT.containsMatchIn(body) ||
            CREDIT_HINT.containsMatchIn(body) ||
            SPENT_HINT.containsMatchIn(body)

    private fun looksLikeCandidate(body: String): Boolean {
        if (!AMOUNT_HINT.containsMatchIn(body)) return false
        return hasMoneyMovementVerb(body) ||
            INFORMATIONAL_HINT.containsMatchIn(body) ||
            PROMOTIONAL_HINT.containsMatchIn(body) ||
            UPI_HINT.containsMatchIn(body)
    }

    /** The amount that moved, never a remaining limit / due / balance figure. */
    private fun extractSpendAmount(body: String): Money? {
        val hits = findAmountHits(body).filterNot { isNonTransactionAmount(body, it.range) }
        if (hits.isEmpty()) return null
        val nearMovement = hits.filter { isNearMovementVerb(body, it.range) }
        return (nearMovement.firstOrNull() ?: hits.first()).money
    }

    private fun extractAnyAmount(body: String): Money? =
        findAmountHits(body).firstOrNull()?.money

    private fun findAmountHits(body: String): List<AmountHit> {
        val hits = mutableListOf<AmountHit>()
        fun addHits(pattern: Regex, currency: String) {
            pattern.findAll(body).forEach { match ->
                val raw = match.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return@forEach
                Money.parseMajorUnitsOrNull(raw, currency)?.absoluteValue?.let { money ->
                    hits += AmountHit(money, match.range)
                }
            }
        }
        addHits(INR_PREFIX_AMOUNT, "INR")
        addHits(INR_SUFFIX_AMOUNT, "INR")
        addHits(RUPEE_AMOUNT, "INR")
        addHits(USD_AMOUNT, "USD")
        addHits(DOLLAR_AMOUNT, "USD")
        addHits(MOVEMENT_BARE_AMOUNT, "INR")
        return hits.sortedBy { it.range.first }
    }

    private fun isNonTransactionAmount(body: String, range: IntRange): Boolean {
        val prefix = body.substring((range.first - 48).coerceAtLeast(0), range.first)
        val suffix = body.substring(
            range.last + 1,
            (range.last + 1 + 36).coerceAtMost(body.length),
        )
        return LIMIT_AMOUNT_PREFIX.containsMatchIn(prefix) ||
            LIMIT_AMOUNT_SUFFIX.containsMatchIn(suffix)
    }

    private fun isNearMovementVerb(body: String, range: IntRange): Boolean {
        val start = (range.first - 40).coerceAtLeast(0)
        val end = (range.last + 1 + 24).coerceAtMost(body.length)
        return MOVEMENT_NEAR_AMOUNT.containsMatchIn(body.substring(start, end))
    }

    private fun detectType(body: String): TransactionType? = when {
        CREDIT_HINT.containsMatchIn(body) &&
            !DEBIT_HINT.containsMatchIn(body) &&
            !SPENT_HINT.containsMatchIn(body) ->
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

    private data class AmountHit(val money: Money, val range: IntRange)

    companion object {
        private val AMOUNT_HINT = Regex(
            """(?:Rs\.?|INR|₹|USD)\s*[\d,]+\.?\d*|[\d,]+\.?\d*\s*(?:USD|dollars?|INR|Rs\.?)|""" +
                "\\\$\\s*[\\d,]+\\.?\\d*",
            RegexOption.IGNORE_CASE,
        )
        private val INR_PREFIX_AMOUNT = Regex(
            """(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE,
        )
        private val INR_SUFFIX_AMOUNT = Regex(
            """([\d,]+(?:\.\d{1,2})?)\s*(?:INR|Rs\.?)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val RUPEE_AMOUNT = Regex("₹\\s*([\\d,]+(?:\\.\\d{1,2})?)")
        private val USD_AMOUNT = Regex(
            """USD\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:USD|dollars?)""",
            RegexOption.IGNORE_CASE,
        )
        private val DOLLAR_AMOUNT = Regex("\\\$\\s*([\\d,]+(?:\\.\\d{1,2})?)")
        private val MOVEMENT_BARE_AMOUNT = Regex(
            """(?:debited|credited|spent|paid|received|payment\s+of)\s+(?:for\s+)?([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE,
        )
        private val DEBIT_HINT = Regex(
            """\b(debited|withdrawn|withdrawal|paid\s+to|sent\s+to|purchase)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val CREDIT_HINT = Regex(
            """\b(credited|deposited|refund(?:ed)?|received\s+from|received\s+in\s+your)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val SPENT_HINT = Regex(
            """\b(spent|purchase|using\s+your|used\s+(?:for|at)|txn\s+of|payment\s+of)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val MOVEMENT_NEAR_AMOUNT = Regex(
            """\b(debited|credited|spent|paid|purchase|using\s+your|used\s+(?:for|at)|""" +
                """payment\s+of|withdrawn|deposited|refund(?:ed)?)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val UPI_HINT = Regex("""\bUPI\b""", RegexOption.IGNORE_CASE)
        private val INFORMATIONAL_HINT = Regex(
            """(?:avail(?:able)?|avl|avbl|remaining|unused)\s+(?:credit\s+)?(?:limit|lim|bal(?:ance)?)|""" +
                """(?:credit\s+)?limit\s+(?:is|of|available|remaining|has\s+been|increased|decreased|revised|enhanced|:)|""" +
                """(?:avail(?:able)?|avl|avbl)\s+bal(?:ance)?|""" +
                """outstanding|(?:total|min(?:imum)?|amt)\s+(?:amt\s+)?due|payment\s+due|due\s+(?:date|amt|amount)|""" +
                """statement\s+(?:is\s+)?generated|reward\s+points?|""" +
                """card\s+(?:is\s+)?(?:blocked|hotlisted|expired)|""" +
                """overdue""",
            RegexOption.IGNORE_CASE,
        )
        private val LIMIT_AMOUNT_PREFIX = Regex(
            """(?:avail(?:able)?|avl|avbl|remaining|unused).{0,24}(?:limit|lim|bal(?:ance)?).{0,28}$|""" +
                """(?:credit\s+)?limit\s+(?:is|of|:)?\s*$|""" +
                """outstanding(?:\s+(?:amt|amount|bal(?:ance)?))?\s*[:\-]?\s*$|""" +
                """(?:total|min(?:imum)?|amt)\s+(?:amt\s+)?due\s*[:\-]?\s*$|""" +
                """payment\s+due\s*[:\-]?\s*$|""" +
                """reward\s+points?\s*[:\-]?\s*$""",
            RegexOption.IGNORE_CASE,
        )
        private val LIMIT_AMOUNT_SUFFIX = Regex(
            """^\s*(?:is|as)?\s*(?:your\s+)?(?:avail(?:able)?|remaining|unused).{0,24}(?:limit|bal)|""" +
                """^\s*(?:INR|Rs\.?|₹)?\s*(?:is\s+)?(?:your\s+)?(?:remaining|available)\s+(?:credit\s+)?limit""",
            RegexOption.IGNORE_CASE,
        )
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
            """(?:debited\s+from|credited\s+to\s+your|withdrawn\s+from|not you\?)""",
            RegexOption.IGNORE_CASE,
        )
        private val PROMOTIONAL_HINT = Regex(
            """(?:credited\s+(?:in|to)\s+your\s+wallet|wallet.{0,40}(?:till|until|valid|expir)|""" +
                """(?:till|until|valid(?:\s+(?:till|until|upto|up to))?|expir(?:es|y|ing)?)\b.{0,40}wallet|""" +
                """eager to serve|\b(?:voucher|coupon|promo(?:tion|tional)?|offer code|cashback)\b|""" +
                """\bwill be credited\b|\b(?:win|unlock|grab|flat)\s+(?:rs\.?|inr|₹)|""" +
                """up\s*to\s+(?:rs\.?|inr|₹)|pre-approved|limited\s+period\s+offer|""" +
                """exclusive\s+offer|don'?t\s+miss|hurry\s+up)""",
            RegexOption.IGNORE_CASE,
        )
        private val MERCHANT_PATTERNS = listOf(
            Regex("""(?:to|at|towards)\s+VPA\s+([^\s,]+@[^\s,]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|at|towards)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{1,40}?)(?:\s+on\s|\s+UPI|\s+Ref|\s+Info|\.|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:from)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{1,40}?)(?:\s+on\s|\s+UPI|\s+Ref|\.|$)""", RegexOption.IGNORE_CASE),
        )
    }
}
