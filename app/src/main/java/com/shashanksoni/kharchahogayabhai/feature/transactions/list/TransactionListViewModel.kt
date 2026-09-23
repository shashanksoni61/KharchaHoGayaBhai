package com.shashanksoni.kharchahogayabhai.feature.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDateBounds
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A transaction paired with the category it belongs to, ready to render. */
data class TransactionEntry(
    val transaction: Transaction,
    val category: Category?,
)

/** Transactions of one day under a heading such as `Today` or `17 September`. */
data class TransactionDayGroup(
    val date: LocalDate,
    val header: String,
    val entries: List<TransactionEntry>,
    val creditTotal: Money,
    val debitTotal: Money,
    val uncategorisedCount: Int,
) {
    val net: Money get() = creditTotal - debitTotal
    val transactionCount: Int get() = entries.size
}

/** A calendar month wrapping its day accordions. Latest month is listed first. */
data class TransactionMonthGroup(
    val month: YearMonth,
    val header: String,
    val dayGroups: List<TransactionDayGroup>,
    val creditTotal: Money,
    val debitTotal: Money,
) {
    val transactionCount: Int get() = dayGroups.sumOf { it.transactionCount }
}

/** How rows inside one day accordion are ordered. Latest-first is the default. */
enum class DayAmountSort {
    LATEST,
    AMOUNT_HIGH_TO_LOW,
    AMOUNT_LOW_TO_HIGH,
}

