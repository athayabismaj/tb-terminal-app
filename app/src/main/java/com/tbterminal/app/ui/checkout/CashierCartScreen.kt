package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.ui.barcode.CameraBarcodeScannerDialog
import com.tbterminal.app.ui.barcode.hardwareBarcodeScanner
import com.tbterminal.app.ui.common.UiText
import com.tbterminal.app.ui.checkout.components.CartCalculationRow
import com.tbterminal.app.ui.checkout.components.CartEmptyState
import com.tbterminal.app.ui.checkout.components.CartItemCard
import com.tbterminal.app.ui.checkout.components.DeliverySectionCard
import com.tbterminal.app.ui.checkout.components.CustomerCreditInspectorCard
import com.tbterminal.app.ui.checkout.components.PaymentMethodSelector
import com.tbterminal.app.ui.checkout.components.QuickAddCustomerSheet
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS DESIGN TOKENS (TB Terminal Design System)
// ═════════════════════════════════════════════════════════════════════════════
private val CartBackground = Color(0xFFF8FAFC)
private val CartSurface = Color.White
private val CartSurfaceContainerLow = Color(0xFFF0FDF4)
private val CartOnSurface = Color(0xFF0F172A)
private val CartOnSurfaceVariant = Color(0xFF64748B)
private val CartOutlineVariant = Color(0xFFE2E8F0)
private val CartOutline = Color(0xFF94A3B8)

private val CartPrimary = Color(0xFF10533C)
private val CartOnPrimary = Color(0xFFFFFFFF)
private val CartSecondary = Color(0xFFB45309)
private val CartSecondaryContainer = Color(0xFFFEF3C7)

