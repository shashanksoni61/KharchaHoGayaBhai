package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionSourceRecordEntity
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionSourceDao {

    @Query(
        """
        SELECT * FROM transaction_sources
        WHERE transaction_id = :transactionId
        ORDER BY imported_at ASC
        """,
    )
    fun observeSourcesOf(transactionId: Long): Flow<List<TransactionSourceRecordEntity>>

    /**
     * Ignores rows whose `(source, source_identifier)` is already present, which
     * makes re-importing the same file harmless.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSourceRecords(records: List<TransactionSourceRecordEntity>)

    /** Lets an import skip input it has already ingested before parsing it again. */
    @Query(
        """
        SELECT * FROM transaction_sources
        WHERE source = :source AND source_identifier = :sourceIdentifier
        LIMIT 1
        """,
    )
    suspend fun findBySourceIdentifier(
        source: TransactionSource,
        sourceIdentifier: String,
    ): TransactionSourceRecordEntity?
}
