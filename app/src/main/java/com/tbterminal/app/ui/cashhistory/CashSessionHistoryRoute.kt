package com.tbterminal.app.ui.cashhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

@Composable
fun AdminCashSessionHistoryScreen(
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
    onShowDetail: (String) -> Unit,
    onBack: () -> Unit = {},
    onLogout: () -> Unit,
    activeDestination: AdminDestination = AdminDestination.CashSessionHistory,
    title: String = "Riwayat Kas Harian",
    viewModel: CashSessionHistoryViewModel = viewModel(
        factory = CashSessionHistoryViewModel.factory(cashRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = activeDestination,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onStockOpnameClick = onStockOpnameClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        pageTitle = title,
        onBack = onBack,
        onLogout = onLogout
    ) { modifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.sessions.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = modifier,
        ) {
            CashSessionHistoryScreen(
            modifier = androidx.compose.ui.Modifier,
            uiState = uiState,
            onSearchChanged = viewModel::setSearchQuery,
            onStatusFilterChanged = viewModel::setStatusFilter,
            onRefresh = viewModel::refresh,
            onDateChanged = viewModel::setDate,
            onDatePresetSelected = viewModel::setDatePreset,
            onPreviousDate = viewModel::previousDate,
            onNextDate = viewModel::nextDate,
            onShowDetail = onShowDetail,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage
            )
        }
    }
}
