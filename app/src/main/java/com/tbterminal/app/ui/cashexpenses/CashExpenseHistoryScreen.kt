package com.tbterminal.app.ui.cashexpenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.ui.components.HistoryDateFilter
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ExpenseBackground = Color(0xFFF4FAFD)
private val ExpenseBorder = Color(0xFFE2E8F0)
private val ExpenseText = Color(0xFF0F172A)
private val ExpenseMuted = Color(0xFF64748B)
private val ExpensePrimary = Color(0xFF059669)
private val ExpenseDanger = Color(0xFFDC2626)
private val ExpenseSoft = Color(0xFFF1F5F9)

@Composable
internal fun CashExpenseHistoryScreen(
    modifier: Modifier,
    uiState: CashExpenseHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onDateChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onShowSessionDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(ExpenseBackground)
            .verticalScroll(rememberScrollState()).padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        ExpenseHistoryHeader(
            uiState = uiState,
            onPreviousDate = onPreviousDate,
            onNextDate = onNextDate,
            onCalendarClick = { showDatePicker = true },
            onClearDate = { onDateChanged(null) },
            onDatePresetSelected = onDatePresetSelected
        )
        ExpenseMetrics(uiState)
        ExpenseTable(
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onRefresh = onRefresh,
            onShowSessionDetail = onShowSessionDetail,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = uiState.endDate ?: uiState.selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChanged(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun ExpenseHistoryHeader(
    uiState: CashExpenseHistoryUiState,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onCalendarClick: () -> Unit,
    onClearDate: () -> Unit,
    onDatePresetSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "Pengeluaran Kas",
            color = ExpenseText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryDateFilter(
                selectedDate = uiState.endDate ?: uiState.selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = onClearDate,
                displayTextOverride = uiState.dateRangeLabel(),
                modifier = Modifier.width(320.dp)
            )
            ExpenseDatePresets(
                selectedPreset = uiState.selectedPreset,
                onSelected = onDatePresetSelected
            )
        }
    }
}

@Composable
private fun ExpenseDatePresets(selectedPreset: String?, onSelected: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ExpenseBorder),
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Hari ini", "Minggu ini", "Bulan ini").forEach { preset ->
                ExpenseDatePresetChip(
                    text = preset,
                    selected = selectedPreset == preset,
                    onClick = { onSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun ExpenseDatePresetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF86F8C9) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = if (selected) Color(0xFF00513A) else ExpenseMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpenseMetrics(uiState: CashExpenseHistoryUiState) {
    val totalValue = uiState.expenses.fold(BigDecimal.ZERO) { total, item -> total + item.amount }
    val sessionCount = uiState.expenses.map(CashExpense::sessionId).distinct().size
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ExpenseMetric("TOTAL CATATAN", uiState.totalExpenses.toString(), "Sesuai data tersimpan", Icons.AutoMirrored.Outlined.ReceiptLong, ExpensePrimary, Modifier.weight(1f))
        ExpenseMetric("NILAI HALAMAN INI", totalValue.asCurrency(), "Maksimal 10 pengeluaran", Icons.Outlined.Payments, ExpenseDanger, Modifier.weight(1f))
        ExpenseMetric("SESI TERKAIT", sessionCount.toString(), "Pada halaman yang tampil", Icons.AutoMirrored.Outlined.ReceiptLong, Color(0xFF2563EB), Modifier.weight(1f))
    }
}

@Composable
private fun ExpenseMetric(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, ExpenseBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = ExpenseText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = ExpenseMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExpenseTable(
    uiState: CashExpenseHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowSessionDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val visibleExpenses = uiState.filteredExpenses()
    Column(modifier = Modifier.fillMaxWidth()) {
        ExpenseToolbar(
            searchQuery = uiState.searchQuery,
            onSearchChanged = onSearchChanged
        )
        Spacer(Modifier.height(28.dp))
        ExpenseHeader()
        when {
            uiState.isLoading -> LoadingBox()
            uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onRefresh)
            uiState.expenses.isEmpty() -> EmptyBox()
            visibleExpenses.isEmpty() -> EmptyBox("Tidak ada pengeluaran yang cocok.")
            else -> visibleExpenses.forEach { ExpenseRow(it, onShowSessionDetail) }
        }
        ExpensePagination(uiState, visibleExpenses.size, onPreviousPage, onNextPage)
    }
}

@Composable
private fun ExpenseToolbar(searchQuery: String, onSearchChanged: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        placeholder = { Text("Cari kasir, sesi, atau deskripsi...", color = ExpenseMuted) },
        trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Cari pengeluaran", tint = ExpenseMuted) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ExpenseText,
            unfocusedTextColor = ExpenseText,
            cursorColor = ExpensePrimary,
            focusedBorderColor = ExpensePrimary,
            unfocusedBorderColor = ExpenseBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun ExpenseHeader() {
    Row(Modifier.fillMaxWidth().background(ExpenseSoft).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Label("KASIR", Modifier.weight(1.2f))
        Label("SESI", Modifier.weight(1.25f))
        Label("WAKTU", Modifier.weight(1.55f))
        Label("DESKRIPSI", Modifier.weight(2f))
        Label("NOMINAL", Modifier.weight(1.1f))
        Label("AKSI", Modifier.weight(0.45f))
    }
}

@Composable
private fun ExpenseRow(item: CashExpense, onShowSessionDetail: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.2f)) {
            Text(item.userName ?: "Kasir", color = ExpenseText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(item.userId.take(8), color = ExpenseMuted, fontSize = 10.sp)
        }
        Text(item.sessionId.take(8), Modifier.weight(1.25f), color = ExpenseMuted, fontSize = 12.sp)
        Text(item.createdAt.asDateTime(), Modifier.weight(1.55f), color = ExpenseMuted, fontSize = 12.sp)
        Text(item.description, Modifier.weight(2f), color = ExpenseText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.amount.asCurrency(), Modifier.weight(1.1f), color = ExpenseDanger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.45f)) {
            IconButton(onClick = { onShowSessionDetail(item.sessionId) }) {
                Icon(Icons.Outlined.Visibility, "Lihat detail sesi", tint = ExpensePrimary)
            }
        }
    }
    HorizontalDivider(color = ExpenseBorder)
}

