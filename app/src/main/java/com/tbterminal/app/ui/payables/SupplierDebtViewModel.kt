package com.tbterminal.app.ui.payables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.model.CreateOpeningPayableCommand
import com.tbterminal.app.data.model.CreatePayableAdjustmentCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.remote.UserFacingErrorMapper
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.runSuspendCatching
import com.tbterminal.app.ui.common.parseWholeRupiahOrExactRemainingInput
import com.tbterminal.app.ui.common.sanitizeWholeRupiahInput
import com.tbterminal.app.ui.common.toNormalizedIdempotencyNumber
import com.tbterminal.app.ui.common.toWholeRupiahInputText
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SupplierPaymentDraftJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
internal data class SupplierPaymentIntent(
    val payableId: String,
    val normalizedAmount: String,
    val method: String,
    val reference: String?,
    val notes: String?
)

internal data class SupplierPaymentDraft(
    val payableId: String,
    val amountInput: String,
    val method: SupplierPaymentMethod,
    val referenceInput: String,
    val notesInput: String
)

@Serializable
internal data class PersistedSupplierPaymentDraft(
    val payableId: String,
    val amountInput: String,
    val methodApiValue: String,
    val referenceInput: String = "",
    val notesInput: String = ""
) {
    fun toDomainOrNull(): SupplierPaymentDraft? {
        if (payableId.isBlank()) return null
        val method = SupplierPaymentMethod.entries.firstOrNull {
            it.apiValue.equals(methodApiValue, ignoreCase = true) ||
                it.name.equals(methodApiValue, ignoreCase = true)
        } ?: SupplierPaymentMethod.Cash
        return SupplierPaymentDraft(
            payableId = payableId,
            amountInput = amountInput,
            method = method,
            referenceInput = referenceInput,
            notesInput = notesInput
        )
    }
}

internal fun SupplierPaymentDraft.toPersistedJson(): String {
    return SupplierPaymentDraftJson.encodeToString(
        PersistedSupplierPaymentDraft(
            payableId = payableId,
            amountInput = amountInput,
            methodApiValue = method.apiValue,
            referenceInput = referenceInput,
            notesInput = notesInput
        )
    )
}

internal fun decodeSupplierPaymentDraft(payloadJson: String?): SupplierPaymentDraft? {
    val raw = payloadJson?.takeIf(String::isNotBlank) ?: return null
    return runSuspendCatching {
        SupplierPaymentDraftJson.decodeFromString<PersistedSupplierPaymentDraft>(raw).toDomainOrNull()
    }.getOrNull()
}

