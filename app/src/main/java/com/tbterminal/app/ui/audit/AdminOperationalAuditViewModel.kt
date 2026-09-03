package com.tbterminal.app.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.data.repository.SystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

data class AdminOperationalAuditUiState(
    val isLoading: Boolean = false,
    val logs: List<AuditLogItem> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val limit: Int = 10,
    val selectedAction: String? = null,
    val selectedDate: String = todayIso(),
    val startDate: String = todayIso(),
    val endDate: String = todayIso(),
    val selectedPreset: String? = "Hari ini"
)

class AdminOperationalAuditViewModel(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOperationalAuditUiState())
    val uiState: StateFlow<AdminOperationalAuditUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs(page: Int = _uiState.value.currentPage, action: String? = _uiState.value.selectedAction) {
        _uiState.update { it.copy(isLoading = true, error = null, selectedAction = action) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = systemRepository.getAuditLogs(
                page = page,
                limit = state.limit,
                action = action,
                range = state.selectedPreset.toAuditRangeQuery()
            )
            when (result) {
                is com.tbterminal.app.data.remote.NetworkResult.Success -> {
                    val apiResponse = result.data
                    val pageData = apiResponse.data
                    val operationalLogs = pageData
                        ?.data
                        .orEmpty()
                        .filter(AuditLogItem::isOperationalAudit)
                        .filter { log -> state.selectedPreset != null || log.isInsideDateRange(state.startDate, state.endDate) }

                    _uiState.update {
                        it.copy(
                        isLoading = false,
                        logs = operationalLogs,
                        currentPage = pageData?.page ?: 1,
                        totalPages = pageData?.totalPages ?: 1,
                        selectedAction = action
                        )
                    }
                }
                is com.tbterminal.app.data.remote.NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                        isLoading = false,
                        error = result.message ?: "Terjadi kesalahan saat memuat log audit"
                        )
                    }
                }
                is com.tbterminal.app.data.remote.NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                        isLoading = false,
                        error = result.e.message ?: "Terjadi kesalahan saat memuat log audit"
                        )
                    }
                }
            }
        }
    }

    fun refresh() = loadLogs(_uiState.value.currentPage, _uiState.value.selectedAction)

    fun setActionFilter(action: String?) {
        loadLogs(page = 1, action = action)
    }

    fun setDate(date: String?) {
        val resolvedDate = date ?: todayIso()
        _uiState.update {
            it.copy(
                selectedDate = resolvedDate,
                startDate = resolvedDate,
                endDate = resolvedDate,
                selectedPreset = if (date == null) "Hari ini" else null,
                currentPage = 1
            )
        }
        loadLogs(page = 1)
    }

    fun setDatePreset(preset: String) {
        val today = LocalDate.now()
        val start = when (preset) {
            "7 hari" -> today.minusDays(6)
            "30 hari" -> today.minusDays(29)
            else -> today
        }
        setDateRange(start, today, preset)
    }

    fun previousDate() {
        shiftRange(-1)
    }

    fun nextDate() {
        val (_, endDate) = currentDateRange()
        if (endDate >= LocalDate.now()) return
        shiftRange(1)
    }

    private fun setDateRange(startDate: LocalDate, endDate: LocalDate, preset: String?) {
        _uiState.update {
            it.copy(
                selectedDate = endDate.toString(),
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                selectedPreset = preset,
                currentPage = 1
            )
        }
        loadLogs(page = 1)
    }

    private fun shiftRange(direction: Int) {
        val (startDate, endDate) = currentDateRange()
        val today = LocalDate.now()
        val rangeLength = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0) + 1
        val shiftedStart = startDate.plusDays(rangeLength * direction)
        val shiftedEnd = endDate.plusDays(rangeLength * direction)

        if (direction > 0 && shiftedEnd > today) {
            setDateRange(today.minusDays(rangeLength - 1), today, null)
        } else {
            setDateRange(shiftedStart, shiftedEnd, null)
        }
    }

    private fun currentDateRange(): Pair<LocalDate, LocalDate> {
        val state = _uiState.value
        return LocalDate.parse(state.startDate) to LocalDate.parse(state.endDate)
    }

    companion object {
        fun factory(repository: SystemRepository): androidx.lifecycle.ViewModelProvider.Factory = 
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AdminOperationalAuditViewModel(repository) as T
                }
            }
    }
}

fun AdminOperationalAuditUiState.dateRangeLabel(): String {
    return if (startDate == endDate) {
        startDate.toDisplayDate()
    } else {
        "${startDate.toDisplayDate()} - ${endDate.toDisplayDate()}"
    }
}

private fun String?.toAuditRangeQuery(): String? {
    return when (this) {
        "Hari ini" -> "today"
        "7 hari" -> "7d"
        "30 hari" -> "30d"
        else -> null
    }
}

private fun AuditLogItem.isInsideDateRange(startDate: String, endDate: String): Boolean {
    val localDate = runCatching { OffsetDateTime.parse(createdAt).toLocalDate() }.getOrNull() ?: return true
    return !localDate.isBefore(LocalDate.parse(startDate)) && !localDate.isAfter(LocalDate.parse(endDate))
}

private fun String.toDisplayDate(): String {
    return runCatching { LocalDate.parse(this).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.forLanguageTag("id-ID"))) }
        .getOrDefault(this)
}

private fun todayIso(): String = LocalDate.now().toString()
