package com.tbterminal.app.ui.dashboard.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.ServerUnavailabilityClassifier
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.data.sync.runSuspendCatching
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tbterminal.app.ui.common.toDisplayTime
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

data class CashierDashboardUiState(
    val isLoading: Boolean = false,
    val activeSession: CashSession? = null,
    val recentTransactions: List<CashTransaction> = emptyList(),
    val todayTransactionsCount: Int = 0,
    val todaySalesRevenue: BigDecimal = BigDecimal.ZERO,
    val openingCash: BigDecimal = BigDecimal.ZERO,
    val systemCash: BigDecimal = BigDecimal.ZERO,
    val cashRevenue: BigDecimal = BigDecimal.ZERO,
    val qrisRevenue: BigDecimal = BigDecimal.ZERO,
    val receivableRevenue: BigDecimal = BigDecimal.ZERO,
    val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val totalCashIn: BigDecimal = BigDecimal.ZERO,
    val shiftOpenedTimeFormatted: String? = null,
    val shiftDurationFormatted: String? = null,
    val averageTicket: BigDecimal = BigDecimal.ZERO,
    val errorMessage: String? = null
)

class CashierDashboardViewModel(
    private val repository: CashReconciliationRepository,
    private val cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    private val localCashExpenseService: LocalCashExpenseService? = null,
    private val offlineStatusRepository: OfflineStatusRepository? = null,
    private val cashierUserId: String? = null,
    private val cashierNameFallback: String? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashierDashboardUiState())
    val uiState: StateFlow<CashierDashboardUiState> = _uiState.asStateFlow()
    private var latestOfflineStatus: OfflineStatus = OfflineStatus()

    init {
        observeOfflineStatus()
        loadDashboardData()
    }

    private fun observeOfflineStatus() {
        val statusRepo = offlineStatusRepository ?: return
        viewModelScope.launch {
            statusRepo.status.collect { status ->
                latestOfflineStatus = status
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (shouldUseLocal()) {
                checkFallbackLocalSession()
                return@launch
            }

            when (val sessionResult = repository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val session = sessionResult.data
                    if (session != null) {
                        loadSessionTransactions(session)
                    } else {
                        reconcileNoActiveRemoteSessionSafely()
                        resetEmptySessionState()
                    }
                }
                is RepositoryResult.Error -> {
                    val serverUnavailable = ServerUnavailabilityClassifier.isServerUnavailable(sessionResult)
                    if (serverUnavailable) {
                        offlineStatusRepository?.markBackendUnreachable()
                    }
                    if (!serverUnavailable || !checkFallbackLocalSession()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = sessionResult.message) }
                    }
                }
                is RepositoryResult.Exception -> {
                    val serverUnavailable = ServerUnavailabilityClassifier.isServerUnavailable(sessionResult)
                    if (serverUnavailable) {
                        offlineStatusRepository?.markBackendUnreachable()
                    }
                    if (!serverUnavailable || !checkFallbackLocalSession()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Koneksi bermasalah. Gagal memuat dashboard.") }
                    }
                }
            }
        }
    }

    private suspend fun shouldUseLocal(): Boolean {
        val statusRepo = offlineStatusRepository ?: return false
        val status = latestOfflineStatus
        if (status.appDataMode == AppDataMode.OFFLINE_ONLY) return true
        if (!statusRepo.isOnline()) return true
        return when (statusRepo.currentBackendStatus()) {
            BackendStatus.CONNECTED -> {
                if (statusRepo.isBackendHealthStale()) {
                    statusRepo.refreshBackendHealth(force = false) != BackendStatus.CONNECTED
                } else {
                    false
                }
            }
            BackendStatus.UNREACHABLE -> true
            BackendStatus.UNKNOWN -> statusRepo.refreshBackendHealth(force = true) != BackendStatus.CONNECTED
        }
    }

    private suspend fun reconcileNoActiveRemoteSessionSafely() {
        val dataSource = cashSessionLocalDataSource ?: return
        val primaryId = resolveCashierUserId()
        runSuspendCatching {
            dataSource.reconcileNoActiveRemoteSession(cashierUserId = primaryId)
        }
        val fallbackId = resolveCashierNameFallback()
        if (fallbackId != null && fallbackId != primaryId) {
            runSuspendCatching {
                dataSource.reconcileNoActiveRemoteSession(cashierUserId = fallbackId)
            }
        }
    }

    private suspend fun checkFallbackLocalSession(): Boolean {
        val dataSource = cashSessionLocalDataSource
        val localSession = if (dataSource == null) {
            null
        } else {
            val primaryId = resolveCashierUserId()
            if (primaryId == null) {
                null
            } else {
                runSuspendCatching {
                    dataSource.getLatestOpenSession(primaryId)
                        ?: resolveCashierNameFallback()?.takeIf { it != primaryId }?.let { fallbackId ->
                            dataSource.getLatestOpenSession(fallbackId)
                        }
                }.getOrNull()
            }
        }
        return if (localSession != null) {
            val session = CashSession(
                id = localSession.serverId ?: localSession.clientGeneratedId,
                userId = localSession.cashierUserId,
                userName = null,
                openedAt = java.time.Instant.ofEpochMilli(localSession.openedAt).toString(),
                closedAt = localSession.closedAt?.let { java.time.Instant.ofEpochMilli(it).toString() },
                openingCash = localSession.startingCash.toBigDecimal(),
                closingCash = localSession.actualCash?.toBigDecimal(),
                systemCash = localSession.expectedCash?.toBigDecimal() ?: localSession.startingCash.toBigDecimal(),
                difference = localSession.difference?.toBigDecimal(),
                totalExpenses = BigDecimal.ZERO,
                totalCashIn = BigDecimal.ZERO,
                notes = localSession.openingNote,
                status = localSession.status
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    openingCash = session.openingCash,
                    systemCash = session.systemCash ?: session.openingCash,
                    totalExpenses = session.totalExpenses,
                    totalCashIn = session.totalCashIn,
                    shiftOpenedTimeFormatted = session.openedAt.toDisplayTime(),
                    shiftDurationFormatted = formatShiftDuration(session.openedAt),
                    errorMessage = null
                )
            }
            true
        } else {
            resetEmptySessionState()
            false
        }
    }

    private fun resetEmptySessionState() {
        _uiState.update {
            it.copy(
                isLoading = false,
                activeSession = null,
                systemCash = BigDecimal.ZERO,
                openingCash = BigDecimal.ZERO,
                totalExpenses = BigDecimal.ZERO,
                totalCashIn = BigDecimal.ZERO,
                cashRevenue = BigDecimal.ZERO,
                qrisRevenue = BigDecimal.ZERO,
                receivableRevenue = BigDecimal.ZERO,
                shiftOpenedTimeFormatted = null,
                shiftDurationFormatted = null,
                averageTicket = BigDecimal.ZERO,
                todayTransactionsCount = 0,
                todaySalesRevenue = BigDecimal.ZERO,
                recentTransactions = emptyList()
            )
        }
    }

    private fun resolveCashierUserId(): String? {
        return cashierUserId?.trim()?.takeIf(String::isNotBlank)
    }

    private fun resolveCashierNameFallback(): String? {
        return cashierNameFallback?.trim()?.takeIf(String::isNotBlank)
    }

    private fun formatShiftDuration(isoDate: String): String {
        return runCatching {
            val openInstant = try {
                Instant.parse(isoDate)
            } catch (_: Throwable) {
                OffsetDateTime.parse(isoDate).toInstant()
            }
            val duration = Duration.between(openInstant, Instant.now())
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            if (hours > 0) "${hours}j ${minutes}m" else "${minutes} mnt"
        }.getOrDefault("-")
    }

    private suspend fun loadSessionTransactions(session: CashSession) {
        val txResult = repository.getTransactions(page = 1, limit = 1000, sessionId = session.id)

        if (txResult is RepositoryResult.Success) {
            val transactionsList = txResult.data.data

            val validTransactions = transactionsList.filter { 
                it.type != "EXPENSE" && 
                !it.status.equals("BATAL", ignoreCase = true) && 
                !it.status.equals("VOIDED", ignoreCase = true) 
            }

            val totalRevenue = validTransactions.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.total) }

            val cashRevenue = validTransactions.filter { t ->
                t.paymentMethods.any { m -> m.contains("TUNAI", ignoreCase = true) || m.contains("CASH", ignoreCase = true) }
            }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.paidAmount) }

            val qrisRevenue = validTransactions.filter { t ->
                t.paymentMethods.any { m -> m.contains("QRIS", ignoreCase = true) || m.contains("TRANSFER", ignoreCase = true) }
            }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.paidAmount) }

            val receivableRevenue = validTransactions.filter { t ->
                t.paymentMethods.any { m -> m.contains("HUTANG", ignoreCase = true) || m.contains("TEMPO", ignoreCase = true) || m.contains("RECEIVABLE", ignoreCase = true) } ||
                (t.remainingAmount != null && t.remainingAmount > BigDecimal.ZERO)
            }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.remainingAmount ?: BigDecimal.ZERO) }

            val averageTicket = if (validTransactions.isNotEmpty()) {
                totalRevenue.divide(BigDecimal(validTransactions.size), 0, java.math.RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            val shiftTime = session.openedAt.toDisplayTime()
            val shiftDur = formatShiftDuration(session.openedAt)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    openingCash = session.openingCash,
                    systemCash = session.systemCash ?: session.openingCash,
                    totalExpenses = session.totalExpenses,
                    totalCashIn = session.totalCashIn,
                    todayTransactionsCount = validTransactions.size,
                    todaySalesRevenue = totalRevenue,
                    cashRevenue = cashRevenue,
                    qrisRevenue = qrisRevenue,
                    receivableRevenue = receivableRevenue,
                    shiftOpenedTimeFormatted = shiftTime,
                    shiftDurationFormatted = shiftDur,
                    averageTicket = averageTicket,
                    recentTransactions = validTransactions.take(5)
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    openingCash = session.openingCash,
                    systemCash = session.systemCash ?: session.openingCash,
                    totalExpenses = session.totalExpenses,
                    totalCashIn = session.totalCashIn,
                    shiftOpenedTimeFormatted = session.openedAt.toDisplayTime(),
                    shiftDurationFormatted = formatShiftDuration(session.openedAt)
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun factory(
            repository: CashReconciliationRepository,
            cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
            localCashExpenseService: LocalCashExpenseService? = null,
            offlineStatusRepository: OfflineStatusRepository? = null,
            cashierUserId: String? = null,
            cashierNameFallback: String? = null,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CashierDashboardViewModel(
                    repository = repository,
                    cashSessionLocalDataSource = cashSessionLocalDataSource,
                    localCashExpenseService = localCashExpenseService,
                    offlineStatusRepository = offlineStatusRepository,
                    cashierUserId = cashierUserId,
                    cashierNameFallback = cashierNameFallback,
                )
            }
        }
    }
}
