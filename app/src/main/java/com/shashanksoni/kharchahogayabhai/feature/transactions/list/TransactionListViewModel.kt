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
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.navigation.PendingTransactionMonth
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
import java.time.Instant
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
    val net: Money get() = Money.net(creditTotal, debitTotal)
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
    val net: Money get() = Money.net(creditTotal, debitTotal)
    val transactionCount: Int get() = dayGroups.sumOf { it.transactionCount }
    val uncategorisedCount: Int get() = dayGroups.sumOf { it.uncategorisedCount }
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
    val minAmountInput: String = "",
    val maxAmountInput: String = "",
    val minAmountMinorUnits: Long? = null,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedAccount: String? = null,
    val accounts: List<String> = emptyList(),
    val excludePromotional: Boolean = false,
    val includeIgnored: Boolean = false,
    val uncategorisedOnly: Boolean = false,
    val isLoading: Boolean = true,
) {
    val transactionCount: Int get() = monthGroups.sumOf { it.transactionCount }
    val isEmpty: Boolean get() = !isLoading && monthGroups.isEmpty()
    val hasAdvancedFilters: Boolean
        get() = minAmountMinorUnits != null ||
            maxAmountInput.isNotBlank() ||
            selectedCategoryIds.isNotEmpty() ||
            selectedAccount != null ||
            uncategorisedOnly
    /** Any list filter other than the month chips. */
    val hasSheetFilters: Boolean
        get() = selectedType != null ||
            selectedSource != null ||
            hasAdvancedFilters ||
            !excludePromotional ||
            includeIgnored

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
    private val pendingTransactionMonth: PendingTransactionMonth? = null,
) : ViewModel() {

    private val currentMonth: YearMonth = YearMonth.now(clock.withZone(zone))
    private val selectedMonth = MutableStateFlow<YearMonth?>(currentMonth)
    private val filter = MutableStateFlow(
        TransactionFilter(
            dateRange = InstantRange.ofMonth(currentMonth, zone),
            excludePromotional = true,
        ),
    )
    private val collapsedMonths = MutableStateFlow<Set<YearMonth>>(emptySet())
    private val collapsedDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val daySorts = MutableStateFlow<Map<LocalDate, DayAmountSort>>(emptyMap())
    private val minAmountInput = MutableStateFlow("")
    private val maxAmountInput = MutableStateFlow("")
    /** Keeps a dashboard-requested month until the user picks a different chip. */
    private var holdRequestedMonth = false

    private val availableMonths: StateFlow<List<YearMonth>> =
        transactionRepository.observeTransactionDates()
            .map { instants -> monthsWithData(instants) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )

    init {
        viewModelScope.launch {
            pendingTransactionMonth?.month?.collect { month ->
                if (month != null) {
                    selectMonth(month, hold = true)
                    pendingTransactionMonth.consume()
                }
            }
        }
        viewModelScope.launch {
            availableMonths.collect { months ->
                if (holdRequestedMonth) return@collect
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
            minAmountInput,
            maxAmountInput,
        ) { activeFilter, month, months, minInput, maxInput ->
            FilterHeader(activeFilter, month, months, minInput, maxInput)
        },
        filter.flatMapLatest(transactionRepository::observeTransactions),
        combine(
            categoryRepository.observeCategories(),
            transactionRepository.observeAccountIdentifiers(),
        ) { categories, accounts -> categories to accounts },
        combine(collapsedMonths, collapsedDates, daySorts) { months, dates, sorts ->
            Triple(months, dates, sorts)
        },
    ) { header, transactions, catalogue, grouping ->
        val (activeFilter, month, months, minInput, maxInput) = header
        val (categories, accounts) = catalogue
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
            minAmountInput = minInput,
            maxAmountInput = maxInput,
            minAmountMinorUnits = activeFilter.minAmountMinorUnits,
            selectedCategoryIds = activeFilter.categoryIds,
            selectedAccount = activeFilter.accountIdentifier,
            accounts = accounts,
            excludePromotional = activeFilter.excludePromotional,
            includeIgnored = activeFilter.includeIgnored,
            uncategorisedOnly = activeFilter.uncategorisedOnly,
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

    fun setMinAmountInput(raw: String) {
        minAmountInput.value = raw
        filter.update { it.copy(minAmountMinorUnits = parseAmountMinor(raw)) }
    }

    fun setMaxAmountInput(raw: String) {
        maxAmountInput.value = raw
        filter.update { it.copy(maxAmountMinorUnits = parseAmountMinor(raw)) }
    }

    /** Quick “at least this many rupees”. Tap the active chip again to clear. */
    fun toggleMinAmountPreset(rupees: Long) {
        val minor = rupees * 100L
        if (filter.value.minAmountMinorUnits == minor && maxAmountInput.value.isBlank()) {
            minAmountInput.value = ""
            filter.update { it.copy(minAmountMinorUnits = null) }
            return
        }
        minAmountInput.value = rupees.toString()
        maxAmountInput.value = ""
        filter.update { it.copy(minAmountMinorUnits = minor, maxAmountMinorUnits = null) }
    }

    fun toggleCategoryFilter(categoryId: Long) {
        filter.update { current ->
            val next = if (categoryId in current.categoryIds) {
                current.categoryIds - categoryId
            } else {
                current.categoryIds + categoryId
            }
            current.copy(categoryIds = next, uncategorisedOnly = false)
        }
    }

    fun toggleAccountFilter(account: String?) {
        filter.update { it.copy(accountIdentifier = if (it.accountIdentifier == account) null else account) }
    }

    fun setExcludePromotional(exclude: Boolean) {
        filter.update { it.copy(excludePromotional = exclude) }
    }

    fun setIncludeIgnored(include: Boolean) {
        filter.update { it.copy(includeIgnored = include) }
    }

    fun setIgnored(transactionId: Long, ignored: Boolean) {
        viewModelScope.launch {
            transactionRepository.setIgnored(transactionId, ignored)
        }
    }

    fun setUncategorisedOnly(only: Boolean) {
        filter.update {
            it.copy(
                uncategorisedOnly = only,
                categoryIds = if (only) emptySet() else it.categoryIds,
            )
        }
    }

    fun clearNonMonthFilters() {
        minAmountInput.value = ""
        maxAmountInput.value = ""
        filter.update {
            it.copy(
                type = null,
                source = null,
                minAmountMinorUnits = null,
                maxAmountMinorUnits = null,
                categoryIds = emptySet(),
                accountIdentifier = null,
                excludePromotional = true,
                includeIgnored = false,
                uncategorisedOnly = false,
            )
        }
    }

    fun clearAdvancedFilters() {
        minAmountInput.value = ""
        maxAmountInput.value = ""
        filter.update {
            it.copy(
                minAmountMinorUnits = null,
                maxAmountMinorUnits = null,
                categoryIds = emptySet(),
                accountIdentifier = null,
                excludePromotional = true,
                includeIgnored = false,
                uncategorisedOnly = false,
            )
        }
    }

    private fun parseAmountMinor(raw: String): Long? =
        raw.trim().takeIf { it.isNotEmpty() }?.let(Money::parseMajorUnitsOrNull)?.minorUnits

    /** Null means every month; otherwise only that calendar month is loaded. */
    fun selectMonth(month: YearMonth?, hold: Boolean = false) {
        holdRequestedMonth = hold
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

    private fun monthsWithData(instants: List<Instant>): List<YearMonth> {
        val months = instants
            .map { YearMonth.from(it.atZone(zone)) }
            .distinct()
            .sortedDescending()
        return months.ifEmpty { listOf(currentMonth) }
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

    private data class FilterHeader(
        val filter: TransactionFilter,
        val selectedMonth: YearMonth?,
        val availableMonths: List<YearMonth>,
        val minAmountInput: String,
        val maxAmountInput: String,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TransactionListViewModel(
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    zone = container.zone,
                    pendingTransactionMonth = container.pendingTransactionMonth,
                )
            }
        }
    }
}
