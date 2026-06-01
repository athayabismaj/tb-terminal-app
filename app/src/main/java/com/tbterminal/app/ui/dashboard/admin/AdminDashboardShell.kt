package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

internal val LocalAdminDestinationNavigator = staticCompositionLocalOf<((AdminDestination) -> Unit)?> { null }

@Composable
fun AdminDashboardShell(
    userName: String,
    role: String,
    activeDestination: AdminDestination,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit = onProductsClick,
    onProductCategoriesClick: () -> Unit = onProductsClick,
    onProductUnitsClick: () -> Unit = onProductsClick,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit = {},
    onStockOpnameFormClick: () -> Unit = onStockOpnameClick,
    onIncomingGoodsClick: () -> Unit = {},
    onIncomingGoodsFormClick: () -> Unit = onIncomingGoodsClick,
    onSuppliersClick: () -> Unit = onIncomingGoodsClick,
    onPurchaseHistoryClick: () -> Unit = onIncomingGoodsClick,
    onStockReportClick: () -> Unit = onReportsClick,
    onSupplierDebtsClick: () -> Unit = {},
    onCashSessionHistoryClick: () -> Unit = onCashReconciliationClick,
    onCashReconciliationDetailClick: () -> Unit = onCashReconciliationClick,
    onCashExpensesClick: () -> Unit = onCashReconciliationClick,
    onReceivablesClick: () -> Unit = {},
    onReceivablePaymentsClick: () -> Unit = onReceivablesClick,
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateOrFallback(destination: AdminDestination, fallback: () -> Unit): () -> Unit = {
        destinationNavigator?.invoke(destination) ?: fallback()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {
        AdminNavigationSidebar(
            userName = userName,
            role = role,
            activeDestination = activeDestination,
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
            onSuppliersClick = navigateOrFallback(AdminDestination.Suppliers, onSuppliersClick),
            onPurchaseHistoryClick = navigateOrFallback(AdminDestination.PurchaseHistory, onPurchaseHistoryClick),
            onStockReportClick = navigateOrFallback(AdminDestination.StockReport, onStockReportClick),
            onSupplierDebtsClick = onSupplierDebtsClick,
            onCashSessionHistoryClick = navigateOrFallback(AdminDestination.CashSessionHistory, onCashSessionHistoryClick),
            onCashReconciliationDetailClick = navigateOrFallback(AdminDestination.CashReconciliationDetail, onCashReconciliationDetailClick),
            onCashExpensesClick = navigateOrFallback(AdminDestination.CashExpenses, onCashExpensesClick),
            onReceivablesClick = onReceivablesClick,
            onReceivablePaymentsClick = navigateOrFallback(AdminDestination.ReceivablePayments, onReceivablePaymentsClick),
            onCustomersClick = onCustomersClick,
            onOperationalAuditClick = onOperationalAuditClick,
            onProfileClick = onProfileClick,
            onLogout = onLogout,
            modifier = Modifier.width(260.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content(Modifier.weight(1f))
        }
    }
}

enum class AdminDestination {
    Dashboard,
    CashReconciliation,
    SalesTransactions,
    Reports,
    Products,
    AddProduct,
    ProductCategories,
    ProductUnits,
    PriceManagement,
    StockOpname,
    StockOpnameForm,
    IncomingGoods,
    IncomingGoodsForm,
    Suppliers,
    PurchaseHistory,
    StockReport,
    SupplierDebts,
    CashSessionHistory,
    CashReconciliationDetail,
    CashExpenses,
    Receivables,
    ReceivablePayments,
    Customers,
    CustomerForm,
    OperationalAudit,
    Profile,
    Settings
}

@Composable
private fun AdminNavigationSidebar(
    userName: String,
    role: String,
    activeDestination: AdminDestination,
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
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProductSectionActive = when (activeDestination) {
        AdminDestination.Products,
        AdminDestination.AddProduct,
        AdminDestination.ProductCategories,
        AdminDestination.ProductUnits,
        AdminDestination.PriceManagement -> true
        else -> false
    }
    val isStockSectionActive = when (activeDestination) {
        AdminDestination.StockOpname,
        AdminDestination.StockOpnameForm,
        AdminDestination.IncomingGoods,
        AdminDestination.IncomingGoodsForm,
        AdminDestination.Suppliers,
        AdminDestination.PurchaseHistory,
        AdminDestination.StockReport -> true
        else -> false
    }
    val isCustomerSectionActive = when (activeDestination) {
        AdminDestination.Customers,
        AdminDestination.CustomerForm,
        AdminDestination.Receivables,
        AdminDestination.ReceivablePayments -> true
        else -> false
    }
    val isFinanceSectionActive = when (activeDestination) {
        AdminDestination.CashReconciliation,
        AdminDestination.SalesTransactions,
        AdminDestination.SupplierDebts,
        AdminDestination.CashSessionHistory,
        AdminDestination.CashReconciliationDetail,
        AdminDestination.CashExpenses -> true
        else -> false
    }
    val isReportSectionActive = when (activeDestination) {
        AdminDestination.Reports,
        AdminDestination.OperationalAudit -> true
        else -> false
    }
    var isProductMenuExpanded by rememberSaveable { mutableStateOf(isProductSectionActive) }
    var isStockMenuExpanded by rememberSaveable { mutableStateOf(isStockSectionActive) }
    var isCustomerMenuExpanded by rememberSaveable { mutableStateOf(isCustomerSectionActive) }
    var isFinanceMenuExpanded by rememberSaveable { mutableStateOf(isFinanceSectionActive) }
    var isReportMenuExpanded by rememberSaveable { mutableStateOf(isReportSectionActive) }

    LaunchedEffect(activeDestination) {
        if (isProductSectionActive) {
            isProductMenuExpanded = true
        }
        if (isStockSectionActive) {
            isStockMenuExpanded = true
        }
        if (isCustomerSectionActive) {
            isCustomerMenuExpanded = true
        }
        if (isFinanceSectionActive) {
            isFinanceMenuExpanded = true
        }
        if (isReportSectionActive) {
            isReportMenuExpanded = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DashboardSurface)
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "TB Terminal",
            color = DashboardBrandGreenDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        Text(
            text = "ADMIN OPERASIONAL",
            color = DashboardTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 48.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            AdminNavigationItem(
                icon = Icons.Outlined.GridView,
                text = "Dashboard",
                isActive = activeDestination == AdminDestination.Dashboard,
                onClick = onDashboardClick
            )
            AdminExpandableNavigationItem(
                icon = Icons.Outlined.Inventory2,
                text = "Produk",
                isActive = isProductSectionActive,
                isExpanded = isProductMenuExpanded,
                onClick = { isProductMenuExpanded = !isProductMenuExpanded }
            )
            if (isProductMenuExpanded) {
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    text = "Daftar Produk",
                    isActive = activeDestination == AdminDestination.Products,
                    onClick = onProductsClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.AddBox,
                    text = "Tambah Produk",
                    isActive = activeDestination == AdminDestination.AddProduct,
                    onClick = onAddProductClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Category,
                    text = "Kategori Produk",
                    isActive = activeDestination == AdminDestination.ProductCategories,
                    onClick = onProductCategoriesClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Straighten,
                    text = "Satuan Produk",
                    isActive = activeDestination == AdminDestination.ProductUnits,
                    onClick = onProductUnitsClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Payments,
                    text = "Manajemen Harga",
                    isActive = activeDestination == AdminDestination.PriceManagement,
                    onClick = onPriceManagementClick
                )
            }
            AdminExpandableNavigationItem(
                icon = Icons.Outlined.AssignmentTurnedIn,
                text = "Stok & Pembelian",
                isActive = isStockSectionActive,
                isExpanded = isStockMenuExpanded,
                onClick = { isStockMenuExpanded = !isStockMenuExpanded }
            )
            if (isStockMenuExpanded) {
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    text = "Daftar Stok",
                    isActive = activeDestination == AdminDestination.StockOpname,
                    onClick = onStockOpnameClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    text = "Form Penyesuaian",
                    isActive = activeDestination == AdminDestination.StockOpnameForm,
                    onClick = onStockOpnameFormClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.GridView,
                    text = "Laporan Stok",
                    isActive = activeDestination == AdminDestination.StockReport,
                    onClick = onStockReportClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Group,
                    text = "Supplier",
                    isActive = activeDestination == AdminDestination.Suppliers,
                    onClick = onSuppliersClick
                )
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    text = "Riwayat Barang Masuk",
                    isActive = activeDestination == AdminDestination.IncomingGoods,
                    onClick = onIncomingGoodsClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.LocalShipping,
                    text = "Form Barang Masuk",
                    isActive = activeDestination == AdminDestination.IncomingGoodsForm,
                    onClick = onIncomingGoodsFormClick
                )
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    text = "Nota Pembelian",
                    isActive = activeDestination == AdminDestination.PurchaseHistory,
                    onClick = onPurchaseHistoryClick
                )
            }
            AdminExpandableNavigationItem(
                icon = Icons.Outlined.Group,
                text = "Pelanggan & Piutang",
                isActive = isCustomerSectionActive,
                isExpanded = isCustomerMenuExpanded,
                onClick = { isCustomerMenuExpanded = !isCustomerMenuExpanded }
            )
            if (isCustomerMenuExpanded) {
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Group,
                    text = "Pelanggan",
                    isActive = activeDestination == AdminDestination.Customers ||
                        activeDestination == AdminDestination.CustomerForm,
                    onClick = onCustomersClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Payments,
                    text = "Piutang",
                    isActive = activeDestination == AdminDestination.Receivables,
                    onClick = onReceivablesClick
                )
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    text = "Pembayaran Piutang",
                    isActive = activeDestination == AdminDestination.ReceivablePayments,
                    onClick = onReceivablePaymentsClick
                )
            }
            AdminExpandableNavigationItem(
                icon = Icons.Outlined.Payments,
                text = "Keuangan",
                isActive = isFinanceSectionActive,
                isExpanded = isFinanceMenuExpanded,
                onClick = { isFinanceMenuExpanded = !isFinanceMenuExpanded }
            )
            if (isFinanceMenuExpanded) {
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Payments,
                    text = "Kas Harian",
                    isActive = activeDestination == AdminDestination.CashReconciliation,
                    onClick = onCashReconciliationClick
                )
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    text = "Riwayat Kas Harian",
                    isActive = activeDestination == AdminDestination.CashSessionHistory,
                    onClick = onCashSessionHistoryClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    text = "Detail Rekonsiliasi",
                    isActive = activeDestination == AdminDestination.CashReconciliationDetail,
                    onClick = onCashReconciliationDetailClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.Payments,
                    text = "Pengeluaran Kas",
                    isActive = activeDestination == AdminDestination.CashExpenses,
                    onClick = onCashExpensesClick
                )
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    text = "Riwayat Transaksi",
                    isActive = activeDestination == AdminDestination.SalesTransactions,
                    onClick = onSalesTransactionsClick
                )
                ProductSubNavigationItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    text = "Utang Supplier",
                    isActive = activeDestination == AdminDestination.SupplierDebts,
                    onClick = onSupplierDebtsClick
                )
            }
            AdminExpandableNavigationItem(
                icon = Icons.Outlined.GridView,
                text = "Laporan & Audit",
                isActive = isReportSectionActive,
                isExpanded = isReportMenuExpanded,
                onClick = { isReportMenuExpanded = !isReportMenuExpanded }
            )
            if (isReportMenuExpanded) {
                ProductSubNavigationItem(
                    icon = Icons.Outlined.GridView,
                    text = "Laporan Analitik",
                    isActive = activeDestination == AdminDestination.Reports,
                    onClick = onReportsClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    text = "Audit Operasional",
                    isActive = activeDestination == AdminDestination.OperationalAudit,
                    onClick = onOperationalAuditClick
                )
            }
        }

        AdminAccountPanel(
            userName = userName,
            role = role,
            onProfileClick = onProfileClick,
            onLogout = onLogout
        )
    }
}

