package com.tbterminal.app.ui.cashier.transactions

import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.model.CashTransactionDetail

data class CashierTransactionHistoryUiState(
    val activeSession: CashSession? = null,
    val transactions: List<CashTransaction> = emptyList(),
    val selectedTransaction: CashTransactionDetail? = null,
    val page: Int = 1,
    val limit: Int = CASHIER_TRANSACTION_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isReceiptLoading: Boolean = false,
    val errorMessage: String? = null,
    val receiptMessage: String? = null,
    val query: String = "",
    val statusFilter: String = "Semua",
    val selectedDate: String? = null, // format: "yyyy-MM-dd", null = semua tanggal
    val isPayDebtDialogOpen: Boolean = false,
    val payDebtAmountInput: String = "",
    val payDebtMethodInput: String = "TUNAI",
    val isSubmittingDebt: Boolean = false
) {
    val hasActiveSession: Boolean
        get() = activeSession?.status.equals("OPEN", ignoreCase = true)

    val currentStart: Long
        get() = if (total == 0L) 0 else ((page - 1L) * limit) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * limit, total)
}

const val CASHIER_TRANSACTION_PAGE_SIZE = 10
