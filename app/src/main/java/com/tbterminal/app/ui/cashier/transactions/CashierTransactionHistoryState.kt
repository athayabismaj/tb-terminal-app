package com.tbterminal.app.ui.cashier.transactions

import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.ui.offline.LocalPendingTransactionUi
import java.time.LocalDate
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.TransactionRefundResult

data class CashierTransactionHistoryUiState(
    val activeSession: CashSession? = null,
    val transactions: List<CashTransaction> = emptyList(),
    val localPendingTransactions: List<LocalPendingTransactionUi> = emptyList(),
    val selectedTransaction: CashTransactionDetail? = null,
    val page: Int = 1,
    val limit: Int = CASHIER_TRANSACTION_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isReceiptLoading: Boolean = false,
    val errorMessage: String? = null,
    val receiptMessage: String? = null,
    val localSyncMessage: String? = null,
    val isBulkSyncing: Boolean = false,
    val bulkSyncProgressMessage: String? = null,
    val query: String = "",
    val statusFilter: String = "Semua",
    val selectedDate: String? = LocalDate.now().toString(),
    val startDate: String? = LocalDate.now().toString(),
    val endDate: String? = LocalDate.now().toString(),
    val selectedPreset: String? = "Hari ini",
    val isPayDebtDialogOpen: Boolean = false,
    val payDebtAmountInput: String = "",
    val payDebtMethodInput: String = "TUNAI",
    val isSubmittingDebt: Boolean = false,
    val isVoidDialogOpen: Boolean = false,
    val voidReasonInput: String = "",
    val voidIdempotencyKey: String? = null,
    val isSubmittingVoid: Boolean = false,
    val voidErrorMessage: String? = null,
    val voidManagerApprovalId: String? = null,
    val isVoidOutcomeAmbiguous: Boolean = false,
    val isRefundDialogOpen: Boolean = false,
    val refundReasonInput: String = "",
    val refundDisposition: RefundDisposition = RefundDisposition.RETURN_TO_STOCK,
    val refundIdempotencyKey: String? = null,
    val isSubmittingRefund: Boolean = false,
    val refundErrorMessage: String? = null,
    val refundManagerApprovalId: String? = null,
    val isRefundOutcomeAmbiguous: Boolean = false,
    val refundResult: TransactionRefundResult? = null,
    val pendingManagerApprovalAction: ManagerApprovalAction? = null,
) {
    val hasActiveSession: Boolean
        get() = activeSession?.status.equals("OPEN", ignoreCase = true)

    val currentStart: Long
        get() = if (total == 0L) 0 else ((page - 1L) * limit) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * limit, total)
}

const val CASHIER_TRANSACTION_PAGE_SIZE = 10
