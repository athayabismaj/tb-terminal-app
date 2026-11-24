package com.tbterminal.app.ui.products

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.media.AndroidProductImageCompressor
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.sync.NetworkConnectivity
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminProductFormScreen(
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
    onBackToProducts: () -> Unit,
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    networkConnectivity: NetworkConnectivity? = null,
    imageCompressor: com.tbterminal.app.data.media.ImageCompressor? = null,
    viewModel: ProductFormViewModel? = null
) {
    val context = LocalContext.current
    val resolvedCompressor = remember(context, imageCompressor) {
        imageCompressor ?: AndroidProductImageCompressor(context.applicationContext.contentResolver)
    }
    val activeViewModel = viewModel ?: viewModel(
        factory = ProductFormViewModel.factory(
            inventoryRepository = inventoryRepository,
            productId = productId,
            imageCompressor = resolvedCompressor,
            networkConnectivity = networkConnectivity
        )
    )
    val uiState by activeViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            activeViewModel.resetSavedState()
            onBackToProducts()
        }
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = if (productId == null) AdminDestination.AddProduct else AdminDestination.Products,
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
        pageTitle = if (productId == null) "Tambah produk" else "Edit produk",
        onBack = onBackToProducts,
        onLogout = onLogout
    ) { contentModifier ->
        ProductFormContent(
            modifier = contentModifier,
            uiState = uiState,
            onInputChanged = activeViewModel::onInputChanged,
            onPhotoPicked = activeViewModel::onPhotoPicked,
            onPhotoSelectionError = activeViewModel::onPhotoSelectionError,
            onRemovePhoto = activeViewModel::onRemovePhoto,
            onRetryPhotoUpload = activeViewModel::retryPhotoUpload,
            onSave = activeViewModel::save,
            onCancel = onBackToProducts
        )
    }
}
