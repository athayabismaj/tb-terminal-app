package com.tbterminal.app.ui.receivables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.database.PendingSubmissionRecord
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.runSuspendCatching
import com.tbterminal.app.ui.common.parseWholeRupiahOrExactRemainingInput
import com.tbterminal.app.ui.common.sanitizeWholeRupiahInput
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val ReceivablePersistenceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

private data class ReceivablePaymentIntent(
    val customerId: String,
    val normalizedAmount: String,
    val method: ReceivablePaymentMethod,
    val invoiceIds: List<String>,
)

@Serializable
private data class PersistedReceivablePendingPayment(
    val customerId: String,
    val normalizedAmount: String,
    val methodName: String,
    val invoiceIds: List<String>,
    val keysByInvoice: Map<String, String>
)

internal data class ReceivableCustomerDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val message: String? = null,
    val customerPhone: String = "",
    val receivables: List<Receivable> = emptyList(),
    val payments: List<ReceivablePaymentHistory> = emptyList(),
    val selectedReceivableForPayment: Receivable? = null,
    val isPayAllOpen: Boolean = false,
    val selectedPayAllInvoiceIds: Set<String> = emptySet(),
    val paymentAmountInput: String = "",
    val paymentMethod: ReceivablePaymentMethod = ReceivablePaymentMethod.Cash,
    val referenceInput: String = "",
    val notesInput: String = "",
    val isSubmittingPayment: Boolean = false,
    val paymentErrorMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
) {
    val customerName: String
        get() = receivables.firstOrNull()?.customerName
            ?: payments.firstOrNull()?.customerName
            ?: "Pelanggan"
    val totalAmount: BigDecimal get() = receivables.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    val totalPaid: BigDecimal get() = receivables.fold(BigDecimal.ZERO) { sum, item -> sum + item.paidAmount }
    val totalRemaining: BigDecimal get() = receivables.fold(BigDecimal.ZERO) { sum, item -> sum + item.remainingAmount }
}

