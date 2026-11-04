package com.tbterminal.app.ui.products.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.products.ALL_PRODUCT_CATEGORIES
import com.tbterminal.app.ui.products.ProductDanger
import com.tbterminal.app.ui.products.ProductEmptyState
import com.tbterminal.app.ui.products.ProductErrorState
import com.tbterminal.app.ui.products.ProductHeaderText
import com.tbterminal.app.ui.products.ProductLine
import com.tbterminal.app.ui.products.ProductListUiState
import com.tbterminal.app.ui.products.ProductMuted
import com.tbterminal.app.ui.products.ProductPagination
import com.tbterminal.app.ui.products.ProductPrimary
import com.tbterminal.app.ui.products.ProductPrimaryDark
import com.tbterminal.app.ui.products.ProductSoft
import com.tbterminal.app.ui.products.ProductStatusPill
import com.tbterminal.app.ui.products.ProductSurface
import com.tbterminal.app.ui.products.ProductText
import com.tbterminal.app.ui.products.ProductWarning
import com.tbterminal.app.ui.products.moneyText
import com.tbterminal.app.ui.products.quantityText

@Composable
internal fun ProductTableCard(
    modifier: Modifier = Modifier,
    uiState: ProductListUiState,
    products: List<ProductStock>,
    onRetry: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onProductDetailClick: (String) -> Unit,
    onToggleProductClick: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Daftar produk",
            color = ProductText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Card(
            modifier = Modifier.fillMaxWidth().testTag("product-list-card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.72f)),
        ) {
            Column {
                if (!compact) ProductTableHeader()
                when {
                    uiState.isLoading && products.isEmpty() -> ProductTableSkeletonRows(compact = compact)
                    uiState.errorMessage != null && products.isEmpty() -> ProductErrorState(message = uiState.errorMessage, onRetry = onRetry)
                    products.isEmpty() -> ProductEmptyState()
                    else -> products.forEachIndexed { index, product ->
                        if (compact) {
                            ProductCompactRow(
                                product = product,
                                onDetail = { onProductDetailClick(product.productId) },
                                onEdit = { onEditProductClick(product.productId) },
                                onToggleStatus = { onToggleProductClick(product) },
                            )
                        } else {
                            ProductTableRow(
                                product = product,
                                useAlternateBackground = index % 2 != 0,
                                onDetail = { onProductDetailClick(product.productId) },
                                onEdit = { onEditProductClick(product.productId) },
                                onToggleStatus = { onToggleProductClick(product) },
                            )
                        }
                        if (index < products.lastIndex) {
                            HorizontalDivider(
                                modifier = if (compact) Modifier.padding(horizontal = 16.dp) else Modifier,
                                color = ProductLine.copy(alpha = 0.58f),
                            )
                        }
                    }
                }
            }
        }
        if (!uiState.isLoading && uiState.errorMessage == null && uiState.totalProducts > 0) {
            ProductPagination(
                page = uiState.page,
                totalPages = uiState.totalPages,
                total = uiState.totalProducts,
                pageSize = uiState.pageSize,
                visibleCount = products.size,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact,
            )
        }
    }
}

