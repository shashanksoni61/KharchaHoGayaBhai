package com.shashanksoni.kharchahogayabhai.sms

import android.content.SharedPreferences
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Exclusive lower bound for the next incremental inbox read. */
data class SmsScanCursor(
    val dateMillis: Long,
    val smsId: Long,
)

/**
 * Remembers how far SMS inbox scanning has progressed so the next scan only
 * reads newer messages, and which automatic sync modes are enabled.
 *
 * Cursor is (date, smsId) so multiple alerts that share the same provider
 * timestamp (common when one bank sender delivers a burst) are all scanned.
 */
class SmsScanPreferences(
    private val prefs: SharedPreferences,
) {

    private val lastScannedAtMillis = MutableStateFlow(readLastScannedAtMillis())
    private val lastScannedSmsId = MutableStateFlow(readLastScannedSmsId())
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

    fun scanCursor(): SmsScanCursor? {
        val date = lastScannedAtMillis.value
        if (date <= 0L) return null
        return SmsScanCursor(dateMillis = date, smsId = lastScannedSmsId.value)
    }

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
     * Advances past [message] when it is strictly after the stored cursor.
     * Pass the newest SMS (by date, then id) read in this scan.
     */
    fun advanceCursorPast(message: SmsMessage) {
        advanceCursorTo(
            dateMillis = message.receivedAt.toEpochMilli(),
            smsId = message.id,
        )
    }

    fun advanceCursorTo(dateMillis: Long, smsId: Long = 0L) {
        val current = scanCursor()
        if (current != null) {
            if (dateMillis < current.dateMillis) return
            if (dateMillis == current.dateMillis && smsId <= current.smsId) return
        } else if (dateMillis <= 0L) {
            return
        }
        prefs.edit()
            .putLong(KEY_LAST_SCANNED_AT, dateMillis)
            .putLong(KEY_LAST_SCANNED_SMS_ID, smsId)
            .apply()
        lastScannedAtMillis.value = dateMillis
        lastScannedSmsId.value = smsId
    }

    /** Wall-clock bump used when an incremental scan finds nothing new. */
    fun advanceCursorTo(newestMessageAt: Instant) {
        advanceCursorTo(newestMessageAt.toEpochMilli(), smsId = Long.MAX_VALUE)
    }

    /** Clears the cursor so the next scan reads the full inbox again. */
    fun clearCursor() {
        prefs.edit()
            .remove(KEY_LAST_SCANNED_AT)
            .remove(KEY_LAST_SCANNED_SMS_ID)
            .apply()
        lastScannedAtMillis.value = 0L
        lastScannedSmsId.value = 0L
    }

    private fun readLastScannedAtMillis(): Long =
        prefs.getLong(KEY_LAST_SCANNED_AT, 0L).coerceAtLeast(0L)

    private fun readLastScannedSmsId(): Long =
        prefs.getLong(KEY_LAST_SCANNED_SMS_ID, 0L).coerceAtLeast(0L)

    companion object {
        private const val KEY_LAST_SCANNED_AT = "sms_last_scanned_at_millis"
        private const val KEY_LAST_SCANNED_SMS_ID = "sms_last_scanned_sms_id"
        private const val KEY_AUTO_SCAN_ON_OPEN = "sms_auto_scan_on_open"
        private const val KEY_LISTEN_BACKGROUND = "sms_listen_in_background"
        private const val DEFAULT_AUTO = true
    }
}
