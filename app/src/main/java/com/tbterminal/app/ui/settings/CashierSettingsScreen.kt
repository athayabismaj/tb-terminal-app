package com.tbterminal.app.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.SystemRepository
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination

@Composable
fun CashierSettingsScreen(
    userName: String,
    role: String,
    systemRepository: SystemRepository,
    localAppSettingsDataSource: LocalAppSettingsDataSource,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(systemRepository, localAppSettingsDataSource)
    )
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    CashierDashboardShell(
        userName = userName,
        role = role,
        activeDestination = CashierDestination.Settings,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        SharedSettingsScreen(
            userName = userName,
            role = role,
            uiState = uiState,
            onReload = viewModel::loadSettings,
            onSaveStoreSettings = viewModel::saveStoreSettings,
            onSaveLocalPreferences = viewModel::saveLocalPreferences,
            onStoreNameChanged = viewModel::onStoreNameChanged,
            onAddressChanged = viewModel::onAddressChanged,
            onPhoneChanged = viewModel::onPhoneChanged,
            onReceiptHeaderChanged = viewModel::onReceiptHeaderChanged,
            onReceiptFooterChanged = viewModel::onReceiptFooterChanged,
            onPrinterSizeChanged = viewModel::onPrinterSizeChanged,
            onDefaultCreditLimitChanged = viewModel::onDefaultCreditLimitChanged,
            onDefaultTermDaysChanged = viewModel::onDefaultTermDaysChanged,
            onCashToleranceChanged = viewModel::onCashToleranceChanged,
            onAutoLockMinutesChanged = viewModel::onAutoLockMinutesChanged,
            onAutoPrintReceiptChanged = viewModel::onAutoPrintReceiptChanged,
            onBarcodeScannerChanged = viewModel::onBarcodeScannerChanged,
            onOfflineCacheChanged = viewModel::onOfflineCacheChanged,
            onSelectPrinter = {
                if (launchAndroidPrintDialog(context, uiState.printerSize)) {
                    viewModel.onPrinterFrameworkOpened()
                } else {
                    viewModel.onPrinterFrameworkFailed()
                }
            },
            modifier = contentModifier.padding(32.dp)
        )
    }
}
