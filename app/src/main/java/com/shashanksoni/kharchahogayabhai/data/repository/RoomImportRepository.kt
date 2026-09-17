package com.shashanksoni.kharchahogayabhai.data.repository

import com.shashanksoni.kharchahogayabhai.core.database.dao.ImportBatchDao
import com.shashanksoni.kharchahogayabhai.core.database.entity.ImportBatchEntity
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import com.shashanksoni.kharchahogayabhai.domain.repository.ImportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomImportRepository(
    private val importBatchDao: ImportBatchDao,
) : ImportRepository {

    override fun observeImportBatches(): Flow<List<ImportBatch>> =
        importBatchDao.observeBatches().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveImportBatch(batch: ImportBatch): Long =
        importBatchDao.insertBatch(batch.toEntity())
}

private fun ImportBatchEntity.toDomain() = ImportBatch(
    id = id,
    fileName = fileName,
    source = source,
    importedAt = Instant.ofEpochMilli(importedAtMillis),
    totalParsed = totalParsed,
    newCount = newCount,
    mergedCount = mergedCount,
    duplicateCount = duplicateCount,
    failedCount = failedCount,
    status = status,
    errorMessage = errorMessage,
)

private fun ImportBatch.toEntity() = ImportBatchEntity(
    id = id,
    fileName = fileName,
    source = source,
    importedAtMillis = importedAt.toEpochMilli(),
    totalParsed = totalParsed,
    newCount = newCount,
    mergedCount = mergedCount,
    duplicateCount = duplicateCount,
    failedCount = failedCount,
    status = status,
    errorMessage = errorMessage,
)
