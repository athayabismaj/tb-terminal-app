package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import com.tbterminal.app.ui.components.AppDatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.components.TbPeriodFilterRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDateRangeFilter(
    startDate: String,
    endDate: String,
    onDateRangeChanged: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(detectPreset(startDate, endDate)) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(startDate, endDate) {
        selectedFilter = detectPreset(startDate, endDate)
    }

    TbPeriodFilterRow(
        selectedValue = selectedFilter,
        presets = listOf(
            "Hari ini" to "Hari",
            "7 hari" to "Minggu",
            "30 hari" to "Bulan",
        ),
        dateLabel = endDate.toCompactDateLabel(),
        dateSelected = selectedFilter.isBlank(),
        onPresetSelected = { filter ->
            selectedFilter = filter
            val today = LocalDate.now()
            val range = when (filter) {
                "Hari ini" -> today to today
                "7 hari" -> today.minusDays(6) to today
                else -> today.minusDays(29) to today
            }
            onDateRangeChanged(range.first, range.second)
        },
        onDateClick = { showDatePicker = true },
        modifier = modifier,
        testTag = "report-period-filter",
        dateTestTag = "report-custom-date-toggle",
    )

    if (showDatePicker) {
        ReportsSingleDatePickerDialog(
            currentDate = startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                onDateRangeChanged(date, date)
            }
        )
    }
}

@Composable
private fun ReportsSingleDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    AppDatePickerDialog(
        initialDate = currentDate,
        title = "Pilih Tanggal",
        subtitle = "Filter laporan berdasarkan tanggal",
        onDismiss = onDismiss,
        onConfirm = { dateStr ->
            runCatching { LocalDate.parse(dateStr) }.getOrNull()?.let(onConfirm)
        }
    )
}

private fun String.toCompactDateLabel(): String =
    toLocalDateOrNull()?.format(DateTimeFormatter.ofPattern("dd/MM")) ?: this

private fun String.toLocalDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDateUtc(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

private fun detectPreset(startDate: String, endDate: String): String {
    return try {
        val start = LocalDate.parse(startDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        val end = LocalDate.parse(endDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        when {
            start == today && end == today -> "Hari ini"
            start == today.minusDays(6) && end == today -> "7 hari"
            start == today.minusDays(29) && end == today -> "30 hari"
            else -> ""
        }
    } catch (_: DateTimeParseException) {
        ""
    }
}
