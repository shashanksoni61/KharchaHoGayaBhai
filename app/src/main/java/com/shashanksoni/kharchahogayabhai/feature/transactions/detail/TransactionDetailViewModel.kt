package com.shashanksoni.kharchahogayabhai.feature.transactions.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import com.shashanksoni.kharchahogayabhai.domain.model.Category
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionDetail
import com.shashanksoni.kharchahogayabhai.domain.repository.CategoryRepository
import com.shashanksoni.kharchahogayabhai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val detail: TransactionDetail? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
)

class TransactionDetailViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        transactionRepository.observeTransactionDetail(transactionId),
        categoryRepository.observeCategories(),
    ) { detail, categories ->
        TransactionDetailUiState(detail = detail, categories = categories, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionDetailUiState(),
    )

    /** A user's choice always wins over whatever categorisation decides later. */
    fun changeCategory(categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.setCategory(transactionId, categoryId)
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
                        categoryRepository = container.categoryRepository,
                    )
                }
            }
    }
}
