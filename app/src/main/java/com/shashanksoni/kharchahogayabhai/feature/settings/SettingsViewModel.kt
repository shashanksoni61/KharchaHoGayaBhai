package com.shashanksoni.kharchahogayabhai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.ImportResult
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.repository.LabelRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.ImportSmsInboxUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.ResetLocalDataUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.SmsScanMode
import com.shashanksoni.kharchahogayabhai.sms.SmsScanPreferences
import java.time.Instant
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
    val resetCompleted: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class SettingsViewModel(
    private val resetLocalData: ResetLocalDataUseCase,
    private val labelRepository: LabelRepository,
    private val importSmsInbox: ImportSmsInboxUseCase,
    private val smsScanPreferences: SmsScanPreferences,
) : ViewModel() {

    private val isResetting = MutableStateFlow(false)
    private val isScanningSms = MutableStateFlow(false)
    private val resetCompleted = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val infoMessage = MutableStateFlow<String?>(null)

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
    ) { labels, smsPrefs, busy ->
        SettingsUiState(
            labels = labels,
            isResetting = busy.resetting,
            isScanningSms = busy.scanning,
            lastSmsScanAt = smsPrefs.lastScanMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli),
            autoSmsScanOnOpen = smsPrefs.autoOnOpen,
            listenSmsInBackground = smsPrefs.listenBackground,
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
        viewModelScope.launch {
            isScanningSms.value = true
            errorMessage.value = null
            try {
                val result = importSmsInbox(mode = mode, recordEmptyHistory = true)
                applySmsResult(result, fullRescan = mode == SmsScanMode.FULL)
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "SMS scan failed"
            } finally {
                isScanningSms.value = false
            }
        }
    }

    fun reportSmsPermissionDenied() {
        errorMessage.value =
            "SMS permission was denied. Allow SMS access to scan transaction alerts."
    }

    private fun applySmsResult(result: ImportResult, fullRescan: Boolean) {
        val batch = result.batch
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
                    smsScanPreferences = container.smsScanPreferences,
                )
            }
        }
    }
}
