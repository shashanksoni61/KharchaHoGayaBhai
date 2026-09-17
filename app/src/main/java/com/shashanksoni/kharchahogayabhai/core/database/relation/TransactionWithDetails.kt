package com.shashanksoni.kharchahogayabhai.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity

/** A transaction with its category and every source that contributed to it. */
data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "id", entityColumn = "transaction_id")
    val sources: List<TransactionSourceRecordEntity>,
)
