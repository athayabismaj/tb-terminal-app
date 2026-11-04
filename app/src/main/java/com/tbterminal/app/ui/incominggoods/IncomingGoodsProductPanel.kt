package com.tbterminal.app.ui.incominggoods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.Sort

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.SkeletonBox
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.drawBehind
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun ProductSelectorCard(
    modifier: Modifier,
    uiState: IncomingGoodsUiState,
    compact: Boolean = false,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onApplyFilters: (String?, IncomingStockStatusFilter, IncomingProductSortOrder) -> Unit = { _, _, _ -> },
    onResetFilters: () -> Unit = {},
    onOpenForm: () -> Unit = {},
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    var localShowSummaryPopup by remember { mutableStateOf(false) }
    val isSummaryOpen = showSummaryPopup || localShowSummaryPopup

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        ProductToolbar(
            uiState = uiState,
            compact = compact,
            isSearchExpanded = isSearchExpanded,
            onToggleSearch = onToggleSearch,
            showFilterPopup = showFilterPopup,
            onDismissFilter = onDismissFilter,
            onSearchChanged = onSearchChanged,
            onCategoryFilterChanged = onCategoryFilterChanged,
            onApplyFilters = onApplyFilters,
            onResetFilters = onResetFilters,
            onOpenForm = onOpenForm,
        )
        if (compact) {
            ProductMobileHeader(
                isLoading = uiState.isLoadingProducts && uiState.products.isEmpty(),
                onShowSummaryClick = { localShowSummaryPopup = true }
            )
            ProductMobileList(uiState, onSelectProduct)
            if (!uiState.isLoadingProducts || uiState.products.isNotEmpty()) {
                ProductMobilePagination(uiState, onPreviousPage, onNextPage)
            }
            if (isSummaryOpen) {
                @OptIn(ExperimentalMaterial3Api::class)
                ModalBottomSheet(
                    onDismissRequest = { localShowSummaryPopup = false; onDismissSummary() },
                    containerColor = Color.White,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFE2E8F0))
                            )
                        }
                    },
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Modal Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ringkasan Produk",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.4).sp
                            )
                            IconButton(
                                onClick = { localShowSummaryPopup = false; onDismissSummary() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Ringkasan",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Modal Body / Metric Rows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // Total Produk
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Produk",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "${uiState.products.size} Macam",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )

                            // Kategori Produk
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kategori Produk",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "${uiState.categoryOptions.size} Kategori",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )

                            // Total Fisik Stok
                            val totalUnits = uiState.inventoryValuationSummary.totalStockUnits
                                .setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Fisik Stok",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "$totalUnits Unit",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )

                            // Valuasi Persediaan (Total HPP)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Valuasi Persediaan (HPP)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = uiState.inventoryValuationSummary.totalValuation.currencyText(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF256B57)
                                )
                            }
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )
                        }

                        // Action Button: Selesai
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { localShowSummaryPopup = false; onDismissSummary() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF256B57)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Selesai",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProductHeader()
                ProductRows(uiState, onSelectProduct)
            }
            if (!uiState.isLoadingProducts || uiState.products.isNotEmpty()) {
                ProductPagination(uiState, onPreviousPage, onNextPage)
            }
        }
    }
}

@Composable
private fun ProductToolbar(
    uiState: IncomingGoodsUiState,
    compact: Boolean,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onApplyFilters: (String?, IncomingStockStatusFilter, IncomingProductSortOrder) -> Unit = { _, _, _ -> },
    onResetFilters: () -> Unit = {},
    onOpenForm: () -> Unit = {}
) {
    val isCompact = compact || LocalConfiguration.current.screenWidthDp < 700
    val isLoading = uiState.isLoadingProducts && uiState.products.isEmpty()

    if (isLoading) {
        if (isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp))
                SkeletonBox(Modifier.size(42.dp), shape = RoundedCornerShape(12.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp))
                SkeletonBox(Modifier.width(220.dp).height(48.dp), shape = RoundedCornerShape(14.dp))
            }
        }
        return
    }
    
    if (isCompact) {
        var localShowFilters by remember { mutableStateOf(false) }
        val isFilterOpen = showFilterPopup || localShowFilters
        
        androidx.compose.animation.AnimatedVisibility(
            visible = isSearchExpanded || uiState.productSearchQuery.isNotEmpty(),
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            IncomingSearchField(
                value = uiState.productSearchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                compact = true,
            )
        }
        if (isFilterOpen) {
            IncomingCategoryFilterBottomSheet(
                uiState = uiState,
                categories = uiState.categoryOptions,
                onApplyFilters = { cat, stat, sort ->
                    onApplyFilters(cat, stat, sort)
                    localShowFilters = false
                    onDismissFilter()
                },
                onResetFilters = {
                    onResetFilters()
                    localShowFilters = false
                    onDismissFilter()
                },
                onDismiss = {
                    localShowFilters = false
                    onDismissFilter()
                },
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IncomingSearchField(
                value = uiState.productSearchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.weight(1f),
                compact = false,
            )
            IncomingCategoryFilterDropdown(
                selectedCategory = uiState.categoryFilter,
                categories = uiState.categoryOptions,
                onCategoryFilterChanged = onCategoryFilterChanged,
                modifier = Modifier.width(220.dp),
            )
        }
    }
}

