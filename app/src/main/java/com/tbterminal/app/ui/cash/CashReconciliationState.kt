package com.tbterminal.app.ui.cash

import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import java.math.BigDecimal

data class CashReconciliationUiState(
    val activeSession: CashSession? = null,
    val transactions: List<CashTransaction> = emptyList(),
    val openingCashInput: String = "",
    val closingCashInput: String = "",
    val closingNotesInput: String = "",
    val isExpenseDialogOpen: Boolean = false,
    val expenseAmountInput: String = "",
    val expenseDescriptionInput: String = "",
    val page: Int = 1,
    val limit: Int = CASH_TRANSACTION_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
) {
    val hasActiveSession: Boolean
        get() = activeSession?.status.equals("OPEN", ignoreCase = true)

    val systemCash: BigDecimal
        get() = activeSession?.systemCash ?: activeSession?.openingCash ?: BigDecimal.ZERO

    val cashSales: BigDecimal
        get() = systemCash.subtract(activeSession?.openingCash ?: BigDecimal.ZERO).coerceAtLeast(BigDecimal.ZERO)

    val currentStart: Long
        get() = if (total == 0L) 0 else ((page - 1L) * limit) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * limit, total)
}

internal const val CASH_TRANSACTION_PAGE_SIZE = 10
