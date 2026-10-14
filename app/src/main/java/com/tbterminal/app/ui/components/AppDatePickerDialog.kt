package com.tbterminal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private val INDONESIAN_MONTHS = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private val BrandPrimary = Color(0xFF059669) // Emerald 600
private val BrandSoft = Color(0xFFE8F5E9)

/**
 * Modern Indonesian Calendar Date Picker Dialog
 * Matches the requested UI specification with Indonesian day initials (M S S R K J S)
 * and Indonesian month names.
 */
@Composable
fun AppDatePickerDialog(
    initialDate: String? = null,
    title: String = "Pilih Tanggal",
    subtitle: String = "Filter transaksi berdasarkan tanggal",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parsedInitialDate = remember(initialDate) {
        initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    }
    var selectedDate by remember { mutableStateOf(parsedInitialDate) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.from(parsedInitialDate)) }
    var isSelectingMonthYear by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 360.dp)
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
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val monthName = INDONESIAN_MONTHS[displayedYearMonth.monthValue - 1]
                        Text(
                            text = "$monthName ${displayedYearMonth.year}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    displayedYearMonth = displayedYearMonth.minusMonths(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Bulan Sebelumnya",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    displayedYearMonth = displayedYearMonth.plusMonths(1)
                                },
                            contentAlignment = Alignment.Center
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
                            isSelectingMonthYear = false
                        }
                    )
                } else {
                    // Days of Week Header (Indonesian: M S S R K J S)
                    // M = Minggu, S = Senin, S = Selasa, R = Rabu, K = Kamis, J = Jumat, S = Sabtu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val daysHeader = listOf(
                            "M" to Color(0xFF94A3B8), // Minggu
                            "S" to Color(0xFF64748B), // Senin
                            "S" to Color(0xFF64748B), // Selasa
                            "R" to Color(0xFF64748B), // Rabu
                            "K" to Color(0xFF64748B), // Kamis
                            "J" to Color(0xFF64748B), // Jumat
                            "S" to Color(0xFF64748B)  // Sabtu
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

                    // Calendar Month Matrix
                    val firstDayOfMonth = displayedYearMonth.atDay(1)
                    val daysInMonth = displayedYearMonth.lengthOfMonth()
                    // Sunday is offset 0 (SUNDAY.value % 7 = 7 % 7 = 0)
                    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
                    val totalSlots = startOffset + daysInMonth
                    val rowCount = (totalSlots + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNumber in 1..daysInMonth) {
                                            val date = displayedYearMonth.atDay(dayNumber)
                                            val isSelected = date == selectedDate
                                            val isToday = date == today

                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier
                                                                .shadow(elevation = 6.dp, shape = CircleShape, spotColor = BrandPrimary)
                                                                .background(BrandPrimary, CircleShape)
                                                        } else if (isToday) {
                                                            Modifier
                                                                .background(BrandSoft, CircleShape)
                                                                .border(1.dp, BrandPrimary.copy(alpha = 0.5f), CircleShape)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        selectedDate = date
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> BrandPrimary
                                                        col == 0 -> Color(0xFFEF4444) // Minggu tint
                                                        else -> Color(0xFF334155) // Slate 700
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(38.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // Footer Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedDate.toString())
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Terapkan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
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