internal class ReceivableCustomerDetailViewModel(
    private val receivableRepository: ReceivableRepository,
    private val customerId: String,
    private val customerRepository: CustomerRepository? = null,
    private val idempotencyKeyGenerator: () -> String = { UUID.randomUUID().toString() },
    private val pendingSubmissionStore: PendingSubmissionStore? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceivableCustomerDetailUiState())
    val uiState: StateFlow<ReceivableCustomerDetailUiState> = _uiState.asStateFlow()

    private val pendingScope = PendingSubmissionScopes.receivablePayment(customerId)
    private var activePaymentIntent: ReceivablePaymentIntent? = null
    private var activePaymentKeysByInvoice: MutableMap<String, String> = mutableMapOf()
    private var activePaymentCreatedAt: Long? = null
    private var isPaymentOutcomeAmbiguous: Boolean = false
    private val restoreJob: Job

    init {
        restoreJob = viewModelScope.launch {
            restorePendingPayment()
        }
        load()
    }

    private suspend fun restorePendingPayment() {
        val store = pendingSubmissionStore ?: return
        val record = store.load(pendingScope) ?: return
        val dto = runSuspendCatching {
            ReceivablePersistenceJson.decodeFromString<PersistedReceivablePendingPayment>(record.intentJson)
        }.getOrElse {
            store.clear(pendingScope)
            return
        }
        val method = ReceivablePaymentMethod.entries.firstOrNull { it.name == dto.methodName }
        val valid = dto.customerId == customerId &&
            method != null &&
            dto.invoiceIds.isNotEmpty() &&
            dto.keysByInvoice.keys.containsAll(dto.invoiceIds) &&
            dto.keysByInvoice.values.all { it.isNotBlank() }

        if (!valid) {
            store.clear(pendingScope)
            return
        }

        if (activePaymentIntent == null) {
            activePaymentIntent = ReceivablePaymentIntent(
                customerId = dto.customerId,
                normalizedAmount = dto.normalizedAmount,
                method = method!!,
                invoiceIds = dto.invoiceIds,
            )
            activePaymentKeysByInvoice = dto.keysByInvoice.toMutableMap()
            activePaymentCreatedAt = record.createdAt
            isPaymentOutcomeAmbiguous = true
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val receivables = receivableRepository.getReceivables(
                page = 1,
                limit = DETAIL_LIMIT,
                customerId = customerId
            )
            val payments = receivableRepository.getReceivablePayments(
                page = 1,
                limit = DETAIL_LIMIT,
                customerId = customerId
            )
            customerRepository?.let { repo ->
                when (val cust = repo.getCustomerById(customerId)) {
                    is RepositoryResult.Success -> {
                        _uiState.update { it.copy(customerPhone = cust.data.phone.orEmpty()) }
                    }
                    else -> Unit
                }
            }
            if (receivables is RepositoryResult.Success) {
                val sortedReceivables = receivables.data.data.sortedWith(
                    compareByDescending<Receivable> { it.debtDate }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        receivables = sortedReceivables,
                        payments = (payments as? RepositoryResult.Success)?.data?.data.orEmpty(),
                        currentPage = 1,
                        totalPages = 1
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = (receivables as? RepositoryResult.Error)?.message
                            ?: "Detail piutang gagal dimuat. Periksa koneksi lalu coba lagi."
                    )
                }
            }
        }
    }

    fun openPayment(receivable: Receivable) {
        _uiState.update {
            it.copy(
                selectedReceivableForPayment = receivable,
                isPayAllOpen = true,
                selectedPayAllInvoiceIds = setOf(receivable.id),
                paymentAmountInput = receivable.remainingAmount.toWholeRupiahInputText(),
                paymentMethod = ReceivablePaymentMethod.Cash,
                referenceInput = "",
                notesInput = "",
                paymentErrorMessage = null
            )
        }
    }

    fun openPayAll() {
        val unpaid = _uiState.value.receivables.filter { it.remainingAmount > BigDecimal.ZERO }
        if (unpaid.isEmpty()) return
        val unpaidIds = unpaid.map { it.id }.toSet()
        val totalRemaining = unpaid.fold(BigDecimal.ZERO) { sum, it -> sum + it.remainingAmount }
        _uiState.update {
            it.copy(
                selectedReceivableForPayment = null,
                isPayAllOpen = true,
                selectedPayAllInvoiceIds = unpaidIds,
                paymentAmountInput = totalRemaining.toWholeRupiahInputText(),
                paymentMethod = ReceivablePaymentMethod.Cash,
                referenceInput = "",
                notesInput = "",
                paymentErrorMessage = null
            )
        }
    }

    fun togglePayAllInvoice(invoiceId: String) {
        val currentSelected = _uiState.value.selectedPayAllInvoiceIds
        val newSelected = if (currentSelected.contains(invoiceId)) {
            currentSelected - invoiceId
        } else {
            currentSelected + invoiceId
        }
        val unpaid = _uiState.value.receivables.filter { it.remainingAmount > BigDecimal.ZERO }
        val newTotal = unpaid.filter { it.id in newSelected }
            .fold(BigDecimal.ZERO) { sum, it -> sum + it.remainingAmount }

        _uiState.update {
            it.copy(
                selectedPayAllInvoiceIds = newSelected,
                paymentAmountInput = newTotal.toWholeRupiahInputText(),
                paymentErrorMessage = null
            )
        }
    }

    fun selectAllPayAllInvoices(selectAll: Boolean) {
        val unpaid = _uiState.value.receivables.filter { it.remainingAmount > BigDecimal.ZERO }
        val newSelected = if (selectAll) unpaid.map { it.id }.toSet() else emptySet()
        val newTotal = if (selectAll) {
            unpaid.fold(BigDecimal.ZERO) { sum, it -> sum + it.remainingAmount }
        } else {
            BigDecimal.ZERO
        }

        _uiState.update {
            it.copy(
                selectedPayAllInvoiceIds = newSelected,
                paymentAmountInput = newTotal.toWholeRupiahInputText(),
                paymentErrorMessage = null
            )
        }
    }

    fun closePayment() {
        _uiState.update {
            it.copy(
                selectedReceivableForPayment = null,
                isPayAllOpen = false,
                paymentErrorMessage = null
            )
        }
    }

    fun onPaymentAmountChanged(value: String) {
        _uiState.update { it.copy(paymentAmountInput = sanitizeWholeRupiahInput(value), paymentErrorMessage = null) }
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    fun submitPayment() {
        val state = _uiState.value
        if (state.isSubmittingPayment) return

        if (state.isPayAllOpen) {
            submitPayAll()
            return
        }

        val receivable = state.selectedReceivableForPayment ?: return
        val parsedAmount = parseWholeRupiahOrExactRemainingInput(state.paymentAmountInput, receivable.remainingAmount)
        val intent = ReceivablePaymentIntent(
            customerId = customerId,
            normalizedAmount = parsedAmount?.stripTrailingZeros()?.toPlainString() ?: state.paymentAmountInput.trim(),
            method = state.paymentMethod,
            invoiceIds = listOf(receivable.id),
        )
        validateReceivablePaymentInput(
            state.paymentAmountInput,
            receivable.remainingAmount,
            activePaymentKeysByInvoice[receivable.id] ?: "receivable-pending-key"
        ).getOrElse {
            _uiState.update { s -> s.copy(paymentErrorMessage = it.message ?: "Nominal pembayaran belum valid.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, paymentErrorMessage = null) }
            restoreJob.join()
            val idempotencyKey = resolveKeysForIntent(intent, prefix = "receivable-pay").getValue(receivable.id)
            val amount = validateReceivablePaymentInput(
                state.paymentAmountInput,
                receivable.remainingAmount,
                idempotencyKey
            ).getOrElse {
                _uiState.update { s ->
                    s.copy(
                        isSubmittingPayment = false,
                        paymentErrorMessage = it.message ?: "Nominal pembayaran belum valid."
                    )
                }
                return@launch
            }
            persistPendingPayment(PendingSubmissionStatus.IN_FLIGHT)
            val command = CreateReceivablePaymentCommand(
                receivableId = receivable.id,
                amount = amount,
                method = state.paymentMethod.apiValue,
                reference = state.referenceInput.trim().takeIf(String::isNotBlank),
                notes = state.notesInput.trim().takeIf(String::isNotBlank),
                idempotencyKey = idempotencyKey
            )
            when (val result = receivableRepository.createReceivablePayment(command)) {
                is RepositoryResult.Success -> {
                    clearPaymentIdempotencyState()
                    pendingSubmissionStore?.clear(pendingScope)
                    _uiState.update {
                        it.copy(
                            selectedReceivableForPayment = null,
                            isSubmittingPayment = false,
                            message = "Pembayaran nota berhasil dicatat."
                        )
                    }
                    load()
                }
                is RepositoryResult.Error -> {
                    val ambiguous = isAmbiguousReceivablePaymentError(result.code)
                    isPaymentOutcomeAmbiguous = ambiguous
                    if (ambiguous) {
                        persistPendingPayment(PendingSubmissionStatus.AMBIGUOUS)
                    } else {
                        pendingSubmissionStore?.clear(pendingScope)
                    }
                    _uiState.update {
                        it.copy(
                            isSubmittingPayment = false,
                            paymentErrorMessage = result.message
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    isPaymentOutcomeAmbiguous = true
                    persistPendingPayment(PendingSubmissionStatus.AMBIGUOUS)
                    _uiState.update {
                        it.copy(
                            isSubmittingPayment = false,
                            paymentErrorMessage = "Pembayaran gagal dicatat karena koneksi bermasalah."
                        )
                    }
                }
            }
        }
    }

    private fun submitPayAll() {
        val state = _uiState.value
        val unpaid = state.receivables.filter { it.remainingAmount > BigDecimal.ZERO }
        if (unpaid.isEmpty()) {
            closePayment()
            return
        }

        val selectedInvoices = unpaid.filter { it.id in state.selectedPayAllInvoiceIds }
        if (selectedInvoices.isEmpty()) {
            _uiState.update { it.copy(paymentErrorMessage = "Pilih minimal satu nota piutang untuk dibayar.") }
            return
        }

        val selectedTotalRemaining = selectedInvoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.remainingAmount }
        val sortedInvoiceIds = selectedInvoices.map { it.id }.sorted()
        val parsedAmount = parseWholeRupiahOrExactRemainingInput(state.paymentAmountInput, selectedTotalRemaining)
        val intent = ReceivablePaymentIntent(
            customerId = customerId,
            normalizedAmount = parsedAmount?.stripTrailingZeros()?.toPlainString() ?: state.paymentAmountInput.trim(),
            method = state.paymentMethod,
            invoiceIds = sortedInvoiceIds,
        )
        val syncAmount = validateReceivablePaymentInput(
            state.paymentAmountInput,
            selectedTotalRemaining,
            activePaymentKeysByInvoice[sortedInvoiceIds.first()] ?: "receivable-pending-key"
        ).getOrElse {
            _uiState.update { s -> s.copy(paymentErrorMessage = it.message ?: "Nominal pembayaran belum valid.") }
            return
        }

        val syncAllocations = calculateFifoAllocations(
            invoices = unpaid,
            selectedIds = state.selectedPayAllInvoiceIds,
            totalPaymentAmount = syncAmount
        )
        val syncPaymentsToMake = syncAllocations.filter { it.isSelected && it.allocatedAmount > BigDecimal.ZERO }
        if (syncPaymentsToMake.isEmpty()) {
            _uiState.update { it.copy(paymentErrorMessage = "Nominal pembayaran belum mencukupi untuk dialokasikan ke nota terpilih.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, paymentErrorMessage = null) }
            restoreJob.join()
            val keysByInvoice = resolveKeysForIntent(intent, prefix = "receivable-pay-all")
            val validationKey = keysByInvoice.getValue(sortedInvoiceIds.first())
            val amount = validateReceivablePaymentInput(
                state.paymentAmountInput,
                selectedTotalRemaining,
                validationKey
            ).getOrElse {
                _uiState.update { s ->
                    s.copy(
                        isSubmittingPayment = false,
                        paymentErrorMessage = it.message ?: "Nominal pembayaran belum valid."
                    )
                }
                return@launch
            }
            val paymentsToMake = calculateFifoAllocations(
                invoices = unpaid,
                selectedIds = state.selectedPayAllInvoiceIds,
                totalPaymentAmount = amount
            ).filter { it.isSelected && it.allocatedAmount > BigDecimal.ZERO }
            if (paymentsToMake.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isSubmittingPayment = false,
                        paymentErrorMessage = "Nominal pembayaran belum mencukupi untuk dialokasikan ke nota terpilih."
                    )
                }
                return@launch
            }
            persistPendingPayment(PendingSubmissionStatus.IN_FLIGHT)

            var successCount = 0
            var failureError: String? = null
            var ambiguousFailure = false

            for (allocation in paymentsToMake) {
                val receivable = allocation.receivable
                val payAmount = allocation.allocatedAmount

                val command = CreateReceivablePaymentCommand(
                    receivableId = receivable.id,
                    amount = payAmount,
                    method = state.paymentMethod.apiValue,
                    reference = state.referenceInput.trim().takeIf(String::isNotBlank),
                    notes = state.notesInput.trim().takeIf(String::isNotBlank) ?: "Pelunasan bayar semua piutang",
                    idempotencyKey = keysByInvoice.getValue(receivable.id)
                )

                when (val result = receivableRepository.createReceivablePayment(command)) {
                    is RepositoryResult.Success -> {
                        successCount++
                    }
                    is RepositoryResult.Error -> {
                        ambiguousFailure = isAmbiguousReceivablePaymentError(result.code)
                        failureError = "${receivable.mobileReference()}: ${result.message}"
                        break
                    }
                    is RepositoryResult.Exception -> {
                        ambiguousFailure = true
                        failureError = "${receivable.mobileReference()}: Terjadi kendala koneksi saat memproses pembayaran"
                        break
                    }
                }
            }

            if (failureError == null) {
                clearPaymentIdempotencyState()
                pendingSubmissionStore?.clear(pendingScope)
                _uiState.update {
                    it.copy(
                        isPayAllOpen = false,
                        isSubmittingPayment = false,
                        message = if (successCount > 1) {
                            "Berhasil mencatat pembayaran untuk $successCount nota piutang."
                        } else {
                            "Pembayaran piutang berhasil dicatat."
                        }
                    )
                }
                load()
            } else {
                isPaymentOutcomeAmbiguous = ambiguousFailure
                if (ambiguousFailure) {
                    persistPendingPayment(PendingSubmissionStatus.AMBIGUOUS)
                } else {
                    pendingSubmissionStore?.clear(pendingScope)
                }
                if (successCount > 0) {
                    _uiState.update {
                        it.copy(
                            isSubmittingPayment = false,
                            paymentErrorMessage = "Berhasil mencatat $successCount nota, namun nota berikutnya terkendala: $failureError"
                        )
                    }
                    load()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmittingPayment = false,
                            paymentErrorMessage = failureError
                        )
                    }
                }
            }
        }
    }

    private fun resolveKeysForIntent(
        intent: ReceivablePaymentIntent,
        prefix: String,
    ): Map<String, String> {
        val existingIntent = activePaymentIntent
        if (existingIntent == intent && activePaymentKeysByInvoice.keys.containsAll(intent.invoiceIds)) {
            return activePaymentKeysByInvoice
        }
        val freshKeys = intent.invoiceIds.associateWith {
            "$prefix-${idempotencyKeyGenerator()}"
        }
        activePaymentIntent = intent
        activePaymentKeysByInvoice = freshKeys.toMutableMap()
        activePaymentCreatedAt = null
        isPaymentOutcomeAmbiguous = false
        return activePaymentKeysByInvoice
    }

    private suspend fun persistPendingPayment(status: PendingSubmissionStatus) {
        val store = pendingSubmissionStore ?: return
        val intent = activePaymentIntent ?: return
        val primaryKey = intent.invoiceIds.firstOrNull()?.let { activePaymentKeysByInvoice[it] } ?: return
        val now = clockMillis()
        val createdAt = activePaymentCreatedAt ?: now
        activePaymentCreatedAt = createdAt

        val intentJson = ReceivablePersistenceJson.encodeToString(
            PersistedReceivablePendingPayment(
                customerId = intent.customerId,
                normalizedAmount = intent.normalizedAmount,
                methodName = intent.method.name,
                invoiceIds = intent.invoiceIds,
                keysByInvoice = activePaymentKeysByInvoice.toMap()
            )
        )
        store.save(
            PendingSubmissionRecord(
                scope = pendingScope,
                idempotencyKey = primaryKey,
                intentJson = intentJson,
                payloadJson = null,
                status = status,
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    private fun clearPaymentIdempotencyState() {
        activePaymentIntent = null
        activePaymentKeysByInvoice.clear()
        activePaymentCreatedAt = null
        isPaymentOutcomeAmbiguous = false
    }

    private fun isAmbiguousReceivablePaymentError(code: String): Boolean {
        val normalized = code.trim().uppercase()
        return normalized.startsWith("HTTP_5") || normalized in AMBIGUOUS_PAYMENT_ERROR_CODES
    }

    companion object {
        private const val DETAIL_LIMIT = 100
        private val AMBIGUOUS_PAYMENT_ERROR_CODES = setOf(
            "NETWORK_TIMEOUT",
            "NETWORK_UNAVAILABLE",
            "CONNECTION_FAILED",
            "CONNECTION_INTERRUPTED",
            "INVALID_RESPONSE",
            "EMPTY_BODY",
        )

        fun factory(
            receivableRepository: ReceivableRepository,
            customerId: String,
            customerRepository: CustomerRepository? = null,
            pendingSubmissionStore: PendingSubmissionStore? = null
        ): ViewModelProvider.Factory =
            viewModelFactory {
                ReceivableCustomerDetailViewModel(
                    receivableRepository = receivableRepository,
                    customerId = customerId,
                    customerRepository = customerRepository,
                    pendingSubmissionStore = pendingSubmissionStore
                )
            }
    }
}
