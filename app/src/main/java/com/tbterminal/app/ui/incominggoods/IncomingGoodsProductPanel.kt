package com.tbterminal.app.ui.incominggoods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun ProductSelectorCard(
    modifier: Modifier,
    uiState: IncomingGoodsUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProductToolbar(uiState, onSearchChanged, onCategoryFilterChanged)
        Spacer(modifier = Modifier.height(28.dp))
        ProductHeader()
        ProductRows(uiState, onSelectProduct)
        ProductPagination(uiState, onPreviousPage, onNextPage)
    }
}

@Composable
private fun ProductToolbar(
    uiState: IncomingGoodsUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.productSearchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari produk atau SKU...") },
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Cari produk", tint = IncomingMuted) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = IncomingTextFieldColors()
        )
        CategoryFilterDropdown(uiState.categoryFilter, uiState.categoryOptions, onCategoryFilterChanged)
    }
}

@Composable
private fun CategoryFilterDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .width(220.dp)
                .height(56.dp),
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
        uiState.isLoadingProducts -> Box(modifier = Modifier.fillMaxWidth().height(180.dp)) { LoadingState() }
        uiState.tableProducts.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            EmptyState("Tidak ada produk aktif yang cocok.")
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
