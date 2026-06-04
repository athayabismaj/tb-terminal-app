package com.tbterminal.app.ui.pricemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.products.ProductBanner
import com.tbterminal.app.ui.products.ProductErrorState
import com.tbterminal.app.ui.products.ProductLoadingState
import com.tbterminal.app.ui.products.ProductSurface
import com.tbterminal.app.ui.products.ProductText
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val AdminLine = Color(0xFFE2E8F0)
private val BrandBlue = Color(0xFF2563EB)
private val BrandBlueLight = Color(0xFFEFF6FF)

@Composable
fun AdminPriceManagementScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: PriceManagementViewModel = viewModel(
        factory = PriceManagementViewModel.factory(inventoryRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.PriceManagement,
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
        PriceManagementContent(
            state = uiState,
            onSearchChanged = viewModel::onSearchQueryChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onRetry = { viewModel.loadProducts(uiState.currentPage) },
            onEdit = viewModel::openPriceDialog,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onClearActionMessage = viewModel::clearActionMessage,
            modifier = contentModifier
        )
    }

    if (uiState.selectedProductStock != null) {
        PriceUpdateDialog(
            state = uiState,
            onDismiss = viewModel::closePriceDialog,
            onSave = viewModel::updatePrice
        )
    }
}

@Composable
private fun PriceManagementContent(
    state: PriceManagementUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    onEdit: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onClearActionMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProductSurface)
            .padding(40.dp)
            .verticalScroll(rememberScrollState())
            ,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Text("Manajemen Harga", color = ProductText, fontSize = 28.sp, fontWeight = FontWeight.Medium)

        PriceManagementToolbar(
            state = state,
            onSearchChanged = onSearchChanged,
            onCategorySelected = onCategorySelected
        )

        if (state.actionMessage != null) {
            ProductBanner(message = state.actionMessage)
            LaunchedEffect(state.actionMessage) {
                kotlinx.coroutines.delay(3000)
                onClearActionMessage()
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            when {
                state.isLoading && state.products.isEmpty() -> ProductLoadingState(modifier = Modifier.height(220.dp))
                state.error != null && state.products.isEmpty() -> ProductErrorState(message = state.error, onRetry = onRetry)
                state.visibleProducts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada produk ditemukan.", color = DashboardTextSecondary)
                    }
                }
                else -> PriceManagementTable(products = state.visibleProducts, onEdit = onEdit)
            }
            PriceManagementPagination(
                state = state,
                visibleCount = state.visibleProducts.size,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage
            )
        }
    }
}

@Composable
private fun PriceUpdateDialog(
    state: PriceManagementUiState,
    onDismiss: () -> Unit,
    onSave: (BigDecimal, BigDecimal, BigDecimal, BigDecimal) -> Unit
) {
    val stock = state.selectedProductStock ?: return
    
    var priceBuyStr by remember(state.selectedProductDetail) { 
        mutableStateOf(state.selectedProductDetail?.priceBuy?.toPlainString() ?: stock.priceBuy.toPlainString()) 
    }
    var priceRetailStr by remember(state.selectedProductDetail) { 
        mutableStateOf(state.selectedProductDetail?.priceRetail?.toPlainString() ?: stock.priceRetail.toPlainString()) 
    }
    var priceContractorStr by remember(state.selectedProductDetail) { 
        mutableStateOf(state.selectedProductDetail?.priceContractor?.toPlainString() ?: stock.priceContractor.toPlainString()) 
    }
    var discountStr by remember(state.selectedProductDetail) { 
        mutableStateOf(state.selectedProductDetail?.discount?.toPlainString() ?: stock.discount.toPlainString()) 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text("Edit Harga Produk", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DashboardTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stock.productName, fontSize = 14.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                Text("SKU: ${stock.sku}", fontSize = 12.sp, color = DashboardTextSecondary)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (state.isDetailLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                } else if (state.selectedProductDetail != null) {
                    // Form Edit
                    OutlinedTextField(
                        value = priceBuyStr,
                        onValueChange = { priceBuyStr = it },
                        label = { Text("Harga Beli (Modal)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = AdminLine
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = priceRetailStr,
                        onValueChange = { priceRetailStr = it },
                        label = { Text("Harga Retail (Eceran)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = AdminLine
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = priceContractorStr,
                        onValueChange = { priceContractorStr = it },
                        label = { Text("Harga Grosir / Kontraktor") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = AdminLine
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it },
                        label = { Text("Potongan Harga (Diskon Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = AdminLine
                        )
                    )
                    
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.error, color = Color.Red, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                            Text("Batal", color = DashboardTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val buy = priceBuyStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val retail = priceRetailStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val contractor = priceContractorStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val disc = discountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                onSave(buy, retail, contractor, disc)
                            },
                            enabled = !state.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Simpan Harga")
                            }
                        }
                    }
                }
            }
        }
    }
}
