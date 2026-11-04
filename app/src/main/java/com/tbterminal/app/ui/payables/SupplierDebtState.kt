package com.tbterminal.app.ui.payables

import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPayable
import java.math.BigDecimal

data class SupplierDebtUiState(
    val payables: List<SupplierPayable> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: SupplierDebtStatusFilter = SupplierDebtStatusFilter.All,
    val dueFilter: SupplierDebtDueFilter = SupplierDebtDueFilter.All,
    val sortOrder: SupplierDebtSortOrder = SupplierDebtSortOrder.NearestDueDate,
    val page: Int = 1,
    val limit: Int = SUPPLIER_DEBT_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val selectedPayable: SupplierPayable? = null,
    val paymentAmountInput: String = "",
    val paymentMethod: SupplierPaymentMethod = SupplierPaymentMethod.Cash,
    val referenceInput: String = "",
    val notesInput: String = "",
    val isLoading: Boolean = false,
    val isSubmittingPayment: Boolean = false,
    val suppliers: List<Supplier> = emptyList(),
    val isOpeningBalanceOpen: Boolean = false,
    val openingSupplierId: String = "",
    val openingAmountInput: String = "",
    val openingDebtDateInput: String = java.time.LocalDate.now().toString(),
    val openingDueDateInput: String = java.time.LocalDate.now().toString(),
    val openingLegacyInvoiceInput: String = "",
    val openingNotesInput: String = "",
    val isSubmittingOpeningBalance: Boolean = false,
    val isOpeningBalanceSaved: Boolean = false,
    val isAdjustmentOpen: Boolean = false,
    val adjustmentSupplierId: String = "",
    val adjustmentAmountInput: String = "",
    val adjustmentDebtDateInput: String = java.time.LocalDate.now().toString(),
    val adjustmentDueDateInput: String = java.time.LocalDate.now().toString(),
    val adjustmentReferenceInput: String = "",
    val adjustmentReasonInput: String = "",
    val isSubmittingAdjustment: Boolean = false,
    val isAdjustmentSaved: Boolean = false,
    val supplierActiveDebts: List<SupplierPayable> = emptyList(),
    val isLoadingSupplierDebts: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
) {
    val filteredPayables: List<SupplierPayable>
        get() {
            val query = searchQuery.trim()
            val list = if (query.isBlank()) payables else payables.filter { payable ->
                payable.supplierName.contains(query, ignoreCase = true) ||
                    payable.purchaseId.contains(query, ignoreCase = true) ||
                    payable.status.contains(query, ignoreCase = true)
            }
            val today = java.time.LocalDate.now()
            val dueFiltered = when (dueFilter) {
                SupplierDebtDueFilter.All -> list
                SupplierDebtDueFilter.Overdue -> list.filter { p ->
                    val d = runCatching { java.time.LocalDate.parse(p.dueDate.take(10)) }.getOrNull()
                    d != null && d.isBefore(today) && p.remainingAmount > BigDecimal.ZERO
                }
                SupplierDebtDueFilter.DueToday -> list.filter { p ->
                    val d = runCatching { java.time.LocalDate.parse(p.dueDate.take(10)) }.getOrNull()
                    d != null && d.isEqual(today) && p.remainingAmount > BigDecimal.ZERO
                }
                SupplierDebtDueFilter.Upcoming -> list.filter { p ->
                    val d = runCatching { java.time.LocalDate.parse(p.dueDate.take(10)) }.getOrNull()
                    d != null && d.isAfter(today) && p.remainingAmount > BigDecimal.ZERO
                }
            }
            return when (sortOrder) {
                SupplierDebtSortOrder.NearestDueDate -> dueFiltered.sortedBy { it.dueDate }
                SupplierDebtSortOrder.HighestAmount -> dueFiltered.sortedByDescending { it.remainingAmount }
                SupplierDebtSortOrder.SupplierNameAsc -> dueFiltered.sortedBy { it.supplierName.lowercase() }
            }
        }

    val currentStart: Long
        get() = if (total == 0L) 0 else ((page - 1L) * limit) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * limit, total)

    val pageRemainingTotal: BigDecimal
        get() = payables.fold(BigDecimal.ZERO) { totalAmount, payable ->
            totalAmount.add(payable.remainingAmount)
        }

    val unpaidCount: Int
        get() = payables.count { !it.status.equals(SupplierDebtStatusFilter.Paid.apiValue, ignoreCase = true) }
}

enum class SupplierDebtStatusFilter(
    val apiValue: String?,
    val label: String
) {
    All(null, "Semua"),
    Unpaid("belum_lunas", "Belum lunas"),
    Partial("sebagian", "Sebagian"),
    Paid("lunas", "Lunas")
}

enum class SupplierPaymentMethod(
    val apiValue: String,
    val label: String,
    val description: String
) {
    Cash("tunai", "Tunai", "Bayar kasir"),
    Transfer("transfer", "Transfer", "Rekening bank"),
    Qris("qris", "QRIS", "Scan barcode")
}

internal const val SUPPLIER_DEBT_PAGE_SIZE = 100
internal const val SUPPLIER_DEBT_USERS_PER_PAGE = 5

enum class SupplierDebtDueFilter(val label: String) {
    All("Semua"),
    Overdue("Lewat Jatuh Tempo"),
    DueToday("Hari Ini"),
    Upcoming("Akan Datang")
}

enum class SupplierDebtSortOrder(val label: String) {
    NearestDueDate("Tempo Terdekat"),
    HighestAmount("Nominal Terbesar"),
    SupplierNameAsc("Nama Supplier (A–Z)")
}
