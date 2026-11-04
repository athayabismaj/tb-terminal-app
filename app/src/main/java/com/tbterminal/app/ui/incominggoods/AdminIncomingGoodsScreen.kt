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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

import androidx.activity.compose.BackHandler

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
    onBack: () -> Unit = {},
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
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    val isFiltered = uiState.isFiltered

    BackHandler(onBack = onBack)

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.IncomingGoods,
        pageTitle = "Barang Masuk",
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("incoming-goods-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari produk",
                    tint = if (isSearchExpanded || uiState.productSearchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
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
                    modifier = Modifier.size(36.dp).testTag("incoming-goods-open-filters")
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
        },
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
        onBack = onBack,
        onLogout = onLogout
    ) { contentModifier ->
        IncomingGoodsListContent(
            modifier = contentModifier,
            uiState = uiState,
            isSearchExpanded = isSearchExpanded,
            onToggleSearch = { isSearchExpanded = !isSearchExpanded },
            showFilterPopup = showFilterPopup,
            onDismissFilter = { showFilterPopup = false },
            showSummaryPopup = showSummaryPopup,
            onDismissSummary = { showSummaryPopup = false },
            onSearchChanged = viewModel::onProductSearchChanged,
            onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
            onApplyFilters = viewModel::onApplyFilters,
            onResetFilters = viewModel::onResetFilters,
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
    onBack: () -> Unit = onIncomingGoodsClick,
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

    BackHandler(onBack = onBack)

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
        onBack = onBack,
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
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    showFilterPopup: Boolean,
    onDismissFilter: () -> Unit,
    showSummaryPopup: Boolean,
    onDismissSummary: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onApplyFilters: (String?, IncomingStockStatusFilter, IncomingProductSortOrder) -> Unit = { _, _, _ -> },
    onResetFilters: () -> Unit = {},
    onOpenForm: () -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF8FAFC))) {
        val compact = maxWidth < 700.dp
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 12.dp else 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 1280.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                ProductSelectorCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    compact = compact,
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = onToggleSearch,
                    showFilterPopup = showFilterPopup,
                    onDismissFilter = onDismissFilter,
                    showSummaryPopup = showSummaryPopup,
                    onDismissSummary = onDismissSummary,
                    onSearchChanged = onSearchChanged,
                    onCategoryFilterChanged = onCategoryFilterChanged,
                    onApplyFilters = onApplyFilters,
                    onResetFilters = onResetFilters,
                    onOpenForm = onOpenForm,
                    onSelectProduct = onSelectProduct,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
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

        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
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