class SupplierDebtViewModel(
    private val purchasingRepository: PurchasingRepository,
    private val idempotencyKeyGenerator: () -> String = { UUID.randomUUID().toString() },
    private val pendingSubmissionStore: PendingSubmissionStore? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierDebtUiState(isLoading = true))
    val uiState: StateFlow<SupplierDebtUiState> = _uiState.asStateFlow()

    private val paymentCoordinator = SupplierPaymentIdempotencyCoordinator(
        keyGenerator = idempotencyKeyGenerator,
        store = pendingSubmissionStore,
        clockMillis = clockMillis,
    )
    private var hasEditedCurrentPaymentDialog = false
    private val syncJobsByPayable = mutableMapOf<String, Job>()
    private val restoreJob: Job

    init {
        restoreJob = viewModelScope.launch {
            paymentCoordinator.restoreAll()
        }
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
        hasEditedCurrentPaymentDialog = false
        val restoreDraft = paymentCoordinator.getRestorableDraft(payable.id)
        _uiState.update {
            it.copy(
                selectedPayable = payable,
                paymentAmountInput = restoreDraft?.amountInput ?: payable.remainingAmount.toWholeRupiahInputText(),
                paymentMethod = restoreDraft?.method ?: SupplierPaymentMethod.Cash,
                referenceInput = restoreDraft?.referenceInput.orEmpty(),
                notesInput = restoreDraft?.notesInput.orEmpty(),
                errorMessage = null,
                message = null
            )
        }
        if (pendingSubmissionStore != null) {
            syncJobsByPayable[payable.id] = viewModelScope.launch {
                restoreJob.join()
                val syncedDraft = paymentCoordinator.syncPayableFromStore(payable.id)
                if (_uiState.value.selectedPayable?.id == payable.id && !hasEditedCurrentPaymentDialog) {
                    _uiState.update {
                        it.copy(
                            paymentAmountInput = syncedDraft?.amountInput ?: payable.remainingAmount.toWholeRupiahInputText(),
                            paymentMethod = syncedDraft?.method ?: SupplierPaymentMethod.Cash,
                            referenceInput = syncedDraft?.referenceInput.orEmpty(),
                            notesInput = syncedDraft?.notesInput.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun closePayment() {
        val closingPayableId = _uiState.value.selectedPayable?.id
        if (closingPayableId != null) {
            paymentCoordinator.onDialogDismissedInMemory(closingPayableId)
            viewModelScope.launch {
                syncJobsByPayable[closingPayableId]?.join()
                paymentCoordinator.persistDismissalIfNotAmbiguous(closingPayableId)
            }
        }
        _uiState.update { it.copy(selectedPayable = null, isSubmittingPayment = false) }
    }

    fun onPaymentAmountChanged(value: String) {
        hasEditedCurrentPaymentDialog = true
        _uiState.update { it.copy(paymentAmountInput = sanitizeWholeRupiahInput(value)) }
    }

    fun onPaymentMethodChanged(method: SupplierPaymentMethod) {
        hasEditedCurrentPaymentDialog = true
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onReferenceChanged(value: String) {
        hasEditedCurrentPaymentDialog = true
        _uiState.update { it.copy(referenceInput = value) }
    }

    fun onNotesChanged(value: String) {
        hasEditedCurrentPaymentDialog = true
        _uiState.update { it.copy(notesInput = value) }
    }

    fun submitPayment() {
        val state = _uiState.value
        val payable = state.selectedPayable ?: return
        val amount = parseWholeRupiahOrExactRemainingInput(state.paymentAmountInput, payable.remainingAmount)
            ?: return setActionError("Nominal pembayaran wajib diisi dengan angka valid.")

        if (amount <= BigDecimal.ZERO) return setActionError("Nominal pembayaran harus lebih dari nol.")
        if (amount > payable.remainingAmount) return setActionError("Pembayaran tidak boleh melebihi sisa utang.")
        if (state.isSubmittingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, errorMessage = null, message = null) }
            restoreJob.join()
            syncJobsByPayable[payable.id]?.join()
            val normalizedReference = state.referenceInput.trim().takeIf(String::isNotBlank)
            val normalizedNotes = state.notesInput.trim().takeIf(String::isNotBlank)
            val intent = SupplierPaymentIntent(
                payableId = payable.id,
                normalizedAmount = amount.toNormalizedIdempotencyNumber(),
                method = state.paymentMethod.apiValue.uppercase(),
                reference = normalizedReference,
                notes = normalizedNotes
            )
            val draft = SupplierPaymentDraft(
                payableId = payable.id,
                amountInput = state.paymentAmountInput,
                method = state.paymentMethod,
                referenceInput = state.referenceInput,
                notesInput = state.notesInput
            )
            val draftJson = draft.toPersistedJson()
            val idempotencyKey = paymentCoordinator.resolveKeyForSubmission(payable.id, intent, draftJson)
            if (idempotencyKey == null) {
                setActionError(UserFacingErrorMapper.message())
                return@launch
            }
            val command = CreateSupplierPaymentCommand(
                payableId = payable.id,
                amount = amount,
                method = state.paymentMethod.apiValue,
                reference = normalizedReference,
                notes = normalizedNotes,
                idempotencyKey = idempotencyKey
            )

            when (val result = purchasingRepository.createSupplierPayment(command)) {
                is RepositoryResult.Success -> {
                    paymentCoordinator.onSuccess(payable.id)
                    _uiState.update {
                        it.copy(
                            selectedPayable = null,
                            isSubmittingPayment = false,
                            message = "Pembayaran utang ${payable.supplierName} berhasil dicatat."
                        )
                    }
                    loadPayables()
                }

                is RepositoryResult.Error -> {
                    paymentCoordinator.onError(payable.id, result.code, result.message, draft, draftJson)
                    setActionError(result.message)
                }

                is RepositoryResult.Exception -> {
                    paymentCoordinator.onException(payable.id, draft, draftJson)
                    setActionError("Pembayaran gagal dicatat karena koneksi bermasalah.")
                }
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

    fun onOpeningAmountChanged(value: String) = _uiState.update { it.copy(openingAmountInput = sanitizeWholeRupiahInput(value)) }
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
                adjustmentPrefilledRemaining = null,
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
                adjustmentPrefilledRemaining = null,
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
        _uiState.update { it.copy(adjustmentSupplierId = value, adjustmentPrefilledRemaining = null) }
        loadSupplierDebtsForAdjustment(value)
    }

    fun onAdjustmentSupplierSelected(supplier: Supplier) {
        _uiState.update { state ->
            val updated = if (state.suppliers.none { it.id == supplier.id }) state.suppliers + supplier else state.suppliers
            state.copy(adjustmentSupplierId = supplier.id, suppliers = updated, adjustmentPrefilledRemaining = null)
        }
        loadSupplierDebtsForAdjustment(supplier.id)
    }

    fun onSelectDebtForAdjustment(debt: SupplierPayable) {
        val debtRef = debt.purchaseId.takeIf(String::isNotBlank) ?: "FAKTUR-${debt.id.takeLast(6).uppercase()}"
        _uiState.update { state ->
            state.copy(
                adjustmentReferenceInput = debtRef,
                adjustmentAmountInput = debt.remainingAmount.toWholeRupiahInputText(),
                adjustmentPrefilledRemaining = debt.remainingAmount,
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

    fun onAdjustmentAmountChanged(value: String) = _uiState.update { it.copy(adjustmentAmountInput = sanitizeWholeRupiahInput(value)) }
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
            reasonInput = state.adjustmentReasonInput,
            exactRemainingAmount = state.adjustmentPrefilledRemaining
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
        fun factory(
            purchasingRepository: PurchasingRepository,
            pendingSubmissionStore: PendingSubmissionStore? = null
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                SupplierDebtViewModel(
                    purchasingRepository = purchasingRepository,
                    pendingSubmissionStore = pendingSubmissionStore
                )
            }
        }
    }
}

internal fun BigDecimal.toInputText(): String = toWholeRupiahInputText()


