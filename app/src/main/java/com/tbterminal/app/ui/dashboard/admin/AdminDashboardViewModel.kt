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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
    val selectedPeriodDays: Int = 1, // 1 = Hari Ini, 7 = 7 Hari, 30 = Bulan Ini, 0 = Kustom
    val customStartDateMillis: Long? = null,
    val customEndDateMillis: Long? = null,
    val previousPeriodNetRevenue: Double? = null,
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
        if (_uiState.value.selectedPeriodDays == days && _uiState.value.customStartDateMillis == null) return
        _uiState.update {
            it.copy(
                selectedPeriodDays = days,
                customStartDateMillis = null,
                customEndDateMillis = null
            )
        }
        loadMetrics(periodDays = days)
    }

    fun setCustomRange(startDateMillis: Long, endDateMillis: Long) {
        val normalizedStart = minOf(startDateMillis, endDateMillis)
        val normalizedEnd = maxOf(startDateMillis, endDateMillis)
        _uiState.update {
            it.copy(
                selectedPeriodDays = 0,
                customStartDateMillis = normalizedStart,
                customEndDateMillis = normalizedEnd
            )
        }
        loadMetrics(
            periodDays = 0,
            customStartMillis = normalizedStart,
            customEndMillis = normalizedEnd
        )
    }

    fun loadMetrics(
        periodDays: Int = _uiState.value.selectedPeriodDays,
        customStartMillis: Long? = _uiState.value.customStartDateMillis,
        customEndMillis: Long? = _uiState.value.customEndDateMillis
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = analyticsRepository.getDashboardMetrics()

                val today = LocalDate.now()
                val (startLocal, endLocal) = if (periodDays == 0 && customStartMillis != null && customEndMillis != null) {
                    val s = Instant.ofEpochMilli(customStartMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    val e = Instant.ofEpochMilli(customEndMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    s to e
                } else if (periodDays == 1) {
                    today to today
                } else if (periodDays == 7) {
                    today.minusDays(6) to today
                } else {
                    today.withDayOfMonth(1) to today
                }
                val startDate = startLocal.toString()
                val endDate = endLocal.toString()

                val fullReportResult = if (includeFinancialSummary) {
                    runCatching {
                        analyticsRepository.getSalesReport(startDate = startDate, endDate = endDate)
                    }
                } else {
                    null
                }

                val previousPeriodNetRevenue = if (includeFinancialSummary) {
                    val spanDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal).coerceAtLeast(0L) + 1L
                    val prevEnd = startLocal.minusDays(1)
                    val prevStart = prevEnd.minusDays(spanDays - 1L)
                    runCatching {
                        analyticsRepository.getSalesReport(
                            startDate = prevStart.toString(),
                            endDate = prevEnd.toString()
                        ).totals.netRevenue.toDouble()
                    }.getOrNull()
                } else {
                    null
                }

                val trendResult = if (includeFinancialSummary) {
                    runCatching {
                        val sevenDaysAgo = endLocal.minusDays(6).toString()
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
                        previousPeriodNetRevenue = previousPeriodNetRevenue,
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
