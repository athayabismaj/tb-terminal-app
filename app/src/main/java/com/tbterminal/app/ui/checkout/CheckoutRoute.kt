package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.common.UiText
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

    LaunchedEffect(viewModel.errorEvent, snackbarHostState) {
        viewModel.errorEvent
            .filterNotNull()
            .collect { error ->
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
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = SurfaceBright,
            contentWindowInsets = WindowInsets(0.dp),
            modifier = contentModifier
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
                onSelectPayment = viewModel::selectPaymentMethod,
                onAmountPaidChanged = viewModel::onAmountPaidChanged,
                onStartingCashChanged = viewModel::onStartingCashChanged,
                onOpenCashSession = viewModel::openCashSession,
                onRefreshCashSession = viewModel::loadCashSession,
                onCheckout = {
                    viewModel.submitCheckout(
                        paymentMethod = uiState.selectedPaymentMethod,
                        amountPaid = uiState.finalTotal
                    )
                },
                onNavigateToCart = onNavigateToCart,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
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
    onBackToPos: () -> Unit,
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

    LaunchedEffect(viewModel.errorEvent, snackbarHostState) {
        viewModel.errorEvent
            .filterNotNull()
            .collect { error ->
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
        onCheckout = {
            viewModel.submitCheckout(
                paymentMethod = uiState.selectedPaymentMethod,
                amountPaid = uiState.finalTotal
            )
        },
        onBackToPos = onBackToPos,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    )
}

private fun UiText.message(): String {
    return when (this) {
        is UiText.DynamicString -> value
    }
}
