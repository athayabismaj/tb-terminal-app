package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminSettingsScreen(
    name: String,
    role: String,
    onDashboardClick: () -> Unit = {},
    onProductsClick: () -> Unit = {},
    onAddProductClick: () -> Unit = {},
    onProductCategoriesClick: () -> Unit = {},
    onProductUnitsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit = {},
    onStockOpnameFormClick: () -> Unit = {},
    onIncomingGoodsClick: () -> Unit = {},
    onIncomingGoodsFormClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Settings,
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
        onLogout = onLogout
    ) { contentModifier ->
        SharedSettingsScreen(userName = name, role = role, modifier = contentModifier)
    }
}
