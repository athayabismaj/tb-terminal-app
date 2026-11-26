package com.tbterminal.app.ui.products

import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.common.parseIdrDecimalInput
import com.tbterminal.app.ui.common.parseWholeRupiahInput
import java.math.BigDecimal
import java.math.RoundingMode

internal fun List<ProductUnit>.selectedUnitLabel(unitId: String): String {
    return firstOrNull { unit -> unit.id == unitId }?.let { unit ->
        "${unit.name} (${unit.symbol})"
    } ?: "Pilih satuan"
}

internal fun String.marginText(basePrice: String): String {
    val buy = parseIdrDecimalInput(basePrice) ?: return "0%"
    val sell = parseWholeRupiahInput(this) ?: return "0%"
    if (buy.compareTo(BigDecimal.ZERO) <= 0) return "0%"

    val margin = sell.subtract(buy)
        .multiply(BigDecimal(100))
        .divide(buy, 0, RoundingMode.HALF_UP)
    return "${margin.toPlainString()}%"
}

internal fun generateSku(productName: String): String {
    val prefix = productName
        .filter(Char::isLetterOrDigit)
        .uppercase()
        .take(3)
        .ifBlank { "PRD" }
    return "$prefix-${System.currentTimeMillis().toString().takeLast(6)}"
}
