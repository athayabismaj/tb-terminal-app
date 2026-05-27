package com.tbterminal.app.ui.dashboard.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val systemCash: BigDecimal = BigDecimal.ZERO,
    val errorMessage: String? = null
)

class CashierDashboardViewModel(
    private val repository: CashReconciliationRepository
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
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                activeSession = null,
                                systemCash = BigDecimal.ZERO,
                                todayTransactionsCount = 0,
                                recentTransactions = emptyList()
                            ) 
                        }
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = sessionResult.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Koneksi bermasalah. Gagal memuat dashboard.") }
                }
            }
        }
    }

    private suspend fun loadSessionTransactions(session: CashSession) {
        val txResult = repository.getTransactions(page = 1, limit = 1000, sessionId = session.id)
        
        if (txResult is RepositoryResult.Success) {
            val transactionsList = txResult.data.data
            
            val validTransactions = transactionsList.filter { it.type != "EXPENSE" && it.status != "BATAL" }
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    systemCash = session.systemCash ?: session.openingCash,
                    todayTransactionsCount = validTransactions.size,
                    recentTransactions = validTransactions.take(3)
                )
            }
        } else {
            // Just update session if tx load fails
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = session,
                    systemCash = session.systemCash ?: session.openingCash
                )
            }
        }
    }

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                CashierDashboardViewModel(repository)
            }
        }
    }
}
