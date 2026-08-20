package com.tbterminal.app.ui.offlinereports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.dao.OfflineCashReportRow
import com.tbterminal.app.data.local.dao.OfflineExpenseReportRow
import com.tbterminal.app.data.local.dao.OfflineReceivableReportRow
import com.tbterminal.app.data.local.dao.OfflineSalesReportRow
import com.tbterminal.app.data.local.dao.OfflineSalesReportSummary
import com.tbterminal.app.data.repository.OfflineReportRepository
import com.tbterminal.app.ui.common.viewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OfflineReportViewModel(
    private val repository: OfflineReportRepository
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()
    private var reportJob: Job? = null

    private val _uiState = MutableStateFlow(
        OfflineReportUiState(
            startDate = today,
            endDate = today,
            customStartInput = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
            customEndInput = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    )
    val uiState: StateFlow<OfflineReportUiState> = _uiState.asStateFlow()

    init {
        observeReports()
    }

    fun selectPreset(preset: OfflineReportPreset) {
        val now = LocalDate.now()
        val range = when (preset) {
            OfflineReportPreset.Today -> now to now
            OfflineReportPreset.Last7Days -> now.minusDays(6) to now
            OfflineReportPreset.ThisMonth -> now.withDayOfMonth(1) to now
            OfflineReportPreset.Custom -> _uiState.value.startDate to _uiState.value.endDate
        }
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                startDate = range.first,
                endDate = range.second,
                customStartInput = range.first.format(DateTimeFormatter.ISO_LOCAL_DATE),
                customEndInput = range.second.format(DateTimeFormatter.ISO_LOCAL_DATE),
                errorMessage = null
            )
        }
        observeReports()
    }

    fun updateCustomStart(value: String) {
        _uiState.update { it.copy(customStartInput = value, errorMessage = null) }
    }

    fun updateCustomEnd(value: String) {
        _uiState.update { it.copy(customEndInput = value, errorMessage = null) }
    }

    fun applyCustomRange() {
        val current = _uiState.value
        val start = runCatching { LocalDate.parse(current.customStartInput) }.getOrNull()
        val end = runCatching { LocalDate.parse(current.customEndInput) }.getOrNull()
        if (start == null || end == null) {
            _uiState.update { it.copy(errorMessage = "Format tanggal harus yyyy-MM-dd.") }
            return
        }
        if (end.isBefore(start)) {
            _uiState.update { it.copy(errorMessage = "Tanggal akhir tidak boleh sebelum tanggal awal.") }
            return
        }
        _uiState.update {
            it.copy(
                selectedPreset = OfflineReportPreset.Custom,
                startDate = start,
                endDate = end,
                errorMessage = null
            )
        }
        observeReports()
    }

    fun refresh() {
        observeReports()
    }

    private fun observeReports() {
        val state = _uiState.value
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.observeReports(
                startDate = state.startDate,
                endDate = state.endDate
            ).collect { snapshot ->
                _uiState.update {
                    it.copy(
                        salesSummary = snapshot.salesSummary,
                        transactions = snapshot.transactions,
                        cashSessions = snapshot.cashSessions,
                        receivables = snapshot.receivables,
                        expenses = snapshot.expenses,
                        lastRefresh = snapshot.lastRefresh,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: OfflineReportRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                OfflineReportViewModel(repository)
            }
        }
    }
}

data class OfflineReportUiState(
    val selectedPreset: OfflineReportPreset = OfflineReportPreset.Today,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val customStartInput: String,
    val customEndInput: String,
    val salesSummary: OfflineSalesReportSummary = OfflineSalesReportSummary(),
    val transactions: List<OfflineSalesReportRow> = emptyList(),
    val cashSessions: List<OfflineCashReportRow> = emptyList(),
    val receivables: List<OfflineReceivableReportRow> = emptyList(),
    val expenses: List<OfflineExpenseReportRow> = emptyList(),
    val lastRefresh: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

enum class OfflineReportPreset(val label: String) {
    Today("Hari ini"),
    Last7Days("7 hari"),
    ThisMonth("Bulan ini"),
    Custom("Custom")
}
