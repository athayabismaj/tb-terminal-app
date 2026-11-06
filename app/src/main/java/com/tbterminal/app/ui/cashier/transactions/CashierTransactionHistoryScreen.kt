package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.components.HistoryDateFilter
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.offline.LocalPendingTransactionsCard
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Forest900 = Color(0xFF10533C)
private val Forest800 = Color(0xFF166534)
private val Forest50 = Color(0xFFF0FDF4)
private val SurfaceCanvas = Color(0xFFF8FAF9)
private val SurfaceCard = Color.White
private val CardBorder = Color(0xFFE2E8F0).copy(alpha = 0.8f)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val Emerald700 = Color(0xFF047857)
private val Emerald50 = Color(0xFFECFDF5)
private val EmeraldBorder = Color(0xFFA7F3D0)

private val Amber700 = Color(0xFFB45309)
private val Amber50 = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)

private val Rose700 = Color(0xFFB91C1C)
private val Rose50 = Color(0xFFFEF2F2)
private val RoseBorder = Color(0xFFFECACA)

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
        Box(modifier = contentModifier.fillMaxSize()) {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
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
            TopFloatingCapsule(
                message = uiState.receiptMessage,
                error = uiState.errorMessage,
                onDismiss = viewModel::clearFeedback,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
    ) {
        val compact = maxWidth < 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = if (compact) 600.dp else 1024.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Section 1: Period & Date Navigator ──
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

            // ── Section 2: Summary KPI Card (Standard Industri POS) ──
            CashierTransactionSummaryCard(state = state)

            // ── Section 3: Search Bar & Status Filter Chips ──
            CashierSearchBarAndFilter(
                query = state.query,
                onQueryChange = viewModel::updateSearchQuery,
                selectedStatus = state.statusFilter,
                onStatusChange = viewModel::updateStatusFilter,
                compact = compact
            )

            // ── Section 4: Offline Pending Sync (if any) ──
            if (state.localPendingTransactions.isNotEmpty()) {
                LocalPendingTransactionsCard(
                    transactions = state.localPendingTransactions,
                    message = state.localSyncMessage,
                    bulkProgressMessage = state.bulkSyncProgressMessage,
                    isBulkSyncing = state.isBulkSyncing,
                    onSyncClick = viewModel::syncLocalTransaction,
                    onSyncAllClick = viewModel::syncAllLocalTransactions
                )
            }

            // ── Section 5: Transactions List Card ──
            CashierTransactionsCard(
                state = state,
                compact = compact,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                onReceiptClick = onReceiptClick,
                onResetFilters = viewModel::resetFilters
            )
        }
    }

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
// SECTION 1: DATE NAVIGATOR & PRESET PILLS
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
    val dateRangeText = remember(startDate, endDate, selectedDate) {
        formatHistoryDateRange(startDate, endDate, selectedDate)
    }

    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HistoryDateFilter(
                selectedDate = endDate ?: selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = { onDatePresetSelected("Hari ini") },
                displayTextOverride = dateRangeText,
                showClearButton = false,
                modifier = Modifier.fillMaxWidth()
            )
            CashierDatePresetChips(
                selectedPreset = selectedPreset,
                onPresetSelected = onDatePresetSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryDateFilter(
                selectedDate = endDate ?: selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = { onDatePresetSelected("Hari ini") },
                displayTextOverride = dateRangeText,
                showClearButton = false,
                modifier = Modifier.weight(1f)
            )
            CashierDatePresetChips(
                selectedPreset = selectedPreset,
                onPresetSelected = onDatePresetSelected,
                modifier = Modifier.widthIn(min = 240.dp)
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
        color = SurfaceCard,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Hari ini", "7 hari", "30 hari").forEach { preset ->
                val isSelected = selectedPreset == preset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Forest900 else Color.Transparent)
                        .clickable { onPresetSelected(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset,
                        color = if (isSelected) Color.White else Slate600,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// SECTION 2: SUMMARY KPI CARD (Standard Industri POS)
// ═══════════════════════════════════════════════
@Composable
private fun CashierTransactionSummaryCard(
    state: CashierTransactionHistoryUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Judul & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Forest50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = Forest900,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Ringkasan Transaksi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (state.hasActiveSession) Emerald50 else Slate100,
                    border = BorderStroke(1.dp, if (state.hasActiveSession) EmeraldBorder else Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.hasActiveSession) Emerald700 else Slate400)
                        )
                        Text(
                            text = if (state.hasActiveSession) "Sesi Aktif" else (state.selectedPreset ?: "Selesai"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.hasActiveSession) Emerald700 else Slate600
                        )
                    }
                }
            }

            // Hero Metric: Total Penjualan Omset
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "TOTAL OMSET PENJUALAN",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = state.summaryTotalSales.cleanMoney(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Akumulasi dari ${state.summaryTransactionCount} transaksi tercatat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            // 4-Grid Divided Clean Metric Box
            val averageTicket = remember(state.summaryTotalSales, state.summaryTransactionCount) {
                if (state.summaryTransactionCount > 0) {
                    state.summaryTotalSales.divide(BigDecimal(state.summaryTransactionCount), 0, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Lunas & Sisa Hutang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        MetricGridCell(
                            modifier = Modifier.weight(1f),
                            label = "Lunas",
                            value = state.summaryTotalPaid.cleanMoney(),
                            subtext = "${state.summaryPaidCount} Trx",
                            indicatorColor = Emerald700
                        )
                        VerticalDivider(
                            modifier = Modifier.height(44.dp),
                            color = Slate200
                        )
                        MetricGridCell(
                            modifier = Modifier.weight(1f),
                            label = "Sisa Hutang",
                            value = state.summaryTotalRemaining.cleanMoney(),
                            subtext = "${state.summaryDebtCount} Trx",
                            indicatorColor = if (state.summaryTotalRemaining > BigDecimal.ZERO) Amber700 else Slate400
                        )
                    }

                    HorizontalDivider(color = Slate200)

                    // Row 2: Total Trx & Rata-rata Struk
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        MetricGridCell(
                            modifier = Modifier.weight(1f),
                            label = "Total Transaksi",
                            value = "${state.summaryTransactionCount} Trx",
                            subtext = if (state.summaryTransactionCount > 0) "100% Tercatat" else "-",
                            indicatorColor = Forest900
                        )
                        VerticalDivider(
                            modifier = Modifier.height(44.dp),
                            color = Slate200
                        )
                        MetricGridCell(
                            modifier = Modifier.weight(1f),
                            label = "Rata-rata Struk",
                            value = averageTicket.cleanMoney(),
                            subtext = "Per transaksi",
                            indicatorColor = Slate600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricGridCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtext: String,
    indicatorColor: Color
) {
    Column(
        modifier = modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtext,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = Slate400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════
// SECTION 3: SEARCH BAR & STATUS FILTER PILLS
// ═══════════════════════════════════════════════
@Composable
private fun CashierSearchBarAndFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    compact: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari no. struk atau pelanggan...",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = Slate900,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Status Segmented Filter Pills
        val statusList = listOf("Semua", "Lunas", "DP", "Hutang")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusList.forEach { status ->
                val isSelected = selectedStatus.equals(status, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) Forest900 else SurfaceCard,
                    border = BorderStroke(1.dp, if (isSelected) Forest900 else CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable { onStatusChange(status) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status,
                            color = if (isSelected) Color.White else Slate600,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// SECTION 5: TRANSACTIONS LIST CARD
// ═══════════════════════════════════════════════
@Composable
private fun CashierTransactionsCard(
    state: CashierTransactionHistoryUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onResetFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Transaksi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Forest50,
                    border = BorderStroke(1.dp, Forest900.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${state.total} Transaksi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest900,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            when {
                state.isLoading -> {
                    CashierHistorySkeletonRows()
                }

                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.errorMessage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Rose700,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onResetFilters,
                                colors = ButtonDefaults.buttonColors(containerColor = Forest900),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Muat Ulang Data", fontSize = 12.sp)
                            }
                        }
                    }
                }

                state.transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Tidak Ada Transaksi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Belum ada transaksi yang sesuai pada filter atau tanggal ini.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                            if (state.query.isNotEmpty() || state.statusFilter != "Semua") {
                                OutlinedButton(
                                    onClick = onResetFilters,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text("Atur Ulang Filter", fontSize = 12.sp, color = Forest900, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (!compact) {
                        CashierHistoryTableHeader()
                        HorizontalDivider(color = Slate100)
                    }

                    state.transactions.forEachIndexed { index, transaction ->
                        if (compact) {
                            CashierHistoryMobileRow(
                                transaction = transaction,
                                onReceiptClick = onReceiptClick
                            )
                        } else {
                            CashierHistoryRow(
                                transaction = transaction,
                                onReceiptClick = onReceiptClick
                            )
                        }
                        if (index < state.transactions.lastIndex) {
                            HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            // Pagination Controls
            CashierHistoryPagination(
                state = state,
                compact = compact,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage
            )
        }
    }
}

// ═══════════════════════════════════════════════
// COMPACT / MOBILE ROW (Standar Industri Modern)
// ═══════════════════════════════════════════════
@Composable
private fun CashierHistoryMobileRow(
    transaction: CashTransaction,
    onReceiptClick: (String) -> Unit
) {
    val statusClean = transaction.status.lowercase()
    val isPaid = statusClean in listOf("lunas", "paid", "success", "completed")
    val isPartial = statusClean in listOf("dp", "partial")
    val isUnpaid = statusClean in listOf("hutang", "unpaid")

    val (badgeBg, badgeText, badgeBorder) = when {
        isPaid -> Triple(Emerald50, Emerald700, EmeraldBorder)
        isPartial -> Triple(Amber50, Amber700, AmberBorder)
        isUnpaid -> Triple(Rose50, Rose700, RoseBorder)
        else -> Triple(Slate100, Slate600, Slate200)
    }

    val customerName = transaction.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan Umum"
    val paymentLabel = transaction.paymentMethods.firstOrNull()?.uppercase() ?: "TUNAI"
    val remaining = transaction.remainingAmount ?: transaction.total.subtract(transaction.paidAmount).max(BigDecimal.ZERO)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Baris 1: Monospace ID Struk & Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = transaction.receiptId,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = if (isPaid) "LUNAS" else transaction.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
                }
            }

            // Baris 2: Pelanggan · Waktu Lengkap · Metode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = customerName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = "·", fontSize = 11.sp, color = Slate400)
                Text(
                    text = transaction.createdAt.formatTransactionFullDate(),
                    fontSize = 11.sp,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = "·", fontSize = 11.sp, color = Slate400)
                Text(
                    text = paymentLabel,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate600
                )
            }
        }

        // Sisi Kanan: Total Nominal & Sisa Hutang jika ada
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.total.cleanMoney(),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                if (remaining > BigDecimal.ZERO && !isPaid) {
                    Text(
                        text = "Sisa: ${remaining.cleanMoney()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber700
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════
// DESKTOP / TABLET TABLE VIEW
// ═══════════════════════════════════════════════
@Composable
private fun CashierHistoryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CashierHeaderText("WAKTU", Modifier.weight(1.3f))
        CashierHeaderText("NO STRUK", Modifier.weight(1.5f))
        CashierHeaderText("PELANGGAN", Modifier.weight(1.3f))
        CashierHeaderText("STATUS", Modifier.weight(1f))
        CashierHeaderText("METODE", Modifier.weight(0.9f))
        CashierHeaderText("TOTAL", Modifier.weight(1.3f), Alignment.End)
        CashierHeaderText("AKSI", Modifier.weight(0.6f), Alignment.End)
    }
}

@Composable
private fun CashierHistoryRow(
    transaction: CashTransaction,
    onReceiptClick: (String) -> Unit
) {
    val statusClean = transaction.status.lowercase()
    val isPaid = statusClean in listOf("lunas", "paid", "success", "completed")
    val isPartial = statusClean in listOf("dp", "partial")
    val isUnpaid = statusClean in listOf("hutang", "unpaid")

    val (badgeBg, badgeText, badgeBorder) = when {
        isPaid -> Triple(Emerald50, Emerald700, EmeraldBorder)
        isPartial -> Triple(Amber50, Amber700, AmberBorder)
        isUnpaid -> Triple(Rose50, Rose700, RoseBorder)
        else -> Triple(Slate100, Slate600, Slate200)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = transaction.createdAt.formatTransactionTime(),
                color = Slate900,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
            Text(
                text = transaction.createdAt.formatTransactionDateShort(),
                color = Slate400,
                fontSize = 11.sp
            )
        }
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = transaction.receiptId,
                color = Slate900,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = transaction.customerName?.ifBlank { "Pelanggan Umum" } ?: "Pelanggan Umum",
                color = Slate800,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = badgeBg,
                border = BorderStroke(1.dp, badgeBorder)
            ) {
                Text(
                    text = if (isPaid) "LUNAS" else transaction.status.uppercase(),
                    color = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = transaction.paymentMethods.firstOrNull()?.uppercase() ?: "TUNAI",
                color = Slate600,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(
            modifier = Modifier.weight(1.3f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = transaction.total.cleanMoney(),
                color = Slate900,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                textAlign = TextAlign.End
            )
            val remaining = transaction.remainingAmount ?: transaction.total.subtract(transaction.paidAmount).max(BigDecimal.ZERO)
            if (remaining > BigDecimal.ZERO && !isPaid) {
                Text(
                    text = "Sisa: ${remaining.cleanMoney()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber700
                )
            }
        }
        Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.CenterEnd) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = "Detail struk",
                tint = Slate400,
                modifier = Modifier.size(18.dp)
            )
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
        Text(
            text = text,
            color = Slate400,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun CashierHistorySkeletonRows() {
    Column {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Box(modifier = Modifier.size(40.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(Slate200))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.size(60.dp, 10.dp).clip(RoundedCornerShape(4.dp)).background(Slate200))
                }
                Column(modifier = Modifier.weight(1.5f)) {
                    Box(modifier = Modifier.size(110.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(Slate200))
                }
                Column(modifier = Modifier.weight(1.3f)) {
                    Box(modifier = Modifier.size(80.dp, 12.dp).clip(RoundedCornerShape(4.dp)).background(Slate200))
                }
                Box(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(54.dp, 20.dp).clip(RoundedCornerShape(10.dp)).background(Slate200))
                }
                Box(modifier = Modifier.weight(1.3f), contentAlignment = Alignment.CenterEnd) {
                    Box(modifier = Modifier.size(80.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(Slate200))
                }
                Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.CenterEnd) {
                    Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Slate200))
                }
            }
            HorizontalDivider(color = Slate100)
        }
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

internal fun BigDecimal?.cleanMoney(): String {
    if (this == null) return "Rp 0"
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp ${format.format(this.setScale(0, RoundingMode.HALF_UP))}"
}

internal fun BigDecimal.moneyText(): String = cleanMoney()

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

private fun parseToWib(raw: String): ZonedDateTime? {
    val trimmed = raw.trim()
    return runCatching { OffsetDateTime.parse(trimmed).atZoneSameInstant(WIB_ZONE) }.getOrNull()
        ?: runCatching { Instant.parse(trimmed).atZone(WIB_ZONE) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(trimmed).atZone(WIB_ZONE) }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed.take(10)).atStartOfDay(WIB_ZONE) }.getOrNull()
}

internal fun String?.formatTransactionFullDate(): String {
    if (this.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = parseToWib(this) ?: return this
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID")).format(parsed)
    }.getOrDefault(this)
}

internal fun String?.formatTransactionTime(): String {
    if (this.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = parseToWib(this) ?: return this
        DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID")).format(parsed)
    }.getOrDefault("-")
}

internal fun String?.formatTransactionDateShort(): String {
    if (this.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = parseToWib(this) ?: return this
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")).format(parsed)
    }.getOrDefault("-")
}

internal fun String.displayDateTime(): String = formatTransactionFullDate()
internal fun String.displayDate(): String = formatTransactionDateShort()
internal fun String.displayTime(): String = formatTransactionTime()

internal fun String.statusColor(): Color = when (lowercase()) {
    "lunas", "paid", "success", "completed" -> Emerald700
    "dp", "partial" -> Amber700
    "hutang", "unpaid" -> Rose700
    else -> Slate500
}

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
    format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")))

private fun String.formatDisplayDateShort(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

private fun String.formatDisplayDateLong(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
