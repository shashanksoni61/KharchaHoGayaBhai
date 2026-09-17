package com.shashanksoni.kharchahogayabhai.domain.model

/**
 * How a category behaves in reports.
 *
 * Income vs expense totals are derived from [TransactionType], not from this;
 * [CategoryKind] drives suggestion and grouping only. [TRANSFER] marks
 * categories that move money without consuming it (self transfers, cash
 * withdrawals, investments), which reporting can exclude later so the same
 * rupee is not counted as spending twice.
 */
enum class CategoryKind {
    EXPENSE,
    INCOME,
    TRANSFER,
}

data class Category(
    val id: Long,
    val name: String,
    val kind: CategoryKind,
    /** `RRGGBB` hex, resolved to a colour by the UI layer. */
    val colorHex: String,
    /** Stable key mapped to an icon by the UI layer; keeps drawables out of the domain. */
    val iconKey: String,
    /** System categories cannot be deleted by the user, only hidden or renamed. */
    val isSystemDefined: Boolean,
    val sortOrder: Int,
)

/**
 * The categories the app ships with.
 *
 * Ids are fixed and permanent: rule-based categorisation, tests and user data
 * all reference them, so they must never be renumbered. New categories take the
 * next free id.
 */
object DefaultCategories {
    const val FOOD = 1L
    const val SHOPPING = 2L
    const val TRANSPORT = 3L
    const val BILLS = 4L
    const val ENTERTAINMENT = 5L
    const val TRAVEL = 6L
    const val HEALTHCARE = 7L
    const val EDUCATION = 8L
    const val RENT = 9L
    const val SALARY = 10L
    const val INVESTMENT = 11L
    const val TRANSFER = 12L
    const val CASH_WITHDRAWAL = 13L
    const val OTHER = 14L

    val all: List<Category> = listOf(
        Category(FOOD, "Food", CategoryKind.EXPENSE, "FF7043", "food", true, 1),
        Category(SHOPPING, "Shopping", CategoryKind.EXPENSE, "AB47BC", "shopping", true, 2),
        Category(TRANSPORT, "Transport", CategoryKind.EXPENSE, "42A5F5", "transport", true, 3),
        Category(BILLS, "Bills", CategoryKind.EXPENSE, "26A69A", "bills", true, 4),
        Category(ENTERTAINMENT, "Entertainment", CategoryKind.EXPENSE, "EC407A", "entertainment", true, 5),
        Category(TRAVEL, "Travel", CategoryKind.EXPENSE, "29B6F6", "travel", true, 6),
        Category(HEALTHCARE, "Healthcare", CategoryKind.EXPENSE, "EF5350", "healthcare", true, 7),
        Category(EDUCATION, "Education", CategoryKind.EXPENSE, "5C6BC0", "education", true, 8),
        Category(RENT, "Rent", CategoryKind.EXPENSE, "8D6E63", "rent", true, 9),
        Category(SALARY, "Salary", CategoryKind.INCOME, "66BB6A", "salary", true, 10),
        Category(INVESTMENT, "Investment", CategoryKind.TRANSFER, "9CCC65", "investment", true, 11),
        Category(TRANSFER, "Transfer", CategoryKind.TRANSFER, "78909C", "transfer", true, 12),
        Category(CASH_WITHDRAWAL, "Cash Withdrawal", CategoryKind.TRANSFER, "BDBDBD", "cash", true, 13),
        Category(OTHER, "Other", CategoryKind.EXPENSE, "90A4AE", "other", true, 14),
    )
}
