package com.tbterminal.app.ui.dashboard.owner

import com.tbterminal.app.data.costing.MovingAverageCalculator
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.incominggoods.IncomingPaymentMethod
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class OwnerBarangMasukCalculation(
    val quantity: BigDecimal = BigDecimal.ZERO,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    val remainingDebt: BigDecimal = BigDecimal.ZERO,
    val amountPaidExceedsTotal: Boolean = false,
    val hasImpactPreview: Boolean = false,
    val oldStock: BigDecimal = BigDecimal.ZERO,
    val newStock: BigDecimal = BigDecimal.ZERO,
    val oldCogs: BigDecimal = BigDecimal.ZERO,
    val newCogs: BigDecimal = BigDecimal.ZERO
)

internal fun parseWholeNumberDigits(raw: String): BigDecimal {
    val digits = raw.filter(Char::isDigit)
    if (digits.isEmpty()) return BigDecimal.ZERO
    return digits.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

internal fun calculateOwnerBarangMasuk(
    selectedProduct: ProductStock?,
    quantityDigits: String,
    unitPriceDigits: String,
    amountPaidDigits: String
): OwnerBarangMasukCalculation {
    val qty = parseWholeNumberDigits(quantityDigits)
    val price = parseWholeNumberDigits(unitPriceDigits)
    val total = if (qty > BigDecimal.ZERO && price > BigDecimal.ZERO) {
        qty.multiply(price).setScale(0, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }
    val paid = parseWholeNumberDigits(amountPaidDigits)
    val exceeds = total > BigDecimal.ZERO && paid > total
    val remaining = if (total > BigDecimal.ZERO && paid < total) {
        total.subtract(paid).coerceAtLeast(BigDecimal.ZERO)
    } else {
        BigDecimal.ZERO
    }

    val oldStock = selectedProduct?.quantity?.coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO
    val oldCogs = selectedProduct?.priceBuy?.coerceAtLeast(BigDecimal.ZERO)
        ?.setScale(0, RoundingMode.HALF_UP)
        ?: BigDecimal.ZERO
    val hasImpactPreview = selectedProduct != null && qty > BigDecimal.ZERO && price > BigDecimal.ZERO

    val impact = if (hasImpactPreview) {
        MovingAverageCalculator.calculateDetails(
            existingStock = selectedProduct!!.quantity,
            existingCogs = selectedProduct.priceBuy,
            incomingQuantity = qty,
            incomingPrice = price,
            scale = 0
        )
    } else {
        null
    }

    return OwnerBarangMasukCalculation(
        quantity = qty,
        unitPrice = price,
        total = total,
        amountPaid = paid,
        remainingDebt = remaining,
        amountPaidExceedsTotal = exceeds,
        hasImpactPreview = hasImpactPreview,
        oldStock = oldStock.setScale(0, RoundingMode.HALF_UP),
        newStock = impact?.newStock?.setScale(0, RoundingMode.HALF_UP) ?: oldStock.setScale(0, RoundingMode.HALF_UP),
        oldCogs = oldCogs,
        newCogs = impact?.newCogs ?: oldCogs
    )
}

internal fun initialAmountPaidDigitsForMethod(
    method: IncomingPaymentMethod,
    total: BigDecimal
): String = when (method) {
    IncomingPaymentMethod.CASH,
    IncomingPaymentMethod.TRANSFER,
    IncomingPaymentMethod.QRIS -> {
        val rounded = total.setScale(0, RoundingMode.HALF_UP)
        if (rounded > BigDecimal.ZERO) rounded.toPlainString() else ""
    }
    IncomingPaymentMethod.DEBT,
    IncomingPaymentMethod.DOWN_PAYMENT -> "0"
}

internal fun initialDueDateForSupplier(
    paymentTermDays: Int?,
    today: LocalDate
): LocalDate? = if (paymentTermDays != null && paymentTermDays > 0) {
    today.plusDays(paymentTermDays.toLong())
} else {
    null
}

internal fun resolveDueDaysForSubmit(
    remainingDebt: BigDecimal,
    dueDate: LocalDate?,
    today: LocalDate
): Int {
    if (remainingDebt <= BigDecimal.ZERO || dueDate == null) return 0
    return ChronoUnit.DAYS.between(today, dueDate).toInt().coerceAtLeast(0)
}

internal fun isOwnerBarangMasukFormValid(
    selectedProduct: ProductStock?,
    selectedSupplier: Supplier?,
    calculation: OwnerBarangMasukCalculation,
    dueDate: LocalDate?
): Boolean {
    if (selectedProduct == null || selectedSupplier == null) return false
    if (calculation.quantity <= BigDecimal.ZERO) return false
    if (calculation.unitPrice <= BigDecimal.ZERO) return false
    if (calculation.total <= BigDecimal.ZERO) return false
    if (calculation.amountPaid < BigDecimal.ZERO || calculation.amountPaidExceedsTotal) return false
    if (calculation.remainingDebt > BigDecimal.ZERO && dueDate == null) return false
    return true
}

internal fun formatRupiahWhole(amount: BigDecimal): String {
    val safe = amount.coerceAtLeast(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP)
    return "Rp ${formatThousandsDots(safe.toPlainString())}"
}

internal fun formatBareWholeNumber(amount: BigDecimal): String {
    val safe = amount.coerceAtLeast(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP)
    return formatThousandsDots(safe.toPlainString())
}

internal fun formatStockWhole(qty: BigDecimal): String =
    qty.coerceAtLeast(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP).toPlainString()

internal fun formatIndonesianDateShort(date: LocalDate): String =
    "${date.dayOfMonth} ${indonesianShortMonth(date.monthValue)} ${date.year}"
