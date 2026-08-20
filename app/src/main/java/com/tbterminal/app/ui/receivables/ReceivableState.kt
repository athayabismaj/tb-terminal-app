package com.tbterminal.app.ui.receivables

import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerReceivableSummary
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import java.math.BigDecimal
import java.time.LocalDate

data class ReceivableUiState(
    val receivables: List<Receivable> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: ReceivableStatusFilter = ReceivableStatusFilter.All,
    val dueFilter: ReceivableDueFilter = ReceivableDueFilter.All,
    val customerSummaries: List<CustomerReceivableSummary> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val page: Int = 1,
    val limit: Int = RECEIVABLE_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val selectedReceivable: Receivable? = null,
    val paymentAmountInput: String = "",
    val paymentMethod: ReceivablePaymentMethod = ReceivablePaymentMethod.Cash,
    val referenceInput: String = "",
    val notesInput: String = "",
    val paymentIdempotencyKey: String = "",
    val lastPaymentReceipt: ReceivablePaymentReceipt? = null,
    val isLoading: Boolean = false,
    val isSubmittingPayment: Boolean = false,
    val isOpeningBalanceOpen: Boolean = false,
    val openingCustomerId: String = "",
    val openingAmountInput: String = "",
    val openingDebtDateInput: String = LocalDate.now().toString(),
    val openingDueDateInput: String = LocalDate.now().toString(),
    val openingLegacyInvoiceInput: String = "",
    val openingNotesInput: String = "",
    val isSubmittingOpeningBalance: Boolean = false,
    val isAdjustmentOpen: Boolean = false,
    val adjustmentCustomerId: String = "",
    val adjustmentAmountInput: String = "",
    val adjustmentDebtDateInput: String = LocalDate.now().toString(),
    val adjustmentDueDateInput: String = LocalDate.now().toString(),
    val adjustmentReferenceInput: String = "",
    val adjustmentReasonInput: String = "",
    val isSubmittingAdjustment: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
) {
    val filteredReceivables: List<Receivable>
        get() {
            val query = searchQuery.trim()
            if (query.isBlank()) return receivables
            return receivables.filter { receivable ->
                receivable.customerName.contains(query, ignoreCase = true) ||
                    receivable.transactionId?.contains(query, ignoreCase = true) == true ||
                    receivable.legacyInvoiceNumber?.contains(query, ignoreCase = true) == true ||
                    receivable.status.contains(query, ignoreCase = true)
            }
        }

    val currentStart: Long
        get() = if (total == 0L) 0 else ((page - 1L) * limit) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * limit, total)

    val pageRemainingTotal: BigDecimal
        get() = receivables.fold(BigDecimal.ZERO) { totalAmount, receivable ->
            totalAmount.add(receivable.remainingAmount)
        }

    val unpaidCount: Int
        get() = receivables.count { it.status != ReceivableStatusFilter.Paid.apiValue }
}

enum class ReceivableStatusFilter(
    val apiValue: String?,
    val label: String
) {
    All(null, "Semua status"),
    Unpaid("UNPAID", "Belum lunas"),
    Partial("PARTIAL", "Sebagian"),
    Paid("PAID", "Lunas")
}

enum class ReceivableDueFilter(val apiValue: String?, val label: String) {
    All(null, "Semua jatuh tempo"),
    Overdue("OVERDUE", "Lewat jatuh tempo"),
    DueToday("DUE_TODAY", "Jatuh tempo hari ini"),
    Upcoming("UPCOMING", "Akan jatuh tempo")
}

enum class ReceivablePaymentMethod(
    val apiValue: String,
    val label: String,
    val description: String
) {
    Cash("tunai", "Tunai", "Dibayar langsung di kas"),
    Transfer("transfer", "Transfer", "Dibayar melalui bank"),
    Qris("qris", "QRIS", "Dibayar menggunakan QRIS")
}

internal const val RECEIVABLE_PAGE_SIZE = 10
