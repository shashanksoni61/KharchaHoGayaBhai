package com.shashanksoni.kharchahogayabhai.sms

import android.content.SharedPreferences
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remembers how far SMS inbox scanning has progressed so the next scan only
 * reads newer messages, and which automatic sync modes are enabled.
 */
class SmsScanPreferences(
    private val prefs: SharedPreferences,
) {

    private val lastScannedAtMillis = MutableStateFlow(readLastScannedAtMillis())
    private val autoScanOnOpen = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SCAN_ON_OPEN, DEFAULT_AUTO),
    )
    private val listenInBackground = MutableStateFlow(
        prefs.getBoolean(KEY_LISTEN_BACKGROUND, DEFAULT_AUTO),
    )

    val lastScannedAtMillisFlow: StateFlow<Long> = lastScannedAtMillis.asStateFlow()
    val autoScanOnOpenFlow: StateFlow<Boolean> = autoScanOnOpen.asStateFlow()
    val listenInBackgroundFlow: StateFlow<Boolean> = listenInBackground.asStateFlow()

    /** Epoch millis of the newest SMS already considered; 0 means never scanned. */
    fun lastScannedAtMillis(): Long = lastScannedAtMillis.value

    fun lastScannedAt(): Instant? =
        lastScannedAtMillis.value.takeIf { it > 0L }?.let(Instant::ofEpochMilli)

    fun autoScanOnOpenEnabled(): Boolean = autoScanOnOpen.value

    fun listenInBackgroundEnabled(): Boolean = listenInBackground.value

    fun setAutoScanOnOpenEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCAN_ON_OPEN, enabled).apply()
        autoScanOnOpen.value = enabled
    }

    fun setListenInBackgroundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LISTEN_BACKGROUND, enabled).apply()
        listenInBackground.value = enabled
    }

    /**
     * Advances the cursor when [newestMessageAt] is later than what we already stored.
     * Pass the newest SMS date that was read in this scan (not wall-clock), so we
     * do not skip messages that arrived during the scan itself.
     */
    fun advanceCursorTo(newestMessageAt: Instant) {
        val millis = newestMessageAt.toEpochMilli()
        if (millis <= lastScannedAtMillis.value) return
        prefs.edit().putLong(KEY_LAST_SCANNED_AT, millis).apply()
        lastScannedAtMillis.value = millis
    }

    /** Clears the cursor so the next scan reads the full inbox again. */
    fun clearCursor() {
        prefs.edit().remove(KEY_LAST_SCANNED_AT).apply()
        lastScannedAtMillis.value = 0L
    }

    private fun readLastScannedAtMillis(): Long =
        prefs.getLong(KEY_LAST_SCANNED_AT, 0L).coerceAtLeast(0L)

    companion object {
        private const val KEY_LAST_SCANNED_AT = "sms_last_scanned_at_millis"
        private const val KEY_AUTO_SCAN_ON_OPEN = "sms_auto_scan_on_open"
        private const val KEY_LISTEN_BACKGROUND = "sms_listen_in_background"
        private const val DEFAULT_AUTO = true
    }
}
