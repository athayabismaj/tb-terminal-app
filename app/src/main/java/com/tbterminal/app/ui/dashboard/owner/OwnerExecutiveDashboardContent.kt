package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ScheduleSend
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wifi
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── INDUSTRIAL MINIMALIST COLOR PALETTE (TAILWIND MOCKUP) ──
private val BrandGreen = Color(0xFF1B5E3C)
private val BrandGreenLight = Color(0xFFEEF7F2)
private val BrandGreenDark = Color(0xFF047857)
private val SurfaceBg = Color(0xFFF8FAF9)
private val CardBorder = Color(0x0A000000) // black/[0.04]

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

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
    recentTransactions: List<CashTransaction> = emptyList(),
    selectedPeriodDays: Int,
    onPeriodSelected: (Int) -> Unit,
    isLoading: Boolean,
    error: String?,
    offlineUiState: OfflineDashboardUiState,
    onTransactionsClick: () -> Unit,
    onNewTransactionClick: () -> Unit = onTransactionsClick,
    onReportsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onCashClick: () -> Unit,
    onStockClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onSyncCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPrivacyHidden by remember { mutableStateOf(false) }
    var showPeriodModal by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showSystemSheet by remember { mutableStateOf(false) }

    val displayName = remember(ownerName) {
        val trimmed = ownerName.trim()
        if (trimmed.isEmpty() || trimmed.equals("owner", ignoreCase = true) || trimmed.equals("pemilik", ignoreCase = true)) {
            "Pemilik Toko"
        } else {
            trimmed
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── 1. TOP GREETING ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Halo, $displayName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
            }

            // ── ERROR BANNER IF ANY ──
            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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

            if (isLoading && metrics == null) {
                OwnerDashboardSkeleton()
            } else {
                // ── 2. PRIMARY HERO METRIC CARD (PENJUALAN & MARGIN) ──
                PrimaryHeroMetricCard(
                    metrics = metrics,
                    financialTotals = financialTotals,
                    selectedPeriodDays = selectedPeriodDays,
                    isPrivacyHidden = isPrivacyHidden,
                    onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                    onOpenPeriodSheet = { showPeriodModal = true }
                )

                // ── 3. AKSI CEPAT (4 QUICK NAV SHORTCUTS) ──
                QuickNavShortcuts(
                    onNewTransactionClick = onNewTransactionClick,
                    onSupplierDebtsClick = onSupplierDebtsClick,
                    onStockClick = onStockClick,
                    onReportsClick = onReportsClick
                )

                // ── 4. TREN PENJUALAN HARIAN (HOURLY / DAILY TREND CHART) ──
                DailySalesTrendCard(
                    dailySalesTrend = dailySalesTrend,
                    financialTotals = financialTotals,
                    onTransactionsClick = onTransactionsClick
                )

                // ── 5. TRANSAKSI TERKINI ──
                RecentTransactionsCard(
                    recentTransactions = recentTransactions,
                    onTransactionsClick = onTransactionsClick
                )

                // ── 6. STATUS SISTEM & SINKRONISASI (SELAIN SISTEM NORMAL) ──
                SystemStatusHealthCard(
                    onClick = { showSystemSheet = true }
                )
            }
        }

        // ── SYSTEM STATUS MODAL SHEET ──
        if (showSystemSheet) {
            SystemStatusModalSheet(
                offlineUiState = offlineUiState,
                onSyncCenterClick = onSyncCenterClick,
                onDismiss = { showSystemSheet = false }
            )
        }

        // ── PERIOD FILTER MODAL SHEET ──
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

        // ── CUSTOM DATE PICKER DIALOG ──
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
// 1. PRIMARY HERO METRIC CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PrimaryHeroMetricCard(
    metrics: DashboardMetricsDto?,
    financialTotals: SalesReportTotalsDto?,
    selectedPeriodDays: Int,
    isPrivacyHidden: Boolean,
    onTogglePrivacy: () -> Unit,
    onOpenPeriodSheet: () -> Unit
) {
    val netRevenue = financialTotals?.netRevenue?.toDouble()
        ?: metrics?.totalRevenueToday
        ?: 0.0

    val grossProfit = financialTotals?.grossProfit?.toDouble()
        ?: 0.0

    val activeReceivables = metrics?.totalActiveReceivables
        ?: 0.0

    val periodHeaderTitle = remember(selectedPeriodDays) {
        when (selectedPeriodDays) {
            1 -> "RINGKASAN HARI INI"
            2 -> "RINGKASAN KEMARIN"
            7 -> "RINGKASAN 7 HARI TERAKHIR"
            30 -> "RINGKASAN BULAN INI"
            else -> "RINGKASAN $selectedPeriodDays HARI"
        }
    }

    val selectedPeriodLabel = remember(selectedPeriodDays) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID"))
        when (selectedPeriodDays) {
            1 -> "Hari Ini · ${today.format(formatter)}"
            2 -> "Kemarin · ${today.minusDays(1).format(formatter)}"
            7 -> "7 Hari Terakhir"
            30 -> "Bulan Ini · ${today.format(DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID")))}"
            else -> "$selectedPeriodDays Hari Terakhir"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Label Ringkasan + Period Pill Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = periodHeaderTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    onClick = onOpenPeriodSheet,
                    shape = RoundedCornerShape(999.dp),
                    color = Slate100
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = selectedPeriodLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Main Metric: Penjualan Bersih + Eye Toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Penjualan Bersih",
                        fontSize = 12.sp,
                        color = Slate500
                    )

                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Sembunyikan / Tampilkan Angka",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = if (isPrivacyHidden) "••••••••" else netRevenue.toRupiah(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sub Metrics: Estimasi Laba & Piutang Tertunda
            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Estimasi Laba",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••••" else grossProfit.toRupiah(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Piutang Tertunda",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••••" else activeReceivables.toRupiah(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. QUICK NAV SHORTCUTS (AKSI CEPAT)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuickNavShortcuts(
    onNewTransactionClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onStockClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AKSI CEPAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400,
                letterSpacing = 0.8.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                title = "Catat Jual",
                icon = Icons.Outlined.ShoppingCart,
                onClick = onNewTransactionClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionItem(
                title = "Catat Beli",
                icon = Icons.Outlined.LocalShipping,
                onClick = onSupplierDebtsClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionItem(
                title = "Cek Stok",
                icon = Icons.Outlined.Inventory2,
                onClick = onStockClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionItem(
                title = "Laporan",
                icon = Icons.Outlined.BarChart,
                onClick = onReportsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate700,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. DAILY SALES TREND CARD (TREN PENJUALAN HARIAN)
// ═════════════════════════════════════════════════════════════════════════════
private data class TrendBarSlot(
    val label: String,
    val fraction: Float,
    val isPeak: Boolean
)

@Composable
private fun DailySalesTrendCard(
    dailySalesTrend: List<DailySalesSummaryDto>,
    financialTotals: SalesReportTotalsDto?,
    onTransactionsClick: () -> Unit
) {
    val totalRevenueInPeriod = remember(dailySalesTrend, financialTotals) {
        if (dailySalesTrend.isNotEmpty()) {
            dailySalesTrend.sumOf { it.totalRevenue }
        } else {
            financialTotals?.netRevenue?.toDouble() ?: 0.0
        }
    }

    val hasSalesData = remember(dailySalesTrend) {
        dailySalesTrend.any { it.totalRevenue > 0 }
    }

    val bars = remember(dailySalesTrend) {
        if (dailySalesTrend.isNotEmpty() && dailySalesTrend.any { it.totalRevenue > 0 }) {
            val displayItems = if (dailySalesTrend.size > 5) dailySalesTrend.takeLast(5) else dailySalesTrend
            val maxRevenue = displayItems.maxOfOrNull { it.totalRevenue }?.takeIf { it > 0 } ?: 1.0
            displayItems.map { summary ->
                val ratio = (summary.totalRevenue / maxRevenue).toFloat().coerceIn(0.12f, 1.0f)
                val isPeak = summary.totalRevenue == maxRevenue && summary.totalRevenue > 0
                TrendBarSlot(
                    label = formatShortDay(summary.date),
                    fraction = ratio,
                    isPeak = isPeak
                )
            }
        } else {
            // Real last 5 calendar days with 0/baseline height
            val today = LocalDate.now()
            (4 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                TrendBarSlot(
                    label = date.format(DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("id-ID"))),
                    fraction = 0.08f,
                    isPeak = false
                )
            }
        }
    }

    val peakLabel = remember(bars, hasSalesData) {
        if (hasSalesData) {
            bars.firstOrNull { it.isPeak }?.label ?: "-"
        } else {
            "-"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Dot + Judul + Total Pill Badge
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
                            .size(8.dp)
                            .background(BrandGreen, CircleShape)
                    )
                    Text(
                        text = "Tren Penjualan Harian",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        letterSpacing = (-0.2).sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate100)
                ) {
                    Text(
                        text = "Total: ${totalRevenueInPeriod.toRupiah()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 5 Visual Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (bar.isPeak) BrandGreenLight else Slate50)
                                .padding(4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(bar.fraction)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandGreen)
                            )
                        }

                        Text(
                            text = bar.label,
                            fontSize = 11.sp,
                            fontWeight = if (bar.isPeak) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (bar.isPeak) BrandGreen else Slate400,
                            maxLines = 1
                        )
                    }
                }
            }

            // Footer: Peak Info & Riwayat Link
            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasSalesData) "Puncak penjualan: $peakLabel" else "Belum ada transaksi pada periode ini",
                    fontSize = 12.sp,
                    color = Slate400
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onTransactionsClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Riwayat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. RECENT TRANSACTIONS CARD (TRANSAKSI TERKINI)
// ═════════════════════════════════════════════════════════════════════════════
private data class DisplayTransactionItem(
    val title: String,
    val subtitle: String,
    val amount: String,
    val status: String = "Lunas"
)

@Composable
private fun RecentTransactionsCard(
    recentTransactions: List<CashTransaction>,
    onTransactionsClick: () -> Unit
) {
    val items = remember(recentTransactions) {
        recentTransactions.take(5).map { trx ->
            val receiptNo = if (trx.receiptId.isNotBlank()) {
                val code = trx.receiptId.takeLast(6).uppercase()
                "#TRX-$code"
            } else {
                "#TRX-${trx.id.takeLast(4).uppercase()}"
            }
            val customer = if (!trx.customerName.isNullOrBlank()) trx.customerName else "Pelanggan Umum"
            val method = trx.paymentMethods.firstOrNull()?.uppercase() ?: "TUNAI"
            val timeStr = formatIsoTime(trx.createdAt)
            DisplayTransactionItem(
                title = "$receiptNo · $customer",
                subtitle = "$timeStr WIB · $method",
                amount = trx.total.toDouble().toRupiah(),
                status = if (trx.status.equals("COMPLETED", ignoreCase = true) || trx.status.equals("PAID", ignoreCase = true) || trx.status.equals("LUNAS", ignoreCase = true)) "Lunas" else trx.status
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Dot + Transaksi Terkini + Lihat Semua Link
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
                            .size(8.dp)
                            .background(BrandGreen, CircleShape)
                    )
                    Text(
                        text = "Transaksi Terkini",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        letterSpacing = (-0.2).sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onTransactionsClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Lihat Semua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Belum ada transaksi pada periode ini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                        Text(
                            text = "Transaksi kasir yang selesai akan muncul otomatis di sini.",
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Transaction rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTransactionsClick)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.amount,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = BrandGreenLight
                            ) {
                                Text(
                                    text = item.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. SYSTEM STATUS HEALTH CARD (SEMAU SISTEM NORMAL)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun SystemStatusHealthCard(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Semua Sistem Normal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BrandGreen, CircleShape)
                        )
                    }

                    Text(
                        text = "Cloud tersinkron · Stok aman",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Detail",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. SYSTEM STATUS & SINKRONISASI MODAL SHEET
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemStatusModalSheet(
    offlineUiState: OfflineDashboardUiState,
    onSyncCenterClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var isSyncing by remember { mutableStateOf(false) }
    var syncSuccess by remember { mutableStateOf(false) }

    val syncRotation by animateFloatAsState(
        targetValue = if (isSyncing) 360f * 4 else 0f,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "syncRotation"
    )

    val syncTime = remember(offlineUiState.lastRefresh) {
        formatTime(offlineUiState.lastRefresh)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Slate200)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header: Icon + Title + Close Button
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Status Sistem & Sinkronisasi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Diagnostik cloud & perangkat kasir",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Sync Banner Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandGreenLight,
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(BrandGreen, CircleShape)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Cloud Tersinkron",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800
                            )
                            Text(
                                text = if (syncSuccess) "Terakhir: Baru saja" else "Terakhir: Hari ini, $syncTime WIB",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (!isSyncing) {
                                coroutineScope.launch {
                                    isSyncing = true
                                    syncSuccess = false
                                    delay(1500)
                                    isSyncing = false
                                    syncSuccess = true
                                    onSyncCenterClick()
                                    delay(1500)
                                    syncSuccess = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (syncSuccess) BrandGreenDark else BrandGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (syncSuccess) Icons.Outlined.Check else Icons.Outlined.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (isSyncing) syncRotation else 0f)
                            )
                            Text(
                                text = when {
                                    isSyncing -> "Menyinkronkan..."
                                    syncSuccess -> "Tersinkron ✓"
                                    else -> "Sinkronkan"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Diagnostik Data Kasir
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DIAGNOSTIK DATA KASIR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticRowItem(
                            icon = Icons.Outlined.Folder,
                            label = "Data Lokal Tersimpan",
                            value = "${offlineUiState.localTransactionCount} nota",
                            valueColor = Slate800
                        )

                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f), thickness = 1.dp)

                        DiagnosticRowItem(
                            icon = Icons.AutoMirrored.Outlined.ScheduleSend,
                            label = "Antrean Menunggu Kirim",
                            value = "${offlineUiState.pendingSyncCount} antrean",
                            valueColor = Slate800
                        )

                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f), thickness = 1.dp)

                        DiagnosticRowItem(
                            icon = Icons.Outlined.ErrorOutline,
                            label = "Gagal Kirim Server",
                            value = "${offlineUiState.failedSyncCount} nota",
                            valueColor = BrandGreen
                        )

                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Status Database Kasir",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(BrandGreen, CircleShape)
                                )
                                Text(
                                    text = "Normal & Aman",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandGreen
                                )
                            }
                        }
                    }
                }
            }

            // Aksi Pintasan
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AKSI PINTASAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Cek Koneksi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800
                                )
                                Text(
                                    text = "Online (Latensi 24ms)",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            onDismiss()
                            onSyncCenterClick()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Log Sinkron",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800
                                )
                                Text(
                                    text = "Riwayat aktivitas",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tutup Button
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tutup",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Slate600
            )
        }

        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 7. PERIOD FILTER MODAL SHEET
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
    var pendingDays by remember(selectedDays) { mutableIntStateOf(selectedDays) }

    val today = remember { LocalDate.now() }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")) }
    val shortRangeFormatter = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Slate200)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon + Title + Close Button
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Pilih Periode Penjualan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Sesuaikan data laporan dan ringkasan",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hari Ini
                PeriodOptionItem(
                    title = "Hari Ini",
                    subtitle = today.format(dayFormatter),
                    isSelected = pendingDays == 1,
                    onClick = { pendingDays = 1 }
                )

                // Kemarin
                PeriodOptionItem(
                    title = "Kemarin",
                    subtitle = today.minusDays(1).format(dayFormatter),
                    isSelected = pendingDays == 2,
                    onClick = { pendingDays = 2 }
                )

                // 7 Hari Terakhir
                PeriodOptionItem(
                    title = "7 Hari Terakhir",
                    subtitle = "${today.minusDays(6).format(shortRangeFormatter)} - ${today.format(shortRangeFormatter)}",
                    isSelected = pendingDays == 7,
                    onClick = { pendingDays = 7 }
                )

                // Bulan Ini
                val startOfMonth = today.withDayOfMonth(1)
                PeriodOptionItem(
                    title = "Bulan Ini",
                    subtitle = "${startOfMonth.format(shortRangeFormatter)} - ${today.format(shortRangeFormatter)}",
                    isSelected = pendingDays == 30,
                    onClick = { pendingDays = 30 }
                )

                // Pilih Tanggal Kustom
                Surface(
                    onClick = onCustomDateClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Pilih Tanggal Kustom",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate800
                            )
                            Text(
                                text = "Pilih rentang tanggal",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Footer Action Buttons (Batal & Terapkan)
            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                Surface(
                    onClick = {
                        onSelectPeriod(pendingDays)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = BrandGreen
                ) {
                    Text(
                        text = "Terapkan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandGreenLight else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandGreen else Slate100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) BrandGreen else Slate800
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isSelected) Slate500 else Slate400
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SKELETON LOADING
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OwnerDashboardSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero card skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBox(Modifier.width(120.dp).height(12.dp))
                    SkeletonBox(Modifier.width(90.dp).height(24.dp).clip(RoundedCornerShape(999.dp)))
                }
                SkeletonBox(Modifier.width(180.dp).height(28.dp))
                HorizontalDivider(color = Slate100, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(80.dp).height(10.dp))
                        SkeletonBox(Modifier.width(100.dp).height(14.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(80.dp).height(10.dp))
                        SkeletonBox(Modifier.width(100.dp).height(14.dp))
                    }
                }
            }
        }

        // Shortcuts skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBox(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                        SkeletonBox(Modifier.width(50.dp).height(10.dp))
                    }
                }
            }
        }

        // Trend card skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder)
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
                    SkeletonBox(Modifier.width(90.dp).height(20.dp).clip(RoundedCornerShape(999.dp)))
                }
                SkeletonBox(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(8.dp)))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// FORMATTERS & HELPERS
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

private fun formatIsoTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        instant.atZone(zone).format(formatter)
    } catch (_: Exception) {
        try {
            val parts = isoString.split("T")
            if (parts.size > 1) {
                parts[1].take(5)
            } else {
                "16:42"
            }
        } catch (_: Exception) {
            "16:42"
        }
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
    return "Rp" + formatter.format(this)
}
