package com.tbterminal.app.ui.payables

import com.tbterminal.app.data.model.SupplierPayable
import java.math.BigDecimal

data class SupplierDebtUiState(
    val payables: List<SupplierPayable> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: SupplierDebtStatusFilter = SupplierDebtStatusFilter.All,
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
    val errorMessage: String? = null,
    val message: String? = null
) {
    val filteredPayables: List<SupplierPayable>
        get() {
            val query = searchQuery.trim()
            if (query.isBlank()) return payables
            return payables.filter { payable ->
                payable.supplierName.contains(query, ignoreCase = true) ||
                    payable.purchaseId.contains(query, ignoreCase = true) ||
                    payable.status.contains(query, ignoreCase = true)
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
    Cash("tunai", "Tunai", "Dibayar langsung di kas"),
    Transfer("transfer", "Transfer", "Dibayar melalui bank"),
    Qris("qris", "QRIS", "Dibayar menggunakan QRIS")
}

internal const val SUPPLIER_DEBT_PAGE_SIZE = 10
