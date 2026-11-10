package com.tbterminal.app.ui.products

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.navigation.LocalPendingProductSearch
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.AppConfirmationSpec
import com.tbterminal.app.ui.components.AppConfirmDialog

@Composable
fun AdminProductListScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onProductDetailClick: (String) -> Unit,
    onCategoriesClick: () -> Unit,
    onUnitsClick: () -> Unit,
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
    viewModel: ProductListViewModel = viewModel(
        factory = ProductListViewModel.factory(inventoryRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingProductSearch = LocalPendingProductSearch.current
    var productToToggle by remember { mutableStateOf<ProductStock?>(null) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingProductSearch.value) {
        val barcodeQuery = pendingProductSearch.value
        if (!barcodeQuery.isNullOrBlank()) {
            pendingProductSearch.value = null
            isSearchExpanded = true
            viewModel.onSearchChanged(barcodeQuery)
        }
    }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }
    val isFiltered = uiState.selectedCategory != ALL_PRODUCT_CATEGORIES
    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val csv = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            if (!csv.isNullOrBlank()) viewModel.previewCsv(csv)
        }
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Products,
        pageTitle = "Daftar produk",
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("product-toggle-search")
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
                    modifier = Modifier.size(36.dp).testTag("product-open-filters")
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

            // Button 3: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("product-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Tampilkan ringkasan produk",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onCategoriesClick,
        onProductUnitsClick = onUnitsClick,
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
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.products.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            ProductListContent(
                modifier = Modifier,
                uiState = uiState,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                showFilterPopup = showFilterPopup,
                onDismissFilter = { showFilterPopup = false },
                showSummaryPopup = showSummaryPopup,
                onDismissSummary = { showSummaryPopup = false },
                onSearchChanged = viewModel::onSearchChanged,
                onCategorySelected = viewModel::onCategorySelected,
                onRetry = viewModel::refresh,
                onAddProductClick = onAddProductClick,
                onImportProductClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
                onEditProductClick = onEditProductClick,
                onProductDetailClick = onProductDetailClick,
                onCategoriesClick = onCategoriesClick,
                onUnitsClick = onUnitsClick,
                onToggleProductClick = { product -> productToToggle = product },
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onDismissMessage = viewModel::clearActionMessage
            )
        }
    }

    if (uiState.showImportDialog) {
        val preview = uiState.importPreview
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text("Preview Impor Produk") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isImporting && preview == null) {
                        Text("Memvalidasi CSV...")
                    } else if (preview != null) {
                        Text("Total ${preview.totalRows} baris • ${preview.validRows} valid • ${preview.invalidRows} gagal")
                        preview.rows.filter { !it.valid }.forEach { row ->
                            Text("Baris ${row.rowNumber} (${row.sku.ifBlank { "tanpa SKU" }}): ${row.errors.joinToString("; ")}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::commitCsv,
                    enabled = !uiState.isImporting && preview != null && preview.invalidRows == 0 && preview.totalRows > 0
                ) { Text(if (uiState.isImporting) "Memproses..." else "Simpan Atomik") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissImport, enabled = !uiState.isImporting) { Text("Batal") }
            }
        )
    }

    productToToggle?.let { product ->
        val isActivating = !product.isActive
        AppConfirmDialog(
            spec = AppConfirmationSpec(
                title = if (isActivating) "Aktifkan produk" else "Nonaktifkan produk",
                target = product.productName,
                consequence = if (isActivating) {
                    "Produk akan tersedia kembali untuk transaksi dan pengelolaan stok."
                } else {
                    "Produk keluar dari katalog aktif, tetapi seluruh riwayat transaksi tetap tersimpan."
                },
                confirmLabel = if (isActivating) "Aktifkan" else "Nonaktifkan",
            ),
            onDismiss = { productToToggle = null },
            onConfirm = {
                productToToggle = null
                viewModel.toggleProductStatus(product)
            },
            destructive = !isActivating,
        )
    }
}
