package com.shashanksoni.kharchahogayabhai.core.database.projection

import androidx.room.ColumnInfo
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType

/**
 * The few columns reporting needs.
 *
 * The dashboard reads one lean projection for the whole period and derives
 * totals, category split and the daily chart from it in a single pass, rather
 * than firing a separate aggregate query per figure.
 */
data class TransactionSummaryProjection(
    @ColumnInfo(name = "transaction_date")
    val transactionDateMillis: Long,
    val type: TransactionType,
    @ColumnInfo(name = "amount_minor_units")
    val amountMinorUnits: Long,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
)
