package com.tbterminal.app.ui.cashexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CashExpenseHistoryViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashExpenseHistoryUiState())
    val uiState: StateFlow<CashExpenseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    fun loadExpenses(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getExpenseHistory(page, _uiState.value.pageSize)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        expenses = result.data.data,
                        page = result.data.page,
                        totalPages = result.data.totalPages.coerceAtLeast(1),
                        totalExpenses = result.data.total,
                        isLoading = false
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Koneksi ke server gagal.")
                }
            }
        }
    }

    fun previousPage() {
        if (_uiState.value.page > 1) loadExpenses(_uiState.value.page - 1)
    }

    fun nextPage() {
        if (_uiState.value.page < _uiState.value.totalPages) loadExpenses(_uiState.value.page + 1)
    }

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CashExpenseHistoryViewModel(repository) as T
                }
            }
        }
    }
}
