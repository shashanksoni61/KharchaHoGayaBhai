package com.shashanksoni.kharchahogayabhai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shashanksoni.kharchahogayabhai.core.database.entity.ImportBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBatchDao {

    @Query("SELECT * FROM import_batches ORDER BY imported_at DESC")
    fun observeBatches(): Flow<List<ImportBatchEntity>>

    @Insert
    suspend fun insertBatch(batch: ImportBatchEntity): Long

    @Query("DELETE FROM import_batches")
    suspend fun deleteAllBatches()
}
