package com.shashanksoni.kharchahogayabhai.navigation

import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries the dashboard's visible month to the transaction list. The list tab
 * keeps its own ViewModel across bottom-nav restores, so this is how a tap on
 * August's chart or "See all" selects August there.
 */
class PendingTransactionMonth {
    private val _month = MutableStateFlow<YearMonth?>(null)
    val month: StateFlow<YearMonth?> = _month.asStateFlow()

    fun request(month: YearMonth) {
        _month.value = month
    }

    fun consume() {
        _month.value = null
    }
}
