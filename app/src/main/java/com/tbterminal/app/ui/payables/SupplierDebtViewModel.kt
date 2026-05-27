package com.tbterminal.app.ui.payables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupplierDebtViewModel(
    private val purchasingRepository: PurchasingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierDebtUiState(isLoading = true))
    val uiState: StateFlow<SupplierDebtUiState> = _uiState.asStateFlow()

    init {
        loadPayables()
    }

    fun loadPayables(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = purchasingRepository.getPayables(
                    page = page,
                    limit = state.limit,
                    status = state.statusFilter.apiValue
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            payables = result.data.data,
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
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Utang supplier gagal dimuat.")
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChanged(filter: SupplierDebtStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter, page = 1) }
        loadPayables(page = 1)
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) loadPayables(previous)
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) loadPayables(next)
    }

    fun openPayment(payable: SupplierPayable) {
        _uiState.update {
            it.copy(
                selectedPayable = payable,
                paymentAmountInput = payable.remainingAmount.toInputText(),
                paymentMethod = SupplierPaymentMethod.Cash,
                referenceInput = "",
                notesInput = "",
                errorMessage = null,
                message = null
            )
        }
    }

    fun closePayment() {
        _uiState.update { it.copy(selectedPayable = null, isSubmittingPayment = false) }
    }

    fun onPaymentAmountChanged(value: String) {
        _uiState.update { it.copy(paymentAmountInput = value.decimalInput()) }
    }

    fun onPaymentMethodChanged(method: SupplierPaymentMethod) {
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
        val payable = state.selectedPayable ?: return
        val amount = state.paymentAmountInput.toBigDecimalOrNull()
            ?: return setActionError("Nominal pembayaran wajib diisi dengan angka valid.")

        if (amount <= BigDecimal.ZERO) return setActionError("Nominal pembayaran harus lebih dari nol.")
        if (amount > payable.remainingAmount) return setActionError("Pembayaran tidak boleh melebihi sisa utang.")
        if (state.isSubmittingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, errorMessage = null, message = null) }
            val command = CreateSupplierPaymentCommand(
                payableId = payable.id,
                amount = amount,
                method = state.paymentMethod.apiValue,
                reference = state.referenceInput.trim().takeIf(String::isNotBlank),
                notes = state.notesInput.trim().takeIf(String::isNotBlank)
            )

            when (val result = purchasingRepository.createSupplierPayment(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPayable = null,
                            isSubmittingPayment = false,
                            message = "Pembayaran utang ${payable.supplierName} berhasil dicatat."
                        )
                    }
                    loadPayables()
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
        fun factory(purchasingRepository: PurchasingRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                SupplierDebtViewModel(purchasingRepository)
            }
        }
    }
}

internal fun String.decimalInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}

internal fun BigDecimal.toInputText(): String {
    return stripTrailingZeros().toPlainString()
}
