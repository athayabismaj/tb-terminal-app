package com.tbterminal.app.ui.receivablepayments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability

@Composable
fun AdminReceivablePaymentHistoryScreen(
    name: String,
    role: String,
    receivableRepository: ReceivableRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReportsClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ReceivablePaymentHistoryViewModel = viewModel(
        factory = ReceivablePaymentHistoryViewModel.factory(receivableRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.ReceivablePayments,
        pageTitle = "Pembayaran Piutang",
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
            isRefreshing = uiState.isLoading && uiState.payments.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = modifier,
        ) {
            ReceivablePaymentHistoryScreen(
            modifier = androidx.compose.ui.Modifier,
            uiState = uiState,
            onSearchChanged = viewModel::onSearchChanged,
            onReceiverSearchChanged = viewModel::onReceiverSearchChanged,
            onReceivableIdChanged = viewModel::onReceivableIdChanged,
            onDateFromChanged = viewModel::onDateFromChanged,
            onDateToChanged = viewModel::onDateToChanged,
            onMethodFilterChanged = viewModel::onMethodFilterChanged,
            onStatusFilterChanged = viewModel::onStatusFilterChanged,
            onApplyFilters = viewModel::applyFilters,
            onShowDetail = viewModel::showDetail,
            onDismissDetail = viewModel::dismissDetail,
            canReverse = AppAccessPolicy.can(role, AppCapability.REVERSE_RECEIVABLE_PAYMENTS),
            onOpenReversal = viewModel::openReversal,
            onReversalReasonChanged = viewModel::onReversalReasonChanged,
            onDismissReversal = viewModel::dismissReversal,
            onSubmitReversal = viewModel::submitReversal,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage
            )
        }
    }

    uiState.lastReversalReceipt?.let { receipt ->
        ReceivablePaymentReceiptDialog(receipt, viewModel::dismissReversalReceipt)
    }
}
