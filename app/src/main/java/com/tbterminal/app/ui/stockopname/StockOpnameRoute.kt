package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminStockOpnameScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    onStockClick: () -> Unit,
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
    onStockOpnameFormClick: (String?) -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: StockOpnameViewModel = viewModel(
        factory = StockOpnameViewModel.factory(
            inventoryRepository = inventoryRepository,
            autoSelectFirst = false
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }
    val isFiltered = uiState.categoryFilter != null

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.StockOpname,
        pageTitle = "Sesuaikan stok",
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("stock-adjustment-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari produk",
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
                    modifier = Modifier.size(36.dp).testTag("stock-adjustment-open-filters")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter kategori",
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

            // Button 3: Tambah / Buat Opname [ ➕ ]
            IconButton(
                onClick = { onStockOpnameFormClick(null) },
                modifier = Modifier.size(36.dp).testTag("stock-adjustment-header-add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Sesuaikan stok baru",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        onBack = onStockClick,
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
        onStockOpnameFormClick = { onStockOpnameFormClick(null) },
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
        StockOpnameListScreen(
            modifier = contentModifier,
            uiState = uiState,
            isSearchExpanded = isSearchExpanded,
            onToggleSearch = { isSearchExpanded = !isSearchExpanded },
            showFilterPopup = showFilterPopup,
            onDismissFilter = { showFilterPopup = false },
            onSearchChanged = viewModel::onSearchChanged,
            onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
            onOpenForm = { onStockOpnameFormClick(null) },
            onSelectProduct = { product -> onStockOpnameFormClick(product.productId) },
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
        )
    }
}

@Composable
fun AdminStockOpnameFormScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    productId: String?,
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
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: StockOpnameViewModel = viewModel(
        factory = StockOpnameViewModel.factory(
            inventoryRepository = inventoryRepository,
            initialProductId = productId,
            autoSelectFirst = true
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.StockOpnameForm,
        pageTitle = "Sesuaikan stok",
        onBack = onStockOpnameClick,
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
        StockOpnameFormScreen(
            modifier = contentModifier,
            uiState = uiState,
            onSelectProduct = viewModel::selectProduct,
            onActualQtyChanged = viewModel::onActualQtyChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onOpeningDateChanged = viewModel::onOpeningDateChanged,
            onAdjustmentTypeChanged = viewModel::onAdjustmentTypeChanged,
            onSubmit = viewModel::submitOpname,
            onDismissMessage = viewModel::clearMessage
        )
    }
}
