package com.tbterminal.app.ui.cashhistory

import com.tbterminal.app.data.model.CashSession

data class CashSessionHistoryUiState(
    val sessions: List<CashSession> = emptyList(),
    val statusFilter: String = "Semua",
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 1,
    val totalSessions: Long = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
