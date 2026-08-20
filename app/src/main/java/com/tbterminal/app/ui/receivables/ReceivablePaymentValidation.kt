package com.tbterminal.app.ui.receivables

import java.math.BigDecimal

internal fun validateReceivablePaymentInput(
    amountInput: String,
    remainingAmount: BigDecimal,
    idempotencyKey: String
): Result<BigDecimal> = runCatching {
    val amount = amountInput.toBigDecimalOrNull()
        ?: error("Nominal pembayaran wajib diisi dengan angka valid.")
    require(amount > BigDecimal.ZERO) { "Nominal pembayaran harus lebih dari nol." }
    require(amount.scale() <= 2) { "Nominal pembayaran maksimal dua angka desimal." }
    require(amount <= remainingAmount) { "Pembayaran tidak boleh melebihi sisa piutang." }
    require(idempotencyKey.length in 8..100) { "Kunci retry pembayaran tidak valid." }
    amount
}
