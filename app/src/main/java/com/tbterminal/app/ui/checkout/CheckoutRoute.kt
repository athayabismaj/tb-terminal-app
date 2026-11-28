package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Schedule
import com.tbterminal.app.ui.quotations.formatQuotationMoney
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

@Composable
private fun RefreshCashSessionOnResume(
    onResume: () -> Unit,
) {
    val currentOnResume by rememberUpdatedState(onResume)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

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
    RefreshCashSessionOnResume { viewModel.refreshCashSessionOnResume() }
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
        showHeader = false,
        topBarActions = {
            // Button 1: Search [ 🔍 ] (Rounded-full 32x32 dp matching HTML mockup)
            Surface(
                onClick = {
                    if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) {
                        isSearchExpanded = false
                        viewModel.onSearchChanged("")
                    } else {
                        isSearchExpanded = true
                    }
                },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("pos-toggle-search")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari produk kasir",
                        tint = if (isSearchActive) Color(0xFF10533C) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Button 2: Filter [ ⚙️ ] (Rounded-full 32x32 dp matching HTML mockup)
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { showFilterDialog = true },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pos-open-filter")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Filter produk kasir",
                            tint = if (isFiltered) Color(0xFF10533C) else Color(0xFF475569),
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

            // Button 3: Kalkulator Bahan Bangunan [ 🧮 ] (Rounded-full 32x32 dp matching HTML mockup)
            Surface(
                onClick = { showMaterialEstimator = true },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("pos-open-calculator")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = "Hitung estimasi bahan bangunan",
                        tint = if (showMaterialEstimator) Color(0xFF10533C) else Color(0xFF475569),
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
                containerColor = Color(0xFFF8FAF9),
                contentWindowInsets = WindowInsets(0.dp),
                modifier = Modifier.fillMaxSize()
            ) { contentPadding ->
            CashierPosScreen(
                state = uiState,
                onSearchChanged = viewModel::onSearchChanged,
                onRefreshProducts = viewModel::refreshProducts,
                onProductPageChanged = viewModel::onProductPageChanged,
                onLoadMoreProducts = viewModel::loadMoreProducts,
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
                onCashSessionClick = onCashSessionClick,
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
                onOpenMaterialEstimator = { showMaterialEstimator = true },
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
            onUnlock = { viewModel.unlockScreen() },
            isLoading = uiState.isUnlockingScreen,
            isLockedOut = uiState.lockScreenRemainingSeconds > 0L,
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
    onOpenQuotations: () -> Unit = {},
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
    RefreshCashSessionOnResume { viewModel.refreshCashSessionOnResume() }
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
        onLockScreen = viewModel::lockScreen,
        onOpenSaveQuotation = viewModel::openSaveQuotationDialog,
        onOpenQuotations = onOpenQuotations,
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
    if (uiState.isSaveQuotationDialogOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSaveQuotationDialog,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            scrimColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Simpan Penawaran Harga (RAB)",
                            fontSize = 18.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Simpan keranjang menjadi dokumen RAB tanpa memotong stok",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(onClick = viewModel::dismissSaveQuotationDialog) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp
                )

                // Info Box Keranjang
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val custName = uiState.selectedCustomer?.name
                                ?: uiState.customerSearchQuery.trim().takeIf { it.isNotEmpty() }
                                ?: "Pelanggan Umum"
                            Text(
                                text = "Pelanggan: $custName",
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${uiState.cartItems.size} barang • Total: ${formatQuotationMoney(uiState.finalTotal)}",
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = Color(0xFF10533C)
                            )
                        }
                    }
                }

                // Field 1: Masa Berlaku (Hari)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Masa Berlaku (Hari) *",
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    OutlinedTextField(
                        value = uiState.quotationValidDaysInput,
                        onValueChange = viewModel::onQuotationValidDaysChanged,
                        placeholder = {
                            Text("14", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "hari",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10533C),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = Color(0xFF10533C),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                        )
                    )
                }

                // Field 2: Catatan Penawaran
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Catatan Penawaran (Opsional)",
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    OutlinedTextField(
                        value = uiState.quotationNotesInput,
                        onValueChange = viewModel::onQuotationNotesChanged,
                        placeholder = {
                            Text(
                                "Contoh: Penawaran renovasi ruko, ongkir ditanggung pembeli...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10533C),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = Color(0xFF10533C),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Button: Simpan RAB
                Button(
                    onClick = viewModel::submitQuotation,
                    enabled = !uiState.isSavingQuotation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10533C)
                    )
                ) {
                    if (uiState.isSavingQuotation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MENYIMPAN DOKUMEN RAB...",
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIMPAN DOKUMEN RAB",
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Action Button: Batal
                TextButton(
                    onClick = viewModel::dismissSaveQuotationDialog,
                    enabled = !uiState.isSavingQuotation,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 13.5.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
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
            onUnlock = { viewModel.unlockScreen() },
            isLoading = uiState.isUnlockingScreen,
            isLockedOut = uiState.lockScreenRemainingSeconds > 0L,
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
    if (uiState.pendingReorderConflict != null) {
        val conflict = uiState.pendingReorderConflict!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissReorderConflict,
            title = {
                androidx.compose.material3.Text("Keranjang Sudah Berisi Barang")
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Anda menyalin ${conflict.plan.items.size} item dari nota #${conflict.receiptNumber}. Pilih cara memuat ke keranjang:"
                    )
                    if (conflict.plan.adjustedItems.isNotEmpty() || conflict.plan.skippedItems.isNotEmpty()) {
                        androidx.compose.material3.Text(
                            text = conflict.plan.buildFeedbackMessage(),
                            color = Color(0xFFB45309)
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { viewModel.resolveReorderConflict(replaceExisting = true) }
                ) {
                    androidx.compose.material3.Text("Ganti Keranjang")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = viewModel::dismissReorderConflict
                    ) {
                        androidx.compose.material3.Text("Batal")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.resolveReorderConflict(replaceExisting = false) }
                    ) {
                        androidx.compose.material3.Text("Tambahkan")
                    }
                }
            }
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
    onOpenQuotations: () -> Unit = {},
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
    RefreshCashSessionOnResume { viewModel.refreshCashSessionOnResume() }
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
            customerRepository = customerRepository,
            onCustomerSearchChanged = viewModel::onCustomerSearchChanged,
            onSelectCustomer = viewModel::selectCustomer,
            onClearCustomer = viewModel::clearSelectedCustomer,
            onUseCustomerName = viewModel::useQuickCustomerName,
            onSelectPayment = viewModel::selectPaymentMethod,
            onAmountPaidChanged = viewModel::onAmountPaidChanged,
            onToggleSplitPayment = viewModel::toggleSplitPayment,
            onSplitDpModeChanged = viewModel::setSplitDpMode,
            onAddSplitLine = viewModel::addSplitPaymentLine,
            onRemoveSplitLine = viewModel::removeSplitPaymentLine,
            onSplitLineMethodChanged = viewModel::updateSplitPaymentLineMethod,
            onSplitLineAmountChanged = viewModel::updateSplitPaymentLineAmount,
            onSplitLineReferenceChanged = viewModel::updateSplitPaymentLineReference,
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
            onDeliveryFleetChanged = viewModel::updateDeliveryFleet,
            onDeliveryRecipientPhoneChanged = viewModel::updateDeliveryRecipientPhone,
            onDeliveryVehiclePlateChanged = viewModel::updateDeliveryVehiclePlate,
            onDeliverySpecialInstructionsChanged = viewModel::updateDeliverySpecialInstructions,
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
            onUnlock = { viewModel.unlockScreen() },
            isLoading = uiState.isUnlockingScreen,
            isLockedOut = uiState.lockScreenRemainingSeconds > 0L,
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
