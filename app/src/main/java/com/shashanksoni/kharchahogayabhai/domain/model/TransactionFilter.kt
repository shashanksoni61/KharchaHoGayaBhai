package com.shashanksoni.kharchahogayabhai.domain.model

/**
 * Everything the transaction list can be narrowed by. Null or empty means
 * "don't filter on this".
 *
 * [source] matches any transaction that a given source contributed to, not just
 * the one that created it, so filtering by CSV still finds a transaction first
 * seen over SMS and later confirmed by a CSV import.
 */
data class TransactionFilter(
    val searchQuery: String? = null,
    val dateRange: InstantRange? = null,
    val type: TransactionType? = null,
    val source: TransactionSource? = null,
    val categoryIds: Set<Long> = emptySet(),
    val accountIdentifier: String? = null,
    /** Inclusive floor, in minor units (paise). Matches the stored absolute amount. */
    val minAmountMinorUnits: Long? = null,
    /** Inclusive ceiling, in minor units (paise). */
    val maxAmountMinorUnits: Long? = null,
    val excludePromotional: Boolean = false,
    val includeIgnored: Boolean = false,
    val uncategorisedOnly: Boolean = false,
) {
    val isActive: Boolean
        get() = !searchQuery.isNullOrBlank() ||
            dateRange != null ||
            type != null ||
            source != null ||
            categoryIds.isNotEmpty() ||
            accountIdentifier != null ||
            minAmountMinorUnits != null ||
            maxAmountMinorUnits != null ||
            excludePromotional ||
            uncategorisedOnly

    companion object {
        val NONE = TransactionFilter()
    }
}
