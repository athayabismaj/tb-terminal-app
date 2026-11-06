package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.barcode.CameraBarcodeScannerDialog
import com.tbterminal.app.ui.barcode.hardwareBarcodeScanner
import com.tbterminal.app.ui.checkout.components.MaterialEstimatorDialog
import com.tbterminal.app.ui.checkout.components.ProductCardGrid
import com.tbterminal.app.ui.checkout.components.ProductErrorState
import com.tbterminal.app.ui.checkout.components.ProductPaginationBar
import com.tbterminal.app.ui.common.toRupiahText
import kotlinx.coroutines.delay
import java.math.BigDecimal

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS DESIGN TOKENS (TB Terminal Design System)
// ═════════════════════════════════════════════════════════════════════════════
val SurfaceBright = Color.White
val Primary = Color(0xFF10533C)
val PrimaryContainer = Color(0xFF0B3828)
val OnPrimary = Color.White

private val BrandForest = Color(0xFF10533C)
private val BrandForestDark = Color(0xFF0B3828)
private val BrandGreenLight = Color(0xFFF0FDF4)
private val BrandGreenBorder = Color(0xFFBBF7D0)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val Emerald700 = Color(0xFF15803D)
private val Emerald50 = Color(0xFFECFDF5)

private val Amber700 = Color(0xFFB45309)
private val AmberBg = Color(0xFFFEF3C7)
private val AmberBorder = Color(0xFFF59E0B)

