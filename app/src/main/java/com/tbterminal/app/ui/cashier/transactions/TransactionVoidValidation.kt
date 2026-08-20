package com.tbterminal.app.ui.cashier.transactions

import java.math.BigDecimal

internal fun validateTransactionVoidReason(value: String): String? {
    val length = value.trim().length
    return when {
        length < 5 -> "Alasan void minimal 5 karakter."
        length > 1000 -> "Alasan void maksimal 1000 karakter."
        else -> null
    }
}

internal fun stockCardBalancesReconciled(currentStock: BigDecimal?, ledgerBalance: BigDecimal?): Boolean {
    return currentStock != null && ledgerBalance != null && currentStock.compareTo(ledgerBalance) == 0
}
