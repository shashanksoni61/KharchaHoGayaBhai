package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant

/** Outcome of one file import run, shown in import history. */
data class ImportBatch(
    val id: Long = UNSAVED_ID,
    val fileName: String,
    val source: TransactionSource,
    val importedAt: Instant,
    val totalParsed: Int,
    val newCount: Int,
    val mergedCount: Int,
    val duplicateCount: Int,
    val failedCount: Int,
    val status: ImportBatchStatus,
    val errorMessage: String? = null,
) {
    companion object {
        const val UNSAVED_ID = 0L
    }
}

enum class ImportBatchStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
}

/**
 * How one parsed row was handled during ingest.
 * Kept separate from [ParseStatus], which describes parse quality only.
 */
enum class IngestOutcome {
    CREATED,
    MERGED,
    DUPLICATE_SKIPPED,
    FAILED,
}

data class ImportResult(
    val batch: ImportBatch,
    val outcomes: List<IngestOutcome>,
    /** How many source records (SMS messages / statement rows) were inspected this run. */
    val scannedCount: Int = 0,
    /** Total transactions the app now holds data from, after this import. */
    val storedTotalCount: Int = 0,
)
