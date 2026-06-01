package com.tbterminal.app.ui.cashdetail

import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction

data class CashReconciliationDetailUiState(
    val sessionId: String = "",
    val session: CashSession? = null,
    val expenses: List<CashExpense> = emptyList(),
    val transactions: List<CashTransaction> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalTransactions: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
