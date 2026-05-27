package com.tbterminal.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.repository.AnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AdminReportsUiState(
    val isLoading: Boolean = false,
    val dashboardMetrics: DashboardMetricsDto? = null,
    val dailySales: List<DailySalesSummaryDto> = emptyList(),
    val error: String? = null,
    val startDate: String = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE),
    val endDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
)

class AdminReportsViewModel(
    private val analyticsRepository: AnalyticsRepository
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
    }

    fun setDateRange(start: LocalDate, end: LocalDate) {
        _uiState.value = _uiState.value.copy(
            startDate = start.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        loadReports()
    }
}
