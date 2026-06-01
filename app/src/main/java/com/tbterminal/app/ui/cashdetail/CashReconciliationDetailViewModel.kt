package com.tbterminal.app.ui.cashdetail

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

class CashReconciliationDetailViewModel(
    private val sessionId: String,
    private val repository: CashReconciliationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashReconciliationDetailUiState(sessionId = sessionId))
    val uiState: StateFlow<CashReconciliationDetailUiState> = _uiState.asStateFlow()

    init {
        if (sessionId.isNotBlank()) loadDetail()
    }

    fun loadDetail(page: Int = 1) {
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val session = loadSession() ?: return@launch
            val expenses = loadExpenses() ?: return@launch
            loadTransactions(session, expenses, page)
        }
    }

    private suspend fun loadSession() = when (val result = repository.getSessionById(sessionId)) {
        is RepositoryResult.Success -> result.data
        is RepositoryResult.Error -> fail(result.message)
        is RepositoryResult.Exception -> fail("Koneksi ke server gagal.")
    }

    private suspend fun loadExpenses() = when (val result = repository.getExpenses(sessionId)) {
        is RepositoryResult.Success -> result.data
        is RepositoryResult.Error -> fail(result.message)
        is RepositoryResult.Exception -> fail("Koneksi ke server gagal.")
    }

    private suspend fun loadTransactions(
        session: com.tbterminal.app.data.model.CashSession,
        expenses: List<com.tbterminal.app.data.model.CashExpense>,
        page: Int
    ) {
        when (val result = repository.getTransactions(page, 10, sessionId)) {
            is RepositoryResult.Success -> _uiState.update {
                it.copy(
                    session = session,
                    expenses = expenses,
                    transactions = result.data.data,
                    page = result.data.page,
                    totalPages = result.data.totalPages.coerceAtLeast(1),
                    totalTransactions = result.data.total,
                    isLoading = false
                )
            }
            is RepositoryResult.Error -> fail(result.message)
            is RepositoryResult.Exception -> fail("Koneksi ke server gagal.")
        }
    }

    private fun fail(message: String): Nothing? {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
        return null
    }

    fun previousPage() {
        if (_uiState.value.page > 1) loadDetail(_uiState.value.page - 1)
    }

    fun nextPage() {
        if (_uiState.value.page < _uiState.value.totalPages) loadDetail(_uiState.value.page + 1)
    }

    companion object {
        fun factory(sessionId: String, repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CashReconciliationDetailViewModel(sessionId, repository) as T
                }
            }
        }
    }
}
