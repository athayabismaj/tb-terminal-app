package com.tbterminal.app.ui.cash

import androidx.compose.ui.graphics.Color
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

internal fun BigDecimal?.money(): String? {
    if (this == null) return null
    return NumberFormat.getCurrencyInstance(IndonesianLocale).format(this)
}

internal fun String?.displayDateTime(): String {
    if (isNullOrBlank()) return "-"
    return runCatching {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", IndonesianLocale))
    }.getOrDefault(this)
}

internal fun String.displayTime(): String {
    return runCatching {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("HH:mm", IndonesianLocale))
    }.getOrDefault("-")
}

internal fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "lunas", "open" -> CashPrimary
        "dp" -> CashInfo
        "hutang" -> CashWarning
        else -> CashMuted
    }
}
