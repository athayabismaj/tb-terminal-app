package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.SystemRepository
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource

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
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            systemRepository,
            localAppSettingsDataSource,
            loadRemoteSettings = false,
        )
    )
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val handleBack: () -> Unit = {
        if (onBack != null) {
            onBack()
        } else {
            onDashboardClick()
        }
    }

    SharedSettingsScreen(
        role = role,
        selectedPage = SettingsPage.DEVICE,
        onPageSelected = {},
        onPageBack = handleBack,
        uiState = uiState,
        showHeader = true,
        headerTitle = "Pengaturan",
        onBack = handleBack,
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
        onPrinterConnectionTypeChanged = viewModel::onPrinterConnectionTypeChanged,
        onPrinterDeviceSelected = viewModel::onPrinterDeviceSelected,
        onAutoCutPaperChanged = viewModel::onAutoCutPaperChanged,
        onOpenCashDrawerChanged = viewModel::onOpenCashDrawerChanged,
        onScannerSoundFeedbackChanged = viewModel::onScannerSoundFeedbackChanged,
        onScannerVibrateFeedbackChanged = viewModel::onScannerVibrateFeedbackChanged,
        onScanPrinters = { viewModel.scanPrinters(context) },
        onTestPrint = { viewModel.testPrint(context) },
        onTestCashDrawer = { viewModel.testCashDrawer(context) },
        onDismissMessage = viewModel::clearMessage,
    )
}
