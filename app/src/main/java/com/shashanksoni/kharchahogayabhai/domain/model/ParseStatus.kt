package com.shashanksoni.kharchahogayabhai.domain.model

/**
 * How confident we are in the values extracted for a transaction.
 *
 * This describes *parse quality only*. Deduplication outcomes (created, merged,
 * duplicate) belong to the import result rather than to the stored transaction,
 * so they are deliberately not modelled here.
 */
enum class ParseStatus {
    /** Every field we need was extracted with confidence. */
    PARSED,

    /** Usable but incomplete, e.g. amount and date known, merchant missing. */
    PARTIALLY_PARSED,

    /**
     * Could not be understood. Kept so the user can review it rather than
     * silently losing data; the original text lives on the source record.
     */
    FAILED,
}