@Composable
private fun AdminAccountPanel(
    userName: String,
    role: String,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                AdminAccountAction(
                    icon = Icons.Outlined.Person,
                    text = "Profil",
                    color = DashboardTextPrimary,
                    onClick = {
                        isExpanded = false
                        onProfileClick()
                    }
                )
                HorizontalDivider(color = Color(0xFFE2E8F0))
                AdminAccountAction(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    text = "Keluar",
                    color = Color(0xFFEF4444),
                    onClick = {
                        isExpanded = false
                        onLogout()
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isExpanded) DashboardBrandGreen.copy(alpha = 0.08f)
                    else Color.Transparent
                )
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DashboardBrandGreen.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "A",
                    color = DashboardBrandGreenDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    color = DashboardTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role,
                    color = DashboardTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = DashboardTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AdminAccountAction(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = if (color == Color(0xFFEF4444)) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun AdminExpandableNavigationItem(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isActive) DashboardBrandGreenDark else DashboardTextPrimary
    val backgroundColor = if (isActive) DashboardBrandGreen.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProductSubNavigationItem(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isActive) DashboardBrandGreenDark else DashboardTextSecondary
    val backgroundColor = if (isActive) DashboardBrandGreen.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AdminNavigationItem(
    icon: ImageVector,
    text: String,
    isActive: Boolean = false,
    tint: Color = DashboardTextPrimary,
    onClick: (() -> Unit)? = null
) {
    val contentColor = if (isActive) DashboardBrandGreenDark else tint
    val backgroundColor = if (isActive) DashboardBrandGreen.copy(alpha = 0.12f) else Color.Transparent
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

