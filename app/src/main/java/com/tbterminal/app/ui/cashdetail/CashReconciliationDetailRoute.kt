package com.tbterminal.app.ui.cashdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminCashReconciliationDetailScreen(
    name: String,
    role: String,
    sessionId: String,
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
    onOpenHistory: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CashReconciliationDetailViewModel = viewModel(
        key = "cash-detail-$sessionId",
        factory = CashReconciliationDetailViewModel.factory(sessionId, cashRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.CashReconciliationDetail,
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
        CashReconciliationDetailScreen(
            modifier = modifier,
            uiState = uiState,
            onOpenHistory = onOpenHistory,
            onRetry = { viewModel.loadDetail(uiState.page) },
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage
        )
    }
}
