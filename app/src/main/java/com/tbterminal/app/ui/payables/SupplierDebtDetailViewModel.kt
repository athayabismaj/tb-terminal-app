package com.tbterminal.app.ui.payables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.parseWholeRupiahOrExactRemainingInput
import com.tbterminal.app.ui.common.sanitizeWholeRupiahInput
import com.tbterminal.app.ui.common.toNormalizedIdempotencyNumber
import com.tbterminal.app.ui.common.toWholeRupiahInputText
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.Job
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
    private val supplierId: String,
    private val idempotencyKeyGenerator: () -> String = { UUID.randomUUID().toString() },
    private val pendingSubmissionStore: PendingSubmissionStore? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierDebtDetailUiState())
    val uiState: StateFlow<SupplierDebtDetailUiState> = _uiState.asStateFlow()

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
                val sorted = directResult.data.data.sortedWith(
                    compareByDescending<SupplierPayable> { it.dueDate }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        payables = sorted
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
                    }.sortedWith(
                        compareByDescending<SupplierPayable> { it.dueDate }
                            .thenByDescending { it.createdAt }
                            .thenByDescending { it.id }
                    )
                    val fallback = (directResult as? RepositoryResult.Success)?.data?.data.orEmpty().sortedWith(
                        compareByDescending<SupplierPayable> { it.dueDate }
                            .thenByDescending { it.createdAt }
                            .thenByDescending { it.id }
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            payables = if (filtered.isNotEmpty()) filtered else fallback
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
                            message = "Pembayaran hutang ${payable.supplierName} berhasil dicatat."
                        )
                    }
                    load()
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
            supplierId: String,
            pendingSubmissionStore: PendingSubmissionStore? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            SupplierDebtDetailViewModel(
                purchasingRepository = purchasingRepository,
                supplierId = supplierId,
                pendingSubmissionStore = pendingSubmissionStore
            )
        }
    }
}
