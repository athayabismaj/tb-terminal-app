package com.tbterminal.app.ui.cashhistory

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

class CashSessionHistoryViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashSessionHistoryUiState())
    val uiState: StateFlow<CashSessionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val status = _uiState.value.statusFilter.takeUnless { it == "Semua" }
            when (val result = repository.getSessions(page, _uiState.value.pageSize, status)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        sessions = result.data.data,
                        page = result.data.page,
                        totalPages = result.data.totalPages.coerceAtLeast(1),
                        totalSessions = result.data.total,
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

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        loadSessions()
    }

    fun previousPage() {
        if (_uiState.value.page > 1) loadSessions(_uiState.value.page - 1)
    }

    fun nextPage() {
        if (_uiState.value.page < _uiState.value.totalPages) loadSessions(_uiState.value.page + 1)
    }

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CashSessionHistoryViewModel(repository) as T
                }
            }
        }
    }
}
