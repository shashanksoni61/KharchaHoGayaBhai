package com.shashanksoni.kharchahogayabhai.feature.transactions.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.LabelRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val detail: TransactionDetail? = null,
    val categories: List<Category> = emptyList(),
    val allLabels: List<TransactionLabel> = emptyList(),
    val isLoading: Boolean = true,
    val labelError: String? = null,
)

class TransactionDetailViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val labelRepository: LabelRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val labelError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        transactionRepository.observeTransactionDetail(transactionId),
        categoryRepository.observeCategories(),
        labelRepository.observeLabels(),
        labelError,
    ) { detail, categories, labels, error ->
        TransactionDetailUiState(
            detail = detail,
            categories = categories,
            allLabels = labels,
            isLoading = false,
            labelError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionDetailUiState(),
    )

    fun setIgnored(ignored: Boolean) {
        viewModelScope.launch {
            transactionRepository.setIgnored(transactionId, ignored)
        }
    }

    fun changeCategory(categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.setCategory(transactionId, categoryId)
        }
    }

    fun toggleLabel(labelId: Long, currentlyAttached: Boolean) {
        viewModelScope.launch {
            labelRepository.setLabelOnTransaction(
                transactionId = transactionId,
                labelId = labelId,
                attached = !currentlyAttached,
            )
        }
    }

    fun createAndAttachCustomLabel(name: String) {
        viewModelScope.launch {
            try {
                val label = labelRepository.createCustomLabel(name)
                labelRepository.setLabelOnTransaction(transactionId, label.id, attached = true)
                labelError.value = null
            } catch (error: Exception) {
                labelError.value = error.message
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer, transactionId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    TransactionDetailViewModel(
                        transactionId = transactionId,
                        transactionRepository = container.transactionRepository,
                        labelRepository = container.labelRepository,
                        categoryRepository = container.categoryRepository,
                    )
                }
            }
    }
}
