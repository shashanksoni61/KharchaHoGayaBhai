package com.shashanksoni.kharchahogayabhai.feature.importfile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import com.shashanksoni.kharchahogayabhai.domain.model.ImportResult
import com.shashanksoni.kharchahogayabhai.domain.repository.ImportRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.ImportSmsInboxUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.ImportStatementFileUseCase
import com.shashanksoni.kharchahogayabhai.domain.usecase.SmsScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportUiState(
    val history: List<ImportBatch> = emptyList(),
    val isImporting: Boolean = false,
    val lastResult: ImportResult? = null,
    val errorMessage: String? = null,
)

class ImportViewModel(
    private val importStatementFile: ImportStatementFileUseCase,
    private val importSmsInboxUseCase: ImportSmsInboxUseCase,
    importRepository: ImportRepository,
) : ViewModel() {

    private val isImporting = MutableStateFlow(false)
    private val lastResult = MutableStateFlow<ImportResult?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ImportUiState> = combine(
        importRepository.observeImportBatches(),
        isImporting,
        lastResult,
        errorMessage,
    ) { history, importing, result, error ->
        ImportUiState(
            history = history,
            isImporting = importing,
            lastResult = result,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ImportUiState(),
    )

    fun importFile(uri: Uri) {
        if (isImporting.value) return
        viewModelScope.launch {
            isImporting.value = true
            errorMessage.value = null
            try {
                val result = importStatementFile(uri)
                lastResult.value = result
                if (result.batch.errorMessage != null &&
                    result.batch.newCount == 0 &&
                    result.batch.mergedCount == 0
                ) {
                    errorMessage.value = result.batch.errorMessage
                }
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "Import failed"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun importSmsInbox() {
        if (isImporting.value) return
        viewModelScope.launch {
            isImporting.value = true
            errorMessage.value = null
            try {
                val result = importSmsInboxUseCase(
                    mode = SmsScanMode.INCREMENTAL,
                    recordEmptyHistory = true,
                )
                lastResult.value = result
                if (result.batch.errorMessage != null &&
                    result.batch.newCount == 0 &&
                    result.batch.mergedCount == 0
                ) {
                    errorMessage.value = result.batch.errorMessage
                }
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "SMS import failed"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun reportSmsPermissionDenied() {
        errorMessage.value =
            "SMS permission was denied. Allow SMS access in system settings to import transaction alerts."
    }

    fun clearMessages() {
        errorMessage.update { null }
        lastResult.update { null }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ImportViewModel(
                    importStatementFile = container.importStatementFile,
                    importSmsInboxUseCase = container.importSmsInbox,
                    importRepository = container.importRepository,
                )
            }
        }
    }
}
