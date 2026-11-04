package com.tbterminal.app.ui.pricemanagement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.products.ProductErrorState
import com.tbterminal.app.ui.products.ProductLoadingState
import com.tbterminal.app.ui.products.ProductPrimaryDark
import com.tbterminal.app.ui.products.ProductText
import java.math.BigDecimal

@Composable
fun AdminPriceManagementScreen(
    name: String,
    role: String,
    inventoryRepository: InventoryRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: PriceManagementViewModel = viewModel(
        factory = PriceManagementViewModel.factory(inventoryRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }
    val isFiltered = uiState.selectedCategory != ALL_PRICE_CATEGORIES

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.PriceManagement,
        pageTitle = "Harga produk",
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("price-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari produk",
                    tint = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
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
                    modifier = Modifier.size(36.dp).testTag("price-open-filters")
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

            // Button 3: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("price-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Tampilkan ringkasan harga",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
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
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        PriceManagementContent(
            state = uiState,
            isSearchExpanded = isSearchExpanded,
            onToggleSearch = { isSearchExpanded = !isSearchExpanded },
            showFilterPopup = showFilterPopup,
            onDismissFilter = { showFilterPopup = false },
            showSummaryPopup = showSummaryPopup,
            onDismissSummary = { showSummaryPopup = false },
            onSearchChanged = viewModel::onSearchQueryChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onRetry = { viewModel.loadProducts(uiState.currentPage) },
            onEdit = viewModel::openPriceDialog,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onClearActionMessage = viewModel::clearActionMessage,
            modifier = contentModifier
        )
    }

    if (uiState.selectedProductStock != null) {
        PriceUpdateDialog(
            state = uiState,
            onDismiss = viewModel::closePriceDialog,
            onSave = viewModel::updatePrice
        )
    }
}

@Composable
private fun PriceManagementContent(
    state: PriceManagementUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    onEdit: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onClearActionMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val compact = maxWidth < 700.dp
        val isFiltered = state.selectedCategory != ALL_PRICE_CATEGORIES
        val isSearchActive = isSearchExpanded || state.searchQuery.isNotEmpty()

        TopFloatingCapsule(
            message = state.actionMessage,
            error = if (state.products.isNotEmpty()) state.error else null,
            onDismiss = onClearActionMessage,
            modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)
        ) {
            if (compact) {
                // Expandable Search Bar (Riwayat Pembelian style)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.5f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Color(0xFF256B57),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        "Cari nama atau SKU produk...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                BasicTextField(
                                    value = state.searchQuery,
                                    onValueChange = onSearchChanged,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("price-product-search"),
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        onSearchChanged("")
                                    } else {
                                        onToggleSearch()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Tutup pencarian",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                // Active Filter Chips
                if (isFiltered) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Filter:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Category, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                Text(
                                    text = state.selectedCategory,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { onCategorySelected(ALL_PRICE_CATEGORIES) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCategorySelected(ALL_PRICE_CATEGORIES) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                PriceManagementToolbar(
                    state = state,
                    onSearchChanged = onSearchChanged,
                    onCategorySelected = onCategorySelected,
                    compact = compact,
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Daftar harga",
                    color = ProductText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                when {
                    state.isLoading && state.products.isEmpty() -> ProductLoadingState(modifier = Modifier.height(220.dp))
                    state.error != null && state.products.isEmpty() -> ProductErrorState(message = state.error, onRetry = onRetry)
                    state.visibleProducts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("Tidak ada produk ditemukan.", color = DashboardTextSecondary)
                        }
                    }
                    else -> {
                        PriceManagementTable(products = state.visibleProducts, onEdit = onEdit, compact = compact)
                    }
                }
                PriceManagementPagination(
                    state = state,
                    visibleCount = state.visibleProducts.size,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    compact = compact
                )
            }
        }

        if (showFilterPopup) {
            PriceFilterBottomSheet(
                state = state,
                onDismiss = onDismissFilter,
                onCategorySelected = onCategorySelected
            )
        }

        if (showSummaryPopup) {
            PriceSummaryBottomSheet(
                state = state,
                onDismiss = onDismissSummary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PriceFilterBottomSheet(
    state: PriceManagementUiState,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCat by remember(state.selectedCategory) { mutableStateOf(state.selectedCategory) }
    var categoryQuery by remember { mutableStateOf("") }
    var isCategorySearchVisible by remember { mutableStateOf(false) }

    val rawCategories = state.availableCategories.filter { it != ALL_PRICE_CATEGORIES }
    val filteredCategories = if (categoryQuery.isBlank()) {
        rawCategories
    } else {
        rawCategories.filter { it.contains(categoryQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filter Harga Produk",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pilih kategori untuk menyaring daftar harga",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Kategori Header & Search Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KATEGORI PRODUK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF64748B)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${rawCategories.size} Kategori",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isCategorySearchVisible) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                            .clickable {
                                isCategorySearchVisible = !isCategorySearchVisible
                                if (!isCategorySearchVisible) categoryQuery = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Cari kategori",
                            tint = if (isCategorySearchVisible || categoryQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expandable Category Search Input
            AnimatedVisibility(
                visible = isCategorySearchVisible || categoryQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (categoryQuery.isEmpty()) {
                                Text(
                                    text = "Cari kategori...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            BasicTextField(
                                value = categoryQuery,
                                onValueChange = { categoryQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (categoryQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { categoryQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus teks",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Category options grid
            val allCategoryItems: List<String> = listOf(ALL_PRICE_CATEGORIES) + filteredCategories
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    allCategoryItems.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { cat ->
                                val isSelected = selectedCat == cat
                                Surface(
                                    onClick = { selectedCat = cat },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = if (isSelected) 1.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF256B57),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedCat = ALL_PRICE_CATEGORIES
                        onCategorySelected(ALL_PRICE_CATEGORIES)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF256B57)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF256B57))
                ) {
                    Text("Reset", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onCategorySelected(selectedCat)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Terapkan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PriceSummaryBottomSheet(
    state: PriceManagementUiState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val withHppCount = state.products.count { it.priceBuy > BigDecimal.ZERO }
    val withoutHppCount = state.products.count { it.priceBuy <= BigDecimal.ZERO }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Harga",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TOTAL PRODUK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${state.products.size} Produk",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF256B57)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 1: HPP Terisi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Harga Modal (HPP) Terisi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "$withHppCount Produk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 2: Tanpa HPP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum Ada Harga Modal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "$withoutHppCount Produk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (withoutHppCount > 0) Color(0xFFEA580C) else Color(0xFF64748B)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 3: Total Kategori
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "${state.categories.size} Kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Selesai Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF256B57),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PriceUpdateDialog(
    state: PriceManagementUiState,
    onDismiss: () -> Unit,
    onSave: (BigDecimal, BigDecimal, BigDecimal, BigDecimal) -> Unit
) {
    val stock = state.selectedProductStock ?: return
    var priceBuyStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.priceBuy?.toPlainString() ?: stock.priceBuy.toPlainString())
    }
    var priceRetailStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.priceRetail?.toPlainString() ?: stock.priceRetail.toPlainString())
    }
    var priceContractorStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.priceContractor?.toPlainString() ?: stock.priceContractor.toPlainString())
    }
    var discountStr by remember(stock.productId, state.selectedProductDetail) {
        mutableStateOf(state.selectedProductDetail?.discount?.toPlainString() ?: stock.discount.toPlainString())
    }
    val validInput = listOf(priceBuyStr, priceRetailStr, priceContractorStr, discountStr)
        .all { it.toBigDecimalOrNull() != null }

    ModalBottomSheet(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("price-edit-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Edit harga",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = ProductText,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss, enabled = !state.isSaving, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = DashboardTextSecondary)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stock.productName, color = ProductText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("SKU ${stock.sku}", color = DashboardTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.isDetailLoading) {
                com.tbterminal.app.ui.components.SkeletonList(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    itemCount = 3,
                )
            } else if (state.selectedProductDetail != null) {
                val isRetailError = state.error != null
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriceEditField("Harga beli", priceBuyStr, { priceBuyStr = it }, "price-edit-buy", !state.isSaving)
                    PriceEditField(
                        label = "Harga jual",
                        value = priceRetailStr,
                        onValueChange = { priceRetailStr = it },
                        testTag = "price-edit-retail",
                        enabled = !state.isSaving,
                        isError = isRetailError,
                        supportingText = if (isRetailError) state.error else null
                    )
                    PriceEditField("Harga kontraktor", priceContractorStr, { priceContractorStr = it }, "price-edit-contractor", !state.isSaving)
                    PriceEditField("Diskon", discountStr, { discountStr = it }, "price-edit-discount", !state.isSaving)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) { Text("Batal") }
                    Button(
                        onClick = {
                            val buy = priceBuyStr.toBigDecimalOrNull() ?: return@Button
                            val retail = priceRetailStr.toBigDecimalOrNull() ?: return@Button
                            val contractor = priceContractorStr.toBigDecimalOrNull() ?: return@Button
                            val discount = discountStr.toBigDecimalOrNull() ?: return@Button
                            onSave(buy, retail, contractor, discount)
                        },
                        enabled = !state.isSaving && validInput,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("price-edit-save"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProductPrimaryDark),
                    ) {
                        if (state.isSaving) CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        ) else Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    enabled: Boolean,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("Rp") },
        isError = isError,
        supportingText = supportingText?.let {
            { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ProductPrimaryDark,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
        ),
    )
}
