package com.tbterminal.app.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

@Composable
fun AdminProductCategoriesScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onStockClick: () -> Unit,
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
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ProductCategoryViewModel = viewModel(
        factory = ProductCategoryViewModel.factory(inventoryRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.isFormVisible) { viewModel.cancelEdit() }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.ProductCategories,
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
        pageTitle = when {
            !uiState.isFormVisible -> "Kategori produk"
            uiState.editingCategory == null -> "Tambah kategori"
            else -> "Edit kategori"
        },
        onBack = if (uiState.isFormVisible) viewModel::cancelEdit else onStockClick,
        onLogout = onLogout
    ) { contentModifier ->
        if (uiState.isFormVisible) {
            ProductCategoryContent(
                modifier = contentModifier,
                uiState = uiState,
                onNameChanged = viewModel::onNameChanged,
                onSave = viewModel::save,
                onAdd = viewModel::openAddForm,
                onEdit = viewModel::edit,
                onCancelEdit = viewModel::cancelEdit,
                onDelete = viewModel::delete,
                onRetry = viewModel::refresh,
                onSearchChanged = viewModel::onSearchChanged,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage
            )
        } else {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.categories.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = contentModifier,
            ) {
                ProductCategoryContent(
                    modifier = androidx.compose.ui.Modifier,
                    uiState = uiState,
                    onNameChanged = viewModel::onNameChanged,
                    onSave = viewModel::save,
                    onAdd = viewModel::openAddForm,
                    onEdit = viewModel::edit,
                    onCancelEdit = viewModel::cancelEdit,
                    onDelete = viewModel::delete,
                    onRetry = viewModel::refresh,
                    onSearchChanged = viewModel::onSearchChanged,
                    onPreviousPage = viewModel::previousPage,
                    onNextPage = viewModel::nextPage
                )
            }
        }
    }
}
