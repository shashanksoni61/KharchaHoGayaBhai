package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant

/** Earliest and latest stored transaction instants, for month filter chips. */
data class TransactionDateBounds(
    val earliest: Instant?,
    val latest: Instant?,
) {
    companion object {
        val EMPTY = TransactionDateBounds(earliest = null, latest = null)
    }
}
