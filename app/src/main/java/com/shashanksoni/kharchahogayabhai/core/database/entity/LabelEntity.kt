package com.shashanksoni.kharchahogayabhai.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "labels",
    indices = [Index(value = ["name"], unique = true)],
)
data class LabelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "color_hex")
    val colorHex: String,
    @ColumnInfo(name = "is_system_defined")
    val isSystemDefined: Boolean,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

@Entity(
    tableName = "transaction_labels",
    primaryKeys = ["transaction_id", "label_id"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["label_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["label_id"]),
    ],
)
data class TransactionLabelCrossRef(
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    @ColumnInfo(name = "label_id")
    val labelId: Long,
)
