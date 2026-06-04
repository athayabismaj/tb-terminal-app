package com.tbterminal.app.ui.receivablepayments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReceivablePaymentHistoryViewModel(
    private val receivableRepository: ReceivableRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceivablePaymentHistoryUiState())
    val uiState: StateFlow<ReceivablePaymentHistoryUiState> = _uiState.asStateFlow()

    init {
        loadPayments()
    }

    fun loadPayments(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val safePage = page.coerceAtLeast(1)
            _uiState.update { it.copy(page = safePage, isLoading = true, errorMessage = null) }
            when (
                val result = receivableRepository.getReceivablePayments(
                    page = safePage,
                    limit = _uiState.value.pageSize
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            payments = result.data.data,
                            page = result.data.page,
                            totalPages = result.data.totalPages.coerceAtLeast(1),
                            totalPayments = result.data.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> setError("Riwayat pembayaran gagal dimuat karena koneksi bermasalah.")
            }
        }
    }

    fun showDetail(payment: ReceivablePaymentHistory) {
        _uiState.update { it.copy(selectedPayment = payment) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedPayment = null) }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onMethodFilterChanged(filter: ReceivablePaymentMethodFilter) {
        _uiState.update { it.copy(methodFilter = filter) }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1 && !state.isLoading) loadPayments(state.page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages && !state.isLoading) loadPayments(state.page + 1)
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    companion object {
        fun factory(receivableRepository: ReceivableRepository): ViewModelProvider.Factory {
            return viewModelFactory { ReceivablePaymentHistoryViewModel(receivableRepository) }
        }
    }
}
