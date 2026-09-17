package com.shashanksoni.kharchahogayabhai.feature.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
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

data class TransactionListUiState(
    val dayGroups: List<TransactionDayGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val collapsedDates: Set<LocalDate> = emptySet(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedSource: TransactionSource? = null,
    val isLoading: Boolean = true,
) {
    val transactionCount: Int get() = dayGroups.sumOf { it.entries.size }
    val isEmpty: Boolean get() = !isLoading && dayGroups.isEmpty()

    fun isCollapsed(date: LocalDate): Boolean = date in collapsedDates
}

/**
 * Owns the transaction list's filters and turns stored transactions into
 * day-grouped rows, so the screen only lays out what it is given.
 */
class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val zone: ZoneId,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter.NONE)
    private val collapsedDates = MutableStateFlow<Set<LocalDate>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListUiState> = combine(
        filter,
        filter.flatMapLatest(transactionRepository::observeTransactions),
        categoryRepository.observeCategories(),
        collapsedDates,
    ) { activeFilter, transactions, categories, collapsed ->
        val categoriesById = categories.associateBy { it.id }
        TransactionListUiState(
            dayGroups = groupByDay(transactions, categoriesById),
            categories = categories,
            collapsedDates = collapsed,
            searchQuery = activeFilter.searchQuery.orEmpty(),
            selectedType = activeFilter.type,
            selectedSource = activeFilter.source,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionListUiState(),
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

    fun toggleDayCollapsed(date: LocalDate) {
        collapsedDates.update { current ->
            if (date in current) current - date else current + date
        }
    }

    fun setCategory(transactionId: Long, categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.setCategory(transactionId, categoryId)
        }
    }

    private fun groupByDay(
        transactions: List<Transaction>,
        categoriesById: Map<Long, Category>,
    ): List<TransactionDayGroup> {
        val today = LocalDate.now(clock.withZone(zone))
        return transactions
            .groupBy { DateTimeFormatters.localDateOf(it.transactionDate, zone) }
            .map { (date, dayTransactions) ->
                val credits = dayTransactions
                    .filter { it.type == TransactionType.CREDIT }
                    .map { it.amount.absoluteValue }
                val debits = dayTransactions
                    .filter { it.type == TransactionType.DEBIT }
                    .map { it.amount.absoluteValue }
                TransactionDayGroup(
                    date = date,
                    header = DateTimeFormatters.dayHeader(date, today),
                    entries = dayTransactions.map { transaction ->
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
