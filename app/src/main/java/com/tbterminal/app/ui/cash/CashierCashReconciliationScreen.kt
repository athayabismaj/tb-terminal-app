package com.tbterminal.app.ui.cash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination

@Composable
fun CashierCashReconciliationScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashReconciliationViewModel = viewModel(
        factory = CashReconciliationViewModel.factory(cashReconciliationRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.CashSession,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        CashReconciliationContent(
            modifier = contentModifier,
            uiState = uiState,
            onDismissMessage = viewModel::clearMessage,
            onOpeningCashChanged = viewModel::onOpeningCashChanged,
            onOpenSession = viewModel::openSession,
            onClosingCashChanged = viewModel::onClosingCashChanged,
            onClosingNotesChanged = viewModel::onClosingNotesChanged,
            onCloseSession = viewModel::closeSession,
            onRefresh = { viewModel.loadCash() },
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
