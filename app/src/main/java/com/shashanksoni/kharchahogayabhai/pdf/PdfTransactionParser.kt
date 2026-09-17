package com.shashanksoni.kharchahogayabhai.pdf

import com.shashanksoni.kharchahogayabhai.core.common.Sha256
import com.shashanksoni.kharchahogayabhai.core.common.StatementDateParser
import com.shashanksoni.kharchahogayabhai.core.parser.ClassifiedField
import com.shashanksoni.kharchahogayabhai.core.parser.StatementFieldClassifier
import com.shashanksoni.kharchahogayabhai.core.parser.StatementFieldKind
import com.shashanksoni.kharchahogayabhai.core.parser.StatementRowAssembler
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.parser.TransactionParser
import java.time.ZoneId

/**
 * Content-driven PDF statement parser.
 *
 * After text extraction, lines are tokenised and classified the same way as CSV
 * cells. Layout and column labels do not matter; a dated line with an amount is
 * enough to produce a transaction for the dashboard.
 *
 * Also understands Google Pay statement layout (`02 Aug, 2026` + `Paid to` +
 * `UPI Transaction ID` + `₹amount`), including PdfBox text that glues words.
 */
class PdfTransactionParser(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : TransactionParser<PdfParseInput> {

    override val source: TransactionSource = TransactionSource.PDF
    override val name: String = "Content-driven PDF"

    override fun canParse(input: PdfParseInput): Boolean {
        val text = preprocess(input.statementText)
        if (looksLikeGooglePay(text) &&
            (GPAY_STACKED.containsMatchIn(text) || GPAY_LAYOUT.containsMatchIn(text))
        ) {
            return true
        }
        val tokens = tokenize(text)
        val fields = StatementFieldClassifier.classifyAll(tokens)
        return fields.any { it.kind == StatementFieldKind.DATE } &&
            fields.any { it.kind == StatementFieldKind.MONEY }
    }

    override fun parse(input: PdfParseInput): List<ParsedTransaction> {
        val text = preprocess(input.statementText)

        val gpayRows = parseGooglePay(text, input.fileName)
        if (gpayRows.isNotEmpty()) return gpayRows

        return parseDatedBlocks(text, input.fileName)
    }

    private fun parseGooglePay(text: String, fileName: String): List<ParsedTransaction> {
        if (!looksLikeGooglePay(text)) return emptyList()

        val fromStacked = GPAY_STACKED.findAll(text).mapIndexed { index, match ->
            gpayMatchToParsed(
                dateRaw = match.groupValues[1],
                timeRaw = match.groupValues[2],
                detail = match.groupValues[3],
                upiId = match.groupValues[4],
                amountRaw = match.groupValues[5],
                fileName = fileName,
                index = index,
            )
        }.toList()
        if (fromStacked.isNotEmpty()) return fromStacked.filterNotNull()

        return GPAY_LAYOUT.findAll(text).mapIndexed { index, match ->
            gpayMatchToParsed(
                dateRaw = match.groupValues[1],
                timeRaw = match.groupValues[4],
                detail = match.groupValues[2],
                upiId = match.groupValues[5],
                amountRaw = match.groupValues[3],
                fileName = fileName,
                index = index,
            )
        }.toList().filterNotNull()
    }

    private fun gpayMatchToParsed(
        dateRaw: String,
        timeRaw: String,
        detail: String,
        upiId: String,
        amountRaw: String,
        fileName: String,
        index: Int,
    ): ParsedTransaction? {
        val amount = Money.parseMajorUnitsOrNull(amountRaw)?.absoluteValue ?: return null
        val instant = StatementDateParser.parseDateAndTime(dateRaw.trim(), timeRaw.trim(), zone)
            ?: StatementDateParser.parseToInstant(dateRaw.trim(), zone)
            ?: return null

        val cleanedDetail = detail.trim().replace(Regex("""\s+"""), " ")
        val type = when {
            cleanedDetail.startsWith("Received", ignoreCase = true) ||
                cleanedDetail.startsWith("Refund", ignoreCase = true) -> TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }
        val merchant = Regex("""(?i)^(?:Paid to|Received from|Refund from)\s+(.+)$""")
            .find(cleanedDetail)?.groupValues?.get(1)?.trim()
            ?: cleanedDetail

        return ParsedTransaction(
            amount = amount,
            type = type,
            transactionDate = instant,
            description = cleanedDetail,
            merchantName = merchant,
            referenceNumber = upiId.trim(),
            paymentMethod = PaymentMethod.UPI,
            source = TransactionSource.PDF,
            sourceIdentifier = "gpay-pdf:${upiId.trim()}",
            originLabel = fileName,
            parseStatus = ParseStatus.PARSED,
        )
    }

    private fun parseDatedBlocks(text: String, fileName: String): List<ParsedTransaction> {
        val blocks = splitIntoDatedBlocks(text)
        if (blocks.isEmpty()) {
            return listOf(failedStub(fileName, text, "No dated amount rows found in PDF"))
        }

        return blocks.mapIndexedNotNull { index, block ->
            if (isSummaryOrPeriodBlock(block)) return@mapIndexedNotNull null

            val tokenFields = StatementFieldClassifier.classifyAll(tokenize(block)).toMutableList()
            residualNarrative(block, tokenFields)?.let { narrative ->
                tokenFields += ClassifiedField(raw = narrative, kind = StatementFieldKind.NARRATIVE)
            }
            val assembled = StatementRowAssembler.assemble(tokenFields, zone) ?: return@mapIndexedNotNull null
            // Ignore header/totals that only have a date + amount with no payment cue.
            if (!looksLikePaymentRow(block, assembled)) return@mapIndexedNotNull null
            toParsed(assembled, block, index, fileName)
        }.ifEmpty {
            listOf(failedStub(fileName, text, "Could not assemble transactions from PDF text"))
        }
    }

    private fun toParsed(
        row: StatementRowAssembler.AssembledRow,
        block: String,
        index: Int,
        fileName: String,
    ): ParsedTransaction {
        val sourceIdentifier = when {
            row.phonePeTransactionId != null -> "phonepe-pdf:${row.phonePeTransactionId}"
            row.referenceNumber != null -> "pdf-ref:${row.referenceNumber}"
            else -> "pdf-$fileName-$index-${Sha256.hexOf(block).take(16)}"
        }
        val status = when (row.confidence) {
            StatementRowAssembler.Confidence.HIGH -> ParseStatus.PARSED
            else -> ParseStatus.PARTIALLY_PARSED
        }
        return ParsedTransaction(
            amount = row.amount,
            type = row.type,
            transactionDate = row.transactionDate,
            description = row.description,
            merchantName = row.merchantName,
            referenceNumber = row.referenceNumber,
            accountIdentifier = row.accountIdentifier,
            paymentMethod = if (row.paymentMethodIsUpi) PaymentMethod.UPI else PaymentMethod.UNKNOWN,
            source = TransactionSource.PDF,
            sourceIdentifier = sourceIdentifier,
            originLabel = fileName,
            parseStatus = status,
            rawPayload = if (status != ParseStatus.PARSED) block.take(500) else null,
        )
    }

    private fun failedStub(fileName: String, text: String, message: String): ParsedTransaction =
        ParsedTransaction(
            amount = Money.zero(),
            type = TransactionType.DEBIT,
            transactionDate = java.time.Instant.EPOCH,
            description = message,
            source = TransactionSource.PDF,
            sourceIdentifier = "pdf-fail-${Sha256.hexOf(fileName).take(12)}",
            originLabel = fileName,
            parseStatus = ParseStatus.FAILED,
            rawPayload = text.take(500),
        )

    private fun splitIntoDatedBlocks(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val blocks = mutableListOf<StringBuilder>()
        var current: StringBuilder? = null

        for (line in lines) {
            if (isBoilerplate(line) || isDateRangeLine(line)) continue
            val lineFields = StatementFieldClassifier.classifyAll(tokenize(line))
            val startsTransaction = lineFields.any { it.kind == StatementFieldKind.DATE }
            if (startsTransaction) {
                current = StringBuilder(line)
                blocks += current
            } else if (current != null) {
                current.append(' ').append(line)
            }
        }
        return blocks.map { it.toString() }
    }

    internal fun tokenize(text: String): List<String> =
        text.split(Regex("""\s+"""))
            .map { it.trim().trim(',', ';') }
            .filter { it.isNotEmpty() }

    /**
     * Normalises PdfBox / GPay quirks so the content classifier sees familiar tokens.
     */
    internal fun preprocess(text: String): String =
        text
            .replace(Regex("""(?i)UPITransactionID\s*:"""), "UPI Transaction ID:")
            .replace(Regex("""(?i)UPI\s*Transaction\s*ID\s*:"""), "UPI Transaction ID:")
            .replace(Regex("""(?i)(\d{1,2}:\d{2})([ap]m)\b"""), "$1 $2")
            .replace(Regex("""(?i)Paidby"""), "Paid by ")
            .replace(Regex("""(?i)Receivedby"""), "Received by ")
            .replace(Regex("""(?i)Paidto"""), "Paid to ")
            .replace(Regex("""(?i)Receivedfrom"""), "Received from ")
            .replace(Regex("""(?i)Date&time"""), "Date & time")
            .replace(Regex("""(?i)Transactiondetails"""), "Transaction details")
            .replace(Regex("""(?i)Transactionstatement"""), "Transaction statement")
            .replace(Regex("""UPI Transaction ID:\s*"""), "UPI Transaction ID: ")

    private fun residualNarrative(block: String, fields: List<ClassifiedField>): String? {
        var residual = block
        fields.filter {
            it.kind == StatementFieldKind.DATE ||
                it.kind == StatementFieldKind.TIME ||
                it.kind == StatementFieldKind.MONEY ||
                it.kind == StatementFieldKind.REFERENCE ||
                it.kind == StatementFieldKind.TYPE_SIGNAL ||
                it.kind == StatementFieldKind.ACCOUNT_MASK
        }.forEach { field ->
            residual = residual.replace(field.raw, " ")
        }
        return residual.replace(Regex("""\s+"""), " ").trim()
            .takeIf { it.length >= 3 && it.any { ch -> ch.isLetter() } }
    }

    private fun isBoilerplate(line: String): Boolean {
        val upper = line.uppercase()
        return upper.contains("STATEMENT") && !upper.any { it.isDigit() } ||
            upper.contains("OPENING BALANCE") ||
            upper.contains("CLOSING BALANCE") ||
            upper.startsWith("PAGE ") ||
            upper.startsWith("SR NO") ||
            upper.startsWith("DATE & TIME") ||
            upper.startsWith("NOTE:") ||
            upper.contains("GOOGLE PAY APP") ||
            upper == "SENT" ||
            upper == "RECEIVED" ||
            upper == "AMOUNT" ||
            upper == "TRANSACTION DETAILS"
    }

    private fun isDateRangeLine(line: String): Boolean {
        // "01 August 2026 - 31 August 2026" period header — not a transaction.
        val dates = Regex("""\d{1,2}\s+[A-Za-z]{3,9},?\s+\d{4}|\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""")
            .findAll(line)
            .count { StatementDateParser.parseDate(it.value) != null }
        return dates >= 2 && line.contains('-')
    }

    private fun isSummaryOrPeriodBlock(block: String): Boolean {
        val upper = block.uppercase()
        if (upper.contains("STATEMENT PERIOD")) return true
        if (isDateRangeLine(block)) return true
        val hasPaymentCue = upper.contains("PAID TO") ||
            upper.contains("RECEIVED FROM") ||
            upper.contains("REFUND FROM") ||
            upper.contains("UPI")
        val looksLikeTotals = (upper.contains("SENT") || upper.contains("RECEIVED")) &&
            !hasPaymentCue
        return looksLikeTotals
    }

    private fun looksLikePaymentRow(block: String, row: StatementRowAssembler.AssembledRow): Boolean {
        val upper = block.uppercase()
        if (upper.contains("PAID TO") || upper.contains("RECEIVED FROM") || upper.contains("REFUND")) {
            return true
        }
        if (row.referenceNumber != null || row.phonePeTransactionId != null) return true
        if (upper.contains("UPI") || upper.contains("IMPS") || upper.contains("NEFT")) return true
        // Generic bank lines often have a narration with letters + amount.
        return (row.description?.count { it.isLetter() } ?: 0) >= 4
    }

    private fun looksLikeGooglePay(text: String): Boolean {
        val upper = text.uppercase()
        return upper.contains("GOOGLE PAY") ||
            upper.contains("UPI TRANSACTION ID") ||
            (upper.contains("PAID TO") && upper.contains("TRANSACTION STATEMENT"))
    }

    companion object {
        /** PdfBox-style stacked rows: date, time, paid to, UPI id, amount. */
        private val GPAY_STACKED = Regex(
            """(?is)(\d{1,2}\s+[A-Za-z]{3,9},?\s+\d{4})\s+(\d{1,2}:\d{2}\s*[AaPp][Mm])\s+""" +
                """((?:Paid to|Received from|Refund from)\s+[^\n\r]+?)\s+""" +
                """UPI\s+Transaction\s+ID:\s*(\d{6,})\s+""" +
                """(?:Paid by[^\n\r₹]*)?\s*₹\s*([\d,]+\.?\d*)""",
        )

        /** Layout-extracted rows: date, paid to, amount, time, UPI id. */
        private val GPAY_LAYOUT = Regex(
            """(?is)(\d{1,2}\s+[A-Za-z]{3,9},?\s+\d{4})\s+""" +
                """((?:Paid to|Received from|Refund from)\s+.+?)\s+""" +
                """₹\s*([\d,]+\.?\d*)\s+""" +
                """(\d{1,2}:\d{2}\s*[AaPp][Mm])\s+""" +
                """UPI\s+Transaction\s+ID:\s*(\d{6,})""",
        )
    }
}

data class PdfParseInput(
    val statementText: String,
    val fileName: String,
)
