package com.tbterminal.app.ui.receivables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.CreateOpeningReceivableCommand
import com.tbterminal.app.data.model.CreateReceivableAdjustmentCommand
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability

class ReceivableViewModel(
    private val receivableRepository: ReceivableRepository,
    private val customerRepository: CustomerRepository,
    role: String
) : ViewModel() {
    private val canAdjust = canManageReceivableAdjustment(role)
    private val _uiState = MutableStateFlow(ReceivableUiState(isLoading = true))
    val uiState: StateFlow<ReceivableUiState> = _uiState.asStateFlow()

    init {
        loadReceivables()
        loadCustomerSummaries()
        loadCustomers()
    }

    fun loadReceivables(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = receivableRepository.getReceivables(
                    page = page,
                    limit = state.limit,
                    status = state.statusFilter.apiValue,
                    dueFilter = state.dueFilter.apiValue
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

    fun refresh() = loadReceivables(_uiState.value.page)

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChanged(filter: ReceivableStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter, page = 1) }
        loadReceivables(page = 1)
    }

    fun onDueFilterChanged(filter: ReceivableDueFilter) {
        _uiState.update { it.copy(dueFilter = filter, page = 1) }
        loadReceivables(page = 1)
        loadCustomerSummaries()
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
                paymentIdempotencyKey = "receivable-pay-${UUID.randomUUID()}",
                lastPaymentReceipt = null,
                errorMessage = null,
                message = null
            )
        }
    }

    fun closePayment() {
        _uiState.update { it.copy(selectedReceivable = null, isSubmittingPayment = false) }
    }

    fun dismissPaymentReceipt() {
        _uiState.update { it.copy(lastPaymentReceipt = null) }
    }

    fun onPaymentAmountChanged(value: String) {
        _uiState.update { it.copy(paymentAmountInput = value.decimalInput()) }
    }

    fun openOpeningBalance() {
        _uiState.update {
            it.copy(
                isOpeningBalanceOpen = true,
                openingCustomerId = "",
                openingAmountInput = "",
                openingDebtDateInput = java.time.LocalDate.now().toString(),
                openingDueDateInput = java.time.LocalDate.now().toString(),
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

    fun openAdjustment() {
        if (!canAdjust) return setActionError("Hanya owner atau admin yang dapat membuat adjustment piutang.")
        val today = java.time.LocalDate.now().toString()
        _uiState.update {
            it.copy(
                isAdjustmentOpen = true,
                adjustmentCustomerId = "",
                adjustmentAmountInput = "",
                adjustmentDebtDateInput = today,
                adjustmentDueDateInput = today,
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

    fun onAdjustmentCustomerChanged(value: String) = _uiState.update { it.copy(adjustmentCustomerId = value) }
    fun onAdjustmentAmountChanged(value: String) = _uiState.update { it.copy(adjustmentAmountInput = value.decimalInput()) }
    fun onAdjustmentDebtDateChanged(value: String) = _uiState.update { it.copy(adjustmentDebtDateInput = value) }
    fun onAdjustmentDueDateChanged(value: String) = _uiState.update { it.copy(adjustmentDueDateInput = value) }
    fun onAdjustmentReferenceChanged(value: String) = _uiState.update { it.copy(adjustmentReferenceInput = value.take(100)) }
    fun onAdjustmentReasonChanged(value: String) = _uiState.update { it.copy(adjustmentReasonInput = value.take(1000)) }

    fun submitAdjustment() {
        if (!canAdjust) return setActionError("Hanya owner atau admin yang dapat membuat adjustment piutang.")
        val state = _uiState.value
        if (state.isSubmittingAdjustment) return
        val validated = validateAdjustmentReceivableInput(
            customerId = state.adjustmentCustomerId,
            activeCustomerIds = state.customers.filter { it.isActive }.mapTo(mutableSetOf()) { it.id },
            amountInput = state.adjustmentAmountInput,
            debtDateInput = state.adjustmentDebtDateInput,
            dueDateInput = state.adjustmentDueDateInput,
            referenceInput = state.adjustmentReferenceInput,
            reasonInput = state.adjustmentReasonInput
        ).getOrElse {
            setAdjustmentError(it.message ?: "Data adjustment belum valid.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAdjustment = true, errorMessage = null, message = null) }
            val command = CreateReceivableAdjustmentCommand(
                customerId = state.adjustmentCustomerId,
                amount = validated.amount,
                debtDate = validated.debtDate.toString(),
                dueDate = validated.dueDate.toString(),
                reference = validated.reference,
                reason = validated.reason
            )
            when (val result = receivableRepository.createAdjustment(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAdjustmentOpen = false,
                            isSubmittingAdjustment = false,
                            message = "Adjustment piutang ${result.data.customerName} berhasil dicatat."
                        )
                    }
                    loadReceivables(page = 1)
                    loadCustomerSummaries()
                    refreshReceivableDetail(result.data.id)
                }
                is RepositoryResult.Error -> setAdjustmentError(result.message)
                is RepositoryResult.Exception -> setAdjustmentError("Adjustment gagal dicatat karena koneksi bermasalah.")
            }
        }
    }

    fun onOpeningCustomerChanged(value: String) = _uiState.update { it.copy(openingCustomerId = value) }
    fun onOpeningAmountChanged(value: String) = _uiState.update { it.copy(openingAmountInput = value.decimalInput()) }
    fun onOpeningDebtDateChanged(value: String) = _uiState.update { it.copy(openingDebtDateInput = value) }
    fun onOpeningDueDateChanged(value: String) = _uiState.update { it.copy(openingDueDateInput = value) }
    fun onOpeningLegacyInvoiceChanged(value: String) =
        _uiState.update { it.copy(openingLegacyInvoiceInput = value.take(100)) }
    fun onOpeningNotesChanged(value: String) = _uiState.update { it.copy(openingNotesInput = value.take(1000)) }

    fun submitOpeningBalance() {
        val state = _uiState.value
        if (state.isSubmittingOpeningBalance) return
        val validated = validateOpeningReceivableInput(
            state.openingCustomerId,
            state.openingAmountInput,
            state.openingDebtDateInput,
            state.openingDueDateInput
        ).getOrElse { error ->
            setActionError(error.message ?: "Data saldo awal piutang belum valid.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOpeningBalance = true, errorMessage = null, message = null) }
            val command = CreateOpeningReceivableCommand(
                customerId = state.openingCustomerId,
                amount = validated.amount,
                debtDate = validated.debtDate.toString(),
                dueDate = validated.dueDate.toString(),
                legacyInvoiceNumber = state.openingLegacyInvoiceInput.trim().takeIf(String::isNotBlank),
                notes = state.openingNotesInput.trim().takeIf(String::isNotBlank)
            )
            when (val result = receivableRepository.createOpeningBalance(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isOpeningBalanceOpen = false,
                            isSubmittingOpeningBalance = false,
                            message = "Saldo awal piutang ${result.data.customerName} berhasil dicatat."
                        )
                    }
                    loadReceivables(page = 1)
                    loadCustomerSummaries()
                }
                is RepositoryResult.Error -> setOpeningError(result.message)
                is RepositoryResult.Exception -> setOpeningError("Saldo awal gagal dicatat karena koneksi bermasalah.")
            }
        }
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
        val amount = validateReceivablePaymentInput(
            state.paymentAmountInput,
            receivable.remainingAmount,
            state.paymentIdempotencyKey
        ).getOrElse { return setActionError(it.message ?: "Pembayaran belum valid.") }
        if (state.isSubmittingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, errorMessage = null, message = null) }
            val command = CreateReceivablePaymentCommand(
                receivableId = receivable.id,
                amount = amount,
                method = state.paymentMethod.apiValue,
                reference = state.referenceInput.trim().takeIf(String::isNotBlank),
                notes = state.notesInput.trim().takeIf(String::isNotBlank),
                idempotencyKey = state.paymentIdempotencyKey.ifBlank {
                    "receivable-pay-${UUID.randomUUID()}"
                }
            )

            when (val result = receivableRepository.createReceivablePayment(command)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedReceivable = null,
                            isSubmittingPayment = false,
                            lastPaymentReceipt = result.data,
                            message = "Pembayaran piutang ${receivable.customerName} berhasil dicatat."
                        )
                    }
                    loadReceivables()
                    loadCustomerSummaries()
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

    private fun setOpeningError(message: String) {
        _uiState.update { it.copy(isSubmittingOpeningBalance = false, errorMessage = message) }
    }

    private fun setAdjustmentError(message: String) {
        _uiState.update { it.copy(isSubmittingAdjustment = false, errorMessage = message) }
    }

    private fun refreshReceivableDetail(id: String) {
        viewModelScope.launch {
            when (val result = receivableRepository.getReceivableById(id)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    val rows = state.receivables.toMutableList()
                    val index = rows.indexOfFirst { it.id == id }
                    if (index >= 0) rows[index] = result.data else rows.add(0, result.data)
                    state.copy(receivables = rows)
                }
                else -> Unit
            }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            when (val result = customerRepository.getCustomers(page = 1, limit = 100)) {
                is RepositoryResult.Success -> _uiState.update { it.copy(customers = result.data.data.filter { row -> row.isActive }) }
                else -> Unit
            }
        }
    }

    private fun loadCustomerSummaries() {
        viewModelScope.launch {
            val state = _uiState.value
            when (val result = receivableRepository.getCustomerSummaries(
                page = 1,
                limit = 100,
                dueFilter = state.dueFilter.apiValue
            )) {
                is RepositoryResult.Success -> _uiState.update { it.copy(customerSummaries = result.data.data) }
                else -> Unit
            }
        }
    }

    companion object {
        fun factory(
            receivableRepository: ReceivableRepository,
            customerRepository: CustomerRepository,
            role: String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                ReceivableViewModel(receivableRepository, customerRepository, role)
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

internal fun canManageReceivableAdjustment(role: String): Boolean =
    AppAccessPolicy.can(role, AppCapability.ADJUST_RECEIVABLES)
