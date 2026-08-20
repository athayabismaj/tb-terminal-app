package com.tbterminal.app.ui.checkout

import java.math.BigDecimal

internal data class CheckoutPaymentInputResult(
    val amountPaid: BigDecimal? = null,
    val error: String? = null
)

internal fun validateCheckoutPaymentInput(
    paymentMethod: PaymentMethod,
    amountPaidInput: String,
    total: BigDecimal
): CheckoutPaymentInputResult {
    if (total <= BigDecimal.ZERO) return CheckoutPaymentInputResult(error = "Total transaksi harus lebih dari nol.")
    val parsed = amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
    val amount = when (paymentMethod) {
        PaymentMethod.HUTANG -> BigDecimal.ZERO
        PaymentMethod.TRANSFER, PaymentMethod.QRIS -> total
        PaymentMethod.TUNAI, PaymentMethod.DP -> parsed
            ?: return CheckoutPaymentInputResult(error = "Nominal bayar belum valid.")
    }
    if (amount < BigDecimal.ZERO) return CheckoutPaymentInputResult(error = "Jumlah bayar tidak boleh negatif.")
    if (amount.scale() > 2) return CheckoutPaymentInputResult(error = "Jumlah bayar maksimal 2 angka desimal.")
    return when (paymentMethod) {
        PaymentMethod.TUNAI -> if (amount < total) {
            CheckoutPaymentInputResult(error = "Pembayaran tunai kurang dari total transaksi.")
        } else CheckoutPaymentInputResult(amount)
        PaymentMethod.TRANSFER, PaymentMethod.QRIS -> CheckoutPaymentInputResult(total)
        PaymentMethod.HUTANG -> CheckoutPaymentInputResult(BigDecimal.ZERO)
        PaymentMethod.DP -> if (amount <= BigDecimal.ZERO || amount >= total) {
            CheckoutPaymentInputResult(error = "DP harus lebih dari nol dan kurang dari total transaksi.")
        } else CheckoutPaymentInputResult(amount)
    }
}

internal fun checkoutChange(amountPaidInput: String, total: BigDecimal): BigDecimal {
    val amount = amountPaidInput.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    return amount.subtract(total).coerceAtLeast(BigDecimal.ZERO)
}
