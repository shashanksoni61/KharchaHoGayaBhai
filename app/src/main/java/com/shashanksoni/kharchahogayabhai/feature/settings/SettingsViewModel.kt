package com.shashanksoni.kharchahogayabhai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.repository.LabelRepository
import com.shashanksoni.kharchahogayabhai.domain.usecase.ResetLocalDataUseCase
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
    val resetCompleted: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class SettingsViewModel(
    private val resetLocalData: ResetLocalDataUseCase,
    private val labelRepository: LabelRepository,
) : ViewModel() {

    private val isResetting = MutableStateFlow(false)
    private val resetCompleted = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val infoMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        labelRepository.observeLabels(),
        isResetting,
        resetCompleted,
        errorMessage,
        infoMessage,
    ) { labels, resetting, completed, error, info ->
        SettingsUiState(
            labels = labels,
            isResetting = resetting,
            resetCompleted = completed,
            errorMessage = error,
            infoMessage = info,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

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
                )
            }
        }
    }
}
