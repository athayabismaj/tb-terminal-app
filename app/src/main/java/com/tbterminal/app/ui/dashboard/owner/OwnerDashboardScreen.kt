package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.OfflineDashboardRepository
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardViewModel
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardViewModel

@Composable
fun OwnerDashboardScreen(
    name: String,
    role: String,
    analyticsRepository: AnalyticsRepository,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    offlineDashboardRepository: OfflineDashboardRepository,
    onLogout: () -> Unit,
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = onReportsClick,
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onNewTransactionClick: () -> Unit = onSalesTransactionsClick,
    onProductsClick: () -> Unit = onStockReportClick,
    onOperationalAuditClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = onSyncCenterClick,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    dashboardViewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModel.factory(
            analyticsRepository = analyticsRepository,
            cashReconciliationRepository = cashReconciliationRepository,
            includeFinancialSummary = true,
        )
    ),
    offlineDashboardViewModel: OfflineDashboardViewModel = viewModel(
        factory = OfflineDashboardViewModel.factory(offlineDashboardRepository)
    )
) {
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val offlineUiState by offlineDashboardViewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Dashboard,
        onProductsClick = onProductsClick,
        onCustomersClick = {},
        onSalesTransactionsClick = onSalesTransactionsClick,
        onDashboardClick = {},
        onReportsClick = onReportsClick,
        onLocalReportsClick = onLocalReportsClick,
        onStockReportClick = onStockReportClick,
        onReceivablesClick = onReceivablesClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onSyncCenterClick = onSyncCenterClick,
        onBackupRestoreClick = onBackupRestoreClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            TopFloatingCapsule(
                message = dashboardUiState.error,
                isError = true,
                onDismiss = dashboardViewModel::clearError
            )

            RefreshableContent(
                isRefreshing = dashboardUiState.isLoading && dashboardUiState.metrics != null,
                onRefresh = dashboardViewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                OwnerExecutiveDashboardContent(
                    ownerName = name,
                    metrics = dashboardUiState.metrics,
                    financialTotals = dashboardUiState.financialTotals,
                    salesReport = dashboardUiState.fullSalesReport,
                    dailySalesTrend = dashboardUiState.dailySalesTrend,
                    selectedPeriodDays = dashboardUiState.selectedPeriodDays,
                    onPeriodSelected = dashboardViewModel::setPeriod,
                    isLoading = dashboardUiState.isLoading,
                    error = dashboardUiState.error,
                    offlineUiState = offlineUiState,
                    onTransactionsClick = onSalesTransactionsClick,
                    onReportsClick = onReportsClick,
                    onReceivablesClick = onReceivablesClick,
                    onSupplierDebtsClick = onSupplierDebtsClick,
                    onCashClick = onCashReconciliationClick,
                    onStockClick = onProductsClick,
                    onOperationalAuditClick = onOperationalAuditClick,
                    onSyncCenterClick = onSyncCenterClick,
                    modifier = Modifier
                )
            }
        }
    }
}
