package com.tbterminal.app.ui.receivablepayments

import com.tbterminal.app.data.model.ReceivablePaymentHistory
import java.math.BigDecimal

data class ReceivablePaymentHistoryUiState(
    val payments: List<ReceivablePaymentHistory> = emptyList(),
    val selectedPayment: ReceivablePaymentHistory? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalPayments: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val pageTotal: BigDecimal
        get() = payments.fold(BigDecimal.ZERO) { total, payment -> total.add(payment.amount) }

    val paidReceivablesOnPage: Int
        get() = payments.count { it.receivableStatus.equals("lunas", ignoreCase = true) }
}
