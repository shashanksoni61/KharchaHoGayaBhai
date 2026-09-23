package com.shashanksoni.kharchahogayabhai.domain.usecase

import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.CategorySpending
import com.shashanksoni.kharchahogayabhai.domain.model.DailySpending
import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.MonthTotals
import com.shashanksoni.kharchahogayabhai.domain.model.MonthlyDashboard
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSummary
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Builds everything the monthly dashboard shows.
 *
 * The requested month and the one before it are read in a single query and split
 * in memory, so month-on-month comparison costs no extra database work. All
 * arithmetic happens on integral minor units.
 *
 * Income and expense totals follow [TransactionType], not category kind, so a
 * transaction with no category is still counted.
 */
class GetMonthlyDashboardUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    operator fun invoke(month: YearMonth): Flow<MonthlyDashboard> {
        val previousMonth = month.minusMonths(1)
        val range = InstantRange.ofMonths(from = previousMonth, to = month, zone = zone)

        return combine(
            transactionRepository.observeSummaries(range),
            transactionRepository.observeTransactions(
                TransactionFilter(dateRange = InstantRange.ofMonth(month, zone)),
            ),
            categoryRepository.observeCategories(),
        ) { summaries, monthTransactions, categories ->
            buildDashboard(month, previousMonth, summaries, monthTransactions, categories)
        }
    }

    private fun buildDashboard(
        month: YearMonth,
        previousMonth: YearMonth,
        summaries: List<TransactionSummary>,
        monthTransactions: List<Transaction>,
        categories: List<Category>,
    ): MonthlyDashboard {
        val byMonth = summaries.groupBy { YearMonth.from(it.transactionDate.atZone(zone)) }
        val currentMonthSummaries = byMonth[month].orEmpty()
        val previousMonthSummaries = byMonth[previousMonth].orEmpty()
        val currencyCode = currentMonthSummaries.firstOrNull()?.amount?.currencyCode
            ?: Money.DEFAULT_CURRENCY_CODE

        val totals = totalsOf(currentMonthSummaries, currencyCode)

        return MonthlyDashboard(
            month = month,
            totals = totals,
            previousMonth = previousMonth,
            previousMonthTotals = previousMonthSummaries
                .takeIf { it.isNotEmpty() }
                ?.let { totalsOf(it, currencyCode) },
            categoryBreakdown = categoryBreakdownOf(
                expenses = currentMonthSummaries.filter { it.type == TransactionType.DEBIT },
                categories = categories,
                totalExpenses = totals.expenses,
                currencyCode = currencyCode,
            ),
            dailySpending = dailySpendingOf(
                expenses = currentMonthSummaries.filter { it.type == TransactionType.DEBIT },
                month = month,
                currencyCode = currencyCode,
            ),
            recentTransactions = monthTransactions.take(RECENT_TRANSACTION_LIMIT),
            categories = categories,
        )
    }

    private fun totalsOf(summaries: List<TransactionSummary>, currencyCode: String): MonthTotals {
        var income = Money.zero(currencyCode)
        var expenses = Money.zero(currencyCode)
        var incomeCount = 0
        var expenseCount = 0

        summaries.forEach { summary ->
            when (summary.type) {
                TransactionType.CREDIT -> {
                    income += summary.amount
                    incomeCount++
                }

                TransactionType.DEBIT -> {
                    expenses += summary.amount
                    expenseCount++
                }
            }
        }

        return MonthTotals(
            income = income,
            expenses = expenses,
            transactionCount = summaries.size,
            incomeCount = incomeCount,
            expenseCount = expenseCount,
        )
    }

    private fun categoryBreakdownOf(
        expenses: List<TransactionSummary>,
        categories: List<Category>,
        totalExpenses: Money,
        currencyCode: String,
    ): List<CategorySpending> {
        val categoriesById = categories.associateBy { it.id }
        return expenses
            .groupBy { it.categoryId }
            .map { (categoryId, entries) ->
                val amount = Money.sum(entries.map { it.amount }, currencyCode)
                CategorySpending(
                    category = categoryId?.let(categoriesById::get),
                    amount = amount,
                    transactionCount = entries.size,
                    shareOfExpenses = amount.shareOf(totalExpenses),
                )
            }
            .sortedByDescending { it.amount.minorUnits }
    }

    /**
     * Every day of the month is present, including days with no spending, so the
     * chart reads as a calendar rather than a list of non-empty days.
     */
    private fun dailySpendingOf(
        expenses: List<TransactionSummary>,
        month: YearMonth,
        currencyCode: String,
    ): List<DailySpending> {
        val spentPerDay: Map<LocalDate, Money> = expenses
            .groupBy { it.transactionDate.atZone(zone).toLocalDate() }
            .mapValues { (_, entries) -> Money.sum(entries.map { it.amount }, currencyCode) }

        return (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            DailySpending(date = date, amount = spentPerDay[date] ?: Money.zero(currencyCode))
        }
    }

    private companion object {
        const val RECENT_TRANSACTION_LIMIT = 8
    }
}
