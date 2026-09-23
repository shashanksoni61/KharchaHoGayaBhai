package com.shashanksoni.kharchahogayabhai.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory unlock state for the current process. Cleared when the app goes to
 * the background so the next open asks for fingerprint / device password again.
 */
class SecuritySession {

    private val appUnlocked = MutableStateFlow(false)
    private val incomeRevealed = MutableStateFlow(false)

    val appUnlockedFlow: StateFlow<Boolean> = appUnlocked.asStateFlow()
    val incomeRevealedFlow: StateFlow<Boolean> = incomeRevealed.asStateFlow()

    /** True while a system biometric / credential prompt is on screen. */
    @Volatile
    var isAuthenticating: Boolean = false

    fun isAppUnlocked(): Boolean = appUnlocked.value

    fun isIncomeRevealed(): Boolean = incomeRevealed.value

    fun unlockApp() {
        appUnlocked.value = true
    }

    fun lockApp() {
        appUnlocked.value = false
        incomeRevealed.value = false
    }

    fun revealIncome() {
        incomeRevealed.value = true
    }

    fun hideIncome() {
        incomeRevealed.value = false
    }
}
