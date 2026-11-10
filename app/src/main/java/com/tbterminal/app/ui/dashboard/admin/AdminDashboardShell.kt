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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
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
    onStoreSettingsClick: () -> Unit = onSettingsClick,
    onDeviceSettingsClick: () -> Unit = onSettingsClick,
    pageTitle: String? = null,
    onBack: (() -> Unit)? = null,
    showPageHeader: Boolean = true,
    topBarActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
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
        val cashierDestination = activeDestination.toCashierDestination()
        CashierDashboardShell(
            userName = userName,
            role = role,
            activeDestination = cashierDestination,
            pageTitle = pageTitle,
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
            topBarActions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topBarActions()
                }
            },
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
        onSettingsClick = navigateOrFallback(AdminDestination.Settings, onSettingsClick),
        onSyncCenterClick = navigateOrFallback(AdminDestination.SyncCenter, onSyncCenterClick),
        onReportsClick = navigateOrFallback(AdminDestination.Reports, onReportsClick),
        onLogout = onLogout,
        pageTitle = pageTitle ?: activeDestination.backofficePageTitleRes()?.let { stringResource(it) }
            ?: activeDestination.backofficeHeaderTitle(role),
        pageSubtitle = activeDestination.backofficeHeaderSubtitle(role),
        onBack = onBack,
        topBarActions = topBarActions,
        showPageHeader = showPageHeader,
    ) { contentModifier ->
        content(contentModifier)
    }
}

internal fun AdminDestination.toCashierDestination(): CashierDestination = when (this) {
    AdminDestination.NewTransaction -> CashierDestination.Pos
    AdminDestination.CashierCashSession,
    AdminDestination.CashReconciliation,
    AdminDestination.CashSessionHistory,
    AdminDestination.CashReconciliationDetail,
    AdminDestination.CashExpenses -> CashierDestination.CashSession
    AdminDestination.CashierTransactionHistory,
    AdminDestination.SalesTransactions -> CashierDestination.TransactionHistory
    AdminDestination.Receivables,
    AdminDestination.OpeningReceivableForm,
    AdminDestination.AdjustmentReceivableForm -> CashierDestination.Receivables
    AdminDestination.ReceivablePayments -> CashierDestination.ReceivablePayments
    AdminDestination.Customers,
    AdminDestination.CustomerForm -> CashierDestination.Customers
    AdminDestination.Profile -> CashierDestination.Profile
    AdminDestination.Settings,
    AdminDestination.DeviceSettings -> CashierDestination.Settings
    else -> CashierDestination.Dashboard
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
    OpeningSupplierDebtForm,
    AdjustmentSupplierDebtForm,
    CashSessionHistory,
    CashReconciliationDetail,
    CashExpenses,
    Receivables,
    ReceivablePayments,
    OpeningReceivableForm,
    AdjustmentReceivableForm,
    Customers,
    CustomerForm,
    OperationalAudit,
    SyncCenter,
    BackupRestore,
    UserManagement,
    SecurityLog,
    Profile,
    Settings,
    StoreSettings,
    DeviceSettings
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
    AdminDestination.StoreSettings,
    AdminDestination.DeviceSettings,
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
    AdminDestination.PurchaseHistory,
    AdminDestination.IncomingGoods,
    AdminDestination.IncomingGoodsForm -> BackofficeSection.TRANSACTIONS
    AdminDestination.FinanceHub,
    AdminDestination.Receivables,
    AdminDestination.ReceivablePayments,
    AdminDestination.OpeningReceivableForm,
    AdminDestination.AdjustmentReceivableForm,
    AdminDestination.SupplierDebts,
    AdminDestination.OpeningSupplierDebtForm,
    AdminDestination.AdjustmentSupplierDebtForm,
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
    AdminDestination.OpeningReceivableForm -> "Saldo awal piutang"
    AdminDestination.AdjustmentReceivableForm -> "Penyesuaian piutang"
    AdminDestination.ReceivablePayments -> "Pembayaran piutang"
    AdminDestination.SupplierDebts -> "Hutang supplier"
    AdminDestination.OpeningSupplierDebtForm -> "Saldo awal hutang"
    AdminDestination.AdjustmentSupplierDebtForm -> "Penyesuaian hutang"
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
    AdminDestination.StoreSettings -> "Identitas toko"
    AdminDestination.DeviceSettings -> "Perangkat & struk"
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
    AdminDestination.StoreSettings -> R.string.owner_menu_store_settings
    AdminDestination.DeviceSettings -> R.string.owner_menu_device_settings
    AdminDestination.SyncCenter -> R.string.owner_menu_sync
    AdminDestination.OperationalAudit -> R.string.owner_menu_activity
    AdminDestination.SecurityLog -> R.string.owner_menu_security_log
    else -> null
}

