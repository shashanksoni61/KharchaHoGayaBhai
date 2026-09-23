package com.shashanksoni.kharchahogayabhai.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shashanksoni.kharchahogayabhai.MainActivity
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.MoneyFormatter
import com.shashanksoni.kharchahogayabhai.domain.model.InstantRange
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionFilter
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Posts a local notification after a new bank/UPI SMS is ingested. The line
 * shows this alert's amount and today's running total for the same direction.
 */
class TransactionAlertNotifier(
    private val appContext: Context,
    private val transactionRepository: TransactionRepository,
    private val zone: ZoneId,
    private val clock: Clock,
) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    suspend fun notifyCreated(created: List<ParsedTransaction>) {
        if (created.isEmpty()) return
        if (!canPostNotifications()) return
        ensureChannel()
        val (todayIn, todayOut) = todayTotals()
        created.filter { !it.isPromotional }.forEach { parsed ->
            notifyOne(parsed, todayIn, todayOut)
        }
    }

    private suspend fun todayTotals(): Pair<Money, Money> {
        val today = LocalDate.now(clock.withZone(zone))
        val rows = transactionRepository.observeTransactions(
            TransactionFilter(dateRange = InstantRange.ofDay(today, zone)),
        ).first()
        val currency = rows.firstOrNull()?.amount?.currencyCode ?: Money.DEFAULT_CURRENCY_CODE
        val credits = rows.filter { it.countsTowardTotals && it.type == TransactionType.CREDIT }
            .map { it.amount.absoluteValue }
        val debits = rows.filter { it.countsTowardTotals && it.type == TransactionType.DEBIT }
            .map { it.amount.absoluteValue }
        return Money.sum(credits, currency) to Money.sum(debits, currency)
    }

    private fun notifyOne(parsed: ParsedTransaction, todayIn: Money, todayOut: Money) {
        val amount = MoneyFormatter.format(parsed.amount.absoluteValue)
        val title = parsed.merchantName?.takeIf { it.isNotBlank() }
            ?: appContext.getString(
                if (parsed.type == TransactionType.CREDIT) {
                    R.string.notification_title_credit
                } else {
                    R.string.notification_title_debit
                },
            )
        val text = if (parsed.type == TransactionType.CREDIT) {
            appContext.getString(
                R.string.notification_credit_body,
                amount,
                MoneyFormatter.format(todayIn),
            )
        } else {
            appContext.getString(
                R.string.notification_debit_body,
                amount,
                MoneyFormatter.format(todayOut),
            )
        }

        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        val id = parsed.sourceIdentifier?.hashCode()
            ?: parsed.referenceNumber?.hashCode()
            ?: parsed.transactionDate.hashCode()
        try {
            NotificationManagerCompat.from(appContext).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked after we checked.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "transaction_alerts"
    }
}
