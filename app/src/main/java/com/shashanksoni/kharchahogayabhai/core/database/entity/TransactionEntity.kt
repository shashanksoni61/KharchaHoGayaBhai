package com.shashanksoni.kharchahogayabhai.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType

/**
 * One deduplicated transaction. Contributing sources live in
 * [TransactionSourceRecordEntity], one row per source.
 *
 * Storage decisions:
 * - Amounts are integral minor units plus a currency code, so no floating point
 *   ever reaches the database.
 * - Timestamps are epoch milliseconds (UTC), converted to `Instant` at the
 *   mapper boundary. Room stays free of type converters this way.
 * - [fingerprint] is uniquely indexed, so the store itself refuses to hold the
 *   same transaction twice even if a caller forgets to deduplicate.
 * - [normalizedReference] is indexed but *not* unique. It is the fast path for
 *   reference-based matching, while whether two rows sharing a reference are
 *   truly the same transaction is a decision for the deduplication engine, not a
 *   constraint that should crash an import.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["normalized_reference"]),
        Index(value = ["transaction_date"]),
        Index(value = ["category_id"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "amount_minor_units")
    val amountMinorUnits: Long,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    val type: TransactionType,
    @ColumnInfo(name = "transaction_date")
    val transactionDateMillis: Long,
    val description: String?,
    @ColumnInfo(name = "merchant_name")
    val merchantName: String?,
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String?,
    @ColumnInfo(name = "normalized_reference")
    val normalizedReference: String?,
    @ColumnInfo(name = "account_identifier")
    val accountIdentifier: String?,
    @ColumnInfo(name = "bank_name")
    val bankName: String?,
    @ColumnInfo(name = "payment_method")
    val paymentMethod: PaymentMethod,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    @ColumnInfo(name = "primary_source")
    val primarySource: TransactionSource,
    val fingerprint: String,
    @ColumnInfo(name = "parse_status")
    val parseStatus: ParseStatus,
    @ColumnInfo(name = "is_promotional", defaultValue = "0")
    val isPromotional: Boolean = false,
    @ColumnInfo(name = "is_ignored", defaultValue = "0")
    val isIgnored: Boolean = false,
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtMillis: Long,
)