data class TransactionListUiState(
    val monthGroups: List<TransactionMonthGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableMonths: List<YearMonth> = emptyList(),
    val selectedMonth: YearMonth? = null,
    val collapsedMonths: Set<YearMonth> = emptySet(),
    val collapsedDates: Set<LocalDate> = emptySet(),
    val daySorts: Map<LocalDate, DayAmountSort> = emptyMap(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedSource: TransactionSource? = null,
    val isLoading: Boolean = true,
) {
    val transactionCount: Int get() = monthGroups.sumOf { it.transactionCount }
    val isEmpty: Boolean get() = !isLoading && monthGroups.isEmpty()

    fun isMonthCollapsed(month: YearMonth): Boolean = month in collapsedMonths

    fun isCollapsed(date: LocalDate): Boolean = date in collapsedDates

    fun sortOf(date: LocalDate): DayAmountSort = daySorts[date] ?: DayAmountSort.LATEST
}

/**
 * Owns the transaction list's filters and turns stored transactions into
 * month- then day-grouped rows, so the screen only lays out what it is given.
 */
class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val zone: ZoneId,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val currentMonth: YearMonth = YearMonth.now(clock.withZone(zone))
    private val selectedMonth = MutableStateFlow<YearMonth?>(currentMonth)
    private val filter = MutableStateFlow(
        TransactionFilter(dateRange = InstantRange.ofMonth(currentMonth, zone)),
    )
    private val collapsedMonths = MutableStateFlow<Set<YearMonth>>(emptySet())
    private val collapsedDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val daySorts = MutableStateFlow<Map<LocalDate, DayAmountSort>>(emptyMap())

    private val availableMonths: StateFlow<List<YearMonth>> =
        transactionRepository.observeDateBounds()
            .map { bounds -> monthsCoveredBy(bounds) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = listOf(currentMonth),
            )

    init {
        viewModelScope.launch {
            availableMonths.collect { months ->
                val selected = selectedMonth.value
                if (selected != null && months.isNotEmpty() && selected !in months) {
                    selectMonth(months.first())
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListUiState> = combine(
        combine(
            filter,
            selectedMonth,
            availableMonths,
        ) { activeFilter, month, months ->
            Triple(activeFilter, month, months)
        },
        filter.flatMapLatest(transactionRepository::observeTransactions),
        categoryRepository.observeCategories(),
        combine(collapsedMonths, collapsedDates, daySorts) { months, dates, sorts ->
            Triple(months, dates, sorts)
        },
    ) { filterState, transactions, categories, grouping ->
        val (activeFilter, month, months) = filterState
        val (collapsedMonthSet, collapsedDateSet, sorts) = grouping
        val categoriesById = categories.associateBy { it.id }
        TransactionListUiState(
            monthGroups = groupByMonth(transactions, categoriesById, sorts),
            categories = categories,
            availableMonths = months,
            selectedMonth = month,
            collapsedMonths = collapsedMonthSet,
            collapsedDates = collapsedDateSet,
            daySorts = sorts,
            searchQuery = activeFilter.searchQuery.orEmpty(),
            selectedType = activeFilter.type,
            selectedSource = activeFilter.source,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionListUiState(selectedMonth = currentMonth),
    )

    fun search(query: String) {
        filter.update { it.copy(searchQuery = query) }
    }

    /** Passing the already selected value clears the filter, which is what a chip toggle means. */
    fun toggleTypeFilter(type: TransactionType?) {
        filter.update { it.copy(type = if (it.type == type) null else type) }
    }

    fun toggleSourceFilter(source: TransactionSource?) {
        filter.update { it.copy(source = if (it.source == source) null else source) }
    }

    /** Null means every month; otherwise only that calendar month is loaded. */
    fun selectMonth(month: YearMonth?) {
        selectedMonth.value = month
        filter.update {
            it.copy(dateRange = month?.let { chosen -> InstantRange.ofMonth(chosen, zone) })
        }
        collapsedMonths.value = if (month == null) {
            availableMonths.value.drop(1).toSet()
        } else {
            emptySet()
        }
    }

    fun toggleMonthCollapsed(month: YearMonth) {
        collapsedMonths.update { current ->
            if (month in current) current - month else current + month
        }
    }

    fun toggleDayCollapsed(date: LocalDate) {
        collapsedDates.update { current ->
            if (date in current) current - date else current + date
        }
    }

    /** Down arrow: largest first. Tap again to return to latest-first. */
    fun sortDayHighToLow(date: LocalDate) {
        daySorts.update { current ->
            val next = if (current[date] == DayAmountSort.AMOUNT_HIGH_TO_LOW) {
                DayAmountSort.LATEST
            } else {
                DayAmountSort.AMOUNT_HIGH_TO_LOW
            }
            current + (date to next)
        }
    }

    /** Up arrow: smallest first. Tap again to return to latest-first. */
    fun sortDayLowToHigh(date: LocalDate) {
        daySorts.update { current ->
            val next = if (current[date] == DayAmountSort.AMOUNT_LOW_TO_HIGH) {
                DayAmountSort.LATEST
            } else {
                DayAmountSort.AMOUNT_LOW_TO_HIGH
            }
            current + (date to next)
        }
    }

    fun setCategory(transactionId: Long, categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.setCategory(transactionId, categoryId)
        }
    }

    private fun monthsCoveredBy(bounds: TransactionDateBounds): List<YearMonth> {
        val latest = bounds.latest?.let { YearMonth.from(it.atZone(zone)) } ?: currentMonth
        val earliest = bounds.earliest?.let { YearMonth.from(it.atZone(zone)) } ?: latest
        val months = mutableListOf<YearMonth>()
        var cursor = latest
        while (!cursor.isBefore(earliest)) {
            months += cursor
            cursor = cursor.minusMonths(1)
        }
        return months
    }

    private fun groupByMonth(
        transactions: List<Transaction>,
        categoriesById: Map<Long, Category>,
        sorts: Map<LocalDate, DayAmountSort>,
    ): List<TransactionMonthGroup> {
        val dayGroups = groupByDay(transactions, categoriesById, sorts)
        return dayGroups
            .groupBy { YearMonth.from(it.date) }
            .map { (month, days) ->
                val orderedDays = days.sortedByDescending { it.date }
                TransactionMonthGroup(
                    month = month,
                    header = DateTimeFormatters.monthLabel(month),
                    dayGroups = orderedDays,
                    creditTotal = Money.sum(orderedDays.map { it.creditTotal }),
                    debitTotal = Money.sum(orderedDays.map { it.debitTotal }),
                )
            }
            .sortedByDescending { it.month }
    }

    private fun groupByDay(
        transactions: List<Transaction>,
        categoriesById: Map<Long, Category>,
        sorts: Map<LocalDate, DayAmountSort>,
    ): List<TransactionDayGroup> {
        val today = LocalDate.now(clock.withZone(zone))
        return transactions
            .groupBy { DateTimeFormatters.localDateOf(it.transactionDate, zone) }
            .map { (date, dayTransactions) ->
                val credits = dayTransactions
                    .filter { it.countsTowardTotals && it.type == TransactionType.CREDIT }
                    .map { it.amount.absoluteValue }
                val debits = dayTransactions
                    .filter { it.countsTowardTotals && it.type == TransactionType.DEBIT }
                    .map { it.amount.absoluteValue }
                val ordered = sortDayTransactions(
                    dayTransactions,
                    sorts[date] ?: DayAmountSort.LATEST,
                )
                TransactionDayGroup(
                    date = date,
                    header = DateTimeFormatters.dayHeader(date, today),
                    entries = ordered.map { transaction ->
                        TransactionEntry(
                            transaction = transaction,
                            category = transaction.categoryId?.let(categoriesById::get),
                        )
                    },
                    creditTotal = Money.sum(credits),
                    debitTotal = Money.sum(debits),
                    uncategorisedCount = dayTransactions.count { it.categoryId == null },
                )
            }
            .sortedByDescending { it.date }
    }

    private fun sortDayTransactions(
        dayTransactions: List<Transaction>,
        sort: DayAmountSort,
    ): List<Transaction> = when (sort) {
        DayAmountSort.LATEST -> dayTransactions.sortedWith(
            compareByDescending<Transaction> { it.transactionDate }
                .thenByDescending { it.id },
        )
        DayAmountSort.AMOUNT_HIGH_TO_LOW -> dayTransactions.sortedWith(
            compareByDescending<Transaction> { it.amount.absoluteValue.minorUnits }
                .thenByDescending { it.transactionDate },
        )
        DayAmountSort.AMOUNT_LOW_TO_HIGH -> dayTransactions.sortedWith(
            compareBy<Transaction> { it.amount.absoluteValue.minorUnits }
                .thenByDescending { it.transactionDate },
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TransactionListViewModel(
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    zone = container.zone,
                )
            }
        }
    }
}
