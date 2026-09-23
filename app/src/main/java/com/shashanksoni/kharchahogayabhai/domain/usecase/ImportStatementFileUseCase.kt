package com.shashanksoni.kharchahogayabhai.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionIngestor
import com.shashanksoni.kharchahogayabhai.csv.CsvParseInput
import com.shashanksoni.kharchahogayabhai.csv.CsvTransactionParser
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatchStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ImportResult
import com.shashanksoni.kharchahogayabhai.domain.model.IngestOutcome
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.repository.ImportRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.pdf.PdfParseInput
import com.shashanksoni.kharchahogayabhai.pdf.PdfTextExtractor
import com.shashanksoni.kharchahogayabhai.pdf.PdfTransactionParser
import java.time.Clock

/**
 * Reads a user-selected CSV or PDF via SAF, parses it, deduplicates, and records
 * an import-history entry. Never uploads the file anywhere.
 */
class ImportStatementFileUseCase(
    private val appContext: Context,
    private val csvParser: CsvTransactionParser,
    private val pdfParser: PdfTransactionParser,
    private val pdfTextExtractor: PdfTextExtractor,
    private val ingestor: TransactionIngestor,
    private val importRepository: ImportRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend operator fun invoke(uri: Uri): ImportResult {
        val fileName = resolveDisplayName(uri) ?: "imported_file"
        val mimeType = appContext.contentResolver.getType(uri).orEmpty()
        val source = detectSource(fileName, mimeType)
            ?: return failedBatch(
                fileName = fileName,
                source = TransactionSource.CSV,
                message = "Unsupported file type. Choose a CSV or PDF statement.",
            )

        return try {
            when (source) {
                TransactionSource.CSV -> importCsv(uri, fileName)
                TransactionSource.PDF -> importPdf(uri, fileName)
                else -> failedBatch(fileName, source, "Unsupported source for file import.")
            }
        } catch (error: Exception) {
            failedBatch(
                fileName = fileName,
                source = source,
                message = error.message ?: "Import failed",
            )
        }
    }

    private suspend fun importCsv(uri: Uri, fileName: String): ImportResult {
        val text = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: return failedBatch(fileName, TransactionSource.CSV, "Could not read the CSV file.")

        val input = CsvParseInput(csvText = text, fileName = fileName)
        if (!csvParser.canParse(input)) {
            return failedBatch(
                fileName,
                TransactionSource.CSV,
                "Could not recognise the CSV columns. Expected a date column and debit/credit or amount.",
            )
        }
        return finishImport(fileName, TransactionSource.CSV, csvParser.parse(input))
    }

    private suspend fun importPdf(uri: Uri, fileName: String): ImportResult {
        val text = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            pdfTextExtractor.extractText(stream)
        } ?: return failedBatch(fileName, TransactionSource.PDF, "Could not read the PDF file.")

        if (text.isBlank()) {
            return failedBatch(
                fileName,
                TransactionSource.PDF,
                "No text found in the PDF. Scanned/image PDFs are not supported yet.",
            )
        }

        val input = PdfParseInput(statementText = text, fileName = fileName)
        if (!pdfParser.canParse(input)) {
            return failedBatch(
                fileName,
                TransactionSource.PDF,
                "Could not find dated transactions in the PDF text.",
            )
        }
        return finishImport(fileName, TransactionSource.PDF, pdfParser.parse(input))
    }

    private suspend fun finishImport(
        fileName: String,
        source: TransactionSource,
        parsed: List<ParsedTransaction>,
    ): ImportResult {
        val outcomes = ingestor.ingest(parsed)
        val newCount = outcomes.count { it == IngestOutcome.CREATED }
        val mergedCount = outcomes.count { it == IngestOutcome.MERGED }
        val duplicateCount = outcomes.count { it == IngestOutcome.DUPLICATE_SKIPPED }
        val failedCount = outcomes.count { it == IngestOutcome.FAILED }

        val status = when {
            outcomes.isEmpty() || (newCount == 0 && mergedCount == 0 && failedCount == outcomes.size) ->
                ImportBatchStatus.FAILED

            failedCount > 0 -> ImportBatchStatus.PARTIAL
            else -> ImportBatchStatus.SUCCESS
        }

        val batch = ImportBatch(
            fileName = fileName,
            source = source,
            importedAt = clock.instant(),
            totalParsed = parsed.size,
            newCount = newCount,
            mergedCount = mergedCount,
            duplicateCount = duplicateCount,
            failedCount = failedCount,
            status = status,
            errorMessage = when (status) {
                ImportBatchStatus.FAILED -> "No new transactions could be imported."
                ImportBatchStatus.PARTIAL ->
                    "Imported with $failedCount row(s) that could not be fully understood."

                ImportBatchStatus.SUCCESS -> null
            },
        )
        val id = importRepository.saveImportBatch(batch)
        return ImportResult(
            batch = batch.copy(id = id),
            outcomes = outcomes,
            scannedCount = parsed.size,
            storedTotalCount = transactionRepository.countTransactions(),
        )
    }

    private suspend fun failedBatch(
        fileName: String,
        source: TransactionSource,
        message: String,
    ): ImportResult {
        val batch = ImportBatch(
            fileName = fileName,
            source = source,
            importedAt = clock.instant(),
            totalParsed = 0,
            newCount = 0,
            mergedCount = 0,
            duplicateCount = 0,
            failedCount = 0,
            status = ImportBatchStatus.FAILED,
            errorMessage = message,
        )
        val id = importRepository.saveImportBatch(batch)
        return ImportResult(batch = batch.copy(id = id), outcomes = emptyList())
    }

    private fun detectSource(fileName: String, mimeType: String): TransactionSource? {
        val lowerName = fileName.lowercase()
        val lowerMime = mimeType.lowercase()
        return when {
            lowerName.endsWith(".csv") ||
                lowerMime.contains("csv") ||
                lowerMime == "text/comma-separated-values" ||
                lowerMime == "text/plain" && lowerName.endsWith(".csv") -> TransactionSource.CSV

            lowerName.endsWith(".pdf") || lowerMime.contains("pdf") -> TransactionSource.PDF
            else -> null
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        DocumentFile.fromSingleUri(appContext, uri)?.name?.let { return it }
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        return uri.lastPathSegment
    }
}
