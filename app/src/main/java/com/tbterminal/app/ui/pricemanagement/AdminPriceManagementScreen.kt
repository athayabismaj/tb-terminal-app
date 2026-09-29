package com.tbterminal.app.ui.pricemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.products.ProductBanner
import com.tbterminal.app.ui.products.ProductErrorState
import com.tbterminal.app.ui.products.ProductLoadingState
import com.tbterminal.app.ui.products.ProductPrimaryDark
import com.tbterminal.app.ui.products.ProductText
import java.math.BigDecimal

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
        pageTitle = "Harga Produk",
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
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val compact = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)) {

        PriceManagementToolbar(
            state = state,
            onSearchChanged = onSearchChanged,
            onCategorySelected = onCategorySelected,
            compact = compact,
        )

        if (state.actionMessage != null) {
            ProductBanner(message = state.actionMessage)
            LaunchedEffect(state.actionMessage) {
                kotlinx.coroutines.delay(3000)
                onClearActionMessage()
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Daftar harga",
                color = ProductText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            when {
                state.isLoading && state.products.isEmpty() -> ProductLoadingState(modifier = Modifier.height(220.dp))
                state.error != null && state.products.isEmpty() -> ProductErrorState(message = state.error, onRetry = onRetry)
                state.visibleProducts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada produk ditemukan.", color = DashboardTextSecondary)
                    }
                }
                else -> {
                    PriceManagementTable(products = state.visibleProducts, onEdit = onEdit, compact = compact)
                }
            }
            PriceManagementPagination(
                state = state,
                visibleCount = state.visibleProducts.size,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PriceUpdateDialog(
    state: PriceManagementUiState,
    onDismiss: () -> Unit,
    onSave: (BigDecimal, BigDecimal, BigDecimal, BigDecimal) -> Unit
) {
    val stock = state.selectedProductStock ?: return
    var priceBuyStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.priceBuy?.toPlainString() ?: stock.priceBuy.toPlainString())
    }
    var priceRetailStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.priceRetail?.toPlainString() ?: stock.priceRetail.toPlainString())
    }
    var priceContractorStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.priceContractor?.toPlainString() ?: stock.priceContractor.toPlainString())
    }
    var discountStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.discount?.toPlainString() ?: stock.discount.toPlainString())
    }
    val validInput = listOf(priceBuyStr, priceRetailStr, priceContractorStr, discountStr)
        .all { it.toBigDecimalOrNull() != null }

    ModalBottomSheet(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("price-edit-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Edit harga",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = ProductText,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss, enabled = !state.isSaving, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = DashboardTextSecondary)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stock.productName, color = ProductText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("SKU ${stock.sku}", color = DashboardTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.isDetailLoading) {
                com.tbterminal.app.ui.components.SkeletonList(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    itemCount = 3,
                )
            } else if (state.selectedProductDetail != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriceEditField("Harga beli", priceBuyStr, { priceBuyStr = it }, "price-edit-buy", !state.isSaving)
                    PriceEditField("Harga jual", priceRetailStr, { priceRetailStr = it }, "price-edit-retail", !state.isSaving)
                    PriceEditField("Harga kontraktor", priceContractorStr, { priceContractorStr = it }, "price-edit-contractor", !state.isSaving)
                    PriceEditField("Diskon", discountStr, { discountStr = it }, "price-edit-discount", !state.isSaving)
                }
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) { Text("Batal") }
                    Button(
                        onClick = {
                            val buy = priceBuyStr.toBigDecimalOrNull() ?: return@Button
                            val retail = priceRetailStr.toBigDecimalOrNull() ?: return@Button
                            val contractor = priceContractorStr.toBigDecimalOrNull() ?: return@Button
                            val discount = discountStr.toBigDecimalOrNull() ?: return@Button
                            onSave(buy, retail, contractor, discount)
                        },
                        enabled = !state.isSaving && validInput,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("price-edit-save"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProductPrimaryDark),
                    ) {
                        if (state.isSaving) CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        ) else Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("Rp") },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ProductPrimaryDark,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
    )
}
