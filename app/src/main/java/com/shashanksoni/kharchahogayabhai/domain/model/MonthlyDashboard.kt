package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.LocalDate
import java.time.YearMonth

/** Headline figures for one calendar month. */
data class MonthTotals(
    val income: Money,
    val expenses: Money,
    val transactionCount: Int,
    val incomeCount: Int,
    val expenseCount: Int,
) {
    val net: Money get() = income - expenses

    companion object {
        fun empty(currencyCode: String = Money.DEFAULT_CURRENCY_CODE) = MonthTotals(
            income = Money.zero(currencyCode),
            expenses = Money.zero(currencyCode),
            transactionCount = 0,
            incomeCount = 0,
            expenseCount = 0,
        )
    }
}

/** Spending in one category, with its share of the month's expenses. */
data class CategorySpending(
    /** Null for transactions that have not been categorised yet. */
    val category: Category?,
    val amount: Money,
    val transactionCount: Int,
    val shareOfExpenses: Float,
)

/** Expense total for a single day, used by the month chart. */
data class DailySpending(
    val date: LocalDate,
    val amount: Money,
)

data class MonthlyDashboard(
    val month: YearMonth,
    val totals: MonthTotals,
    /** Same figures for the preceding month; null when there is no data to compare. */
    val previousMonth: YearMonth,
    val previousMonthTotals: MonthTotals?,
    val categoryBreakdown: List<CategorySpending>,
    val dailySpending: List<DailySpending>,
) {
    val hasData: Boolean get() = totals.transactionCount > 0

    /** Change in expenses against the previous month, as a fraction; null when incomparable. */
    val expenseChangeRatio: Float?
        get() {
            val previous = previousMonthTotals?.expenses ?: return null
            if (previous.isZero) return null
            return (totals.expenses.minorUnits - previous.minorUnits).toFloat() /
                previous.minorUnits.toFloat()
        }
}
