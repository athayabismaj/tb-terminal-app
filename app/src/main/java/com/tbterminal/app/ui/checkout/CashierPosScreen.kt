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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.ModalBottomSheet
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
        containerColor = Color(0xFFF8FAF9),
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
                    // ── 1. OPERATIONAL NOTA STATUS ROW (Hadir di bagian atas katalog produk seperti HTML mockup) ──
                    PosOperationalStatusRow(
                        heldCartsCount = state.heldCarts.size,
                        cartItemCount = cartItemCount,
                        onOpenHeldCarts = onOpenHeldCarts,
                        onOpenHoldCartDialog = onOpenHoldCartDialog
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
        // Tag Identitas Mesin Kasir POS-01 (Pill rounded-full dengan green pulse dot)
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color(0xFFECFDF5),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF059669))
                )
                Text(
                    text = "POS-01",
                    color = Color(0xFF065F46),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action Parkir & Tunda Transaksi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge Parkir Nota jika ada yang ditahan
            if (heldCartsCount > 0) {
                Surface(
                    onClick = onOpenHeldCarts,
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PauseCircleOutline,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Parkir ($heldCartsCount)",
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Tombol Tunda / Parkir Transaksi saat kasir melayani transaksi yang perlu ditunda
            if (cartItemCount > 0) {
                Surface(
                    onClick = onOpenHoldCartDialog,
                    shape = RoundedCornerShape(100.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Tunda Transaksi",
                            color = Slate700,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
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
// SUB-KOMPONEN 2B: POS FILTER BOTTOM SHEET (DESAIN FILTER PENJUALAN)
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pos-filter-sheet")
        ) {
            // Modal Header: Judul di kiri, tombol TERAPKAN & TUTUP di kanan atas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Filter Katalog Produk",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur kategori dan ketersediaan stok produk",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Terapkan di bagian atas
                    Button(
                        onClick = {
                            onApplyFilter(tempCategory, tempOnlyInStock)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF004528),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("pos-filter-apply")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Terapkan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tombol Tutup (X)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup Filter",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Scrollable Filter Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Section 1: Ketersediaan Stok
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "KETERSEDIAAN STOK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PosFilterOptionCard(
                            label = "Semua Produk",
                            isSelected = !tempOnlyInStock,
                            onClick = { tempOnlyInStock = false },
                            modifier = Modifier.weight(1f)
                        )
                        PosFilterOptionCard(
                            label = "Stok Tersedia (>0)",
                            isSelected = tempOnlyInStock,
                            onClick = { tempOnlyInStock = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Section 2: Kategori Produk
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "KATEGORI PRODUK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )
                    val categoryPairs = categories.chunked(2)
                    categoryPairs.forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowCategories.forEach { category ->
                                PosFilterOptionCard(
                                    label = category,
                                    isSelected = category == tempCategory,
                                    onClick = { tempCategory = category },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowCategories.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Sticky Footer: Tombol Reset di bagian bawah
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Surface(
                    onClick = {
                        tempCategory = "Semua Kategori"
                        tempOnlyInStock = false
                        onResetFilter()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pos-filter-reset"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFF004528))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF004528),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Reset Semua Filter",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004528)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosFilterOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Color(0xFF004528) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 0.5.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF004528) else Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isSelected) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color(0xFF004528),
                    modifier = Modifier.size(16.dp)
                )
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
// SUB-KOMPONEN 4: FLOATING BOTTOM CART DOCK (MATCHING HTML MOCKUP)
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
        shape = RoundedCornerShape(18.dp),
        color = BrandForest,
        border = BorderStroke(1.dp, Color(0xFF065F46)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Cart icon + badge + subtotal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandForestDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = "Keranjang",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // White circular badge with BrandForest text
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, BrandForestDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cartItemCount.toString(),
                            color = BrandForest,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "$cartItemCount Barang Dipilih",
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = cartSubtotal.toRupiahText(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Right: Checkout Action Button (Solid White Pill with BrandForest Text)
            Surface(
                onClick = onNavigateToCart,
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Review & Bayar",
                        color = BrandForest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = BrandForest,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun com.tbterminal.app.ui.common.UiText.toMessage(): String {
    return when (this) {
        is com.tbterminal.app.ui.common.UiText.DynamicString -> value
    }
}
