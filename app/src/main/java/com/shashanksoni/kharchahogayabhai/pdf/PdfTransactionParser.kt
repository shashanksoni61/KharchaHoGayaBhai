package com.shashanksoni.kharchahogayabhai.pdf

import com.shashanksoni.kharchahogayabhai.core.common.Sha256
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
 */
class PdfTransactionParser(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : TransactionParser<PdfParseInput> {

    override val source: TransactionSource = TransactionSource.PDF
    override val name: String = "Content-driven PDF"

    override fun canParse(input: PdfParseInput): Boolean {
        val tokens = tokenize(input.statementText)
        val fields = StatementFieldClassifier.classifyAll(tokens)
        return fields.any { it.kind == StatementFieldKind.DATE } &&
            fields.any { it.kind == StatementFieldKind.MONEY }
    }

    override fun parse(input: PdfParseInput): List<ParsedTransaction> {
        val blocks = splitIntoDatedBlocks(input.statementText)
        if (blocks.isEmpty()) {
            return listOf(
                ParsedTransaction(
                    amount = Money.zero(),
                    type = TransactionType.DEBIT,
                    transactionDate = java.time.Instant.EPOCH,
                    description = "No dated amount rows found in PDF",
                    source = TransactionSource.PDF,
                    sourceIdentifier = "pdf-empty-${Sha256.hexOf(input.fileName).take(12)}",
                    originLabel = input.fileName,
                    parseStatus = ParseStatus.FAILED,
                    rawPayload = input.statementText.take(500),
                ),
            )
        }

        return blocks.mapIndexedNotNull { index, block ->
            val tokenFields = StatementFieldClassifier.classifyAll(tokenize(block)).toMutableList()
            // Keep the unbroken narration: tokenising "Paid to Foo Bar" would lose the merchant.
            residualNarrative(block, tokenFields)?.let { narrative ->
                tokenFields += com.shashanksoni.kharchahogayabhai.core.parser.ClassifiedField(
                    raw = narrative,
                    kind = StatementFieldKind.NARRATIVE,
                )
            }
            val assembled = StatementRowAssembler.assemble(tokenFields, zone) ?: return@mapIndexedNotNull null
            toParsed(assembled, block, index, input.fileName)
        }.ifEmpty {
            listOf(
                ParsedTransaction(
                    amount = Money.zero(),
                    type = TransactionType.DEBIT,
                    transactionDate = java.time.Instant.EPOCH,
                    description = "Could not assemble transactions from PDF text",
                    source = TransactionSource.PDF,
                    sourceIdentifier = "pdf-fail-${Sha256.hexOf(input.fileName).take(12)}",
                    originLabel = input.fileName,
                    parseStatus = ParseStatus.FAILED,
                    rawPayload = input.statementText.take(500),
                ),
            )
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

    private fun splitIntoDatedBlocks(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val blocks = mutableListOf<StringBuilder>()
        var current: StringBuilder? = null

        for (line in lines) {
            if (isBoilerplate(line)) continue
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

    private fun residualNarrative(
        block: String,
        fields: List<com.shashanksoni.kharchahogayabhai.core.parser.ClassifiedField>,
    ): String? {
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
            upper.startsWith("SR NO")
    }
}

data class PdfParseInput(
    val statementText: String,
    val fileName: String,
)
