package com.tbterminal.app.ui.cashier.transactions

internal fun validateTransactionRefundReason(value: String): String? {
    val length = value.trim().length
    return when {
        length < 5 -> "Alasan refund minimal 5 karakter."
        length > 1000 -> "Alasan refund maksimal 1000 karakter."
        else -> null
    }
}
