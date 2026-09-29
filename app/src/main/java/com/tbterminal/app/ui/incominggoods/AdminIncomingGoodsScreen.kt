package com.tbterminal.app.ui.incominggoods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminIncomingGoodsScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    purchasingRepository: PurchasingRepository,
    documentNumberGenerator: DocumentNumberGenerator,
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
    onIncomingGoodsFormClick: (String?) -> Unit,
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: IncomingGoodsViewModel = viewModel(
        factory = IncomingGoodsViewModel.factory(
            inventoryRepository = inventoryRepository,
            purchasingRepository = purchasingRepository,
            documentNumberGenerator = documentNumberGenerator,
            autoSelectFirst = false
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.IncomingGoods,
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
        onIncomingGoodsFormClick = { onIncomingGoodsFormClick(null) },
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        IncomingGoodsListContent(
            modifier = contentModifier,
            uiState = uiState,
            onSearchChanged = viewModel::onProductSearchChanged,
            onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
            onOpenForm = { onIncomingGoodsFormClick(null) },
            onSelectProduct = { product -> onIncomingGoodsFormClick(product.productId) },
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
        )
    }
}

@Composable
fun AdminIncomingGoodsFormScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    purchasingRepository: PurchasingRepository,
    documentNumberGenerator: DocumentNumberGenerator,
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
    viewModel: IncomingGoodsViewModel = viewModel(
        factory = IncomingGoodsViewModel.factory(
            inventoryRepository = inventoryRepository,
            purchasingRepository = purchasingRepository,
            documentNumberGenerator = documentNumberGenerator,
            initialProductId = productId,
            autoSelectFirst = true
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.IncomingGoodsForm,
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
        onBack = onIncomingGoodsClick,
        onLogout = onLogout
    ) { contentModifier ->
        IncomingGoodsFormContent(
            modifier = contentModifier,
            uiState = uiState,
            onSelectProduct = viewModel::selectProduct,
            onSelectSupplier = viewModel::selectSupplier,
            onSupplierNameChanged = viewModel::onSupplierNameChanged,
            onCreateSupplier = viewModel::createSupplier,
            onInvoiceChanged = viewModel::onInvoiceChanged,
            onGenerateInvoiceNumber = viewModel::generateInvoiceNumber,
            onQuantityChanged = viewModel::onQuantityChanged,
            onBuyPriceChanged = viewModel::onBuyPriceChanged,
            onAmountPaidChanged = viewModel::onAmountPaidChanged,
            onDueDaysChanged = viewModel::onDueDaysChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onPaymentMethodChanged = viewModel::onPaymentMethodChanged,
            onSubmit = viewModel::submitIncomingGoods,
            onDismissMessage = viewModel::clearMessage
        )
    }
}

@Composable
private fun IncomingGoodsListContent(
    modifier: Modifier,
    uiState: IncomingGoodsUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onOpenForm: () -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF8FAFC))) {
        val compact = maxWidth < 720.dp
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 14.dp else 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 1180.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                IncomingGoodsAlert(uiState, onDismissMessage)
                ProductSelectorCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    compact = compact,
                    onSearchChanged = onSearchChanged,
                    onCategoryFilterChanged = onCategoryFilterChanged,
                    onOpenForm = onOpenForm,
                    onSelectProduct = onSelectProduct,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            }
        }
    }
}

