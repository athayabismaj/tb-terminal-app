package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tbterminal.app.R
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.repository.SystemRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminSettingsScreen(
    name: String,
    role: String,
    systemRepository: SystemRepository,
    localAppSettingsDataSource: LocalAppSettingsDataSource,
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
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(systemRepository, localAppSettingsDataSource)
    )
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var selectedPage by rememberSaveable { mutableStateOf<SettingsPage?>(null) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Settings,
        pageTitle = selectedPage?.title ?: stringResource(R.string.owner_menu_settings),
        onBack = selectedPage?.let { { selectedPage = null } },
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
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        SharedSettingsScreen(
            role = role,
            selectedPage = selectedPage,
            onPageSelected = { selectedPage = it },
            onPageBack = { selectedPage = null },
            uiState = uiState,
            onReload = viewModel::reload,
            onSaveStoreSettings = viewModel::saveStoreSettings,
            onSaveLocalPreferences = viewModel::saveLocalPreferences,
            onStoreNameChanged = viewModel::onStoreNameChanged,
            onAddressChanged = viewModel::onAddressChanged,
            onPhoneChanged = viewModel::onPhoneChanged,
            onReceiptHeaderChanged = viewModel::onReceiptHeaderChanged,
            onReceiptFooterChanged = viewModel::onReceiptFooterChanged,
            onPrinterSizeChanged = viewModel::onPrinterSizeChanged,
            onCashToleranceChanged = viewModel::onCashToleranceChanged,
            onAutoLockMinutesChanged = viewModel::onAutoLockMinutesChanged,
            onAutoPrintReceiptChanged = viewModel::onAutoPrintReceiptChanged,
            onSelectPrinter = {
                if (launchAndroidPrintDialog(context, uiState.printerSize)) {
                    viewModel.onPrinterFrameworkOpened()
                } else {
                    viewModel.onPrinterFrameworkFailed()
                }
            },
            modifier = contentModifier
        )
    }
}
