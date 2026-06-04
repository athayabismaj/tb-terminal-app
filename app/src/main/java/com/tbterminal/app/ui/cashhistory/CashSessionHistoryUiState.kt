package com.tbterminal.app.ui.cashhistory

import com.tbterminal.app.data.model.CashSession
import java.time.LocalDate

data class CashSessionHistoryUiState(
    val sessions: List<CashSession> = emptyList(),
    val statusFilter: String = "Semua",
    val searchQuery: String = "",
    val selectedDate: String? = LocalDate.now().toString(),
    val startDate: String? = LocalDate.now().toString(),
    val endDate: String? = LocalDate.now().toString(),
    val selectedPreset: String? = "Hari ini",
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 1,
    val totalSessions: Long = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
