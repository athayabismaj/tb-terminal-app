package com.tbterminal.app.ui.receivablepayments

import com.tbterminal.app.data.model.ReceivablePaymentHistory
import java.math.BigDecimal

data class ReceivablePaymentHistoryUiState(
    val payments: List<ReceivablePaymentHistory> = emptyList(),
    val selectedPayment: ReceivablePaymentHistory? = null,
    val searchQuery: String = "",
    val methodFilter: ReceivablePaymentMethodFilter = ReceivablePaymentMethodFilter.All,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalPayments: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredPayments: List<ReceivablePaymentHistory>
        get() {
            val query = searchQuery.trim()
            return payments.filter { payment ->
                methodFilter.matches(payment.method) &&
                    (
                        query.isBlank() ||
                            payment.customerName.contains(query, ignoreCase = true) ||
                            payment.transactionId.contains(query, ignoreCase = true)
                    )
            }
        }

    val pageTotal: BigDecimal
        get() = payments.fold(BigDecimal.ZERO) { total, payment -> total.add(payment.amount) }

    val paidReceivablesOnPage: Int
        get() = payments.count { it.receivableStatus.equals("lunas", ignoreCase = true) }

    val hasLocalFilter: Boolean
        get() = searchQuery.isNotBlank() || methodFilter != ReceivablePaymentMethodFilter.All

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
}
