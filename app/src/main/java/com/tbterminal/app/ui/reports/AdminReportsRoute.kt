package com.tbterminal.app.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.ReportCsvType

@Composable
fun AdminReportsRoute(
    name: String,
    role: String,
    analyticsRepository: AnalyticsRepository,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackToPrevious: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    val viewModel: AdminReportsViewModel = viewModel(
        factory = AdminReportsViewModel.factory(
            analyticsRepository = analyticsRepository,
            cashReconciliationRepository = cashReconciliationRepository
        )
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<ReportCsvType?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val type = pendingExport
        pendingExport = null
        if (uri != null && type != null) {
            val output = context.contentResolver.openOutputStream(uri)
            if (output != null) viewModel.exportCsv(type, output)
        }
    }

    AdminReportsScreen(
        name = name,
        role = role,
        uiState = uiState,
        onDateRangeChanged = viewModel::setDateRange,
        onRefresh = viewModel::loadReports,
        onRetry = viewModel::loadReports,
        onSalesReportRetry = viewModel::loadSalesReport,
        onTransactionsRetry = { viewModel.loadTransactions(uiState.transactionPage) },
        onPreviousTransactionPage = viewModel::previousTransactionPage,
        onNextTransactionPage = viewModel::nextTransactionPage,
        onExportCsv = { type ->
            if (!uiState.isExporting) {
                pendingExport = type
                exportLauncher.launch("${type.path}-${uiState.startDate}-${uiState.endDate}.csv")
            }
        },
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
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onBackToPrevious = onBackToPrevious,
        onLogout = onLogout
    )
}
