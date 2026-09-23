package com.shashanksoni.kharchahogayabhai.domain.repository

import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import kotlinx.coroutines.flow.Flow

/**
 * Read and edit access to stored transactions, free of any Room or Android type.
 *
 * Ingestion (importing from SMS, CSV, PDF or manual entry) is deliberately not
 * here: it needs deduplication and merging, and will arrive as its own
 * abstraction so that reading transactions stays this simple.
 */
interface TransactionRepository {

    fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>>

    /** Emits null once the transaction no longer exists. */
    fun observeTransactionDetail(transactionId: Long): Flow<TransactionDetail?>

    fun observeSummaries(range: InstantRange): Flow<List<TransactionSummary>>

    /** Distinct account tails currently present, for the account filter. */
    fun observeAccountIdentifiers(): Flow<List<String>>

    /** Total stored transactions, used for the overall "data from N messages" counter. */
    suspend fun countTransactions(): Int

    /** Passing null clears the category. */
    suspend fun setCategory(transactionId: Long, categoryId: Long?)
}
