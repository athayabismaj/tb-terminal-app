package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.DiscountType
import java.math.BigDecimal
import java.math.RoundingMode

internal data class CheckoutEstimatedTotals(
    val grossSubtotal: BigDecimal,
    val itemDiscountTotal: BigDecimal,
    val transactionDiscountAmount: BigDecimal,
    val totalDiscountAmount: BigDecimal,
    val netTotal: BigDecimal,
)

internal fun estimateCheckoutTotals(
    items: List<CartItem>,
    transactionDiscount: CheckoutDiscount?,
): CheckoutEstimatedTotals {
    val gross = items.fold(BigDecimal.ZERO) { total, item ->
        total.add(item.unitPrice.multiply(item.quantity.toBigDecimal()).money())
    }.money()
    val itemDiscount = items.fold(BigDecimal.ZERO) { total, item ->
        val lineGross = item.unitPrice.multiply(item.quantity.toBigDecimal()).money()
        total.add(estimateDiscountAmount(lineGross, item.discountRequest))
    }.money()
    val afterItem = gross.subtract(itemDiscount).coerceAtLeast(BigDecimal.ZERO).money()
    val transactionAmount = estimateDiscountAmount(afterItem, transactionDiscount)
    val totalDiscount = itemDiscount.add(transactionAmount).money()
    return CheckoutEstimatedTotals(
        grossSubtotal = gross,
        itemDiscountTotal = itemDiscount,
        transactionDiscountAmount = transactionAmount,
        totalDiscountAmount = totalDiscount,
        netTotal = gross.subtract(totalDiscount).coerceAtLeast(BigDecimal.ZERO).money(),
    )
}

internal fun validateCheckoutDiscount(
    baseAmount: BigDecimal,
    discount: CheckoutDiscount?,
): String? {
    discount ?: return null
    if (discount.value < BigDecimal.ZERO) return "Diskon tidak boleh negatif."
    if (discount.value.scale().coerceAtLeast(0) > 2) return "Diskon maksimal 2 angka desimal."
    if (discount.type == DiscountType.PERCENTAGE && discount.value > HUNDRED) {
        return "Persentase diskon harus antara 0 dan 100."
    }
    if (estimateDiscountAmount(baseAmount, discount) > baseAmount.money()) {
        return "Diskon tidak boleh melebihi nilai yang didiskon."
    }
    return null
}

private fun estimateDiscountAmount(base: BigDecimal, discount: CheckoutDiscount?): BigDecimal {
    discount ?: return BigDecimal.ZERO.setScale(MONEY_SCALE)
    return when (discount.type) {
        DiscountType.PERCENTAGE -> base.multiply(discount.value)
            .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP)
        DiscountType.FIXED_AMOUNT -> discount.value.money()
    }
}

private fun BigDecimal.money(): BigDecimal = setScale(MONEY_SCALE, RoundingMode.HALF_UP)

private const val MONEY_SCALE = 2
private val HUNDRED = BigDecimal("100")