@Composable
internal fun ProductListToolbar(
    searchQuery: String,
    categories: List<String>,
    selectedCategory: String,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAddProductClick: (() -> Unit)? = null,
    onImportProductClick: (() -> Unit)? = null,
    compact: Boolean = false
) {
    var categoriesExpanded by remember { mutableStateOf(false) }
    var showMobileFilters by remember { mutableStateOf(false) }

    val filter: @Composable (Modifier) -> Unit = { filterModifier ->
        Box(modifier = filterModifier) {
            Surface(
                onClick = { categoriesExpanded = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("product-category-filter"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.62f)),
                color = ProductSurface,
                contentColor = ProductText,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedCategory,
                        modifier = Modifier.weight(1f),
                        color = ProductText,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "Pilih kategori",
                        modifier = Modifier.size(20.dp),
                        tint = ProductMuted,
                    )
                }
            }
            DropdownMenu(
                expanded = categoriesExpanded,
                onDismissRequest = { categoriesExpanded = false },
                modifier = Modifier.width(260.dp).heightIn(max = 320.dp),
                shape = RoundedCornerShape(14.dp),
                containerColor = ProductSurface,
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category, fontWeight = if (category == selectedCategory) FontWeight.SemiBold else FontWeight.Normal) },
                        modifier = Modifier.testTag("product-category-option-$category"),
                        onClick = { categoriesExpanded = false; onCategorySelected(category) }
                    )
                }
            }
        }
    }
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        Surface(
            modifier = fieldModifier.height(52.dp).testTag("product-search"),
            color = ProductSoft.copy(alpha = 0.58f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.42f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari produk",
                    modifier = Modifier.size(20.dp),
                    tint = ProductMuted,
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = ProductText),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isBlank()) {
                                Text(
                                    text = "Cari nama atau SKU",
                                    color = ProductMuted,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }

    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            search(Modifier.weight(1f))
            Surface(
                onClick = { showMobileFilters = true },
                modifier = Modifier.size(48.dp).testTag("product-open-filters"),
                shape = RoundedCornerShape(14.dp),
                color = com.tbterminal.app.ui.theme.TbGreenLight,
                contentColor = com.tbterminal.app.ui.theme.TbGreenDark,
                border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            onAddProductClick?.let { onAdd ->
                Surface(
                    onClick = onAdd,
                    modifier = Modifier.size(48.dp).testTag("product-add"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    contentColor = com.tbterminal.app.ui.theme.TbText,
                    border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.7f)),
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah produk", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        if (showMobileFilters) {
            TbMobileControlSheet(
                title = "Filter produk",
                subtitle = if (onImportProductClick != null) {
                    "Pilih kategori atau impor data produk"
                } else {
                    "Pilih kategori produk yang ingin ditampilkan"
                },
                onDismiss = { showMobileFilters = false },
                testTag = "product-filter-sheet",
            ) {
                Text(
                    "Kategori",
                    color = ProductText,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                filter(Modifier.fillMaxWidth())
                onImportProductClick?.let { onImport ->
                    OutlinedButton(
                        onClick = {
                            showMobileFilters = false
                            onImport()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("product-import"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Impor CSV", maxLines = 1)
                    }
                }
                TbMobileSheetDoneButton(
                    onClick = { showMobileFilters = false },
                    testTag = "product-filter-done",
                )
            }
        }
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        search(Modifier.weight(1f))
        filter(Modifier.width(220.dp))
        onImportProductClick?.let { onImport ->
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.height(52.dp).testTag("product-import"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text("Impor CSV", maxLines = 1)
            }
        }
        onAddProductClick?.let { onAdd ->
            Button(
                onClick = onAdd,
                modifier = Modifier.height(52.dp).testTag("product-add"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProductPrimaryDark),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah produk",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text("Tambah", maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProductCompactRow(
    product: ProductStock,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val lowStock = product.quantity <= product.minStock
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetail)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("product-list-row-${product.productId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    product.productName,
                    color = ProductText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${product.sku} · ${product.categoryName}",
                    color = ProductMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 2.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp).testTag("product-edit-${product.productId}"),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${product.productName}", tint = ProductMuted, modifier = Modifier.size(18.dp))
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Lihat detail ${product.productName}",
                    tint = ProductMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            ProductCompactMetric("Harga retail", product.priceRetail.moneyText(), Modifier.weight(5f))
            ProductCompactMetric(
                "Stok",
                "${product.quantity.quantityText()} ${product.unitName}",
                Modifier.weight(4f),
                if (lowStock) ProductDanger else ProductText,
            )
            Column(Modifier.weight(3f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Aktif", color = ProductMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = product.isActive,
                    onCheckedChange = { onToggleStatus() },
                    modifier = Modifier.testTag("product-status-${product.productId}").height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.tbterminal.app.ui.theme.TbGreen,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE4E4E7),
                        uncheckedBorderColor = Color(0xFFD4D4D8)
                    )
                )
            }
        }
    }
}

@Composable
private fun ProductCompactMetric(label: String, value: String, modifier: Modifier, valueColor: Color = ProductText) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = ProductMuted, fontSize = 11.sp, maxLines = 1)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ProductTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProductSoft)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductHeaderText("INFORMASI PRODUK", Modifier.weight(2.5f))
        ProductHeaderText("SKU & KATEGORI", Modifier.weight(2f))
        ProductHeaderText("HARGA RITEL / KONTRAKTOR", Modifier.weight(2.5f))
        ProductHeaderText("STOK", Modifier.weight(1f))
        ProductHeaderText("STATUS", Modifier.weight(1f), Alignment.CenterHorizontally)
        ProductHeaderText("AKSI", Modifier.weight(1f), Alignment.End)
    }
}

