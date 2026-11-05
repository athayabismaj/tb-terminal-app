package com.tbterminal.app.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CashReconciliationViewModel(
    private val repository: CashReconciliationRepository,
    private val cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    private val localCashSessionService: LocalCashSessionService? = null,
    private val localCashExpenseService: LocalCashExpenseService? = null,
    private val offlineStatusRepository: OfflineStatusRepository? = null,
    private val cashierUserId: String? = null,
    private val cashierNameFallback: String? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashReconciliationUiState(isLoading = true))
    val uiState: StateFlow<CashReconciliationUiState> = _uiState.asStateFlow()
    private var latestOfflineStatus = OfflineStatus()

    init {
        observeOfflineStatus()
        loadCash()
    }

    private fun observeOfflineStatus() {
        val repository = offlineStatusRepository ?: return
        viewModelScope.launch {
            repository.status.collect { status ->
                latestOfflineStatus = status
            }
        }
    }

    fun loadCash(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (shouldUseLocalCashSession()) {
                if (!restoreLocalOpenCashSessionIfAvailable(page, allowAnyCashier = true)) {
                    _uiState.update {
                        it.copy(
                            activeSession = null,
                            transactions = emptyList(),
                            isLoading = false,
                            isSubmitting = false,
                            isUsingLocalActiveSession = false,
                            localCashSessionId = null,
                            localCashSessionSyncStatus = null,
                            message = "Mode kas lokal aktif. Buka sesi lokal untuk mulai transaksi offline."
                        )
                    }
                }
                return@launch
            }

            when (val sessionResult = repository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val session = sessionResult.data
                    _uiState.update {
                        it.copy(
                            activeSession = session,
                            isUsingLocalActiveSession = false,
                            localCashSessionId = null,
                            localCashSessionSyncStatus = null,
                            page = page,
                            total = 0,
                            totalPages = 1,
                            transactions = emptyList()
                        )
                    }

                    if (session == null) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        loadTransactionsForSession(session.id, page)
                    }
                }

                is RepositoryResult.Error -> {
                    if (sessionResult.isAuthFailure() || !sessionResult.isServerFailure()) {
                        setLoadError(sessionResult.message)
                    } else if (!restoreLocalOpenCashSessionIfAvailable(page, allowAnyCashier = true)) {
                        setLoadError(sessionResult.message)
                    }
                }
                is RepositoryResult.Exception -> {
                    if (!restoreLocalOpenCashSessionIfAvailable(page, allowAnyCashier = true)) {
                        setLoadError("Server kas tidak tersambung. Kas harian online belum dapat dimuat.")
                    }
                }
            }
        }
    }

    fun refresh() = loadCash(_uiState.value.page)

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) loadCash(previous)
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) loadCash(next)
    }

    fun onOpeningCashChanged(value: String) {
        _uiState.update { it.copy(openingCashInput = value.moneyInput(), errorMessage = null) }
    }

    fun onClosingCashChanged(value: String) {
        _uiState.update { it.copy(closingCashInput = value.moneyInput(), errorMessage = null) }
    }

    fun onClosingNotesChanged(value: String) {
        _uiState.update { it.copy(closingNotesInput = value, errorMessage = null) }
    }

    fun openSession() {
        val startingCash = _uiState.value.openingCashInput.toBigDecimalOrNull()
            ?: return setActionError("Modal awal wajib diisi dengan angka valid.")
        if (startingCash < BigDecimal.ZERO) return setActionError("Modal awal tidak boleh kurang dari nol.")

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }

            if (shouldUseLocalCashSession()) {
                openLocalSession(startingCash)
                return@launch
            }

            when (val result = repository.openSession(startingCash)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            activeSession = result.data,
                            openingCashInput = "",
                            isSubmitting = false,
                            isUsingLocalActiveSession = false,
                            localCashSessionId = null,
                            localCashSessionSyncStatus = null,
                            message = "Sesi kas berhasil dibuka."
                        )
                    }
                    loadCash(page = 1)
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> openLocalSession(startingCash)
            }
        }
    }

    fun closeSession() {
        if (!_uiState.value.hasActiveSession) return setActionError("Tidak ada sesi kas aktif.")
        val closingCash = _uiState.value.closingCashInput.toBigDecimalOrNull()
            ?: return setActionError("Kas fisik akhir wajib diisi dengan angka valid.")
        if (closingCash < BigDecimal.ZERO) return setActionError("Kas fisik akhir tidak boleh kurang dari nol.")

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }

            if (_uiState.value.isUsingLocalActiveSession || shouldUseLocalCashSession()) {
                closeLocalSession(closingCash)
                return@launch
            }

            when (
                val result = repository.closeSession(
                    endingCashPhysical = closingCash,
                    notes = _uiState.value.closingNotesInput.trim().takeIf(String::isNotBlank)
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            activeSession = result.data,
                            closingCashInput = "",
                            closingNotesInput = "",
                            isSubmitting = false,
                            isUsingLocalActiveSession = false,
                            localCashSessionId = null,
                            localCashSessionSyncStatus = null,
                            message = "Sesi kas berhasil ditutup."
                        )
                    }
                    loadCash(page = 1)
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> closeLocalSession(closingCash)
            }
        }
    }

    private suspend fun openLocalSession(startingCash: BigDecimal) {
        val service = localCashSessionService ?: return setActionError("Service sesi kas lokal belum tersedia.")
        val localCashierId = resolveCashierUserId()
            ?: return setActionError("Kasir sesi lokal belum valid.")

        when (
            val result = service.openLocalSession(
                cashierUserId = localCashierId,
                startingCash = startingCash
            )
        ) {
            is LocalCashSessionResult.Success -> {
                _uiState.update {
                    it.copy(
                        activeSession = result.session.toCashSession(),
                        openingCashInput = "",
                        isSubmitting = false,
                        isUsingLocalActiveSession = true,
                        localCashSessionId = result.session.localId,
                        localCashSessionSyncStatus = result.session.syncStatus.name,
                        errorMessage = null,
                        message = "Sesi kas lokal berhasil dibuka. Belum tersinkron ke server."
                    )
                }
            }
            is LocalCashSessionResult.Failed -> setActionError(result.message)
        }
    }

    private suspend fun closeLocalSession(closingCash: BigDecimal) {
        val service = localCashSessionService ?: return setActionError("Service sesi kas lokal belum tersedia.")
        val localId = _uiState.value.localCashSessionId
            ?: _uiState.value.activeSession?.id?.removePrefix("LOCAL-")?.toLongOrNull()
            ?: return setActionError("Sesi kas lokal belum valid.")

        when (
            val result = service.closeLocalSession(
                localId = localId,
                closingCash = closingCash,
                closingNote = _uiState.value.closingNotesInput
            )
        ) {
            is LocalCashSessionResult.Success -> {
                _uiState.update {
                    it.copy(
                        activeSession = result.session.toCashSession(),
                        closingCashInput = "",
                        closingNotesInput = "",
                        isSubmitting = false,
                        isUsingLocalActiveSession = true,
                        localCashSessionId = result.session.localId,
                        localCashSessionSyncStatus = result.session.syncStatus.name,
                        errorMessage = null,
                        message = "Sesi kas lokal berhasil ditutup. Belum tersinkron ke server."
                    )
                }
            }
            is LocalCashSessionResult.Failed -> setActionError(result.message)
        }
    }

    fun showExpenseDialog() {
        showPettyCashDialog(com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_OUT)
    }

    fun hideExpenseDialog() {
        hidePettyCashDialog()
    }

    fun onExpenseAmountChanged(value: String) {
        onPettyCashAmountChanged(value)
    }

    fun onExpenseDescriptionChanged(value: String) {
        onPettyCashDescriptionChanged(value)
    }

    fun addExpense() {
        addPettyCash()
    }

    fun showPettyCashDialog(type: com.tbterminal.app.data.local.cashexpense.CashMovementType = com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_OUT) {
        _uiState.update {
            it.copy(
                isPettyCashDialogOpen = true,
                isExpenseDialogOpen = true,
                pettyCashType = type,
                pettyCashAmountInput = "",
                pettyCashCategoryInput = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Tambah Modal Kembalian" else "Bensin Armada",
                pettyCashDescriptionInput = "",
                errorMessage = null
            )
        }
    }

    fun hidePettyCashDialog() {
        _uiState.update {
            it.copy(isPettyCashDialogOpen = false, isExpenseDialogOpen = false)
        }
    }

    fun onPettyCashTypeChanged(type: com.tbterminal.app.data.local.cashexpense.CashMovementType) {
        _uiState.update {
            it.copy(
                pettyCashType = type,
                pettyCashCategoryInput = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Tambah Modal Kembalian" else "Bensin Armada"
            )
        }
    }

    fun onPettyCashAmountChanged(value: String) {
        _uiState.update {
            it.copy(
                pettyCashAmountInput = value.moneyInput(),
                expenseAmountInput = value.moneyInput(),
                errorMessage = null
            )
        }
    }

    fun onPettyCashCategoryChanged(category: String) {
        _uiState.update { it.copy(pettyCashCategoryInput = category, errorMessage = null) }
    }

    fun onPettyCashDescriptionChanged(description: String) {
        _uiState.update {
            it.copy(
                pettyCashDescriptionInput = description,
                expenseDescriptionInput = description,
                errorMessage = null
            )
        }
    }

    fun addPettyCash() {
        val state = _uiState.value
        val rawInput = state.pettyCashAmountInput.ifBlank { state.expenseAmountInput }
        val amount = rawInput.toBigDecimalOrNull()
            ?: return setActionError("Nominal wajib diisi dengan angka valid.")
        if (amount <= BigDecimal.ZERO) return setActionError("Nominal harus lebih besar dari nol.")
        val type = state.pettyCashType
        val cat = state.pettyCashCategoryInput.trim()
        val rawDesc = state.pettyCashDescriptionInput.ifBlank { state.expenseDescriptionInput }
        val desc = rawDesc.trim().ifBlank { cat }
        val actionLabel = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Kas masuk" else "Pengeluaran"

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }

            if (_uiState.value.isUsingLocalActiveSession || shouldUseLocalCashSession()) {
                addLocalCashMovement(type, amount, cat, desc)
                return@launch
            }

            when (val result = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) {
                repository.addExpense(amount, "KAS MASUK: $desc ($cat)")
            } else {
                repository.addExpense(amount, "$desc ($cat)")
            }) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isPettyCashDialogOpen = false,
                            isExpenseDialogOpen = false,
                            isSubmitting = false,
                            message = "$actionLabel berhasil dicatat."
                        )
                    }
                    loadCash(page = 1)
                }
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> addLocalCashMovement(type, amount, cat, desc)
            }
        }
    }

    private suspend fun addLocalCashMovement(
        type: com.tbterminal.app.data.local.cashexpense.CashMovementType,
        amount: BigDecimal,
        category: String,
        description: String
    ) {
        val service = localCashExpenseService ?: return setActionError("Service mutasi kas lokal belum tersedia.")
        val localSessionId = _uiState.value.localCashSessionId
            ?: _uiState.value.activeSession?.id?.removePrefix("LOCAL-")?.toLongOrNull()
            ?: return setActionError("Sesi kas lokal belum valid.")

        val actionLabel = if (type == com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_IN) "Kas masuk" else "Pengeluaran"
        when (
            val result = service.addLocalCashMovement(
                cashSessionLocalId = localSessionId,
                type = type,
                amount = amount,
                category = category,
                description = description
            )
        ) {
            is com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult.Success -> {
                _uiState.update {
                    it.copy(
                        isPettyCashDialogOpen = false,
                        isExpenseDialogOpen = false,
                        pettyCashAmountInput = "",
                        pettyCashDescriptionInput = "",
                        expenseAmountInput = "",
                        expenseDescriptionInput = "",
                        isSubmitting = false,
                        message = "$actionLabel kas lokal berhasil dicatat."
                    )
                }
                loadCash(page = 1)
            }
            is com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult.Failed -> setActionError(result.message)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }

    private suspend fun loadTransactionsForSession(sessionId: String, page: Int) {
        val state = _uiState.value
        val txResult = repository.getTransactions(page = page, limit = state.limit, sessionId = sessionId)
        val exResult = repository.getExpenses(sessionId)

        if (txResult is RepositoryResult.Success) {
            val transactionsList = txResult.data.data.toMutableList()
            
            if (exResult is RepositoryResult.Success) {
                val mappedExpenses = exResult.data.map { ex ->
                    com.tbterminal.app.data.model.CashTransaction(
                        id = ex.id,
                        receiptId = ex.description,
                        sessionId = sessionId,
                        customerId = null,
                        customerName = null,
                        type = "EXPENSE",
                        status = "LUNAS",
                        total = ex.amount,
                        paidAmount = ex.amount,
                        createdAt = ex.createdAt
                    )
                }
                transactionsList.addAll(mappedExpenses)
                transactionsList.sortByDescending { it.createdAt }
            }

            _uiState.update {
                it.copy(
                    transactions = transactionsList,
                    page = txResult.data.page,
                    limit = txResult.data.limit,
                    total = txResult.data.total,
                    totalPages = txResult.data.totalPages.coerceAtLeast(1),
                    isLoading = false
                )
            }
        } else if (txResult is RepositoryResult.Error) {
            setLoadError(txResult.message)
        } else {
            setLoadError("Server kas tidak tersambung. Transaksi sesi online belum dapat dimuat.")
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, isSubmitting = false, errorMessage = message) }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
    }

    private suspend fun restoreLocalOpenCashSessionIfAvailable(
        page: Int,
        allowAnyCashier: Boolean = false
    ): Boolean {
        val dataSource = cashSessionLocalDataSource ?: return false
        val currentCashierUserId = resolveCashierUserId()
            ?: _uiState.value.activeSession?.userId?.takeIf(String::isNotBlank)
            ?: if (allowAnyCashier) null else return false
        val localSession = runCatching {
            dataSource.getLatestOpenSession(currentCashierUserId)
                ?: resolveCashierNameFallback()?.let { fallbackCashierId ->
                    if (fallbackCashierId == currentCashierUserId) {
                        null
                    } else {
                        dataSource.getLatestOpenSession(fallbackCashierId)
                    }
                }
                ?: if (allowAnyCashier) dataSource.getLatestOpenSession(null) else null
        }.getOrNull() ?: return false
        val localExpenses = localCashExpenseService?.getLocalExpenses(localSession.localId).orEmpty()
        val totalExpenses = localExpenses.totalExpenses()
        val totalCashIn = localExpenses.totalCashIn()

        _uiState.update {
            it.copy(
                activeSession = localSession.toCashSession(totalExpenses = totalExpenses, totalCashIn = totalCashIn),
                transactions = localExpenses.map { expense -> expense.toCashTransaction(localSession) },
                totalCashIn = totalCashIn,
                totalCashOut = totalExpenses,
                page = page,
                total = localExpenses.size.toLong(),
                totalPages = 1,
                isLoading = false,
                isSubmitting = false,
                isUsingLocalActiveSession = true,
                localCashSessionId = localSession.localId,
                localCashSessionSyncStatus = localSession.syncStatus.name,
                errorMessage = null,
                message = "Sesi kas aktif dari data lokal.\nServer tidak tersambung. Transaksi akan disimpan lokal."
            )
        }
        return true
    }

    private suspend fun shouldUseLocalCashSession(): Boolean {
        val repository = offlineStatusRepository ?: return false
        val status = latestOfflineStatus
        if (status.appDataMode == AppDataMode.OFFLINE_ONLY) return true
        if (!status.isInternetOnline) return true

        return when (status.backendStatus) {
            BackendStatus.CONNECTED -> false
            BackendStatus.UNREACHABLE -> true
            BackendStatus.UNKNOWN -> {
                repository.refreshBackendHealth(force = true) != BackendStatus.CONNECTED
            }
        }
    }

    private fun resolveCashierUserId(): String? {
        return cashierUserId
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: _uiState.value.activeSession?.userId?.takeIf(String::isNotBlank)
    }

    private fun resolveCashierNameFallback(): String? {
        return cashierNameFallback
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }

    private fun RepositoryResult.Error.isAuthFailure(): Boolean {
        val normalizedCode = code.lowercase()
        val normalizedMessage = message.lowercase()
        return normalizedCode == "http_401" ||
            normalizedCode == "http_403" ||
            normalizedCode.contains("unauthorized") ||
            normalizedCode.contains("forbidden") ||
            normalizedMessage.contains("unauthorized") ||
            normalizedMessage.contains("forbidden")
    }

    private fun RepositoryResult.Error.isServerFailure(): Boolean {
        val normalizedCode = code.lowercase()
        val normalizedMessage = message.lowercase()
        return normalizedCode.startsWith("http_5") ||
            normalizedCode.contains("internal_server_error") ||
            normalizedCode.contains("service_unavailable") ||
            normalizedCode.contains("gateway") ||
            normalizedCode.contains("timeout") ||
            normalizedMessage.contains("server tidak tersambung") ||
            normalizedMessage.contains("server kas tidak tersambung")
    }

    companion object {
        fun factory(
            repository: CashReconciliationRepository,
            cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
            localCashSessionService: LocalCashSessionService? = null,
            localCashExpenseService: LocalCashExpenseService? = null,
            offlineStatusRepository: OfflineStatusRepository? = null,
            cashierUserId: String? = null,
            cashierNameFallback: String? = null
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CashReconciliationViewModel(
                    repository = repository,
                    cashSessionLocalDataSource = cashSessionLocalDataSource,
                    localCashSessionService = localCashSessionService,
                    localCashExpenseService = localCashExpenseService,
                    offlineStatusRepository = offlineStatusRepository,
                    cashierUserId = cashierUserId,
                    cashierNameFallback = cashierNameFallback
                )
            }
        }
    }
}

