package com.tbterminal.app.ui.suppliers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent
import androidx.compose.ui.Modifier

@Composable
fun AdminSupplierScreen(
    name: String,
    role: String,
    purchasingRepository: PurchasingRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSuppliersClick: () -> Unit,
    onPurchaseHistoryClick: () -> Unit,
    onStockReportClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReportsClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SupplierViewModel = viewModel(factory = SupplierViewModel.factory(purchasingRepository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Suppliers,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSuppliersClick = onSuppliersClick,
        onPurchaseHistoryClick = onPurchaseHistoryClick,
        onStockReportClick = onStockReportClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onLogout = onLogout
    ) { modifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.suppliers.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = modifier,
        ) {
            SupplierScreen(
            modifier = Modifier,
            uiState = uiState,
            onNameChanged = viewModel::onNameChanged,
            onPhoneChanged = viewModel::onPhoneChanged,
            onAddressChanged = viewModel::onAddressChanged,
            onPaymentTermChanged = viewModel::onPaymentTermChanged,
            onSearchChanged = viewModel::onSearchChanged,
            onSave = viewModel::save,
            onEdit = viewModel::edit,
            onCancelEdit = viewModel::cancelEdit,
            onDelete = viewModel::delete,
            onRefresh = viewModel::refresh,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
            )
        }
    }
}
