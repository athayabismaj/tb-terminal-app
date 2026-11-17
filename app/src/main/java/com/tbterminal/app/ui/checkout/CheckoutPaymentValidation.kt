package com.tbterminal.app.ui.checkout

import java.math.BigDecimal

data class CheckoutPaymentInputResult(
    val amountPaid: BigDecimal? = null,
    val error: String? = null
)

fun validateCheckoutPaymentInput(
    paymentMethod: PaymentMethod,
    amountPaidInput: String,
    total: BigDecimal
): CheckoutPaymentInputResult {
    if (total < BigDecimal.ZERO) return CheckoutPaymentInputResult(error = "Total transaksi tidak boleh negatif.")
    val parsed = amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
    val amount = when (paymentMethod) {
        PaymentMethod.HUTANG -> BigDecimal.ZERO
        PaymentMethod.TRANSFER, PaymentMethod.QRIS -> total
        PaymentMethod.TUNAI, PaymentMethod.DP -> parsed
            ?: return CheckoutPaymentInputResult(error = "Nominal bayar belum valid.")
    }
    if (amount < BigDecimal.ZERO) return CheckoutPaymentInputResult(error = "Jumlah bayar tidak boleh negatif.")
    if (amount.scale() > 2) return CheckoutPaymentInputResult(error = "Jumlah bayar maksimal 2 angka desimal.")
    if (total.compareTo(BigDecimal.ZERO) == 0) {
        return when (paymentMethod) {
            PaymentMethod.HUTANG, PaymentMethod.DP -> CheckoutPaymentInputResult(
                error = "Transaksi bernilai nol tidak dapat dibuat sebagai hutang atau DP.",
            )
            PaymentMethod.TUNAI -> if (amount.compareTo(BigDecimal.ZERO) != 0) {
                CheckoutPaymentInputResult(error = "Nominal bayar harus nol untuk transaksi gratis.")
            } else CheckoutPaymentInputResult(BigDecimal.ZERO)
            PaymentMethod.TRANSFER, PaymentMethod.QRIS -> CheckoutPaymentInputResult(BigDecimal.ZERO)
        }
    }
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

fun checkoutChange(amountPaidInput: String, total: BigDecimal): BigDecimal {
    val amount = amountPaidInput.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    return amount.subtract(total).coerceAtLeast(BigDecimal.ZERO)
}

data class SplitPaymentValidationResult(
    val totalTendered: BigDecimal = BigDecimal.ZERO,
    val cashTendered: BigDecimal = BigDecimal.ZERO,
    val nonCashPaid: BigDecimal = BigDecimal.ZERO,
    val changeAmount: BigDecimal = BigDecimal.ZERO,
    val remainingReceivable: BigDecimal = BigDecimal.ZERO,
    val error: String? = null
)

fun validateSplitCheckoutPayment(
    lines: List<SplitPaymentLineUi>,
    total: BigDecimal,
    isDpMode: Boolean,
    customerSelected: Boolean,
): SplitPaymentValidationResult {
    if (total < BigDecimal.ZERO) return SplitPaymentValidationResult(error = "Total transaksi tidak boleh negatif.")
    if (lines.isEmpty()) return SplitPaymentValidationResult(error = "Tambahkan minimal 1 metode pembayaran.")
    if (lines.size > 5) return SplitPaymentValidationResult(error = "Maksimal 5 metode pembayaran.")
    if (lines.count { it.method == PaymentMethod.TUNAI } > 1) {
        return SplitPaymentValidationResult(error = "Maksimal 1 baris pembayaran Tunai.")
    }
    if (lines.any { it.method == PaymentMethod.HUTANG || it.method == PaymentMethod.DP }) {
        return SplitPaymentValidationResult(error = "Split payment hanya mendukung Tunai, Transfer, atau QRIS.")
    }

    var nonCash = BigDecimal.ZERO
    var cash = BigDecimal.ZERO
    for (line in lines) {
        val amt = line.amountInput.trim().replace(',', '.').toBigDecimalOrNull()
            ?: return SplitPaymentValidationResult(error = "Nominal split payment belum valid.")
        if (amt <= BigDecimal.ZERO && total > BigDecimal.ZERO) {
            return SplitPaymentValidationResult(error = "Setiap nominal pembayaran harus lebih dari nol.")
        }
        if (amt < BigDecimal.ZERO || amt.scale() > 2) {
            return SplitPaymentValidationResult(error = "Nominal maksimal 2 desimal dan tidak boleh negatif.")
        }
        if (line.method == PaymentMethod.TUNAI) {
            cash = cash.add(amt)
        } else {
            nonCash = nonCash.add(amt)
        }
    }

    val tendered = nonCash.add(cash)
    if (nonCash > total) {
        return SplitPaymentValidationResult(
            totalTendered = tendered,
            cashTendered = cash,
            nonCashPaid = nonCash,
            error = "Total pembayaran non-tunai melebihi total tagihan."
        )
    }

    return if (isDpMode) {
        when {
            !customerSelected -> SplitPaymentValidationResult(
                totalTendered = tendered,
                cashTendered = cash,
                nonCashPaid = nonCash,
                error = "DP split wajib memilih pelanggan terdaftar."
            )
            tendered <= BigDecimal.ZERO || tendered >= total -> SplitPaymentValidationResult(
                totalTendered = tendered,
                cashTendered = cash,
                nonCashPaid = nonCash,
                error = "Total DP harus lebih dari nol dan kurang dari total tagihan."
            )
            else -> SplitPaymentValidationResult(
                totalTendered = tendered,
                cashTendered = cash,
                nonCashPaid = nonCash,
                remainingReceivable = total.subtract(tendered).coerceAtLeast(BigDecimal.ZERO)
            )
        }
    } else {
        if (tendered < total) {
            SplitPaymentValidationResult(
                totalTendered = tendered,
                cashTendered = cash,
                nonCashPaid = nonCash,
                error = "Total pembayaran split kurang dari tagihan."
            )
        } else if (cash > BigDecimal.ZERO && nonCash.compareTo(total) == 0) {
            SplitPaymentValidationResult(
                totalTendered = tendered,
                cashTendered = cash,
                nonCashPaid = nonCash,
                error = "Non-tunai sudah lunas; hapus baris tunai."
            )
        } else {
            SplitPaymentValidationResult(
                totalTendered = tendered,
                cashTendered = cash,
                nonCashPaid = nonCash,
                changeAmount = tendered.subtract(total).coerceAtLeast(BigDecimal.ZERO)
            )
        }
    }
}
