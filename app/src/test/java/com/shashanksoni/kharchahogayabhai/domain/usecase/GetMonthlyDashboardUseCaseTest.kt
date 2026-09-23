package com.shashanksoni.kharchahogayabhai.domain.usecase

import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.DefaultCategories
import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class GetMonthlyDashboardUseCaseTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val september: YearMonth = YearMonth.of(2026, 9)
    private val august: YearMonth = YearMonth.of(2026, 8)

    @Test
    fun `totals separate income from expenses and count both`() = runTest {
        val dashboard = dashboardOf(
            expense(day = 17, amountMajor = "540.00", categoryId = DefaultCategories.FOOD),
            expense(day = 17, amountMajor = "1299.00", categoryId = DefaultCategories.SHOPPING),
            income(day = 1, amountMajor = "95000.00", categoryId = DefaultCategories.SALARY),
        )

        assertEquals(Money.fromMajorUnits(BigDecimal("95000.00")), dashboard.totals.income)
        assertEquals(Money.fromMajorUnits(BigDecimal("1839.00")), dashboard.totals.expenses)
        assertEquals(Money.fromMajorUnits(BigDecimal("93161.00")), dashboard.totals.net)
        assertEquals(3, dashboard.totals.transactionCount)
        assertEquals(1, dashboard.totals.incomeCount)
        assertEquals(2, dashboard.totals.expenseCount)
    }

    @Test
    fun `the previous month is reported separately for comparison`() = runTest {
        val dashboard = dashboardOf(
            expense(day = 17, amountMajor = "1000.00", categoryId = DefaultCategories.FOOD),
            expense(day = 10, amountMajor = "800.00", categoryId = DefaultCategories.FOOD, month = august),
        )

        assertEquals(Money.fromMajorUnits(BigDecimal("1000.00")), dashboard.totals.expenses)
        assertEquals(
            Money.fromMajorUnits(BigDecimal("800.00")),
            dashboard.previousMonthTotals?.expenses,
        )
        assertEquals(august, dashboard.previousMonth)
        assertEquals(0.25f, dashboard.expenseChangeRatio!!, 0.0001f)
    }

    @Test
    fun `no previous month data means nothing to compare against`() = runTest {
        val dashboard = dashboardOf(
            expense(day = 17, amountMajor = "1000.00", categoryId = DefaultCategories.FOOD),
        )

        assertNull(dashboard.previousMonthTotals)
        assertNull(dashboard.expenseChangeRatio)
    }

    @Test
    fun `category spending is grouped, sorted by amount and shares add up`() = runTest {
        val dashboard = dashboardOf(
            expense(day = 2, amountMajor = "300.00", categoryId = DefaultCategories.FOOD),
            expense(day = 4, amountMajor = "200.00", categoryId = DefaultCategories.FOOD),
            expense(day = 6, amountMajor = "1500.00", categoryId = DefaultCategories.SHOPPING),
        )

        val breakdown = dashboard.categoryBreakdown
        assertEquals(2, breakdown.size)
        assertEquals(DefaultCategories.SHOPPING, breakdown.first().category?.id)
        assertEquals(Money.fromMajorUnits(BigDecimal("1500.00")), breakdown.first().amount)
        assertEquals(DefaultCategories.FOOD, breakdown.last().category?.id)
        assertEquals(2, breakdown.last().transactionCount)
        assertEquals(0.75f, breakdown.first().shareOfExpenses, 0.0001f)
        assertEquals(0.25f, breakdown.last().shareOfExpenses, 0.0001f)
    }

    @Test
    fun `uncategorised spending is reported rather than dropped`() = runTest {
        val dashboard = dashboardOf(
            expense(day = 3, amountMajor = "620.00", categoryId = null),
        )

        assertEquals(1, dashboard.categoryBreakdown.size)
        assertNull(dashboard.categoryBreakdown.single().category)
        assertEquals(
            Money.fromMajorUnits(BigDecimal("620.00")),
            dashboard.categoryBreakdown.single().amount,
        )
    }

    @Test
    fun `income is excluded from the category breakdown`() = runTest {
        val dashboard = dashboardOf(
            income(day = 1, amountMajor = "95000.00", categoryId = DefaultCategories.SALARY),
        )

        assertEquals(0, dashboard.categoryBreakdown.size)
    }

    @Test
    fun `daily spending covers every day of the month`() = runTest {
        val dashboard = dashboardOf(
            expense(day = 17, amountMajor = "540.00", categoryId = DefaultCategories.FOOD),
            expense(day = 17, amountMajor = "460.00", categoryId = DefaultCategories.FOOD),
        )

        assertEquals(30, dashboard.dailySpending.size)
        val seventeenth = dashboard.dailySpending.single { it.date == september.atDay(17) }
        assertEquals(Money.fromMajorUnits(BigDecimal("1000.00")), seventeenth.amount)
        val sixteenth = dashboard.dailySpending.single { it.date == september.atDay(16) }
        assertEquals(Money.zero(), sixteenth.amount)
    }

    @Test
    fun `a month with no transactions reports empty totals`() = runTest {
        val dashboard = dashboardOf()

        assertEquals(0, dashboard.totals.transactionCount)
        assertEquals(Money.zero(), dashboard.totals.expenses)
        assertEquals(false, dashboard.hasData)
    }

    private suspend fun dashboardOf(vararg summaries: TransactionSummary) =
        GetMonthlyDashboardUseCase(
            transactionRepository = FakeTransactionRepository(summaries.toList()),
            categoryRepository = FakeCategoryRepository(DefaultCategories.all),
            zone = zone,
        ).invoke(september).first()

    private fun expense(
        day: Int,
        amountMajor: String,
        categoryId: Long?,
        month: YearMonth = september,
    ) = TransactionSummary(
        transactionDate = instantOf(month, day),
        type = TransactionType.DEBIT,
        amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
        categoryId = categoryId,
    )

    private fun income(
        day: Int,
        amountMajor: String,
        categoryId: Long?,
        month: YearMonth = september,
    ) = TransactionSummary(
        transactionDate = instantOf(month, day),
        type = TransactionType.CREDIT,
        amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
        categoryId = categoryId,
    )

    private fun instantOf(month: YearMonth, day: Int): Instant =
        LocalDate.of(month.year, month.month, day).atTime(LocalTime.NOON).atZone(zone).toInstant()

    private class FakeTransactionRepository(
        private val summaries: List<TransactionSummary>,
    ) : TransactionRepository {

        override fun observeSummaries(range: InstantRange): Flow<List<TransactionSummary>> =
            flowOf(summaries.filter { it.transactionDate in range })

        override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
            error("Not needed by the dashboard")

        override fun observeTransactionDetail(transactionId: Long): Flow<TransactionDetail?> =
            error("Not needed by the dashboard")

        override fun observeAccountIdentifiers(): Flow<List<String>> =
            error("Not needed by the dashboard")

        override suspend fun countTransactions(): Int =
            error("Not needed by the dashboard")

        override suspend fun setCategory(transactionId: Long, categoryId: Long?) =
            error("Not needed by the dashboard")
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
        override suspend fun ensureDefaultCategoriesExist() = Unit
    }
}
