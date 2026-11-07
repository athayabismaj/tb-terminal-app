package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalContext
import com.tbterminal.app.ui.managerapproval.ManagerApprovalDialog
import com.tbterminal.app.ui.common.UiText
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.checkout.components.HeldCartsSheet
import com.tbterminal.app.ui.checkout.components.HoldCartDialog
import com.tbterminal.app.ui.checkout.components.QuickPinLockOverlay
import com.tbterminal.app.ui.checkout.components.QrisPaymentDialog
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPosRoute(
    name: String,
    role: String,
    checkoutRepository: CheckoutRepository,
    inventoryRepository: InventoryRepository,
    customerRepository: CustomerRepository,
    cashReconciliationRepository: CashReconciliationRepository,
    managerApprovalRepository: ManagerApprovalRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    onNavigateToReceipt: (String) -> Unit = {},
    viewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.factory(
            checkoutRepository = checkoutRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            cashReconciliationRepository = cashReconciliationRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var approvalAttemptId by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showMaterialEstimator by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Semua Kategori") }
    var onlyInStock by rememberSaveable { mutableStateOf(false) }

    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()
    val isFiltered = (selectedCategory != "Semua Kategori") || onlyInStock

    LaunchedEffect(uiState.checkoutPreview?.checkoutAttemptId, uiState.checkoutPreview?.approvalRequired) {
        val preview = uiState.checkoutPreview
        if (preview?.approvalRequired == true) approvalAttemptId = preview.checkoutAttemptId
    }

    LaunchedEffect(viewModel.errorEvent, snackbarHostState) {
        viewModel.errorEvent
            .filterNotNull()
            .collect { error ->
                feedbackMessage = error.message()
                snackbarHostState.showSnackbar(error.message())
                viewModel.clearErrorEvent()
            }
    }

    LaunchedEffect(viewModel.checkoutEvents, onNavigateToReceipt) {
        viewModel.checkoutEvents.collect { event ->
            when (event) {
                is CheckoutEvent.NavigateToReceipt -> onNavigateToReceipt(event.transactionId)
            }
        }
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.Pos,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            Surface(
                onClick = { isSearchExpanded = !isSearchExpanded },
                shape = RoundedCornerShape(12.dp),
                color = if (isSearchActive) Color(0xFFE8F6ED) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isSearchActive) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("pos-toggle-search")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari produk kasir",
                        tint = if (isSearchActive) Color(0xFF004528) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Button 2: Filter [ ⚙️ ]
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { showFilterDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFiltered) Color(0xFFE8F6ED) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isFiltered) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pos-open-filter")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Filter produk kasir",
                            tint = if (isFiltered) Color(0xFF004528) else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .size(7.dp)
                            .background(Color(0xFFE5A93C), CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Kalkulator Bahan Bangunan [ 🧮 ]
            Surface(
                onClick = { showMaterialEstimator = true },
                shape = RoundedCornerShape(12.dp),
                color = if (showMaterialEstimator) Color(0xFFE8F6ED) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (showMaterialEstimator) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("pos-open-calculator")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = "Hitung estimasi bahan bangunan",
                        tint = if (showMaterialEstimator) Color(0xFF004528) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout,
        onLockScreen = viewModel::lockScreen
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            TopFloatingCapsule(
                message = feedbackMessage ?: uiState.quickFeedbackMessage,
                isError = feedbackMessage != null,
                onDismiss = {
                    feedbackMessage = null
                    viewModel.clearQuickFeedback()
                }
            )

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = SurfaceBright,
                contentWindowInsets = WindowInsets(0.dp),
                modifier = Modifier.fillMaxSize()
            ) { contentPadding ->
            CashierPosScreen(
                state = uiState,
                onSearchChanged = viewModel::onSearchChanged,
                onRefreshProducts = viewModel::loadProducts,
                onProductPageChanged = viewModel::onProductPageChanged,
                onProductClick = viewModel::addProduct,
                onCustomerSearchChanged = viewModel::onCustomerSearchChanged,
                onSelectCustomer = viewModel::selectCustomer,
                onClearCustomer = viewModel::clearSelectedCustomer,
                onUseCustomerName = viewModel::useQuickCustomerName,
                onRefreshCustomers = viewModel::loadCustomers,
                onUpdateQty = viewModel::updateCartQuantity,
                onToggleUnit = viewModel::toggleCartItemUnit,
                onSelectPayment = viewModel::selectPaymentMethod,
                onAmountPaidChanged = viewModel::onAmountPaidChanged,
                onStartingCashChanged = viewModel::onStartingCashChanged,
                onOpenCashSession = viewModel::openCashSession,
                onRefreshCashSession = viewModel::loadCashSession,
                onCheckout = {
                    val preview = uiState.checkoutPreview
                    if (uiState.hasAmbiguousCheckout) {
                        viewModel.prepareCheckout(uiState.selectedPaymentMethod, uiState.finalTotal)
                    } else if (preview?.approvalRequired == true) {
                        val approvalId = uiState.approvedManagerApprovalId
                        if (approvalId != null) {
                            viewModel.continueCheckoutWithApproval(approvalId, preview.checkoutAttemptId)
                        } else {
                            approvalAttemptId = preview.checkoutAttemptId
                        }
                    } else {
                        viewModel.prepareCheckout(
                            paymentMethod = uiState.selectedPaymentMethod,
                            amountPaid = uiState.finalTotal,
                        )
                    }
                },
                onNavigateToCart = onNavigateToCart,
                onScanBarcode = { barcode -> viewModel.onScanBarcode(barcode, context) },
                onClearScanNotification = viewModel::clearScanNotification,
                onOpenHoldCartDialog = viewModel::openHoldCartDialog,
                onOpenHeldCarts = viewModel::openHeldCartsSheet,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onlyInStock = onlyInStock,
                onToggleOnlyInStock = { onlyInStock = it },
                showFilterDialog = showFilterDialog,
                onDismissFilterDialog = { showFilterDialog = false },
                showMaterialEstimator = showMaterialEstimator,
                onDismissMaterialEstimator = { showMaterialEstimator = false },
                onApplyMaterialSearch = { query ->
                    isSearchExpanded = true
                    viewModel.onSearchChanged(query)
                    showMaterialEstimator = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
    }
}
    if (uiState.isHoldCartDialogOpen) {
        HoldCartDialog(
            itemCount = uiState.cartItems.sumOf { it.quantity },
            defaultNote = uiState.selectedCustomer?.name?.let { "Pesanan $it" } ?: "",
            onDismiss = viewModel::dismissHoldCartDialog,
            onConfirm = viewModel::holdCurrentCart
        )
    }
    if (uiState.isHeldCartsSheetOpen) {
        HeldCartsSheet(
            heldCarts = uiState.heldCarts,
            onDismiss = viewModel::dismissHeldCartsSheet,
            onResumeCart = viewModel::resumeHeldCart,
            onDeleteCart = viewModel::deleteHeldCart
        )
    }
    if (uiState.isScreenLocked) {
        QuickPinLockOverlay(
            cashierName = name,
            pinInput = uiState.lockPinInput,
            errorMessage = uiState.lockPinError,
            onPinChange = viewModel::updateLockPinInput,
            onUnlock = { viewModel.unlockScreen() }
        )
    }
    if (uiState.isQrisDialogOpen) {
        QrisPaymentDialog(
            amount = uiState.finalTotal,
            storeName = "TB TERMINAL",
            onConfirmPayment = viewModel::confirmQrisPayment,
            onDismissRequest = viewModel::dismissQrisDialog
        )
    }
    if (uiState.isPettyCashDialogOpen) {
        com.tbterminal.app.ui.cash.components.PettyCashDialog(
            selectedType = uiState.pettyCashType,
            amountInput = uiState.pettyCashAmountInput,
            categoryInput = uiState.pettyCashCategoryInput,
            descriptionInput = uiState.pettyCashDescriptionInput,
            isSubmitting = uiState.isPettyCashSubmitting,
            errorMessage = uiState.pettyCashError,
            onTypeChanged = viewModel::onPettyCashTypeChanged,
            onAmountChanged = viewModel::onPettyCashAmountChanged,
            onCategoryChanged = viewModel::onPettyCashCategoryChanged,
            onDescriptionChanged = viewModel::onPettyCashDescriptionChanged,
            onDismiss = viewModel::dismissPettyCashDialog,
            onConfirm = viewModel::submitPettyCash
        )
    }
    approvalAttemptId?.let { attemptId ->
        ManagerApprovalDialog(
            context = ManagerApprovalContext(
                action = ManagerApprovalAction.DISCOUNT_OVERRIDE,
                resourceId = attemptId,
            ),
            repository = managerApprovalRepository,
            onDismiss = { approvalAttemptId = null },
            onApproved = { grant ->
                approvalAttemptId = null
                viewModel.continueCheckoutWithApproval(grant.approvalId, grant.resourceId)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierCartRoute(
    name: String,
    role: String,
    checkoutRepository: CheckoutRepository,
    inventoryRepository: InventoryRepository,
    customerRepository: CustomerRepository,
    cashReconciliationRepository: CashReconciliationRepository,
    managerApprovalRepository: ManagerApprovalRepository,
    onBackToPos: () -> Unit,
    onNavigateToPayment: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToReceipt: (String) -> Unit = {},
    viewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.factory(
            checkoutRepository = checkoutRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            cashReconciliationRepository = cashReconciliationRepository
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var approvalAttemptId by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.checkoutPreview?.checkoutAttemptId, uiState.checkoutPreview?.approvalRequired) {
        val preview = uiState.checkoutPreview
        if (preview?.approvalRequired == true) approvalAttemptId = preview.checkoutAttemptId
    }

    LaunchedEffect(viewModel.errorEvent, snackbarHostState) {
        viewModel.errorEvent
            .filterNotNull()
            .collect { error ->
                feedbackMessage = error.message()
                snackbarHostState.showSnackbar(error.message())
                viewModel.clearErrorEvent()
            }
    }

    LaunchedEffect(viewModel.checkoutEvents, onNavigateToReceipt) {
        viewModel.checkoutEvents.collect { event ->
            when (event) {
                is CheckoutEvent.NavigateToReceipt -> onNavigateToReceipt(event.transactionId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TopFloatingCapsule(
            message = feedbackMessage ?: uiState.quickFeedbackMessage,
            isError = feedbackMessage != null,
            onDismiss = {
                feedbackMessage = null
                viewModel.clearQuickFeedback()
            }
        )

        CashierCartScreen(
            name = name,
            role = role,
        state = uiState,
        snackbarHostState = snackbarHostState,
        onIncrease = { cartItemId ->
            val currentItem = uiState.cartItems.firstOrNull { it.cartItemId == cartItemId }
            if (currentItem != null) {
                viewModel.updateCartQuantity(cartItemId, currentItem.quantity + 1)
            }
        },
        onDecrease = { cartItemId ->
            val currentItem = uiState.cartItems.firstOrNull { it.cartItemId == cartItemId }
            if (currentItem != null) {
                viewModel.updateCartQuantity(cartItemId, currentItem.quantity - 1)
            }
        },
        onRemove = { cartItemId ->
            viewModel.updateCartQuantity(cartItemId, 0)
        },
        onCustomerSearchChanged = viewModel::onCustomerSearchChanged,
        onSelectCustomer = viewModel::selectCustomer,
        onClearCustomer = viewModel::clearSelectedCustomer,
        onUseCustomerName = viewModel::useQuickCustomerName,
        onSelectPayment = viewModel::selectPaymentMethod,
        onAmountPaidChanged = viewModel::onAmountPaidChanged,
        onClearCart = {
            viewModel.replaceCart(emptyList())
        },
        onItemDiscountChanged = viewModel::setItemDiscount,
        onTransactionDiscountChanged = viewModel::setTransactionDiscount,
        onToggleUnit = viewModel::toggleCartItemUnit,
        onCheckout = {
            val preview = uiState.checkoutPreview
            if (uiState.hasAmbiguousCheckout) {
                viewModel.prepareCheckout(uiState.selectedPaymentMethod, uiState.finalTotal)
            } else if (preview?.approvalRequired == true) {
                val approvalId = uiState.approvedManagerApprovalId
                if (approvalId != null) {
                    viewModel.continueCheckoutWithApproval(approvalId, preview.checkoutAttemptId)
                } else {
                    approvalAttemptId = preview.checkoutAttemptId
                }
            } else {
                viewModel.prepareCheckout(
                    paymentMethod = uiState.selectedPaymentMethod,
                    amountPaid = uiState.finalTotal,
                )
            }
        },
        onBackToPos = onBackToPos,
        onNavigateToPayment = onNavigateToPayment,
        onOpenHoldCartDialog = viewModel::openHoldCartDialog,
        onOpenHeldCarts = viewModel::openHeldCartsSheet,
        onToggleDelivery = viewModel::toggleDelivery,
        onDeliveryFeeChanged = viewModel::updateDeliveryFee,
        onDeliveryAddressChanged = viewModel::updateDeliveryAddress,
        onDeliveryCourierChanged = viewModel::updateDeliveryCourier,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout,
        onScanBarcode = { barcode -> viewModel.onScanBarcode(barcode, context) },
        onOpenQrisDialog = viewModel::openQrisDialog,
        onOpenPettyCashDialog = { viewModel.openPettyCashDialog() },
        onOpenQuickAddCustomer = viewModel::openQuickAddCustomer,
        onDismissQuickAddCustomer = viewModel::dismissQuickAddCustomer,
        onQuickCreateCustomer = viewModel::quickCreateCustomer,
        onLockScreen = viewModel::lockScreen
    )
}
    if (uiState.isHoldCartDialogOpen) {
        HoldCartDialog(
            itemCount = uiState.cartItems.sumOf { it.quantity },
            defaultNote = uiState.selectedCustomer?.name?.let { "Pesanan $it" } ?: "",
            onDismiss = viewModel::dismissHoldCartDialog,
            onConfirm = viewModel::holdCurrentCart
        )
    }
    if (uiState.isHeldCartsSheetOpen) {
        HeldCartsSheet(
            heldCarts = uiState.heldCarts,
            onDismiss = viewModel::dismissHeldCartsSheet,
            onResumeCart = viewModel::resumeHeldCart,
            onDeleteCart = viewModel::deleteHeldCart
        )
    }
    if (uiState.isScreenLocked) {
        QuickPinLockOverlay(
            cashierName = name,
            pinInput = uiState.lockPinInput,
            errorMessage = uiState.lockPinError,
            onPinChange = viewModel::updateLockPinInput,
            onUnlock = { viewModel.unlockScreen() }
        )
    }
    if (uiState.isQrisDialogOpen) {
        QrisPaymentDialog(
            amount = uiState.finalTotal,
            storeName = "TB TERMINAL",
            onConfirmPayment = viewModel::confirmQrisPayment,
            onDismissRequest = viewModel::dismissQrisDialog
        )
    }
    if (uiState.isPettyCashDialogOpen) {
        com.tbterminal.app.ui.cash.components.PettyCashDialog(
            selectedType = uiState.pettyCashType,
            amountInput = uiState.pettyCashAmountInput,
            categoryInput = uiState.pettyCashCategoryInput,
            descriptionInput = uiState.pettyCashDescriptionInput,
            isSubmitting = uiState.isPettyCashSubmitting,
            errorMessage = uiState.pettyCashError,
            onTypeChanged = viewModel::onPettyCashTypeChanged,
            onAmountChanged = viewModel::onPettyCashAmountChanged,
            onCategoryChanged = viewModel::onPettyCashCategoryChanged,
            onDescriptionChanged = viewModel::onPettyCashDescriptionChanged,
            onDismiss = viewModel::dismissPettyCashDialog,
            onConfirm = viewModel::submitPettyCash
        )
    }
    approvalAttemptId?.let { attemptId ->
        ManagerApprovalDialog(
            context = ManagerApprovalContext(
                action = ManagerApprovalAction.DISCOUNT_OVERRIDE,
                resourceId = attemptId,
            ),
            repository = managerApprovalRepository,
            onDismiss = { approvalAttemptId = null },
            onApproved = { grant ->
                approvalAttemptId = null
                viewModel.continueCheckoutWithApproval(grant.approvalId, grant.resourceId)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPaymentRoute(
    name: String,
    role: String,
    checkoutRepository: CheckoutRepository,
    inventoryRepository: InventoryRepository,
    customerRepository: CustomerRepository,
    cashReconciliationRepository: CashReconciliationRepository,
    managerApprovalRepository: ManagerApprovalRepository,
    onBackToCart: () -> Unit,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToReceipt: (String) -> Unit = {},
    viewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.factory(
            checkoutRepository = checkoutRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            cashReconciliationRepository = cashReconciliationRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var approvalAttemptId by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.checkoutPreview?.checkoutAttemptId, uiState.checkoutPreview?.approvalRequired) {
        val preview = uiState.checkoutPreview
        if (preview?.approvalRequired == true) approvalAttemptId = preview.checkoutAttemptId
    }

    LaunchedEffect(viewModel.errorEvent, snackbarHostState) {
        viewModel.errorEvent
            .filterNotNull()
            .collect { error ->
                feedbackMessage = error.message()
                snackbarHostState.showSnackbar(error.message())
                viewModel.clearErrorEvent()
            }
    }

    LaunchedEffect(viewModel.checkoutEvents, onNavigateToReceipt) {
        viewModel.checkoutEvents.collect { event ->
            when (event) {
                is CheckoutEvent.NavigateToReceipt -> onNavigateToReceipt(event.transactionId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TopFloatingCapsule(
            message = feedbackMessage ?: uiState.quickFeedbackMessage,
            isError = feedbackMessage != null,
            onDismiss = {
                feedbackMessage = null
                viewModel.clearQuickFeedback()
            }
        )

        CashierPaymentScreen(
            name = name,
            role = role,
            state = uiState,
            snackbarHostState = snackbarHostState,
            onCustomerSearchChanged = viewModel::onCustomerSearchChanged,
            onSelectCustomer = viewModel::selectCustomer,
            onClearCustomer = viewModel::clearSelectedCustomer,
            onUseCustomerName = viewModel::useQuickCustomerName,
            onSelectPayment = viewModel::selectPaymentMethod,
            onAmountPaidChanged = viewModel::onAmountPaidChanged,
            onTransactionDiscountChanged = viewModel::setTransactionDiscount,
            onCheckout = {
                val preview = uiState.checkoutPreview
                if (uiState.hasAmbiguousCheckout) {
                    viewModel.prepareCheckout(uiState.selectedPaymentMethod, uiState.finalTotal)
                } else if (preview?.approvalRequired == true) {
                    val approvalId = uiState.approvedManagerApprovalId
                    if (approvalId != null) {
                        viewModel.continueCheckoutWithApproval(approvalId, preview.checkoutAttemptId)
                    } else {
                        approvalAttemptId = preview.checkoutAttemptId
                    }
                } else {
                    viewModel.prepareCheckout(
                        paymentMethod = uiState.selectedPaymentMethod,
                        amountPaid = uiState.finalTotal,
                    )
                }
            },
            onBackToCart = onBackToCart,
            onOpenHoldCartDialog = viewModel::openHoldCartDialog,
            onToggleDelivery = viewModel::toggleDelivery,
            onDeliveryFeeChanged = viewModel::updateDeliveryFee,
            onDeliveryAddressChanged = viewModel::updateDeliveryAddress,
            onDeliveryCourierChanged = viewModel::updateDeliveryCourier,
            onDashboardClick = onDashboardClick,
            onPosClick = onPosClick,
            onCashSessionClick = onCashSessionClick,
            onTransactionHistoryClick = onTransactionHistoryClick,
            onStockCheckClick = onStockCheckClick,
            onProfileClick = onProfileClick,
            onSettingsClick = onSettingsClick,
            onLogout = onLogout,
            onOpenQrisDialog = viewModel::openQrisDialog,
            onOpenQuickAddCustomer = viewModel::openQuickAddCustomer,
            onDismissQuickAddCustomer = viewModel::dismissQuickAddCustomer,
            onQuickCreateCustomer = viewModel::quickCreateCustomer,
            onLockScreen = viewModel::lockScreen
        )
    }

    if (uiState.isHoldCartDialogOpen) {
        HoldCartDialog(
            itemCount = uiState.cartItems.sumOf { it.quantity },
            defaultNote = uiState.selectedCustomer?.name?.let { "Pesanan $it" } ?: "",
            onDismiss = viewModel::dismissHoldCartDialog,
            onConfirm = viewModel::holdCurrentCart
        )
    }
    if (uiState.isScreenLocked) {
        QuickPinLockOverlay(
            cashierName = name,
            pinInput = uiState.lockPinInput,
            errorMessage = uiState.lockPinError,
            onPinChange = viewModel::updateLockPinInput,
            onUnlock = { viewModel.unlockScreen() }
        )
    }
    if (uiState.isQrisDialogOpen) {
        QrisPaymentDialog(
            amount = uiState.finalTotal,
            storeName = "TB TERMINAL",
            onConfirmPayment = viewModel::confirmQrisPayment,
            onDismissRequest = viewModel::dismissQrisDialog
        )
    }
    approvalAttemptId?.let { attemptId ->
        ManagerApprovalDialog(
            context = ManagerApprovalContext(
                action = ManagerApprovalAction.DISCOUNT_OVERRIDE,
                resourceId = attemptId,
            ),
            repository = managerApprovalRepository,
            onDismiss = { approvalAttemptId = null },
            onApproved = { grant ->
                approvalAttemptId = null
                viewModel.continueCheckoutWithApproval(grant.approvalId, grant.resourceId)
            },
        )
    }
}

private fun UiText.message(): String {
    return when (this) {
        is UiText.DynamicString -> value
    }
}
