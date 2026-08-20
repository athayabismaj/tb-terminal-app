package com.tbterminal.app.ui.products

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

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
    var productToToggle by remember { mutableStateOf<ProductStock?>(null) }
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
        ProductListContent(
            modifier = contentModifier,
            uiState = uiState,
            onSearchChanged = viewModel::onSearchChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onRetry = { viewModel.loadProducts() },
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
        AlertDialog(
            onDismissRequest = { productToToggle = null },
            title = { Text(if (isActivating) "Aktifkan Produk" else "Nonaktifkan Produk") },
            text = {
                Text(
                    if (isActivating) {
                        "Produk ${product.productName} akan tersedia kembali untuk transaksi dan pengelolaan stok."
                    } else {
                        "Produk ${product.productName} akan dinonaktifkan dari katalog aktif, tetapi riwayat transaksinya tetap tersimpan."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToToggle = null
                        viewModel.toggleProductStatus(product)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActivating) ProductPrimary else ProductDanger
                    )
                ) {
                    Text(if (isActivating) "Aktifkan" else "Nonaktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToToggle = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
