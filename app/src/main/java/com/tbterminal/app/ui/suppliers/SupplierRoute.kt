package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    onAddSupplierClick: () -> Unit,
    onEditSupplierClick: () -> Unit,
    onBackToPrevious: (() -> Unit)? = null,
    onLogout: () -> Unit,
    viewModel: SupplierViewModel = viewModel(factory = SupplierViewModel.factory(purchasingRepository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 700
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }
    val isFiltered = uiState.statusFilter != SupplierStatusFilter.ALL

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Suppliers,
        pageTitle = "Supplier",
        onBack = onBackToPrevious,
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("supplier-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari supplier",
                    tint = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 2: Filter [ 🎛️ ]
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFilterPopup = true },
                    modifier = Modifier.size(36.dp).testTag("supplier-open-filter")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter supplier",
                        tint = if (isFiltered) Color(0xFF256B57) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("supplier-open-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Ringkasan supplier",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 4: Tambah [ ➕ ]
            SupplierHeaderAddButton(
                compact = isCompactScreen,
                onClick = {
                    viewModel.cancelEdit()
                    onAddSupplierClick()
                },
            )
        },
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
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                showFilterPopup = showFilterPopup,
                onDismissFilter = { showFilterPopup = false },
                showSummaryPopup = showSummaryPopup,
                onDismissSummary = { showSummaryPopup = false },
                onSearchChanged = viewModel::onSearchChanged,
                onStatusFilterChanged = viewModel::onStatusFilterChanged,
                onAdd = {
                    viewModel.cancelEdit()
                    onAddSupplierClick()
                },
                onEdit = { supplier ->
                    viewModel.edit(supplier)
                    onEditSupplierClick()
                },
                onDelete = viewModel::delete,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onDismissMessage = viewModel::clearMessage
            )
        }
    }
}

@Composable
fun AdminSupplierFormScreen(
    name: String,
    role: String,
    viewModel: SupplierViewModel,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onSuppliersClick: () -> Unit,
    onBackToSuppliers: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.resetSavedState()
            onBackToSuppliers()
        }
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SupplierForm,
        pageTitle = if (uiState.editingSupplier == null) "Tambah Supplier" else "Edit Supplier",
        onBack = onBackToSuppliers,
        showPageHeader = true,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onSuppliersClick = onSuppliersClick,
        onLogout = onLogout,
    ) { modifier ->
        SupplierFormScreen(
            modifier = modifier,
            uiState = uiState,
            onNameChanged = viewModel::onNameChanged,
            onPhoneChanged = viewModel::onPhoneChanged,
            onAddressChanged = viewModel::onAddressChanged,
            onPaymentTermChanged = viewModel::onPaymentTermChanged,
            onSave = viewModel::save,
            onCancel = {
                viewModel.cancelEdit()
                onBackToSuppliers()
            },
            onDismissMessage = viewModel::clearMessage,
        )
    }
}
