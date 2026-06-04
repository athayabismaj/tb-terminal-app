package com.tbterminal.app.ui.cashexpenses

import com.tbterminal.app.data.model.CashExpense
import java.time.LocalDate

data class CashExpenseHistoryUiState(
    val expenses: List<CashExpense> = emptyList(),
    val searchQuery: String = "",
    val selectedDate: String? = LocalDate.now().toString(),
    val startDate: String? = LocalDate.now().toString(),
    val endDate: String? = LocalDate.now().toString(),
    val selectedPreset: String? = "Hari ini",
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 1,
    val totalExpenses: Long = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
