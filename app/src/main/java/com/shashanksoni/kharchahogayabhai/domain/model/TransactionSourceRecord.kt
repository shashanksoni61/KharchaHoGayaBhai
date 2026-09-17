package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant

/**
 * Evidence that one source contributed to a transaction.
 *
 * Deduplication merges transactions but must not throw away provenance, so each
 * contributing source keeps its own record. That is what lets the detail screen
 * show "seen in SMS, CSV and PDF" and lets a future re-import recognise data it
 * has already ingested.
 */
data class TransactionSourceRecord(
    val id: Long = UNSAVED_ID,
    /** [UNSAVED_ID] until the transaction it belongs to has been stored. */
    val transactionId: Long = UNSAVED_ID,
    val source: TransactionSource,
    /**
     * Stable identity of the incoming item within its source, e.g. an SMS id, a
     * hash of a CSV row, or a PDF page + line hash. Used to avoid ingesting the
     * exact same input twice. Null when the source cannot supply one.
     */
    val sourceIdentifier: String? = null,
    /** Human-friendly origin shown in the UI, e.g. "HDFCBK" or "September Statement.pdf". */
    val originLabel: String? = null,
    /**
     * Original text, kept only when parsing was incomplete so the user can
     * review it. Successfully parsed sources store nothing: raw SMS and
     * statement text is sensitive and we do not retain it by default.
     */
    val rawPayload: String? = null,
    val importedAt: Instant,
) {
    companion object {
        const val UNSAVED_ID = 0L
    }
}
