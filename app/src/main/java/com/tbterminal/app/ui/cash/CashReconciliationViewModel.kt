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
import com.tbterminal.app.data.local.mapper.hasUnsyncedLocalChanges
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.ServerUnavailabilityClassifier
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineSessionAgeAlert
import com.tbterminal.app.data.sync.OfflineSessionAgePolicy
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.data.sync.runSuspendCatching
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.ui.common.parseWholeRupiahInput
import com.tbterminal.app.ui.common.sanitizeWholeRupiahInput
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
    private val cashierNameFallback: String? = null,
    private val actorRole: String = "KASIR",
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CashReconciliationUiState(
            isLoading = true,
            canOpenCashSession = AppAccessPolicy.canOpenCashSession(actorRole),
        )
    )
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
                if (!restoreLocalOpenCashSessionIfAvailable(page)) {
                    val ageAlert = resolveOfflineSessionAgeAlert()
                    _uiState.update {
                        it.copy(
                            activeSession = null,
                            transactions = emptyList(),
                            isLoading = false,
                            isSubmitting = false,
                            isUsingLocalActiveSession = false,
                            localCashSessionId = null,
                            localCashSessionSyncStatus = null,
                            offlineSessionAgeAlert = ageAlert,
                            message = if (it.canOpenCashSession) {
                                "Mode kas lokal aktif. Buka sesi lokal untuk mulai transaksi offline."
                            } else {
                                "Mode kas lokal aktif. Hanya kasir yang dapat membuka sesi kas."
                            }
                        )
                    }
                }
                return@launch
            }

            when (val sessionResult = repository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val session = sessionResult.data
                    if (session == null) {
                        reconcileNoActiveRemoteSessionSafely()
                        val ageAlert = resolveOfflineSessionAgeAlert()
                        _uiState.update {
                            it.copy(
                                activeSession = null,
                                isUsingLocalActiveSession = false,
                                localCashSessionId = null,
                                localCashSessionSyncStatus = null,
                                offlineSessionAgeAlert = ageAlert,
                                page = page,
                                total = 0,
                                totalPages = 1,
                                transactions = emptyList(),
                                isLoading = false,
                            )
                        }
                    } else {
                        val mirroredSession = mirrorRemoteSessionEntitySafely(session)
                        val locallyClosedPending = mirroredSession != null &&
                            mirroredSession.hasUnsyncedLocalChanges() &&
                            !mirroredSession.status.equals("OPEN", ignoreCase = true)
                        if (locallyClosedPending) {
                            if (!restoreLocalOpenCashSessionIfAvailable(page)) {
                                val ageAlert = resolveOfflineSessionAgeAlert()
                                _uiState.update {
                                    it.copy(
                                        activeSession = null,
                                        isUsingLocalActiveSession = false,
                                        localCashSessionId = null,
                                        localCashSessionSyncStatus = null,
                                        offlineSessionAgeAlert = ageAlert,
                                        page = page,
                                        total = 0,
                                        totalPages = 1,
                                        transactions = emptyList(),
                                        isLoading = false,
                                    )
                                }
                            }
                        } else {
                            val ageAlert = resolveOfflineSessionAgeAlert()
                            _uiState.update {
                                it.copy(
                                    activeSession = session,
                                    isUsingLocalActiveSession = false,
                                    localCashSessionId = mirroredSession?.localId,
                                    localCashSessionSyncStatus = null,
                                    offlineSessionAgeAlert = ageAlert,
                                    page = page,
                                    total = 0,
                                    totalPages = 1,
                                    transactions = emptyList()
                                )
                            }
                            loadTransactionsForSession(session.id, page)
                        }
                    }
                }

                is RepositoryResult.Error -> {
                    val serverUnavailable = ServerUnavailabilityClassifier.isServerUnavailable(sessionResult)
                    if (serverUnavailable) {
                        markBackendUnreachableAndSyncState()
                    }
                    if (!serverUnavailable || !restoreLocalOpenCashSessionIfAvailable(page)) {
                        setLoadError(sessionResult.message, resolveOfflineSessionAgeAlert())
                    }
                }
                is RepositoryResult.Exception -> {
                    markBackendUnreachableAndSyncState()
                    if (!restoreLocalOpenCashSessionIfAvailable(page)) {
                        setLoadError(
                            "Server kas tidak tersambung. Kas harian online belum dapat dimuat.",
                            resolveOfflineSessionAgeAlert()
                        )
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
        _uiState.update { it.copy(openingCashInput = sanitizeWholeRupiahInput(value), errorMessage = null) }
    }

    fun onClosingCashChanged(value: String) {
        _uiState.update { it.copy(closingCashInput = sanitizeWholeRupiahInput(value), errorMessage = null) }
    }

    fun onClosingNotesChanged(value: String) {
        _uiState.update { it.copy(closingNotesInput = value, errorMessage = null) }
    }

    fun openSession() {
        if (!_uiState.value.canOpenCashSession) {
            return setActionError("Hanya kasir yang dapat membuka sesi kas.")
        }
        val startingCash = parseWholeRupiahInput(_uiState.value.openingCashInput)
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
                    val mirroredLocalId = mirrorRemoteSessionSafely(result.data)
                    _uiState.update {
                        it.copy(
                            activeSession = result.data,
                            openingCashInput = "",
                            isSubmitting = false,
                            isUsingLocalActiveSession = false,
                            localCashSessionId = mirroredLocalId,
                            localCashSessionSyncStatus = null,
                            message = "Sesi kas berhasil dibuka."
                        )
                    }
                    loadCash(page = 1)
                }

                is RepositoryResult.Error -> {
                    if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                        markBackendUnreachableAndSyncState()
                        openLocalSession(startingCash)
                    } else {
                        setActionError(result.message)
                    }
                }
                is RepositoryResult.Exception -> {
                    markBackendUnreachableAndSyncState()
                    openLocalSession(startingCash)
                }
            }
        }
    }

    fun closeSession() {
        if (!_uiState.value.hasActiveSession) return setActionError("Tidak ada sesi kas aktif.")
        val closingCash = parseWholeRupiahInput(_uiState.value.closingCashInput)
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
                    mirrorRemoteSessionEntitySafely(result.data)
                    _uiState.update {
                        it.copy(
                            activeSession = result.data,
                            lastClosedSession = result.data,
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

                is RepositoryResult.Error -> {
                    if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                        markBackendUnreachableAndSyncState()
                        closeLocalSession(closingCash)
                    } else {
                        setActionError(result.message)
                    }
                }
                is RepositoryResult.Exception -> {
                    markBackendUnreachableAndSyncState()
                    closeLocalSession(closingCash)
                }
            }
        }
    }

    private suspend fun openLocalSession(startingCash: BigDecimal) {
        val localCashierId = resolveCashierUserId()
            ?: return setActionError("Kasir sesi lokal belum valid.")

        val existingOpenSession = findExistingOpenSessionForCashier(
            cashierId = localCashierId,
        )
        if (existingOpenSession != null) {
            val localExpenses = localCashExpenseService?.getLocalExpenses(existingOpenSession.localId).orEmpty()
            val totalExpenses = localExpenses.totalExpenses()
            val totalCashIn = localExpenses.totalCashIn()
            val ageAlert = resolveOfflineSessionAgeAlert(existingOpenSession)
            _uiState.update {
                it.copy(
                    activeSession = existingOpenSession.toCashSession(totalExpenses = totalExpenses, totalCashIn = totalCashIn),
                    transactions = localExpenses.map { expense -> expense.toCashTransaction(existingOpenSession) },
                    totalCashIn = totalCashIn,
                    totalCashOut = totalExpenses,
                    openingCashInput = "",
                    isSubmitting = false,
                    isUsingLocalActiveSession = true,
                    localCashSessionId = existingOpenSession.localId,
                    localCashSessionSyncStatus = existingOpenSession.syncStatus.name,
                    offlineSessionAgeAlert = ageAlert,
                    errorMessage = null,
                    message = "Sesi kas aktif dari data lokal.\nServer tidak tersambung. Transaksi akan disimpan lokal."
                )
            }
            return
        }

        val service = localCashSessionService ?: return setActionError("Service sesi kas lokal belum tersedia.")
        when (
            val result = service.openLocalSession(
                cashierUserId = localCashierId,
                startingCash = startingCash
            )
        ) {
            is LocalCashSessionResult.Success -> {
                val ageAlert = resolveOfflineSessionAgeAlert(result.session)
                _uiState.update {
                    it.copy(
                        activeSession = result.session.toCashSession(),
                        openingCashInput = "",
                        isSubmitting = false,
                        isUsingLocalActiveSession = true,
                        localCashSessionId = result.session.localId,
                        localCashSessionSyncStatus = result.session.syncStatus.name,
                        offlineSessionAgeAlert = ageAlert,
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
        val localId = resolveLocalSessionId()
            ?: return setActionError("Sesi kas lokal belum valid.")

        when (
            val result = service.closeLocalSession(
                localId = localId,
                closingCash = closingCash,
                closingNote = _uiState.value.closingNotesInput
            )
        ) {
            is LocalCashSessionResult.Success -> {
                val sessionModel = result.session.toCashSession()
                val ageAlert = resolveOfflineSessionAgeAlert(result.session)
                _uiState.update {
                    it.copy(
                        activeSession = sessionModel,
                        lastClosedSession = sessionModel,
                        closingCashInput = "",
                        closingNotesInput = "",
                        isSubmitting = false,
                        isUsingLocalActiveSession = true,
                        localCashSessionId = result.session.localId,
                        localCashSessionSyncStatus = result.session.syncStatus.name,
                        offlineSessionAgeAlert = ageAlert,
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
        val sanitized = sanitizeWholeRupiahInput(value)
        _uiState.update {
            it.copy(
                pettyCashAmountInput = sanitized,
                expenseAmountInput = sanitized,
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
        val amount = parseWholeRupiahInput(rawInput)
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
                is RepositoryResult.Error -> {
                    if (ServerUnavailabilityClassifier.isServerUnavailable(result)) {
                        markBackendUnreachableAndSyncState()
                        addLocalCashMovement(type, amount, cat, desc)
                    } else {
                        setActionError(result.message)
                    }
                }
                is RepositoryResult.Exception -> {
                    markBackendUnreachableAndSyncState()
                    addLocalCashMovement(type, amount, cat, desc)
                }
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
        val localSessionId = resolveLocalSessionId()
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

    private fun setLoadError(
        message: String,
        offlineSessionAgeAlert: OfflineSessionAgeAlert? = _uiState.value.offlineSessionAgeAlert
    ) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSubmitting = false,
                offlineSessionAgeAlert = offlineSessionAgeAlert,
                errorMessage = message
            )
        }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
    }

    private suspend fun findExistingOpenSessionForCashier(
        cashierId: String?,
    ): LocalCashSessionEntity? {
        val dataSource = cashSessionLocalDataSource ?: return null
        val effectiveCashierId = cashierId?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        return runCatching {
            dataSource.getLatestOpenSession(effectiveCashierId)
                ?: resolveCashierNameFallback()?.let { fallbackCashierId ->
                    if (fallbackCashierId == effectiveCashierId) {
                        null
                    } else {
                        dataSource.getLatestOpenSession(fallbackCashierId)
                    }
                }
        }.getOrNull()
    }

    private suspend fun mirrorRemoteSessionSafely(session: CashSession): Long? {
        return mirrorRemoteSessionEntitySafely(session)?.localId
    }

    private suspend fun mirrorRemoteSessionEntitySafely(session: CashSession): LocalCashSessionEntity? {
        return runSuspendCatching {
            cashSessionLocalDataSource?.mirrorRemoteSession(session)
        }.getOrNull()
    }

    private suspend fun reconcileNoActiveRemoteSessionSafely() {
        runSuspendCatching {
            cashSessionLocalDataSource?.reconcileNoActiveRemoteSession(
                cashierUserId = resolveCashierUserId()
            )
        }
    }

    private suspend fun resolveLocalSessionId(): Long? {
        _uiState.value.localCashSessionId?.takeIf { it > 0L }?.let { return it }
        val activeId = _uiState.value.activeSession?.id?.trim()?.takeIf(String::isNotBlank)
        activeId?.removePrefix("LOCAL-")?.toLongOrNull()?.let { return it }
        if (activeId != null) {
            runCatching { cashSessionLocalDataSource?.getCachedCashSessionByServerId(activeId)?.localId }
                .getOrNull()
                ?.let { return it }
        }
        return findExistingOpenSessionForCashier(
            cashierId = resolveCashierUserId(),
        )?.localId
    }

    private suspend fun restoreLocalOpenCashSessionIfAvailable(
        page: Int,
    ): Boolean {
        val currentCashierUserId = resolveCashierUserId()
            ?: _uiState.value.activeSession?.userId?.takeIf(String::isNotBlank)
        val localSession = findExistingOpenSessionForCashier(
            cashierId = currentCashierUserId,
        ) ?: return false
        val localExpenses = localCashExpenseService?.getLocalExpenses(localSession.localId).orEmpty()
        val totalExpenses = localExpenses.totalExpenses()
        val totalCashIn = localExpenses.totalCashIn()
        val ageAlert = resolveOfflineSessionAgeAlert(localSession)

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
                offlineSessionAgeAlert = ageAlert,
                errorMessage = null,
                message = "Sesi kas aktif dari data lokal.\nServer tidak tersambung. Transaksi akan disimpan lokal."
            )
        }
        return true
    }

    private suspend fun resolveOfflineSessionAgeAlert(
        activeLocalSession: LocalCashSessionEntity? = null
    ): OfflineSessionAgeAlert? {
        val now = clockMillis()
        val activeCashierId = resolveCashierUserId()
        val fallbackName = resolveCashierNameFallback()
        val activeAlert = activeLocalSession
            ?.takeIf {
                it.deletedAt == null &&
                    it.serverId.isNullOrBlank() &&
                    it.syncStatus != SyncStatus.SYNCED
            }
            ?.let { OfflineSessionAgePolicy.evaluateOpenedAt(it.openedAt, nowMillis = now) }
        val unsyncedSessions = runSuspendCatching {
            cashSessionLocalDataSource?.getUnsyncedOfflineSessions().orEmpty()
        }.getOrDefault(emptyList()).filter { session ->
            activeCashierId != null && (
                session.cashierUserId == activeCashierId ||
                    (fallbackName != null && session.cashierUserId == fallbackName)
                )
        }
        val daoAlert = OfflineSessionAgePolicy.evaluateUnsyncedSessions(
            sessions = unsyncedSessions,
            nowMillis = now
        )
        return when {
            daoAlert == null -> activeAlert
            activeAlert == null -> daoAlert
            activeAlert.ageHours >= daoAlert.ageHours -> activeAlert
            else -> daoAlert
        }
    }

    private suspend fun shouldUseLocalCashSession(): Boolean {
        val repository = offlineStatusRepository ?: return false
        val status = latestOfflineStatus
        if (status.appDataMode == AppDataMode.OFFLINE_ONLY) return true
        if (!repository.isOnline()) return true

        return when (repository.currentBackendStatus()) {
            BackendStatus.CONNECTED -> {
                if (repository.isBackendHealthStale()) {
                    val refreshed = repository.refreshBackendHealth(force = false)
                    latestOfflineStatus = status.copy(
                        backendStatus = refreshed,
                        isServerReachable = refreshed == BackendStatus.CONNECTED,
                    )
                    refreshed != BackendStatus.CONNECTED
                } else {
                    false
                }
            }
            BackendStatus.UNREACHABLE -> true
            BackendStatus.UNKNOWN -> {
                val refreshed = repository.refreshBackendHealth(force = true)
                latestOfflineStatus = status.copy(
                    backendStatus = refreshed,
                    isServerReachable = refreshed == BackendStatus.CONNECTED,
                )
                refreshed != BackendStatus.CONNECTED
            }
        }
    }

    private fun markBackendUnreachableAndSyncState() {
        offlineStatusRepository?.markBackendUnreachable()
        latestOfflineStatus = latestOfflineStatus.copy(
            backendStatus = if (latestOfflineStatus.isInternetOnline) BackendStatus.UNREACHABLE else BackendStatus.UNKNOWN,
            isServerReachable = false,
        )
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

    companion object {
        fun factory(
            repository: CashReconciliationRepository,
            cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
            localCashSessionService: LocalCashSessionService? = null,
            localCashExpenseService: LocalCashExpenseService? = null,
            offlineStatusRepository: OfflineStatusRepository? = null,
            cashierUserId: String? = null,
            cashierNameFallback: String? = null,
            actorRole: String = "KASIR",
            clockMillis: () -> Long = { System.currentTimeMillis() },
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CashReconciliationViewModel(
                    repository = repository,
                    cashSessionLocalDataSource = cashSessionLocalDataSource,
                    localCashSessionService = localCashSessionService,
                    localCashExpenseService = localCashExpenseService,
                    offlineStatusRepository = offlineStatusRepository,
                    cashierUserId = cashierUserId,
                    cashierNameFallback = cashierNameFallback,
                    actorRole = actorRole,
                    clockMillis = clockMillis,
                )
            }
        }
    }
}

private fun LocalCashSessionEntity.toCashSession(
    totalExpenses: BigDecimal = BigDecimal.ZERO,
    totalCashIn: BigDecimal = BigDecimal.ZERO
): CashSession {
    val baseSystemCash = expectedCash?.toBigDecimal() ?: startingCash.toBigDecimal()
    return CashSession(
        id = serverId ?: "LOCAL-$localId",
        userId = cashierUserId,
        userName = null,
        openedAt = openedAt.toOffsetDateTimeString(),
        closedAt = closedAt?.toOffsetDateTimeString(),
        openingCash = startingCash.toBigDecimal(),
        closingCash = actualCash?.toBigDecimal(),
        systemCash = baseSystemCash.subtract(totalExpenses).add(totalCashIn),
        difference = difference?.toBigDecimal(),
        totalExpenses = totalExpenses,
        totalCashIn = totalCashIn,
        notes = openingNote,
        status = status
    )
}

private fun List<LocalCashExpenseEntity>.totalExpenses(): BigDecimal {
    return filter { !it.category.orEmpty().startsWith("CASH_IN", ignoreCase = true) && !it.category.orEmpty().startsWith("KAS_MASUK", ignoreCase = true) }
        .fold(BigDecimal.ZERO) { acc, expense -> acc.add(expense.amount.toBigDecimal()) }
}

private fun List<LocalCashExpenseEntity>.totalCashIn(): BigDecimal {
    return filter { it.category.orEmpty().startsWith("CASH_IN", ignoreCase = true) || it.category.orEmpty().startsWith("KAS_MASUK", ignoreCase = true) }
        .fold(BigDecimal.ZERO) { acc, expense -> acc.add(expense.amount.toBigDecimal()) }
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
        total = amount.toBigDecimal(),
        paidAmount = amount.toBigDecimal(),
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
