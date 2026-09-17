package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant

/**
 * The minimum a transaction contributes to a report: when, which direction, how
 * much, and under which category. Reporting reads these instead of whole
 * transactions.
 */
data class TransactionSummary(
    val transactionDate: Instant,
    val type: TransactionType,
    val amount: Money,
    val categoryId: Long?,
)
