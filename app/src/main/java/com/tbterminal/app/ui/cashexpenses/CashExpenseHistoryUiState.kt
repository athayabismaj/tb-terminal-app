package com.tbterminal.app.ui.cashexpenses

import com.tbterminal.app.data.model.CashExpense

data class CashExpenseHistoryUiState(
    val expenses: List<CashExpense> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 1,
    val totalExpenses: Long = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
