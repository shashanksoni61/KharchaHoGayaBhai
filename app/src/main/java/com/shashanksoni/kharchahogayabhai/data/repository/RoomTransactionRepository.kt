package com.shashanksoni.kharchahogayabhai.data.repository

import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toDomain
import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val clock: Clock = Clock.systemUTC(),
) : TransactionRepository {

    override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.observeTransactions(
            startMillis = filter.dateRange?.start?.toEpochMilli(),
            endMillisExclusive = filter.dateRange?.endExclusive?.toEpochMilli(),
            type = filter.type?.name,
            source = filter.source?.name,
            accountIdentifier = filter.accountIdentifier,
            filterByCategory = if (filter.categoryIds.isEmpty()) 0 else 1,
            // `IN ()` is not valid SQL, so an inactive category filter still has to
            // bind one value; this id can never exist.
            categoryIds = filter.categoryIds.takeIf { it.isNotEmpty() }?.toList()
                ?: listOf(UNMATCHABLE_CATEGORY_ID),
            searchQuery = filter.searchQuery?.trim()?.takeIf { it.isNotEmpty() },
        ).map { entities -> entities.map { it.toDomain() } }

    override fun observeTransactionDetail(transactionId: Long): Flow<TransactionDetail?> =
        transactionDao.observeTransactionWithDetails(transactionId)
            .map { it?.toDomain() }

    override fun observeSummaries(range: InstantRange): Flow<List<TransactionSummary>> =
        transactionDao.observeSummaries(
            startMillis = range.start.toEpochMilli(),
            endMillisExclusive = range.endExclusive.toEpochMilli(),
        ).map { projections -> projections.map { it.toDomain() } }

    override fun observeAccountIdentifiers(): Flow<List<String>> =
        transactionDao.observeAccountIdentifiers()

    override suspend fun countTransactions(): Int = transactionDao.countTransactions()

    override suspend fun setCategory(transactionId: Long, categoryId: Long?) {
        transactionDao.updateCategory(
            transactionId = transactionId,
            categoryId = categoryId,
            updatedAtMillis = clock.millis(),
        )
    }

    private companion object {
        const val UNMATCHABLE_CATEGORY_ID = -1L
    }
}
