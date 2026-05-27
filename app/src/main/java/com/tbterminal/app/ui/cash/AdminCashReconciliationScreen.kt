package com.tbterminal.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminCashReconciliationScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
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
        factory = CashReconciliationViewModel.factory(cashReconciliationRepository)
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

@Composable
internal fun CashReconciliationContent(
    modifier: Modifier,
    uiState: CashReconciliationUiState,
    onDismissMessage: () -> Unit,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onShowExpenseDialog: () -> Unit,
    onHideExpenseDialog: () -> Unit,
    onExpenseAmountChanged: (String) -> Unit,
    onExpenseDescriptionChanged: (String) -> Unit,
    onAddExpense: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CashBackground)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CashHeader()
        CashMessage(uiState, onDismissMessage)
        CashMetrics(uiState)
        CashSessionPanel(
            uiState = uiState,
            onOpeningCashChanged = onOpeningCashChanged,
            onOpenSession = onOpenSession,
            onClosingCashChanged = onClosingCashChanged,
            onClosingNotesChanged = onClosingNotesChanged,
            onCloseSession = onCloseSession,
            onShowExpenseDialog = onShowExpenseDialog
        )
        CashTransactionsTable(
            uiState = uiState,
            onRefresh = onRefresh,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (uiState.isExpenseDialogOpen) {
        CashExpenseDialog(
            uiState = uiState,
            onDismiss = onHideExpenseDialog,
            onAmountChanged = onExpenseAmountChanged,
            onDescriptionChanged = onExpenseDescriptionChanged,
            onConfirm = onAddExpense
        )
    }
}
