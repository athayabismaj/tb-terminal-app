package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.StockLowSummaryDto
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardSection
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrandGreen = Color(0xFF256B57)
private val BrandGreenDark = Color(0xFF174D3F)
private val BrandGreenLight = Color(0xFFF0F7F4)
private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate100 = Color(0xFFF1F5F9)
private val SlateBorder = Color(0xFFE2E8F0)

private val AmberText = Color(0xFFD97706)
private val AmberBg = Color(0xFFFFFBEB)
private val RoseText = Color(0xFFE11D48)
private val RoseBg = Color(0xFFFFF1F2)
private val BlueText = Color(0xFF2563EB)
private val BlueBg = Color(0xFFEFF6FF)

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
    TbPageSurface(modifier = modifier) { layout, pageModifier ->
        Column(
            modifier = pageModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. HEADER & SHIFT STATUS ──
            AdminHeaderSection(
                adminName = adminName,
                hasActiveSession = activeCashierSession != null,
                activeCashierName = activeCashierSession?.userName,
                compact = layout.isCompact
            )

            // ── 2. OPERATIONAL QUICK ACTION TOOLBAR ──
            OperationalQuickActionToolbar(
                onIncomingGoodsClick = onIncomingGoodsClick,
                onStockOpnameClick = onStockOpnameClick,
                onCashExpensesClick = onCashExpensesClick,
                onTransactionsClick = onTransactionsClick
            )

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoseBg),
                    border = BorderStroke(1.dp, RoseText.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Ringkasan operasional belum dapat dimuat: $error",
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        color = RoseText,
                        fontSize = 13.sp
                    )
                }
            }

            if (isLoading && metrics == null) {
                repeat(if (layout.isCompact) 4 else 2) { SkeletonCard() }
            } else {
                // ── 3. OPERATIONAL 4-GRID STATUS CARDS ──
                OperationalStatusCards(
                    metrics = metrics,
                    activeSession = activeCashierSession,
                    onTransactionsClick = onTransactionsClick,
                    onCashClick = onCashClick,
                    onStockClick = onStockClick,
                    onReceivablesClick = onReceivablesClick,
                    compact = layout.isCompact
                )

                // ── 4. LOW STOCK BUFFER ALERT SECTION ──
                val lowStockItems = metrics?.lowStockItems ?: emptyList()
                if (lowStockItems.isNotEmpty()) {
                    LowStockActionPanel(
                        lowStockItems = lowStockItems,
                        totalLowStockCount = metrics?.lowStockCount ?: lowStockItems.size.toLong(),
                        onManageStockClick = onStockClick,
                        onReceiveGoodsClick = onIncomingGoodsClick
                    )
                }

                // ── 5. RECENT TRANSACTIONS STREAM ──
                if (recentTransactions.isNotEmpty()) {
                    RecentTransactionsLiveCard(
                        transactions = recentTransactions,
                        onViewAllClick = onTransactionsClick
                    )
                }

                // ── 6. OFFLINE SYNC STATUS ──
                OfflineDashboardSection(
                    uiState = offlineUiState,
                    onSyncCenterClick = onSyncCenterClick
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. HEADER SECTION
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminHeaderSection(
    adminName: String,
    hasActiveSession: Boolean,
    activeCashierName: String?,
    compact: Boolean
) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dashboard Operasional",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Admin: $adminName",
                    fontSize = 13.sp,
                    color = Slate500
                )
                ShiftStatusBadge(hasActiveSession, activeCashierName)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard Operasional Toko",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Kelola transaksi kasir, stok gudang, dan penerimaan barang harian",
                    fontSize = 14.sp,
                    color = Slate500
                )
            }
            ShiftStatusBadge(hasActiveSession, activeCashierName)
        }
    }
}