private fun LocalCashSessionEntity.toCashSession(
    totalExpenses: BigDecimal = BigDecimal.ZERO,
    totalCashIn: BigDecimal = BigDecimal.ZERO
): CashSession {
    val baseSystemCash = expectedCash?.let(BigDecimal::valueOf) ?: BigDecimal.valueOf(startingCash)
    return CashSession(
        id = serverId ?: "LOCAL-$localId",
        userId = cashierUserId,
        userName = null,
        openedAt = openedAt.toOffsetDateTimeString(),
        closedAt = closedAt?.toOffsetDateTimeString(),
        openingCash = BigDecimal.valueOf(startingCash),
        closingCash = actualCash?.let(BigDecimal::valueOf),
        systemCash = baseSystemCash.subtract(totalExpenses).add(totalCashIn),
        difference = difference?.let(BigDecimal::valueOf),
        totalExpenses = totalExpenses,
        totalCashIn = totalCashIn,
        notes = openingNote,
        status = status
    )
}

private fun List<LocalCashExpenseEntity>.totalExpenses(): BigDecimal {
    return filter { !it.category.orEmpty().startsWith("CASH_IN", ignoreCase = true) && !it.category.orEmpty().startsWith("KAS_MASUK", ignoreCase = true) }
        .fold(BigDecimal.ZERO) { acc, expense -> acc.add(BigDecimal.valueOf(expense.amount)) }
}

