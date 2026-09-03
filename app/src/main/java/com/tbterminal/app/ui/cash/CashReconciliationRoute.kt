package com.tbterminal.app.ui.cash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

@Composable
fun CashReconciliationRoute(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashReconciliationViewModel = viewModel(
        factory = CashReconciliationViewModel.factory(
            repository = cashReconciliationRepository,
            cashSessionLocalDataSource = cashSessionLocalDataSource
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.CashReconciliation,
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
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            CashReconciliationScreen(
            modifier = androidx.compose.ui.Modifier,
            uiState = uiState,
            onDismissMessage = viewModel::clearMessage,
            onOpeningCashChanged = viewModel::onOpeningCashChanged,
            onOpenSession = viewModel::openSession,
            onClosingCashChanged = viewModel::onClosingCashChanged,
            onClosingNotesChanged = viewModel::onClosingNotesChanged,
            onCloseSession = viewModel::closeSession,
            onRefresh = viewModel::refresh,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onShowExpenseDialog = viewModel::showExpenseDialog,
            onHideExpenseDialog = viewModel::hideExpenseDialog,
            onExpenseAmountChanged = viewModel::onExpenseAmountChanged,
            onExpenseDescriptionChanged = viewModel::onExpenseDescriptionChanged,
            onAddExpense = viewModel::addExpense
            )
        }
    }
}
