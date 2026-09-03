package com.tbterminal.app.ui.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesiaLocale: Locale = Locale.forLanguageTag("id-ID")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", indonesiaLocale)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", indonesiaLocale)

fun BigDecimal.toRupiahText(): String = "Rp ${rupiahNumberFormatter().format(this)}"

fun Number.toRupiahText(): String = toString().toBigDecimalOrNull()?.toRupiahText() ?: "Rp 0"

fun String.toDisplayDate(): String {
    val value = trim()
    if (value.isEmpty()) return "-"
    return runCatching { OffsetDateTime.parse(value).format(dateFormatter) }
        .recoverCatching { LocalDateTime.parse(value).format(dateFormatter) }
        .recoverCatching { LocalDate.parse(value.take(10)).format(dateFormatter) }
        .getOrDefault("-")
}

fun String.toDisplayTime(): String {
    val value = trim()
    if (value.isEmpty()) return "-"
    return runCatching { OffsetDateTime.parse(value).format(timeFormatter) }
        .recoverCatching { LocalDateTime.parse(value).format(timeFormatter) }
        .getOrDefault("-")
}

fun String.toDisplayDateTime(): String {
    val date = toDisplayDate()
    val time = toDisplayTime()
    return when {
        date == "-" -> "-"
        time == "-" -> date
        else -> "$date · $time"
    }
}

private fun rupiahNumberFormatter(): NumberFormat = NumberFormat.getNumberInstance(indonesiaLocale).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
    isGroupingUsed = true
}
