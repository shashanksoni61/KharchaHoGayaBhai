package com.shashanksoni.kharchahogayabhai.core.database.projection

import androidx.room.ColumnInfo

data class TransactionDateBoundsProjection(
    @ColumnInfo(name = "min_date")
    val minDate: Long?,
    @ColumnInfo(name = "max_date")
    val maxDate: Long?,
)
