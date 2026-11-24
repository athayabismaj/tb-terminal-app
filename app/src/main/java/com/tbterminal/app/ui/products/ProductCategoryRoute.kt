package com.tbterminal.app.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.isOwnerPersona
import com.tbterminal.app.ui.dashboard.owner.OwnerKategoriRoute
import com.tbterminal.app.ui.dashboard.owner.OwnerTambahKategoriRoute

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
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    if (isOwnerPersona(role)) {
        if (uiState.isFormVisible) {
            OwnerTambahKategoriRoute(
                uiState = uiState,
                onNameChanged = viewModel::onNameChanged,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
                onCancel = viewModel::cancelEdit,
                onStockClick = onStockClick,
                onDashboardClick = onDashboardClick,
                onDismissMessage = viewModel::clearMessage
            )
        } else {
            OwnerKategoriRoute(
                uiState = uiState,
                isSearchExpanded = isSearchExpanded,
                onOpenSearch = { isSearchExpanded = true },
                onCloseSearch = {
                    isSearchExpanded = false
                    if (uiState.searchQuery.isNotEmpty()) {
                        viewModel.onSearchChanged("")
                    }
                },
                onSearchChanged = viewModel::onSearchChanged,
                onAddCategoryClick = viewModel::openAddForm,
                onCategoryClick = viewModel::edit,
                onRefresh = viewModel::refresh,
                onLoadNextPage = viewModel::nextPage,
                onRetryLoadMore = { viewModel.loadCategories(uiState.page) },
                onBack = null,
                onStockClick = onStockClick,
                onDashboardClick = onDashboardClick,
                onDismissMessage = viewModel::clearMessage
            )
        }
        return
    }

    BackHandler(enabled = uiState.isFormVisible) { viewModel.cancelEdit() }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.ProductCategories,
        showPageHeader = true,
        topBarActions = {
            if (!uiState.isFormVisible) {
                // Button 1: Search [ 🔍 ]
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(36.dp).testTag("category-toggle-search")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari kategori",
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
                isSearchExpanded = false,
                onToggleSearch = {},
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
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = { isSearchExpanded = !isSearchExpanded },
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
