package com.tbterminal.app.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

@Composable
fun AdminProductUnitsScreen(
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
    viewModel: ProductUnitViewModel = viewModel(
        factory = ProductUnitViewModel.factory(inventoryRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = uiState.isFormVisible) { viewModel.cancelEdit() }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.ProductUnits,
        showPageHeader = true,
        topBarActions = {
            if (!uiState.isFormVisible) {
                // Button 1: Search [ 🔍 ]
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(36.dp).testTag("unit-toggle-search")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari satuan",
                        tint = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
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
            !uiState.isFormVisible -> "Satuan produk"
            uiState.editingUnit == null -> "Tambah satuan"
            else -> "Edit satuan"
        },
        onBack = if (uiState.isFormVisible) viewModel::cancelEdit else onStockClick,
        onLogout = onLogout
    ) { contentModifier ->
        if (uiState.isFormVisible) {
            ProductUnitContent(
                modifier = contentModifier,
                uiState = uiState,
                isSearchExpanded = false,
                onToggleSearch = {},
                onNameChanged = viewModel::onNameChanged,
                onSymbolChanged = viewModel::onSymbolChanged,
                onSearchChanged = viewModel::onSearchChanged,
                onSave = viewModel::save,
                onAdd = viewModel::openAddForm,
                onEdit = viewModel::edit,
                onCancelEdit = viewModel::cancelEdit,
                onDelete = viewModel::delete,
                onRetry = viewModel::refresh,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage
            )
        } else {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.units.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = contentModifier,
            ) {
                ProductUnitContent(
                    modifier = androidx.compose.ui.Modifier,
                    uiState = uiState,
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                    onNameChanged = viewModel::onNameChanged,
                    onSymbolChanged = viewModel::onSymbolChanged,
                    onSearchChanged = viewModel::onSearchChanged,
                    onSave = viewModel::save,
                    onAdd = viewModel::openAddForm,
                    onEdit = viewModel::edit,
                    onCancelEdit = viewModel::cancelEdit,
                    onDelete = viewModel::delete,
                    onRetry = viewModel::refresh,
                    onPreviousPage = viewModel::previousPage,
                    onNextPage = viewModel::nextPage
                )
            }
        }
    }
}
