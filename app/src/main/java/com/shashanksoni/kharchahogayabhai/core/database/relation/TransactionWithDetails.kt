package com.shashanksoni.kharchahogayabhai.core.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.shashanksoni.kharchahogayabhai.core.database.entity.CategoryEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.LabelEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionLabelCrossRef
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity

/** A transaction with its category, sources, and flags/labels. */
data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "id", entityColumn = "transaction_id")
    val sources: List<TransactionSourceRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TransactionLabelCrossRef::class,
            parentColumn = "transaction_id",
            entityColumn = "label_id",
        ),
    )
    val labels: List<LabelEntity>,
)
