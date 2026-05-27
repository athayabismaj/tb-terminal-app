package com.tbterminal.app.ui.receivables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReceivableViewModel(
    private val receivableRepository: ReceivableRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceivableUiState(isLoading = true))
    val uiState: StateFlow<ReceivableUiState> = _uiState.asStateFlow()

    init {
        loadReceivables()
    }

    fun loadReceivables(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = receivableRepository.getReceivables(
                    page = page,
                    limit = state.limit,
                    status = state.statusFilter.apiValue
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            receivables = result.data.data,
                            page = result.data.page,
                            limit = result.data.limit,
                            total = result.data.total,
                            totalPages = result.data.totalPages.coerceAtLeast(1),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }

                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Piutang pelanggan gagal dimuat.")
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChanged(filter: ReceivableStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter, page = 1) }
        loadReceivables(page = 1)
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) loadReceivables(previous)
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) loadReceivables(next)
    }

    fun openPayment(receivable: Receivable) {
        _uiState.update {
            it.copy(
                selectedReceivable = receivable,
                paymentAmountInput = receivable.remainingAmount.toInputText(),
                paymentMethod = ReceivablePaymentMethod.Cash,
                referenceInput = "",
                notesInput = "",
                errorMessage = null,
                message = null
            )
        }
    }

    fun closePayment() {
        _uiState.update { it.copy(selectedReceivable = null, isSubmittingPayment = false) }
    }

    fun onPaymentAmountChanged(value: String) {
        _uiState.update { it.copy(paymentAmountInput = value.decimalInput()) }
    }

    fun onPaymentMethodChanged(method: ReceivablePaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onReferenceChanged(value: String) {
        _uiState.update { it.copy(referenceInput = value) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notesInput = value) }
    }

    fun submitPayment() {
        val state = _uiState.value
        val receivable = state.selectedReceivable ?: return
        val amount = state.paymentAmountInput.toBigDecimalOrNull()
            ?: return setActionError("Nominal pembayaran wajib diisi dengan angka valid.")

        if (amount <= BigDecimal.ZERO) return setActionError("Nominal pembayaran harus lebih dari nol.")
        if (amount > receivable.remainingAmount) return setActionError("Pembayaran tidak boleh melebihi sisa piutang.")
        if (state.isSubmittingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, errorMessage = null, message = null) }
            val command = CreateReceivablePaymentCommand(
                receivableId = receivable.id,
                amount = amount,
                method = state.paymentMethod.apiValue,
                reference = state.referenceInput.trim().takeIf(String::isNotBlank),
                notes = state.notesInput.trim().takeIf(String::isNotBlank)
            )

            when (val result = receivableRepository.createReceivablePayment(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedReceivable = null,
                            isSubmittingPayment = false,
                            message = "Pembayaran piutang ${receivable.customerName} berhasil dicatat."
                        )
                    }
                    loadReceivables()
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Pembayaran gagal dicatat karena koneksi bermasalah.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isSubmittingPayment = false, errorMessage = message) }
    }

    companion object {
        fun factory(receivableRepository: ReceivableRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                ReceivableViewModel(receivableRepository)
            }
        }
    }
}

private fun String.decimalInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}

private fun BigDecimal.toInputText(): String {
    return stripTrailingZeros().toPlainString()
}
