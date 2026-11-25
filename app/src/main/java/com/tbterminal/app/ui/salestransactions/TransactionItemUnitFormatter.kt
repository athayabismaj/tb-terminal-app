package com.tbterminal.app.ui.salestransactions

import com.tbterminal.app.data.model.CashTransactionItem
import java.math.BigDecimal
import java.math.RoundingMode

internal fun formatTransactionQty(quantity: BigDecimal): String {
    if (quantity.compareTo(BigDecimal.ZERO) == 0) return "0"
    val stripped = quantity.stripTrailingZeros()
    return if (stripped.scale() <= 0) {
        stripped.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
    } else {
        stripped.toPlainString()
    }
}

internal fun formatTransactionQtyWithOptionalUnit(
    quantity: BigDecimal,
    unitName: String?,
): String {
    val qtyText = formatTransactionQty(quantity)
    val cleanUnit = unitName?.trim()?.takeIf(String::isNotBlank)
    return if (cleanUnit != null) "$qtyText $cleanUnit" else qtyText
}

internal fun resolveInvoiceUnitLabel(
    unitName: String?,
    fallback: String = "pcs",
): String = unitName?.trim()?.takeIf(String::isNotBlank) ?: fallback

internal fun resolveSummaryTotalUnitLabel(
    items: List<CashTransactionItem>,
    fallback: String = "Pcs",
): String {
    if (items.isEmpty()) return fallback
    val cleanUnits = items.map { item ->
        item.unitName?.trim()?.takeIf(String::isNotBlank) ?: return fallback
    }
    return if (cleanUnits.map { it.lowercase() }.distinct().size == 1) {
        cleanUnits.first()
    } else {
        fallback
    }
}
