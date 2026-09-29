package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange
import com.tbterminal.app.ui.components.HistoryDateFilter
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.offline.LocalPendingTransactionsCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CashierLine = Color(0xFFE2E8F0)
private val CashierSurfaceSoft = Color(0xFFF1F5F9)

@Composable
fun CashierTransactionHistoryScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    transactionDao: TransactionDao? = null,
    offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
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
        factory = CashierTransactionHistoryViewModel.factory(
            repository = cashReconciliationRepository,
            transactionDao = transactionDao,
            offlineCheckoutSyncService = offlineCheckoutSyncService
        )
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
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            CashierTransactionHistoryContent(
            state = uiState,
            viewModel = viewModel,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onReceiptClick = onReceiptClick,
            modifier = Modifier
            )
        }
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

    BoxWithConstraints(modifier.fillMaxSize().background(Color.White)) {
        val compact = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)) {
        // ── Header ──
        CashierHistoryHeader(
            selectedDate = state.selectedDate,
            startDate = state.startDate,
            endDate = state.endDate,
            selectedPreset = state.selectedPreset,
            onPreviousDate = viewModel::previousDate,
            onNextDate = viewModel::nextDate,
            onCalendarClick = { showCalendarPicker = true },
            onDatePresetSelected = viewModel::setDatePreset,
            compact = compact
        )

        // ── Period Info ──
        // ── Search + Date Navigator ──
        val searchField: @Composable (Modifier) -> Unit = { fieldModifier ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CashierLine),
                modifier = fieldModifier.height(52.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = DashboardTextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (state.query.isEmpty()) {
                            Text("Cari struk atau pembeli", color = DashboardTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp)
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
        }
        if (compact) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            searchField(Modifier.fillMaxWidth())
            CashierStatusDropdown(state.statusFilter, viewModel::updateStatusFilter, Modifier.fillMaxWidth())
        } else Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            searchField(Modifier.weight(1f))
            CashierStatusDropdown(state.statusFilter, viewModel::updateStatusFilter, Modifier.width(220.dp))
        }

        // ── Status Chips ──

        // ── Table Card ──
        LocalPendingTransactionsCard(
            transactions = state.localPendingTransactions,
            message = state.localSyncMessage,
            bulkProgressMessage = state.bulkSyncProgressMessage,
            isBulkSyncing = state.isBulkSyncing,
            onSyncClick = viewModel::syncLocalTransaction,
            onSyncAllClick = viewModel::syncAllLocalTransactions
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DashboardSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CashierLine)
        ) {
            Column {
                if (!compact) CashierHistoryTableHeader()
                HorizontalDivider(color = CashierLine)

                when {
                    state.isLoading -> CashierHistorySkeletonRows()
                    state.errorMessage != null -> CashierHistoryMessage(state.errorMessage)
                    !state.hasActiveSession -> CashierHistoryMessage("Belum ada sesi kasir aktif.")
                    state.transactions.isEmpty() -> CashierHistoryMessage("Belum ada transaksi pada sesi ini.")
                    else -> {
                        state.transactions.forEachIndexed { index, transaction ->
                            if (compact) CashierHistoryMobileRow(transaction, onReceiptClick) else CashierHistoryRow(transaction = transaction, onReceiptClick = onReceiptClick)
                            if (index < state.transactions.lastIndex) {
                                HorizontalDivider(color = CashierLine.copy(alpha = 0.75f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = CashierLine)
                CashierHistoryPagination(state, compact, onPreviousPage, onNextPage)
            }
        }
    }
    }

    // ── Calendar Picker Dialog ──
    if (showCalendarPicker) {
        HistoryDatePickerDialog(
            currentDate = state.endDate ?: state.selectedDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
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
// ═══════════════════════════════════════════════
// CALENDAR PICKER DIALOG (single date)
// ═══════════════════════════════════════════════
// ═══════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════
@Composable
private fun CashierHistoryHeader(
    selectedDate: String?,
    startDate: String?,
    endDate: String?,
    selectedPreset: String?,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onCalendarClick: () -> Unit,
    onDatePresetSelected: (String) -> Unit,
    compact: Boolean
) {
    if (compact) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HistoryDateFilter(selectedDate = endDate ?: selectedDate, onPreviousDate = onPreviousDate, onNextDate = onNextDate, onCalendarClick = onCalendarClick, onClearDate = { onDatePresetSelected("Hari ini") }, displayTextOverride = formatHistoryDateRange(startDate, endDate, selectedDate), showClearButton = false, modifier = Modifier.fillMaxWidth())
        CashierDatePresetChips(selectedPreset, onDatePresetSelected, Modifier.fillMaxWidth())
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryDateFilter(
                selectedDate = endDate ?: selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = { onDatePresetSelected("Hari ini") },
                displayTextOverride = formatHistoryDateRange(startDate, endDate, selectedDate),
                showClearButton = false,
                modifier = Modifier.width(320.dp)
            )
            CashierDatePresetChips(
                selectedPreset = selectedPreset,
                onPresetSelected = onDatePresetSelected
            )
        }
    }
}

@Composable
private fun CashierDatePresetChips(
    selectedPreset: String?,
    onPresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CashierLine),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Hari ini", "7 hari", "30 hari").forEach { preset ->
                CashierDatePresetChip(
                    text = preset,
                    selected = selectedPreset == preset,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun CashierDatePresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Color(0xFF86F8C9) else Color.Transparent
    val textColor = if (selected) Color(0xFF00513A) else DashboardTextSecondary

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CashierStatusDropdown(
    selectedStatus: String,
    onStatusChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CashierLine),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = DashboardTextPrimary
            )
        ) {
            Text(
                text = selectedStatus,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "Pilih status pembayaran",
                tint = DashboardTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp).background(Color.White)
        ) {
            listOf("Semua", "Lunas", "DP", "Hutang").forEach { status ->
                DropdownMenuItem(
                    text = { Text(status, color = DashboardTextPrimary) },
                    onClick = {
                        onStatusChanged(status)
                        expanded = false
                    }
                )
            }
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
private fun CashierHistoryMobileRow(
    transaction: CashTransaction,
    onReceiptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.receiptNumber(),
                    color = DashboardTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${transaction.createdAt.displayDate()} · ${transaction.createdAt.displayTime()}",
                    color = DashboardTextSecondary,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(transaction.status.statusColor().copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    transaction.status.uppercase(),
                    color = transaction.status.statusColor(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.customerName ?: "Pelanggan umum", color = DashboardTextSecondary, fontSize = 12.sp)
                Text(transaction.total.moneyText(), color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.Visibility, contentDescription = "Lihat struk", tint = DashboardBrandGreenDark)
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
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    TbPagination(
        currentPage = state.page,
        totalPages = state.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (compact) "${state.currentStart}-${state.currentEnd} dari ${state.total} transaksi"
        else "Menampilkan ${state.currentStart}-${state.currentEnd} dari ${state.total} transaksi",
        isLoading = state.isLoading,
        testTag = "cashier-history-pagination",
    )
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

private fun formatHistoryDateRange(startDate: String?, endDate: String?, fallbackDate: String?): String {
    val start = startDate ?: fallbackDate
    val end = endDate ?: fallbackDate
    return when {
        start.isNullOrBlank() && end.isNullOrBlank() -> LocalDate.now().formatDisplayDateLong()
        start == end -> start.orEmpty().formatDisplayDateLong()
        else -> "${start.orEmpty().formatDisplayDateShort()} - ${end.orEmpty().formatDisplayDateLong()}"
    }
}

private fun LocalDate.formatDisplayDateLong(): String =
    format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))

private fun String.formatDisplayDateShort(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

private fun String.formatDisplayDateLong(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
