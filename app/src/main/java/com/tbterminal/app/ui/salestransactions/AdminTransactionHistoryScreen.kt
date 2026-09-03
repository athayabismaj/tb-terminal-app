package com.tbterminal.app.ui.salestransactions

import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.components.HistoryDateFilter
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange

import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.offline.LocalPendingTransactionsCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val CashierLine = Color(0xFFE2E8F0)
private val CashierSurfaceSoft = Color(0xFFF1F5F9)
private val DateBarBg = Color(0xFF1E293B)

@Composable
fun AdminTransactionHistoryScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    transactionDao: TransactionDao? = null,
    offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReceiptClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminTransactionHistoryViewModel = viewModel(
        factory = AdminTransactionHistoryViewModel.factory(
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

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SalesTransactions,
        pageTitle = "Riwayat Penjualan",
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onSalesTransactionsClick = onSalesTransactionsClick,
        onReportsClick = onReportsClick,
        onPriceManagementClick = onPriceManagementClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
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
    state: AdminTransactionHistoryUiState,
    viewModel: AdminTransactionHistoryViewModel,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReceiptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendarPicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize().background(DashboardBackground)) {
        val compact = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)) {
        // ── Header ──
        CashierHistoryHeader(
            state = state,
            onPreviousDate = viewModel::previousDate,
            onNextDate = viewModel::nextDate,
            onCalendarClick = { showCalendarPicker = true },
            onClearDate = { viewModel.setDate(null) },
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
                            Text("Cari struk, pembeli, atau kasir", color = DashboardTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp)
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
        if (compact) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                searchField(Modifier.fillMaxWidth())
                TransactionStatusDropdown(state.statusFilter, viewModel::updateStatusFilter, Modifier.fillMaxWidth())
                TransactionFilterDropdown(state.paymentMethodFilter, listOf("Semua", "tunai", "transfer", "qris", "hutang", "dp"), viewModel::updatePaymentMethodFilter, Modifier.fillMaxWidth())
            }
        } else Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            searchField(Modifier.weight(1.5f))
            TransactionStatusDropdown(state.statusFilter, viewModel::updateStatusFilter, Modifier.width(220.dp))
            TransactionFilterDropdown(state.paymentMethodFilter, listOf("Semua", "tunai", "transfer", "qris", "hutang", "dp"), viewModel::updatePaymentMethodFilter, Modifier.width(180.dp))
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
                    state.error != null -> CashierHistoryMessage(state.error)
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
            currentDate = state.endDate ?: state.selectedDate,
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
    val displayText = selectedDate?.formatDisplayDate() ?: "Hari ini"
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
private fun CashierHistoryHeader(
    state: AdminTransactionHistoryUiState,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onCalendarClick: () -> Unit,
    onClearDate: () -> Unit,
    onDatePresetSelected: (String) -> Unit,
    compact: Boolean
) {
    if (compact) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HistoryDateFilter(selectedDate = state.endDate ?: state.selectedDate, onPreviousDate = onPreviousDate, onNextDate = onNextDate, onCalendarClick = onCalendarClick, onClearDate = onClearDate, displayTextOverride = state.dateRangeLabel(), modifier = Modifier.fillMaxWidth())
        TransactionDatePresets(state.selectedPreset, onDatePresetSelected, Modifier.fillMaxWidth())
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryDateFilter(
                selectedDate = state.endDate ?: state.selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = onClearDate,
                displayTextOverride = state.dateRangeLabel(),
                modifier = Modifier.width(320.dp)
            )
            TransactionDatePresets(
                selectedPreset = state.selectedPreset,
                onSelected = onDatePresetSelected
            )
        }
    }
}

// ═══════════════════════════════════════════════
// TABLE HEADER, ROW, SKELETON, PAGINATION
// ═══════════════════════════════════════════════
@Composable
private fun TransactionDatePresets(selectedPreset: String?, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
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
            listOf("Hari ini", "Minggu ini", "Bulan ini").forEach { preset ->
                Surface(
                    onClick = { onSelected(preset) },
                    color = if (selectedPreset == preset) Color(0xFF86F8C9) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            preset,
                            color = if (selectedPreset == preset) Color(0xFF00513A) else DashboardTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionStatusDropdown(
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
                text = selectedStatus.statusFilterLabel(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = "Pilih status pembayaran",
                tint = DashboardTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .widthIn(min = 220.dp)
                .background(Color.White)
        ) {
            listOf("Semua", "Lunas", "DP", "Hutang", "Voided").forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.statusFilterLabel(), color = DashboardTextPrimary) },
                    onClick = {
                        onStatusChanged(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionFilterDropdown(
    selected: String,
    choices: List<String>,
    onChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = modifier.height(48.dp)) {
            Text(if (selected == "Semua") "Semua metode" else selected.uppercase(), modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { value ->
                DropdownMenuItem(text = { Text(if (value == "Semua") "Semua metode" else value.uppercase()) }, onClick = {
                    expanded = false
                    onChanged(value)
                })
            }
        }
    }
}

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
private fun CashierHistoryMobileRow(transaction: CashTransaction, onReceiptClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onReceiptClick(transaction.id) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(transaction.receiptNumber(), color = DashboardTextPrimary, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${transaction.createdAt.displayDate()} · ${transaction.createdAt.displayTime()}", color = DashboardTextSecondary, fontSize = 11.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(transaction.status.statusColor().copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(transaction.status.uppercase(), color = transaction.status.statusColor(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(transaction.customerName ?: "Pelanggan umum", color = DashboardTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Total", color = DashboardTextSecondary, fontSize = 10.sp); Text(transaction.total.moneyText(), color = DashboardTextPrimary, fontWeight = FontWeight.Bold) }
            Column(horizontalAlignment = Alignment.End) { Text("Dibayar", color = DashboardTextSecondary, fontSize = 10.sp); Text(transaction.paidAmount.moneyText(), color = DashboardTextSecondary, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.Visibility, "Detail struk", tint = DashboardTextSecondary)
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
    state: AdminTransactionHistoryUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val controls: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onPreviousPage, enabled = state.page > 1, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CashierLine)) { Icon(Icons.Default.ChevronLeft, null) }
            Text("${state.page} / ${state.totalPages}", color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
            OutlinedButton(onNextPage, enabled = state.page < state.totalPages, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CashierLine)) { Icon(Icons.Default.ChevronRight, null) }
        }
    }
    if (compact) Column(Modifier.fillMaxWidth().background(CashierSurfaceSoft).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${state.total} transaksi", color = DashboardTextSecondary, fontSize = 12.sp)
        controls()
    } else Row(Modifier.fillMaxWidth().background(CashierSurfaceSoft).padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Menampilkan ${state.currentStart}-${state.currentEnd} dari ${state.total} transaksi", color = DashboardTextSecondary, fontWeight = FontWeight.Bold)
        controls()
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
    "voided" -> Color(0xFF7F1D1D)
    else -> DashboardTextSecondary
}

private fun String.statusFilterLabel(): String = when (this) {
    "Semua" -> "Semua pembayaran"
    "Lunas" -> "Lunas"
    "DP" -> "DP"
    "Hutang" -> "Hutang"
    "Voided" -> "Dibatalkan"
    else -> this
}

private fun AdminTransactionHistoryUiState.dateRangeLabel(): String? {
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

/** Format "yyyy-MM-dd" -> "dd/MM/yyyy" for display */
internal fun String.formatDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)

