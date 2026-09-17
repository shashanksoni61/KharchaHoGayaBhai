package com.shashanksoni.kharchahogayabhai.csv

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
 * Content-driven CSV parser.
 *
 * Column headers are optional hints only. Every row is classified cell-by-cell
 * (date / time / amount / reference / narrative / …). A row becomes a transaction
 * when it contains at least a **date** and an **amount**, regardless of column
 * names or order — so PhonePe, HDFC, SBI and renamed exports all feed the same
 * dashboard.
 */
class CsvTransactionParser(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : TransactionParser<CsvParseInput> {

    override val source: TransactionSource = TransactionSource.CSV
    override val name: String = "Content-driven CSV"

    override fun canParse(input: CsvParseInput): Boolean =
        input.csvText.lineSequence()
            .map { parseCsvLine(it.trimEnd('\r')) }
            .any { cells ->
                val fields = StatementFieldClassifier.classifyAll(cells)
                fields.any { it.kind == StatementFieldKind.DATE } &&
                    fields.any { it.kind == StatementFieldKind.MONEY }
            }

    override fun parse(input: CsvParseInput): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        input.csvText.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .filterNot { isBoilerplate(it) }
            .forEachIndexed { index, line ->
                val cells = parseCsvLine(line).map { it.trim().trim('"') }
                if (cells.size < 2) return@forEachIndexed

                val fields = StatementFieldClassifier.classifyAll(cells)
                val assembled = StatementRowAssembler.assemble(fields, zone) ?: return@forEachIndexed

                results += toParsedTransaction(assembled, line, index, input.fileName)
            }

        if (results.isEmpty()) {
            results += ParsedTransaction(
                amount = Money.zero(),
                type = TransactionType.DEBIT,
                transactionDate = java.time.Instant.EPOCH,
                description = "No date+amount rows found in CSV",
                source = TransactionSource.CSV,
                sourceIdentifier = "csv-empty-${Sha256.hexOf(input.fileName).take(12)}",
                originLabel = input.fileName,
                parseStatus = ParseStatus.FAILED,
                rawPayload = input.csvText.take(300),
            )
        }
        return results
    }

    private fun toParsedTransaction(
        row: StatementRowAssembler.AssembledRow,
        line: String,
        index: Int,
        fileName: String,
    ): ParsedTransaction {
        val sourceIdentifier = when {
            row.phonePeTransactionId != null -> "phonepe:${row.phonePeTransactionId}"
            row.referenceNumber != null -> "csv-ref:${row.referenceNumber}"
            else -> "csv-$fileName-r$index-${Sha256.hexOf(line).take(16)}"
        }

        val parseStatus = when (row.confidence) {
            StatementRowAssembler.Confidence.HIGH -> ParseStatus.PARSED
            StatementRowAssembler.Confidence.MEDIUM -> ParseStatus.PARTIALLY_PARSED
            StatementRowAssembler.Confidence.LOW -> ParseStatus.PARTIALLY_PARSED
        }

        return ParsedTransaction(
            amount = row.amount,
            type = row.type,
            transactionDate = row.transactionDate,
            description = buildList {
                row.description?.let { add(it) }
                row.phonePeTransactionId
                    ?.takeIf { it != row.referenceNumber }
                    ?.let { add("Txn ID $it") }
            }.joinToString(" · ").ifBlank { null },
            merchantName = row.merchantName,
            referenceNumber = row.referenceNumber,
            accountIdentifier = row.accountIdentifier,
            bankName = if (row.phonePeTransactionId != null) "PhonePe" else null,
            paymentMethod = if (row.paymentMethodIsUpi) PaymentMethod.UPI else PaymentMethod.UNKNOWN,
            source = TransactionSource.CSV,
            sourceIdentifier = sourceIdentifier,
            originLabel = fileName,
            parseStatus = parseStatus,
            rawPayload = if (parseStatus != ParseStatus.PARSED) line.take(500) else null,
        )
    }

    private fun isBoilerplate(line: String): Boolean {
        val lower = line.lowercase()
        return lower.startsWith("this is an automatically") ||
            lower.startsWith("disclaimer") ||
            lower.contains("phonepe terms") ||
            lower.contains("fictitious offers") ||
            lower.startsWith("transaction statement for") ||
            lower.startsWith("duration")
    }

    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                c == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }

                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }
}

data class CsvParseInput(
    val csvText: String,
    val fileName: String,
)
