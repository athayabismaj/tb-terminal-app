package com.tbterminal.app.ui.products

import androidx.compose.ui.graphics.Color
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal val ProductBackground = Color(0xFFF8FAFC)
internal val ProductSurface = Color.White
internal val ProductPrimary = Color(0xFF10B981)
internal val ProductPrimaryDark = Color(0xFF059669)
internal val ProductText = Color(0xFF0F172A)
internal val ProductMuted = Color(0xFF64748B)
internal val ProductLine = Color(0xFFE2E8F0)
internal val ProductSoft = Color(0xFFF1F5F9)
internal val ProductDanger = Color(0xFFEF4444)
internal val ProductWarning = Color(0xFFF59E0B)
internal val ProductInfo = Color(0xFF2563EB)

internal fun String.numericInput(): String {
    return filter { char -> char.isDigit() || char == '.' || char == ',' }
}

internal fun BigDecimal.moneyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

internal fun BigDecimal.quantityText(): String {
    return stripTrailingZeros().toPlainString()
}
