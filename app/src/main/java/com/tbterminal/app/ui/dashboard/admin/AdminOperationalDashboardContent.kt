package com.tbterminal.app.ui.dashboard.admin

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.StockLowSummaryDto
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbAdaptiveBox
import com.tbterminal.app.ui.dashboard.BackofficeAvatarButton
import com.tbterminal.app.ui.dashboard.LocalBackofficeAccountSheet
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── MODERN MINIMALIST DESIGN TOKENS ──
private val SurfaceBg = TbBackground
private val CardBorder = TbOutline.copy(alpha = 0.22f)
private val BrandGreen = Color(0xFF1B5E3C)
private val BrandGreenLight = Color(0xFFEEF7F2)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald500 = Color(0xFF10B981)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)

private val AmberText = Color(0xFFD97706)
private val AmberBg = Color(0xFFFFFBEB)
private val RoseText = Color(0xFFE11D48)
private val RoseBg = Color(0xFFFFF1F2)
private val RoseBorder = Color(0xFFFECDD3)

@Composable
fun AdminOperationalDashboardContent(
    adminName: String,
    metrics: DashboardMetricsDto?,
    activeCashierSession: CashSession?,
    recentTransactions: List<CashTransaction>,
    isLoading: Boolean,
    error: String?,
    offlineUiState: OfflineDashboardUiState,
    onIncomingGoodsClick: () -> Unit,
    onStockClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onCashClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onSyncCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPrivacyHidden by rememberSaveable { mutableStateOf(false) }

    TbAdaptiveBox(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) { layout ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (layout.isCompact) 540.dp else 1040.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = layout.horizontalPadding, vertical = 14.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── 1. HEADER MINIMALIS & STATUS SHIFT ──
                AdminOperationalHeader(
                    adminName = adminName,
                    activeSession = activeCashierSession,
                    offlineUiState = offlineUiState,
                    onCashClick = onCashClick
                )

                if (error != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = RoseBg,
                        border = BorderStroke(1.dp, RoseBorder)
                    ) {
                        Text(
                            text = "Ringkasan operasional belum dapat dimuat: $error",
                            color = RoseText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                if (isLoading && metrics == null) {
                    AdminDashboardSkeleton()
                } else {
                    // ── 2. HERO RINGKASAN OPERASIONAL HARI INI ──
                    OperationalHeroCard(
                        metrics = metrics,
                        activeSession = activeCashierSession,
                        transactionCount = if (recentTransactions.isNotEmpty()) {
                            recentTransactions.size
                        } else {
                            offlineUiState.localTransactionCount
                        },
                        isPrivacyHidden = isPrivacyHidden,
                        onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                        onTransactionsClick = onTransactionsClick,
                        onCashClick = onCashClick,
                        onStockClick = onStockClick,
                        onReceivablesClick = onReceivablesClick
                    )

                    // ── 3. GRID AKSI CEPAT OPERASIONAL (BENTO MINIMALIS) ──
                    AdminQuickActionsSection(
                        isCompact = layout.isCompact,
                        onIncomingGoodsClick = onIncomingGoodsClick,
                        onStockOpnameClick = onStockOpnameClick,
                        onCashExpensesClick = onCashExpensesClick,
                        onCashClick = onCashClick,
                        onReceivablesClick = onReceivablesClick,
                        onSupplierDebtsClick = onSupplierDebtsClick
                    )

                    val lowStockItems = metrics?.lowStockItems ?: emptyList()

                    if (!layout.isCompact && lowStockItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                LowStockActionPanel(
                                    lowStockItems = lowStockItems,
                                    totalLowStockCount = metrics?.lowStockCount ?: lowStockItems.size.toLong(),
                                    onManageStockClick = onStockClick,
                                    onReceiveGoodsClick = onIncomingGoodsClick
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                RecentTransactionsLiveCard(
                                    transactions = recentTransactions,
                                    isPrivacyHidden = isPrivacyHidden,
                                    onViewAllClick = onTransactionsClick
                                )
                            }
                        }
                    } else {
                        // ── 4. PERINGATAN STOK MENIPIS (ACTIONABLE) ──
                        if (lowStockItems.isNotEmpty()) {
                            LowStockActionPanel(
                                lowStockItems = lowStockItems,
                                totalLowStockCount = metrics?.lowStockCount ?: lowStockItems.size.toLong(),
                                onManageStockClick = onStockClick,
                                onReceiveGoodsClick = onIncomingGoodsClick
                            )
                        }

                        // ── 5. TRANSAKSI TERKINI KASIR ──
                        RecentTransactionsLiveCard(
                            transactions = recentTransactions,
                            isPrivacyHidden = isPrivacyHidden,
                            onViewAllClick = onTransactionsClick
                        )
                    }

                    // ── 6. STATUS SINKRONISASI & DATA LOKAL TERPADU ──
                    AdminSyncHealthSection(
                        offlineUiState = offlineUiState,
                        isPrivacyHidden = isPrivacyHidden,
                        onSyncCenterClick = onSyncCenterClick
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. MINIMALIST HEADER & LIVE SHIFT PILL
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminOperationalHeader(
    adminName: String,
    activeSession: CashSession?,
    offlineUiState: OfflineDashboardUiState,
    onCashClick: () -> Unit
) {
    val displayName = remember(adminName) {
        val trimmed = adminName.trim()
        if (trimmed.isEmpty() || trimmed.equals("admin", ignoreCase = true)) {
            "Admin"
        } else {
            trimmed
        }
    }
    val hasActiveSession = activeSession != null
    val openAccountSheet = LocalBackofficeAccountSheet.current

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.forLanguageTag("id-ID")))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Halo, $displayName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$todayFormatted · Operasional & kontrol toko",
                    fontSize = 12.sp,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tonal shift status pill (tanpa border kaku)
                Surface(
                    onClick = onCashClick,
                    shape = RoundedCornerShape(999.dp),
                    color = if (hasActiveSession) Emerald50 else TbSurfaceMuted,
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (hasActiveSession) Emerald500 else Slate400, CircleShape)
                        )
                        Text(
                            text = if (hasActiveSession) {
                                "Shift: ${activeSession?.userName?.takeIf { it.isNotBlank() } ?: "Aktif"}"
                            } else {
                                "Shift Tutup"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasActiveSession) BrandGreen else Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (openAccountSheet != null) {
                    BackofficeAvatarButton(
                        name = displayName,
                        onClick = openAccountSheet,
                        testTag = "dashboard-admin-avatar-btn"
                    )
                }
            }
        }

        val hasPending = offlineUiState.pendingSyncCount > 0 || offlineUiState.failedSyncCount > 0
        if (hasPending) {
            val syncTimeStr = remember(offlineUiState.lastRefresh) { formatTime(offlineUiState.lastRefresh) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (offlineUiState.failedSyncCount > 0) RoseText else AmberText, CircleShape)
                )
                Text(
                    text = if (offlineUiState.failedSyncCount > 0) {
                        "${offlineUiState.failedSyncCount} sinkronisasi gagal · Ketuk Pusat Sync untuk periksa"
                    } else {
                        "${offlineUiState.pendingSyncCount} antrean offline · Sinkron terakhir $syncTimeStr"
                    },
                    fontSize = 11.sp,
                    color = if (offlineUiState.failedSyncCount > 0) RoseText else AmberText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. HERO RINGKASAN OPERASIONAL HARI INI
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OperationalHeroCard(
    metrics: DashboardMetricsDto?,
    activeSession: CashSession?,
    transactionCount: Int,
    isPrivacyHidden: Boolean,
    onTogglePrivacy: () -> Unit,
    onTransactionsClick: () -> Unit,
    onCashClick: () -> Unit,
    onStockClick: () -> Unit,
    onReceivablesClick: () -> Unit
) {
    val todayRevenue = metrics?.totalRevenueToday?.toRupiah() ?: "Rp0"
    val lowStockCount = metrics?.lowStockCount ?: 0L
    val activeReceivables = metrics?.totalActiveReceivables?.toRupiah() ?: "Rp0"
    val receivablesCount = metrics?.activeReceivableCount ?: 0L

    val drawerCashValue = remember(activeSession) {
        activeSession?.let { (it.systemCash ?: it.openingCash).toRupiah() } ?: "Tutup"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TbSurface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Label + Privacy Button + Struk Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Penjualan Kasir Hari Ini",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("admin-dashboard-toggle-privacy")
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Sembunyikan atau tampilkan angka",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TbSurfaceMuted
                ) {
                    Text(
                        text = "$transactionCount Struk",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Hero Number
            Text(
                text = if (isPrivacyHidden) "Rp••••••" else todayRevenue,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                letterSpacing = (-0.8).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onTransactionsClick)
                    .padding(vertical = 2.dp)
            )

            // Tonal 3-Sub-Metrics Strip
            val hasCriticalStock = lowStockCount > 0L
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = TbSurfaceMuted
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OperationalSubMetricTile(
                        label = "Kas Laci",
                        value = if (isPrivacyHidden && activeSession != null) "Rp••••" else drawerCashValue,
                        subtitle = if (activeSession != null) "Estimasi shift" else "Belum dibuka",
                        valueColor = if (activeSession != null) Slate900 else Slate500,
                        onClick = onCashClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(Slate200)
                    )

                    OperationalSubMetricTile(
                        label = "Stok Kritis",
                        value = if (hasCriticalStock) "$lowStockCount Produk" else "Aman",
                        subtitle = if (hasCriticalStock) "Perlu restok" else "Stok terjaga",
                        valueColor = if (hasCriticalStock) RoseText else BrandGreen,
                        onClick = onStockClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(Slate200)
                    )

                    OperationalSubMetricTile(
                        label = "Piutang",
                        value = if (isPrivacyHidden) "Rp••••" else activeReceivables,
                        subtitle = "$receivablesCount nota aktif",
                        valueColor = Slate900,
                        onClick = onReceivablesClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OperationalSubMetricTile(
    label: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = Slate400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. AKSI CEPAT OPERASIONAL (ADAPTIVE MINIMALIST GRID)
// ═════════════════════════════════════════════════════════════════════════════
private data class AdminQuickActionSpec(
    val title: String,
    val icon: ImageVector,
    val isPrimary: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun AdminQuickActionsSection(
    isCompact: Boolean,
    onIncomingGoodsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onCashClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = remember(
        onIncomingGoodsClick,
        onStockOpnameClick,
        onCashExpensesClick,
        onCashClick,
        onReceivablesClick,
        onSupplierDebtsClick
    ) {
        listOf(
            AdminQuickActionSpec(
                title = "Barang Masuk",
                icon = Icons.Outlined.LocalShipping,
                isPrimary = true,
                onClick = onIncomingGoodsClick
            ),
            AdminQuickActionSpec(
                title = "Sesuaikan Stok",
                icon = Icons.Outlined.Inventory2,
                isPrimary = false,
                onClick = onStockOpnameClick
            ),
            AdminQuickActionSpec(
                title = "Kas Keluar",
                icon = Icons.Outlined.PriceChange,
                isPrimary = false,
                onClick = onCashExpensesClick
            ),
            AdminQuickActionSpec(
                title = "Cocokkan Kas",
                icon = Icons.Outlined.Payments,
                isPrimary = false,
                onClick = onCashClick
            ),
            AdminQuickActionSpec(
                title = "Piutang",
                icon = Icons.Outlined.AccountBalanceWallet,
                isPrimary = false,
                onClick = onReceivablesClick
            ),
            AdminQuickActionSpec(
                title = "Hutang Supplier",
                icon = Icons.Outlined.Receipt,
                isPrimary = false,
                onClick = onSupplierDebtsClick
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "AKSI CEPAT OPERASIONAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate400,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        if (isCompact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            AdminQuickActionTile(
                                spec = item,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                actions.forEach { item ->
                    AdminQuickActionTile(
                        spec = item,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminQuickActionTile(
    spec: AdminQuickActionSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = spec.onClick,
        shape = RoundedCornerShape(14.dp),
        color = TbSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (spec.isPrimary) BrandGreen.copy(alpha = 0.28f) else CardBorder
        ),
        shadowElevation = 0.dp,
        modifier = modifier.heightIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (spec.isPrimary) BrandGreenLight else TbSurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = spec.title,
                    tint = if (spec.isPrimary) BrandGreen else Slate700,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = spec.title,
                fontSize = 11.5.sp,
                fontWeight = if (spec.isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                color = if (spec.isPrimary) BrandGreen else Slate800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. ACTIONABLE LOW STOCK BUFFER ALERT PANEL
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun LowStockActionPanel(
    lowStockItems: List<StockLowSummaryDto>,
    totalLowStockCount: Long,
    onManageStockClick: () -> Unit,
    onReceiveGoodsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TbSurface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoseBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = RoseText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Stok Menipis",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = RoseBg
                    ) {
                        Text(
                            text = "$totalLowStockCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onManageStockClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Lihat Stok",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Column {
                lowStockItems.take(4).forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button, onClick = onManageStockClick)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.productName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val meta = buildString {
                                if (item.sku.isNotBlank()) {
                                    append(item.sku)
                                    append(" · ")
                                }
                                append("Min ${item.minStock.formatStockQty()}")
                            }
                            Text(
                                text = meta,
                                fontSize = 11.sp,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RoseBg
                        ) {
                            Text(
                                text = "Sisa ${item.quantity.formatStockQty()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RoseText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onReceiveGoodsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                shape = RoundedCornerShape(12.dp),
                color = BrandGreenLight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Catat Barang Masuk",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. RECENT TRANSACTIONS STREAM
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun RecentTransactionsLiveCard(
    transactions: List<CashTransaction>,
    isPrivacyHidden: Boolean,
    onViewAllClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TbSurface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terkini",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onViewAllClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
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
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TbSurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Belum ada transaksi kasir tercatat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                        Text(
                            text = "Aktivitas struk kasir akan muncul otomatis di sini.",
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column {
                    transactions.take(5).forEachIndexed { index, trx ->
                        if (index > 0) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                        }
                        val receiptNo = if (trx.receiptId.isNotBlank()) {
                            "#${trx.receiptId.takeLast(8).uppercase()}"
                        } else {
                            "#${trx.id.takeLast(6).uppercase()}"
                        }
                        val methodText = trx.paymentMethods.firstOrNull()?.uppercase() ?: "TUNAI"
                        val buyer = trx.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan Umum"
                        val cashier = trx.cashierName?.takeIf { it.isNotBlank() } ?: "Kasir"
                        val timeText = formatIsoTime(trx.createdAt)
                        val statusBadge = resolveTransactionStatusDisplay(trx)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clickable(role = Role.Button, onClick = onViewAllClick)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TbSurfaceMuted),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "$receiptNo · $buyer",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = TbSurfaceMuted
                                        ) {
                                            Text(
                                                text = methodText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate600,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$timeText WIB · $cashier",
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isPrivacyHidden) "Rp••••••" else trx.total.toRupiah(),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = statusBadge.first,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusBadge.second
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. UNIFIED DEVICE & SYNC HEALTH CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminSyncHealthSection(
    offlineUiState: OfflineDashboardUiState,
    isPrivacyHidden: Boolean,
    onSyncCenterClick: () -> Unit
) {
    val hasIssues = offlineUiState.failedSyncCount > 0 || offlineUiState.pendingSyncCount > 0
    val isFailed = offlineUiState.failedSyncCount > 0
    val refreshedTime = remember(offlineUiState.lastRefresh) {
        formatTime(offlineUiState.lastRefresh)
    }

    Surface(
        onClick = onSyncCenterClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TbSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFailed) RoseBorder else CardBorder
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isFailed -> RoseBg
                                    hasIssues -> AmberBg
                                    else -> BrandGreenLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isFailed -> Icons.Outlined.ErrorOutline
                                hasIssues -> Icons.Outlined.Sync
                                else -> Icons.Outlined.CheckCircle
                            },
                            contentDescription = null,
                            tint = when {
                                isFailed -> RoseText
                                hasIssues -> AmberText
                                else -> BrandGreen
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = when {
                                isFailed -> "${offlineUiState.failedSyncCount} Data Gagal Sinkron"
                                hasIssues -> "${offlineUiState.pendingSyncCount} Antrean Sinkronisasi"
                                else -> "Semua Data Tersinkron"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900
                        )
                        Text(
                            text = "Perangkat lokal · Diperbarui $refreshedTime",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Pusat Sync",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFailed) RoseText else BrandGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (isFailed) RoseText else BrandGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = TbSurfaceMuted
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transaksi Lokal", fontSize = 10.5.sp, color = Slate500)
                        Text(
                            text = "${offlineUiState.localTransactionCount} Nota",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Slate200)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Text("Omzet Lokal", fontSize = 10.5.sp, color = Slate500)
                        Text(
                            text = if (isPrivacyHidden) "Rp••••" else offlineUiState.localRevenueToday.toRupiah(),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Slate200)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {
                        Text("Antrean Sync", fontSize = 10.5.sp, color = Slate500)
                        val syncCountLabel = when {
                            offlineUiState.failedSyncCount > 0 -> "${offlineUiState.failedSyncCount} Gagal"
                            offlineUiState.pendingSyncCount > 0 -> "${offlineUiState.pendingSyncCount} Pending"
                            else -> "0 Antrean"
                        }
                        Text(
                            text = syncCountLabel,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFailed -> RoseText
                                offlineUiState.pendingSyncCount > 0 -> AmberText
                                else -> BrandGreen
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 7. SKELETON LOADING STATE
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminDashboardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = TbSurface,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBox(Modifier.width(130.dp).height(12.dp))
                    SkeletonBox(Modifier.width(72.dp).height(20.dp))
                }
                SkeletonBox(Modifier.width(190.dp).height(30.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(54.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = TbSurface,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SkeletonBox(Modifier.size(36.dp))
                                SkeletonBox(Modifier.width(56.dp).height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = TbSurface,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonBox(Modifier.width(140.dp).height(14.dp))
                repeat(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonBox(Modifier.size(34.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(12.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.4f).height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// FORMATTER & STATUS UTILITIES
// ═════════════════════════════════════════════════════════════════════════════
private fun resolveTransactionStatusDisplay(trx: CashTransaction): Pair<String, Color> {
    val normalized = trx.status.trim().uppercase()
    if (trx.voidedAt != null || normalized == "VOID" || normalized == "VOIDED" || normalized == "BATAL" || normalized == "DIBATALKAN") {
        return "Batal" to RoseText
    }
    if (normalized == "REFUND" || normalized == "REFUNDED") {
        return "Refund" to RoseText
    }
    val hasRemaining = trx.remainingAmount != null && trx.remainingAmount > BigDecimal.ZERO
    if (hasRemaining || normalized == "PARTIAL" || normalized == "UNPAID" || normalized == "CREDIT" || normalized == "HUTANG") {
        return "Belum Lunas" to AmberText
    }
    if (normalized == "COMPLETED" || normalized == "PAID" || normalized == "LUNAS" || normalized == "SELESAI") {
        return "Lunas" to BrandGreen
    }
    return trx.status to Slate600
}

private fun BigDecimal.formatStockQty(): String {
    val normalized = stripTrailingZeros()
    return if (normalized.scale() <= 0) {
        normalized.toBigInteger().toString()
    } else {
        normalized.toPlainString()
    }
}

private fun formatTime(millis: Long): String {
    return try {
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        "--:--"
    }
}

private fun formatIsoTime(isoString: String): String {
    return try {
        Instant.parse(isoString)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(isoString)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            val parts = isoString.split("T")
            if (parts.size > 1 && parts[1].length >= 5) parts[1].take(5) else "--:--"
        }
    }
}

private fun Number.toRupiah(): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return "Rp" + DecimalFormat("#,##0", symbols).format(this)
}

// ═════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "Admin Operational Dashboard - Normal", showBackground = true, widthDp = 380, heightDp = 760)
@Composable
private fun AdminOperationalDashboardNormalPreview() {
    TbterminalappTheme {
        AdminOperationalDashboardContent(
            adminName = "Ahmad",
            metrics = DashboardMetricsDto(
                totalRevenueToday = BigDecimal("5200000"),
                totalRevenueThisMonth = BigDecimal("42000000"),
                totalActiveReceivables = BigDecimal("850000"),
                activeReceivableCount = 3,
                lowStockCount = 1,
                lowStockItems = emptyList()
            ),
            activeCashierSession = CashSession(
                id = "SES-01",
                userId = "USR-01",
                userName = "Siti Kasir",
                openedAt = "2026-11-18T08:00:00Z",
                closedAt = null,
                openingCash = BigDecimal("500000"),
                closingCash = null,
                systemCash = BigDecimal("1250000"),
                difference = null,
                totalExpenses = BigDecimal.ZERO,
                notes = null,
                status = "OPEN"
            ),
            recentTransactions = emptyList(),
            isLoading = false,
            error = null,
            offlineUiState = OfflineDashboardUiState(
                pendingSyncCount = 0,
                failedSyncCount = 0
            ),
            onIncomingGoodsClick = {},
            onStockClick = {},
            onStockOpnameClick = {},
            onCashExpensesClick = {},
            onCashClick = {},
            onTransactionsClick = {},
            onReceivablesClick = {},
            onSupplierDebtsClick = {},
            onSyncCenterClick = {}
        )
    }
}

@Preview(name = "Admin Operational Dashboard - Offline with Queue", showBackground = true, widthDp = 380, heightDp = 760)
@Composable
private fun AdminOperationalDashboardOfflineWithQueuePreview() {
    TbterminalappTheme {
        AdminOperationalDashboardContent(
            adminName = "Ahmad",
            metrics = DashboardMetricsDto(
                totalRevenueToday = BigDecimal("2100000"),
                totalRevenueThisMonth = BigDecimal("31000000"),
                totalActiveReceivables = BigDecimal("450000"),
                activeReceivableCount = 2,
                lowStockCount = 4,
                lowStockItems = emptyList()
            ),
            activeCashierSession = null,
            recentTransactions = emptyList(),
            isLoading = false,
            error = null,
            offlineUiState = OfflineDashboardUiState(
                pendingSyncCount = 4,
                failedSyncCount = 1
            ),
            onIncomingGoodsClick = {},
            onStockClick = {},
            onStockOpnameClick = {},
            onCashExpensesClick = {},
            onCashClick = {},
            onTransactionsClick = {},
            onReceivablesClick = {},
            onSupplierDebtsClick = {},
            onSyncCenterClick = {}
        )
    }
}