@Composable
private fun IncomingSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.height(if (compact) 42.dp else 48.dp),
        color = Color.White,
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
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
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "Cari produk atau SKU...",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Hapus pencarian",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingCategoryFilterDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit,
    modifier: Modifier = Modifier.width(220.dp),
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            contentColor = Color(0xFF0F172A),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedCategory ?: "Semua Kategori",
                    modifier = Modifier.weight(1f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = "Pilih kategori",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .width(220.dp)
                .heightIn(max = 320.dp)
        ) {
            val isAllSelected = selectedCategory == null
            DropdownMenuItem(
                text = {
                    Text(
                        "Semua Kategori",
                        color = if (isAllSelected) Color(0xFF256B57) else Color(0xFF0F172A),
                        fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onCategoryFilterChanged(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                DropdownMenuItem(
                    text = {
                        Text(
                            category,
                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF0F172A),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onCategoryFilterChanged(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingCategoryFilterBottomSheet(
    uiState: IncomingGoodsUiState,
    categories: List<String>,
    onApplyFilters: (String?, IncomingStockStatusFilter, IncomingProductSortOrder) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(uiState.categoryFilter) }
    var selectedStockStatus by remember { mutableStateOf(uiState.stockStatusFilter) }
    var selectedSortOrder by remember { mutableStateOf(uiState.sortOrder) }
    var categoryQuery by remember { mutableStateOf("") }
    var isCategorySearchVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredCategories = remember(categories, categoryQuery) {
        if (categoryQuery.isBlank()) categories
        else categories.filter { it.contains(categoryQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
        modifier = Modifier.testTag("incoming-filter-sheet")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Filter Produk",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Pilih kategori dan urutkan produk barang masuk",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp
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
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // SECTION 1: KATEGORI PRODUK
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KATEGORI PRODUK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${categories.size} Kategori",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isCategorySearchVisible || categoryQuery.isNotEmpty()) Color(0xFFE2E8F0) else Color(0xFFF1F5F9))
                                    .clickable {
                                        isCategorySearchVisible = !isCategorySearchVisible
                                        if (!isCategorySearchVisible) {
                                            categoryQuery = ""
                                        }
                                    }
                                    .testTag("incoming-filter-toggle-category-search"),
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

                    // Search input
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("incoming-filter-category-search")
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

                    // Grid categories
                    val allCategoryItems: List<String?> = listOf<String?>(null) + filteredCategories
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            allCategoryItems.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { cat ->
                                        val isSelected = if (cat == null) selectedCategory == null else selectedCategory.equals(cat, ignoreCase = true)
                                        IncomingFilterCard(
                                            label = cat ?: "Semua Kategori",
                                            isSelected = isSelected,
                                            onClick = { selectedCategory = cat },
                                            modifier = Modifier.weight(1f),
                                            testTag = "incoming-filter-category-${cat ?: "all"}"
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: STATUS STOK
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "STATUS STOK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IncomingFilterCard(
                            label = "Semua",
                            isSelected = selectedStockStatus == IncomingStockStatusFilter.ALL,
                            onClick = { selectedStockStatus = IncomingStockStatusFilter.ALL },
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-stock-all"
                        )
                        IncomingFilterCard(
                            label = "Aman",
                            isSelected = selectedStockStatus == IncomingStockStatusFilter.SAFE,
                            onClick = { selectedStockStatus = IncomingStockStatusFilter.SAFE },
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-stock-safe"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IncomingFilterCard(
                            label = "Menipis",
                            isSelected = selectedStockStatus == IncomingStockStatusFilter.LOW,
                            onClick = { selectedStockStatus = IncomingStockStatusFilter.LOW },
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-stock-low"
                        )
                        IncomingFilterCard(
                            label = "Habis",
                            isSelected = selectedStockStatus == IncomingStockStatusFilter.OUT_OF_STOCK,
                            onClick = { selectedStockStatus = IncomingStockStatusFilter.OUT_OF_STOCK },
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-stock-out"
                        )
                    }
                }

                // SECTION 3: URUTKAN BERDASARKAN
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "URUTKAN BERDASARKAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IncomingFilterCard(
                            label = "Terbaru",
                            isSelected = selectedSortOrder == IncomingProductSortOrder.NEWEST,
                            onClick = { selectedSortOrder = IncomingProductSortOrder.NEWEST },
                            icon = Icons.Outlined.Schedule,
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-sort-newest"
                        )
                        IncomingFilterCard(
                            label = "Nama (A–Z)",
                            isSelected = selectedSortOrder == IncomingProductSortOrder.NAME_ASC,
                            onClick = { selectedSortOrder = IncomingProductSortOrder.NAME_ASC },
                            icon = Icons.AutoMirrored.Outlined.Sort,
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-sort-name"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IncomingFilterCard(
                            label = "Stok Terbanyak",
                            isSelected = selectedSortOrder == IncomingProductSortOrder.STOCK_DESC,
                            onClick = { selectedSortOrder = IncomingProductSortOrder.STOCK_DESC },
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-sort-stock"
                        )
                        IncomingFilterCard(
                            label = "Harga Terendah",
                            isSelected = selectedSortOrder == IncomingProductSortOrder.PRICE_ASC,
                            onClick = { selectedSortOrder = IncomingProductSortOrder.PRICE_ASC },
                            modifier = Modifier.weight(1f),
                            testTag = "incoming-filter-sort-price"
                        )
                    }
                }
            }

            // Sticky Footer Action Buttons
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button (2/5)
                Surface(
                    onClick = {
                        selectedCategory = null
                        selectedStockStatus = IncomingStockStatusFilter.ALL
                        selectedSortOrder = IncomingProductSortOrder.NEWEST
                        categoryQuery = ""
                        isCategorySearchVisible = false
                        onResetFilters()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("incoming-filter-reset"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFF256B57))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Reset",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF256B57)
                        )
                    }
                }

                // Terapkan Button (3/5)
                Button(
                    onClick = {
                        onApplyFilters(selectedCategory, selectedStockStatus, selectedSortOrder)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(3f)
                        .height(48.dp)
                        .testTag("incoming-filter-apply"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = "Terapkan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Home Bar Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(4.dp)
                        .background(Color(0xFFCBD5E1), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun IncomingFilterCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    testTag: String = "",
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF256B57)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.dp,
        modifier = modifier
            .height(46.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF256B57) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color(0xFF256B57),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IncomingSoft)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        HeaderText("PRODUK", Modifier.weight(2f))
        HeaderText("KATEGORI / SATUAN", Modifier.weight(1.4f))
        HeaderText("STOK SAAT INI", Modifier.weight(1.1f), Alignment.End)
        HeaderText("HARGA BELI", Modifier.weight(1.2f), Alignment.End)
        HeaderText("AKSI", Modifier.weight(0.8f), Alignment.End)
    }
}

@Composable
private fun ProductRows(
    uiState: IncomingGoodsUiState,
    onSelectProduct: (ProductStock) -> Unit
) {
    when {
        uiState.isLoadingProducts -> IncomingProductTableSkeleton()
        uiState.tableProducts.isEmpty() -> androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFDFE5E1)),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(
                    text = if (uiState.productSearchQuery.isBlank()) "Belum ada produk aktif." else "Tidak ada produk aktif yang cocok.",
                    color = Color(0xFF94A3B8)
                )
            }
        }
        else -> Column(modifier = Modifier.fillMaxWidth()) {
            uiState.tablePageProducts.forEachIndexed { index, product ->
                ProductRow(
                    product = product,
                    isSelected = product.productId == uiState.selectedProduct?.productId,
                    useAlternateBackground = index % 2 != 0,
                    onClick = { onSelectProduct(product) }
                )
                HorizontalDivider(color = IncomingLine.copy(alpha = 0.65f))
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductStock,
    isSelected: Boolean,
    useAlternateBackground: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        isSelected -> IncomingPrimary.copy(alpha = 0.08f)
        useAlternateBackground -> IncomingSoft.copy(alpha = 0.76f)
        else -> IncomingSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IncomingPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(product.productName.firstOrNull()?.uppercase() ?: "P", color = IncomingPrimaryDark, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(product.productName, color = IncomingText, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("SKU: ${product.sku}", color = IncomingMuted, fontSize = 12.sp)
            }
        }
        Column(modifier = Modifier.weight(1.4f)) {
            Text(product.categoryName, color = IncomingText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.unitName, color = IncomingMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Row(modifier = Modifier.weight(1.1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
            Text(product.quantity.qtyText(), color = if (product.quantity <= product.minStock) IncomingWarning else IncomingText, fontWeight = FontWeight.ExtraBold)
            Text(product.unitName, color = IncomingMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 1.dp))
        }
        Text(
            text = product.priceBuy.currencyText(),
            modifier = Modifier.weight(1.2f),
            color = IncomingText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Isi Form",
            modifier = Modifier.weight(0.8f),
            color = IncomingPrimaryDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProductPagination(
    uiState: IncomingGoodsUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val supportingText = "Menampilkan ${uiState.tableStartIndex}–${uiState.tableEndIndex} dari ${uiState.totalTableProducts} produk"
    TbPagination(
        currentPage = uiState.tablePage,
        totalPages = uiState.totalTablePages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = supportingText,
        isLoading = uiState.isLoadingProducts,
        compact = false,
        testTag = "incoming-goods-pagination",
    )
}

@Composable
private fun ProductPageButton(
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = IncomingText)
    ) {
        Icon(icon, contentDescription = null)
    }
}


@Composable
private fun ProductMobileHeader(
    isLoading: Boolean = false,
    onShowSummaryClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            SkeletonBox(Modifier.width(120.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
        } else {
            androidx.compose.material3.Text(
                text = "Barang Masuk",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
            )
        }
    }
}

@Composable
private fun ProductMobileList(
    uiState: IncomingGoodsUiState,
    onSelectProduct: (ProductStock) -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        when {
            uiState.isLoadingProducts -> {
                IncomingProductMobileSkeleton()
            }
            uiState.tableProducts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text(
                        text = if (uiState.productSearchQuery.isBlank()) "Belum ada produk aktif." else "Tidak ada produk aktif yang cocok.",
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            else -> {
                Column {
                    uiState.tablePageProducts.forEachIndexed { index, product ->
                        ProductMobileCard(
                            product = product,
                            onClick = { onSelectProduct(product) }
                        )
                        if (index < uiState.tablePageProducts.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductMobileCard(
    product: ProductStock,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text(
                        text = product.productName.firstOrNull()?.uppercase() ?: "P",
                        color = Color(0xFF256B57),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.material3.Text(
                        text = product.productName,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                androidx.compose.material3.Text(
                    text = "${product.sku} · ${product.categoryName}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                androidx.compose.material3.Text(
                    text = "STOK SAAT INI",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                val qtyText = if (product.quantity <= java.math.BigDecimal.ZERO) "0" else product.quantity.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
                androidx.compose.material3.Text(
                    text = "$qtyText ${product.unitName}",
                    color = Color(0xFF1E293B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.material3.Text(
                    text = "HARGA BELI",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(Modifier.height(2.dp))
                val currencyText = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply {
                    maximumFractionDigits = 0
                }.format(product.priceBuy)
                androidx.compose.material3.Text(
                    text = currencyText,
                    color = Color(0xFF256B57),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ProductMobilePagination(
    uiState: IncomingGoodsUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val totalProducts = uiState.totalTableProducts
    val supportingText = "${uiState.tablePageProducts.size} dari $totalProducts produk"
    TbPagination(
        currentPage = uiState.tablePage,
        totalPages = uiState.totalTablePages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = supportingText,
        isLoading = uiState.isLoadingProducts,
        compact = true,
        testTag = "incoming-goods-mobile-pagination",
    )
}

@Composable
private fun IncomingProductMobileSkeleton() {
    Column {
        repeat(4) { index ->
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.5f).height(14.dp))
                            Spacer(Modifier.height(4.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.35f).height(12.dp))
                        }
                    }
                    SkeletonBox(Modifier.size(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        SkeletonBox(Modifier.width(70.dp).height(10.dp))
                        Spacer(Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(60.dp).height(14.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SkeletonBox(Modifier.width(60.dp).height(10.dp))
                        Spacer(Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(90.dp).height(16.dp))
                    }
                }
            }
            if (index < 3) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

@Composable
private fun IncomingProductTableSkeleton() {
    Column {
        repeat(5) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) Color.White else IncomingSoft.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.weight(1.8f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(1.4f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(1.1f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(1.2f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(0.8f).height(16.dp))
            }
            HorizontalDivider(color = IncomingLine)
        }
    }
}
