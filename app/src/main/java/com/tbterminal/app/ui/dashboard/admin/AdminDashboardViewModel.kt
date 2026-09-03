package com.tbterminal.app.ui.dashboard.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.data.repository.AnalyticsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val metrics: DashboardMetricsDto? = null,
    val financialTotals: SalesReportTotalsDto? = null,
    val financialError: String? = null,
    val error: String? = null
)

class AdminDashboardViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val includeFinancialSummary: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = analyticsRepository.getDashboardMetrics()
                val financialResult = if (includeFinancialSummary) {
                    runCatching {
                        val today = LocalDate.now().toString()
                        analyticsRepository.getSalesReport(startDate = today, endDate = today).totals
                    }
                } else {
                    null
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        metrics = data,
                        financialTotals = financialResult?.getOrNull(),
                        financialError = financialResult?.exceptionOrNull()?.message,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message ?: "Terjadi kesalahan yang tidak diketahui") 
                }
            }
        }
    }

    fun refresh() = loadMetrics()

    companion object {
        fun factory(
            analyticsRepository: AnalyticsRepository,
            includeFinancialSummary: Boolean = false,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminDashboardViewModel(analyticsRepository, includeFinancialSummary) as T
                }
            }
    }
}
