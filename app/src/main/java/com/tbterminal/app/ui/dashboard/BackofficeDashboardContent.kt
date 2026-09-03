package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardSection
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import com.tbterminal.app.ui.components.SkeletonCard
import java.text.NumberFormat
import java.util.Locale

private data class DashboardSummary(
    val title: String,
    val value: String,
    val supportingText: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

internal enum class DashboardSummaryKey {
    SALES,
    NET_REVENUE,
    REFUND,
    DISCOUNT,
    RECEIVABLES,
    PAYABLES,
    CASH,
    STOCK,
}

internal fun dashboardSummaryKeys(role: String): List<DashboardSummaryKey> = buildList {
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) return@buildList
    add(DashboardSummaryKey.SALES)
    if (AppAccessPolicy.can(role, AppCapability.FINANCIAL_ANALYTICS)) {
        add(DashboardSummaryKey.NET_REVENUE)
        add(DashboardSummaryKey.REFUND)
        add(DashboardSummaryKey.DISCOUNT)
    }
    add(DashboardSummaryKey.RECEIVABLES)
    add(DashboardSummaryKey.PAYABLES)
    if (!AppAccessPolicy.can(role, AppCapability.FINANCIAL_ANALYTICS)) {
        add(DashboardSummaryKey.CASH)
    }
    add(DashboardSummaryKey.STOCK)
}

