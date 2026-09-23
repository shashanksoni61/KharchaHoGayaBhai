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
import com.shashanksoni.kharchahogayabhai.sms.SmsMessage
import com.shashanksoni.kharchahogayabhai.sms.SmsParseInput
import com.shashanksoni.kharchahogayabhai.sms.SmsScanPreferences
import com.shashanksoni.kharchahogayabhai.sms.SmsTransactionParser
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SmsScanMode {
    /** Only messages newer than the last successful scan cursor. */
    INCREMENTAL,

    /** Entire inbox; resets and then advances the cursor. */
    FULL,
}

/**
 * Scans the device SMS inbox for bank / UPI alerts and ingests them through the
 * same dedup path as CSV/PDF (UTR / fingerprint / sms:{id} source key).
 *
 * Remembers the newest SMS date considered so the next scan only reads newer
 * messages. Auto-open sync uses [SmsScanMode.INCREMENTAL] and skips noisy empty history.
 */
class ImportSmsInboxUseCase(
    private val inboxReader: SmsInboxReader,
    private val smsParser: SmsTransactionParser,
    private val ingestor: TransactionIngestor,
    private val importRepository: ImportRepository,
    private val scanPreferences: SmsScanPreferences,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val mutex = Mutex()

    suspend operator fun invoke(
        mode: SmsScanMode = SmsScanMode.INCREMENTAL,
        recordEmptyHistory: Boolean = true,
    ): ImportResult = mutex.withLock {
        if (mode == SmsScanMode.FULL) {
            scanPreferences.clearCursor()
        }

        val cursor = scanPreferences.scanCursor()
        val messages = try {
            inboxReader.readInbox(afterExclusive = cursor)
        } catch (error: SecurityException) {
            return failed(
                message = "SMS permission is required to read transaction alerts.",
                recordHistory = recordEmptyHistory,
            )
        } catch (error: Exception) {
            return failed(
                message = error.message ?: "Could not read SMS inbox.",
                recordHistory = recordEmptyHistory,
            )
        }

        // Newest by provider date, then inbox id — keeps same-timestamp bursts ordered.
        val newestReadMessage = messages.maxWithOrNull(
            compareBy({ it.receivedAt }, { it.id }),
        )
        if (messages.isEmpty()) {
            // Caught up — bump cursor to now so we do not keep re-querying forever
            // when the user opens the app with no new SMS.
            if (mode == SmsScanMode.INCREMENTAL && cursor != null) {
                scanPreferences.advanceCursorTo(clock.instant())
            }
            return failed(
                message = if (cursor == null) {
                    "No SMS messages found on this device."
                } else {
                    "No new SMS since the last scan."
                },
                recordHistory = recordEmptyHistory,
            )
        }

        val input = SmsParseInput(messages)
        val parsed = if (smsParser.canParse(input)) {
            smsParser.parse(input)
        } else {
            emptyList()
        }

        // Always advance past what we inspected, including OTPs we skipped.
        newestReadMessage?.let(scanPreferences::advanceCursorPast)

        if (parsed.isEmpty()) {
            return failed(
                message = "No bank or UPI transaction alerts were recognised in the scanned SMS.",
                recordHistory = recordEmptyHistory,
            )
        }

        return finishImport(parsed, newestReadMessage)
    }

    /**
     * Silent background sync used when the app opens. Skips if auto-scan is off.
     * Does not write empty/failed import-history rows.
     */
    suspend fun syncOnAppOpen(): ImportResult? {
        if (!scanPreferences.autoScanOnOpenEnabled()) return null
        return silentIncremental()
    }

    /**
     * Silent incremental sync after a new SMS broadcast. Skips if background
     * listening is disabled in Settings.
     */
    suspend fun syncOnNewSms(): ImportResult? {
        if (!scanPreferences.listenInBackgroundEnabled()) return null
        return silentIncremental()
    }

    private suspend fun silentIncremental(): ImportResult? {
        return invoke(mode = SmsScanMode.INCREMENTAL, recordEmptyHistory = false)
            .takeIf { result ->
                result.batch.newCount > 0 ||
                    result.batch.mergedCount > 0 ||
                    result.batch.totalParsed > 0
            }
    }

    private suspend fun finishImport(
        parsed: List<ParsedTransaction>,
        newestReadMessage: SmsMessage?,
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

        newestReadMessage?.let(scanPreferences::advanceCursorPast)

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

    private suspend fun failed(
        message: String,
        recordHistory: Boolean,
    ): ImportResult {
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
        if (!recordHistory) {
            return ImportResult(batch = batch, outcomes = emptyList())
        }
        val id = importRepository.saveImportBatch(batch)
        return ImportResult(batch = batch.copy(id = id), outcomes = emptyList())
    }
}