@Composable
private fun IncomingGoodsFormContent(
    modifier: Modifier,
    uiState: IncomingGoodsUiState,
    onSelectProduct: (ProductStock) -> Unit,
    onSelectSupplier: (Supplier) -> Unit,
    onSupplierNameChanged: (String) -> Unit,
    onCreateSupplier: () -> Unit,
    onInvoiceChanged: (String) -> Unit,
    onGenerateInvoiceNumber: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onBuyPriceChanged: (String) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onDueDaysChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onPaymentMethodChanged: (IncomingPaymentMethod) -> Unit,
    onSubmit: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var showPaymentSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF8FAFC))) {
        val compact = maxWidth < 720.dp
        androidx.compose.material3.Scaffold(
            containerColor = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
            bottomBar = {
                androidx.compose.material3.Surface(color = IncomingSurface, shadowElevation = 3.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = 12.dp),
                        contentAlignment = if (compact) Alignment.Center else Alignment.CenterEnd,
                    ) {
                        IncomingGoodsSubmitButton(
                            uiState = uiState,
                            onSubmit = { showPaymentSheet = true },
                            modifier = if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(max = 280.dp),
                        )
                    }
                }
            }
        ) { innerPadding ->
            IncomingGoodsFormCard(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                uiState = uiState,
                compact = compact,
                onSelectProduct = onSelectProduct,
                onSelectSupplier = onSelectSupplier,
                onSupplierNameChanged = onSupplierNameChanged,
                onCreateSupplier = onCreateSupplier,
                onInvoiceChanged = onInvoiceChanged,
                onGenerateInvoiceNumber = onGenerateInvoiceNumber,
                onQuantityChanged = onQuantityChanged,
                onBuyPriceChanged = onBuyPriceChanged,
                onAmountPaidChanged = onAmountPaidChanged,
                onDueDaysChanged = onDueDaysChanged,
                onNotesChanged = onNotesChanged,
                onPaymentMethodChanged = onPaymentMethodChanged,
                onSubmit = onSubmit // Not used inside anymore
            )
            IncomingGoodsAlert(uiState, onDismissMessage)
        }

        if (showPaymentSheet) {
            IncomingPaymentBottomSheet(
                uiState = uiState,
                onPaymentMethodChanged = onPaymentMethodChanged,
                onAmountPaidChanged = onAmountPaidChanged,
                onDueDaysChanged = onDueDaysChanged,
                onSubmit = {
                    showPaymentSheet = false
                    onSubmit()
                },
                onDismiss = { showPaymentSheet = false }
            )
        }
    }
}

@Composable
private fun IncomingGoodsAlert(
    uiState: IncomingGoodsUiState,
    onDismiss: () -> Unit
) {
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val tint = if (isError) androidx.compose.ui.graphics.Color(0xFFEF4444) else androidx.compose.ui.graphics.Color(0xFF059669)
    val background = if (isError) androidx.compose.ui.graphics.Color(0xFFEF4444).copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Outlined.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = tint, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup", color = tint) }
    }
}

@Composable
private fun IncomingGoodsFormCard(
    modifier: Modifier,
    uiState: IncomingGoodsUiState,
    compact: Boolean,
    onSelectProduct: (ProductStock) -> Unit,
    onSelectSupplier: (Supplier) -> Unit,
    onSupplierNameChanged: (String) -> Unit,
    onCreateSupplier: () -> Unit,
    onInvoiceChanged: (String) -> Unit,
    onGenerateInvoiceNumber: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onBuyPriceChanged: (String) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onDueDaysChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onPaymentMethodChanged: (IncomingPaymentMethod) -> Unit,
    onSubmit: () -> Unit
) {
    Box(modifier = modifier) {
        IncomingGoodsFormBody(
            uiState = uiState,
            compact = compact,
            onSelectProduct = onSelectProduct,
            onSelectSupplier = onSelectSupplier,
            onSupplierNameChanged = onSupplierNameChanged,
            onCreateSupplier = onCreateSupplier,
            onInvoiceChanged = onInvoiceChanged,
            onGenerateInvoiceNumber = onGenerateInvoiceNumber,
            onQuantityChanged = onQuantityChanged,
            onBuyPriceChanged = onBuyPriceChanged,
            onAmountPaidChanged = onAmountPaidChanged,
            onDueDaysChanged = onDueDaysChanged,
            onNotesChanged = onNotesChanged,
            
            )
    }
}

