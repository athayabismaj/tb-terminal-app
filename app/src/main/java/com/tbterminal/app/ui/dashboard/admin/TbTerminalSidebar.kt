package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.offline.OfflineStatusIndicatorHost
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbSurface

private val SidebarBackground = TbSurface
private val SidebarTextPrimary = Color(0xFF111111)
private val SidebarTextSecondary = Color(0xFF6B7378)
private val SidebarLine = Color(0xFFD8E0E4)
private val SidebarTeal = TbGreen
private val SidebarSelected = TbGreenLight

@Composable
internal fun TbTerminalSidebar(
    userName: String,
    role: String,
    activeDestination: AdminDestination,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockReportClick: () -> Unit,
    onSuppliersClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onPurchaseHistoryClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onReceivablePaymentsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onCashSessionHistoryClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onLocalReportsClick: () -> Unit = onReportsClick,
    onSyncCenterClick: () -> Unit,
    onBackupRestoreClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProductsActive = activeDestination in productDestinations
    val isStockActive = activeDestination in stockDestinations
    val isCustomersActive = activeDestination in customerDestinations
    val isFinanceActive = activeDestination in financeDestinations
    val isReportsActive = activeDestination in reportDestinations
    var productsExpanded by rememberSaveable { mutableStateOf(isProductsActive) }
    var stockExpanded by rememberSaveable { mutableStateOf(isStockActive) }
    var customersExpanded by rememberSaveable { mutableStateOf(isCustomersActive) }
    var financeExpanded by rememberSaveable { mutableStateOf(isFinanceActive) }
    var reportsExpanded by rememberSaveable { mutableStateOf(isReportsActive) }

    LaunchedEffect(activeDestination) {
        productsExpanded = isProductsActive
        stockExpanded = isStockActive
        customersExpanded = isCustomersActive
        financeExpanded = isFinanceActive
        reportsExpanded = isReportsActive
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SidebarBackground)
    ) {
        SidebarBrand()
        Text(
            text = "ADMIN OPERASIONAL",
            color = SidebarTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 27.dp, top = 30.dp, bottom = 14.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            SidebarItem(
                title = "Dashboard",
                icon = Icons.Outlined.Home,
                selected = activeDestination == AdminDestination.Dashboard,
                onClick = onDashboardClick
            )
            SidebarExpandableItem(
                title = "Produk",
                icon = Icons.Outlined.Inventory2,
                selected = isProductsActive,
                expanded = productsExpanded,
                onClick = { productsExpanded = !productsExpanded }
            )
            if (productsExpanded) {
                SidebarSubMenu(
                    items = listOf(
                        SidebarEntry("Daftar Produk", Icons.AutoMirrored.Outlined.ListAlt, activeDestination == AdminDestination.Products, onProductsClick),
                        SidebarEntry("Kategori Produk", Icons.Outlined.Category, activeDestination == AdminDestination.ProductCategories, onProductCategoriesClick),
                        SidebarEntry("Satuan Produk", Icons.Outlined.Straighten, activeDestination == AdminDestination.ProductUnits, onProductUnitsClick),
                        SidebarEntry("Manajemen Harga", Icons.Outlined.AttachMoney, activeDestination == AdminDestination.PriceManagement, onPriceManagementClick)
                    )
                )
            }
            SidebarExpandableItem(
                title = "Stok & Pembelian",
                icon = Icons.Outlined.ShoppingCart,
                selected = isStockActive,
                expanded = stockExpanded,
                onClick = { stockExpanded = !stockExpanded }
            )
            if (stockExpanded) {
                SidebarSubMenu(
                    items = listOf(
                        SidebarEntry("Stok Opname", Icons.Outlined.AssignmentTurnedIn, activeDestination == AdminDestination.StockOpname, onStockOpnameClick),
                        SidebarEntry("Laporan Stok", Icons.Outlined.GridView, activeDestination == AdminDestination.StockReport, onStockReportClick),
                        SidebarEntry("Supplier", Icons.Outlined.Group, activeDestination == AdminDestination.Suppliers, onSuppliersClick),
                        SidebarEntry("Restok Barang", Icons.Outlined.Inventory2, activeDestination == AdminDestination.IncomingGoods, onIncomingGoodsClick),
                        SidebarEntry("Nota Pembelian", Icons.AutoMirrored.Outlined.ReceiptLong, activeDestination == AdminDestination.PurchaseHistory, onPurchaseHistoryClick)
                    )
                )
            }
            SidebarExpandableItem(
                title = "Pelanggan & Piutang",
                icon = Icons.Outlined.Groups,
                selected = isCustomersActive,
                expanded = customersExpanded,
                onClick = { customersExpanded = !customersExpanded }
            )
            if (customersExpanded) {
                SidebarSubMenu(
                    items = listOf(
                        SidebarEntry("Pelanggan", Icons.Outlined.Groups, activeDestination == AdminDestination.Customers || activeDestination == AdminDestination.CustomerForm, onCustomersClick),
                        SidebarEntry("Piutang", Icons.Outlined.Payments, activeDestination == AdminDestination.Receivables, onReceivablesClick),
                        SidebarEntry("Riwayat Pembayaran Piutang", Icons.AutoMirrored.Outlined.ReceiptLong, activeDestination == AdminDestination.ReceivablePayments, onReceivablePaymentsClick)
                    )
                )
            }
            SidebarExpandableItem(
                title = "Keuangan",
                icon = Icons.Outlined.CreditCard,
                selected = isFinanceActive,
                expanded = financeExpanded,
                onClick = { financeExpanded = !financeExpanded }
            )
            if (financeExpanded) {
                SidebarSubMenu(
                    items = listOf(
                        SidebarEntry("Kas Harian", Icons.Outlined.Payments, activeDestination == AdminDestination.CashReconciliation, onCashReconciliationClick),
                        SidebarEntry("Riwayat Kas Harian", Icons.AutoMirrored.Outlined.ListAlt, activeDestination == AdminDestination.CashSessionHistory, onCashSessionHistoryClick),
                        SidebarEntry("Pengeluaran Kas", Icons.Outlined.AttachMoney, activeDestination == AdminDestination.CashExpenses, onCashExpensesClick),
                        SidebarEntry("Riwayat Transaksi", Icons.AutoMirrored.Outlined.ReceiptLong, activeDestination == AdminDestination.SalesTransactions, onSalesTransactionsClick),
                        SidebarEntry("Utang Supplier", Icons.AutoMirrored.Outlined.ReceiptLong, activeDestination == AdminDestination.SupplierDebts, onSupplierDebtsClick)
                    )
                )
            }
            SidebarExpandableItem(
                title = "Laporan & Audit",
                icon = Icons.Outlined.Description,
                selected = isReportsActive,
                expanded = reportsExpanded,
                onClick = { reportsExpanded = !reportsExpanded }
            )
            if (reportsExpanded) {
                SidebarSubMenu(
                    items = listOf(
                        SidebarEntry("Laporan Analitik", Icons.Outlined.GridView, activeDestination == AdminDestination.Reports, onReportsClick),
                        SidebarEntry("Laporan Lokal", Icons.Outlined.Assessment, activeDestination == AdminDestination.LocalReports, onLocalReportsClick),
                        SidebarEntry("Sinkronisasi & Konflik", Icons.Outlined.Sync, activeDestination == AdminDestination.SyncCenter, onSyncCenterClick),
                        SidebarEntry("Audit Operasional", Icons.Outlined.AssignmentTurnedIn, activeDestination == AdminDestination.OperationalAudit, onOperationalAuditClick)
                    )
                )
            }
        }
        OfflineStatusIndicatorHost(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = SidebarLine)
        SidebarProfileSection(
            userName = userName,
            role = role,
            onProfileClick = onProfileClick,
            onLogout = onLogout
        )
    }
}

