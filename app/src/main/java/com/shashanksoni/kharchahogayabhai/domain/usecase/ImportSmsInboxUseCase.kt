package com.shashanksoni.kharchahogayabhai.domain.usecase

import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionIngestor
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatchStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ImportResult
import com.shashanksoni.kharchahogayabhai.domain.model.IngestOutcome
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.repository.ImportRepository
import com.shashanksoni.kharchahogayabhai.sms.SmsInboxReader
import com.shashanksoni.kharchahogayabhai.sms.SmsParseInput
import com.shashanksoni.kharchahogayabhai.sms.SmsTransactionParser
import java.time.Clock

/**
 * Scans the device SMS inbox for bank / UPI alerts and ingests them through the
 * same dedup path as CSV/PDF (UTR / fingerprint / sms:{id} source key).
 */
class ImportSmsInboxUseCase(
    private val inboxReader: SmsInboxReader,
    private val smsParser: SmsTransactionParser,
    private val ingestor: TransactionIngestor,
    private val importRepository: ImportRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend operator fun invoke(): ImportResult {
        val messages = try {
            inboxReader.readInbox()
        } catch (error: SecurityException) {
            return failed("SMS permission is required to read transaction alerts.")
        } catch (error: Exception) {
            return failed(error.message ?: "Could not read SMS inbox.")
        }

        if (messages.isEmpty()) {
            return failed("No SMS messages found on this device.")
        }

        val input = SmsParseInput(messages)
        if (!smsParser.canParse(input)) {
            return failed("No bank or UPI transaction alerts were recognised in the inbox.")
        }

        val parsed = smsParser.parse(input)
        return finishImport(parsed)
    }

    private suspend fun finishImport(parsed: List<ParsedTransaction>): ImportResult {
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
            fileName = "SMS inbox",
            source = TransactionSource.SMS,
            importedAt = clock.instant(),
            totalParsed = parsed.size,
            newCount = newCount,
            mergedCount = mergedCount,
            duplicateCount = duplicateCount,
            failedCount = failedCount,
            status = status,
            errorMessage = when (status) {
                ImportBatchStatus.FAILED ->
                    "No new SMS transactions could be imported (all may already be present)."

                ImportBatchStatus.PARTIAL ->
                    "Imported with $failedCount alert(s) that could not be fully understood."

                ImportBatchStatus.SUCCESS -> null
            },
        )
        val id = importRepository.saveImportBatch(batch)
        return ImportResult(batch = batch.copy(id = id), outcomes = outcomes)
    }

    private suspend fun failed(message: String): ImportResult {
        val batch = ImportBatch(
            fileName = "SMS inbox",
            source = TransactionSource.SMS,
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
}
