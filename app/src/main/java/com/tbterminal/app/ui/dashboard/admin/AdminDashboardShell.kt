package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardSidebar
import com.tbterminal.app.ui.dashboard.owner.OwnerDestination
import com.tbterminal.app.ui.components.TbCompactDashboardTopBar
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import kotlinx.coroutines.launch

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
    pageTitle: String? = null,
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateOrFallback(destination: AdminDestination, fallback: () -> Unit): () -> Unit = {
        destinationNavigator?.invoke(destination) ?: fallback()
    }

    fun selectSection(section: BackofficeSection) {
        when (section) {
            BackofficeSection.HOME -> navigateOrFallback(AdminDestination.Dashboard, onDashboardClick)()
            BackofficeSection.TRANSACTIONS -> navigateOrFallback(AdminDestination.TransactionsHub, onSalesTransactionsClick)()
            BackofficeSection.FINANCE -> navigateOrFallback(AdminDestination.FinanceHub, onReceivablesClick)()
            BackofficeSection.STOCK -> navigateOrFallback(AdminDestination.StockHub, onProductsClick)()
            BackofficeSection.MORE -> navigateOrFallback(AdminDestination.MoreHub, onSettingsClick)()
        }
    }

    BackofficeAdaptiveShell(
        userName = userName,
        role = role,
        activeSection = activeDestination.backofficeSection(),
        onSectionSelected = ::selectSection,
        onProfileClick = navigateOrFallback(AdminDestination.Profile, onProfileClick),
        onLogout = onLogout,
        pageTitle = pageTitle,
        onBack = onBack
    ) { contentModifier ->
        content(contentModifier)
    }
}

enum class AdminDestination {
    Dashboard,
    TransactionsHub,
    FinanceHub,
    StockHub,
    MoreHub,
    NewTransaction,
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

internal fun AdminDestination.backofficeSection(): BackofficeSection = when (this) {
    AdminDestination.Dashboard -> BackofficeSection.HOME
    AdminDestination.TransactionsHub,
    AdminDestination.NewTransaction,
    AdminDestination.SalesTransactions,
    AdminDestination.PurchaseHistory -> BackofficeSection.TRANSACTIONS
    AdminDestination.FinanceHub,
    AdminDestination.Receivables,
    AdminDestination.ReceivablePayments,
    AdminDestination.SupplierDebts,
    AdminDestination.CashReconciliation,
    AdminDestination.CashSessionHistory,
    AdminDestination.CashReconciliationDetail,
    AdminDestination.CashExpenses -> BackofficeSection.FINANCE
    AdminDestination.StockHub,
    AdminDestination.Products,
    AdminDestination.AddProduct,
    AdminDestination.ProductCategories,
    AdminDestination.ProductUnits,
    AdminDestination.PriceManagement,
    AdminDestination.StockOpname,
    AdminDestination.StockOpnameForm,
    AdminDestination.IncomingGoods,
    AdminDestination.IncomingGoodsForm,
    AdminDestination.StockReport -> BackofficeSection.STOCK
    else -> BackofficeSection.MORE
}

private fun AdminDestination.toOwnerDestination(): OwnerDestination = when (this) {
    AdminDestination.TransactionsHub -> OwnerDestination.Dashboard
    AdminDestination.NewTransaction -> OwnerDestination.Dashboard
    AdminDestination.FinanceHub -> OwnerDestination.CashReconciliation
    AdminDestination.StockHub -> OwnerDestination.StockReport
    AdminDestination.MoreHub -> OwnerDestination.Settings
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
