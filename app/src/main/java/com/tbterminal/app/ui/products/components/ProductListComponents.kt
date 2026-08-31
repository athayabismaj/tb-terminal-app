package com.tbterminal.app.ui.products.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
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
    Column(modifier = modifier.fillMaxWidth()) {
        if (!compact) ProductTableHeader()

        when {
            uiState.isLoading -> ProductTableSkeletonRows()
            uiState.errorMessage != null -> ProductErrorState(message = uiState.errorMessage, onRetry = onRetry)
            products.isEmpty() -> ProductEmptyState()
            else -> {
                products.forEachIndexed { index, product ->
                    if (compact) {
                        ProductCompactCard(
                            product = product,
                            onDetail = { onProductDetailClick(product.productId) },
                            onEdit = { onEditProductClick(product.productId) },
                            onToggleStatus = { onToggleProductClick(product) }
                        )
                    } else {
                        ProductTableRow(
                            product = product,
                            useAlternateBackground = index % 2 != 0,
                            onDetail = { onProductDetailClick(product.productId) },
                            onEdit = { onEditProductClick(product.productId) },
                            onToggleStatus = { onToggleProductClick(product) }
                        )
                    }
                }
            }
        }

        ProductPagination(
            page = uiState.page,
            totalPages = uiState.totalPages,
            total = uiState.totalProducts,
            pageSize = uiState.pageSize,
            visibleCount = products.size,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
internal fun ProductListToolbar(
    searchQuery: String,
    categories: List<String>,
    selectedCategory: String,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    compact: Boolean = false
) {
    var categoriesExpanded by remember { mutableStateOf(false) }

    val filter: @Composable () -> Unit = {
        Box {
            OutlinedButton(
                onClick = { categoriesExpanded = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ProductLine),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = ProductSurface,
                    contentColor = ProductText
                )
            ) {
                Text(selectedCategory, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ExpandMore, "Pilih kategori", tint = ProductMuted)
            }
            DropdownMenu(
                expanded = categoriesExpanded,
                onDismissRequest = { categoriesExpanded = false },
                modifier = Modifier.width(260.dp).heightIn(max = 320.dp).background(ProductSurface)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category, fontWeight = if (category == selectedCategory) FontWeight.SemiBold else FontWeight.Normal) },
                        onClick = { categoriesExpanded = false; onCategorySelected(category) }
                    )
                }
            }
        }
    }
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari SKU atau nama produk...", color = ProductMuted, fontSize = 14.sp) },
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Cari produk", tint = ProductMuted) },
            singleLine = true,
            modifier = fieldModifier.height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ProductText, unfocusedTextColor = ProductText,
                cursorColor = ProductPrimaryDark, focusedBorderColor = ProductPrimaryDark,
                unfocusedBorderColor = ProductLine, focusedContainerColor = ProductSurface,
                unfocusedContainerColor = ProductSurface
            )
        )
    }

    if (compact) Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        search(Modifier.fillMaxWidth())
        filter()
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        search(Modifier.weight(1f))
        Box(modifier = Modifier.width(220.dp)) { filter() }
    }
}

@Composable
private fun ProductCompactCard(
    product: ProductStock,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val lowStock = product.quantity <= product.minStock
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onDetail),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ProductLine)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.productName, color = ProductText, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text("${product.sku} · ${product.categoryName}", color = ProductMuted, fontSize = 12.sp)
                }
                ProductStatusPill(product.isActive)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Harga retail", color = ProductMuted, fontSize = 11.sp)
                    Text(product.priceRetail.moneyText(), color = ProductText, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Stok", color = ProductMuted, fontSize = 11.sp)
                    Text(
                        "${product.quantity.quantityText()} ${product.unitName}",
                        color = if (lowStock) ProductDanger else ProductText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(color = ProductLine)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                Spacer(Modifier.weight(1f))
                Text(if (product.isActive) "Aktif" else "Nonaktif", color = ProductMuted, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Switch(checked = product.isActive, onCheckedChange = { onToggleStatus() })
            }
        }
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
