package com.tbterminal.app.ui.receivablepayments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReverseReceivablePaymentCommand
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

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
                    limit = _uiState.value.pageSize,
                    receivableId = _uiState.value.receivableIdFilter.trim().takeIf(String::isNotBlank),
                    method = _uiState.value.methodFilter.apiValue(),
                    customerSearch = _uiState.value.searchQuery.trim().takeIf(String::isNotBlank),
                    receiverSearch = _uiState.value.receiverSearch.trim().takeIf(String::isNotBlank),
                    status = _uiState.value.statusFilter.apiValue,
                    dateFrom = _uiState.value.dateFrom.takeIf(String::isNotBlank),
                    dateTo = _uiState.value.dateTo.takeIf(String::isNotBlank)
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

    fun refresh() = loadPayments(_uiState.value.page)

    fun showDetail(payment: ReceivablePaymentHistory) {
        viewModelScope.launch {
            when (val result = receivableRepository.getReceivablePaymentReceipt(payment.id)) {
                is RepositoryResult.Success -> _uiState.update { it.copy(selectedPayment = result.data, errorMessage = null) }
                else -> _uiState.update { it.copy(selectedPayment = payment) }
            }
        }
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

    fun onReceiverSearchChanged(value: String) = _uiState.update { it.copy(receiverSearch = value.take(100)) }
    fun onReceivableIdChanged(value: String) = _uiState.update { it.copy(receivableIdFilter = value.take(36)) }
    fun onDateFromChanged(value: String) = _uiState.update { it.copy(dateFrom = value.take(10)) }
    fun onDateToChanged(value: String) = _uiState.update { it.copy(dateTo = value.take(10)) }
    fun onStatusFilterChanged(value: ReceivablePaymentStatusFilter) = _uiState.update { it.copy(statusFilter = value) }

    fun applyFilters() {
        val state = _uiState.value
        val from = state.dateFrom.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val to = state.dateTo.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (state.dateFrom.isNotBlank() && from == null || state.dateTo.isNotBlank() && to == null) {
            return setError("Format tanggal filter harus yyyy-MM-dd.")
        }
        if (from != null && to != null && from > to) return setError("Tanggal awal tidak boleh setelah tanggal akhir.")
        loadPayments(1)
    }

    fun openReversal(payment: ReceivablePaymentHistory) {
        _uiState.update {
            it.copy(
                paymentToReverse = payment,
                reversalReason = "",
                reversalIdempotencyKey = "receivable-reversal-${UUID.randomUUID()}",
                errorMessage = null
            )
        }
    }

    fun onReversalReasonChanged(value: String) = _uiState.update { it.copy(reversalReason = value.take(1000)) }

    fun dismissReversal() = _uiState.update { it.copy(paymentToReverse = null, isReversing = false) }

    fun dismissReversalReceipt() = _uiState.update { it.copy(lastReversalReceipt = null) }

    fun submitReversal() {
        val state = _uiState.value
        val payment = state.paymentToReverse ?: return
        val reason = state.reversalReason.trim()
        if (reason.length < 5) return setError("Alasan reversal minimal 5 karakter.")
        if (state.isReversing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReversing = true, errorMessage = null) }
            when (val result = receivableRepository.reverseReceivablePayment(
                ReverseReceivablePaymentCommand(
                    paymentId = payment.id,
                    reason = reason,
                    idempotencyKey = state.reversalIdempotencyKey
                )
            )) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            paymentToReverse = null,
                            selectedPayment = null,
                            isReversing = false,
                            lastReversalReceipt = result.data,
                            message = "Reversal ${result.data.paymentNumber} berhasil dicatat."
                        )
                    }
                    loadPayments(1)
                }
                is RepositoryResult.Error -> _uiState.update { it.copy(isReversing = false, errorMessage = result.message) }
                is RepositoryResult.Exception -> _uiState.update { it.copy(isReversing = false, errorMessage = "Reversal gagal karena koneksi bermasalah.") }
            }
        }
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
