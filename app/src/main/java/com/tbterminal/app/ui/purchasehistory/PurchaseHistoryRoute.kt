package com.tbterminal.app.ui.purchasehistory

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
fun AdminPurchaseHistoryScreen(
    name: String,
    role: String,
    purchasingRepository: PurchasingRepository,
    onDashboardClick: () -> Unit,
    onTransactionsClick: () -> Unit,
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
    viewModel: PurchaseHistoryViewModel = viewModel(factory = PurchaseHistoryViewModel.factory(purchasingRepository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.PurchaseHistory,
        pageTitle = "Riwayat Pembelian",
        showPageHeader = true,
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
        onBack = onTransactionsClick,
        onLogout = onLogout
    ) { modifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.purchases.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = modifier,
        ) {
            PurchaseHistoryScreen(
            modifier = Modifier,
            uiState = uiState,
            onSupplierSelected = viewModel::onSupplierSelected,
            onSearchChanged = viewModel::onSearchChanged,
            onRefresh = viewModel::refresh,
            onShowDetail = viewModel::showDetail,
            onDismissDetail = viewModel::dismissDetail,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage
            )
        }
    }
}