private val CartError = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierCartScreen(
    name: String,
    role: String,
    state: CheckoutUiState,
    snackbarHostState: SnackbarHostState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onClearCart: () -> Unit,
    onItemDiscountChanged: (String, CheckoutDiscount?) -> Unit,
    onTransactionDiscountChanged: (CheckoutDiscount?) -> Unit,
    onToggleUnit: (String, Boolean) -> Unit = { _, _ -> },
    onCheckout: () -> Unit,
    onBackToPos: () -> Unit,
    onNavigateToPayment: () -> Unit = onCheckout,
    onOpenHoldCartDialog: () -> Unit = {},
    onOpenHeldCarts: () -> Unit = {},
    onToggleDelivery: (Boolean) -> Unit = {},
    onDeliveryFeeChanged: (String) -> Unit = {},
    onDeliveryAddressChanged: (String) -> Unit = {},
    onDeliveryCourierChanged: (String) -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onScanBarcode: (String) -> Unit = {},
    onOpenQrisDialog: () -> Unit = {},
    onOpenPettyCashDialog: () -> Unit = {},
    onOpenQuickAddCustomer: () -> Unit = {},
    onDismissQuickAddCustomer: () -> Unit = {},
    onQuickCreateCustomer: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onLockScreen: (() -> Unit)? = null
) {
    var discountItemId by remember { mutableStateOf<String?>(null) }
    var showTransactionDiscount by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showClearCartDialog by remember { mutableStateOf(false) }
    var showParkirChooserDialog by remember { mutableStateOf(false) }

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
        onLogout = onLogout,
        onLockScreen = onLockScreen,
        showHeader = false
    ) { contentModifier ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = CartBackground,
            modifier = contentModifier.hardwareBarcodeScanner { barcode -> onScanBarcode(barcode) },
            contentWindowInsets = WindowInsets(0.dp)
        ) { contentPadding ->
            if (state.cartItems.isEmpty()) {
                CartEmptyState(
                    onBackToPos = onBackToPos,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            } else {
                CartContent(
                    state = state,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                    onRemove = onRemove,
                    onCustomerSearchChanged = onCustomerSearchChanged,
                    onSelectCustomer = onSelectCustomer,
                    onClearCustomer = onClearCustomer,
                    onUseCustomerName = onUseCustomerName,
                    onSelectPayment = onSelectPayment,
                    onAmountPaidChanged = onAmountPaidChanged,
                    onClearCart = { showClearCartDialog = true },
                    onEditItemDiscount = { discountItemId = it },
                    onRemoveItemDiscount = { cartItemId -> onItemDiscountChanged(cartItemId, null) },
                    onEditTransactionDiscount = { showTransactionDiscount = true },
                    onToggleUnit = onToggleUnit,
                    onCheckout = onCheckout,
                    onBackToPos = onBackToPos,
                    onNavigateToPayment = onNavigateToPayment,
                    onOpenHoldCartDialog = onOpenHoldCartDialog,
                    onOpenHeldCarts = onOpenHeldCarts,
                    onOpenParkir = {
                        if (state.heldCarts.isNotEmpty()) {
                            showParkirChooserDialog = true
                        } else {
                            onOpenHoldCartDialog()
                        }
                    },
                    onToggleDelivery = onToggleDelivery,
                    onDeliveryFeeChanged = onDeliveryFeeChanged,
                    onDeliveryAddressChanged = onDeliveryAddressChanged,
                    onDeliveryCourierChanged = onDeliveryCourierChanged,
                    onOpenCameraScanner = { showCameraScanner = true },
                    onOpenQrisDialog = onOpenQrisDialog,
                    onOpenQuickAddCustomer = onOpenQuickAddCustomer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            }
        }
    }

    if (state.isQuickAddCustomerOpen) {
        QuickAddCustomerSheet(
            initialName = state.customerSearchQuery,
            isSubmitting = state.isQuickCustomerSubmitting,
            errorMessage = state.customerError?.toMessage(),
            onDismiss = onDismissQuickAddCustomer,
            onSubmit = onQuickCreateCustomer
        )
    }

    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            onDismissRequest = { showCameraScanner = false },
            onBarcodeScanned = { barcode ->
                showCameraScanner = false
                onScanBarcode(barcode)
            }
        )
    }

    discountItemId?.let { cartItemId ->
        val item = state.cartItems.firstOrNull { it.cartItemId == cartItemId }
        if (item != null) {
            DiscountInputDialog(
                title = "Diskon ${item.productName}",
                baseAmount = item.unitPrice.multiply(item.quantity.toBigDecimal()),
                initialValue = item.discountRequest,
                onDismiss = { discountItemId = null },
                onApply = { discount ->
                    onItemDiscountChanged(cartItemId, discount)
                    discountItemId = null
                },
            )
        }
    }
    if (showTransactionDiscount) {
        val afterItemDiscount = state.subtotal.subtract(state.itemDiscountTotal)
        DiscountInputDialog(
            title = "Diskon transaksi",
            baseAmount = afterItemDiscount,
            initialValue = state.transactionDiscount,
            onDismiss = { showTransactionDiscount = false },
            onApply = { discount ->
                onTransactionDiscountChanged(discount)
                showTransactionDiscount = false
            },
        )
    }

    if (showClearCartDialog) {
        val totalItems = state.cartItems.sumOf { it.quantity }
        AlertDialog(
            onDismissRequest = { showClearCartDialog = false },
            title = {
                Text(
                    text = "Kosongkan Keranjang?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CartOnSurface
                )
            },
            text = {
                Text(
                    text = "Semua $totalItems barang belanjaan dalam keranjang ini akan dihapus.",
                    fontSize = 13.5.sp,
                    color = CartOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCartDialog = false
                        onClearCart()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CartError,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Kosongkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearCartDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CartOutlineVariant)
                ) {
                    Text("Batal", color = CartOnSurface, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showParkirChooserDialog) {
        AlertDialog(
            onDismissRequest = { showParkirChooserDialog = false },
            title = {
                Text(
                    text = "Opsi Parkir Transaksi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CartOnSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Terdapat ${state.heldCarts.size} transaksi yang sedang diparkir:",
                        fontSize = 13.sp,
                        color = CartOnSurfaceVariant
                    )
                    Surface(
                        onClick = {
                            showParkirChooserDialog = false
                            onOpenHoldCartDialog()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PauseCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Parkir / Tunda Pesanan Ini",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            showParkirChooserDialog = false
                            onOpenHeldCarts()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = CartPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Buka Daftar Parkir (${state.heldCarts.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showParkirChooserDialog = false }) {
                    Text("Tutup", color = CartOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// RESPONSIVE CART CONTENT (ADAPTIVE MOBILE / TABLET SPLIT)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CartContent(
    state: CheckoutUiState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onClearCart: () -> Unit,
    onEditItemDiscount: (String) -> Unit,
    onRemoveItemDiscount: (String) -> Unit,
    onEditTransactionDiscount: () -> Unit,
    onToggleUnit: (String, Boolean) -> Unit,
    onCheckout: () -> Unit,
    onBackToPos: () -> Unit,
    onNavigateToPayment: () -> Unit = onCheckout,
    onOpenHoldCartDialog: () -> Unit,
    onOpenHeldCarts: () -> Unit,
    onOpenParkir: () -> Unit,
    onToggleDelivery: (Boolean) -> Unit,
    onDeliveryFeeChanged: (String) -> Unit,
    onDeliveryAddressChanged: (String) -> Unit,
    onDeliveryCourierChanged: (String) -> Unit,
    onOpenCameraScanner: () -> Unit,
    onOpenQrisDialog: () -> Unit,
    onOpenQuickAddCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalItems = state.cartItems.sumOf { it.quantity }
    val paymentIsValid = validateCheckoutPaymentInput(
        paymentMethod = state.selectedPaymentMethod,
        amountPaidInput = state.amountPaidInput,
        total = state.finalTotal
    ).error == null
    val creditCustomerIsValid = state.selectedPaymentMethod !in setOf(PaymentMethod.HUTANG, PaymentMethod.DP) ||
            state.selectedCustomer != null

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 768.dp

        if (isTablet) {
            // ── TAMPILAN TABLET: 2-KOLOM SPLIT REGISTER ──
            Row(modifier = Modifier.fillMaxSize()) {
                // Sisi Kiri: Daftar Item Keranjang
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(20.dp)
                ) {
                    CartTopBar(
                        totalItems = totalItems,
                        onBackToPos = onBackToPos,
                        onOpenCameraScanner = onOpenCameraScanner,
                        onClearCart = onClearCart
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.cartItems, key = { it.cartItemId }) { item ->
                            val product = state.products.firstOrNull { it.productId == item.productId }
                            CartItemCard(
                                item = item,
                                sku = item.sku.ifBlank { product?.sku ?: "" },
                                unitName = item.unitName.ifBlank { product?.unitName ?: "" },
                                onIncrease = { onIncrease(item.cartItemId) },
                                onDecrease = { onDecrease(item.cartItemId) },
                                onRemove = { onRemove(item.cartItemId) },
                                onDiscountClick = { onEditItemDiscount(item.cartItemId) },
                                onRemoveDiscount = { onRemoveItemDiscount(item.cartItemId) },
                                onToggleUnit = { toSecondary -> onToggleUnit(item.cartItemId, toSecondary) },
                            )
                        }
                    }
                }

                // Sisi Kanan: Ringkasan Pesanan & Pembayaran
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    color = Color.White,
                    border = BorderStroke(1.dp, CartOutlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CartSummaryPanel(
                            state = state,
                            paymentIsValid = paymentIsValid,
                            creditCustomerIsValid = creditCustomerIsValid,
                            onCustomerSearchChanged = onCustomerSearchChanged,
                            onSelectCustomer = onSelectCustomer,
                            onClearCustomer = onClearCustomer,
                            onUseCustomerName = onUseCustomerName,
                            onOpenQuickAddCustomer = onOpenQuickAddCustomer,
                            onToggleDelivery = onToggleDelivery,
                            onDeliveryFeeChanged = onDeliveryFeeChanged,
                            onDeliveryAddressChanged = onDeliveryAddressChanged,
                            onDeliveryCourierChanged = onDeliveryCourierChanged,
                            onSelectPayment = onSelectPayment,
                            onAmountPaidChanged = onAmountPaidChanged,
                            onOpenQrisDialog = onOpenQrisDialog,
                            onEditTransactionDiscount = onEditTransactionDiscount,
                            onClearCart = onClearCart,
                            onOpenHoldCartDialog = onOpenHoldCartDialog,
                            onCheckout = onCheckout
                        )
                    }
                }
            }
        } else {
            // ── TAMPILAN SMARTPHONE: PERSIS SESUAI SPESIFIKASI HTML MOCKUP ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
                // Header (TopBar)
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        CartTopBar(
                            totalItems = totalItems,
                            onBackToPos = onBackToPos,
                            onOpenCameraScanner = onOpenCameraScanner,
                            onClearCart = onClearCart,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.8f))
                    }
                }

                // Scrollable Cart Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.cartItems, key = { it.cartItemId }) { item ->
                        val product = state.products.firstOrNull { it.productId == item.productId }
                        CartItemCard(
                            item = item,
                            sku = item.sku.ifBlank { product?.sku ?: "" },
                            unitName = item.unitName.ifBlank { product?.unitName ?: "" },
                            onIncrease = { onIncrease(item.cartItemId) },
                            onDecrease = { onDecrease(item.cartItemId) },
                            onRemove = { onRemove(item.cartItemId) },
                            onDiscountClick = { onEditItemDiscount(item.cartItemId) },
                            onRemoveDiscount = { onRemoveItemDiscount(item.cartItemId) },
                            onToggleUnit = { toSecondary -> onToggleUnit(item.cartItemId, toSecondary) },
                        )
                    }

                    // Quick Add SKU / Custom Manual Charge button
                    item {
                        Surface(
                            onClick = onBackToPos,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF10533C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tambah Biaya Lain / Custom Item",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // CheckoutSummaryPanel (Bottom Sheet matching HTML specification)
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Drag handle pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                        )

                        // Breakdown lines (Subtotal, Diskon Item, Diskon Transaksi, Ongkir, Pajak)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Subtotal (N item)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subtotal ($totalItems item)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = formatRupiah(state.subtotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            // Diskon Item (if any)
                            if (state.itemDiscountTotal > BigDecimal.ZERO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalOffer,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Diskon Item",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                    Text(
                                        text = "- ${formatRupiah(state.itemDiscountTotal)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }

                            // Diskon Transaksi (clickable)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.clickable { onEditTransactionDiscount() },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF10533C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (state.transactionDiscountAmount > BigDecimal.ZERO) "Diskon Transaksi" else "Tambah Diskon Transaksi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10533C)
                                    )
                                }
                                Text(
                                    text = if (state.transactionDiscountAmount > BigDecimal.ZERO) "- ${formatRupiah(state.transactionDiscountAmount)}" else "- Rp 0",
                                    fontSize = 12.sp,
                                    fontWeight = if (state.transactionDiscountAmount > BigDecimal.ZERO) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (state.transactionDiscountAmount > BigDecimal.ZERO) Color(0xFFB45309) else Color(0xFF94A3B8)
                                )
                            }

                            // Ongkir (if delivery enabled)
                            if (state.isDeliveryEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ongkos Kirim Armada",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "+ ${formatRupiah(state.deliveryFee)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            // Pajak Restitusi / Non-PKP
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pajak Restitusi / Non-PKP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Rp 0",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        // Dashed divider
                        HorizontalDivider(
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp
                        )

                        // TOTAL TAGIHAN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL TAGIHAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Termasuk semua item & diskon",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = formatRupiah(state.finalTotal),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10533C),
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Buttons Row: Parkir (col-span-4) & Lanjut Pembayaran (col-span-8)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Parkir Button (approx 35-40% width)
                            OutlinedButton(
                                onClick = onOpenParkir,
                                modifier = Modifier
                                    .weight(0.42f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1E293B)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PauseCircleOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Parkir",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    if (state.heldCarts.isNotEmpty()) {
                                        Text(
                                            text = "(${state.heldCarts.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            // Lanjut Pembayaran Button (dominant)
                            Button(
                                onClick = onNavigateToPayment,
                                enabled = state.cartItems.isNotEmpty(),
                                modifier = Modifier
                                    .weight(0.78f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10533C),
                                    disabledContainerColor = Color(0xFFE2E8F0),
                                    disabledContentColor = Color(0xFF94A3B8)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Lanjut Pembayaran",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Home Indicator Pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 2.dp)
                                .size(width = 128.dp, height = 4.dp)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN: TOP BAR KERANJANG
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CartTopBar(
    totalItems: Int,
    onBackToPos: () -> Unit,
    onOpenCameraScanner: () -> Unit,
    onClearCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sisi Kiri: Tombol Back Bulat + Judul "Keranjang" + (N item)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBackToPos,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali ke POS",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Keranjang",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp
                )

                Text(
                    text = "($totalItems item)",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Sisi Kanan: Barcode Scanner & Kosongkan Keranjang (Trash)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onOpenCameraScanner,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onClearCart,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Kosongkan Keranjang",
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN: SUMMARY & CHECKOUT PANEL
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CartSummaryPanel(
    state: CheckoutUiState,
    paymentIsValid: Boolean,
    creditCustomerIsValid: Boolean,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onOpenQuickAddCustomer: () -> Unit,
    onToggleDelivery: (Boolean) -> Unit,
    onDeliveryFeeChanged: (String) -> Unit,
    onDeliveryAddressChanged: (String) -> Unit,
    onDeliveryCourierChanged: (String) -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onOpenQrisDialog: () -> Unit,
    onEditTransactionDiscount: () -> Unit,
    onClearCart: () -> Unit,
    onOpenHoldCartDialog: () -> Unit,
    onCheckout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Pemilih Pelanggan
        CartCustomerSelector(
            customerSearchQuery = state.customerSearchQuery,
            selectedCustomer = state.selectedCustomer,
            customers = state.customers,
            isLoading = state.isCustomerLoading,
            onSearchChanged = onCustomerSearchChanged,
            onSelectCustomer = onSelectCustomer,
            onClearCustomer = onClearCustomer,
            onUseCustomerName = onUseCustomerName,
            error = state.customerError,
            onOpenQuickAddCustomer = onOpenQuickAddCustomer
        )

        if (state.selectedCustomer != null) {
            CustomerCreditInspectorCard(
                customer = state.selectedCustomer,
                currentBillTotal = state.finalTotal
            )
        }

        // 2. Pengiriman Toko
        DeliverySectionCard(
            isDeliveryEnabled = state.isDeliveryEnabled,
            deliveryFeeInput = state.deliveryFeeInput,
            deliveryAddress = state.deliveryAddress,
            deliveryCourier = state.deliveryCourier,
            onToggleDelivery = onToggleDelivery,
            onDeliveryFeeChanged = onDeliveryFeeChanged,
            onDeliveryAddressChanged = onDeliveryAddressChanged,
            onDeliveryCourierChanged = onDeliveryCourierChanged,
        )

        // 3. Metode Pembayaran
        PaymentMethodSelector(
            selectedMethod = state.selectedPaymentMethod,
            onSelectPayment = onSelectPayment
        )

        // 4. Input Uang Tunai / DP
        if (state.selectedPaymentMethod == PaymentMethod.DP || state.selectedPaymentMethod == PaymentMethod.TUNAI) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.amountPaidInput,
                    onValueChange = onAmountPaidChanged,
                    label = {
                        Text(if (state.selectedPaymentMethod == PaymentMethod.TUNAI) "Uang Tunai Diterima" else "Nominal DP Proyek")
                    },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Text(
                            "Rp",
                            fontWeight = FontWeight.Bold,
                            color = CartPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = CartOutlineVariant,
                        focusedBorderColor = CartPrimary
                    ),
                    singleLine = true
                )

                // Quick Cash Presets untuk Tunai
                if (state.selectedPaymentMethod == PaymentMethod.TUNAI && state.finalTotal > BigDecimal.ZERO) {
                    val presets = getCashQuickPresets(state.finalTotal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                onAmountPaidChanged(state.finalTotal.stripTrailingZeros().toPlainString())
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Uang Pas",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF004528)
                                )
                            }
                        }

                        presets.forEach { preset ->
                            Surface(
                                onClick = {
                                    onAmountPaidChanged(preset.stripTrailingZeros().toPlainString())
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatRupiah(preset).replace("Rp ", ""),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Kembalian / Kurang Bayar untuk Tunai
                if (state.selectedPaymentMethod == PaymentMethod.TUNAI && state.amountPaidInput.isNotBlank()) {
                    val parsedAmount = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                    if (parsedAmount != null) {
                        val diff = parsedAmount.subtract(state.finalTotal)
                        if (diff >= BigDecimal.ZERO) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF004528),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Kembalian",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF004528)
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(diff),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF004528)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Kurang Bayar",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(diff.abs()),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                // DP Sisa Tagihan Info
                if (state.selectedPaymentMethod == PaymentMethod.DP && state.amountPaidInput.isNotBlank()) {
                    val parsedDp = state.amountPaidInput.trim().replace(',', '.').toBigDecimalOrNull()
                    if (parsedDp != null && parsedDp > BigDecimal.ZERO && parsedDp < state.finalTotal) {
                        val sisaHutang = state.finalTotal.subtract(parsedDp)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Sisa Tagihan Tempo",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = formatRupiah(sisaHutang),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. QRIS Shortcut Banner
        if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
            Surface(
                onClick = onOpenQrisDialog,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tampilkan Kode QRIS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "Tampilkan QR di layar tablet untuk discan pembeli",
                            fontSize = 11.sp,
                            color = Color(0xFF047857)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 6. Ringkasan Kalkulasi Nota
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CartOutlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "RINCIAN PERHITUNGAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartOutline,
                    letterSpacing = 0.8.sp
                )

                CartCalculationRow("Subtotal Produk", formatRupiah(state.subtotal))

                if (state.itemDiscountTotal > BigDecimal.ZERO) {
                    CartCalculationRow(
                        "Diskon Item",
                        "-${formatRupiah(state.itemDiscountTotal)}",
                        isDiscount = true
                    )
                }

                if (state.transactionDiscountAmount > BigDecimal.ZERO) {
                    CartCalculationRow(
                        "Diskon Transaksi",
                        "-${formatRupiah(state.transactionDiscountAmount)}",
                        isDiscount = true
                    )
                }

                if (state.isDeliveryEnabled && state.deliveryFee > BigDecimal.ZERO) {
                    CartCalculationRow(
                        "Ongkos Kirim Armada",
                        "+${formatRupiah(state.deliveryFee)}"
                    )
                }

                TextButton(
                    onClick = onEditTransactionDiscount,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (state.transactionDiscount == null) "+ Tambah Diskon Nota" else "Ubah Diskon Nota",
                        color = CartPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = CartOutlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Total Bayar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CartOnSurface
                    )
                    Text(
                        text = formatRupiah(state.finalTotal),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CartPrimary
                    )
                }
            }
        }

        // Status Peringatan Diskon / Ambiguous
        if (state.isPreviewLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = CartPrimary
            )
        } else if (state.checkoutPreview?.approvalRequired == true) {
            Text(
                text = "Diskon melebihi limit dan memerlukan persetujuan Owner/Admin.",
                style = MaterialTheme.typography.bodySmall,
                color = CartSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 7. Tombol Aksi Bawah (Tablet)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onOpenHoldCartDialog,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CartOutlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = CartOnSurface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    tint = CartSecondary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Parkir", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
                        onOpenQrisDialog()
                    } else {
                        onCheckout()
                    }
                },
                enabled = !state.isLoading && !state.isPreviewLoading && state.cartItems.isNotEmpty() && paymentIsValid && creditCustomerIsValid,
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF004528),
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                if (state.isLoading || state.isPreviewLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CartOnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (state.selectedPaymentMethod == PaymentMethod.QRIS) Icons.Outlined.QrCode2 else Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            state.hasAmbiguousCheckout -> "Periksa"
                            state.checkoutPreview?.approvalRequired == true -> "Persetujuan"
                            state.selectedPaymentMethod == PaymentMethod.QRIS -> "QRIS"
                            else -> "Proses Bayar"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN: PEMILIH PELANGGAN
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun CartCustomerSelector(
    customerSearchQuery: String,
    selectedCustomer: Customer?,
    customers: List<Customer>,
    isLoading: Boolean,
    onSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    error: UiText?,
    onOpenQuickAddCustomer: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val typedName = customerSearchQuery.trim()
    val hasQuickName = selectedCustomer == null && typedName.isNotBlank()
    val exactMatch = customers.any { customer ->
        customer.name.equals(typedName, ignoreCase = true)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            if (selectedCustomer != null || hasQuickName) CartPrimary
            else CartOutlineVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectedCustomer == null) {
                            isExpanded = !isExpanded
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (selectedCustomer != null || hasQuickName) CartSurfaceContainerLow
                            else CartSecondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedCustomer != null) Icons.Outlined.Person else Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = if (selectedCustomer != null || hasQuickName) CartPrimary else CartSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedCustomer?.name ?: typedName.ifBlank { "Pilih Pelanggan" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CartOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            selectedCustomer != null -> "Pelanggan Terdaftar"
                            hasQuickName -> "Dipakai di nota kasir"
                            else -> "Opsional untuk transaksi tunai"
                        },
                        fontSize = 11.sp,
                        color = CartOnSurfaceVariant
                    )
                }
                if (selectedCustomer != null || hasQuickName) {
                    IconButton(
                        onClick = {
                            onClearCustomer()
                            isExpanded = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus pelanggan",
                            tint = CartOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = CartOnSurfaceVariant
                    )
                }
            }

            // Dropdown Pencarian Pelanggan
            if (isExpanded && selectedCustomer == null) {
                HorizontalDivider(color = CartOutlineVariant)
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = {
                            Text("Cari nama pelanggan...", color = CartOutline, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = CartOutline,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = CartOutlineVariant,
                            focusedBorderColor = CartPrimary
                        ),
                        singleLine = true
                    )

                    error?.let {
                        Text(
                            text = it.toMessage(),
                            color = CartError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (typedName.isNotBlank() && !exactMatch) {
                        Button(
                            onClick = {
                                onUseCustomerName()
                                isExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CartPrimary)
                        ) {
                            Text(
                                text = "Pakai \"$typedName\" di nota",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onOpenQuickAddCustomer()
                            isExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CartPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CartPrimary)
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (typedName.isNotBlank() && !exactMatch) "Daftarkan \"$typedName\"" else "+ Tambah Pelanggan Baru",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CartPrimary
                            )
                        }
                    } else {
                        customers.take(3).forEach { customer ->
                            Surface(
                                onClick = {
                                    onSelectCustomer(customer)
                                    isExpanded = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(CartSurfaceContainerLow, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.take(1).uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CartPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CartOnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun formatRupiah(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp ${formatter.format(amount)}"
}

internal fun getCashQuickPresets(total: BigDecimal): List<BigDecimal> {
    if (total <= BigDecimal.ZERO) {
        return listOf(BigDecimal(50_000), BigDecimal(100_000))
    }
    val presets = linkedSetOf<BigDecimal>()
    val totalLong = total.toLong()

    val next10k = ((totalLong / 10_000) + 1) * 10_000
    if (next10k > totalLong && (next10k - totalLong) >= 1_000) {
        presets.add(BigDecimal(next10k))
    }

    val next50k = ((totalLong / 50_000) + 1) * 50_000
    if (next50k > totalLong) {
        presets.add(BigDecimal(next50k))
    }

    val next100k = ((totalLong / 100_000) + 1) * 100_000
    if (next100k > totalLong) {
        presets.add(BigDecimal(next100k))
    }

    if (totalLong < 50_000) {
        presets.add(BigDecimal(50_000))
        presets.add(BigDecimal(100_000))
    } else if (totalLong < 100_000) {
        presets.add(BigDecimal(100_000))
    }

    return presets.filter { it > total }.take(3)
}

private fun UiText.toMessage(): String {
    return when (this) {
        is UiText.DynamicString -> value
    }
}
