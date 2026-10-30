package com.tbterminal.app.ui.dashboard.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val metrics: DashboardMetricsDto? = null,
    val financialTotals: SalesReportTotalsDto? = null,
    val fullSalesReport: SalesReportResponseDto? = null,
    val dailySalesTrend: List<DailySalesSummaryDto> = emptyList(),
    val recentTransactions: List<CashTransaction> = emptyList(),
    val activeCashierSession: CashSession? = null,
    val selectedPeriodDays: Int = 1, // 1 = Hari Ini, 7 = 7 Hari, 30 = Bulan Ini
    val financialError: String? = null,
    val error: String? = null
)

class AdminDashboardViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val cashReconciliationRepository: CashReconciliationRepository? = null,
    private val includeFinancialSummary: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun setPeriod(days: Int) {
        if (_uiState.value.selectedPeriodDays == days) return
        _uiState.update { it.copy(selectedPeriodDays = days) }
        loadMetrics(periodDays = days)
    }

    fun loadMetrics(periodDays: Int = _uiState.value.selectedPeriodDays) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = analyticsRepository.getDashboardMetrics()

                val today = LocalDate.now()
                val startDate = if (periodDays == 1) {
                    today.toString()
                } else if (periodDays == 7) {
                    today.minusDays(6).toString()
                } else {
                    today.withDayOfMonth(1).toString()
                }
                val endDate = today.toString()

                val fullReportResult = if (includeFinancialSummary) {
                    runCatching {
                        analyticsRepository.getSalesReport(startDate = startDate, endDate = endDate)
                    }
                } else {
                    null
                }

                val trendResult = if (includeFinancialSummary) {
                    runCatching {
                        val sevenDaysAgo = today.minusDays(6).toString()
                        analyticsRepository.getDailySales(startDate = sevenDaysAgo, endDate = endDate)
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }

                var recentTrx: List<CashTransaction> = emptyList()
                var activeSession: CashSession? = null

                if (cashReconciliationRepository != null) {
                    runCatching {
                        when (val sessionRes = cashReconciliationRepository.getActiveSession()) {
                            is RepositoryResult.Success -> activeSession = sessionRes.data
                            else -> {}
                        }
                    }
                    runCatching {
                        when (val trxRes = cashReconciliationRepository.getTransactions(page = 1, limit = 5, sessionId = null)) {
                            is RepositoryResult.Success -> recentTrx = trxRes.data.data
                            else -> {}
                        }
                    }
                }

                val report = fullReportResult?.getOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        metrics = data,
                        fullSalesReport = report,
                        financialTotals = report?.totals,
                        dailySalesTrend = trendResult,
                        recentTransactions = recentTrx,
                        activeCashierSession = activeSession,
                        financialError = fullReportResult?.exceptionOrNull()?.message,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(
            analyticsRepository: AnalyticsRepository,
            cashReconciliationRepository: CashReconciliationRepository? = null,
            includeFinancialSummary: Boolean = false,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminDashboardViewModel(
                        analyticsRepository = analyticsRepository,
                        cashReconciliationRepository = cashReconciliationRepository,
                        includeFinancialSummary = includeFinancialSummary
                    ) as T
                }
            }
    }
}