internal fun AdminDestination.backofficeHeaderTitle(role: String? = null): String? {
    val owner = isOwnerPersona(role)
    return when (this) {
        AdminDestination.Dashboard -> if (owner) "Dashboard Eksekutif" else "Dashboard Operasional"
        AdminDestination.TransactionsHub -> "Pusat Transaksi"
        AdminDestination.FinanceHub -> "Kelola Keuangan"
        AdminDestination.StockHub -> "Manajemen Stok"
        AdminDestination.MoreHub -> if (owner) "Menu Pemilik" else "Menu Lainnya"
        else -> null
    } ?: backofficePageTitle()
}

internal fun AdminDestination.backofficeHeaderSubtitle(role: String? = null): String {
    val owner = isOwnerPersona(role)
    return when (this) {
        AdminDestination.Dashboard -> if (owner) "Pemilik · TB Barokah Jaya" else "Admin · TB Barokah Jaya"
        AdminDestination.TransactionsHub -> if (owner) "Monitoring penjualan & pembelian" else "Penjualan & barang masuk toko"
        AdminDestination.FinanceHub -> "Arus kas, piutang & hutang supplier"
        AdminDestination.StockHub -> "Inventaris & valuasi gudang"
        AdminDestination.MoreHub -> if (owner) "Kendali toko, pengguna & laporan" else "Pengaturan, laporan & akun"
        AdminDestination.SalesTransactions -> if (owner) "Monitoring transaksi penjualan" else "Riwayat & detail nota penjualan"
        AdminDestination.PurchaseHistory -> "Riwayat faktur & pembelian supplier"
        AdminDestination.IncomingGoods, AdminDestination.IncomingGoodsForm -> "Penerimaan & input stok masuk"
        AdminDestination.Receivables,
        AdminDestination.OpeningReceivableForm,
        AdminDestination.AdjustmentReceivableForm -> "Tagihan & jatuh tempo pelanggan"
        AdminDestination.ReceivablePayments -> "Riwayat cicilan & pelunasan"
        AdminDestination.SupplierDebts,
        AdminDestination.OpeningSupplierDebtForm,
        AdminDestination.AdjustmentSupplierDebtForm -> "Kewajiban & pembayaran supplier"
        AdminDestination.CashReconciliation,
        AdminDestination.CashReconciliationDetail,
        AdminDestination.CashSessionHistory,
        AdminDestination.CashExpenses -> "Shift kasir & rekonsiliasi kas"
        AdminDestination.Products, AdminDestination.AddProduct -> "Katalog & ketersediaan stok"
        AdminDestination.ProductCategories, AdminDestination.ProductUnits -> "Master data katalog produk"
        AdminDestination.PriceManagement -> "Atur harga jual & margin produk"
        AdminDestination.StockOpname, AdminDestination.StockOpnameForm -> "Pencocokan stok fisik gudang"
        AdminDestination.StockReport -> "Mutasi keluar-masuk barang"
        AdminDestination.Customers, AdminDestination.CustomerForm -> "Data & batas kredit pelanggan"
        AdminDestination.Suppliers, AdminDestination.SupplierForm -> "Data & kontak supplier toko"
        AdminDestination.Reports, AdminDestination.LocalReports -> if (owner) "Analitik & rekap performa toko" else "Rekap operasional toko"
        AdminDestination.UserManagement -> "Akun kasir, admin & hak akses"
        AdminDestination.OperationalAudit -> "Jejak perubahan data operasional"
        AdminDestination.SecurityLog -> "Riwayat login & aktivitas keamanan"
        AdminDestination.SyncCenter -> "Antrean & sinkronisasi data lokal"
        AdminDestination.BackupRestore -> "Cadangan & pemulihan database"
        AdminDestination.StoreSettings -> "Identitas toko, alamat & struk"
        AdminDestination.DeviceSettings -> "Printer thermal, toleransi & kunci layar"
        AdminDestination.Settings -> "Konfigurasi toko & perangkat"
        AdminDestination.Profile -> "Informasi akun & keamanan"
        else -> if (owner) "Pemilik · TB Barokah Jaya" else "Admin · TB Barokah Jaya"
    }
}
