package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.StockLowSummaryDto
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbAdaptiveBox
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SurfaceBg = Color(0xFFF8FAF9)
private val CardBorder = Color(0x0A000000)
private val BrandGreen = Color(0xFF10533C)
private val Forest700 = Color(0xFF1B5E3C)
private val BrandGreenLight = Color(0xFFEEF7F2)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald500 = Color(0xFF10B981)
private val EmeraldBorder = Color(0xFFA7F3D0)

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

private val AmberText = Color(0xFFD97706)
private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val RoseText = Color(0xFFE11D48)
private val RoseBg = Color(0xFFFFF1F2)
private val RoseBorder = Color(0xFFFDA4AF)

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
                // ── 1. SAPAAN & STATUS SHIFT OPERASIONAL LIVE ──
                AdminOperationalHeader(
                    adminName = adminName,
                    activeSession = activeCashierSession,
                    isPrivacyHidden = isPrivacyHidden,
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
                        isPrivacyHidden = isPrivacyHidden,
                        onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                        onTransactionsClick = onTransactionsClick,
                        onCashClick = onCashClick,
                        onStockClick = onStockClick,
                        onReceivablesClick = onReceivablesClick
                    )

                    // ── 3. CTA UTAMA GUDANG & AKSI CEPAT (6 MENU) ──
                    OperationalPrimaryCtaCard(
                        onIncomingGoodsClick = onIncomingGoodsClick
                    )

                    OperationalQuickActionsGrid(
                        compact = layout.isCompact,
                        onStockOpnameClick = onStockOpnameClick,
                        onCashClick = onCashClick,
                        onCashExpensesClick = onCashExpensesClick,
                        onReceivablesClick = onReceivablesClick,
                        onSupplierDebtsClick = onSupplierDebtsClick,
                        onTransactionsClick = onTransactionsClick
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
                            onViewAllClick = onTransactionsClick
                        )
                    }

                    // ── 6. STATUS SINKRONISASI & DATA PERANGKAT RINGKAS ──
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
// 1. GREETING & LIVE SHIFT STATUS CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminOperationalHeader(
    adminName: String,
    activeSession: CashSession?,
    isPrivacyHidden: Boolean,
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

    Surface(
        onClick = onCashClick,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Halo, $displayName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                if (hasActiveSession) {
                    val infiniteTransition = rememberInfiniteTransition(label = "adminPulseShift")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "adminPulseAlpha"
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Emerald50,
                        border = BorderStroke(1.dp, EmeraldBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Emerald500.copy(alpha = pulseAlpha), CircleShape)
                            )
                            Text(
                                text = "Shift: ${activeSession?.userName?.takeIf { it.isNotBlank() } ?: "Kasir"}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Forest700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AmberBg,
                        border = BorderStroke(1.dp, AmberBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(AmberText, CircleShape)
                            )
                            Text(
                                text = "Shift Belum Buka",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberText
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (hasActiveSession) Emerald500 else Slate400, CircleShape)
                    )
                    val statusInfo = if (activeSession != null) {
                        val openingText = if (isPrivacyHidden) "••••••" else activeSession.openingCash.toRupiah()
                        "Kasir aktif · Modal $openingText"
                    } else {
                        "Belum ada sesi kasir aktif hari ini"
                    }
                    Text(
                        text = statusInfo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (activeSession != null) {
                    val openedTime = remember(activeSession.openedAt) { formatIsoTime(activeSession.openedAt) }
                    Text(
                        text = "Buka $openedTime WIB",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate400
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Kelola Kas",
                            fontSize = 11.5.sp,
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
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. HERO RINGKASAN OPERASIONAL HARI INI
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OperationalHeroCard(
    metrics: DashboardMetricsDto?,
    activeSession: CashSession?,
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

    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID")))
    }

    val drawerCashValue = remember(activeSession) {
        activeSession?.let { (it.systemCash ?: it.openingCash).toRupiah() } ?: "Tutup"
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Date Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RINGKASAN OPERASIONAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "Hari Ini · $todayLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Hero Number: Penjualan Hari Ini
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onTransactionsClick),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Penjualan Kasir Hari Ini",
                        fontSize = 12.sp,
                        color = Slate500
                    )

                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Sembunyikan atau tampilkan angka",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = if (isPrivacyHidden) "••••••••" else todayRevenue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // 3 Operational Sub-Metrics Strip
            val hasCriticalStock = lowStockCount > 0L
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OperationalSubMetricTile(
                    label = "Kas Shift",
                    value = if (isPrivacyHidden && activeSession != null) "••••••" else drawerCashValue,
                    subtitle = if (activeSession != null) "Estimasi laci" else "Belum dibuka",
                    valueColor = if (activeSession != null) Slate900 else Slate500,
                    onClick = onCashClick,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                Box(modifier = Modifier.width(1.dp).height(38.dp).background(Slate100))

                OperationalSubMetricTile(
                    label = "Stok Kritis",
                    value = if (hasCriticalStock) "$lowStockCount Produk" else "Aman",
                    subtitle = if (hasCriticalStock) "Perlu restok" else "Stok terjaga",
                    valueColor = if (hasCriticalStock) RoseText else BrandGreen,
                    onClick = onStockClick,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )

                Box(modifier = Modifier.width(1.dp).height(38.dp).background(Slate100))

                OperationalSubMetricTile(
                    label = "Piutang Aktif",
                    value = if (isPrivacyHidden) "••••••" else activeReceivables,
                    subtitle = "$receivablesCount nota",
                    valueColor = Slate900,
                    onClick = onReceivablesClick,
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                )
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate400,
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = Slate400,
            maxLines = 1
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. PRIMARY WAREHOUSE CTA & 6-TILE AKSI CEPAT (MOBILE-SAFE GRID)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OperationalPrimaryCtaCard(
    onIncomingGoodsClick: () -> Unit
) {
    Surface(
        onClick = onIncomingGoodsClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Emerald50)
                        .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "TERIMA BARANG MASUK / PO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Catat penerimaan stok supplier ke gudang",
                        fontSize = 11.5.sp,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .border(1.dp, Slate200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Buka Terima Barang",
                    tint = Slate600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class AdminQuickActionSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlighted: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun OperationalQuickActionsGrid(
    compact: Boolean,
    onStockOpnameClick: () -> Unit,
    onCashClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onTransactionsClick: () -> Unit
) {
    val actions = listOf(
        AdminQuickActionSpec("Stok Opname", "Audit Fisik", Icons.Outlined.Inventory2, highlighted = true, onClick = onStockOpnameClick),
        AdminQuickActionSpec("Kas Harian", "Cek Shift", Icons.Outlined.Payments, onClick = onCashClick),
        AdminQuickActionSpec("Beban Kas", "Kas Kecil", Icons.Outlined.PriceChange, onClick = onCashExpensesClick),
        AdminQuickActionSpec("Piutang", "Nota Tagih", Icons.Outlined.AccountBalanceWallet, onClick = onReceivablesClick),
        AdminQuickActionSpec("Hutang", "Supplier", Icons.Outlined.Receipt, onClick = onSupplierDebtsClick),
        AdminQuickActionSpec("Riwayat", "Semua Struk", Icons.AutoMirrored.Outlined.ReceiptLong, onClick = onTransactionsClick),
    )

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
                text = "AKSI CEPAT OPERASIONAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "6 Menu",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            )
        }

        val rows = if (compact) actions.chunked(3) else listOf(actions)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    AdminActionTile(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminActionTile(
    item: AdminQuickActionSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = item.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (item.highlighted) EmeraldBorder else CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item.highlighted) Emerald50 else BrandGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.subtitle,
                    fontSize = 10.sp,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
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
        color = Color.White,
        border = BorderStroke(1.dp, RoseBorder.copy(alpha = 0.5f)),
        shadowElevation = 0.5.dp
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
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
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
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = RoseText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Stok Menipis ($totalLowStockCount)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onManageStockClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Buka Gudang",
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

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                lowStockItems.take(4).forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = Slate100, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onManageStockClick)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
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
                            color = RoseBg,
                            border = BorderStroke(1.dp, RoseText.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "Sisa ${item.quantity.formatStockQty()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onReceiveGoodsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Catat Barang Masuk / Restok",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. RECENT TRANSACTIONS STREAM (WITH EMPTY STATE & ACCURATE STATUS COLORS)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun RecentTransactionsLiveCard(
    transactions: List<CashTransaction>,
    onViewAllClick: () -> Unit
) {
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
                        text = "Transaksi Terakhir Kasir",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onViewAllClick)
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

            if (transactions.isEmpty()) {
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
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Belum ada transaksi kasir tercatat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
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
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    transactions.take(5).forEachIndexed { index, trx ->
                        if (index > 0) HorizontalDivider(color = Slate100, thickness = 1.dp)
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
                                .clickable(onClick = onViewAllClick)
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$receiptNo · $buyer",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Slate100
                                    ) {
                                        Text(
                                            text = methodText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "$timeText WIB · Kasir: $cashier",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = trx.total.toRupiah(),
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
// 6. COMPACT DEVICE & SYNC HEALTH SECTION
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (hasIssues) {
            val bg = if (isFailed) RoseBg else AmberBg
            val border = if (isFailed) RoseBorder else AmberBorder
            val fg = if (isFailed) RoseText else AmberText
            val message = when {
                offlineUiState.failedSyncCount > 0 && offlineUiState.pendingSyncCount > 0 ->
                    "${offlineUiState.pendingSyncCount} antrean & ${offlineUiState.failedSyncCount} data gagal sinkron"
                offlineUiState.failedSyncCount > 0 ->
                    "Ada ${offlineUiState.failedSyncCount} data gagal sinkronisasi"
                else ->
                    "Ada ${offlineUiState.pendingSyncCount} data menunggu sinkronisasi"
            }

            Surface(
                onClick = onSyncCenterClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = bg,
                border = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isFailed) Icons.Outlined.ErrorOutline else Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = fg,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Sinkronkan →",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                }
            }
        }

        Surface(
            onClick = onSyncCenterClick,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hasIssues) AmberBg else BrandGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasIssues) Icons.Outlined.Sync else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (isFailed) RoseText else if (hasIssues) AmberText else BrandGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = if (isFailed) "Sinkronisasi Perlu Dicek" else if (hasIssues) "Sinkronisasi Berjalan" else "Semua Data Tersinkron",
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

                HorizontalDivider(color = Slate100, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transaksi Lokal", fontSize = 10.5.sp, color = Slate400)
                        Text(
                            text = "${offlineUiState.localTransactionCount} Nota",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(Slate100))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text("Omzet Lokal", fontSize = 10.5.sp, color = Slate400)
                        Text(
                            text = if (isPrivacyHidden) "••••••" else offlineUiState.localRevenueToday.toRupiah(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(Slate100))
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Antrean Sync", fontSize = 10.5.sp, color = Slate400)
                        val syncCountLabel = when {
                            offlineUiState.failedSyncCount > 0 -> "${offlineUiState.failedSyncCount} Gagal"
                            offlineUiState.pendingSyncCount > 0 -> "${offlineUiState.pendingSyncCount} Pending"
                            else -> "0 Antrean"
                        }
                        Text(
                            text = syncCountLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFailed) RoseText else if (offlineUiState.pendingSyncCount > 0) AmberText else BrandGreen,
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
            color = Color.White,
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
                    SkeletonBox(Modifier.width(96.dp).height(22.dp))
                }
                SkeletonBox(Modifier.width(190.dp).height(28.dp))
                HorizontalDivider(color = Slate100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(10.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.85f).height(16.dp))
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SkeletonBox(Modifier.size(44.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.65f).height(14.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.85f).height(12.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
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
                        SkeletonBox(Modifier.size(40.dp))
                        SkeletonBox(Modifier.width(56.dp).height(11.dp))
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

private fun Double.formatStockQty(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        String.format(Locale.forLanguageTag("id-ID"), "%.1f", this)
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
