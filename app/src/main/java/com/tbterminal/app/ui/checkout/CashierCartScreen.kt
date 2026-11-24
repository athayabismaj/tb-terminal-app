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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    onItemNoteChanged: (String, String?) -> Unit = { _, _ -> },
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
    onLockScreen: (() -> Unit)? = null,
    onOpenSaveQuotation: () -> Unit = {},
    onOpenQuotations: () -> Unit = {},
) {
    var discountItemId by remember { mutableStateOf<String?>(null) }
    var noteItemId by remember { mutableStateOf<String?>(null) }
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
        showHeader = false,
        showBottomBar = false
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
                    onEditItemNote = { noteItemId = it },
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
                    onOpenSaveQuotation = onOpenSaveQuotation,
                    onOpenQuotations = onOpenQuotations,
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
    noteItemId?.let { cartItemId ->
        val item = state.cartItems.firstOrNull { it.cartItemId == cartItemId }
        if (item != null) {
            var noteDraft by remember(cartItemId, item.note) { mutableStateOf(item.note.orEmpty()) }
            ModalBottomSheet(
                onDismissRequest = { noteItemId = null },
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
                                text = "Catatan Item",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartOnSurface
                            )
                            Text(
                                text = "Tambahkan instruksi atau keterangan khusus produk",
                                fontSize = 12.sp,
                                color = CartOnSurfaceVariant
                            )
                        }

                        IconButton(onClick = { noteItemId = null }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                tint = CartOnSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = CartOutlineVariant, thickness = 1.dp)

                    // Info Produk Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, CartOutlineVariant),
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
                                color = Color(0xFFE8F3EC),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = null,
                                        tint = CartPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.productName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CartOnSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val qtyText = item.quantity.toString()
                                val uName = item.unitName.ifBlank { "pcs" }
                                Text(
                                    text = "$qtyText $uName • ${formatRupiah(item.unitPrice)}",
                                    fontSize = 12.sp,
                                    color = CartOnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Input Catatan Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Instruksi / Catatan Khusus",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CartOnSurface
                        )
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it.take(300) },
                            placeholder = {
                                Text(
                                    text = "Contoh: Potong 3 meter, warna putih tulang, titip gudang...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = null,
                                    tint = CartOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CartPrimary,
                                unfocusedBorderColor = CartOutlineVariant,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = CartPrimary,
                                focusedTextColor = CartOnSurface,
                                unfocusedTextColor = CartOnSurface,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Button(
                        onClick = {
                            onItemNoteChanged(cartItemId, noteDraft.trim().takeIf { it.isNotEmpty() })
                            noteItemId = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CartPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIMPAN CATATAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (!item.note.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                onItemNoteChanged(cartItemId, null)
                                noteItemId = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFEF2F2),
                                contentColor = CartError
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CartError
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hapus Catatan",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CartError
                            )
                        }
                    }

                    TextButton(
                        onClick = { noteItemId = null },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = CartOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    if (showTransactionDiscount) {
        val afterItemDiscount = state.subtotal.subtract(state.itemDiscountTotal)
        DiscountInputDialog(
            title = "Diskon Nota",
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
    onEditItemNote: (String) -> Unit = {},
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
    onOpenSaveQuotation: () -> Unit = {},
    onOpenQuotations: () -> Unit = {},
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
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    CartTopBar(
                        totalItems = totalItems,
                        onBackToPos = onBackToPos,
                        onOpenCameraScanner = onOpenCameraScanner,
                        onClearCart = onClearCart,
                        onOpenSaveQuotation = onOpenSaveQuotation,
                        onOpenQuotations = onOpenQuotations,
                    )
                    state.activeQuotationNumber?.let { rabNumber ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Konversi dari Penawaran #$rabNumber",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

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
                                onNoteClick = { onEditItemNote(item.cartItemId) },
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
            // ── TAMPILAN SMARTPHONE: SATU LEMBAR STRUK TERPADU (FASE 3) ──
            var showCustomerPickerSheet by remember { mutableStateOf(false) }

            if (showCustomerPickerSheet) {
                CustomerModalBottomSheet(
                    customerRepository = null,
                    initialCustomers = state.customers,
                    currentSelectedCustomer = state.selectedCustomer,
                    onCustomerSelected = { customer ->
                        if (customer != null) {
                            onSelectCustomer(customer)
                        } else {
                            onClearCustomer()
                        }
                        showCustomerPickerSheet = false
                    },
                    onAddNewCustomerClick = {
                        showCustomerPickerSheet = false
                        onOpenQuickAddCustomer()
                    },
                    onDismiss = { showCustomerPickerSheet = false }
                )
            }

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
                            onOpenSaveQuotation = onOpenSaveQuotation,
                            onOpenQuotations = onOpenQuotations,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        state.activeQuotationNumber?.let { rabNumber ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Konversi dari Penawaran #$rabNumber",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.8f))
                    }
                }

                // ── COMPACT PILLS ROW: Pelanggan & Diskon ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeCustomer = state.selectedCustomer
                    Surface(
                        onClick = { showCustomerPickerSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeCustomer != null) Color(0xFFF0FDF4) else Color.White,
                        border = BorderStroke(1.dp, if (activeCustomer != null) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = if (activeCustomer != null) Color(0xFF10533C) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = activeCustomer?.name ?: "Pelanggan Umum",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeCustomer != null) FontWeight.Bold else FontWeight.Medium,
                                    color = if (activeCustomer != null) Color(0xFF0F172A) else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (activeCustomer != null) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus pelanggan",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onClearCustomer() }
                                )
                            }
                        }
                    }

                    val hasTrxDiscount = state.transactionDiscountAmount > BigDecimal.ZERO
                    Surface(
                        onClick = onEditTransactionDiscount,
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasTrxDiscount) Color(0xFFFEF3C7) else Color.White,
                        border = BorderStroke(1.dp, if (hasTrxDiscount) Color(0xFFFCD34D) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = if (hasTrxDiscount) Color(0xFFB45309) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (hasTrxDiscount) "- ${formatRupiah(state.transactionDiscountAmount)}" else "+ Diskon Nota",
                                fontSize = 12.sp,
                                fontWeight = if (hasTrxDiscount) FontWeight.Bold else FontWeight.Medium,
                                color = if (hasTrxDiscount) Color(0xFF92400E) else Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ── SATU LEMBAR STRUK TERPADU (Unified Receipt Sheet) ──
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header Struk
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color(0xFF10533C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "STRUK PESANAN",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        text = "$totalItems Item",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                if (state.cartItems.isEmpty()) {
                                    // Empty state inside the receipt sheet
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFFF1F5F9), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ShoppingCart,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Text(
                                            text = "Keranjang Belanja Kosong",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Pilih produk di POS untuk menambahkan ke struk belanja.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center
                                        )
                                        OutlinedButton(
                                            onClick = onBackToPos,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF10533C)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10533C)),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("Pilih Produk di POS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else {
                                    // List of Items with hairline dividers
                                    state.cartItems.forEachIndexed { index, item ->
                                        if (index > 0) {
                                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                        }
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
                                            onNoteClick = { onEditItemNote(item.cartItemId) },
                                            onToggleUnit = { toSecondary -> onToggleUnit(item.cartItemId, toSecondary) },
                                            isReceiptMode = true
                                        )
                                    }

                                    // Quick Add SKU / Custom Manual Charge row
                                    Surface(
                                        onClick = onBackToPos,
                                        color = Color(0xFFFAFAFA),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = null,
                                                tint = Color(0xFF10533C),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "+ Tambah Item Lain / Biaya Manual",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }

                                    // Hairline separator before breakdown
                                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                                    // Order Breakdown inside Receipt Sheet
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Subtotal
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Subtotal ($totalItems item)", fontSize = 12.5.sp, color = Color(0xFF475569))
                                            Text(formatRupiah(state.subtotal), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                        }

                                        // Diskon Item (if any)
                                        if (state.itemDiscountTotal > BigDecimal.ZERO) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Diskon Item", fontSize = 12.5.sp, color = Color(0xFFB45309))
                                                Text("- ${formatRupiah(state.itemDiscountTotal)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                                            }
                                        }

                                        // Diskon Transaksi (if any)
                                        if (state.transactionDiscountAmount > BigDecimal.ZERO) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Diskon Transaksi", fontSize = 12.5.sp, color = Color(0xFFB45309))
                                                Text("- ${formatRupiah(state.transactionDiscountAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                                            }
                                        }

                                        // Ongkos Kirim Armada (if delivery enabled)
                                        if (state.isDeliveryEnabled) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Ongkos Kirim Armada", fontSize = 12.5.sp, color = Color(0xFF475569))
                                                Text("+ ${formatRupiah(state.deliveryFee)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                            }
                                        }

                                        // Pajak PPN (if tax applied)
                                        if (state.taxEnabled) {
                                            val rateStr = state.taxRate.stripTrailingZeros().toPlainString()
                                            val isInclude = state.taxMode.equals("INCLUDE", ignoreCase = true)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${state.taxLabel} ($rateStr%${if (isInclude) " • Incl." else ""})", fontSize = 12.5.sp, color = Color(0xFF475569))
                                                Text(if (isInclude) "(${formatRupiah(state.taxAmount)})" else "+ ${formatRupiah(state.taxAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                            }
                                        }

                                        // Pembulatan Tunai (if rounding applied)
                                        if (state.roundingAmount.compareTo(BigDecimal.ZERO) != 0) {
                                            val sign = if (state.roundingAmount > BigDecimal.ZERO) "+" else "-"
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Pembulatan Tunai", fontSize = 12.5.sp, color = Color(0xFF475569))
                                                Text("$sign ${formatRupiah(state.roundingAmount.abs())}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (state.roundingAmount > BigDecimal.ZERO) Color(0xFF1E293B) else Color(0xFF047857))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── STICKY FOOTER: Total Tebal + Tombol Lanjut Bayar ──
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Row
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
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "$totalItems item pesanan",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = formatRupiah(state.finalTotal),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10533C),
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Buttons Row: Parkir + Lanjut Bayar
                        val showParkirButton = state.cartItems.isNotEmpty() || state.heldCarts.isNotEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showParkirButton) {
                                OutlinedButton(
                                    onClick = onOpenParkir,
                                    modifier = Modifier
                                        .weight(0.38f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
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
                            }

                            Button(
                                onClick = onNavigateToPayment,
                                enabled = state.cartItems.isNotEmpty(),
                                modifier = Modifier
                                    .then(if (showParkirButton) Modifier.weight(0.82f) else Modifier.fillMaxWidth())
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10533C),
                                    disabledContainerColor = Color(0xFFE2E8F0),
                                    disabledContentColor = Color(0xFF94A3B8)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Lanjut Bayar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
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
    onOpenSaveQuotation: () -> Unit = {},
    onOpenQuotations: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sisi Kiri: Tombol Back + Judul "Keranjang" + (N item)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBackToPos,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali ke POS",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Keranjang",
                    fontSize = 17.sp,
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

        // Sisi Kanan: 4 Kotak Action Icons (Simpan RAB, Daftar RAB, Scan Barcode, Kosongkan Keranjang)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Simpan RAB (Floppy disk icon, Amber)
            Surface(
                onClick = onOpenSaveQuotation,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "Simpan RAB",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Daftar RAB (Document icon, Slate)
            Surface(
                onClick = onOpenQuotations,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Daftar RAB",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Scan Barcode (QrCodeScanner icon, Slate)
            Surface(
                onClick = onOpenCameraScanner,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 4. Kosongkan Keranjang (Delete / Trash icon, Rose)
            Surface(
                onClick = onClearCart,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
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
                        "Diskon Nota",
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

                if (state.taxEnabled) {
                    val rateStr = state.taxRate.stripTrailingZeros().toPlainString()
                    val isInclude = state.taxMode.equals("INCLUDE", ignoreCase = true)
                    CartCalculationRow(
                        label = "${state.taxLabel} ($rateStr%${if (isInclude) " • Incl." else ""})",
                        value = if (isInclude) "(${formatRupiah(state.taxAmount)})" else "+${formatRupiah(state.taxAmount)}"
                    )
                }

                if (state.roundingAmount.compareTo(BigDecimal.ZERO) != 0) {
                    val sign = if (state.roundingAmount > BigDecimal.ZERO) "+" else "-"
                    CartCalculationRow(
                        label = "Pembulatan Tunai",
                        value = "$sign${formatRupiah(state.roundingAmount.abs())}",
                        isDiscount = state.roundingAmount < BigDecimal.ZERO
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
            if (state.cartItems.isNotEmpty() || state.heldCarts.isNotEmpty()) {
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
                    Text(if (state.heldCarts.isNotEmpty()) "Parkir (${state.heldCarts.size})" else "Parkir", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
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
