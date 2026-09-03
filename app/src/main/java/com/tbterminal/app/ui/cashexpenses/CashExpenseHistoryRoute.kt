package com.tbterminal.app.ui.cashexpenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

@Composable
fun AdminCashExpenseHistoryScreen(
    name: String,
    role: String,
    cashRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReportsClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onShowSessionDetail: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CashExpenseHistoryViewModel = viewModel(
        factory = CashExpenseHistoryViewModel.factory(cashRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.CashExpenses,
        pageTitle = "Pengeluaran Kas",
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onStockOpnameClick = onStockOpnameClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onLogout = onLogout
    ) { modifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.expenses.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = modifier,
        ) {
            CashExpenseHistoryScreen(
            modifier = androidx.compose.ui.Modifier,
            uiState = uiState,
            onSearchChanged = viewModel::setSearchQuery,
            onRefresh = viewModel::refresh,
            onDateChanged = viewModel::setDate,
            onDatePresetSelected = viewModel::setDatePreset,
            onPreviousDate = viewModel::previousDate,
            onNextDate = viewModel::nextDate,
            onShowSessionDetail = onShowSessionDetail,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage
            )
        }
    }
}