private val DangerRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPosScreen(
    state: CheckoutUiState,
    onSearchChanged: (String) -> Unit,
    onRefreshProducts: () -> Unit,
    onProductPageChanged: (Int) -> Unit,
    onProductClick: (Product) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onRefreshCustomers: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onToggleUnit: (String, Boolean) -> Unit = { _, _ -> },
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onStartingCashChanged: (String) -> Unit,
    onOpenCashSession: () -> Unit,
    onRefreshCashSession: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToCart: () -> Unit,
    onScanBarcode: (String) -> Unit = {},
    onClearScanNotification: () -> Unit = {},
    onOpenHoldCartDialog: () -> Unit = {},
    onOpenHeldCarts: () -> Unit = {},
    onOpenPettyCashDialog: () -> Unit = {},
    onLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCameraScanner by remember { mutableStateOf(false) }
    var showMaterialEstimator by remember { mutableStateOf(false) }

    val allCategory = "Semua Kategori"
    val categories = remember(state.products) {
        listOf(allCategory) + state.products.map { it.categoryName }.filter { it.isNotBlank() }.distinct()
    }
    var selectedCategory by remember { mutableStateOf(allCategory) }
    LaunchedEffect(categories) {
        if (selectedCategory !in categories) {
            selectedCategory = allCategory
        }
    }

    val filteredProducts = remember(selectedCategory, state.products) {
        if (selectedCategory == allCategory) state.products
        else state.products.filter { it.categoryName == selectedCategory }
    }

    val cartItemCount = state.cartItems.sumOf { it.quantity }
    val cartSubtotal = state.cartItems.fold(BigDecimal.ZERO) { acc, item ->
        acc.add(item.unitPrice.multiply(item.quantity.toBigDecimal()))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .hardwareBarcodeScanner { barcode -> onScanBarcode(barcode) },
        containerColor = Slate50,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val compact = maxWidth < 600.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (compact) 14.dp else 24.dp)
                        .padding(top = 12.dp)
                ) {
                    // ── 1. OPERATIONAL REGISTER HEADER ──
                    PosOperationalHeader(
                        heldCartsCount = state.heldCarts.size,
                        cartItemCount = cartItemCount,
                        onOpenHeldCarts = onOpenHeldCarts,
                        onOpenHoldCartDialog = onOpenHoldCartDialog,
                        onOpenPettyCashDialog = onOpenPettyCashDialog,
                        onOpenMaterialEstimator = { showMaterialEstimator = true },
                        onLockScreen = onLockScreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── 2. SEARCH & SCANNER INPUT BAR ──
                    PosSearchBar(
                        query = state.searchQuery,
                        onQueryChanged = onSearchChanged,
                        onOpenCameraScanner = { showCameraScanner = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── 3. CATEGORY PILL CAROUSEL (ONE-TAP SPEED) ──
                    PosCategoryCarousel(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )

                    // ── 4. BARCODE SCAN NOTIFICATION TOAST PILL ──
                    state.scanNotification?.let { notification ->
                        LaunchedEffect(notification) {
                            delay(2500)
                            onClearScanNotification()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = BrandGreenLight,
                            border = BorderStroke(1.dp, BrandGreenBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = BrandForest,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = notification,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandForestDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── 5. PRODUCT CATALOG GRID ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when {
                            state.isProductLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = BrandForest,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            state.productError != null -> {
                                ProductErrorState(
                                    message = state.productError.toMessage(),
                                    onRetry = onRefreshProducts,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            state.products.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Katalog Produk Kosong",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Belum ada produk yang terdaftar di toko.",
                                            fontSize = 12.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }

                            else -> {
                                ProductCardGrid(
                                    products = filteredProducts,
                                    cartItems = state.cartItems,
                                    onProductClick = onProductClick,
                                    onUpdateQty = onUpdateQty,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // ── 6. PAGINATION BAR ──
                    ProductPaginationBar(
                        page = state.productPage,
                        totalPages = state.productTotalPages,
                        totalItems = state.productTotal,
                        limit = state.productLimit,
                        isLoading = state.isProductLoading,
                        onPrevious = { onProductPageChanged(state.productPage - 1) },
                        onNext = { onProductPageChanged(state.productPage + 1) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── 7. STICKY INDUSTRIAL BOTTOM CART DOCK (POS DOCK BAR) ──
            if (cartItemCount > 0) {
                PosBottomCartDock(
                    cartItemCount = cartItemCount,
                    cartSubtotal = cartSubtotal,
                    onNavigateToCart = onNavigateToCart,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }

    // Modal Scanner Kamera Barcode
    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            onDismissRequest = { showCameraScanner = false },
            onBarcodeScanned = { barcode ->
                showCameraScanner = false
                onScanBarcode(barcode)
            }
        )
    }

    // Modal Kalkulator Bahan Bangunan
    if (showMaterialEstimator) {
        MaterialEstimatorDialog(
            onDismiss = { showMaterialEstimator = false },
            onApplySearchQuery = { query -> onSearchChanged(query) }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN 1: OPERATIONAL POS REGISTER HEADER
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosOperationalHeader(
    heldCartsCount: Int,
    cartItemCount: Int,
    onOpenHeldCarts: () -> Unit,
    onOpenHoldCartDialog: () -> Unit,
    onOpenPettyCashDialog: () -> Unit,
    onOpenMaterialEstimator: () -> Unit,
    onLockScreen: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Register Identity Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Kasir POS",
                color = Slate900,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BrandGreenLight,
                border = BorderStroke(1.dp, BrandGreenBorder)
            ) {
                Text(
                    text = "POS-01",
                    color = BrandForest,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Right: Operational Shortcuts
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Badge Parkir Nota (Held Carts) jika ada yang ditahan
            if (heldCartsCount > 0) {
                Surface(
                    onClick = onOpenHeldCarts,
                    shape = RoundedCornerShape(10.dp),
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PauseCircleOutline,
                            contentDescription = null,
                            tint = Amber700,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Parkir ($heldCartsCount)",
                            color = Amber700,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Tombol Parkir Nota Baru saat kasir melayani transaksi yang perlu ditunda
            if (cartItemCount > 0) {
                OutlinedButton(
                    onClick = onOpenHoldCartDialog,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Amber700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber700),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Parkir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Shortcut 1: Kas Kecil
            IconButton(
                onClick = onOpenPettyCashDialog,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BrandGreenLight)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = "Kas Kecil",
                    tint = BrandForest,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Shortcut 2: Kalkulator Material
            IconButton(
                onClick = onOpenMaterialEstimator,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = "Kalkulator Bahan Bangunan",
                    tint = Slate700,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Shortcut 3: Kunci Kasir
            IconButton(
                onClick = onLockScreen,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Kunci Layar Kasir",
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN 2: SEARCH & BARCODE SCANNER BAR
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onOpenCameraScanner: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            Text(
                text = "Cari nama barang, SKU, atau scan barcode...",
                color = Slate400,
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = { onQueryChanged("") },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Hapus Pencarian",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenCameraScanner,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandGreenLight)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan Barcode Kamera",
                        tint = BrandForest,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Slate200,
            focusedBorderColor = BrandForest
        ),
        singleLine = true
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN 3: HORIZONTAL CATEGORY PILL CAROUSEL
// Fast, tactile category filtering like modern industrial POS systems
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosCategoryCarousel(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 1.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory

            Surface(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) BrandForest else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) BrandForest else Slate200
                ),
                shadowElevation = if (isSelected) 1.dp else 0.dp
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else Slate700,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN 4: STICKY INDUSTRIAL BOTTOM CART DOCK
// Anchored at bottom with live price, item count, and direct review CTA
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosBottomCartDock(
    cartItemCount: Int,
    cartSubtotal: BigDecimal,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToCart),
        shape = RoundedCornerShape(16.dp),
        color = BrandForestDark,
        border = BorderStroke(1.dp, BrandForest),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Cart icon + badge + subtotal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandForest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = "Keranjang",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .border(1.5.dp, BrandForestDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cartItemCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "$cartItemCount Barang Dipilih",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = cartSubtotal.toRupiahText(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Right: Review CTA Button
            Button(
                onClick = onNavigateToCart,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreenLight,
                    contentColor = BrandForest
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "Review & Bayar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun com.tbterminal.app.ui.common.UiText.toMessage(): String {
    return when (this) {
        is com.tbterminal.app.ui.common.UiText.DynamicString -> value
    }
}
