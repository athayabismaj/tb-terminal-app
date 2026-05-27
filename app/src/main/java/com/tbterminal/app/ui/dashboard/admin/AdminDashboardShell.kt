package com.tbterminal.app.ui.dashboard.admin

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

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
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {
        AdminNavigationSidebar(
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
            onSupplierDebtsClick = onSupplierDebtsClick,
            onReceivablesClick = onReceivablesClick,
            onCustomersClick = onCustomersClick,
            onOperationalAuditClick = onOperationalAuditClick,
            onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
            onLogout = onLogout,
            modifier = Modifier.width(260.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            AdminDashboardHeader(userName = userName, role = role)
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
    SupplierDebts,
    Receivables,
    Customers,
    CustomerForm,
    OperationalAudit,
    Profile,
    Settings
}

@Composable
private fun AdminNavigationSidebar(
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
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
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
        AdminDestination.IncomingGoodsForm -> true
        else -> false
    }
    val isCustomerSectionActive = when (activeDestination) {
        AdminDestination.Customers,
        AdminDestination.CustomerForm,
        AdminDestination.Receivables -> true
        else -> false
    }
    val isFinanceSectionActive = when (activeDestination) {
        AdminDestination.CashReconciliation,
        AdminDestination.SalesTransactions,
        AdminDestination.SupplierDebts -> true
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
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    text = "Daftar Barang Masuk",
                    isActive = activeDestination == AdminDestination.IncomingGoods,
                    onClick = onIncomingGoodsClick
                )
                ProductSubNavigationItem(
                    icon = Icons.Outlined.LocalShipping,
                    text = "Form Barang Masuk",
                    isActive = activeDestination == AdminDestination.IncomingGoodsForm,
                    onClick = onIncomingGoodsFormClick
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
                    text = "Laporan",
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
            AdminNavigationItem(
                icon = Icons.Outlined.Settings,
                text = "Pengaturan",
                isActive = activeDestination == AdminDestination.Settings,
                onClick = onSettingsClick
            )
        }

        AdminNavigationItem(
            icon = Icons.AutoMirrored.Outlined.Logout,
            text = "Keluar",
            tint = DashboardTextSecondary,
            onClick = onLogout
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

@Composable
private fun AdminDashboardHeader(
    userName: String,
    role: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashboardSurface)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Cari produk, stok, pembelian...", color = DashboardTextSecondary, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = DashboardTextSecondary)
            },
            modifier = Modifier
                .width(420.dp)
                .height(50.dp),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = DashboardBrandGreenDark,
                unfocusedContainerColor = DashboardBackground,
                focusedContainerColor = DashboardBackground
            )
        )

        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Notifications, contentDescription = "Notifikasi", tint = DashboardTextSecondary)
        Spacer(modifier = Modifier.width(32.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(userName, color = DashboardTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("DASHBOARD ${role.uppercase()}", color = DashboardTextSecondary, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DashboardBrandGreen.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "A",
                color = DashboardBrandGreenDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
