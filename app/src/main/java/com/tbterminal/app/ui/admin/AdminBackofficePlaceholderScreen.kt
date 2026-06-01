package com.tbterminal.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminBackofficePlaceholderScreen(
    name: String,
    role: String,
    destination: AdminDestination,
    title: String,
    subtitle: String,
    badgeText: String,
    focusItems: List<String>,
    integrationNotes: List<String>,
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
    onSuppliersClick: () -> Unit,
    onPurchaseHistoryClick: () -> Unit,
    onStockReportClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onCashSessionHistoryClick: () -> Unit,
    onCashReconciliationDetailClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onReceivablePaymentsClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = destination,
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
        PlaceholderContent(
            modifier = contentModifier,
            title = title,
            subtitle = subtitle,
            badgeText = badgeText,
            icon = destination.icon(),
            focusItems = focusItems,
            integrationNotes = integrationNotes
        )
    }
}

@Composable
private fun PlaceholderContent(
    modifier: Modifier,
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    focusItems: List<String>,
    integrationNotes: List<String>
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdminPlaceholderBackground)
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(title, color = AdminPlaceholderText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = AdminPlaceholderMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(AdminPlaceholderPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = AdminPlaceholderPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(badgeText, color = AdminPlaceholderPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AdminPlaceholderLine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AdminPlaceholderPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AdminPlaceholderPrimary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fondasi halaman sudah tersedia", color = AdminPlaceholderText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Halaman ini sengaja tidak memakai data palsu. Berikutnya tinggal disambungkan ke endpoint backend dan ViewModel spesifik saat kontrak API final.",
                        color = AdminPlaceholderMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PlaceholderListCard(
                modifier = Modifier.weight(1f),
                title = "Data yang dibutuhkan",
                items = focusItems
            )
            PlaceholderListCard(
                modifier = Modifier.weight(1f),
                title = "Integrasi berikutnya",
                items = integrationNotes
            )
        }
    }
}

@Composable
private fun PlaceholderListCard(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AdminPlaceholderLine)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, color = AdminPlaceholderText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AdminPlaceholderPrimary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item, color = AdminPlaceholderMuted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

private fun AdminDestination.icon(): ImageVector {
    return when (this) {
        AdminDestination.CashReconciliation -> Icons.Outlined.Payments
        AdminDestination.CashSessionHistory -> Icons.AutoMirrored.Outlined.ReceiptLong
        AdminDestination.CashReconciliationDetail -> Icons.Outlined.AssignmentTurnedIn
        AdminDestination.CashExpenses -> Icons.Outlined.Payments
        AdminDestination.SalesTransactions -> Icons.AutoMirrored.Outlined.ReceiptLong
        AdminDestination.Reports -> Icons.Outlined.GridView
        AdminDestination.StockReport -> Icons.Outlined.GridView
        AdminDestination.PriceManagement -> Icons.Outlined.Payments
        AdminDestination.Suppliers -> Icons.Outlined.Group
        AdminDestination.PurchaseHistory -> Icons.AutoMirrored.Outlined.ReceiptLong
        AdminDestination.ReceivablePayments -> Icons.Outlined.Payments
        AdminDestination.OperationalAudit -> Icons.Outlined.AssignmentTurnedIn
        AdminDestination.Settings -> Icons.Outlined.Settings
        AdminDestination.IncomingGoods,
        AdminDestination.IncomingGoodsForm -> Icons.Outlined.LocalShipping
        else -> Icons.Outlined.GridView
    }
}

private val AdminPlaceholderBackground = Color(0xFFF4FAFD)
private val AdminPlaceholderText = Color(0xFF0F172A)
private val AdminPlaceholderMuted = Color(0xFF64748B)
private val AdminPlaceholderLine = Color(0xFFE2E8F0)
private val AdminPlaceholderPrimary = Color(0xFF059669)