private fun List<LocalCashExpenseEntity>.totalCashIn(): BigDecimal {
    return filter { it.category.orEmpty().startsWith("CASH_IN", ignoreCase = true) || it.category.orEmpty().startsWith("KAS_MASUK", ignoreCase = true) }
        .fold(BigDecimal.ZERO) { acc, expense -> acc.add(BigDecimal.valueOf(expense.amount)) }
}

private fun LocalCashExpenseEntity.toCashTransaction(session: LocalCashSessionEntity): CashTransaction {
    val isCashIn = category.orEmpty().startsWith("CASH_IN", ignoreCase = true) || category.orEmpty().startsWith("KAS_MASUK", ignoreCase = true)
    return CashTransaction(
        id = serverId ?: (if (isCashIn) "LOCAL-CASHIN-$localId" else "LOCAL-EXPENSE-$localId"),
        receiptId = description ?: (if (isCashIn) "Kas Masuk" else "Pengeluaran kas"),
        sessionId = session.serverId ?: "LOCAL-${session.localId}",
        customerId = null,
        customerName = null,
        type = if (isCashIn) "CASH_IN" else "EXPENSE",
        status = syncStatus.name,
        total = BigDecimal.valueOf(amount),
        paidAmount = BigDecimal.valueOf(amount),
        remainingAmount = BigDecimal.ZERO,
        createdAt = occurredAt.toOffsetDateTimeString()
    )
}

private fun Long.toOffsetDateTimeString(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.of("Asia/Jakarta"))
        .toOffsetDateTime()
        .toString()
}

private fun String.moneyInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}
