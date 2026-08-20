package com.tbterminal.app.ui.receivables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.receivablepayments.ReceivablePaymentReceiptDialog

@Composable
fun AdminReceivableScreen(
    name: String,
    role: String,
    receivableRepository: ReceivableRepository,
    customerRepository: CustomerRepository,
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
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ReceivableViewModel = viewModel(
        factory = ReceivableViewModel.factory(receivableRepository, customerRepository, role)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Receivables,
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
        ReceivableScreen(
            modifier = contentModifier,
            uiState = uiState,
            onSearchChanged = viewModel::onSearchChanged,
            onStatusFilterChanged = viewModel::onStatusFilterChanged,
            onDueFilterChanged = viewModel::onDueFilterChanged,
            canAdjust = canManageReceivableAdjustment(role),
            onAddOpeningBalance = viewModel::openOpeningBalance,
            onAddAdjustment = viewModel::openAdjustment,
            onPayClick = viewModel::openPayment,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
        )
    }

    uiState.selectedReceivable?.let { receivable ->
        ReceivablePaymentDialog(
            receivable = receivable,
            uiState = uiState,
            onAmountChanged = viewModel::onPaymentAmountChanged,
            onMethodChanged = viewModel::onPaymentMethodChanged,
            onReferenceChanged = viewModel::onReferenceChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onDismiss = viewModel::closePayment,
            onSubmit = viewModel::submitPayment
        )
    }

    if (uiState.isOpeningBalanceOpen) {
        OpeningReceivableDialog(
            uiState = uiState,
            onCustomerChanged = viewModel::onOpeningCustomerChanged,
            onAmountChanged = viewModel::onOpeningAmountChanged,
            onDebtDateChanged = viewModel::onOpeningDebtDateChanged,
            onDueDateChanged = viewModel::onOpeningDueDateChanged,
            onLegacyInvoiceChanged = viewModel::onOpeningLegacyInvoiceChanged,
            onNotesChanged = viewModel::onOpeningNotesChanged,
            onDismiss = viewModel::closeOpeningBalance,
            onSubmit = viewModel::submitOpeningBalance
        )
    }

    if (uiState.isAdjustmentOpen) {
        AdjustmentReceivableDialog(
            uiState = uiState,
            onCustomerChanged = viewModel::onAdjustmentCustomerChanged,
            onAmountChanged = viewModel::onAdjustmentAmountChanged,
            onDebtDateChanged = viewModel::onAdjustmentDebtDateChanged,
            onDueDateChanged = viewModel::onAdjustmentDueDateChanged,
            onReferenceChanged = viewModel::onAdjustmentReferenceChanged,
            onReasonChanged = viewModel::onAdjustmentReasonChanged,
            onDismiss = viewModel::closeAdjustment,
            onSubmit = viewModel::submitAdjustment
        )
    }

    uiState.lastPaymentReceipt?.let { receipt ->
        ReceivablePaymentReceiptDialog(
            receipt = receipt,
            onDismiss = viewModel::dismissPaymentReceipt
        )
    }
}
