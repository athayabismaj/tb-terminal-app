package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val CashierLine = Color(0xFFE2E8F0)
private val CashierSurfaceSoft = Color(0xFFF1F5F9)
private val DateBarBg = Color(0xFF1E293B)

@Composable
fun CashierTransactionHistoryScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReceiptClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CashierTransactionHistoryViewModel = viewModel(
        factory = CashierTransactionHistoryViewModel.factory(cashReconciliationRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadTransactions(page = 1)
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.TransactionHistory,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        CashierTransactionHistoryContent(
            state = uiState,
            viewModel = viewModel,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onReceiptClick = onReceiptClick,
            modifier = contentModifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashierTransactionHistoryContent(
    state: CashierTransactionHistoryUiState,
    viewModel: CashierTransactionHistoryViewModel,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReceiptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendarPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardBackground)
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──
        CashierHistoryHeader()

        Spacer(modifier = Modifier.height(20.dp))

        // ── Period Info ──
        Text(
            text = if (state.selectedDate != null)
                "Periode data: ${state.selectedDate.formatDisplayDate()}"
            else
                "Periode data: Semua tanggal pada sesi aktif",
            color = DashboardTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Search + Date Navigator ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CashierLine),
                modifier = Modifier.weight(1.5f).height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = DashboardTextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (state.query.isEmpty()) {
                            Text("Cari no struk atau pembeli...", color = DashboardTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = viewModel::updateSearchQuery,
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = DashboardTextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Date Navigator Bar
            CashierDateNavigator(
                selectedDate = state.selectedDate,
                onPreviousDate = viewModel::previousDate,
                onNextDate = viewModel::nextDate,
                onCalendarClick = { showCalendarPicker = true },
                onClearDate = { viewModel.setDate(null) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Status Chips ──
        val statuses = listOf("Semua", "Lunas", "DP", "Hutang")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statuses.size) { index ->
                val status = statuses[index]
                val isSelected = status == state.statusFilter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) DashboardBrandGreenDark else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else CashierLine),
                    onClick = { viewModel.updateStatusFilter(status) }
                ) {
                    Text(
                        text = status,
                        color = if (isSelected) Color.White else DashboardTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Table Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DashboardSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CashierLine)
        ) {
            Column {
                CashierHistoryTableHeader()
                HorizontalDivider(color = CashierLine)

                when {
                    state.isLoading -> CashierHistorySkeletonRows()
                    state.errorMessage != null -> CashierHistoryMessage(state.errorMessage)
                    !state.hasActiveSession -> CashierHistoryMessage("Belum ada sesi kasir aktif.")
                    state.transactions.isEmpty() -> CashierHistoryMessage("Belum ada transaksi pada sesi ini.")
                    else -> {
                        state.transactions.forEachIndexed { index, transaction ->
                            CashierHistoryRow(transaction = transaction, onReceiptClick = onReceiptClick)
                            if (index < state.transactions.lastIndex) {
                                HorizontalDivider(color = CashierLine.copy(alpha = 0.75f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = CashierLine)
                CashierHistoryPagination(state, onPreviousPage, onNextPage)
            }
        }
    }

    // ── Calendar Picker Dialog ──
    if (showCalendarPicker) {
        CashierCalendarPickerDialog(
            currentDate = state.selectedDate,
            onDismiss = { showCalendarPicker = false },
            onConfirm = { date ->
                viewModel.setDate(date)
                showCalendarPicker = false
            }
        )
    }
}

// ═══════════════════════════════════════════════
// DATE NAVIGATOR BAR
// ═══════════════════════════════════════════════
@Composable
private fun CashierDateNavigator(
    selectedDate: String?,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onCalendarClick: () -> Unit,
    onClearDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayText = selectedDate?.formatDisplayDate() ?: "Semua Tanggal"
    val isToday = selectedDate == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CashierLine),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left arrow
            IconButton(
                onClick = {
                    if (selectedDate == null) {
                        onClearDate()
                    }
                    onPreviousDate()
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Hari sebelumnya", tint = DashboardTextSecondary)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Date text
            Text(
                text = displayText,
                color = DashboardTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            if (selectedDate != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Hapus filter tanggal",
                    tint = DashboardTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClearDate() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Calendar icon
            IconButton(onClick = onCalendarClick) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pilih tanggal", tint = DashboardBrandGreenDark)
            }

            // Right arrow
            IconButton(
                onClick = onNextDate,
                enabled = !isToday && selectedDate != null
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Hari berikutnya",
                    tint = if (!isToday && selectedDate != null) DashboardTextSecondary else CashierLine
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// CALENDAR PICKER DIALOG (single date)
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashierCalendarPickerDialog(
    currentDate: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialMillis = currentDate?.let {
        runCatching {
            LocalDate.parse(it).toEpochDay() * 86_400_000L
        }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pilih Tanggal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DashboardTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup")
                    }
                }

                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.height(460.dp),
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        selectedDayContainerColor = DashboardBrandGreenDark,
                        todayDateBorderColor = DashboardBrandGreenDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = DashboardTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DashboardBrandGreenDark,
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                onConfirm(dateFormat.format(Date(millis)))
                            }
                        }
                    ) {
                        Text(
                            "Terapkan",
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

// ═══════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════
@Composable
private fun CashierHistoryHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashboardBrandGreenDark.copy(alpha = 0.12f))
                    .padding(14.dp)
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Outlined.History,
                    contentDescription = null,
                    tint = DashboardBrandGreenDark,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Riwayat Transaksi", color = DashboardTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "Lihat transaksi pada sesi kasir aktif dan buka detail struk.",
                    color = DashboardTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(DashboardBrandGreen.copy(alpha = 0.14f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.History, contentDescription = null, tint = DashboardBrandGreenDark)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Riwayat sesi kasir", color = DashboardBrandGreenDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═══════════════════════════════════════════════
// TABLE HEADER, ROW, SKELETON, PAGINATION
// ═══════════════════════════════════════════════
@Composable
private fun CashierHistoryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CashierSurfaceSoft)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CashierHeaderText("WAKTU", Modifier.weight(1.2f))
        CashierHeaderText("NO STRUK", Modifier.weight(1.6f))
        CashierHeaderText("PEMBELI", Modifier.weight(1.2f))
        CashierHeaderText("STATUS", Modifier.weight(1f))
        CashierHeaderText("TOTAL", Modifier.weight(1.35f), Alignment.End)
        CashierHeaderText("DIBAYAR", Modifier.weight(1.35f), Alignment.End)
        CashierHeaderText("AKSI", Modifier.weight(0.6f), Alignment.End)
    }
}

@Composable
private fun CashierHistoryRow(
    transaction: CashTransaction,
    onReceiptClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(transaction.createdAt.displayTime(), color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
            Text(transaction.createdAt.displayDate(), color = DashboardTextSecondary, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(1.6f)) {
            Text(
                text = transaction.receiptNumber(),
                color = DashboardTextPrimary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = transaction.customerName ?: "-",
                color = DashboardTextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(transaction.status.statusColor().copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(transaction.status.uppercase(), color = transaction.status.statusColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            transaction.total.moneyText(),
            color = DashboardTextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.35f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            transaction.paidAmount.moneyText(),
            color = DashboardTextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.35f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.CenterEnd) {
            Icon(Icons.Outlined.Visibility, contentDescription = "Detail struk", tint = DashboardTextSecondary)
        }
    }
}

@Composable
private fun CashierHeaderText(
    text: String,
    modifier: Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, color = DashboardTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CashierHistorySkeletonRows() {
    Column {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Box(modifier = Modifier.size(40.dp, 16.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.size(60.dp, 12.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                }
                Column(modifier = Modifier.weight(1.6f)) {
                    Box(modifier = Modifier.size(100.dp, 16.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    Box(modifier = Modifier.size(80.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                }
                Box(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(60.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(CashierLine))
                }
                Box(modifier = Modifier.weight(1.35f), contentAlignment = Alignment.CenterEnd) {
                    Box(modifier = Modifier.size(80.dp, 16.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                }
                Box(modifier = Modifier.weight(1.35f), contentAlignment = Alignment.CenterEnd) {
                    Box(modifier = Modifier.size(80.dp, 16.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                }
                Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.CenterEnd) {
                    Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(CashierLine))
                }
            }
            HorizontalDivider(color = CashierLine.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun CashierHistoryMessage(message: String) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Text(message, color = DashboardTextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CashierHistoryPagination(
    state: CashierTransactionHistoryUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CashierSurfaceSoft)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Menampilkan ${state.currentStart}-${state.currentEnd} dari ${state.total} transaksi",
            color = DashboardTextSecondary,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onPreviousPage,
                enabled = state.page > 1,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CashierLine)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DashboardBrandGreenDark),
                contentAlignment = Alignment.Center
            ) {
                Text(state.page.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${state.totalPages}", color = DashboardTextSecondary, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = onNextPage,
                enabled = state.page < state.totalPages,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CashierLine)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// UTILITY EXTENSIONS
// ═══════════════════════════════════════════════
internal fun CashTransaction.receiptNumber(): String = receiptId

internal fun BigDecimal.moneyText(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

internal fun String.displayDateTime(): String = runCatching {
    OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

internal fun String.displayDate(): String = runCatching {
    OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}.getOrDefault("-")

internal fun String.displayTime(): String = runCatching {
    OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault("-")

internal fun String.statusColor(): Color = when (lowercase()) {
    "lunas", "paid", "success", "completed" -> DashboardBrandGreenDark
    "dp", "partial" -> DashboardWarningOrange
    "hutang", "unpaid" -> Color(0xFFEF4444)
    else -> DashboardTextSecondary
}

/** Format "yyyy-MM-dd" -> "dd/MM/yyyy" for display */
private fun String.formatDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)
