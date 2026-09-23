package com.shashanksoni.kharchahogayabhai.data.repository

import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toDomain
import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDateBounds
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val transactionSourceDao: TransactionSourceDao,
    private val clock: Clock = Clock.systemUTC(),
) : TransactionRepository {

    override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.observeTransactions(
            startMillis = filter.dateRange?.start?.toEpochMilli(),
            endMillisExclusive = filter.dateRange?.endExclusive?.toEpochMilli(),
            type = filter.type?.name,
            source = filter.source?.name,
            accountIdentifier = filter.accountIdentifier,
            filterByCategory = if (filter.uncategorisedOnly || filter.categoryIds.isEmpty()) 0 else 1,
            // `IN ()` is not valid SQL, so an inactive category filter still has to
            // bind one value; this id can never exist.
            categoryIds = filter.categoryIds.takeIf { it.isNotEmpty() }?.toList()
                ?: listOf(UNMATCHABLE_CATEGORY_ID),
            searchQuery = filter.searchQuery?.trim()?.takeIf { it.isNotEmpty() },
            minAmountMinor = filter.minAmountMinorUnits,
            maxAmountMinor = filter.maxAmountMinorUnits,
            excludePromotional = if (filter.excludePromotional) 1 else 0,
            includeIgnored = if (filter.includeIgnored) 1 else 0,
            uncategorisedOnly = if (filter.uncategorisedOnly) 1 else 0,
        ).map { entities -> entities.map { it.toDomain() } }

    override fun observeTransactionDetail(transactionId: Long): Flow<TransactionDetail?> =
        transactionDao.observeTransactionWithDetails(transactionId)
            .map { it?.toDomain() }

    override fun observeSummaries(range: InstantRange): Flow<List<TransactionSummary>> =
        transactionDao.observeSummaries(
            startMillis = range.start.toEpochMilli(),
            endMillisExclusive = range.endExclusive.toEpochMilli(),
        ).map { projections -> projections.map { it.toDomain() } }

    override fun observeDateBounds(): Flow<TransactionDateBounds> =
        transactionDao.observeDateBounds().map { bounds ->
            TransactionDateBounds(
                earliest = bounds.minDate?.let(Instant::ofEpochMilli),
                latest = bounds.maxDate?.let(Instant::ofEpochMilli),
            )
        }

    override fun observeTransactionDates(): Flow<List<Instant>> =
        transactionDao.observeTransactionDates().map { millis ->
            millis.map(Instant::ofEpochMilli)
        }

    override fun observeAccountIdentifiers(): Flow<List<String>> =
        transactionDao.observeAccountIdentifiers()

    override suspend fun countTransactions(): Int = transactionDao.countTransactions()

    override fun observeTransactionCount(): Flow<Int> =
        transactionDao.observeTransactionCount()

    override fun observeTransactionCountBySource(source: TransactionSource): Flow<Int> =
        transactionDao.observeTransactionCountBySource(source.name)

    override suspend fun setCategory(transactionId: Long, categoryId: Long?) {
        transactionDao.updateCategory(
            transactionId = transactionId,
            categoryId = categoryId,
            updatedAtMillis = clock.millis(),
        )
    }

    override suspend fun setIgnored(transactionId: Long, ignored: Boolean) {
        transactionDao.updateIgnored(
            transactionId = transactionId,
            ignored = ignored,
            updatedAtMillis = clock.millis(),
        )
    }

    override suspend fun listVisibleSmsTransactions(): List<Transaction> =
        transactionDao.findVisibleSmsTransactions().map { it.toDomain() }

    override suspend fun listSmsSourceRecords(): List<TransactionSourceRecord> =
        transactionSourceDao.findSourcesBySource(TransactionSource.SMS).map { it.toDomain() }

    override suspend fun setPromotional(transactionId: Long, promotional: Boolean) {
        transactionDao.updatePromotional(
            transactionId = transactionId,
            promotional = promotional,
            updatedAtMillis = clock.millis(),
        )
    }

    private companion object {
        const val UNMATCHABLE_CATEGORY_ID = -1L
    }
}
