package com.tbterminal.app.ui.payables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateOpeningPayableCommand
import com.tbterminal.app.data.model.CreatePayableAdjustmentCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.time.LocalDate
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
        loadSuppliers()
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

    fun loadSuppliers() {
        viewModelScope.launch {
            when (val result = purchasingRepository.getSuppliers(page = 1, limit = 50)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(suppliers = result.data.data.filter { s -> s.isActive }) }
                }
                else -> Unit
            }
        }
    }

    fun refresh() = loadPayables(_uiState.value.page)

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChanged(filter: SupplierDebtStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter, page = 1) }
        loadPayables(page = 1)
    }

    fun onDueFilterChanged(filter: SupplierDebtDueFilter) {
        _uiState.update { it.copy(dueFilter = filter) }
    }

    fun onSortOrderChanged(order: SupplierDebtSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
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

    // Opening Balance
    fun openOpeningBalance() {
        val today = LocalDate.now().toString()
        _uiState.update {
            it.copy(
                isOpeningBalanceOpen = true,
                openingSupplierId = "",
                openingAmountInput = "",
                openingDebtDateInput = today,
                openingDueDateInput = LocalDate.now().plusDays(30).toString(),
                openingLegacyInvoiceInput = "",
                openingNotesInput = "",
                errorMessage = null,
                message = null
            )
        }
    }

    fun closeOpeningBalance() {
        _uiState.update { it.copy(isOpeningBalanceOpen = false, isSubmittingOpeningBalance = false) }
    }

    fun initOpeningBalanceForSupplier(supplierId: String?) {
        val sid = supplierId.orEmpty()
        _uiState.update { state ->
            state.copy(
                isOpeningBalanceOpen = true,
                openingSupplierId = sid,
                openingAmountInput = "",
                openingDebtDateInput = LocalDate.now().toString(),
                openingDueDateInput = LocalDate.now().plusDays(30).toString(),
                openingLegacyInvoiceInput = "",
                openingNotesInput = "",
                errorMessage = null,
                isOpeningBalanceSaved = false
            )
        }
        if (sid.isNotBlank()) {
            loadSingleSupplierIfNeeded(sid)
            loadSupplierDebtsForAdjustment(sid)
        }
    }

    fun resetOpeningBalanceSaved() {
        _uiState.update { it.copy(isOpeningBalanceSaved = false) }
    }

    fun onOpeningSupplierChanged(value: String) = _uiState.update { it.copy(openingSupplierId = value) }

    fun onOpeningSupplierSelected(supplier: Supplier) = _uiState.update { state ->
        val updated = if (state.suppliers.none { it.id == supplier.id }) state.suppliers + supplier else state.suppliers
        state.copy(openingSupplierId = supplier.id, suppliers = updated)
    }

    fun onOpeningAmountChanged(value: String) = _uiState.update { it.copy(openingAmountInput = value.decimalInput()) }
    fun onOpeningDebtDateChanged(value: String) = _uiState.update { it.copy(openingDebtDateInput = value) }
    fun onOpeningDueDateChanged(value: String) = _uiState.update { it.copy(openingDueDateInput = value) }
    fun onOpeningLegacyInvoiceChanged(value: String) =
        _uiState.update { it.copy(openingLegacyInvoiceInput = value.take(100)) }
    fun onOpeningNotesChanged(value: String) = _uiState.update { it.copy(openingNotesInput = value.take(1000)) }

    fun submitOpeningBalance() {
        val state = _uiState.value
        if (state.isSubmittingOpeningBalance) return
        val validated = validateOpeningSupplierDebtInput(
            supplierId = state.openingSupplierId,
            amountInput = state.openingAmountInput,
            debtDateInput = state.openingDebtDateInput,
            dueDateInput = state.openingDueDateInput
        ).getOrElse { error ->
            setOpeningError(error.message ?: "Data saldo awal hutang belum valid.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOpeningBalance = true, errorMessage = null, message = null) }
            val command = CreateOpeningPayableCommand(
                supplierId = state.openingSupplierId,
                amount = validated.amount,
                debtDate = validated.debtDate.toString(),
                dueDate = validated.dueDate.toString(),
                legacyInvoiceNumber = state.openingLegacyInvoiceInput.trim().takeIf(String::isNotBlank),
                notes = state.openingNotesInput.trim().takeIf(String::isNotBlank)
            )
            when (val result = purchasingRepository.createOpeningBalance(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isOpeningBalanceOpen = false,
                            isSubmittingOpeningBalance = false,
                            isOpeningBalanceSaved = true,
                            message = "Saldo awal hutang ${result.data.supplierName} berhasil dicatat."
                        )
                    }
                    loadPayables(page = 1)
                }
                is RepositoryResult.Error -> setOpeningError(result.message)
                is RepositoryResult.Exception -> setOpeningError("Saldo awal hutang gagal dicatat karena koneksi bermasalah.")
            }
        }
    }

    // Adjustment
    fun openAdjustment() {
        val today = LocalDate.now().toString()
        _uiState.update {
            it.copy(
                isAdjustmentOpen = true,
                adjustmentSupplierId = "",
                adjustmentAmountInput = "",
                adjustmentDebtDateInput = today,
                adjustmentDueDateInput = LocalDate.now().plusDays(30).toString(),
                adjustmentReferenceInput = "",
                adjustmentReasonInput = "",
                errorMessage = null,
                message = null
            )
        }
    }

    fun closeAdjustment() = _uiState.update {
        it.copy(isAdjustmentOpen = false, isSubmittingAdjustment = false)
    }

    fun initAdjustmentForSupplier(supplierId: String?) {
        val sid = supplierId.orEmpty()
        _uiState.update { state ->
            state.copy(
                isAdjustmentOpen = true,
                adjustmentSupplierId = sid,
                adjustmentAmountInput = "",
                adjustmentDebtDateInput = LocalDate.now().toString(),
                adjustmentDueDateInput = LocalDate.now().plusDays(30).toString(),
                adjustmentReferenceInput = "",
                adjustmentReasonInput = "",
                errorMessage = null,
                isAdjustmentSaved = false
            )
        }
        if (sid.isNotBlank()) {
            loadSingleSupplierIfNeeded(sid)
        }
    }

    fun resetAdjustmentSaved() {
        _uiState.update { it.copy(isAdjustmentSaved = false) }
    }

    fun onAdjustmentSupplierChanged(value: String) {
        _uiState.update { it.copy(adjustmentSupplierId = value) }
        loadSupplierDebtsForAdjustment(value)
    }

    fun onAdjustmentSupplierSelected(supplier: Supplier) {
        _uiState.update { state ->
            val updated = if (state.suppliers.none { it.id == supplier.id }) state.suppliers + supplier else state.suppliers
            state.copy(adjustmentSupplierId = supplier.id, suppliers = updated)
        }
        loadSupplierDebtsForAdjustment(supplier.id)
    }

    fun onSelectDebtForAdjustment(debt: SupplierPayable) {
        val debtRef = debt.purchaseId.takeIf(String::isNotBlank) ?: "FAKTUR-${debt.id.takeLast(6).uppercase()}"
        _uiState.update { state ->
            state.copy(
                adjustmentReferenceInput = debtRef,
                adjustmentAmountInput = debt.remainingAmount.toInputText(),
                adjustmentDueDateInput = debt.dueDate.take(10).ifBlank { state.adjustmentDueDateInput }
            )
        }
    }

    fun loadSupplierDebtsForAdjustment(supplierId: String) {
        val sid = supplierId.trim()
        if (sid.isBlank()) {
            _uiState.update { it.copy(supplierActiveDebts = emptyList(), isLoadingSupplierDebts = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSupplierDebts = true) }
            when (val result = purchasingRepository.getPayables(page = 1, limit = 50, supplierId = sid)) {
                is RepositoryResult.Success -> {
                    val activeDebts = result.data.data.filter { it.remainingAmount > BigDecimal.ZERO }
                    _uiState.update { it.copy(supplierActiveDebts = activeDebts, isLoadingSupplierDebts = false) }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingSupplierDebts = false) }
                }
            }
        }
    }

    fun onAdjustmentAmountChanged(value: String) = _uiState.update { it.copy(adjustmentAmountInput = value.decimalInput()) }
    fun onAdjustmentDebtDateChanged(value: String) = _uiState.update { it.copy(adjustmentDebtDateInput = value) }
    fun onAdjustmentDueDateChanged(value: String) = _uiState.update { it.copy(adjustmentDueDateInput = value) }
    fun onAdjustmentReferenceChanged(value: String) = _uiState.update { it.copy(adjustmentReferenceInput = value.take(100)) }
    fun onAdjustmentReasonChanged(value: String) = _uiState.update { it.copy(adjustmentReasonInput = value.take(1000)) }

    fun submitAdjustment() {
        val state = _uiState.value
        if (state.isSubmittingAdjustment) return
        val validated = validateAdjustmentSupplierDebtInput(
            supplierId = state.adjustmentSupplierId,
            activeSupplierIds = state.suppliers.filter { it.isActive }.mapTo(mutableSetOf()) { it.id },
            amountInput = state.adjustmentAmountInput,
            debtDateInput = state.adjustmentDebtDateInput,
            dueDateInput = state.adjustmentDueDateInput,
            referenceInput = state.adjustmentReferenceInput,
            reasonInput = state.adjustmentReasonInput
        ).getOrElse {
            setAdjustmentError(it.message ?: "Data penyesuaian hutang belum valid.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAdjustment = true, errorMessage = null, message = null) }
            val command = CreatePayableAdjustmentCommand(
                supplierId = state.adjustmentSupplierId,
                amount = validated.amount,
                debtDate = validated.debtDate.toString(),
                dueDate = validated.dueDate.toString(),
                reference = validated.reference,
                reason = validated.reason
            )
            when (val result = purchasingRepository.createAdjustment(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAdjustmentOpen = false,
                            isSubmittingAdjustment = false,
                            isAdjustmentSaved = true,
                            message = "Penyesuaian hutang ${result.data.supplierName} berhasil dicatat."
                        )
                    }
                    loadPayables(page = 1)
                }
                is RepositoryResult.Error -> setAdjustmentError(result.message)
                is RepositoryResult.Exception -> setAdjustmentError("Penyesuaian hutang gagal dicatat karena koneksi bermasalah.")
            }
        }
    }

    private fun loadSingleSupplierIfNeeded(supplierId: String) {
        if (_uiState.value.suppliers.any { it.id == supplierId }) return
        viewModelScope.launch {
            when (val result = purchasingRepository.getSuppliers(page = 1, limit = 50)) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        val found = result.data.data.find { it.id == supplierId }
                        if (found != null && state.suppliers.none { it.id == found.id }) {
                            state.copy(suppliers = state.suppliers + found)
                        } else state
                    }
                }
                else -> Unit
            }
        }
    }

    fun setMessage(msg: String) {
        _uiState.update { it.copy(message = msg, errorMessage = null) }
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

    private fun setOpeningError(message: String) {
        _uiState.update { it.copy(isSubmittingOpeningBalance = false, errorMessage = message) }
    }

    private fun setAdjustmentError(message: String) {
        _uiState.update { it.copy(isSubmittingAdjustment = false, errorMessage = message) }
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
