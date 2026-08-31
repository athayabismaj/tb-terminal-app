package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDateRangeFilter(
    startDate: String,
    endDate: String,
    onDateRangeChanged: (LocalDate, LocalDate) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(detectPreset(startDate, endDate)) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(startDate, endDate) {
        selectedFilter = detectPreset(startDate, endDate)
    }

    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 600.dp
        val dateSelector: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Filter tanggal",
                    tint = ReportColors.Outline,
                    modifier = Modifier.size(16.dp)
                )
                DateText(text = dateRangeDisplay(startDate, endDate))
            }
        }
        val presets: @Composable () -> Unit = {
            Row(
                modifier = if (compact) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Hari ini", "7 hari", "30 hari").forEach { filter ->
                    DateFilterChip(
                        text = filter,
                        selected = selectedFilter == filter,
                        onClick = {
                            selectedFilter = filter
                            val today = LocalDate.now()
                            val range = when (filter) {
                                "Hari ini" -> today to today
                                "7 hari" -> today.minusDays(6) to today
                                else -> today.minusDays(29) to today
                            }
                            onDateRangeChanged(range.first, range.second)
                        }
                    )
                }
            }
        }
        Surface(
            modifier = if (compact) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ReportColors.Surface,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, ReportColors.Slate200)
        ) {
            if (compact) Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dateSelector()
                presets()
            } else Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dateSelector()
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(ReportColors.Slate200)
            )
                presets()
            }
        }
    }

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
private fun DateText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = ReportColors.OnSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DateFilterChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) Color(0xFF86F8C9) else Color.Transparent
    val textColor = if (selected) Color(0xFF00513A) else ReportColors.Slate500

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsSingleDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val initialMillis = currentDate.toLocalDateOrNull()?.toEpochMillis()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = ReportColors.Surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Tanggal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReportColors.OnSurface
                    )
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = ReportColors.SurfaceSoft,
                        onClick = onDismiss
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                tint = ReportColors.Outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.height(430.dp),
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = ReportColors.Surface,
                        selectedDayContainerColor = ReportColors.PrimaryDark,
                        todayDateBorderColor = ReportColors.PrimaryDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Batal",
                            color = ReportColors.Slate500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ReportColors.PrimaryDark,
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                onConfirm(millis.toLocalDateUtc())
                            }
                        }
                    ) {
                        Text(
                            text = "Terapkan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun String.toDisplayDate(): String {
    return try {
        LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE).toDisplayDate()
    } catch (_: DateTimeParseException) {
        this
    }
}

private fun dateRangeDisplay(startDate: String, endDate: String): String {
    val start = startDate.toLocalDateOrNull() ?: return startDate.toDisplayDate()
    val end = endDate.toLocalDateOrNull() ?: return endDate.toDisplayDate()
    return if (start == end) {
        start.toDisplayDate()
    } else {
        "${start.toDisplayDate()} - ${end.toDisplayDate()}"
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun LocalDate.toDisplayDate(): String {
    return format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
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