private val productDestinations = setOf(
    AdminDestination.Products,
    AdminDestination.AddProduct,
    AdminDestination.ProductCategories,
    AdminDestination.ProductUnits,
    AdminDestination.PriceManagement
)

private val stockDestinations = setOf(
    AdminDestination.StockOpname,
    AdminDestination.StockOpnameForm,
    AdminDestination.IncomingGoods,
    AdminDestination.IncomingGoodsForm,
    AdminDestination.Suppliers,
    AdminDestination.SupplierForm,
    AdminDestination.PurchaseHistory,
    AdminDestination.StockReport
)

private val customerDestinations = setOf(
    AdminDestination.Customers,
    AdminDestination.CustomerForm,
    AdminDestination.Receivables,
    AdminDestination.ReceivablePayments
)

private val financeDestinations = setOf(
    AdminDestination.CashReconciliation,
    AdminDestination.SalesTransactions,
    AdminDestination.SupplierDebts,
    AdminDestination.CashSessionHistory,
    AdminDestination.CashReconciliationDetail,
    AdminDestination.CashExpenses
)

private val reportDestinations = setOf(
    AdminDestination.Reports,
    AdminDestination.LocalReports,
    AdminDestination.SyncCenter,
    AdminDestination.BackupRestore,
    AdminDestination.OperationalAudit
)

private data class SidebarEntry(
    val title: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun SidebarBrand() {
    Row(
        modifier = Modifier.padding(start = 27.dp, top = 28.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFE6ECEF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TB",
                color = SidebarTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = "Terminal",
            color = SidebarTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SidebarItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) SidebarTeal else SidebarTextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(13.dp))
        Text(title, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SidebarExpandableItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) SidebarTeal else SidebarTextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = SidebarTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SidebarSubMenu(items: List<SidebarEntry>) {
    Row(modifier = Modifier.padding(start = 22.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp)
                .width(1.dp)
                .height((items.size * 40).dp)
                .background(SidebarLine)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.selected) SidebarSelected else Color.Transparent)
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (item.selected) SidebarTeal else SidebarTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        color = if (item.selected) SidebarTeal else SidebarTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarProfileSection(
    userName: String,
    role: String,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            ) {
                SidebarProfileAction(Icons.Outlined.Person, "Profil", SidebarTextPrimary) {
                    expanded = false
                    onProfileClick()
                }
                HorizontalDivider(color = SidebarLine)
                SidebarProfileAction(Icons.AutoMirrored.Outlined.Logout, "Keluar", Color(0xFFDC2626)) {
                    expanded = false
                    onLogout()
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6ECEF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "A",
                    color = SidebarTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    color = SidebarTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role.uppercase(),
                    color = SidebarTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = SidebarTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SidebarProfileAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
