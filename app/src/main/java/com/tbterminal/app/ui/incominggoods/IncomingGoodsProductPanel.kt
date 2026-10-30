package com.tbterminal.app.ui.incominggoods

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
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onOpenForm: () -> Unit = {},
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    var showSummaryPopup by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        ProductToolbar(
            uiState = uiState,
            compact = compact,
            onSearchChanged = onSearchChanged,
            onCategoryFilterChanged = onCategoryFilterChanged,
            onOpenForm = onOpenForm,
        )
        if (compact) {
            ProductMobileHeader(
                isLoading = uiState.isLoadingProducts && uiState.products.isEmpty(),
                onShowSummaryClick = { showSummaryPopup = true }
            )
            ProductMobileList(uiState, onSelectProduct)
            if (!uiState.isLoadingProducts || uiState.products.isNotEmpty()) {
                ProductMobilePagination(uiState, onPreviousPage, onNextPage)
            }
            if (showSummaryPopup) {
                @OptIn(ExperimentalMaterial3Api::class)
                ModalBottomSheet(
                    onDismissRequest = { showSummaryPopup = false },
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
                                onClick = { showSummaryPopup = false },
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
                            onClick = { showSummaryPopup = false },
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
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
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
        var showMobileFilters by remember { mutableStateOf(false) }
        val isFiltered = uiState.categoryFilter != null
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IncomingSearchField(
                value = uiState.productSearchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.weight(1f),
                compact = true,
            )
            Surface(
                onClick = { showMobileFilters = true },
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (isFiltered) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                contentColor = Color(0xFF0F172A),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Filter produk",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF0F172A),
                    )
                }
            }
        }
        if (showMobileFilters) {
            IncomingCategoryFilterBottomSheet(
                selectedCategory = uiState.categoryFilter,
                categories = uiState.categoryOptions,
                onCategoryFilterChanged = onCategoryFilterChanged,
                onDismiss = { showMobileFilters = false },
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
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Filter Produk",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Pilih kategori untuk memfilter produk",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Section: KATEGORI
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "KATEGORI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    IncomingCategoryDropdownField(
                        selectedValue = selectedCategory,
                        options = categories,
                        onSelected = onCategoryFilterChanged,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Action Button (Selesai)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Text(
                        text = "Selesai",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingCategoryDropdownField(
    selectedValue: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAF9))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedValue ?: "Semua Kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = "Pilih kategori",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = 300.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White,
        ) {
            val isAll = selectedValue == null
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Semua Kategori",
                        fontSize = 14.sp,
                        fontWeight = if (isAll) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isAll) Color(0xFF256B57) else Color(0xFF1E293B),
                    )
                },
                onClick = {
                    expanded = false
                    onSelected(null)
                },
            )
            options.forEach { category ->
                val isSelected = selectedValue == category
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF1E293B),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(category)
                    },
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IncomingSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Menampilkan ${uiState.tableStartIndex}-${uiState.tableEndIndex} dari ${uiState.totalTableProducts} produk",
                color = IncomingText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Maksimal $INCOMING_GOODS_TABLE_PAGE_SIZE produk per halaman",
                color = IncomingMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductPageButton(uiState.tablePage > 1 && !uiState.isLoadingProducts, onPreviousPage, Icons.Default.ChevronLeft)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IncomingPrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Text("${uiState.tablePage}", color = IncomingSurface, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalTablePages}", color = IncomingMuted, fontWeight = FontWeight.SemiBold)
            ProductPageButton(uiState.tablePage < uiState.totalTablePages && !uiState.isLoadingProducts, onNextPage, Icons.Default.ChevronRight)
        }
    }
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
    onShowSummaryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            SkeletonBox(Modifier.width(120.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
            SkeletonBox(Modifier.size(32.dp), shape = RoundedCornerShape(12.dp))
        } else {
            androidx.compose.material3.Text(
                text = "Barang Masuk",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .clickable { onShowSummaryClick() }
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FactCheck,
                    contentDescription = "Ringkasan",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(16.dp)
                )
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val totalProducts = uiState.totalTableProducts
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            androidx.compose.material3.Text(
                text = "${uiState.tablePageProducts.size} dari $totalProducts produk",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hasPrevious = uiState.tablePage > 1 && !uiState.isLoadingProducts
                val hasNext = uiState.tablePage < uiState.totalTablePages && !uiState.isLoadingProducts
                androidx.compose.material3.IconButton(
                    onClick = onPreviousPage,
                    enabled = hasPrevious,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Halaman sebelumnya",
                        tint = if (hasPrevious) Color(0xFF475569) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
                androidx.compose.material3.Text(
                    text = "${uiState.tablePage} / ${uiState.totalTablePages.coerceAtLeast(1)}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
                androidx.compose.material3.IconButton(
                    onClick = onNextPage,
                    enabled = hasNext,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Halaman berikutnya",
                        tint = if (hasNext) Color(0xFF475569) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
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
