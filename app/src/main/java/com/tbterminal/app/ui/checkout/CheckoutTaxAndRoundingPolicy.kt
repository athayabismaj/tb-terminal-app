package com.tbterminal.app.ui.checkout

import java.math.BigDecimal
import java.math.RoundingMode

internal data class CheckoutTaxCalculation(
    val taxEnabled: Boolean,
    val taxMode: String?,
    val taxRate: BigDecimal,
    val taxLabel: String?,
    val taxableBase: BigDecimal,
    val taxAmount: BigDecimal,
    val totalWithTax: BigDecimal,
)

internal data class CheckoutRoundingCalculation(
    val roundingAmount: BigDecimal,
    val finalTotal: BigDecimal,
)

internal fun estimateTaxCalculation(
    netGoodsTotal: BigDecimal,
    taxEnabled: Boolean,
    taxRate: BigDecimal,
    taxMode: String,
    taxLabel: String,
): CheckoutTaxCalculation {
    val net = netGoodsTotal.coerceAtLeast(BigDecimal.ZERO).money()
    if (!taxEnabled) {
        return CheckoutTaxCalculation(
            taxEnabled = false,
            taxMode = null,
            taxRate = BigDecimal.ZERO.setScale(MONEY_SCALE),
            taxLabel = null,
            taxableBase = BigDecimal.ZERO.setScale(MONEY_SCALE),
            taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE),
            totalWithTax = net,
        )
    }

    val rate = taxRate.coerceIn(BigDecimal.ZERO, HUNDRED).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
    val label = taxLabel.trim().ifBlank { "PPN" }
    val normalizedMode = if (taxMode.equals("INCLUDE", ignoreCase = true)) "INCLUDE" else "EXCLUDE"

    return if (normalizedMode == "INCLUDE") {
        val divisor = BigDecimal.ONE.add(rate.divide(HUNDRED, 6, RoundingMode.HALF_UP))
        val dpp = if (divisor.compareTo(BigDecimal.ZERO) == 0) net
        else net.divide(divisor, MONEY_SCALE, RoundingMode.HALF_UP)
        val taxAmount = net.subtract(dpp).money()
        CheckoutTaxCalculation(
            taxEnabled = true,
            taxMode = "INCLUDE",
            taxRate = rate,
            taxLabel = label,
            taxableBase = dpp,
            taxAmount = taxAmount,
            totalWithTax = net,
        )
    } else {
        val taxAmount = net.multiply(rate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP)
        CheckoutTaxCalculation(
            taxEnabled = true,
            taxMode = "EXCLUDE",
            taxRate = rate,
            taxLabel = label,
            taxableBase = net,
            taxAmount = taxAmount,
            totalWithTax = net.add(taxAmount).money(),
        )
    }
}

internal fun estimateCashRoundingCalculation(
    totalBeforeRounding: BigDecimal,
    hasCashPayment: Boolean,
    roundingUnit: Int,
    roundingRule: String,
): CheckoutRoundingCalculation {
    val base = totalBeforeRounding.coerceAtLeast(BigDecimal.ZERO).money()
    if (!hasCashPayment || roundingUnit !in setOf(100, 500, 1000) || base.compareTo(BigDecimal.ZERO) <= 0) {
        return CheckoutRoundingCalculation(
            roundingAmount = BigDecimal.ZERO.setScale(MONEY_SCALE),
            finalTotal = base,
        )
    }

    val step = BigDecimal(roundingUnit)
    val mode = when (roundingRule.uppercase()) {
        "DOWN" -> RoundingMode.FLOOR
        "UP" -> RoundingMode.CEILING
        else -> RoundingMode.HALF_UP
    }
    val rounded = base.divide(step, 0, mode)
        .multiply(step)
        .coerceAtLeast(BigDecimal.ZERO)
        .money()

    return CheckoutRoundingCalculation(
        roundingAmount = rounded.subtract(base).money(),
        finalTotal = rounded,
    )
}

private fun BigDecimal.money(): BigDecimal = setScale(MONEY_SCALE, RoundingMode.HALF_UP)

private const val MONEY_SCALE = 2
private val HUNDRED = BigDecimal("100")
