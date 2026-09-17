package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionEntity
import com.shashanksoni.kharchahogayabhai.core.database.projection.TransactionSummaryProjection
import com.shashanksoni.kharchahogayabhai.core.database.relation.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * One query behind every list filter. Each condition is skipped when its
     * parameter is null, so search, date, type, source, category and account
     * filters compose without building SQL by hand.
     *
     * [categoryIds] must never be empty (`IN ()` is invalid SQL); callers pass a
     * placeholder and leave [filterByCategory] at 0 when no category filter is
     * active.
     *
     * The source condition matches any *contributing* source, so filtering by CSV
     * still finds a transaction that arrived over SMS and was later confirmed by a
     * CSV import.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:startMillis IS NULL OR transaction_date >= :startMillis)
          AND (:endMillisExclusive IS NULL OR transaction_date < :endMillisExclusive)
          AND (:type IS NULL OR type = :type)
          AND (:accountIdentifier IS NULL OR account_identifier = :accountIdentifier)
          AND (:filterByCategory = 0 OR category_id IN (:categoryIds))
          AND (
            :searchQuery IS NULL
            OR merchant_name LIKE '%' || :searchQuery || '%'
            OR description LIKE '%' || :searchQuery || '%'
            OR notes LIKE '%' || :searchQuery || '%'
            OR reference_number LIKE '%' || :searchQuery || '%'
            OR bank_name LIKE '%' || :searchQuery || '%'
          )
          AND (
            :source IS NULL
            OR EXISTS (
              SELECT 1 FROM transaction_sources
              WHERE transaction_sources.transaction_id = transactions.id
                AND transaction_sources.source = :source
            )
          )
        ORDER BY transaction_date DESC, id DESC
        """,
    )
    fun observeTransactions(
        startMillis: Long?,
        endMillisExclusive: Long?,
        type: String?,
        source: String?,
        accountIdentifier: String?,
        filterByCategory: Int,
        categoryIds: List<Long>,
        searchQuery: String?,
    ): Flow<List<TransactionEntity>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun observeTransactionWithDetails(transactionId: Long): Flow<TransactionWithDetails?>

    @Query(
        """
        SELECT transaction_date, type, amount_minor_units, currency_code, category_id
        FROM transactions
        WHERE transaction_date >= :startMillis AND transaction_date < :endMillisExclusive
        ORDER BY transaction_date ASC
        """,
    )
    fun observeSummaries(
        startMillis: Long,
        endMillisExclusive: Long,
    ): Flow<List<TransactionSummaryProjection>>

    /** Feeds the account filter without a separate accounts table. */
    @Query(
        """
        SELECT DISTINCT account_identifier FROM transactions
        WHERE account_identifier IS NOT NULL
        ORDER BY account_identifier ASC
        """,
    )
    fun observeAccountIdentifiers(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    /** Level 3 duplicate check: exact identity match. */
    @Query("SELECT * FROM transactions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): TransactionEntity?

    /**
     * Level 1 duplicate check: candidates sharing a bank reference. Returns a list
     * because a reference can legitimately appear on both legs of a transfer; the
     * caller decides which candidate, if any, is the same transaction.
     */
    @Query("SELECT * FROM transactions WHERE normalized_reference = :normalizedReference")
    suspend fun findByNormalizedReference(normalizedReference: String): List<TransactionEntity>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET category_id = :categoryId, updated_at = :updatedAtMillis WHERE id = :transactionId")
    suspend fun updateCategory(transactionId: Long, categoryId: Long?, updatedAtMillis: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
