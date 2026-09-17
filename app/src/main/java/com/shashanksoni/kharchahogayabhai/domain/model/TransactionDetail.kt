package com.shashanksoni.kharchahogayabhai.domain.model

/** A transaction together with everything the detail screen needs to show. */
data class TransactionDetail(
    val transaction: Transaction,
    val category: Category?,
    val sources: List<TransactionSourceRecord>,
) {
    val contributingSources: Set<TransactionSource>
        get() = sources.mapTo(linkedSetOf()) { it.source }
}
