package com.tbterminal.app.ui.dashboard.admin

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
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardViewModel

@Composable
fun AdminDashboardScreen(
    name: String,
    role: String,
    analyticsRepository: AnalyticsRepository,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    offlineDashboardRepository: OfflineDashboardRepository,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onNewTransactionClick: () -> Unit = onSalesTransactionsClick,
    onReportsClick: () -> Unit,
    onLocalReportsClick: () -> Unit = onReportsClick,
    onSyncCenterClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSuppliersClick: () -> Unit = onIncomingGoodsClick,
    onPurchaseHistoryClick: () -> Unit = onIncomingGoodsClick,
    onStockReportClick: () -> Unit = onReportsClick,
    onSupplierDebtsClick: () -> Unit,
    onCashSessionHistoryClick: () -> Unit = onCashReconciliationClick,
    onCashReconciliationDetailClick: () -> Unit = onCashReconciliationClick,
    onCashExpensesClick: () -> Unit = onCashReconciliationClick,
    onReceivablesClick: () -> Unit,
    onReceivablePaymentsClick: () -> Unit = onReceivablesClick,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModel.factory(
            analyticsRepository = analyticsRepository,
            cashReconciliationRepository = cashReconciliationRepository,
            includeFinancialSummary = false
        )
    ),
    offlineDashboardViewModel: OfflineDashboardViewModel = viewModel(
        factory = OfflineDashboardViewModel.factory(offlineDashboardRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val offlineUiState by offlineDashboardViewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Dashboard,
        onDashboardClick = {},
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onSalesTransactionsClick = onSalesTransactionsClick,
        onReportsClick = onReportsClick,
        onLocalReportsClick = onLocalReportsClick,
        onSyncCenterClick = onSyncCenterClick,
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
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            TopFloatingCapsule(
                message = uiState.error,
                isError = true,
                onDismiss = viewModel::clearError
            )

            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.metrics != null,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                AdminOperationalDashboardContent(
                    adminName = name,
                    metrics = uiState.metrics,
                    activeCashierSession = uiState.activeCashierSession,
                    recentTransactions = uiState.recentTransactions,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    offlineUiState = offlineUiState,
                    onIncomingGoodsClick = onIncomingGoodsClick,
                    onStockClick = onProductsClick,
                    onStockOpnameClick = onStockOpnameClick,
                    onCashExpensesClick = onCashExpensesClick,
                    onCashClick = onCashReconciliationClick,
                    onTransactionsClick = onSalesTransactionsClick,
                    onReceivablesClick = onReceivablesClick,
                    onSupplierDebtsClick = onSupplierDebtsClick,
                    onSyncCenterClick = onSyncCenterClick,
                    modifier = Modifier
                )
            }
        }
    }
}
