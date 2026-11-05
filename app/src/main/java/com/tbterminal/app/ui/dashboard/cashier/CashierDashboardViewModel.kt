package com.tbterminal.app.ui.dashboard.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CashierDashboardUiState(
    val isLoading: Boolean = false,
    val activeSession: CashSession? = null,
    val recentTransactions: List<CashTransaction> = emptyList(),
    val todayTransactionsCount: Int = 0,
    val todaySalesRevenue: BigDecimal = BigDecimal.ZERO,
    val openingCash: BigDecimal = BigDecimal.ZERO,
    val systemCash: BigDecimal = BigDecimal.ZERO,
    val errorMessage: String? = null
)

class CashierDashboardViewModel(
    private val repository: CashReconciliationRepository,
    private val cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    private val localCashExpenseService: LocalCashExpenseService? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashierDashboardUiState())
    val uiState: StateFlow<CashierDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val sessionResult = repository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val session = sessionResult.data
                    if (session != null) {
                        loadSessionTransactions(session)
                    } else {
                        checkFallbackLocalSession()
                    }
                }
                is RepositoryResult.Error -> {
                    if (!checkFallbackLocalSession()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = sessionResult.message) }
                    }
                }
                is RepositoryResult.Exception -> {
                    if (!checkFallbackLocalSession()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Koneksi bermasalah. Gagal memuat dashboard.") }
                    }
                }
            }
        }
    }

    private suspend fun checkFallbackLocalSession(): Boolean {
        val localSession = cashSessionLocalDataSource?.getLatestOpenSession()
        return if (localSession != null) {
            val session = CashSession(
                id = localSession.serverId ?: localSession.clientGeneratedId,
                userId = localSession.cashierUserId,
                userName = null,
                openedAt = java.time.Instant.ofEpochMilli(localSession.openedAt).toString(),
                closedAt = localSession.closedAt?.let { java.time.Instant.ofEpochMilli(it).toString() },
                openingCash = BigDecimal.valueOf(localSession.startingCash),
                closingCash = localSession.actualCash?.let { BigDecimal.valueOf(it) },
                systemCash = localSession.expectedCash?.let { BigDecimal.valueOf(it) } ?: BigDecimal.valueOf(localSession.startingCash),
                difference = localSession.difference?.let { BigDecimal.valueOf(it) },
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
                    errorMessage = null
                )
            }
            true
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = null,
                    systemCash = BigDecimal.ZERO,
                    openingCash = BigDecimal.ZERO,
                    todayTransactionsCount = 0,
                    todaySalesRevenue = BigDecimal.ZERO,
                    recentTransactions = emptyList()
                )
            }
            false
        }
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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    openingCash = session.openingCash,
                    systemCash = session.systemCash ?: session.openingCash,
                    todayTransactionsCount = validTransactions.size,
                    todaySalesRevenue = totalRevenue,
                    recentTransactions = validTransactions.take(5)
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    openingCash = session.openingCash,
                    systemCash = session.systemCash ?: session.openingCash
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
            localCashExpenseService: LocalCashExpenseService? = null
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CashierDashboardViewModel(
                    repository = repository,
                    cashSessionLocalDataSource = cashSessionLocalDataSource,
                    localCashExpenseService = localCashExpenseService
                )
            }
        }
    }
}
