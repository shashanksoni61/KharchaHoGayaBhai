package com.shashanksoni.kharchahogayabhai.domain.repository

import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import kotlinx.coroutines.flow.Flow

interface ImportRepository {
    fun observeImportBatches(): Flow<List<ImportBatch>>
    suspend fun saveImportBatch(batch: ImportBatch): Long
}
