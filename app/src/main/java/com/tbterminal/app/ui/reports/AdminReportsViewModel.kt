package com.tbterminal.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.AnalyticsFilter
import com.tbterminal.app.data.repository.ReportCsvType
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.OutputStream

data class AdminReportsUiState(
    val isLoading: Boolean = false,
    val dashboardMetrics: DashboardMetricsDto? = null,
    val dailySales: List<DailySalesSummaryDto> = emptyList(),
    val error: String? = null,
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val endDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val transactions: List<CashTransaction> = emptyList(),
    val isTransactionsLoading: Boolean = false,
    val transactionsError: String? = null,
    val transactionPage: Int = 1,
    val transactionLimit: Int = 10,
    val transactionTotal: Long = 0,
    val transactionTotalPages: Int = 1,
    val transactionCurrentStart: Int = 0,
    val transactionCurrentEnd: Int = 0,
    val salesReport: SalesReportResponseDto? = null,
    val isLoadingSalesReport: Boolean = false,
    val salesReportError: String? = null,
    val isExporting: Boolean = false,
    val exportMessage: String? = null
)

class AdminReportsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val cashReconciliationRepository: CashReconciliationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val metrics = analyticsRepository.getDashboardMetrics()
                val sales = analyticsRepository.getDailySales(
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dashboardMetrics = metrics,
                    dailySales = sales
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Terjadi kesalahan saat memuat laporan"
                )
            }
        }
        loadTransactions(page = _uiState.value.transactionPage)
        loadSalesReport()
    }

    fun loadSalesReport() {
        _uiState.value = _uiState.value.copy(
            isLoadingSalesReport = true,
            salesReportError = null
        )
        viewModelScope.launch {
            try {
                val report = analyticsRepository.getSalesReport(
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate,
                    topProductsLimit = 10
                )
                _uiState.value = _uiState.value.copy(
                    salesReport = report,
                    isLoadingSalesReport = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSalesReport = false,
                    salesReportError = e.message ?: "Laporan penjualan agregat gagal dimuat"
                )
            }
        }
    }

    fun loadTransactions(page: Int = 1) {
        _uiState.value = _uiState.value.copy(
            isTransactionsLoading = true,
            transactionsError = null
        )
        viewModelScope.launch {
            when (
                val result = cashReconciliationRepository.getTransactions(
                    page = page,
                    limit = _uiState.value.transactionLimit,
                    sessionId = null,
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate
                )
            ) {
                is RepositoryResult.Success -> {
                    val data = result.data.data
                    val currentStart = if (data.isEmpty()) 0 else (result.data.page - 1) * result.data.limit + 1
                    val currentEnd = if (data.isEmpty()) 0 else currentStart + data.size - 1
                    _uiState.value = _uiState.value.copy(
                        transactions = data,
                        isTransactionsLoading = false,
                        transactionPage = result.data.page,
                        transactionLimit = result.data.limit,
                        transactionTotal = result.data.total,
                        transactionTotalPages = result.data.totalPages.coerceAtLeast(1),
                        transactionCurrentStart = currentStart,
                        transactionCurrentEnd = currentEnd
                    )
                }
                is RepositoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTransactionsLoading = false,
                        transactionsError = result.message
                    )
                }
                is RepositoryResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isTransactionsLoading = false,
                        transactionsError = "Koneksi ke server gagal"
                    )
                }
            }
        }
    }

    fun nextTransactionPage() {
        val state = _uiState.value
        if (state.transactionPage < state.transactionTotalPages) {
            loadTransactions(page = state.transactionPage + 1)
        }
    }

    fun previousTransactionPage() {
        val state = _uiState.value
        if (state.transactionPage > 1) {
            loadTransactions(page = state.transactionPage - 1)
        }
    }

    fun setDateRange(start: LocalDate, end: LocalDate) {
        _uiState.value = _uiState.value.copy(
            startDate = start.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate = end.format(DateTimeFormatter.ISO_LOCAL_DATE),
            transactionPage = 1
        )
        loadReports()
    }

    fun exportCsv(type: ReportCsvType, output: OutputStream) {
        if (_uiState.value.isExporting) {
            output.close()
            return
        }
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        viewModelScope.launch {
            try {
                val state = _uiState.value
                analyticsRepository.exportCsv(
                    type = type,
                    filter = AnalyticsFilter(startDate = state.startDate, endDate = state.endDate),
                    output = output
                )
                _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = "Ekspor ${type.label} berhasil disimpan")
            } catch (e: Exception) {
                runCatching { output.close() }
                _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = e.message ?: "Ekspor gagal")
            }
        }
    }

    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }

    companion object {
        fun factory(
            analyticsRepository: AnalyticsRepository,
            cashReconciliationRepository: CashReconciliationRepository
        ): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AdminReportsViewModel(
                        analyticsRepository = analyticsRepository,
                        cashReconciliationRepository = cashReconciliationRepository
                    ) as T
                }
            }
    }
}
