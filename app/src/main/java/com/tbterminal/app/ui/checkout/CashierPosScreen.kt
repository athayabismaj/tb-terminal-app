package com.tbterminal.app.ui.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    selectedCategory: String = "Semua Kategori",
    onCategorySelected: (String) -> Unit = {},
    onlyInStock: Boolean = false,
    onToggleOnlyInStock: (Boolean) -> Unit = {},
    showFilterDialog: Boolean = false,
    onDismissFilterDialog: () -> Unit = {},
    showMaterialEstimator: Boolean = false,
    onDismissMaterialEstimator: () -> Unit = {},
    onApplyMaterialSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCameraScanner by remember { mutableStateOf(false) }

    val allCategory = "Semua Kategori"
    val categories = remember(state.products) {
        listOf(allCategory) + state.products.map { it.categoryName }.filter { it.isNotBlank() }.distinct()
    }
    LaunchedEffect(categories, selectedCategory) {
        if (selectedCategory !in categories && selectedCategory != allCategory) {
            onCategorySelected(allCategory)
        }
    }

    val filteredProducts = remember(selectedCategory, onlyInStock, state.products) {
        state.products
            .filter { selectedCategory == allCategory || it.categoryName == selectedCategory }
            .filter { !onlyInStock || it.stockQty > BigDecimal.ZERO }
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
                        .padding(top = 8.dp)
                ) {
                    // ── 1. OPERATIONAL NOTA STATUS ROW (Hanya tampil jika ada nota diparkir / keranjang terisi) ──
                    if (state.heldCarts.isNotEmpty() || cartItemCount > 0) {
                        PosOperationalStatusRow(
                            heldCartsCount = state.heldCarts.size,
                            cartItemCount = cartItemCount,
                            onOpenHeldCarts = onOpenHeldCarts,
                            onOpenHoldCartDialog = onOpenHoldCartDialog
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── 2. EXPANDABLE SEARCH & SCANNER INPUT BAR (Hanya nampak ketika icon cari di-trigger atau teks query terisi) ──
                    AnimatedVisibility(
                        visible = isSearchExpanded || state.searchQuery.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            PosSearchBar(
                                query = state.searchQuery,
                                onQueryChanged = onSearchChanged,
                                onOpenCameraScanner = { showCameraScanner = true },
                                onCloseSearch = {
                                    onSearchChanged("")
                                    onToggleSearch()
                                }
                            )
                        }
                    }

                    // ── 3. CATEGORY PILL CAROUSEL (ONE-TAP SPEED) ──
                    PosCategoryCarousel(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = onCategorySelected
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
            onDismiss = onDismissMaterialEstimator,
            onApplySearchQuery = onApplyMaterialSearch
        )
    }

    // Modal Filter Produk & Kategori POS
    if (showFilterDialog) {
        PosFilterDialog(
            categories = categories,
            selectedCategory = selectedCategory,
            onlyInStock = onlyInStock,
            onApplyFilter = { cat, stockOnly ->
                onCategorySelected(cat)
                onToggleOnlyInStock(stockOnly)
                onDismissFilterDialog()
            },
            onResetFilter = {
                onCategorySelected(allCategory)
                onToggleOnlyInStock(false)
                onDismissFilterDialog()
            },
            onDismiss = onDismissFilterDialog
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN 1: OPERATIONAL NOTA STATUS ROW (PARKIR NOTA)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosOperationalStatusRow(
    heldCartsCount: Int,
    cartItemCount: Int,
    onOpenHeldCarts: () -> Unit,
    onOpenHoldCartDialog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tag Identitas Mesin Kasir POS
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = BrandGreenLight,
            border = BorderStroke(1.dp, BrandGreenBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(BrandForest, CircleShape)
                )
                Text(
                    text = "POS-01",
                    color = BrandForest,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Parkir Nota
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Badge Parkir Nota jika ada yang ditahan
            if (heldCartsCount > 0) {
                Surface(
                    onClick = onOpenHeldCarts,
                    shape = RoundedCornerShape(10.dp),
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PauseCircleOutline,
                            contentDescription = null,
                            tint = Amber700,
                            modifier = Modifier.size(14.dp)
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

            // Tombol Tunda / Parkir Transaksi saat kasir melayani transaksi yang perlu ditunda
            if (cartItemCount > 0) {
                OutlinedButton(
                    onClick = onOpenHoldCartDialog,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Amber700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber700),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Tunda Transaksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SUB-KOMPONEN 2: EXPANDABLE SEARCH & BARCODE SCANNER BAR
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onOpenCameraScanner: () -> Unit,
    onCloseSearch: () -> Unit
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
                tint = BrandForest,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = { onQueryChanged("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Hapus Pencarian",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenCameraScanner,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandGreenLight)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan Barcode Kamera",
                        tint = BrandForest,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onCloseSearch,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Tutup Pencarian",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
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
// SUB-KOMPONEN 2B: POS FILTER DIALOG (KATEGORI & STATUS STOK)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PosFilterDialog(
    categories: List<String>,
    selectedCategory: String,
    onlyInStock: Boolean,
    onApplyFilter: (category: String, onlyInStock: Boolean) -> Unit,
    onResetFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }
    var tempOnlyInStock by remember(onlyInStock) { mutableStateOf(onlyInStock) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F6ED),
                            border = BorderStroke(1.dp, Color(0xFF1B5E3C))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF004528),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Filter Katalog Produk",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Saring produk berdasarkan kategori & ketersediaan",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Tutup",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Section 1: Ketersediaan Stok
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ketersediaan Stok",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option A: Semua Produk
                        Surface(
                            onClick = { tempOnlyInStock = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!tempOnlyInStock) Color(0xFFE8F6ED) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (!tempOnlyInStock) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Semua Produk",
                                    fontSize = 12.sp,
                                    fontWeight = if (!tempOnlyInStock) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!tempOnlyInStock) Color(0xFF004528) else Color(0xFF475569)
                                )
                            }
                        }

                        // Option B: Hanya Stok Tersedia
                        Surface(
                            onClick = { tempOnlyInStock = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (tempOnlyInStock) Color(0xFFE8F6ED) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (tempOnlyInStock) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Stok Tersedia (>0)",
                                    fontSize = 12.sp,
                                    fontWeight = if (tempOnlyInStock) FontWeight.Bold else FontWeight.Medium,
                                    color = if (tempOnlyInStock) Color(0xFF004528) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                // Section 2: Kategori Produk
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kategori Produk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = category == tempCategory
                                Surface(
                                    onClick = { tempCategory = category },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF004528) else Color(0xFF334155)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF1B5E3C),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Footer Buttons: Reset & Terapkan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetFilter,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = "Reset",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { onApplyFilter(tempCategory, tempOnlyInStock) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004528)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = "Terapkan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
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
