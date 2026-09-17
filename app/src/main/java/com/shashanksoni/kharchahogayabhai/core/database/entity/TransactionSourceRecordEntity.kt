package com.shashanksoni.kharchahogayabhai.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource

/**
 * Provenance for a transaction: one row per source that described it.
 *
 * This is what makes merging non-destructive. When a CSV import turns out to
 * duplicate an SMS transaction, we add a row here instead of dropping the CSV
 * data, and the detail screen can honestly say the payment was seen in both.
 *
 * `(source, source_identifier)` is unique, so re-importing the very same file or
 * re-processing the same SMS cannot create a second record. SQLite treats NULLs
 * as distinct, which is the behaviour we want for sources that cannot provide an
 * identifier.
 */
@Entity(
    tableName = "transaction_sources",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["source", "source_identifier"], unique = true),
    ],
)
data class TransactionSourceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    val source: TransactionSource,
    @ColumnInfo(name = "source_identifier")
    val sourceIdentifier: String?,
    @ColumnInfo(name = "origin_label")
    val originLabel: String?,
    /** Populated only for incompletely parsed input, so the user can review it. */
    @ColumnInfo(name = "raw_payload")
    val rawPayload: String?,
    @ColumnInfo(name = "imported_at")
    val importedAtMillis: Long,
)
