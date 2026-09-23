package com.shashanksoni.kharchahogayabhai.security

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remembers the user's on-device security choices. Authentication itself uses the
 * platform [androidx.biometric.BiometricPrompt] with device credential fallback,
 * so no password or biometric material is ever stored by the app.
 *
 * - [appLockEnabled]: require auth to open the app.
 * - [hideIncomeEnabled]: mask income / balance until the user unlocks it with the
 *   eye toggle for the current session.
 */
class SecurityPreferences(
    private val prefs: SharedPreferences,
) {

    private val appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    private val hideIncomeEnabled = MutableStateFlow(prefs.getBoolean(KEY_HIDE_INCOME, true))

    val appLockEnabledFlow: StateFlow<Boolean> = appLockEnabled.asStateFlow()
    val hideIncomeEnabledFlow: StateFlow<Boolean> = hideIncomeEnabled.asStateFlow()

    fun isAppLockEnabled(): Boolean = appLockEnabled.value

    fun isHideIncomeEnabled(): Boolean = hideIncomeEnabled.value

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        appLockEnabled.value = enabled
    }

    fun setHideIncomeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_INCOME, enabled).apply()
        hideIncomeEnabled.value = enabled
    }

    companion object {
        private const val KEY_APP_LOCK = "security_app_lock_enabled"
        private const val KEY_HIDE_INCOME = "security_hide_income_enabled"
    }
}
