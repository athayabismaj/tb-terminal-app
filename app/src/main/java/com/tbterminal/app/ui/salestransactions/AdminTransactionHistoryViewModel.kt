package com.tbterminal.app.ui.salestransactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.CashReconciliationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AdminTransactionHistoryUiState(
    val transactions: List<CashTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val statusFilter: String = "Semua", // Semua, Lunas, DP, Hutang
    val selectedDate: String? = LocalDate.now().toString(),
    val startDate: String? = LocalDate.now().toString(),
    val endDate: String? = LocalDate.now().toString(),
    val selectedPreset: String? = "Hari ini",
    val currentStart: Int = 0,
    val currentEnd: Int = 0,
    val total: Long = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val hasMorePages: Boolean = false
)

class AdminTransactionHistoryViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTransactionHistoryUiState())
    val uiState: StateFlow<AdminTransactionHistoryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTransactions()
    }

    fun loadTransactions(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Untuk Admin, kita pass sessionId = null agar menarik semua transaksi
            val statusParam = if (_uiState.value.statusFilter == "Semua") null else _uiState.value.statusFilter
            val searchParam = _uiState.value.query.takeIf { it.isNotBlank() }

            val startOfDay = _uiState.value.startDate ?: _uiState.value.selectedDate
            val endOfDay = _uiState.value.endDate ?: _uiState.value.selectedDate

            when (
                val result = repository.getTransactions(
                    page = page,
                    limit = 50,
                    sessionId = null,
                    search = searchParam,
                    status = statusParam,
                    startDate = startOfDay,
                    endDate = endOfDay
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        val data = result.data.data
                        val currentStart = if (data.isEmpty()) 0 else (result.data.page - 1) * 50 + 1
                        val currentEnd = currentStart + data.size - 1
                        it.copy(
                            transactions = data,
                            page = result.data.page,
                            totalPages = result.data.totalPages,
                            total = result.data.total,
                            currentStart = currentStart,
                            currentEnd = currentEnd,
                            hasMorePages = result.data.page < result.data.totalPages,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, error = "Koneksi ke server gagal") }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            loadTransactions(page = 1)
        }
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status, page = 1) }
        loadTransactions(page = 1)
    }

    fun setDate(date: String?) {
        val resolvedDate = date ?: LocalDate.now().toString()
        _uiState.update {
            it.copy(
                selectedDate = resolvedDate,
                startDate = resolvedDate,
                endDate = resolvedDate,
                selectedPreset = if (date == null) "Hari ini" else null,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    fun setDatePreset(preset: String) {
        val today = LocalDate.now()
        val startDate = when (preset) {
            "Minggu ini", "7 hari" -> today.with(DayOfWeek.MONDAY)
            "Bulan ini", "30 hari" -> today.withDayOfMonth(1)
            else -> today
        }
        val selectedPreset = when (preset) {
            "7 hari" -> "Minggu ini"
            "30 hari" -> "Bulan ini"
            else -> preset
        }
        setDateRange(startDate, today, selectedPreset)
    }

    fun nextDate() {
        val state = _uiState.value
        val (startDate, endDate) = currentDateRange()
        val today = LocalDate.now()
        if (endDate >= today) return

        when (state.selectedPreset) {
            "Minggu ini", "7 hari" -> {
                val nextStart = startDate.plusWeeks(1).with(DayOfWeek.MONDAY)
                if (nextStart > today) return
                setDateRange(nextStart, minOf(nextStart.plusDays(6), today), "Minggu ini")
            }
            "Bulan ini", "30 hari" -> {
                val nextStart = startDate.plusMonths(1).withDayOfMonth(1)
                if (nextStart > today) return
                setDateRange(nextStart, minOf(nextStart.endOfMonth(), today), "Bulan ini")
            }
            else -> shiftRange(daysDirection = 1)
        }
    }

    fun previousDate() {
        val state = _uiState.value
        val (startDate, endDate) = currentDateRange()

        when (state.selectedPreset) {
            "Minggu ini", "7 hari" -> {
                val previousStart = startDate.minusWeeks(1).with(DayOfWeek.MONDAY)
                setDateRange(previousStart, previousStart.plusDays(6), "Minggu ini")
            }
            "Bulan ini", "30 hari" -> {
                val previousStart = startDate.minusMonths(1).withDayOfMonth(1)
                setDateRange(previousStart, previousStart.endOfMonth(), "Bulan ini")
            }
            else -> shiftRange(daysDirection = -1)
        }
    }

    fun nextPage() {
        if (_uiState.value.hasMorePages) {
            loadTransactions(_uiState.value.page + 1)
        }
    }

    fun previousPage() {
        if (_uiState.value.page > 1) {
            loadTransactions(_uiState.value.page - 1)
        }
    }

    private fun setDateRange(startDate: LocalDate, endDate: LocalDate, preset: String?) {
        _uiState.update {
            it.copy(
                selectedDate = endDate.toString(),
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                selectedPreset = preset,
                page = 1
            )
        }
        loadTransactions(page = 1)
    }

    private fun currentDateRange(): Pair<LocalDate, LocalDate> {
        val state = _uiState.value
        val endDate = (state.endDate ?: state.selectedDate)?.let(LocalDate::parse) ?: LocalDate.now()
        val startDate = (state.startDate ?: state.selectedDate)?.let(LocalDate::parse) ?: endDate
        return startDate to endDate
    }

    private fun shiftRange(daysDirection: Int) {
        val (startDate, endDate) = currentDateRange()
        val today = LocalDate.now()
        val rangeLength = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0) + 1
        val shiftedStart = startDate.plusDays(rangeLength * daysDirection)
        val shiftedEnd = endDate.plusDays(rangeLength * daysDirection)
        if (daysDirection > 0 && shiftedEnd > today) {
            setDateRange(today.minusDays(rangeLength - 1), today, null)
        } else {
            setDateRange(shiftedStart, shiftedEnd, null)
        }
    }

    private fun LocalDate.endOfMonth(): LocalDate = withDayOfMonth(lengthOfMonth())

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminTransactionHistoryViewModel(repository) as T
                }
            }
        }
    }
}