@Composable
private fun ShiftStatusBadge(
    hasActiveSession: Boolean,
    activeCashierName: String?
) {
    val bg = if (hasActiveSession) BrandGreenLight else Slate100
    val text = if (hasActiveSession) BrandGreen else Slate600
    val label = if (hasActiveSession) {
        "Shift Aktif: ${activeCashierName ?: "Kasir"}"
    } else {
        "Tidak Ada Shift Aktif"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, text.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (hasActiveSession) Color(0xFF10B981) else Slate400, CircleShape)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = text
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. QUICK ACTION TOOLBAR
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OperationalQuickActionToolbar(
    onIncomingGoodsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onTransactionsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Primary Action Button (Barang Masuk)
        Button(
            onClick = onIncomingGoodsClick,
            modifier = Modifier.weight(1.2f).height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Terima Barang", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        // Secondary Action: Cek Stok
        ActionToolbarButton(
            modifier = Modifier.weight(1f),
            label = "Stok Opname",
            icon = Icons.Outlined.Inventory2,
            onClick = onStockOpnameClick
        )

        // Tertiary Action: Pengeluaran Kas
        ActionToolbarButton(
            modifier = Modifier.weight(1f),
            label = "Beban Kas",
            icon = Icons.Outlined.PriceChange,
            onClick = onCashExpensesClick
        )

        // Quaternary Action: Riwayat Struk
        ActionToolbarButton(
            modifier = Modifier.weight(1f),
            label = "Riwayat Struk",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            onClick = onTransactionsClick
        )
    }
}

@Composable
private fun ActionToolbarButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SlateBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate700, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. OPERATIONAL STATUS CARDS (4-GRID)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun OperationalStatusCards(
    metrics: DashboardMetricsDto?,
    activeSession: CashSession?,
    onTransactionsClick: () -> Unit,
    onCashClick: () -> Unit,
    onStockClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    compact: Boolean
) {
    val todayRevenue = metrics?.totalRevenueToday?.toRupiah() ?: "Rp0"
    val lowStockCount = metrics?.lowStockCount ?: 0
    val activeReceivables = metrics?.totalActiveReceivables?.toRupiah() ?: "Rp0"
    val receivablesCount = "${metrics?.activeReceivableCount ?: 0} nota aktif"

    val sessionTitle = if (activeSession != null) {
        "Shift: ${activeSession.userName ?: "Kasir"}"
    } else {
        "Sesi Kas Ditutup"
    }
    val sessionSubtitle = if (activeSession != null) {
        "Modal: ${activeSession.openingCash.toRupiah()}"
    } else {
        "Klik untuk buka sesi kas"
    }

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OperationalCard(
                    modifier = Modifier.weight(1f),
                    title = "Penjualan Hari Ini",
                    value = todayRevenue,
                    subtitle = "Omzet transaksi kasir tercatat",
                    icon = Icons.Outlined.Payments,
                    iconTint = BrandGreen,
                    iconBg = BrandGreenLight,
                    onClick = onTransactionsClick
                )
                OperationalCard(
                    modifier = Modifier.weight(1f),
                    title = "Sesi Kasir",
                    value = sessionTitle,
                    subtitle = sessionSubtitle,
                    icon = Icons.Outlined.Storefront,
                    iconTint = BlueText,
                    iconBg = BlueBg,
                    onClick = onCashClick
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OperationalCard(
                    modifier = Modifier.weight(1f),
                    title = "Stok Kritis",
                    value = "$lowStockCount Produk",
                    subtitle = "Di bawah batas minimum buffer",
                    icon = Icons.Outlined.Inventory2,
                    iconTint = RoseText,
                    iconBg = RoseBg,
                    onClick = onStockClick
                )
                OperationalCard(
                    modifier = Modifier.weight(1f),
                    title = "Piutang Pelanggan",
                    value = activeReceivables,
                    subtitle = receivablesCount,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = AmberText,
                    iconBg = AmberBg,
                    onClick = onReceivablesClick
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OperationalCard(
                modifier = Modifier.weight(1f),
                title = "Penjualan Hari Ini",
                value = todayRevenue,
                subtitle = "Omzet transaksi kasir tercatat",
                icon = Icons.Outlined.Payments,
                iconTint = BrandGreen,
                iconBg = BrandGreenLight,
                onClick = onTransactionsClick
            )
            OperationalCard(
                modifier = Modifier.weight(1f),
                title = "Sesi Kasir",
                value = sessionTitle,
                subtitle = sessionSubtitle,
                icon = Icons.Outlined.Storefront,
                iconTint = BlueText,
                iconBg = BlueBg,
                onClick = onCashClick
            )
            OperationalCard(
                modifier = Modifier.weight(1f),
                title = "Stok Kritis",
                value = "$lowStockCount Produk",
                subtitle = "Di bawah batas minimum buffer",
                icon = Icons.Outlined.Inventory2,
                iconTint = RoseText,
                iconBg = RoseBg,
                onClick = onStockClick
            )
            OperationalCard(
                modifier = Modifier.weight(1f),
                title = "Piutang Pelanggan",
                value = activeReceivables,
                subtitle = receivablesCount,
                icon = Icons.Outlined.AccountBalanceWallet,
                iconTint = AmberText,
                iconBg = AmberBg,
                onClick = onReceivablesClick
            )
        }
    }
}

@Composable
private fun OperationalCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate500,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. LOW STOCK BUFFER ACTION PANEL
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun LowStockActionPanel(
    lowStockItems: List<StockLowSummaryDto>,
    totalLowStockCount: Long,
    onManageStockClick: () -> Unit,
    onReceiveGoodsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).background(RoseBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = RoseText, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Peringatan Stok Menipis ($totalLowStockCount)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Text(
                    text = "Buka Gudang →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandGreen,
                    modifier = Modifier.clickable(onClick = onManageStockClick)
                )
            }

            Spacer(Modifier.height(12.dp))

            lowStockItems.take(4).forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = SlateBorder.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = item.productName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Batas minimum: ${item.minStock} unit",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoseBg,
                        border = BorderStroke(1.dp, RoseText.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Sisa: ${item.quantity}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).background(BrandGreenLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Receipt, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                    }
                    Text("Transaksi Terakhir Kasir", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
                Text(
                    text = "Semua Transaksi →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandGreen,
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }

            Spacer(Modifier.height(12.dp))

            transactions.take(5).forEachIndexed { index, trx ->
                if (index > 0) HorizontalDivider(color = SlateBorder.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = trx.receiptId.takeLast(12),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900,
                                fontFamily = FontFamily.Monospace
                            )
                            val methodText = trx.paymentMethods.firstOrNull()?.uppercase() ?: "TUNAI"
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = methodText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        val buyer = if (!trx.customerName.isNullOrBlank()) trx.customerName else "Pelanggan Umum"
                        val cashier = if (!trx.cashierName.isNullOrBlank()) trx.cashierName else "Kasir"
                        Text(
                            text = "$buyer • Kasir: $cashier",
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = trx.total.toRupiah(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = trx.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (trx.status.equals("Lunas", ignoreCase = true)) BrandGreen else AmberText
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// FORMATTER UTILITIES
// ═════════════════════════════════════════════════════════════════════════════
private fun Double.toRupiah(): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "Rp " + formatter.format(this)
}

private fun BigDecimal.toRupiah(): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "Rp " + formatter.format(this)
}
