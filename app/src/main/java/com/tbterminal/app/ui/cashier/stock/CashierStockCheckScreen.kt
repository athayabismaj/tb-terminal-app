package com.tbterminal.app.ui.cashier.stock

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import java.math.BigDecimal

// ==========================================
// WARNA
// ==========================================
private val StockSurface = Color(0xFFFFFFFF)
private val StockPrimary = Color(0xFF006948)
private val StockPrimaryLight = Color(0xFF00855D)
private val StockOnPrimary = Color(0xFFFFFFFF)
private val StockTextPrimary = Color(0xFF111827)
private val StockTextSecondary = Color(0xFF6B7280)
private val StockTextMuted = Color(0xFF9CA3AF)
private val StockBorder = Color(0xFFE5E7EB)
private val StockBorderLight = Color(0xFFF3F4F6)
private val StockTableHeaderBg = Color(0xFFF9FAFB)
private val StockPaginationBg = Color(0xFFF8FAFC)
private val StockDanger = Color(0xFFEF4444)
private val StockWarning = Color(0xFFF59E0B)
private val StockWarningDark = Color(0xFFB45309)
private val StockCategoryGreen = Color(0xFF047857)
private val StockAccentBlue = Color(0xFFEFF6FF)
private val StockAccentBlueDark = Color(0xFF0F172A)

@Composable
fun CashierStockCheckScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashierStockCheckViewModel = viewModel(
        factory = CashierStockCheckViewModel.factory(inventoryRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.StockCheck,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(32.dp)
        ) {
            // ── Header Judul ──
            StockPageTitle()

            Spacer(modifier = Modifier.height(24.dp))

            var selectedCategory by remember { mutableStateOf("Semua") }
            val filteredProducts = remember(uiState.products, selectedCategory) {
                if (selectedCategory == "Semua") {
                    uiState.products
                } else {
                    uiState.products.filter { it.categoryName == selectedCategory }
                }
            }

            // ── Search + Filter (di luar kartu) ──
            StockToolbar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChanged,
                products = uiState.products,
                isLoading = uiState.isLoading,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Kartu Tabel Utama (berisi tabel + pagination) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                colors = CardDefaults.cardColors(containerColor = StockSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StockBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Konten Tabel
                    when {
                        uiState.isLoading -> {
                            StockTableHeader()
                            HorizontalDivider(color = StockBorder)
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                items(5) {
                                    StockTableRowSkeleton()
                                    HorizontalDivider(color = StockBorderLight)
                                }
                            }
                        }
                        uiState.errorMessage != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Gagal memuat data",
                                        color = StockDanger,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        uiState.errorMessage!!,
                                        color = StockTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        filteredProducts.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = StockTextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Produk tidak ditemukan",
                                        color = StockTextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Coba ubah kata kunci atau filter kategori",
                                        color = StockTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            // Header Tabel
                            StockTableHeader()
                            HorizontalDivider(color = StockBorder)

                            // Baris Data
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                items(filteredProducts, key = { it.productId }) { product ->
                                    StockTableRow(product)
                                    HorizontalDivider(color = StockBorderLight)
                                }
                            }
                        }
                    }

                    // Pagination
                    HorizontalDivider(color = StockBorder)
                    StockPagination(
                        uiState = uiState,
                        onPreviousPage = viewModel::previousPage,
                        onNextPage = viewModel::nextPage
                    )
                }
            }
        }
    }
}

// ==========================================
// JUDUL HALAMAN (tanpa search bar)
// ==========================================
@Composable
private fun StockPageTitle() {
    Text(
        "Cek Stok",
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = StockTextPrimary
    )
}

// ==========================================
// TOOLBAR: SEARCH BAR + FILTER KATEGORI (di dalam kartu)
// ==========================================
@Composable
private fun StockToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    products: List<ProductStock>,
    isLoading: Boolean,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    // Ambil daftar kategori unik dari produk
    val categories = remember(products) {
        val cats = products
            .map { it.categoryName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("Semua") + cats
    }


    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Bar Custom
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StockSurface,
            border = BorderStroke(1.dp, StockBorder),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = StockTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Cari produk atau SKU...",
                            color = StockTextMuted,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = StockTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        StockCategoryDropdown(
            categories = categories,
            selectedCategory = selectedCategory,
            enabled = !isLoading && categories.size > 1,
            onCategorySelected = onCategorySelected
        )
    }
}

