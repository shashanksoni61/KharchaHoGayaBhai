package com.shashanksoni.kharchahogayabhai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.ImportResult
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.repository.LabelRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.ImportSmsInboxUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.ResetLocalDataUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.SmsScanMode
import com.shashanksoni.kharchahogayabhai.security.SecurityPreferences
import com.shashanksoni.kharchahogayabhai.security.SecuritySession
import com.shashanksoni.kharchahogayabhai.sms.SmsInboxReader
import com.shashanksoni.kharchahogayabhai.sms.SmsScanPreferences
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val labels: List<TransactionLabel> = emptyList(),
    val isResetting: Boolean = false,
    val isScanningSms: Boolean = false,
    val lastSmsScanAt: Instant? = null,
    val autoSmsScanOnOpen: Boolean = true,
    val listenSmsInBackground: Boolean = true,
    /** Total transactions currently stored across every source. */
    val totalTransactionCount: Int = 0,
    /** How many of those transactions came from an SMS alert. */
    val smsTransactionCount: Int = 0,
    /** Messages inspected in the most recent scan this session (0 until one runs). */
    val lastScanScannedCount: Int = 0,
    /** Inbox size reported by the provider when the last scan started. */
    val lastScanInboxTotal: Int = 0,
    /** Bank/UPI alerts recognised in the most recent scan this session. */
    val lastScanParsedCount: Int = 0,
    /** Inbox rows the phone reports (OTPs and chats included). */
    val phoneInboxCount: Int = 0,
    /** Inbox + sent + drafts, for comparison with other SMS apps. */
    val phoneAllSmsCount: Int = 0,
    val appLockEnabled: Boolean = false,
    val hideIncomeEnabled: Boolean = false,
    val resetCompleted: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class SettingsViewModel(
    private val resetLocalData: ResetLocalDataUseCase,
    private val labelRepository: LabelRepository,
    private val importSmsInbox: ImportSmsInboxUseCase,
    private val transactionRepository: TransactionRepository,
    private val smsScanPreferences: SmsScanPreferences,
    private val securityPreferences: SecurityPreferences,
    private val securitySession: SecuritySession,
    private val inboxReader: SmsInboxReader,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val isResetting = MutableStateFlow(false)
    private val isScanningSms = MutableStateFlow(false)
    private val resetCompleted = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val infoMessage = MutableStateFlow<String?>(null)
    private val lastScanScannedCount = MutableStateFlow(0)
    private val lastScanInboxTotal = MutableStateFlow(0)
    private val lastScanParsedCount = MutableStateFlow(0)
    private val phoneInboxCount = MutableStateFlow(0)
    private val phoneAllSmsCount = MutableStateFlow(0)

    init {
        refreshPhoneSmsCounts()
    }

    private data class BusyState(
        val resetting: Boolean,
        val scanning: Boolean,
        val completed: Boolean,
        val error: String?,
        val info: String?,
    )

    private data class SmsPrefsState(
        val lastScanMillis: Long,
        val autoOnOpen: Boolean,
        val listenBackground: Boolean,
    )

    private data class CountsState(
        val total: Int,
        val fromSms: Int,
        val lastScanScanned: Int,
        val lastScanInboxTotal: Int,
        val lastScanParsed: Int,
        val phoneInboxCount: Int,
        val phoneAllSmsCount: Int,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        labelRepository.observeLabels(),
        combine(
            smsScanPreferences.lastScannedAtMillisFlow,
            smsScanPreferences.autoScanOnOpenFlow,
            smsScanPreferences.listenInBackgroundFlow,
        ) { last, autoOnOpen, listen ->
            SmsPrefsState(last, autoOnOpen, listen)
        },
        combine(
            isResetting,
            isScanningSms,
            resetCompleted,
            errorMessage,
            infoMessage,
        ) { resetting, scanning, completed, error, info ->
            BusyState(resetting, scanning, completed, error, info)
        },
        combine(
            combine(
                transactionRepository.observeTransactionCount(),
                transactionRepository.observeTransactionCountBySource(TransactionSource.SMS),
            ) { total, fromSms -> total to fromSms },
            combine(
                lastScanScannedCount,
                lastScanInboxTotal,
                lastScanParsedCount,
                phoneInboxCount,
                phoneAllSmsCount,
            ) { scanned, inboxTotal, parsed, phoneInbox, phoneAll ->
                intArrayOf(scanned, inboxTotal, parsed, phoneInbox, phoneAll)
            },
        ) { stored, scan ->
            CountsState(
                total = stored.first,
                fromSms = stored.second,
                lastScanScanned = scan[0],
                lastScanInboxTotal = scan[1],
                lastScanParsed = scan[2],
                phoneInboxCount = scan[3],
                phoneAllSmsCount = scan[4],
            )
        },
        combine(
            securityPreferences.appLockEnabledFlow,
            securityPreferences.hideIncomeEnabledFlow,
        ) { appLock, hideIncome ->
            appLock to hideIncome
        },
    ) { labels, smsPrefs, busy, counts, security ->
        SettingsUiState(
            labels = labels,
            isResetting = busy.resetting,
            isScanningSms = busy.scanning,
            lastSmsScanAt = smsPrefs.lastScanMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
            autoSmsScanOnOpen = smsPrefs.autoOnOpen,
            listenSmsInBackground = smsPrefs.listenBackground,
            totalTransactionCount = counts.total,
            smsTransactionCount = counts.fromSms,
            lastScanScannedCount = counts.lastScanScanned,
            lastScanInboxTotal = counts.lastScanInboxTotal,
            lastScanParsedCount = counts.lastScanParsed,
            phoneInboxCount = counts.phoneInboxCount,
            phoneAllSmsCount = counts.phoneAllSmsCount,
            appLockEnabled = security.first,
            hideIncomeEnabled = security.second,
            resetCompleted = busy.completed,
            errorMessage = busy.error,
            infoMessage = busy.info,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setAutoSmsScanOnOpen(enabled: Boolean) {
        smsScanPreferences.setAutoScanOnOpenEnabled(enabled)
    }

    fun setListenSmsInBackground(enabled: Boolean) {
        smsScanPreferences.setListenInBackgroundEnabled(enabled)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securityPreferences.setAppLockEnabled(enabled)
        if (enabled) securitySession.unlockApp()
    }

    fun setHideIncomeEnabled(enabled: Boolean) {
        securityPreferences.setHideIncomeEnabled(enabled)
        if (!enabled) securitySession.revealIncome()
        else securitySession.hideIncome()
    }

    /** Manual scan: only messages after the last saved scan date (full inbox if never scanned). */
    fun scanNewSms() {
        runSmsScan(SmsScanMode.INCREMENTAL)
    }

    /** Optional full inbox re-read; resets the cursor first. */
    fun rescanAllSms() {
        runSmsScan(SmsScanMode.FULL)
    }

    private fun runSmsScan(mode: SmsScanMode) {
        if (isScanningSms.value || isResetting.value) return
        applicationScope.launch {
            isScanningSms.value = true
            errorMessage.value = null
            try {
                val result = importSmsInbox(
                    mode = mode,
                    recordEmptyHistory = true,
                    onProgress = { progress ->
                        lastScanScannedCount.value = progress.scannedCount
                        lastScanInboxTotal.value = progress.inboxTotal
                        lastScanParsedCount.value = progress.parsedCount
                        if (progress.allSmsCount > 0) {
                            phoneAllSmsCount.value = progress.allSmsCount
                        }
                        phoneInboxCount.value = progress.inboxTotal
                    },
                )
                applySmsResult(result, fullRescan = mode == SmsScanMode.FULL)
                refreshPhoneSmsCounts()
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "SMS scan failed"
            } finally {
                isScanningSms.value = false
            }
        }
    }

    private fun refreshPhoneSmsCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                phoneInboxCount.value = inboxReader.countInbox()
                phoneAllSmsCount.value = inboxReader.countAllSms()
            }
        }
    }

    fun reportSmsPermissionDenied() {
        errorMessage.value =
            "SMS permission was denied. Allow SMS access to scan transaction alerts."
    }

    fun reportSecurityError(message: String) {
        errorMessage.value = message
        infoMessage.value = null
    }

    private fun applySmsResult(result: ImportResult, fullRescan: Boolean) {
        val batch = result.batch
        lastScanScannedCount.value = result.scannedCount
        lastScanParsedCount.value = batch.totalParsed
        val totals = "Now showing data from ${result.storedTotalCount} transactions."
        if (batch.newCount > 0 || batch.mergedCount > 0) {
            val scope = if (fullRescan) "Full SMS scan" else "SMS scan"
            infoMessage.value =
                "$scope: scanned ${result.scannedCount} messages · " +
                    "${batch.newCount} new, ${batch.mergedCount} merged, " +
                    "${batch.duplicateCount} already present. $totals"
            errorMessage.value = null
        } else if (batch.errorMessage != null) {
            errorMessage.value =
                if (result.scannedCount > 0) {
                    "${batch.errorMessage} (scanned ${result.scannedCount} messages). $totals"
                } else {
                    "${batch.errorMessage} $totals"
                }
            infoMessage.value = null
        } else {
            infoMessage.value =
                "SMS scan finished — scanned ${result.scannedCount} messages, nothing new to add. $totals"
        }
    }

    fun resetAllData() {
        if (isResetting.value) return
        viewModelScope.launch {
            isResetting.value = true
            errorMessage.value = null
            try {
                resetLocalData()
                resetCompleted.value = true
                infoMessage.value = "All transactions and import history were cleared."
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "Could not reset data"
            } finally {
                isResetting.value = false
            }
        }
    }

    fun createCustomLabel(name: String) {
        viewModelScope.launch {
            try {
                labelRepository.createCustomLabel(name)
                infoMessage.value = "Label \"$name\" created."
                errorMessage.value = null
            } catch (error: Exception) {
                errorMessage.value = error.message
            }
        }
    }

    fun deleteCustomLabel(labelId: Long) {
        viewModelScope.launch {
            labelRepository.deleteCustomLabel(labelId)
        }
    }

    fun clearMessages() {
        errorMessage.update { null }
        infoMessage.update { null }
        resetCompleted.update { false }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    resetLocalData = container.resetLocalData,
                    labelRepository = container.labelRepository,
                    importSmsInbox = container.importSmsInbox,
                    transactionRepository = container.transactionRepository,
                    smsScanPreferences = container.smsScanPreferences,
                    securityPreferences = container.securityPreferences,
                    securitySession = container.securitySession,
                    inboxReader = container.smsInboxReader,
                    applicationScope = container.applicationScope,
                )
            }
        }
    }
}
