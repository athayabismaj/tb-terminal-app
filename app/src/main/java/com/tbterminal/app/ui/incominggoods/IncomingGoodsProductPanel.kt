package com.tbterminal.app.ui.incominggoods

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.material.icons.outlined.SignalCellularAlt
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

    Column(modifier = modifier.fillMaxWidth()) {
        ProductToolbar(uiState, onSearchChanged, onCategoryFilterChanged, onOpenForm)
        if (compact) {
            Spacer(modifier = Modifier.height(16.dp))
            ProductMobileHeader(onShowSummaryClick = { showSummaryPopup = true })
            Spacer(modifier = Modifier.height(8.dp))
            ProductMobileList(uiState, onSelectProduct)
            Spacer(modifier = Modifier.height(16.dp))
            ProductMobilePagination(uiState, onPreviousPage, onNextPage)
            if (showSummaryPopup) {
                com.tbterminal.app.ui.components.TbMobileControlSheet(
                    title = "Ringkasan Produk",
                    subtitle = "Informasi total produk yang tersedia",
                    onDismiss = { showSummaryPopup = false }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ProductSummarySheetMetric(
                            title = "TOTAL PRODUK",
                            value = "${uiState.products.size} Macam",
                            note = "Semua produk",
                            icon = Icons.Outlined.Inventory2,
                            iconColor = Color(0xFF256B57),
                            iconBgColor = Color(0xFFE1EFEA)
                        )
                        ProductSummarySheetMetric(
                            title = "KATEGORI PRODUK",
                            value = "${uiState.categoryOptions.size} Kategori",
                            note = "Sesuai daftar produk",
                            icon = Icons.Outlined.List,
                            iconColor = Color(0xFF256B57),
                            iconBgColor = Color(0xFFE1EFEA)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    com.tbterminal.app.ui.components.TbMobileSheetDoneButton(
                        onClick = { showSummaryPopup = false }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(28.dp))
            ProductHeader()
            ProductRows(uiState, onSelectProduct)
            ProductPagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun ProductToolbar(
    uiState: IncomingGoodsUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onOpenForm: () -> Unit
) {
    val compact = LocalConfiguration.current.screenWidthDp < 700
    
    if (compact) {
        var showMobileFilters by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = uiState.productSearchQuery,
                onValueChange = onSearchChanged,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF0F172A)),
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFE0E7E3), RoundedCornerShape(24.dp))
                    .height(44.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (uiState.productSearchQuery.isEmpty()) {
                                Text("Cari produk atau SKU...", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                            }
                            innerTextField()
                        }
                    }
                }
            )
            Surface(
                onClick = { showMobileFilters = !showMobileFilters },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E7E3)),
                color = Color.White,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Filter", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                }
            }
            Surface(
                onClick = onOpenForm,
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF256B57), // bg-brand-600
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (showMobileFilters) {
            com.tbterminal.app.ui.components.TbMobileControlSheet(
                title = "Filter Kategori",
                subtitle = "Pilih kategori untuk memfilter produk",
                onDismiss = { showMobileFilters = false }
            ) {
                androidx.compose.material3.Text(
                    "Kategori",
                    color = Color(0xFF0F172A),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                CategoryFilterDropdown(uiState.categoryFilter, uiState.categoryOptions, onCategoryFilterChanged, Modifier.fillMaxWidth().height(48.dp))
                Spacer(Modifier.height(16.dp))
                com.tbterminal.app.ui.components.TbMobileSheetDoneButton(
                    onClick = { showMobileFilters = false }
                )
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CategoryFilterDropdown(uiState.categoryFilter, uiState.categoryOptions, onCategoryFilterChanged)
            androidx.compose.material3.Button(
                onClick = onOpenForm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // IncomingPrimary fallback just in case
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tambah Produk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CategoryFilterDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit,
    modifier: Modifier = Modifier.width(220.dp).height(56.dp)
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, IncomingLine),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = IncomingSurface,
                contentColor = IncomingText
            )
        ) {
            Text(
                text = selectedCategory ?: "Semua kategori",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Pilih kategori", tint = IncomingMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .heightIn(max = 320.dp)
                .background(IncomingSurface)
        ) {
            DropdownMenuItem(text = { Text("Semua kategori") }, onClick = {
                onCategoryFilterChanged(null)
                expanded = false
            })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = {
                    onCategoryFilterChanged(category)
                    expanded = false
                })
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
        uiState.isLoadingProducts -> com.tbterminal.app.ui.components.SkeletonList(modifier = Modifier.fillMaxWidth().height(180.dp), itemCount = 4)
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
private fun ProductMobileHeader(onShowSummaryClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = "Daftar Produk Masuk",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.2).sp
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
                Icons.Outlined.SignalCellularAlt,
                contentDescription = "Ringkasan",
                tint = Color(0xFF334155),
                modifier = Modifier.size(16.dp)
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
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDFE5E1)),
        shadowElevation = 2.dp
    ) {
        when {
            uiState.isLoadingProducts -> {
                com.tbterminal.app.ui.components.SkeletonList(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    itemCount = 4,
                )
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
                                color = Color(0xFFEEF2F0),
                                modifier = Modifier.padding(horizontal = 16.dp)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFEEF2F0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = product.productName.firstOrNull()?.uppercase() ?: "P",
                    color = Color(0xFF256B57), // PurchasePrimary
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    androidx.compose.material3.Text(
                        text = product.productName,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill=false)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                androidx.compose.material3.Text(
                    text = "${product.sku} • ${product.categoryName}",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                androidx.compose.material3.Text("STOK SAAT INI", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                val qtyText = if (product.quantity <= java.math.BigDecimal.ZERO) "0" else product.quantity.toString()
                androidx.compose.material3.Text(
                    text = "$qtyText ${product.unitName}",
                    color = Color(0xFF334155),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.material3.Text("HARGA BELI", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                val currencyText = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply {
                    maximumFractionDigits = 0
                }.format(product.priceBuy)
                androidx.compose.material3.Text(
                    text = currencyText,
                    color = Color(0xFF256B57),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
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
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val totalProducts = uiState.totalTableProducts
        // Pill
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(50.dp))
                .border(1.dp, Color(0xFFE0E7E3), RoundedCornerShape(50.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            androidx.compose.material3.Text(
                text = "${uiState.tablePageProducts.size} dari $totalProducts produk",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
        }
        
        // Pagination Nav
        Row(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(50.dp))
                .border(1.dp, Color(0xFFE0E7E3), RoundedCornerShape(50.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hasPrevious = uiState.tablePage > 1 && !uiState.isLoadingProducts
            androidx.compose.material3.IconButton(onClick = onPreviousPage, enabled = hasPrevious, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = if (hasPrevious) Color(0xFF334155) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
            androidx.compose.material3.Text("${uiState.tablePage} / ${uiState.totalTablePages}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
            val hasNext = uiState.tablePage < uiState.totalTablePages && !uiState.isLoadingProducts
            androidx.compose.material3.IconButton(onClick = onNextPage, enabled = hasNext, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = if (hasNext) Color(0xFF334155) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }
    }
}


@Composable
private fun ProductSummarySheetMetric(
    title: String,
    value: String,
    note: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    isHighlighted: Boolean = false
) {
    val bgColor = if (isHighlighted) Color(0xFFF2F8F5) else Color.White
    val borderColor = if (isHighlighted) Color(0xFFC3DFD5) else Color(0xFFE2E8F0)
    val titleColor = if (isHighlighted) Color(0xFF1E5847) else Color(0xFF94A3B8)
    val valueColor = if (isHighlighted) Color(0xFF256B57) else Color(0xFF0F172A)

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isHighlighted) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, color = titleColor, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(value, fontSize = if (isHighlighted) 26.sp else 24.sp, color = valueColor, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp)
                Spacer(Modifier.height(4.dp))
                Text(note, fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}