@Composable
private fun StockCategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    enabled: Boolean,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = StockSurface,
            border = BorderStroke(1.dp, StockBorder),
            modifier = Modifier
                .width(220.dp)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedCategory == "Semua") "Semua kategori" else selectedCategory,
                    color = if (enabled) StockTextPrimary else StockTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = if (enabled) StockTextSecondary else StockTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (category == "Semua") "Semua kategori" else category,
                            fontSize = 14.sp,
                            fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Medium,
                            color = if (category == selectedCategory) StockPrimary else StockTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ==========================================
// HEADER TABEL
// ==========================================
@Composable
private fun StockTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StockTableHeaderBg)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "INFORMASI PRODUK",
            modifier = Modifier.weight(3f),
            color = StockTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Text(
            "SKU & KATEGORI",
            modifier = Modifier.weight(2f),
            color = StockTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Text(
            "STOK",
            modifier = Modifier.weight(1.4f),
            color = StockTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.End
        )
    }
}

// ==========================================
// BARIS DATA TABEL
// ==========================================
@Composable
private fun StockTableRow(product: ProductStock) {
    val isOutOfStock = product.quantity <= BigDecimal.ZERO
    val isLowStock = !isOutOfStock &&
            product.minStock > BigDecimal.ZERO &&
            product.quantity <= product.minStock

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kolom: Info Produk
        Column(modifier = Modifier.weight(3f)) {
            Text(
                text = product.productName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StockTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isOutOfStock || isLowStock) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOutOfStock) StockDanger.copy(alpha = 0.08f)
                    else StockWarning.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (isOutOfStock) "Stok habis" else "Stok menipis",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutOfStock) StockDanger else StockWarningDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Kolom: SKU & Kategori
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = product.sku,
                fontSize = 14.sp,
                color = StockTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = StockPrimary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = product.categoryName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockCategoryGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Kolom: Stok
        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = product.quantity.stockQuantity(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isOutOfStock) StockDanger else StockTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = product.unitName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = StockTextSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

// ==========================================
// SKELETON LOADING
// ==========================================
@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE2E8F0).copy(alpha = alpha))
    )
}

@Composable
private fun StockTableRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Info Produk
        Column(modifier = Modifier.weight(3f)) {
            ShimmerBox(modifier = Modifier.height(20.dp).fillMaxWidth(0.8f))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.height(16.dp).fillMaxWidth(0.3f))
        }

        // SKU & Kategori
        Column(modifier = Modifier.weight(2f)) {
            ShimmerBox(modifier = Modifier.height(18.dp).fillMaxWidth(0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.height(16.dp).fillMaxWidth(0.4f))
        }

        // Stok
        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            ShimmerBox(modifier = Modifier.size(40.dp, 24.dp))
            Spacer(modifier = Modifier.width(4.dp))
            ShimmerBox(modifier = Modifier.size(24.dp, 16.dp).padding(bottom = 2.dp))
        }
    }
}

// ==========================================
// PAGINATION
// ==========================================
@Composable
private fun StockPagination(
    uiState: CashierStockCheckUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StockPaginationBg)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} produk",
            color = StockTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Previous
            Surface(
                onClick = onPreviousPage,
                enabled = uiState.page > 1,
                shape = RoundedCornerShape(10.dp),
                color = if (uiState.page > 1) StockSurface else Color.White,
                border = BorderStroke(1.dp, if (uiState.page > 1) StockBorder else StockBorderLight)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Sebelumnya",
                        tint = if (uiState.page > 1) StockTextPrimary else StockTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Halaman Aktif (Kotak Hijau)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(StockPrimaryLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.page.toString(),
                    color = StockOnPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Total Halaman
            Text(
                text = "/ ${uiState.totalPages}",
                color = StockTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Tombol Next
            Surface(
                onClick = onNextPage,
                enabled = uiState.page < uiState.totalPages,
                shape = RoundedCornerShape(10.dp),
                color = if (uiState.page < uiState.totalPages) StockSurface else Color.White,
                border = BorderStroke(1.dp, if (uiState.page < uiState.totalPages) StockBorder else StockBorderLight)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Berikutnya",
                        tint = if (uiState.page < uiState.totalPages) StockTextPrimary else StockTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// HELPER
// ==========================================
private fun BigDecimal.stockQuantity(): String {
    return stripTrailingZeros().toPlainString()
}