@Composable
fun BackofficeDashboardContent(
    role: String,
    metrics: DashboardMetricsDto?,
    financialTotals: SalesReportTotalsDto? = null,
    isLoading: Boolean,
    error: String?,
    offlineUiState: OfflineDashboardUiState,
    onNewTransactionClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onCashClick: () -> Unit,
    onStockClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSyncCenterClick: () -> Unit,
    showNewTransactionAction: Boolean = true,
    showOfflineDeviceSummary: Boolean = true,
    modifier: Modifier = Modifier
) {
    TbPageSurface(modifier = modifier) { layout, pageModifier ->
        Column(
            modifier = pageModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(layout.verticalSpacing)
        ) {
            DashboardHeader(layout, onNewTransactionClick, showNewTransactionAction)

            if (error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = "Ringkasan online belum dapat dimuat. $error",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
            val loadingText = if (isLoading) "Memuat…" else "Rp0"
            val financialLoadingText = if (isLoading) "Memuat…" else "Tidak tersedia"
            val summaries = dashboardSummaryKeys(role).map { key ->
                when (key) {
                    DashboardSummaryKey.SALES -> DashboardSummary(
                        "Penjualan hari ini",
                        metrics?.totalRevenueToday?.let(currency::format) ?: loadingText,
                        "Transaksi aktif hari ini",
                        Icons.Outlined.Payments,
                        MaterialTheme.colorScheme.primary,
                        onTransactionsClick,
                    )
                    DashboardSummaryKey.NET_REVENUE -> DashboardSummary(
                        "Pendapatan bersih",
                        financialTotals?.netRevenue?.let(currency::format) ?: financialLoadingText,
                        "Setelah diskon dan refund hari ini",
                        Icons.Outlined.Storefront,
                        Color(0xFF047857),
                        onReportsClick,
                    )
                    DashboardSummaryKey.REFUND -> DashboardSummary(
                        "Refund",
                        financialTotals?.refundAmount?.let(currency::format) ?: financialLoadingText,
                        "Nilai refund hari ini",
                        Icons.AutoMirrored.Outlined.ReceiptLong,
                        Color(0xFFB42318),
                        onReportsClick,
                    )
                    DashboardSummaryKey.DISCOUNT -> DashboardSummary(
                        "Diskon",
                        financialTotals?.discountAmount?.let(currency::format) ?: financialLoadingText,
                        "Diskon transaksi aktif hari ini",
                        Icons.Outlined.Payments,
                        Color(0xFF7C3AED),
                        onReportsClick,
                    )
                    DashboardSummaryKey.RECEIVABLES -> DashboardSummary(
                        "Piutang pelanggan",
                        metrics?.totalActiveReceivables?.let(currency::format) ?: loadingText,
                        "${metrics?.activeReceivableCount ?: 0} nota belum lunas",
                        Icons.Outlined.AccountBalanceWallet,
                        Color(0xFFB26A00),
                        onReceivablesClick,
                    )
                    DashboardSummaryKey.PAYABLES -> DashboardSummary(
                        "Hutang supplier",
                        "Lihat rincian",
                        "Periksa sisa dan jatuh tempo",
                        Icons.AutoMirrored.Outlined.ReceiptLong,
                        Color(0xFF8B5CF6),
                        onSupplierDebtsClick,
                    )
                    DashboardSummaryKey.CASH -> DashboardSummary(
                        "Sesi kas",
                        "Pantau sesi",
                        "Saldo awal, masuk, keluar, dan rekonsiliasi",
                        Icons.Outlined.Storefront,
                        Color(0xFF047857),
                        onCashClick,
                    )
                    DashboardSummaryKey.STOCK -> DashboardSummary(
                        "Stok menipis",
                        "${metrics?.lowStockCount ?: 0} produk",
                        "Stok sama atau di bawah batas minimum",
                        Icons.Outlined.Inventory2,
                        Color(0xFFC2410C),
                        onStockClick,
                    )
                }
            }
            if (isLoading && metrics == null) {
                repeat(if (layout.isCompact) 4 else 2) { SkeletonCard() }
            } else {
                DashboardSummaryGrid(layout, summaries)
            }

            if (showOfflineDeviceSummary) {
                OfflineDashboardSection(uiState = offlineUiState, onSyncCenterClick = onSyncCenterClick)
            }

            val secondaryColumns = if (layout.isCompact) 1 else 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardSimplePanel(
                    modifier = Modifier.weight(1f),
                    title = "Perlu diperiksa",
                    icon = Icons.Outlined.Warning,
                    lines = buildList {
                        add("Piutang dan hutang yang mendekati jatuh tempo")
                        metrics?.lowStockItems?.take(3)?.forEach { add("Stok ${it.productName}: ${it.quantity}") }
                        if (size == 1) add("Tidak ada peringatan stok tambahan")
                    },
                    onClick = onReceivablesClick
                )
                if (secondaryColumns > 1) {
                    DashboardSimplePanel(
                        modifier = Modifier.weight(1f),
                        title = "Transaksi terakhir",
                        icon = Icons.Outlined.Schedule,
                        lines = listOf("Buka daftar penjualan dan pembelian terbaru", "Cari berdasarkan nomor, pelanggan, kasir, atau status"),
                        onClick = onTransactionsClick
                    )
                }
            }
            if (secondaryColumns == 1) {
                DashboardSimplePanel(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Transaksi terakhir",
                    icon = Icons.Outlined.Schedule,
                    lines = listOf("Buka daftar penjualan dan pembelian terbaru", "Cari berdasarkan nomor, pelanggan, kasir, atau status"),
                    onClick = onTransactionsClick
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    layout: TbLayoutInfo,
    onNewTransactionClick: () -> Unit,
    showNewTransactionAction: Boolean
) {
    if (layout.isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardTitle()
            if (showNewTransactionAction) {
                Button(onClick = onNewTransactionClick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("+ Transaksi Baru")
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DashboardTitle()
            if (showNewTransactionAction) {
                Button(onClick = onNewTransactionClick, modifier = Modifier.height(48.dp)) { Text("+ Transaksi Baru") }
            }
        }
    }
}

@Composable
private fun DashboardTitle() {
    Column {
        Text("Beranda", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ringkasan pekerjaan toko hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardSummaryGrid(layout: TbLayoutInfo, items: List<DashboardSummary>) {
    val columns = when {
        layout.isCompact -> 1
        layout.isExpanded -> 3
        else -> 2
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { summary ->
                    Card(
                        onClick = summary.onClick,
                        modifier = Modifier.weight(1f).height(144.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(summary.icon, contentDescription = null, tint = summary.tint, modifier = Modifier.size(24.dp))
                            Text(summary.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(summary.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(summary.supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DashboardSimplePanel(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    lines: List<String>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            lines.forEach { line ->
                Text("• $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
