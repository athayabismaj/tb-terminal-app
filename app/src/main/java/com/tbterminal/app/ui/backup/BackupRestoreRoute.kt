package com.tbterminal.app.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.LocalBackupRepository
import com.tbterminal.app.data.repository.ServerBackupRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

@Composable
fun BackupRestoreRoute(
    name: String,
    role: String,
    localBackupRepository: LocalBackupRepository,
    serverBackupRepository: ServerBackupRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = onReportsClick,
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: BackupRestoreViewModel = viewModel(
        factory = BackupRestoreViewModel.factory(localBackupRepository, serverBackupRepository, role)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.BackupRestore,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onSalesTransactionsClick = onSalesTransactionsClick,
        onReportsClick = onReportsClick,
        onLocalReportsClick = onLocalReportsClick,
        onSyncCenterClick = onSyncCenterClick,
        onBackupRestoreClick = onBackupRestoreClick,
        onPriceManagementClick = onPriceManagementClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isServerLoading && uiState.serverJobs.isNotEmpty(),
            onRefresh = {
                viewModel.refreshSummary()
                viewModel.refreshServerBackups()
            },
            modifier = contentModifier,
        ) {
            BackupRestoreScreen(
            modifier = androidx.compose.ui.Modifier,
            uiState = uiState,
            onCreateBackup = viewModel::createBackup,
            onRestoreFileSelected = viewModel::inspectRestoreFile,
            onConfirmRestore = viewModel::confirmRestore,
            onDismissRestore = viewModel::dismissRestoreDialog,
            onDismissMessage = viewModel::clearMessage,
            canManageServerBackup = canManageServerDatabaseBackup(role),
            onRefreshServerBackups = viewModel::refreshServerBackups,
            onCreateServerBackup = viewModel::createServerBackup,
            onDownloadServerBackup = viewModel::downloadServerBackup,
            onServerRestoreFileSelected = viewModel::validateServerRestore,
            onServerRestorePhraseChanged = viewModel::onServerRestorePhraseChanged,
            onServerRestoreAcknowledgedChanged = viewModel::onServerRestoreAcknowledgedChanged,
            onConfirmServerRestore = viewModel::confirmServerRestore,
            onDismissServerRestore = viewModel::dismissServerRestore
            )
        }
    }
}