@Composable
private fun ExpensePagination(
    uiState: CashExpenseHistoryUiState,
    visibleCount: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val start = if (visibleCount == 0) 0 else ((uiState.page - 1) * uiState.pageSize) + 1
    val end = if (visibleCount == 0) 0 else start + visibleCount - 1
    val totalText = if (uiState.searchQuery.isBlank()) {
        "${uiState.totalExpenses} pengeluaran"
    } else {
        "$visibleCount hasil pada halaman ini"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(ExpenseSoft)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Menampilkan $start-$end dari $totalText",
                color = ExpenseText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Maksimal ${uiState.pageSize} pengeluaran per halaman",
                color = ExpenseMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpensePageButton(onClick = onPreviousPage, enabled = uiState.page > 1, text = "<")
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(ExpensePrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${uiState.page}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = ExpenseMuted, fontWeight = FontWeight.SemiBold)
            ExpensePageButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages, text = ">")
        }
    }
}

@Composable
private fun ExpensePageButton(onClick: () -> Unit, enabled: Boolean, text: String) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseText)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun EmptyBox(message: String = "Belum ada pengeluaran kas.") = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text(message, color = ExpenseMuted) }

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = ExpenseDanger)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) = Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun CashExpenseHistoryUiState.filteredExpenses(): List<CashExpense> {
    val query = searchQuery.trim()
    if (query.isBlank()) return expenses
    return expenses.filter { expense ->
        expense.id.contains(query, ignoreCase = true) ||
            expense.sessionId.contains(query, ignoreCase = true) ||
            expense.userId.contains(query, ignoreCase = true) ||
            expense.userName.orEmpty().contains(query, ignoreCase = true) ||
            expense.description.contains(query, ignoreCase = true) ||
            expense.amount.asCurrency().contains(query, ignoreCase = true)
    }
}

private fun CashExpenseHistoryUiState.dateRangeLabel(): String? {
    val start = startDate ?: selectedDate
    val end = endDate ?: selectedDate
    if (start == null || end == null) return null
    val startText = start.asShortDate()
    val endText = end.asShortDate()
    return if (start == end) startText else "$startText - $endText"
}

private fun String.asShortDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

private fun String.asDateTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
