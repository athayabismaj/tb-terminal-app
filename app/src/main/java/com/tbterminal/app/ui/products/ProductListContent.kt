package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun ProductListContent(
    modifier: Modifier,
    uiState: ProductListUiState,
    onSearchChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onProductDetailClick: (String) -> Unit,
    onCategoriesClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onToggleProductClick: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    val filterCategories = remember(uiState.products) {
        listOf("Semua") + uiState.products
            .map(ProductStock::categoryName)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProductBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProductHeader(
            title = "Daftar Produk",
            subtitle = "Kelola inventaris stok dan harga barang Anda.",
            actions = {}
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filterCategories, key = { category -> category }) { category ->
                ProductCategoryFilterChip(
                    text = category,
                    isSelected = category == selectedCategory,
                    onClick = {
                        selectedCategory = category
                        onSearchChanged(if (category == "Semua") "" else category)
                    }
                )
            }
        }

        if (uiState.actionMessage != null) {
            ProductBanner(message = uiState.actionMessage, onDismiss = onDismissMessage)
        }

        ProductTableCard(
            modifier = Modifier.weight(1f),
            uiState = uiState,
            onSearchChanged = { query ->
                selectedCategory = "Semua"
                onSearchChanged(query)
            },
            onRetry = onRetry,
            onEditProductClick = onEditProductClick,
            onProductDetailClick = onProductDetailClick,
            onToggleProductClick = onToggleProductClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
internal fun ProductTableCard(
    modifier: Modifier = Modifier,
    uiState: ProductListUiState,
    onSearchChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onProductDetailClick: (String) -> Unit,
    onToggleProductClick: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ProductLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Cari SKU atau nama produk...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProductPrimary,
                        unfocusedBorderColor = ProductLine,
                        focusedContainerColor = ProductSoft.copy(alpha = 0.58f),
                        unfocusedContainerColor = ProductSoft.copy(alpha = 0.58f)
                    )
                )
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ProductLine),
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Muat Ulang", fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = ProductLine)
            ProductTableHeader()
            HorizontalDivider(color = ProductLine)

            when {
                uiState.isLoading -> ProductLoadingState()
                uiState.errorMessage != null -> ProductErrorState(message = uiState.errorMessage, onRetry = onRetry)
                uiState.products.isEmpty() -> ProductEmptyState()
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = uiState.products,
                            key = { product -> product.productId }
                        ) { product ->
                            ProductTableRow(
                                product = product,
                                onDetail = { onProductDetailClick(product.productId) },
                                onEdit = { onEditProductClick(product.productId) },
                                onToggleStatus = { onToggleProductClick(product) }
                            )
                            HorizontalDivider(color = ProductLine.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            ProductPagination(
                page = uiState.page,
                totalPages = uiState.totalPages,
                total = uiState.totalProducts,
                pageSize = uiState.pageSize,
                visibleCount = uiState.products.size,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage
            )
        }
    }
}

@Composable
internal fun ProductTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProductSoft)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductHeaderText("INFORMASI PRODUK", Modifier.weight(3f))
        ProductHeaderText("SKU & KATEGORI", Modifier.weight(2f))
        ProductHeaderText("HARGA RITEL / KONTRAKTOR", Modifier.weight(2.5f), Alignment.End)
        ProductHeaderText("STOK", Modifier.weight(1.5f), Alignment.End)
        ProductHeaderText("STATUS", Modifier.weight(1.5f), Alignment.CenterHorizontally)
        ProductHeaderText("AKSI", Modifier.weight(1f), Alignment.End)
    }
}

@Composable
internal fun ProductTableRow(
    product: ProductStock,
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
            .clickable(onClick = onDetail)
            .alpha(rowAlpha)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(3f)) {
            Text(
                product.productName,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isLowStock) "Stok perlu dicek" else "Produk fisik",
                color = if (isLowStock) ProductWarning else ProductMuted,
                fontSize = 12.sp,
                fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(modifier = Modifier.weight(2f)) {
            Text(product.sku, color = titleColor, fontSize = 14.sp)
            Text(
                product.categoryName,
                color = if (product.isActive) ProductPrimaryDark else ProductMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(modifier = Modifier.weight(2.5f), horizontalAlignment = Alignment.End) {
            Text(product.priceRetail.moneyText(), color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(product.priceContractor.moneyText(), color = ProductMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = product.quantity.quantityText(),
                color = if (isLowStock) ProductDanger else titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                product.unitName,
                color = ProductMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
            )
        }

        Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
            ProductStatusPill(isActive = product.isActive)
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ProductMuted, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onToggleStatus) {
                Icon(
                    imageVector = if (product.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                    contentDescription = if (product.isActive) "Nonaktifkan" else "Aktifkan",
                    tint = if (product.isActive) ProductPrimary else ProductMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductCategoryFilterChip(
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

