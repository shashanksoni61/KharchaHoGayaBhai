package com.shashanksoni.kharchahogayabhai.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.shashanksoni.kharchahogayabhai.KharchaApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Listens for new SMS in the background. When enabled in Settings, runs an
 * incremental inbox scan (messages after the last saved scan cursor) so bank /
 * UPI alerts reach the dashboard without opening the app.
 *
 * A short delay lets the system write the SMS into the inbox provider before we query.
 */
class SmsReceivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as? KharchaApplication ?: return
        val prefs = app.container.smsScanPreferences
        if (!prefs.listenInBackgroundEnabled()) return

        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                // Inbox ContentProvider is often updated slightly after the broadcast.
                delay(INBOX_SETTLE_DELAY_MS)
                val result = app.container.importSmsInbox.syncOnNewSms()
                result?.createdTransactions?.let { created ->
                    app.container.transactionAlertNotifier.notifyCreated(created)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val INBOX_SETTLE_DELAY_MS = 1_500L
    }
}
