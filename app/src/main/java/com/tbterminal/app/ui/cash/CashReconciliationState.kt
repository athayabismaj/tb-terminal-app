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
    val isPettyCashDialogOpen: Boolean = false,
    val pettyCashType: com.tbterminal.app.data.local.cashexpense.CashMovementType = com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_OUT,
    val pettyCashAmountInput: String = "",
    val pettyCashCategoryInput: String = "Bensin Armada",
    val pettyCashDescriptionInput: String = "",
    val totalCashIn: BigDecimal = BigDecimal.ZERO,
    val totalCashOut: BigDecimal = BigDecimal.ZERO,
    val page: Int = 1,
    val limit: Int = CASH_TRANSACTION_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isUsingLocalActiveSession: Boolean = false,
    val localCashSessionId: Long? = null,
    val localCashSessionSyncStatus: String? = null,
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
