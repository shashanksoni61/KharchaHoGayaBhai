package com.shashanksoni.kharchahogayabhai.domain.usecase

import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDateBounds
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReclassifyStoredSmsUseCaseTest {

    @Test
    fun hidesLimitAlertAndLeavesRealUpi() = runTest {
        val limit = transaction(
            id = 1L,
            description = "Your remaining credit limit for the card is 1,38,717 INR",
        )
        val upi = transaction(
            id = 2L,
            description = "INR 30.00 debited A/c no. XX2073 UPI/P2M/347484353597/STORE",
        )
        val repo = RecordingRepository(listOf(limit, upi))
        val useCase = ReclassifyStoredSmsUseCase(
            transactionRepository = repo,
            isNotAPayment = { body, _ -> body.contains("remaining credit limit") },
        )

        val result = useCase()

        assertEquals(2, result.scannedCount)
        assertEquals(1, result.hiddenCount)
        assertEquals(setOf(1L), repo.promotionalIds)
    }

    @Test
    fun prefersRawSmsBodyWhenPresent() = runTest {
        val tx = transaction(id = 7L, description = "AMAZON")
        val repo = RecordingRepository(
            transactions = listOf(tx),
            sources = listOf(
                TransactionSourceRecord(
                    id = 70L,
                    transactionId = 7L,
                    source = TransactionSource.SMS,
                    originLabel = "AX-AXISBK",
                    rawPayload = "EMI of Rs 4,500 is due on 02-10-26",
                    importedAt = Instant.parse("2026-09-17T09:44:00Z"),
                ),
            ),
        )
        var scoredBody: String? = null
        val useCase = ReclassifyStoredSmsUseCase(
            transactionRepository = repo,
            isNotAPayment = { body, address ->
                scoredBody = body
                assertEquals("AX-AXISBK", address)
                true
            },
        )

        useCase()

        assertEquals("EMI of Rs 4,500 is due on 02-10-26", scoredBody)
        assertEquals(setOf(7L), repo.promotionalIds)
    }

    @Test
    fun skipsBlankRows() = runTest {
        val tx = transaction(id = 3L, description = null, merchantName = null, bankName = null)
        val repo = RecordingRepository(listOf(tx))
        val useCase = ReclassifyStoredSmsUseCase(
            transactionRepository = repo,
            isNotAPayment = { _, _ -> true },
        )

        val result = useCase()

        assertEquals(1, result.scannedCount)
        assertEquals(0, result.hiddenCount)
        assertTrue(repo.promotionalIds.isEmpty())
    }

    private fun transaction(
        id: Long,
        description: String? = null,
        merchantName: String? = "Store",
        bankName: String? = "HDFC Bank",
    ) = Transaction(
        id = id,
        amount = Money.fromMajorUnits(BigDecimal("100.00")),
        type = TransactionType.DEBIT,
        transactionDate = Instant.parse("2026-09-17T09:44:00Z"),
        description = description,
        merchantName = merchantName,
        bankName = bankName,
        primarySource = TransactionSource.SMS,
        fingerprint = "v1:sms-$id",
        parseStatus = ParseStatus.PARSED,
        createdAt = Instant.parse("2026-09-17T09:44:00Z"),
        updatedAt = Instant.parse("2026-09-17T09:44:00Z"),
    )

    private class RecordingRepository(
        private val transactions: List<Transaction>,
        private val sources: List<TransactionSourceRecord> = emptyList(),
    ) : TransactionRepository {
        val promotionalIds = mutableSetOf<Long>()

        override suspend fun listVisibleSmsTransactions(): List<Transaction> = transactions

        override suspend fun listSmsSourceRecords(): List<TransactionSourceRecord> = sources

        override suspend fun setPromotional(transactionId: Long, promotional: Boolean) {
            if (promotional) promotionalIds += transactionId else promotionalIds -= transactionId
        }

        override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
            flowOf(emptyList())

        override fun observeTransactionDetail(transactionId: Long): Flow<TransactionDetail?> =
            error("unused")

        override fun observeSummaries(range: InstantRange): Flow<List<TransactionSummary>> =
            error("unused")

        override fun observeDateBounds(): Flow<TransactionDateBounds> = error("unused")

        override fun observeTransactionDates(): Flow<List<Instant>> = error("unused")

        override fun observeAccountIdentifiers(): Flow<List<String>> = error("unused")

        override suspend fun countTransactions(): Int = error("unused")

        override fun observeTransactionCount(): Flow<Int> = error("unused")

        override fun observeTransactionCountBySource(source: TransactionSource): Flow<Int> =
            error("unused")

        override suspend fun setCategory(transactionId: Long, categoryId: Long?) = error("unused")

        override suspend fun setIgnored(transactionId: Long, ignored: Boolean) = error("unused")
    }
}
