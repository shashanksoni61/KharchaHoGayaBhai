package com.shashanksoni.kharchahogayabhai.domain.repository

import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDateBounds
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import java.time.Instant
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

    /** Earliest and latest transaction times, used to build month filter chips. */
    fun observeDateBounds(): Flow<TransactionDateBounds>

    /** Stored transaction instants, so month chips only appear when that month has rows. */
    fun observeTransactionDates(): Flow<List<Instant>>

    /** Distinct account tails currently present, for the account filter. */
    fun observeAccountIdentifiers(): Flow<List<String>>

    /** Total stored transactions, used for the overall "data from N messages" counter. */
    suspend fun countTransactions(): Int

    /** Live total transaction count. */
    fun observeTransactionCount(): Flow<Int>

    /** Live count of transactions that have [source] as a contributing source. */
    fun observeTransactionCountBySource(source: TransactionSource): Flow<Int>

    /** Passing null clears the category. */
    suspend fun setCategory(transactionId: Long, categoryId: Long?)

    /** Hide or restore a row the user marked as not a payment. */
    suspend fun setIgnored(transactionId: Long, ignored: Boolean)

    /** Visible SMS-backed rows the on-device model can re-score. */
    suspend fun listVisibleSmsTransactions(): List<Transaction>

    /** Every stored SMS provenance row, used to recover sender + leftover body text. */
    suspend fun listSmsSourceRecords(): List<TransactionSourceRecord>

    /** Mark or clear a not-a-payment classification. */
    suspend fun setPromotional(transactionId: Long, promotional: Boolean)
}
