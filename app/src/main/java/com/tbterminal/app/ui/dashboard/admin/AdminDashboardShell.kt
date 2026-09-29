package com.tbterminal.app.ui.dashboard.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.tbterminal.app.R
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.dashboard.isOwnerPersona
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability

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
    showPageHeader: Boolean = true,
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
            BackofficeSection.MENU -> navigateOrFallback(AdminDestination.MoreHub, onSettingsClick)()
        }
    }

    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) {
        val cashierDestination = when (activeDestination) {
            AdminDestination.Receivables,
            AdminDestination.Receivables -> CashierDestination.Receivables
            AdminDestination.ReceivablePayments -> CashierDestination.ReceivablePayments
            AdminDestination.Customers,
            AdminDestination.CustomerForm -> CashierDestination.Customers
            else -> CashierDestination.Dashboard
        }
        CashierDashboardShell(
            userName = userName,
            role = role,
            activeDestination = cashierDestination,
            onDashboardClick = navigateOrFallback(AdminDestination.Dashboard, onDashboardClick),
            onPosClick = navigateOrFallback(AdminDestination.NewTransaction, onSalesTransactionsClick),
            onCashSessionClick = navigateOrFallback(AdminDestination.CashierCashSession, onCashReconciliationClick),
            onTransactionHistoryClick = navigateOrFallback(AdminDestination.CashierTransactionHistory, onSalesTransactionsClick),
            onReceivablesClick = navigateOrFallback(AdminDestination.Receivables, onReceivablesClick),
            onCustomersClick = navigateOrFallback(AdminDestination.Customers, onCustomersClick),
            onReceivablePaymentsClick = navigateOrFallback(AdminDestination.ReceivablePayments, onReceivablePaymentsClick),
            onProfileClick = navigateOrFallback(AdminDestination.Profile, onProfileClick),
            onSettingsClick = navigateOrFallback(AdminDestination.Settings, onSettingsClick),
            onLogout = onLogout,
            content = content,
        )
        return
    }

    BackofficeAdaptiveShell(
        userName = userName,
        role = role,
        activeSection = activeDestination.backofficeSection(role),
        onSectionSelected = ::selectSection,
        onProfileClick = navigateOrFallback(AdminDestination.Profile, onProfileClick),
        onLogout = onLogout,
        pageTitle = pageTitle ?: activeDestination.backofficePageTitleRes()?.let { stringResource(it) }
            ?: activeDestination.backofficePageTitle(),
        onBack = onBack,
        showPageHeader = showPageHeader && !activeDestination.isBackofficeRootDestination(),
    ) { contentModifier ->
        content(contentModifier)
    }
}

internal fun AdminDestination.isBackofficeRootDestination(): Boolean = when (this) {
    AdminDestination.Dashboard,
    AdminDestination.TransactionsHub,
    AdminDestination.FinanceHub,
    AdminDestination.StockHub,
    AdminDestination.MoreHub -> true
    else -> false
}

enum class AdminDestination {
    Dashboard,
    TransactionsHub,
    FinanceHub,
    StockHub,
    MoreHub,
    NewTransaction,
    CashierCashSession,
    CashierTransactionHistory,
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
    SupplierForm,
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
    UserManagement,
    SecurityLog,
    Profile,
    Settings
}

private val ownerMenuDestinations = setOf(
    AdminDestination.MoreHub,
    AdminDestination.Customers,
    AdminDestination.CustomerForm,
    AdminDestination.Suppliers,
    AdminDestination.SupplierForm,
    AdminDestination.Reports,
    AdminDestination.LocalReports,
    AdminDestination.BackupRestore,
    AdminDestination.Settings,
    AdminDestination.SyncCenter,
    AdminDestination.OperationalAudit,
    AdminDestination.UserManagement,
    AdminDestination.SecurityLog,
    AdminDestination.Profile,
)

/** Single source of truth for destination-to-primary-section mapping. */
internal fun AdminDestination.backofficeSection(role: String? = null): BackofficeSection {
    if (isOwnerPersona(role) && this in ownerMenuDestinations) return BackofficeSection.MENU
    return when (this) {
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
}

/** Child pages name the actual task, not the navigation group. Hub/account titles come from the shell. */
internal fun AdminDestination.backofficePageTitle(): String? = when (this) {
    AdminDestination.Dashboard, AdminDestination.TransactionsHub, AdminDestination.FinanceHub,
    AdminDestination.StockHub, AdminDestination.MoreHub -> null
    AdminDestination.Profile -> "Profil"
    AdminDestination.SalesTransactions, AdminDestination.CashierTransactionHistory -> "Penjualan"
    AdminDestination.PurchaseHistory -> "Pembelian"
    AdminDestination.Products -> "Produk"
    AdminDestination.AddProduct -> "Tambah produk"
    AdminDestination.ProductCategories -> "Kategori"
    AdminDestination.ProductUnits -> "Satuan"
    AdminDestination.PriceManagement -> "Harga produk"
    AdminDestination.StockOpname, AdminDestination.StockOpnameForm -> "Sesuaikan stok"
    AdminDestination.IncomingGoods, AdminDestination.IncomingGoodsForm -> "Barang masuk"
    AdminDestination.StockReport -> "Kartu stok"
    AdminDestination.Suppliers -> "Supplier"
    AdminDestination.SupplierForm -> "Form supplier"
    AdminDestination.Customers, AdminDestination.CustomerForm -> "Pelanggan"
    AdminDestination.Receivables -> "Piutang pelanggan"
    AdminDestination.ReceivablePayments -> "Pembayaran piutang"
    AdminDestination.SupplierDebts -> "Hutang supplier"
    AdminDestination.CashReconciliation, AdminDestination.CashierCashSession -> "Kas harian"
    AdminDestination.CashReconciliationDetail -> "Cocokkan kas"
    AdminDestination.CashSessionHistory -> "Riwayat kas"
    AdminDestination.CashExpenses -> "Pengeluaran kas"
    AdminDestination.Reports -> "Laporan"
    AdminDestination.LocalReports -> "Laporan lokal"
    AdminDestination.OperationalAudit -> "Riwayat aktivitas"
    AdminDestination.UserManagement -> "Pengguna & akses"
    AdminDestination.SecurityLog -> "Log keamanan"
    AdminDestination.SyncCenter -> "Sinkronisasi"
    AdminDestination.BackupRestore -> "Cadangan data"
    AdminDestination.Settings -> "Pengaturan aplikasi"
    AdminDestination.NewTransaction -> "Transaksi baru"
}

@StringRes
internal fun AdminDestination.backofficePageTitleRes(): Int? = when (this) {
    AdminDestination.Profile -> R.string.profile_title
    AdminDestination.UserManagement -> R.string.owner_menu_users
    AdminDestination.Reports -> R.string.owner_menu_reports
    AdminDestination.LocalReports -> R.string.owner_menu_device_reports
    AdminDestination.BackupRestore -> R.string.owner_menu_backup
    AdminDestination.Settings -> R.string.owner_menu_settings
    AdminDestination.SyncCenter -> R.string.owner_menu_sync
    AdminDestination.OperationalAudit -> R.string.owner_menu_activity
    AdminDestination.SecurityLog -> R.string.owner_menu_security_log
    else -> null
}
