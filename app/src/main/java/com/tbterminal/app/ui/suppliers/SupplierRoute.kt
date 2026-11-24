package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.isOwnerPersona
import com.tbterminal.app.ui.dashboard.owner.OwnerDetailSupplierRoute
import com.tbterminal.app.ui.dashboard.owner.OwnerSupplierRoute
import com.tbterminal.app.ui.dashboard.owner.OwnerTambahSupplierRoute

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
    onSupplierDetailClick: (String) -> Unit = {},
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
    var wasDisplayed by rememberSaveable { mutableStateOf(false) }
    val isFiltered = uiState.statusFilter != SupplierStatusFilter.ALL

    LaunchedEffect(viewModel) {
        if (wasDisplayed) viewModel.refresh()
        wasDisplayed = true
    }

    if (isOwnerPersona(role)) {
        var sortOrder by rememberSaveable { mutableStateOf(SupplierSortOrder.NAME_ASC) }
        OwnerSupplierRoute(
            uiState = uiState,
            canManage = true,
            isSearchExpanded = isSearchExpanded,
            sortOrder = sortOrder,
            onOpenSearch = { isSearchExpanded = true },
            onCloseSearch = {
                isSearchExpanded = false
                if (uiState.searchQuery.isNotEmpty()) {
                    viewModel.onSearchChanged("")
                }
            },
            onSearchChanged = viewModel::onSearchChanged,
            onOpenFilter = { showFilterPopup = true },
            onAddSupplierClick = {
                viewModel.cancelEdit()
                onAddSupplierClick()
            },
            onSupplierDetailClick = onSupplierDetailClick,
            onRefresh = viewModel::refresh,
            onLoadNextPage = viewModel::nextPage,
            onRetryLoadMore = { viewModel.loadSuppliers(uiState.page + 1) },
            onBack = onBackToPrevious,
            onDashboardClick = onDashboardClick,
            onDismissMessage = viewModel::clearMessage,
            loadingContent = { SupplierLoadingState() }
        )
        if (showFilterPopup) {
            SupplierFilterSheet(
                statusFilter = uiState.statusFilter,
                onApplyFilter = viewModel::onStatusFilterChanged,
                onDismiss = { showFilterPopup = false },
                sortOrder = sortOrder,
                onSortApply = { sortOrder = it }
            )
        }
        return
    }

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

    if (isOwnerPersona(role)) {
        OwnerTambahSupplierRoute(
            uiState = uiState,
            onNameChanged = viewModel::onNameChanged,
            onPhoneChanged = viewModel::onPhoneChanged,
            onAddressChanged = viewModel::onAddressChanged,
            onPaymentTermChanged = viewModel::onPaymentTermChanged,
            onSave = viewModel::save,
            onSavedReset = viewModel::resetSavedState,
            onCancel = {
                viewModel.cancelEdit()
                onBackToSuppliers()
            },
            onDashboardClick = onDashboardClick,
            onDismissMessage = viewModel::clearMessage
        )
        return
    }

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

@Composable
fun AdminSupplierDetailScreen(
    name: String,
    role: String,
    purchasingRepository: PurchasingRepository,
    supplierId: String,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onSuppliersClick: () -> Unit,
    onEditSupplierClick: (Supplier) -> Unit,
    onLogout: () -> Unit,
    viewModel: SupplierDetailViewModel = viewModel(
        factory = SupplierDetailViewModel.factory(purchasingRepository, supplierId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canManage = AppAccessPolicy.can(role, AppCapability.SUPPLIERS)
    var wasDisplayed by rememberSaveable(supplierId) { mutableStateOf(false) }

    LaunchedEffect(viewModel, supplierId) {
        if (isOwnerPersona(role) && wasDisplayed) {
            viewModel.loadDetail()
        }
        wasDisplayed = true
    }

    if (isOwnerPersona(role)) {
        OwnerDetailSupplierRoute(
            uiState = uiState,
            canManage = canManage,
            onBack = onSuppliersClick,
            onRetry = viewModel::loadDetail,
            onEditSupplierClick = { uiState.supplier?.let(onEditSupplierClick) },
            onDeactivateConfirm = viewModel::deactivate,
            onClearActionError = viewModel::clearActionError,
            onDeactivatedReset = viewModel::resetDeactivatedState,
            onDashboardClick = onDashboardClick,
            onActivateClick = viewModel::activate,
            onUndoActivateClick = viewModel::undoActivate,
            onReactivatedReset = viewModel::resetReactivatedState
        )
        return
    }

    LaunchedEffect(uiState.isDeactivated) {
        if (uiState.isDeactivated) {
            viewModel.resetDeactivatedState()
            onSuppliersClick()
        }
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Suppliers,
        pageTitle = "Detail Supplier",
        onBack = onSuppliersClick,
        showPageHeader = true,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onSuppliersClick = onSuppliersClick,
        onLogout = onLogout
    ) { modifier ->
        val supplier = uiState.supplier
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                uiState.isLoading && supplier == null -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null && supplier == null -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SupplierSlate200)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(uiState.errorMessage.orEmpty(), color = SupplierSlate900, fontSize = 14.sp)
                            Button(onClick = viewModel::loadDetail) {
                                Text("Coba lagi")
                            }
                        }
                    }
                }
                supplier != null -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SupplierSlate200)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = supplier.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupplierSlate900
                            )
                            Text(
                                text = if (supplier.isActive) "Status: Aktif" else "Status: Nonaktif",
                                fontSize = 13.sp,
                                color = if (supplier.isActive) SupplierEmerald700 else SupplierSlate500
                            )
                            HorizontalDivider(color = SupplierSlate100)
                            Text("Nomor HP: ${supplier.phone?.ifBlank { null } ?: "Belum diisi"}", fontSize = 14.sp)
                            Text("Alamat: ${supplier.address?.ifBlank { null } ?: "Belum diisi"}", fontSize = 14.sp)
                            Text(
                                text = if (supplier.paymentTermDays > 0) "Termin: ${supplier.paymentTermDays} hari" else "Termin: 0 hari (tunai)",
                                fontSize = 14.sp
                            )
                            if (canManage) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { onEditSupplierClick(supplier) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF256B57))
                                    ) {
                                        Text("Edit")
                                    }
                                    if (supplier.isActive) {
                                        OutlinedButton(onClick = viewModel::deactivate, enabled = !uiState.isMutating) {
                                            Text("Nonaktifkan")
                                        }
                                    } else {
                                        Button(onClick = viewModel::activate, enabled = !uiState.isMutating) {
                                            Text("Aktifkan")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