@Composable
internal fun ProductTableRow(
    product: ProductStock,
    useAlternateBackground: Boolean,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isLowStock = product.quantity <= product.minStock
    val rowAlpha = if (product.isActive) 1f else 0.62f
    val titleColor = if (product.isActive) ProductText else ProductMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (useAlternateBackground) ProductSoft.copy(alpha = 0.76f) else ProductSurface)
            .clickable(onClick = onDetail)
            .alpha(rowAlpha)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2.5f)) {
            Text(
                product.productName,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isLowStock) "Stok perlu dicek" else "Produk fisik",
                color = if (isLowStock) ProductWarning else ProductMuted,
                fontSize = 14.sp,
                fontWeight = if (isLowStock) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(modifier = Modifier.weight(2f)) {
            Text(product.sku, color = titleColor, fontSize = 14.sp)
            Text(
                product.categoryName,
                color = ProductMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(modifier = Modifier.weight(2.5f)) {
            Text(product.priceRetail.moneyText(), color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(product.priceContractor.moneyText(), color = ProductMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }

        Text(
            text = "${product.quantity.quantityText()} ${product.unitName}",
            modifier = Modifier.weight(1f),
            color = if (isLowStock) ProductDanger else titleColor,
            fontSize = 14.sp
        )

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ProductStatusPill(isActive = product.isActive)
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ProductMuted, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = product.isActive,
                onCheckedChange = { onToggleStatus() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ProductPrimaryDark,
                    uncheckedThumbColor = ProductMuted,
                    uncheckedTrackColor = ProductSoft
                )
            )
        }
    }
}

@Composable
internal fun ProductCategoryFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) ProductPrimary else ProductSurface
    val contentColor = if (isSelected) Color.White else ProductText
    val borderColor = if (isSelected) Color.Transparent else ProductLine

    Card(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductFilterBottomSheet(
    uiState: ProductListUiState,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember(uiState.selectedCategory) { mutableStateOf(uiState.selectedCategory) }
    var categoryQuery by remember { mutableStateOf("") }
    var isCategorySearchVisible by remember { mutableStateOf(false) }

    val rawCategories = uiState.availableCategories.filter { it != ALL_PRODUCT_CATEGORIES }
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
                        text = "Filter Produk",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pilih kategori untuk menyaring produk",
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
            val allCategoryItems: List<String> = listOf(ALL_PRODUCT_CATEGORIES) + filteredCategories
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
                                val isSelected = selectedCategory == cat
                                ProductFilterOptionCard(
                                    label = cat,
                                    isSelected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    modifier = Modifier.weight(1f)
                                )
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
                        selectedCategory = ALL_PRODUCT_CATEGORIES
                        onCategorySelected(ALL_PRODUCT_CATEGORIES)
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
                        onCategorySelected(selectedCategory)
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

@Composable
internal fun ProductFilterOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
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
                text = label,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductSummaryBottomSheet(
    uiState: ProductListUiState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    text = "Ringkasan Produk",
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
                        text = "${uiState.totalProducts} Produk",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF256B57)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 1: Produk Aktif
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Produk Aktif",
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
                        text = "${uiState.activeCount} Produk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 2: Stok Menipis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok Menipis / Kritis",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "${uiState.lowStockCount} Produk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.lowStockCount > 0) Color(0xFFEA580C) else Color(0xFF64748B)
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
                    text = "${uiState.categories.size} Kategori",
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
