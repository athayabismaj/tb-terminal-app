package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── COLOR PALETTE ACCORDING TO HTML DESIGN ──
private val BrandSage = Color(0xFF256B57)       // brand-600
private val BrandSageLight = Color(0xFFF2F8F5)  // brand-50
private val BrandSageBorder = Color(0xFFC7E0D6) // brand-200
private val BrandSageDark = Color(0xFF1D5545)   // brand-700

private val SurfaceBg = Color(0xFFF6F8F6)
private val SurfaceBorder = Color(0xFFE3E8E5)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val EmeraldBg = Color(0xFFECFDF5)
private val EmeraldBorder = Color(0xFFA7F3D0)
private val EmeraldText = Color(0xFF047857)
private val EmeraldDot = Color(0xFF10B981)

private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFFD97706)
private val RoseBg = Color(0xFFFFF1F2)
private val RoseBorder = Color(0xFFFDA4AF)
private val RoseText = Color(0xFFE11D48)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerExecutiveDashboardContent(
    ownerName: String,
    metrics: DashboardMetricsDto?,
    financialTotals: SalesReportTotalsDto?,
    salesReport: SalesReportResponseDto?,
    dailySalesTrend: List<DailySalesSummaryDto>,
    selectedPeriodDays: Int,
    onPeriodSelected: (Int) -> Unit,
    isLoading: Boolean,
    error: String?,
    offlineUiState: OfflineDashboardUiState,
    onTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onCashClick: () -> Unit,
    onStockClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onSyncCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isProfitVisible by remember { mutableStateOf(true) }
    var showPeriodModal by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── 1. HEADER WITH GREETING & STATUS PILL ──
            OwnerHeader(
                ownerName = ownerName,
                selectedDays = selectedPeriodDays,
                onPeriodClick = { showPeriodModal = true }
            )

            // ── ERROR BANNER IF ANY ──
            if (error != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = RoseBg,
                    border = BorderStroke(1.dp, RoseBorder)
                ) {
                    Text(
                        text = "Pemberitahuan: $error",
                        color = RoseText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // ── 2. DASHBOARD BODY ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading && metrics == null) {
                    OwnerDashboardSkeleton()
                } else {
                    // ── 2.1 KEY FINANCIAL METRICS GRID (SINGLE CARD 2x2) ──
                    KeyFinancialMetricsCard(
                        metrics = metrics,
                        financialTotals = financialTotals,
                        salesReport = salesReport,
                        isProfitVisible = isProfitVisible,
                        onToggleProfitVisibility = { isProfitVisible = !isProfitVisible },
                        onTransactionsClick = onTransactionsClick,
                        onReportsClick = onReportsClick,
                        onReceivablesClick = onReceivablesClick,
                        onSupplierDebtsClick = onSupplierDebtsClick
                    )

                    // ── 2.2 PINTASAN LAPORAN & MANAJEMEN ──
                    QuickShortcutsSection(
                        onReportsClick = onReportsClick,
                        onReceivablesClick = onReceivablesClick,
                        onCashClick = onCashClick
                    )

                    // ── 2.3 TREN PENJUALAN HARIAN ──
                    DailySalesTrendCard(
                        dailySalesTrend = dailySalesTrend,
                        financialTotals = financialTotals,
                        onHistoryClick = onTransactionsClick
                    )

                    // ── 2.4 PERHATIAN STOK PRODUK ──
                    StockStatusAlertCard(
                        metrics = metrics,
                        onStockClick = onStockClick
                    )

                    // ── 2.5 SINKRONISASI CLOUD ──
                    CloudSynchronizationCard(
                        offlineUiState = offlineUiState,
                        onSyncCenterClick = onSyncCenterClick
                    )
                }
            }
        }

        // ── 3. PERIOD FILTER MODAL SHEET ──
        if (showPeriodModal) {
            PeriodFilterModalSheet(
                selectedDays = selectedPeriodDays,
                onSelectPeriod = { days ->
                    onPeriodSelected(days)
                    showPeriodModal = false
                },
                onCustomDateClick = {
                    showPeriodModal = false
                    showCustomDatePicker = true
                },
                onDismiss = { showPeriodModal = false }
            )
        }

        // ── 4. CUSTOM DATE PICKER DIALOG ──
        if (showCustomDatePicker) {
            HistoryDatePickerDialog(
                currentDate = null,
                onDismiss = { showCustomDatePicker = false },
                onConfirm = {
                    showCustomDatePicker = false
                    onReportsClick()
                },
                onConfirmRange = { _, _, _ ->
                    showCustomDatePicker = false
                    onReportsClick()
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. MAIN HEADER & PERIOD SELECTOR
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OwnerHeader(
    ownerName: String,
    selectedDays: Int,
    onPeriodClick: () -> Unit
) {
    val displayName = remember(ownerName) {
        val trimmed = ownerName.trim()
        if (trimmed.isEmpty() || trimmed.equals("owner", ignoreCase = true) || trimmed.equals("pemilik", ignoreCase = true)) {
            "Pemilik Toko"
        } else {
            trimmed
        }
    }

    val periodLabel = remember(selectedDays) {
        when (selectedDays) {
            1 -> "Hari Ini"
            7 -> "7 Hari"
            30 -> "Bulan Ini"
            else -> "$selectedDays Hari"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Greeting & Online Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Halo, $displayName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 28.sp
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terminal Barokah Jaya",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Slate500
                        )
                        Text(
                            text = " · ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Text(
                            text = "Kinerja Toko",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Slate500
                        )
                    }
                }

                // Online Status Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = EmeraldBg,
                    border = BorderStroke(1.dp, EmeraldBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(EmeraldDot, CircleShape)
                        )
                        Text(
                            text = "Online",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = EmeraldText
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Period Selector Row: [ 📅 Hari Ini ⌄ ] on left, "Data diperbarui otomatis" on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPeriodClick,
                    shape = RoundedCornerShape(999.dp),
                    color = BrandSageLight,
                    border = BorderStroke(1.dp, BrandSageBorder.copy(alpha = 0.6f)),
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = BrandSage,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = periodLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = BrandSage,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "Data diperbarui otomatis",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate400
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1.1 PERIOD FILTER MODAL SHEET (MATCHES HTML periodModal EXACTLY)
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodFilterModalSheet(
    selectedDays: Int,
    onSelectPeriod: (Int) -> Unit,
    onCustomDateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title + Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandSageLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = BrandSage,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Pilih Rentang Waktu",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.2).sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Option 1: Hari Ini (Real-time)
                PeriodOptionItem(
                    label = "Hari Ini (Real-time)",
                    isSelected = selectedDays == 1,
                    onClick = { onSelectPeriod(1) }
                )

                // Option 2: 7 Hari Terakhir
                PeriodOptionItem(
                    label = "7 Hari Terakhir",
                    isSelected = selectedDays == 7,
                    onClick = { onSelectPeriod(7) }
                )

                // Option 3: Bulan Ini
                PeriodOptionItem(
                    label = "Bulan Ini",
                    isSelected = selectedDays == 30,
                    onClick = { onSelectPeriod(30) }
                )

                // Option 4: Pilih Tanggal / Custom
                Surface(
                    onClick = onCustomDateClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), CircleShape)
                            )
                            Text(
                                text = "Pilih Tanggal / Custom",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandSageLight else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, BrandSageBorder) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BrandSage, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), CircleShape)
                    )
                }

                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) BrandSageDark else Slate700
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = BrandSage,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. FINANCIAL METRICS GRID (SINGLE CARD 2x2 WITH DIVIDERS)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun KeyFinancialMetricsCard(
    metrics: DashboardMetricsDto?,
    financialTotals: SalesReportTotalsDto?,
    salesReport: SalesReportResponseDto?,
    isProfitVisible: Boolean,
    onToggleProfitVisibility: () -> Unit,
    onTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit
) {
    val netRevenue = financialTotals?.netRevenue?.toDouble()
        ?: metrics?.totalRevenueToday
        ?: 0.0
    val grossProfit = financialTotals?.grossProfit?.toDouble()
        ?: 0.0
    val activeReceivables = metrics?.totalActiveReceivables
        ?: salesReport?.receivables?.remainingAmount?.toDouble()
        ?: 0.0
    val receivableCount: Long = metrics?.activeReceivableCount
        ?: salesReport?.receivables?.receivableCount
        ?: 0L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Penjualan Bersih | Estimasi Laba
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Quadrant 1: Penjualan Bersih
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onTransactionsClick)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandSageLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = BrandSage,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(-45f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Column {
                        Text(
                            text = "PENJUALAN BERSIH",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = netRevenue.toRupiah(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Setelah diskon",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(SurfaceBorder)
                )

                // Quadrant 2: Estimasi Laba
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onReportsClick)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandSageLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = BrandSage,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleProfitVisibility,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isProfitVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Sembunyikan / Tampilkan Laba",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Column {
                        Text(
                            text = "ESTIMASI LABA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = if (isProfitVisible) grossProfit.toRupiah() else "••••••",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Gross margin",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Horizontal Divider
            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

            // Row 2: Piutang Pelanggan | Hutang Supplier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Quadrant 3: Piutang Pelanggan
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onReceivablesClick)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandSageLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BrandSage,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(-45f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Column {
                        Text(
                            text = "PIUTANG PELANGGAN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = activeReceivables.toRupiah(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$receivableCount nota belum lunas",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(SurfaceBorder)
                )

                // Quadrant 4: Hutang Supplier
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onSupplierDebtsClick)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandSageLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = BrandSage,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(-45f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Column {
                        Text(
                            text = "HUTANG SUPPLIER",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Periksa Jadwal",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Pantau faktur & jatuh tempo",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. PINTASAN LAPORAN & MANAJEMEN (3 TILES)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuickShortcutsSection(
    onReportsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCashClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PINTASAN LAPORAN & MANAJEMEN",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Shortcut 1: Laba / Rugi
            QuickShortcutCard(
                title = "Laba / Rugi",
                subtitle = "Finansial",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = onReportsClick,
                modifier = Modifier.weight(1f)
            )

            // Shortcut 2: Buku Piutang
            QuickShortcutCard(
                title = "Buku Piutang",
                subtitle = "Debitur",
                icon = Icons.Outlined.CalendarMonth,
                onClick = onReceivablesClick,
                modifier = Modifier.weight(1f)
            )

            // Shortcut 3: Rekonsiliasi
            QuickShortcutCard(
                title = "Rekonsiliasi",
                subtitle = "Arus Kas",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                onClick = onCashClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandSageLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandSage,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate900,
                lineHeight = 16.sp
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. TREN PENJUALAN HARIAN
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun DailySalesTrendCard(
    dailySalesTrend: List<DailySalesSummaryDto>,
    financialTotals: SalesReportTotalsDto?,
    onHistoryClick: () -> Unit
) {
    val totalRevenueInPeriod = remember(dailySalesTrend, financialTotals) {
        if (dailySalesTrend.isNotEmpty()) {
            dailySalesTrend.sumOf { it.totalRevenue }
        } else {
            financialTotals?.netRevenue?.toDouble() ?: 0.0
        }
    }

    val maxDailyRevenue = remember(dailySalesTrend) {
        val maxVal = dailySalesTrend.maxOfOrNull { it.totalRevenue } ?: 0.0
        if (maxVal <= 0.0) 1.0 else maxVal
    }

    val hasData = dailySalesTrend.any { it.totalRevenue > 0.0 }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tren Penjualan Harian",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BrandSage, CircleShape)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Slate100
                ) {
                    Text(
                        text = "Total: ${totalRevenueInPeriod.toRupiah()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Timeline Bars Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (hasData) {
                        val displayItems = if (dailySalesTrend.size > 5) dailySalesTrend.takeLast(5) else dailySalesTrend
                        displayItems.forEach { daySummary ->
                            val ratio = (daySummary.totalRevenue / maxDailyRevenue).coerceIn(0.12, 1.0).toFloat()
                            val barHeight = (42 * ratio).dp

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (daySummary.totalRevenue > 0) BrandSage else Slate100)
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = formatShortDay(daySummary.date),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate400,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        // Clean timeline skeleton matching user mockup: 08:00, 11:00, 14:00, 17:00, 20:00
                        val skeletonBars = listOf(
                            24.dp to "08:00",
                            32.dp to "11:00",
                            20.dp to "14:00",
                            40.dp to "17:00",
                            16.dp to "20:00"
                        )
                        skeletonBars.forEach { (height, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(height),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate100)
                                ) {}

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Footer status and History CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasData) "Menampilkan tren omzet transaksi" else "Belum ada transaksi di periode ini",
                    fontSize = 11.5.sp,
                    color = Slate400
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onHistoryClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Riwayat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandSage
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Buka Riwayat",
                        tint = BrandSage,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. PERHATIAN STOK PRODUK (STOCK STATUS ALERT)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun StockStatusAlertCard(
    metrics: DashboardMetricsDto?,
    onStockClick: () -> Unit
) {
    val lowStockCount = metrics?.lowStockCount ?: 0L

    Surface(
        onClick = onStockClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Perhatian Stok Produk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = if (lowStockCount > 0) "$lowStockCount produk di bawah batas minimum" else "Semua persediaan berada di atas batas minimum",
                        fontSize = 11.sp,
                        color = Slate500,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (lowStockCount > 0) AmberBg else BrandSageLight,
                border = BorderStroke(1.dp, if (lowStockCount > 0) AmberBorder else BrandSageBorder.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (lowStockCount > 0) "$lowStockCount Perlu Restock" else "Stok Aman",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lowStockCount > 0) AmberText else BrandSageDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. SINKRONISASI CLOUD (CLOUD SYNCHRONIZATION)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CloudSynchronizationCard(
    offlineUiState: OfflineDashboardUiState,
    onSyncCenterClick: () -> Unit
) {
    val lastRefreshText = remember(offlineUiState.lastRefresh) {
        formatTime(offlineUiState.lastRefresh)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandSageLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = BrandSage,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Sinkronisasi Cloud",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Sinkron terakhir: $lastRefreshText",
                            fontSize = 10.5.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }

                Surface(
                    onClick = onSyncCenterClick,
                    shape = RoundedCornerShape(8.dp),
                    color = BrandSage,
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Sinkronkan Sekarang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            HorizontalDivider(
                color = SurfaceBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Metrics List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SyncMetricRow(
                    label = "Data Lokal Tersimpan",
                    value = "${offlineUiState.localTransactionCount} nota",
                    dotColor = EmeraldDot
                )
                SyncMetricRow(
                    label = "Antrean Menunggu Kirim",
                    value = "${offlineUiState.pendingSyncCount} antrean",
                    dotColor = if (offlineUiState.pendingSyncCount > 0) AmberText else Color(0xFFCBD5E1)
                )
                SyncMetricRow(
                    label = "Gagal Kirim Server",
                    value = "${offlineUiState.failedSyncCount} nota",
                    dotColor = if (offlineUiState.failedSyncCount > 0) RoseText else Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun SyncMetricRow(
    label: String,
    value: String,
    dotColor: Color
) {
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
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600
            )
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Slate100
        ) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = Slate700,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SKELETON LOADING (MATCHES THE EXACT LAYOUT)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OwnerDashboardSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 2x2 Metric Grid Skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricQuadrantSkeleton(Modifier.weight(1f))
                    Box(modifier = Modifier.width(1.dp).height(120.dp).background(SurfaceBorder))
                    MetricQuadrantSkeleton(Modifier.weight(1f))
                }
                HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricQuadrantSkeleton(Modifier.weight(1f))
                    Box(modifier = Modifier.width(1.dp).height(120.dp).background(SurfaceBorder))
                    MetricQuadrantSkeleton(Modifier.weight(1f))
                }
            }
        }

        // Shortcuts skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBox(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
                        SkeletonBox(Modifier.width(60.dp).height(12.dp))
                        SkeletonBox(Modifier.width(40.dp).height(10.dp))
                    }
                }
            }
        }

        // Trend card skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(Modifier.width(140.dp).height(14.dp))
                    SkeletonBox(Modifier.width(80.dp).height(18.dp).clip(RoundedCornerShape(999.dp)))
                }
                SkeletonBox(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(4.dp)))
            }
        }

        // Alert card skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonBox(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(130.dp).height(12.dp))
                        SkeletonBox(Modifier.width(170.dp).height(10.dp))
                    }
                }
                SkeletonBox(Modifier.width(70.dp).height(22.dp).clip(RoundedCornerShape(999.dp)))
            }
        }

        // Cloud sync skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonBox(Modifier.width(110.dp).height(12.dp))
                            SkeletonBox(Modifier.width(90.dp).height(10.dp))
                        }
                    }
                    SkeletonBox(Modifier.width(120.dp).height(28.dp).clip(RoundedCornerShape(8.dp)))
                }
                repeat(3) {
                    SkeletonBox(Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricQuadrantSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
            SkeletonBox(Modifier.size(14.dp).clip(CircleShape))
        }
        Spacer(Modifier.height(4.dp))
        SkeletonBox(Modifier.width(80.dp).height(10.dp))
        SkeletonBox(Modifier.fillMaxWidth(0.7f).height(20.dp))
        SkeletonBox(Modifier.width(60.dp).height(10.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// FORMATTERS
// ═════════════════════════════════════════════════════════════════════════════
private fun formatTime(millis: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(millis)
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        instant.atZone(zone).format(formatter)
    } catch (_: Exception) {
        "12:51"
    }
}

private fun formatShortDay(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val day = parts[2].toIntOrNull() ?: parts[2]
            val month = when (parts[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "Mei"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Agt"
                "09" -> "Sep"
                "10" -> "Okt"
                "11" -> "Nov"
                "12" -> "Des"
                else -> parts[1]
            }
            "$day $month"
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

private fun Double.toRupiah(): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "Rp " + formatter.format(this)
}
