package com.tbterminal.app.ui.incominggoods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LocalShipping
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
import androidx.compose.ui.unit.sp
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
            onRefresh = viewModel::refresh,
            onSelectProduct = { product -> onIncomingGoodsFormClick(product.productId) },
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
    onRefresh: () -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IncomingBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        IncomingGoodsHeader()
        IncomingGoodsMessage(uiState, onDismissMessage)
        ProductSelectorCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onRefresh = onRefresh,
            onSelectProduct = onSelectProduct
        )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IncomingBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        IncomingGoodsHeader()
        IncomingGoodsMessage(uiState, onDismissMessage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            IncomingGoodsFormCard(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                uiState = uiState,
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
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
private fun IncomingGoodsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text("Barang Masuk", color = IncomingText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Catat restok dari supplier, perbarui HPP, dan buat utang jika belum lunas.",
                color = IncomingMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(IncomingPrimary.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = IncomingPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mode restok supplier", color = IncomingPrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IncomingGoodsMessage(
    uiState: IncomingGoodsUiState,
    onDismiss: () -> Unit
) {
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val tint = if (isError) IncomingDanger else IncomingPrimaryDark
    val background = if (isError) IncomingDanger.copy(alpha = 0.1f) else IncomingPrimary.copy(alpha = 0.1f)

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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = IncomingSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IncomingLine)
    ) {
        IncomingGoodsFormBody(
            uiState = uiState,
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
            onSubmit = onSubmit
        )
    }
}
