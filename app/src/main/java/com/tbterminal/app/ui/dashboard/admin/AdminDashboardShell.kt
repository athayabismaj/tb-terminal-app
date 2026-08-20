package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardSidebar
import com.tbterminal.app.ui.dashboard.owner.OwnerDestination

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
    onLocalReportsClick: () -> Unit = onReportsClick,
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = onSyncCenterClick,
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
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
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
        if (role.equals("owner", ignoreCase = true)) {
            OwnerDashboardSidebar(
                activeDestination = activeDestination.toOwnerDestination(),
                onDashboardClick = navigateOrFallback(AdminDestination.Dashboard, onDashboardClick),
                onReportsClick = navigateOrFallback(AdminDestination.Reports, onReportsClick),
                onLocalReportsClick = navigateOrFallback(AdminDestination.LocalReports, onLocalReportsClick),
                onSyncCenterClick = navigateOrFallback(AdminDestination.SyncCenter, onSyncCenterClick),
                onBackupRestoreClick = navigateOrFallback(AdminDestination.BackupRestore, onBackupRestoreClick),
                onStockReportClick = navigateOrFallback(AdminDestination.StockReport, onStockReportClick),
                onReceivablesClick = navigateOrFallback(AdminDestination.Receivables, onReceivablesClick),
                onSupplierDebtsClick = navigateOrFallback(AdminDestination.SupplierDebts, onSupplierDebtsClick),
                onCashReconciliationClick = navigateOrFallback(AdminDestination.CashReconciliation, onCashReconciliationClick),
                onOperationalAuditClick = navigateOrFallback(AdminDestination.OperationalAudit, onOperationalAuditClick),
                onUserManagementClick = onUserManagementClick,
                onSecurityLogClick = onSecurityLogClick,
                onSettingsClick = navigateOrFallback(AdminDestination.Settings, onSettingsClick),
                onLogout = onLogout,
                userName = userName,
                role = role,
                modifier = Modifier.width(260.dp)
            )
        } else {
            TbTerminalSidebar(
                userName = userName,
                role = role,
                activeDestination = activeDestination,
                onDashboardClick = navigateOrFallback(AdminDestination.Dashboard, onDashboardClick),
                onProductsClick = navigateOrFallback(AdminDestination.Products, onProductsClick),
                onProductCategoriesClick = navigateOrFallback(AdminDestination.ProductCategories, onProductCategoriesClick),
                onProductUnitsClick = navigateOrFallback(AdminDestination.ProductUnits, onProductUnitsClick),
                onPriceManagementClick = navigateOrFallback(AdminDestination.PriceManagement, onPriceManagementClick),
                onStockOpnameClick = navigateOrFallback(AdminDestination.StockOpname, onStockOpnameClick),
                onStockReportClick = navigateOrFallback(AdminDestination.StockReport, onStockReportClick),
                onSuppliersClick = navigateOrFallback(AdminDestination.Suppliers, onSuppliersClick),
                onIncomingGoodsClick = navigateOrFallback(AdminDestination.IncomingGoods, onIncomingGoodsClick),
                onPurchaseHistoryClick = navigateOrFallback(AdminDestination.PurchaseHistory, onPurchaseHistoryClick),
                onCustomersClick = navigateOrFallback(AdminDestination.Customers, onCustomersClick),
                onReceivablesClick = navigateOrFallback(AdminDestination.Receivables, onReceivablesClick),
                onReceivablePaymentsClick = navigateOrFallback(AdminDestination.ReceivablePayments, onReceivablePaymentsClick),
                onCashReconciliationClick = navigateOrFallback(AdminDestination.CashReconciliation, onCashReconciliationClick),
                onCashSessionHistoryClick = navigateOrFallback(AdminDestination.CashSessionHistory, onCashSessionHistoryClick),
                onCashExpensesClick = navigateOrFallback(AdminDestination.CashExpenses, onCashExpensesClick),
                onSalesTransactionsClick = navigateOrFallback(AdminDestination.SalesTransactions, onSalesTransactionsClick),
                onSupplierDebtsClick = navigateOrFallback(AdminDestination.SupplierDebts, onSupplierDebtsClick),
                onReportsClick = navigateOrFallback(AdminDestination.Reports, onReportsClick),
                onLocalReportsClick = navigateOrFallback(AdminDestination.LocalReports, onLocalReportsClick),
                onSyncCenterClick = navigateOrFallback(AdminDestination.SyncCenter, onSyncCenterClick),
                onBackupRestoreClick = navigateOrFallback(AdminDestination.BackupRestore, onBackupRestoreClick),
                onOperationalAuditClick = navigateOrFallback(AdminDestination.OperationalAudit, onOperationalAuditClick),
                onProfileClick = navigateOrFallback(AdminDestination.Profile, onProfileClick),
                onLogout = onLogout,
                modifier = Modifier.width(266.dp)
            )
        }
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
    LocalReports,
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
    SyncCenter,
    BackupRestore,
    Profile,
    Settings
}

private fun AdminDestination.toOwnerDestination(): OwnerDestination = when (this) {
    AdminDestination.Reports -> OwnerDestination.Reports
    AdminDestination.LocalReports -> OwnerDestination.LocalReports
    AdminDestination.StockReport -> OwnerDestination.StockReport
    AdminDestination.Receivables,
    AdminDestination.ReceivablePayments,
    AdminDestination.Customers,
    AdminDestination.CustomerForm -> OwnerDestination.Receivables
    AdminDestination.SupplierDebts -> OwnerDestination.SupplierDebts
    AdminDestination.CashReconciliation,
    AdminDestination.CashSessionHistory,
    AdminDestination.CashReconciliationDetail,
    AdminDestination.CashExpenses,
    AdminDestination.SalesTransactions -> OwnerDestination.CashReconciliation
    AdminDestination.OperationalAudit -> OwnerDestination.OperationalAudit
    AdminDestination.SyncCenter -> OwnerDestination.SyncCenter
    AdminDestination.BackupRestore -> OwnerDestination.BackupRestore
    AdminDestination.Settings -> OwnerDestination.Settings
    else -> OwnerDestination.Dashboard
}
