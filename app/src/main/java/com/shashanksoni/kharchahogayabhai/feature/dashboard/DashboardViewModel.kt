package com.shashanksoni.kharchahogayabhai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.MonthlyDashboard
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.GetMonthlyDashboardUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

data class DashboardUiState(
    val selectedMonth: YearMonth,
    val currentMonth: YearMonth,
    val dashboard: MonthlyDashboard? = null,
    val isLoading: Boolean = true,
) {
    /** Future months hold nothing, so browsing into them is not offered. */
    val canSelectNextMonth: Boolean get() = selectedMonth < currentMonth
}

class DashboardViewModel(
    private val getMonthlyDashboard: GetMonthlyDashboardUseCase,
    private val transactionRepository: TransactionRepository,
    zone: ZoneId,
    clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val currentMonth: YearMonth = YearMonth.now(clock.withZone(zone))
    private val selectedMonth = MutableStateFlow(currentMonth)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = selectedMonth
        .flatMapLatest { month ->
            getMonthlyDashboard(month)
                .map { dashboard -> uiStateOf(month, dashboard) }
                // Switching months shows the new heading immediately, without the
                // previous month's figures underneath it.
                .onStart { emit(uiStateOf(month, dashboard = null)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = uiStateOf(selectedMonth.value, dashboard = null),
        )

    fun showPreviousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun showNextMonth() {
        selectedMonth.update { month -> if (month < currentMonth) month.plusMonths(1) else month }
    }

    fun setCategory(transactionId: Long, categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.setCategory(transactionId, categoryId)
        }
    }

    fun setIgnored(transactionId: Long, ignored: Boolean) {
        viewModelScope.launch {
            transactionRepository.setIgnored(transactionId, ignored)
        }
    }

    private fun uiStateOf(month: YearMonth, dashboard: MonthlyDashboard?) = DashboardUiState(
        selectedMonth = month,
        currentMonth = currentMonth,
        dashboard = dashboard,
        isLoading = dashboard == null,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    getMonthlyDashboard = container.getMonthlyDashboard,
                    transactionRepository = container.transactionRepository,
                    zone = container.zone,
                )
            }
        }
    }
}
