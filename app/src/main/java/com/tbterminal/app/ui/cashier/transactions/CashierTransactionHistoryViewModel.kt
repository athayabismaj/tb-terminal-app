package com.tbterminal.app.ui.cashier.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.model.remainingAmount
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.OfflineCheckoutSyncResult
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.common.viewModelFactory
import com.tbterminal.app.ui.offline.toLocalPendingTransactionUi
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

class CashierTransactionHistoryViewModel(
    private val repository: CashReconciliationRepository,
    private val transactionDao: TransactionDao? = null,
    private val offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
    private val actorRole: String = "KASIR",
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashierTransactionHistoryUiState(isLoading = true))
    val uiState: StateFlow<CashierTransactionHistoryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeLocalPendingTransactions()
        loadTransactions()
    }

    private fun observeLocalPendingTransactions() {
        val dao = transactionDao ?: return
        viewModelScope.launch {
            offlineCheckoutSyncService?.recoverStaleSyncing()
            dao.observeUnsyncedTransactionsWithError(
                pending = SyncStatus.PENDING,
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.FAILED,
                conflict = SyncStatus.CONFLICT,
                entityType = SyncEntityType.TRANSACTION,
                limit = 10
            ).collect { localTransactions ->
                _uiState.update { state ->
                    state.copy(
                        localPendingTransactions = localTransactions.map { it.toLocalPendingTransactionUi() }
                    )
                }
            }
        }
    }

    private fun sortTransactions(
        list: List<CashTransaction>,
        order: SalesSortOrder
    ): List<CashTransaction> {
        return when (order) {
            SalesSortOrder.Newest -> list.sortedByDescending { it.createdAt }
            SalesSortOrder.HighestAmount -> list.sortedByDescending { it.total }
            SalesSortOrder.ReceiptAsc -> list.sortedBy { it.receiptNumber().lowercase() }
        }
    }

    fun loadTransactions(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val statusParam = when (_uiState.value.statusFilter.lowercase()) {
                "semua" -> null
                "belum lunas", "hutang" -> "hutang"
                "dibatalkan", "voided" -> "voided"
                else -> _uiState.value.statusFilter
            }
            val searchParam = _uiState.value.query.takeIf { it.isNotBlank() }

            val isAllPreset = _uiState.value.selectedPreset == "Semua"
            val startOfDay = if (isAllPreset) null else (_uiState.value.startDate ?: _uiState.value.selectedDate)?.take(10)
            val endOfDay = if (isAllPreset) null else (_uiState.value.endDate ?: _uiState.value.selectedDate)?.take(10)
            val paymentParam = when (_uiState.value.paymentMethodFilter.lowercase()) {
                "semua" -> null
                "cash", "tunai" -> "tunai"
                else -> _uiState.value.paymentMethodFilter
            }

            when (
                val result = repository.getTransactions(
                    page = page,
                    limit = CASHIER_TRANSACTION_PAGE_SIZE,
                    sessionId = null,
                    search = searchParam,
                    paymentMethod = paymentParam,
                    status = statusParam,
                    startDate = startOfDay,
                    endDate = endOfDay
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        val data = result.data.data
                        val mergedData = if (page == 1) {
                            mergeActiveSessionTransactions(data)
                        } else {
                            data
                        }
                        val sortedData = sortTransactions(mergedData, it.sortOrder)

                        val totalSales = sortedData.fold(java.math.BigDecimal.ZERO) { acc, tx -> acc + tx.total }
                        val totalPaid = sortedData.fold(java.math.BigDecimal.ZERO) { acc, tx -> acc + tx.paidAmount }
                        val totalRemaining = sortedData.fold(java.math.BigDecimal.ZERO) { acc, tx ->
                            val rem = tx.remainingAmount ?: (tx.total.subtract(tx.paidAmount)).max(java.math.BigDecimal.ZERO)
                            acc + rem
                        }
                        val paidCount = sortedData.count { tx -> tx.status.lowercase() in listOf("lunas", "paid", "success", "completed") }
                        val debtCount = sortedData.count { tx -> tx.status.lowercase() in listOf("hutang", "tempo", "unpaid", "dp", "partial") }

                        it.copy(
                            transactions = sortedData,
                            page = result.data.page,
                            totalPages = result.data.totalPages,
                            total = maxOf(result.data.total, sortedData.size.toLong()),
                            summaryTotalSales = totalSales,
                            summaryTotalPaid = totalPaid,
                            summaryTotalRemaining = totalRemaining,
                            summaryTransactionCount = sortedData.size,
                            summaryPaidCount = paidCount,
                            summaryDebtCount = debtCount,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Koneksi ke server gagal") }
                }
            }
        }
    }

    private suspend fun mergeActiveSessionTransactions(
        remoteTransactions: List<CashTransaction>
    ): List<CashTransaction> {
        val activeSession = when (val sessionResult = repository.getActiveSession()) {
            is RepositoryResult.Success -> sessionResult.data
            else -> null
        }
        if (activeSession != null) {
            _uiState.update { it.copy(activeSession = activeSession) }
        } else {
            return remoteTransactions
        }

        val activeTransactions = when (
            val sessionTransactions = repository.getTransactions(
                page = 1,
                limit = 200,
                sessionId = activeSession.id,
                search = null,
                status = null,
                startDate = null,
                endDate = null
            )
        ) {
            is RepositoryResult.Success -> sessionTransactions.data.data
            else -> emptyList()
        }

        if (activeTransactions.isEmpty()) return remoteTransactions

        val state = _uiState.value
        val merged = (activeTransactions.filter { it.matchesCurrentFilter(state) } + remoteTransactions)
            .distinctBy { it.id }
        return sortTransactions(merged, state.sortOrder).take(CASHIER_TRANSACTION_PAGE_SIZE)
    }

    private fun CashTransaction.matchesCurrentFilter(state: CashierTransactionHistoryUiState): Boolean {
        val query = state.query.trim()
        if (query.isNotBlank()) {
            val matchesQuery = receiptId.contains(query, ignoreCase = true) ||
                id.contains(query, ignoreCase = true) ||
                (customerName?.contains(query, ignoreCase = true) == true)
            if (!matchesQuery) return false
        }

        if (state.statusFilter != "Semua") {
            val targetStatus = state.statusFilter.lowercase()
            val normalizedStatus = status.lowercase()
            val matchesStatus = normalizedStatus == targetStatus ||
                (targetStatus == "lunas" && normalizedStatus in listOf("lunas", "paid", "success", "completed")) ||
                (targetStatus == "dp" && normalizedStatus in listOf("dp", "partial")) ||
                (targetStatus in listOf("hutang", "tempo", "belum lunas") && normalizedStatus in listOf("hutang", "tempo", "unpaid", "belum lunas")) ||
                (targetStatus in listOf("voided", "void", "dibatalkan", "cancelled", "batal") && normalizedStatus in listOf("voided", "void", "dibatalkan", "cancelled", "batal"))
            if (!matchesStatus) return false
        }

        if (state.paymentMethodFilter != "Semua") {
            val targetMethod = state.paymentMethodFilter.lowercase()
            val matchesMethod = paymentMethods.any { method ->
                val m = method.lowercase()
                when (targetMethod) {
                    "tunai", "cash" -> m in listOf("tunai", "cash")
                    "transfer" -> m in listOf("transfer", "bank", "transfer bank", "bank_transfer")
                    "qris" -> m in listOf("qris", "qr")
                    "hutang", "bon" -> m in listOf("hutang", "tempo", "credit", "bon")
                    "dp" -> m in listOf("dp", "down_payment")
                    else -> m == targetMethod
                }
            } || (targetMethod in listOf("hutang", "bon", "tempo") && (status.lowercase() in listOf("hutang", "tempo", "unpaid") || remainingAmount?.let { it > java.math.BigDecimal.ZERO } == true))
            if (!matchesMethod) return false
        }

        if (state.selectedPreset != "Semua") {
            val start = (state.startDate ?: state.selectedDate)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val end = (state.endDate ?: state.selectedDate)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (start != null || end != null) {
                val transactionDate = createdAt.toLocalDateOrNull() ?: return false
                if (start != null && transactionDate.isBefore(start)) return false
                if (end != null && transactionDate.isAfter(end)) return false
            }
        }

        return true
    }

    fun refresh() = loadTransactions(_uiState.value.page)

    fun syncLocalTransaction(localId: Long) {
        if (_uiState.value.isBulkSyncing) return
        val service = offlineCheckoutSyncService ?: run {
            _uiState.update { it.copy(localSyncMessage = "Service sinkronisasi belum tersedia") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(localSyncMessage = null) }
            when (val result = service.syncOne(localId)) {
                is OfflineCheckoutSyncResult.Success -> {
                    _uiState.update {
                        it.copy(localSyncMessage = "Transaksi lokal berhasil disinkronkan")
                    }
                    loadTransactions(page = 1)
                }
                is OfflineCheckoutSyncResult.Failed -> {
                    _uiState.update {
                        it.copy(localSyncMessage = result.message)
                    }
                }
            }
        }
    }

    fun syncAllLocalTransactions() {
        if (_uiState.value.isBulkSyncing) return
        val service = offlineCheckoutSyncService ?: run {
            _uiState.update { it.copy(localSyncMessage = "Service sinkronisasi belum tersedia") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBulkSyncing = true,
                    localSyncMessage = null,
                    bulkSyncProgressMessage = "Menyiapkan sinkronisasi..."
                )
            }
            val result = service.syncPendingAndFailed { current, total ->
                _uiState.update {
                    it.copy(bulkSyncProgressMessage = "Menyinkronkan $current dari $total transaksi...")
                }
            }
            val message = when {
                result.total == 0 -> "Tidak ada transaksi offline yang perlu disinkronkan."
                result.successCount == result.total -> "Semua transaksi berhasil disinkronkan."
                result.successCount == 0 -> "Sinkronisasi gagal. Periksa koneksi atau data transaksi."
                else -> "Sebagian transaksi gagal disinkronkan. Periksa detail error."
            }
            _uiState.update {
                it.copy(
                    isBulkSyncing = false,
                    bulkSyncProgressMessage = null,
                    localSyncMessage = message
                )
            }
            loadTransactions(page = 1)
        }
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) {
            _uiState.update { it.copy(page = previous) }
            loadTransactions(page = previous)
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) {
            _uiState.update { it.copy(page = next) }
            loadTransactions(page = next)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(query = query, page = 1) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            loadTransactions(page = 1)
        }
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status, page = 1) }
        loadTransactions(page = 1)
    }

    fun updatePaymentMethodFilter(method: String) {
        _uiState.update { it.copy(paymentMethodFilter = method, page = 1) }
        loadTransactions(page = 1)
    }

    fun updateSortOrder(order: SalesSortOrder) {
        _uiState.update { state ->
            val sorted = sortTransactions(state.transactions, order)
            state.copy(sortOrder = order, transactions = sorted)
        }
    }

    fun applyFilters(
        status: String,
        paymentMethod: String,
        startDateStr: String?,
        endDateStr: String?,
        preset: String?,
        sortOrder: SalesSortOrder = _uiState.value.sortOrder
    ) {
        val start = startDateStr?.take(10)
        val end = endDateStr?.take(10)
        _uiState.update {
            it.copy(
                statusFilter = status,
                paymentMethodFilter = paymentMethod,
                startDate = start,
                endDate = end,
                selectedDate = end ?: it.selectedDate,
                selectedPreset = preset,
                sortOrder = sortOrder,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    fun setDate(date: String?) {
        val resolvedDate = date ?: LocalDate.now(WIB_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE)
        _uiState.update {
            it.copy(
                selectedDate = resolvedDate,
                startDate = resolvedDate,
                endDate = resolvedDate,
                selectedPreset = if (date == null) "Hari ini" else null,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    fun setDatePreset(preset: String) {
        val today = LocalDate.now(WIB_ZONE)
        val (startDate, endDate) = when (preset) {
            "Semua" -> null to null
            "Minggu ini", "Minggu", "7 hari" -> today.minusDays(6) to today
            "Bulan ini", "Bulan", "30 hari" -> today.minusDays(29) to today
            else -> today to today
        }
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                startDate = startDate?.toString(),
                endDate = endDate?.toString(),
                selectedDate = endDate?.toString() ?: it.selectedDate,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    fun setDateRange(startDateStr: String, endDateStr: String, preset: String?) {
        val start = startDateStr.take(10)
        val end = endDateStr.take(10)
        _uiState.update {
            it.copy(
                startDate = start,
                endDate = end,
                selectedDate = end,
                selectedPreset = preset,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    fun previousDate() {
        shiftRange(daysDirection = -1)
    }

    fun nextDate() {
        val (startDate, endDate) = currentDateRange()
        val today = LocalDate.now(WIB_ZONE)
        if (endDate >= today) return

        val rangeLength = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0) + 1
        val nextStart = startDate.plusDays(rangeLength)
        val nextEnd = endDate.plusDays(rangeLength)
        if (nextEnd > today) {
            setDateRange(today.minusDays(rangeLength - 1).toString(), today.toString(), null)
        } else {
            setDateRange(nextStart.toString(), nextEnd.toString(), null)
        }
    }

    fun resetFilters() {
        val today = LocalDate.now(WIB_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE)
        _uiState.update {
            it.copy(
                query = "",
                statusFilter = "Semua",
                paymentMethodFilter = "Semua",
                sortOrder = SalesSortOrder.Newest,
                selectedPreset = "Hari ini",
                selectedDate = today,
                startDate = today,
                endDate = today,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    fun loadReceipt(transactionId: String, preserveMessage: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isReceiptLoading = true,
                    errorMessage = null,
                    receiptMessage = if (preserveMessage) state.receiptMessage else null,
                )
            }

            when (val result = repository.getTransactionById(transactionId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            selectedTransaction = result.data,
                            isReceiptLoading = false,
                            receiptMessage = if (preserveMessage) state.receiptMessage else null,
                        )
                    }
                }
                is RepositoryResult.Error -> setReceiptError(result.message)
                is RepositoryResult.Exception -> setReceiptError("Koneksi bermasalah. Struk gagal dimuat.")
            }
        }
    }

    fun showVoidDialog() {
        val transaction = _uiState.value.selectedTransaction ?: return
        if (!transactionActionAccess(actorRole, transaction.status, transaction.type).canVoid) return
        _uiState.update {
            it.copy(
                isVoidDialogOpen = true,
                voidReasonInput = if (it.isVoidOutcomeAmbiguous) it.voidReasonInput else "",
                voidIdempotencyKey = it.voidIdempotencyKey
                    ?.takeIf { _ -> it.isVoidOutcomeAmbiguous }
                    ?: "void-mobile-${UUID.randomUUID()}",
                voidErrorMessage = null,
            )
        }
    }

    fun hideVoidDialog() {
        if (_uiState.value.isSubmittingVoid) return
        _uiState.update {
            it.copy(
                isVoidDialogOpen = false,
                voidReasonInput = if (it.isVoidOutcomeAmbiguous) it.voidReasonInput else "",
                voidIdempotencyKey = if (it.isVoidOutcomeAmbiguous) it.voidIdempotencyKey else null,
                voidErrorMessage = null,
            )
        }
    }

    fun onVoidReasonChanged(value: String) {
        if (_uiState.value.isVoidOutcomeAmbiguous) return
        if (value.length <= 1000) {
            _uiState.update {
                it.copy(
                    voidReasonInput = value,
                    voidIdempotencyKey = "void-mobile-${UUID.randomUUID()}",
                    voidErrorMessage = null,
                )
            }
        }
    }

    fun submitVoid() {
        val state = _uiState.value
        if (state.isSubmittingVoid) return
        val transaction = state.selectedTransaction ?: return
        if (!transactionActionAccess(actorRole, transaction.status, transaction.type).canVoid) {
            _uiState.update { it.copy(voidErrorMessage = "Status transaksi ini tidak dapat dibatalkan.") }
            return
        }
        val reason = state.voidReasonInput.trim()
        val validationError = validateTransactionVoidReason(reason)
        if (validationError != null) {
            _uiState.update { it.copy(voidErrorMessage = validationError) }
            return
        }
        val access = transactionActionAccess(actorRole, transaction.status, transaction.type)
        if (access.requiresManagerApproval && state.voidManagerApprovalId == null) {
            _uiState.update {
                it.copy(
                    isVoidDialogOpen = false,
                    pendingManagerApprovalAction = ManagerApprovalAction.VOID_TRANSACTION,
                )
            }
            return
        }
        val key = state.voidIdempotencyKey ?: "void-mobile-${UUID.randomUUID()}"
        executeVoid(transaction.id, reason, key, state.voidManagerApprovalId)
    }

    private fun executeVoid(
        transactionId: String,
        reason: String,
        idempotencyKey: String,
        managerApprovalId: String?,
    ) {
        if (_uiState.value.isSubmittingVoid) return
        _uiState.update {
            it.copy(
                isSubmittingVoid = true,
                voidErrorMessage = null,
                voidIdempotencyKey = idempotencyKey,
                pendingManagerApprovalAction = null,
            )
        }
        viewModelScope.launch {
            when (
                val result = repository.voidTransaction(
                    transactionId,
                    reason,
                    idempotencyKey,
                    managerApprovalId,
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingVoid = false,
                            isVoidDialogOpen = false,
                            voidIdempotencyKey = null,
                            voidManagerApprovalId = null,
                            isVoidOutcomeAmbiguous = false,
                            receiptMessage = if (result.data.idempotentReplay) {
                                "Void sebelumnya sudah berhasil."
                            } else {
                                "Transaksi berhasil dibatalkan."
                            },
                        )
                    }
                    loadReceipt(transactionId, preserveMessage = true)
                }
                is RepositoryResult.Error -> {
                    val approvalInvalid = requiresNewActionApproval(result.code)
                    val ambiguous = isAmbiguousTransactionActionError(result.code)
                    _uiState.update {
                        it.copy(
                            isSubmittingVoid = false,
                            isVoidDialogOpen = true,
                            voidManagerApprovalId = if (approvalInvalid) null else it.voidManagerApprovalId,
                            isVoidOutcomeAmbiguous = ambiguous,
                            voidErrorMessage = transactionActionErrorMessage(result.code, result.message),
                        )
                    }
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(
                        isSubmittingVoid = false,
                        isVoidDialogOpen = true,
                        isVoidOutcomeAmbiguous = true,
                        voidErrorMessage = "Status void belum dapat dipastikan. Tekan konfirmasi lagi untuk memeriksa request yang sama.",
                    )
                }
            }
        }
    }

    fun showRefundDialog() {
        val transaction = _uiState.value.selectedTransaction ?: return
        if (!transactionActionAccess(actorRole, transaction.status, transaction.type).canRefund) return
        _uiState.update {
            it.copy(
                isRefundDialogOpen = true,
                refundReasonInput = if (it.isRefundOutcomeAmbiguous) it.refundReasonInput else "",
                refundDisposition = if (it.isRefundOutcomeAmbiguous) {
                    it.refundDisposition
                } else {
                    RefundDisposition.RETURN_TO_STOCK
                },
                refundIdempotencyKey = it.refundIdempotencyKey
                    ?.takeIf { _ -> it.isRefundOutcomeAmbiguous }
                    ?: "refund-mobile-${UUID.randomUUID()}",
                refundErrorMessage = null,
            )
        }
    }

    fun hideRefundDialog() {
        if (_uiState.value.isSubmittingRefund) return
        _uiState.update {
            it.copy(
                isRefundDialogOpen = false,
                refundReasonInput = if (it.isRefundOutcomeAmbiguous) it.refundReasonInput else "",
                refundIdempotencyKey = if (it.isRefundOutcomeAmbiguous) it.refundIdempotencyKey else null,
                refundErrorMessage = null,
            )
        }
    }

    fun onRefundReasonChanged(value: String) {
        if (_uiState.value.isRefundOutcomeAmbiguous) return
        if (value.length <= 1000) {
            _uiState.update {
                it.copy(
                    refundReasonInput = value,
                    refundIdempotencyKey = "refund-mobile-${UUID.randomUUID()}",
                    refundErrorMessage = null,
                )
            }
        }
    }

    fun onRefundDispositionChanged(disposition: RefundDisposition) {
        if (_uiState.value.isRefundOutcomeAmbiguous) return
        _uiState.update {
            it.copy(
                refundDisposition = disposition,
                refundIdempotencyKey = "refund-mobile-${UUID.randomUUID()}",
                refundErrorMessage = null,
            )
        }
    }

    fun submitRefund() {
        val state = _uiState.value
        if (state.isSubmittingRefund) return
        val transaction = state.selectedTransaction ?: return
        if (!transactionActionAccess(actorRole, transaction.status, transaction.type).canRefund) {
            _uiState.update { it.copy(refundErrorMessage = "Status transaksi ini tidak dapat direfund.") }
            return
        }
        val reason = state.refundReasonInput.trim()
        val validationError = validateTransactionRefundReason(reason)
        if (validationError != null) {
            _uiState.update { it.copy(refundErrorMessage = validationError) }
            return
        }
        val access = transactionActionAccess(actorRole, transaction.status, transaction.type)
        if (access.requiresManagerApproval && state.refundManagerApprovalId == null) {
            _uiState.update {
                it.copy(
                    isRefundDialogOpen = false,
                    pendingManagerApprovalAction = ManagerApprovalAction.REFUND_TRANSACTION,
                )
            }
            return
        }
        executeRefund(
            transactionId = transaction.id,
            reason = reason,
            disposition = state.refundDisposition,
            idempotencyKey = state.refundIdempotencyKey ?: "refund-mobile-${UUID.randomUUID()}",
            managerApprovalId = state.refundManagerApprovalId,
        )
    }

    private fun executeRefund(
        transactionId: String,
        reason: String,
        disposition: RefundDisposition,
        idempotencyKey: String,
        managerApprovalId: String?,
    ) {
        if (_uiState.value.isSubmittingRefund) return
        _uiState.update {
            it.copy(
                isSubmittingRefund = true,
                refundErrorMessage = null,
                refundIdempotencyKey = idempotencyKey,
                pendingManagerApprovalAction = null,
            )
        }
        viewModelScope.launch {
            when (
                val result = repository.refundTransaction(
                    id = transactionId,
                    reason = reason,
                    disposition = disposition,
                    idempotencyKey = idempotencyKey,
                    managerApprovalId = managerApprovalId,
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingRefund = false,
                            isRefundDialogOpen = false,
                            refundIdempotencyKey = null,
                            refundManagerApprovalId = null,
                            isRefundOutcomeAmbiguous = false,
                            refundResult = result.data,
                            receiptMessage = if (result.data.idempotentReplay) {
                                "Refund sebelumnya sudah berhasil."
                            } else {
                                "Refund transaksi berhasil."
                            },
                        )
                    }
                    loadReceipt(transactionId, preserveMessage = true)
                }
                is RepositoryResult.Error -> {
                    val approvalInvalid = requiresNewActionApproval(result.code)
                    val ambiguous = isAmbiguousTransactionActionError(result.code)
                    _uiState.update {
                        it.copy(
                            isSubmittingRefund = false,
                            isRefundDialogOpen = true,
                            refundManagerApprovalId = if (approvalInvalid) null else it.refundManagerApprovalId,
                            isRefundOutcomeAmbiguous = ambiguous,
                            refundErrorMessage = transactionActionErrorMessage(result.code, result.message),
                        )
                    }
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(
                        isSubmittingRefund = false,
                        isRefundDialogOpen = true,
                        isRefundOutcomeAmbiguous = true,
                        refundErrorMessage = "Status refund belum dapat dipastikan. Tekan konfirmasi lagi untuk memeriksa request yang sama.",
                    )
                }
            }
        }
    }

    fun onManagerApprovalGranted(grant: ManagerApprovalGrant) {
        val state = _uiState.value
        val transaction = state.selectedTransaction ?: return
        val expectedAction = state.pendingManagerApprovalAction ?: return
        val error = validateActionApproval(expectedAction, transaction, grant)
        if (error != null) {
            _uiState.update {
                it.copy(
                    pendingManagerApprovalAction = null,
                    isVoidDialogOpen = expectedAction == ManagerApprovalAction.VOID_TRANSACTION,
                    isRefundDialogOpen = expectedAction == ManagerApprovalAction.REFUND_TRANSACTION,
                    voidErrorMessage = error.takeIf { expectedAction == ManagerApprovalAction.VOID_TRANSACTION },
                    refundErrorMessage = error.takeIf { expectedAction == ManagerApprovalAction.REFUND_TRANSACTION },
                )
            }
            return
        }
        when (expectedAction) {
            ManagerApprovalAction.VOID_TRANSACTION -> {
                _uiState.update {
                    it.copy(voidManagerApprovalId = grant.approvalId, pendingManagerApprovalAction = null)
                }
                submitVoid()
            }
            ManagerApprovalAction.REFUND_TRANSACTION -> {
                _uiState.update {
                    it.copy(refundManagerApprovalId = grant.approvalId, pendingManagerApprovalAction = null)
                }
                submitRefund()
            }
            ManagerApprovalAction.DISCOUNT_OVERRIDE -> {
                _uiState.update {
                    it.copy(
                        pendingManagerApprovalAction = null,
                        errorMessage = "Persetujuan diskon tidak dapat digunakan untuk Void atau Refund.",
                    )
                }
            }
        }
    }

    fun cancelManagerApproval() {
        val action = _uiState.value.pendingManagerApprovalAction ?: return
        _uiState.update {
            it.copy(
                pendingManagerApprovalAction = null,
                isVoidDialogOpen = action == ManagerApprovalAction.VOID_TRANSACTION,
                isRefundDialogOpen = action == ManagerApprovalAction.REFUND_TRANSACTION,
            )
        }
    }

    private fun setListError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun setReceiptError(message: String) {
        _uiState.update { state ->
            state.copy(isReceiptLoading = false, errorMessage = message)
        }
    }

    private fun currentDateRange(): Pair<LocalDate, LocalDate> {
        val state = _uiState.value
        val endDate = (state.endDate ?: state.selectedDate)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(WIB_ZONE)
        val startDate = (state.startDate ?: state.selectedDate)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: endDate
        return startDate to endDate
    }

    private fun shiftRange(daysDirection: Int) {
        val (startDate, endDate) = currentDateRange()
        val rangeLength = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0) + 1
        val shiftedStart = startDate.plusDays(rangeLength * daysDirection)
        val shiftedEnd = endDate.plusDays(rangeLength * daysDirection)
        setDateRange(shiftedStart.toString(), shiftedEnd.toString(), null)
    }

    fun showPayDebtDialog() {
        _uiState.update { state ->
            val sisa = state.selectedTransaction?.remainingAmount()?.toPlainString() ?: ""
            state.copy(isPayDebtDialogOpen = true, payDebtAmountInput = sisa, payDebtMethodInput = "TUNAI")
        }
    }

    fun hidePayDebtDialog() {
        _uiState.update { it.copy(isPayDebtDialogOpen = false) }
    }

    fun onPayDebtAmountChanged(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        _uiState.update { it.copy(payDebtAmountInput = filtered) }
    }

    fun onPayDebtMethodChanged(method: String) {
        _uiState.update { it.copy(payDebtMethodInput = method) }
    }

    fun payDebt() {
        val state = _uiState.value
        val transactionId = state.selectedTransaction?.id ?: return
        val amount = state.payDebtAmountInput.toBigDecimalOrNull()
        if (amount == null || amount <= java.math.BigDecimal.ZERO) {
            setReceiptError("Nominal bayar tidak valid")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingDebt = true, errorMessage = null, receiptMessage = null) }
            
            when (val result = repository.payTransactionDebt(transactionId, amount, state.payDebtMethodInput)) {
                is RepositoryResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSubmittingDebt = false,
                            isPayDebtDialogOpen = false,
                            selectedTransaction = result.data,
                            receiptMessage = "Pelunasan berhasil disimpan"
                        )
                    }
                    // Refresh history transactions silently
                    loadTransactions(_uiState.value.page)
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isSubmittingDebt = false, errorMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isSubmittingDebt = false, errorMessage = "Gagal memproses pelunasan") }
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, receiptMessage = null) }
    }

    companion object {
        fun factory(
            repository: CashReconciliationRepository,
            transactionDao: TransactionDao? = null,
            offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
            actorRole: String = "KASIR",
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CashierTransactionHistoryViewModel(
                    repository,
                    transactionDao,
                    offlineCheckoutSyncService,
                    actorRole,
                )
            }
        }
    }
}

private fun CashTransaction.receiptNumber(): String = receiptId.ifBlank { id }

private fun String.toLocalDateOrNull(): LocalDate? {
    val value = trim()
    return runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(WIB_ZONE).toLocalDate()
    }.getOrNull()
        ?: runCatching {
            Instant.parse(value).atZone(WIB_ZONE).toLocalDate()
        }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value).atZone(WIB_ZONE).toLocalDate()
        }.getOrNull()
        ?: runCatching {
            LocalDate.parse(value.take(10))
        }.getOrNull()
}
