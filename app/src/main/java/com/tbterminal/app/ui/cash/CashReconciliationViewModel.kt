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
        _uiState.update { it.copy(openingCashInput = value.moneyInput()) }
    }

    fun onClosingCashChanged(value: String) {
        _uiState.update { it.copy(closingCashInput = value.moneyInput()) }
    }

    fun onClosingNotesChanged(value: String) {
        _uiState.update { it.copy(closingNotesInput = value) }
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
        _uiState.update { it.copy(isExpenseDialogOpen = true, expenseAmountInput = "", expenseDescriptionInput = "") }
    }

    fun hideExpenseDialog() {
        _uiState.update { it.copy(isExpenseDialogOpen = false) }
    }

    fun onExpenseAmountChanged(value: String) {
        _uiState.update { it.copy(expenseAmountInput = value.moneyInput()) }
    }

    fun onExpenseDescriptionChanged(value: String) {
        _uiState.update { it.copy(expenseDescriptionInput = value) }
    }

    fun addExpense() {
        val amount = _uiState.value.expenseAmountInput.toBigDecimalOrNull()
            ?: return setActionError("Nominal pengeluaran wajib diisi dengan angka valid.")
        if (amount <= BigDecimal.ZERO) return setActionError("Nominal pengeluaran harus lebih besar dari nol.")
        val desc = _uiState.value.expenseDescriptionInput.trim()
        if (desc.isEmpty()) return setActionError("Keterangan pengeluaran wajib diisi.")

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }

            if (_uiState.value.isUsingLocalActiveSession || shouldUseLocalCashSession()) {
                addLocalExpense(amount, desc)
                return@launch
            }

            when (val result = repository.addExpense(amount, desc)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExpenseDialogOpen = false,
                            isSubmitting = false,
                            message = "Pengeluaran berhasil dicatat."
                        )
                    }
                    loadCash(page = 1) // Refresh session data
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> addLocalExpense(amount, desc)
            }
        }
    }

    private suspend fun addLocalExpense(amount: BigDecimal, description: String) {
        val service = localCashExpenseService ?: return setActionError("Service pengeluaran kas lokal belum tersedia.")
        val localSessionId = _uiState.value.localCashSessionId
            ?: _uiState.value.activeSession?.id?.removePrefix("LOCAL-")?.toLongOrNull()
            ?: return setActionError("Sesi kas lokal belum valid. Pengeluaran belum bisa dicatat offline.")

        when (
            val result = service.addLocalExpense(
                cashSessionLocalId = localSessionId,
                amount = amount,
                description = description
            )
        ) {
            is LocalCashExpenseResult.Success -> {
                _uiState.update {
                    it.copy(
                        isExpenseDialogOpen = false,
                        expenseAmountInput = "",
                        expenseDescriptionInput = "",
                        isSubmitting = false,
                        message = "Pengeluaran kas lokal berhasil dicatat. Belum tersinkron ke server."
                    )
                }
                loadCash(page = 1)
            }
            is LocalCashExpenseResult.Failed -> setActionError(result.message)
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

        _uiState.update {
            it.copy(
                activeSession = localSession.toCashSession(totalExpenses = localExpenses.totalAmount()),
                transactions = localExpenses.map { expense -> expense.toCashTransaction(localSession) },
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

private fun LocalCashSessionEntity.toCashSession(totalExpenses: BigDecimal = BigDecimal.ZERO): CashSession {
    val baseSystemCash = expectedCash?.let(BigDecimal::valueOf) ?: BigDecimal.valueOf(startingCash)
    return CashSession(
        id = serverId ?: "LOCAL-$localId",
        userId = cashierUserId,
        userName = null,
        openedAt = openedAt.toOffsetDateTimeString(),
        closedAt = closedAt?.toOffsetDateTimeString(),
        openingCash = BigDecimal.valueOf(startingCash),
        closingCash = actualCash?.let(BigDecimal::valueOf),
        systemCash = baseSystemCash.subtract(totalExpenses),
        difference = difference?.let(BigDecimal::valueOf),
        totalExpenses = totalExpenses,
        notes = openingNote,
        status = status
    )
}

private fun List<LocalCashExpenseEntity>.totalAmount(): BigDecimal {
    return fold(BigDecimal.ZERO) { acc, expense -> acc.add(BigDecimal.valueOf(expense.amount)) }
}

private fun LocalCashExpenseEntity.toCashTransaction(session: LocalCashSessionEntity): CashTransaction {
    return CashTransaction(
        id = serverId ?: "LOCAL-EXPENSE-$localId",
        receiptId = description ?: "Pengeluaran kas",
        sessionId = session.serverId ?: "LOCAL-${session.localId}",
        customerId = null,
        customerName = null,
        type = "EXPENSE",
        status = syncStatus.name,
        total = BigDecimal.valueOf(amount),
        paidAmount = BigDecimal.valueOf(amount),
        remainingAmount = BigDecimal.ZERO,
        createdAt = occurredAt.toOffsetDateTimeString()
    )
}

private fun Long.toOffsetDateTimeString(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .toString()
}

private fun String.moneyInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}
