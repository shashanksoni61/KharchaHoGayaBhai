package com.shashanksoni.kharchahogayabhai.domain.usecase

import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionIngestor
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatchStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ImportResult
import com.shashanksoni.kharchahogayabhai.domain.model.IngestOutcome
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.repository.ImportRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.sms.SmsInboxReader
import com.shashanksoni.kharchahogayabhai.sms.SmsParseInput
import com.shashanksoni.kharchahogayabhai.sms.SmsScanProgress
import com.shashanksoni.kharchahogayabhai.sms.SmsScanPreferences
import com.shashanksoni.kharchahogayabhai.sms.SmsTransactionParser
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

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
 * Walks the inbox in pages so a 20k+ mailbox is fully read instead of stopping
 * at a hard cap. Remembers the newest SMS considered so the next incremental
 * scan only reads newer messages.
 */
class ImportSmsInboxUseCase(
    private val inboxReader: SmsInboxReader,
    private val smsParser: SmsTransactionParser,
    private val ingestor: TransactionIngestor,
    private val importRepository: ImportRepository,
    private val transactionRepository: TransactionRepository,
    private val scanPreferences: SmsScanPreferences,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val mutex = Mutex()

    suspend operator fun invoke(
        mode: SmsScanMode = SmsScanMode.INCREMENTAL,
        recordEmptyHistory: Boolean = true,
        onProgress: (SmsScanProgress) -> Unit = {},
    ): ImportResult = mutex.withLock {
        if (mode == SmsScanMode.FULL) {
            scanPreferences.clearCursor()
        }

        val inboxTotal = runCatching { inboxReader.countInbox() }.getOrDefault(0)
        onProgress(SmsScanProgress(scannedCount = 0, inboxTotal = inboxTotal, parsedCount = 0))

        val startingCursor = scanPreferences.scanCursor()
        val allOutcomes = mutableListOf<IngestOutcome>()
        val created = mutableListOf<ParsedTransaction>()
        var scannedCount = 0
        var parsedCount = 0
        var pagesRead = 0

        while (true) {
            val cursor = scanPreferences.scanCursor()
            val messages = try {
                inboxReader.readInbox(
                    afterExclusive = cursor,
                    limit = SCAN_PAGE_SIZE,
                )
            } catch (error: SecurityException) {
                return failed(
                    message = "SMS permission is required to read transaction alerts.",
                    recordHistory = recordEmptyHistory,
                )
            } catch (error: Exception) {
                return failed(
                    message = error.message ?: "Could not read SMS inbox.",
                    recordHistory = recordEmptyHistory,
                    scannedCount = scannedCount,
                )
            }

            if (messages.isEmpty()) {
                if (pagesRead == 0) {
                    if (mode == SmsScanMode.INCREMENTAL && startingCursor != null) {
                        scanPreferences.advanceCursorTo(clock.instant())
                    }
                    return failed(
                        message = if (startingCursor == null) {
                            "No SMS messages found on this device."
                        } else {
                            "No new SMS since the last scan."
                        },
                        recordHistory = recordEmptyHistory,
                        scannedCount = 0,
                    )
                }
                break
            }

            pagesRead += 1
            scannedCount += messages.size
            val newestReadMessage = messages.maxWithOrNull(
                compareBy({ it.receivedAt }, { it.id }),
            )
            newestReadMessage?.let(scanPreferences::advanceCursorPast)

            val input = SmsParseInput(messages)
            val parsed = if (smsParser.canParse(input)) {
                smsParser.parse(input)
            } else {
                emptyList()
            }
            if (parsed.isNotEmpty()) {
                val outcomes = ingestor.ingest(parsed)
                parsedCount += parsed.size
                allOutcomes += outcomes
                if (!recordEmptyHistory) {
                    parsed.zip(outcomes).forEach { (row, outcome) ->
                        if (outcome == IngestOutcome.CREATED) created += row
                    }
                }
            }

            onProgress(
                SmsScanProgress(
                    scannedCount = scannedCount,
                    inboxTotal = inboxTotal,
                    parsedCount = parsedCount,
                ),
            )
            yield()
        }

        if (parsedCount == 0) {
            return failed(
                message = "No bank or UPI transaction alerts were recognised in the scanned SMS.",
                recordHistory = recordEmptyHistory,
                scannedCount = scannedCount,
            )
        }

        return completeImport(
            parsedCount = parsedCount,
            outcomes = allOutcomes,
            created = created,
            scannedCount = scannedCount,
        )
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

    private suspend fun completeImport(
        parsedCount: Int,
        outcomes: List<IngestOutcome>,
        created: List<ParsedTransaction>,
        scannedCount: Int,
    ): ImportResult {
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
            totalParsed = parsedCount,
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
        return ImportResult(
            batch = batch.copy(id = id),
            outcomes = outcomes,
            scannedCount = scannedCount,
            storedTotalCount = transactionRepository.countTransactions(),
            createdTransactions = created,
        )
    }

    private suspend fun failed(
        message: String,
        recordHistory: Boolean,
        scannedCount: Int = 0,
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
        val storedTotal = transactionRepository.countTransactions()
        if (!recordHistory) {
            return ImportResult(
                batch = batch,
                outcomes = emptyList(),
                scannedCount = scannedCount,
                storedTotalCount = storedTotal,
            )
        }
        val id = importRepository.saveImportBatch(batch)
        return ImportResult(
            batch = batch.copy(id = id),
            outcomes = emptyList(),
            scannedCount = scannedCount,
            storedTotalCount = storedTotal,
        )
    }

    companion object {
        const val SCAN_PAGE_SIZE = 1_000
    }
}
