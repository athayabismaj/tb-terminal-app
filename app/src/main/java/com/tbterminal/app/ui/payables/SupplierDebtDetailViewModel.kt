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

internal data class SupplierDebtDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val payables: List<SupplierPayable> = emptyList(),
    val selectedPayable: SupplierPayable? = null,
    val paymentAmountInput: String = "",
    val paymentMethod: SupplierPaymentMethod = SupplierPaymentMethod.Cash,
    val referenceInput: String = "",
    val notesInput: String = "",
    val isSubmittingPayment: Boolean = false,
    val message: String? = null
) {
    val supplierName: String
        get() = payables.firstOrNull()?.supplierName ?: "Supplier"

    val totalAmount: BigDecimal
        get() = payables.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }

    val totalPaid: BigDecimal
        get() = payables.fold(BigDecimal.ZERO) { sum, item -> sum + item.paidAmount }

    val totalRemaining: BigDecimal
        get() = payables.fold(BigDecimal.ZERO) { sum, item -> sum + item.remainingAmount }

    fun toSupplierDebtUiState(): SupplierDebtUiState = SupplierDebtUiState(
        payables = payables,
        selectedPayable = selectedPayable,
        paymentAmountInput = paymentAmountInput,
        paymentMethod = paymentMethod,
        referenceInput = referenceInput,
        notesInput = notesInput,
        isSubmittingPayment = isSubmittingPayment,
        errorMessage = errorMessage,
        message = message
    )
}

internal class SupplierDebtDetailViewModel(
    private val purchasingRepository: PurchasingRepository,
    private val supplierId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierDebtDetailUiState())
    val uiState: StateFlow<SupplierDebtDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val directResult = purchasingRepository.getPayables(
                page = 1,
                limit = DETAIL_LIMIT,
                supplierId = supplierId.takeIf(String::isNotBlank)
            )

            if (directResult is RepositoryResult.Success && directResult.data.data.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        payables = directResult.data.data
                    )
                }
            } else {
                // Fallback: load all payables and filter locally by ID or Name
                val allResult = purchasingRepository.getPayables(
                    page = 1,
                    limit = DETAIL_LIMIT
                )
                if (allResult is RepositoryResult.Success) {
                    val filtered = allResult.data.data.filter {
                        it.supplierId.equals(supplierId, ignoreCase = true) ||
                            it.supplierName.equals(supplierId, ignoreCase = true)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            payables = if (filtered.isNotEmpty()) filtered else (directResult as? RepositoryResult.Success)?.data?.data.orEmpty()
                        )
                    }
                } else {
                    val errorMsg = (directResult as? RepositoryResult.Error)?.message
                        ?: (allResult as? RepositoryResult.Error)?.message
                        ?: "Detail hutang supplier gagal dimuat."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        }
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
                            message = "Pembayaran hutang ${payable.supplierName} berhasil dicatat."
                        )
                    }
                    load()
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Pembayaran gagal dicatat karena koneksi bermasalah.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isSubmittingPayment = false, errorMessage = message) }
    }

    companion object {
        private const val DETAIL_LIMIT = 100

        fun factory(
            purchasingRepository: PurchasingRepository,
            supplierId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            SupplierDebtDetailViewModel(purchasingRepository, supplierId)
        }
    }
}
