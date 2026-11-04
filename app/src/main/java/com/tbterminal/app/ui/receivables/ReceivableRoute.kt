package com.tbterminal.app.ui.receivables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Description

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
    onAddOpeningBalanceClick: () -> Unit = {},
    onAddAdjustmentClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ReceivableViewModel = viewModel(
        factory = ReceivableViewModel.factory(receivableRepository, customerRepository, role)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var wasDisplayed by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        if (wasDisplayed) {
            viewModel.refresh()
        }
        wasDisplayed = true
    }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    val isFiltered = uiState.statusFilter != ReceivableStatusFilter.All ||
        uiState.sortOrder != ReceivableSortOrder.NearestDueDate ||
        uiState.dueFilter != ReceivableDueFilter.All

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
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("receivable-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari piutang",
                    tint = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 2: Filter [ ⚙️ ]
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFilterPopup = true },
                    modifier = Modifier.size(36.dp).testTag("receivable-open-filters")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter piutang",
                        tint = if (isFiltered) Color(0xFF256B57) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(8.dp)
                            .background(Color(0xFFE5A93C), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Rekap / Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("receivable-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Tampilkan ringkasan piutang",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
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
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                showFilterPopup = showFilterPopup,
                onDismissFilter = { showFilterPopup = false },
                showSummaryPopup = showSummaryPopup,
                onOpenSummary = { showSummaryPopup = true },
                onDismissSummary = { showSummaryPopup = false },
                onResetAllFilters = {
                    viewModel.onStatusFilterChanged(ReceivableStatusFilter.All)
                    viewModel.onDueFilterChanged(ReceivableDueFilter.All)
                    viewModel.onSortOrderChanged(ReceivableSortOrder.NearestDueDate)
                    viewModel.onSearchChanged("")
                },
                onSearchChanged = viewModel::onSearchChanged,
                onStatusFilterChanged = viewModel::onStatusFilterChanged,
                onDueFilterChanged = viewModel::onDueFilterChanged,
                canAdjust = canManageReceivableAdjustment(role),
                onAddOpeningBalance = onAddOpeningBalanceClick,
                onAddAdjustment = onAddAdjustmentClick,
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




