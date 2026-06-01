package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardInfoBlue
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange

@Composable
fun AdminDashboardScreen(
    name: String,
    role: String,
    analyticsRepository: AnalyticsRepository,
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
    onSuppliersClick: () -> Unit = onIncomingGoodsClick,
    onPurchaseHistoryClick: () -> Unit = onIncomingGoodsClick,
    onStockReportClick: () -> Unit = onReportsClick,
    onSupplierDebtsClick: () -> Unit,
    onCashSessionHistoryClick: () -> Unit = onCashReconciliationClick,
    onCashReconciliationDetailClick: () -> Unit = onCashReconciliationClick,
    onCashExpensesClick: () -> Unit = onCashReconciliationClick,
    onReceivablesClick: () -> Unit,
    onReceivablePaymentsClick: () -> Unit = onReceivablesClick,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModel.factory(analyticsRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Dashboard,
        onDashboardClick = {},
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
        onSuppliersClick = onSuppliersClick,
        onPurchaseHistoryClick = onPurchaseHistoryClick,
        onStockReportClick = onStockReportClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onCashSessionHistoryClick = onCashSessionHistoryClick,
        onCashReconciliationDetailClick = onCashReconciliationDetailClick,
        onCashExpensesClick = onCashExpensesClick,
        onReceivablesClick = onReceivablesClick,
        onReceivablePaymentsClick = onReceivablePaymentsClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        AdminDashboardContent(
            uiState = uiState,
            modifier = contentModifier.padding(32.dp)
        )
    }
}

@Composable
private fun AdminDashboardContent(
    uiState: AdminDashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text(
                text = "Dashboard Admin",
                color = DashboardTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Fokus pada kontrol inventory, pembelian, dan laporan operasional.",
                color = DashboardTextSecondary,
                fontSize = 14.sp
            )
        }

        val moneyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"))
        val revenueTodayStr = uiState.metrics?.totalRevenueToday?.let { moneyFormat.format(it) } ?: "Rp 0"
        val activeReceivablesStr = uiState.metrics?.totalActiveReceivables?.let { moneyFormat.format(it) } ?: "Rp 0"

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                title = "PENDAPATAN HARI INI",
                value = revenueTodayStr,
                subtitle = "Total transaksi berjalan",
                icon = Icons.Outlined.Payments,
                tint = DashboardBrandGreenDark
            )
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                title = "PIUTANG AKTIF",
                value = activeReceivablesStr,
                subtitle = "${uiState.metrics?.activeReceivableCount ?: 0} nota belum lunas",
                icon = Icons.Outlined.AccountBalanceWallet,
                tint = DashboardWarningOrange
            )
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                title = "LOW STOCK SKU",
                value = "${uiState.metrics?.lowStockCount ?: 0} SKU",
                subtitle = "Perlu direstock",
                icon = Icons.Outlined.Inventory2,
                tint = DashboardInfoBlue
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Card(
                modifier = Modifier
                    .weight(1.5f)
                    .height(360.dp),
                colors = CardDefaults.cardColors(containerColor = DashboardSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("Prioritas Operasional", color = DashboardTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    
                    if (uiState.metrics?.lowStockItems?.isNotEmpty() == true) {
                        uiState.metrics.lowStockItems.take(3).forEach { item ->
                            AdminTaskItem(
                                icon = Icons.Outlined.Warning, 
                                title = "Stok menipis: ${item.productName}", 
                                description = "Sisa stok: ${item.quantity} (Minimum: ${item.minStock}). Segera buat pesanan ke supplier.", 
                                tint = DashboardWarningOrange
                            )
                        }
                    } else {
                        Text("Tidak ada prioritas operasional saat ini.", color = DashboardTextSecondary, fontSize = 14.sp)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(360.dp),
                colors = CardDefaults.cardColors(containerColor = DashboardSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Kesiapan Toko", color = DashboardTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(18.dp))
                    AdminProgress("Inventory valid", 0.78f)
                    AdminProgress("Supplier tertangani", 0.64f)
                    AdminProgress("Kas harian", 0.88f)
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(156.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Column {
                Text(title, color = DashboardTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = DashboardTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AdminTaskItem(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = DashboardTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = DashboardTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AdminProgress(
    label: String,
    progress: Float
) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = DashboardTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${(progress * 100).toInt()}%", color = DashboardTextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = DashboardBrandGreenDark,
            trackColor = DashboardBrandGreen.copy(alpha = 0.16f)
        )
    }
}
