package com.tbterminal.app.ui.receivables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.receivablepayments.ReceivablePaymentReceiptDialog
import com.tbterminal.app.ui.components.RefreshableContent

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
    onBack: () -> Unit = {},
    onCustomerDetailClick: (String) -> Unit = {},
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
                showPageHeader = true,
        pageTitle = "Piutang Pelanggan",
        topBarActions = {
            if (canManageReceivableAdjustment(role)) {
                IconButton(
                    onClick = viewModel::openAdjustment,
                    modifier = Modifier.padding(end = 0.dp) // Shifted to the right by removing end padding
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Tune,
                        contentDescription = "Sesuaikan",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.rotate(90f).size(20.dp) // Slightly smaller icon
                    )
                }
            }
        },
        onBack = onBack,
        onLogout = onLogout
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.receivables.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            ReceivableScreen(
            modifier = Modifier,
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
            onDismissMessage = viewModel::clearMessage,
            onCustomerDetailClick = onCustomerDetailClick
            )
        }
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
            customerRepository = customerRepository,
            uiState = uiState,
            onCustomerChanged = viewModel::onOpeningCustomerChanged,
            onCustomerSelected = viewModel::onOpeningCustomerSelected,
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
            customerRepository = customerRepository,
            uiState = uiState,
            onCustomerChanged = viewModel::onAdjustmentCustomerChanged,
            onCustomerSelected = viewModel::onAdjustmentCustomerSelected,
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




