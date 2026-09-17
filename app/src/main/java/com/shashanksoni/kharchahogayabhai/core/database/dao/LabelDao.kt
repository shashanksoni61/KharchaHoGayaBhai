package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shashanksoni.kharchahogayabhai.core.database.entity.LabelEntity
import com.shashanksoni.kharchahogayabhai.core.database.entity.TransactionLabelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Query("SELECT * FROM labels ORDER BY sort_order ASC, name ASC")
    fun observeLabels(): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT labels.* FROM labels
        INNER JOIN transaction_labels ON labels.id = transaction_labels.label_id
        WHERE transaction_labels.transaction_id = :transactionId
        ORDER BY labels.sort_order ASC, labels.name ASC
        """,
    )
    fun observeLabelsForTransaction(transactionId: Long): Flow<List<LabelEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingLabels(labels: List<LabelEntity>)

    /** Replaces built-in tags when the app ships a new lifestyle set. Custom tags stay. */
    @Query("DELETE FROM labels WHERE is_system_defined = 1")
    suspend fun deleteSystemLabels()

    @Insert
    suspend fun insertLabel(label: LabelEntity): Long

    @Query("DELETE FROM labels WHERE id = :labelId AND is_system_defined = 0")
    suspend fun deleteCustomLabel(labelId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun attachLabel(crossRef: TransactionLabelCrossRef)

    @Query(
        """
        DELETE FROM transaction_labels
        WHERE transaction_id = :transactionId AND label_id = :labelId
        """,
    )
    suspend fun detachLabel(transactionId: Long, labelId: Long)

    @Query("SELECT COUNT(*) FROM labels WHERE LOWER(name) = LOWER(:name)")
    suspend fun countByName(name: String): Int
}
