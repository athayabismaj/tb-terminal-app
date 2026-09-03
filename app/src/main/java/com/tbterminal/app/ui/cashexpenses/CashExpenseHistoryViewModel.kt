package com.tbterminal.app.ui.cashexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CashExpenseHistoryViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashExpenseHistoryUiState())
    val uiState: StateFlow<CashExpenseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    fun loadExpenses(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value
            val startDate = state.startDate ?: state.selectedDate
            val endDate = state.endDate ?: state.selectedDate
            when (val result = repository.getExpenseHistory(page, state.pageSize, startDate = startDate, endDate = endDate)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        expenses = result.data.data,
                        page = result.data.page,
                        totalPages = result.data.totalPages.coerceAtLeast(1),
                        totalExpenses = result.data.total,
                        isLoading = false
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Koneksi ke server gagal.")
                }
            }
        }
    }

    fun refresh() = loadExpenses(_uiState.value.page)

    fun previousPage() {
        if (_uiState.value.page > 1) loadExpenses(_uiState.value.page - 1)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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
        loadExpenses()
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

    fun nextPage() {
        if (_uiState.value.page < _uiState.value.totalPages) loadExpenses(_uiState.value.page + 1)
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
        loadExpenses()
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
                    return CashExpenseHistoryViewModel(repository) as T
                }
            }
        }
    }
}
