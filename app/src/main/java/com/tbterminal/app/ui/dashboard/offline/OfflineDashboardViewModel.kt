package com.tbterminal.app.ui.dashboard.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.OfflineDashboardRepository
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OfflineDashboardViewModel(
    private val repository: OfflineDashboardRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OfflineDashboardUiState())
    val uiState: StateFlow<OfflineDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTodaySnapshot().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        localRevenueToday = snapshot.localRevenueToday,
                        localCollectedToday = snapshot.localCollectedToday,
                        localReceivableOutstanding = snapshot.localReceivableOutstanding,
                        localExpenseToday = snapshot.localExpenseToday,
                        localTransactionCount = snapshot.localTransactionCount,
                        pendingSyncCount = snapshot.pendingSyncCount,
                        failedSyncCount = snapshot.failedSyncCount,
                        lastRefresh = snapshot.lastRefresh,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: OfflineDashboardRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                OfflineDashboardViewModel(repository)
            }
        }
    }
}

data class OfflineDashboardUiState(
    val localRevenueToday: Double = 0.0,
    val localCollectedToday: Double = 0.0,
    val localReceivableOutstanding: Double = 0.0,
    val localExpenseToday: Double = 0.0,
    val localTransactionCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val lastRefresh: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
    val error: String? = null
)
