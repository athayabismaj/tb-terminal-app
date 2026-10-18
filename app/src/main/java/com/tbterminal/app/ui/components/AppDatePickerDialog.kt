package com.tbterminal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val INDONESIAN_MONTHS = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private val BrandPrimary = Color(0xFF256B57)
private val BrandDark = Color(0xFF1E5646)
private val BrandSoft = Color(0xFFE8F2EE)
private val PreviewDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))

/**
 * Modern Indonesian Calendar Date Picker Dialog
 * Supports selection modes: "Hari" (single day), "Minggu" (full week Mon-Sun),
 * and "Bulan" (full month 1st to last day).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: String? = null,
    title: String? = null,
    subtitle: String? = null,
    initialMode: String = "Hari",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit = {},
    onConfirmRange: ((startDate: String, endDate: String, mode: String) -> Unit)? = null
) {
    val parsedInitialDate = remember(initialDate) {
        initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    }
    var currentMode by remember {
        mutableStateOf(
            when (initialMode.lowercase()) {
                "bulan", "bulan ini" -> "Bulan"
                "minggu", "minggu ini" -> "Minggu"
                else -> "Hari"
            }
        )
    }
    var selectedDate by remember { mutableStateOf(parsedInitialDate) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.from(parsedInitialDate)) }
    var isSelectingMonthYear by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }

    val resolvedTitle = title ?: when (currentMode) {
        "Bulan" -> "Pilih Bulan"
        "Minggu" -> "Pilih Minggu"
        else -> "Pilih Tanggal"
    }
    val resolvedSubtitle = subtitle ?: when (currentMode) {
        "Bulan" -> "Pilih tanggal/bulan untuk filter 1 bulan penuh"
        "Minggu" -> "Pilih tanggal untuk filter 1 minggu penuh"
        else -> "Filter transaksi berdasarkan tanggal"
    }

    val daysInMonth = displayedYearMonth.lengthOfMonth()
    val monthName = INDONESIAN_MONTHS[displayedYearMonth.monthValue - 1]

    val (rangeStart, rangeEnd) = when (currentMode) {
        "Bulan" -> {
            val start = displayedYearMonth.atDay(1)
            val end = displayedYearMonth.atEndOfMonth()
            start to end
        }
        "Minggu" -> {
            val start = selectedDate.with(DayOfWeek.MONDAY)
            val end = selectedDate.with(DayOfWeek.SUNDAY)
            start to end
        }
        else -> {
            selectedDate to selectedDate
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {

            // Header: Title, Subtitle, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = resolvedTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = resolvedSubtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Dialog",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month Navigation Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month & Year Button with Dropdown Indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isSelectingMonthYear = !isSelectingMonthYear }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$monthName ${displayedYearMonth.year}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Icon(
                        imageVector = if (isSelectingMonthYear) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pilih Bulan dan Tahun",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Month Pager Controls (< >)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            displayedYearMonth = displayedYearMonth.minusMonths(1)
                            if (currentMode == "Bulan") {
                                selectedDate = displayedYearMonth.atDay(1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan Sebelumnya",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            displayedYearMonth = displayedYearMonth.plusMonths(1)
                            if (currentMode == "Bulan") {
                                selectedDate = displayedYearMonth.atDay(1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan Berikutnya",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isSelectingMonthYear) {
                // Month & Year Quick Selector View
                MonthYearQuickPicker(
                    currentYearMonth = displayedYearMonth,
                    onYearMonthSelected = { ym ->
                        displayedYearMonth = ym
                        selectedDate = ym.atDay(1)
                        isSelectingMonthYear = false
                    }
                )
            } else {
                // Days of Week Header (Indonesian: S S R K J S M, Senin - Minggu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysHeader = listOf(
                        "S" to Color(0xFF64748B), // Senin
                        "S" to Color(0xFF64748B), // Selasa
                        "R" to Color(0xFF64748B), // Rabu
                        "K" to Color(0xFF64748B), // Kamis
                        "J" to Color(0xFF64748B), // Jumat
                        "S" to Color(0xFF64748B), // Sabtu
                        "M" to Color(0xFFF43F5E)  // Minggu (Weekend)
                    )
                    daysHeader.forEach { (initial, color) ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }
                    }
                }

                // Calendar Month Matrix (Monday = 0 ... Sunday = 6)
                val firstDayOfMonth = displayedYearMonth.atDay(1)
                val startOffset = firstDayOfMonth.dayOfWeek.value - 1
                val totalSlots = startOffset + daysInMonth
                val rowCount = (totalSlots + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0 until rowCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 0..6) {
                                val slotIndex = row * 7 + col
                                val dayNumber = slotIndex - startOffset + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNumber in 1..daysInMonth) {
                                        val date = displayedYearMonth.atDay(dayNumber)
                                        val isSunday = col == 6
                                        val isToday = date == today
                                        val isSelected = date == selectedDate

                                        // LAYER 1: Background Strip
                                        when (currentMode) {
                                            "Minggu" -> {
                                                val isInWeek = !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
                                                if (isInWeek) {
                                                    when {
                                                        date == rangeStart -> {
                                                            // Dari tengah ke kanan
                                                            Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .fillMaxHeight()
                                                                        .background(BrandSoft)
                                                                )
                                                            }
                                                        }
                                                        date == rangeEnd -> {
                                                            // Dari kiri ke tengah
                                                            Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .fillMaxHeight()
                                                                        .background(BrandSoft)
                                                                )
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                        else -> {
                                                            // Penuh di tengah
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(32.dp)
                                                                    .background(BrandSoft)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            "Bulan" -> {
                                                when {
                                                    date.dayOfMonth == 1 -> {
                                                        Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight()
                                                                    .background(BrandSoft)
                                                            )
                                                        }
                                                    }
                                                    date.dayOfMonth == daysInMonth -> {
                                                        Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight()
                                                                    .background(BrandSoft)
                                                            )
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    else -> {
                                                        val shape = when {
                                                            col == 0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                                                            col == 6 -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                                                            else -> RoundedCornerShape(0.dp)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(32.dp)
                                                                .background(BrandSoft, shape)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // LAYER 2: Circle Indicator & Text
                                        val isSolidStart = when (currentMode) {
                                            "Hari" -> isSelected
                                            "Minggu" -> date == rangeStart
                                            "Bulan" -> date.dayOfMonth == 1
                                            else -> isSelected
                                        }
                                        val isBorderEnd = when (currentMode) {
                                            "Minggu" -> date == rangeEnd
                                            "Bulan" -> date.dayOfMonth == daysInMonth
                                            else -> false
                                        }
                                        val isInRangeMiddle = when (currentMode) {
                                            "Minggu" -> !date.isBefore(rangeStart) && !date.isAfter(rangeEnd) && !isSolidStart && !isBorderEnd
                                            "Bulan" -> date.dayOfMonth in 2 until daysInMonth
                                            else -> false
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .then(
                                                    when {
                                                        isSolidStart -> Modifier
                                                            .shadow(elevation = 3.dp, shape = CircleShape, spotColor = BrandPrimary)
                                                            .background(BrandPrimary, CircleShape)
                                                        isBorderEnd -> Modifier
                                                            .background(Color.White, CircleShape)
                                                            .border(2.dp, BrandPrimary, CircleShape)
                                                        else -> Modifier
                                                    }
                                                )
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedDate = date
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = when {
                                                        isSolidStart || isBorderEnd || isInRangeMiddle -> FontWeight.Bold
                                                        isSunday -> FontWeight.SemiBold
                                                        else -> FontWeight.Medium
                                                    },
                                                    color = when {
                                                        isSolidStart -> Color.White
                                                        isBorderEnd -> if (isSunday) Color(0xFFF43F5E) else BrandDark
                                                        isInRangeMiddle -> BrandDark
                                                        isSunday -> Color(0xFFF43F5E)
                                                        else -> Color(0xFF334155)
                                                    }
                                                )
                                                if (isToday && !isSolidStart) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.dp)
                                                            .background(
                                                                if (isInRangeMiddle || isBorderEnd) BrandDark else BrandPrimary,
                                                                CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Selection Summary Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Periode terpilih:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = when (currentMode) {
                                "Bulan" -> "${rangeStart.format(PreviewDateFormatter)} – ${rangeEnd.format(PreviewDateFormatter)}"
                                "Minggu" -> "${rangeStart.format(PreviewDateFormatter)} – ${rangeEnd.format(PreviewDateFormatter)}"
                                else -> rangeStart.format(PreviewDateFormatter)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
                Text(
                    text = when (currentMode) {
                        "Bulan" -> "${displayedYearMonth.lengthOfMonth()} Hari"
                        "Minggu" -> "7 Hari"
                        else -> "1 Hari"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        onConfirmRange?.invoke(rangeStart.toString(), rangeEnd.toString(), currentMode)
                        onConfirm(rangeStart.toString())
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Terapkan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
}

@Composable
private fun MonthYearQuickPicker(
    currentYearMonth: YearMonth,
    onYearMonthSelected: (YearMonth) -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYearMonth.year) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Year Switcher Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { selectedYear -= 1 },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Tahun Lalu",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF475569)
                )
            }
            Text(
                text = selectedYear.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { selectedYear += 1 },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tahun Depan",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF475569)
                )
            }
        }

        // 12 Months Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(INDONESIAN_MONTHS.indices.toList()) { monthIndex ->
                val month = monthIndex + 1
                val isSelected = selectedYear == currentYearMonth.year && month == currentYearMonth.monthValue
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandPrimary else Color(0xFFF8FAFC))
                        .clickable {
                            onYearMonthSelected(YearMonth.of(selectedYear, month))
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = INDONESIAN_MONTHS[monthIndex],
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF334155)
                    )
                }
            }
        }
    }
}
