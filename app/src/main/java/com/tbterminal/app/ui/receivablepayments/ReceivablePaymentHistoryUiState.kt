package com.tbterminal.app.ui.receivablepayments

import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import java.math.BigDecimal

data class ReceivablePaymentHistoryUiState(
    val payments: List<ReceivablePaymentHistory> = emptyList(),
    val selectedPayment: ReceivablePaymentHistory? = null,
    val searchQuery: String = "",
    val receiverSearch: String = "",
    val receivableIdFilter: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val methodFilter: ReceivablePaymentMethodFilter = ReceivablePaymentMethodFilter.All,
    val statusFilter: ReceivablePaymentStatusFilter = ReceivablePaymentStatusFilter.All,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalPayments: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val message: String? = null,
    val paymentToReverse: ReceivablePaymentHistory? = null,
    val reversalReason: String = "",
    val reversalIdempotencyKey: String = "",
    val isReversing: Boolean = false,
    val lastReversalReceipt: ReceivablePaymentReceipt? = null
) {
    val filteredPayments: List<ReceivablePaymentHistory>
        get() = payments

    val pageTotal: BigDecimal
        get() = payments.fold(BigDecimal.ZERO) { total, payment ->
            if (payment.entryType == "REVERSAL") total.subtract(payment.amount) else total.add(payment.amount)
        }

    val paidReceivablesOnPage: Int
        get() = payments.count {
            it.receivableStatus.equals("PAID", ignoreCase = true) ||
                it.receivableStatus.equals("lunas", ignoreCase = true)
        }

    val hasLocalFilter: Boolean
        get() = searchQuery.isNotBlank() || receiverSearch.isNotBlank() || receivableIdFilter.isNotBlank() || dateFrom.isNotBlank() ||
            dateTo.isNotBlank() || methodFilter != ReceivablePaymentMethodFilter.All ||
            statusFilter != ReceivablePaymentStatusFilter.All

    val currentStart: Long
        get() = if (totalPayments == 0L) 0 else ((page - 1L) * pageSize) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * pageSize, totalPayments)
}

enum class ReceivablePaymentMethodFilter(val label: String, private val apiValue: String?) {
    All("Semua metode", null),
    Cash("Tunai", "tunai"),
    Transfer("Transfer", "transfer"),
    Qris("QRIS", "qris");

    fun matches(method: String): Boolean {
        return apiValue == null || method.equals(apiValue, ignoreCase = true)
    }

    fun apiValue(): String? = apiValue
}

enum class ReceivablePaymentStatusFilter(val label: String, val apiValue: String?) {
    All("Semua status", null),
    Unpaid("Belum lunas", "UNPAID"),
    Partial("Sebagian", "PARTIAL"),
    Paid("Lunas", "PAID")
}
