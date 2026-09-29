package com.tbterminal.app.ui.pricemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.products.ProductLine
import com.tbterminal.app.ui.products.ProductMuted
import com.tbterminal.app.ui.products.ProductPrimaryDark
import com.tbterminal.app.ui.products.ProductSoft
import com.tbterminal.app.ui.products.ProductSurface
import com.tbterminal.app.ui.products.ProductText
import com.tbterminal.app.ui.products.components.ProductListToolbar
import com.tbterminal.app.ui.products.moneyText
import com.tbterminal.app.ui.components.TbPagination
import java.math.BigDecimal

@Composable
internal fun PriceManagementToolbar(
    state: PriceManagementUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    compact: Boolean,
) {
    ProductListToolbar(
        searchQuery = state.searchQuery,
        categories = state.availableCategories,
        selectedCategory = state.selectedCategory,
        onSearchChanged = onSearchChanged,
        onCategorySelected = onCategorySelected,
        compact = compact,
    )
}

@Composable
internal fun PriceManagementTable(
    products: List<ProductStock>,
    onEdit: (ProductStock) -> Unit,
    compact: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("price-product-list-card"),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.72f)),
    ) {
        Column {
            if (!compact) PriceTableHeader()
            products.forEachIndexed { index, product ->
                if (compact) PriceMobileRow(product) { onEdit(product) } else PriceTableRow(
                    product = product,
                    useAlternateBackground = index % 2 != 0,
                    onEdit = { onEdit(product) }
                )
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

@Composable
private fun PriceMobileRow(product: ProductStock, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("price-product-card-${product.productId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    product.productName,
                    color = ProductText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${product.sku} · ${product.categoryName.ifBlank { "Tanpa kategori" }}",
                    modifier = Modifier.testTag("price-product-category"),
                    color = ProductMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Edit harga ${product.productName}",
                tint = ProductMuted,
                modifier = Modifier.padding(start = 6.dp).size(22.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PriceMobileValue("Harga beli", product.priceBuy.moneyText(), Modifier.weight(1f))
            PriceMobileValue("Harga jual", product.priceRetail.moneyText(), Modifier.weight(1f), Alignment.CenterHorizontally, true)
            PriceMobileValue("Kontraktor", product.priceContractor.moneyText(), Modifier.weight(1f), Alignment.End)
        }
        if (product.discount > BigDecimal.ZERO) {
            Text(
                "Diskon ${product.discount.moneyText()}",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PriceMobileValue(
    label: String,
    value: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    emphasized: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = ProductMuted, fontSize = 11.sp, maxLines = 1)
        Text(
            value,
            color = if (emphasized) ProductPrimaryDark else ProductText,
            fontSize = 13.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PriceTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProductSoft)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PriceHeader("PRODUK", Modifier.weight(3.1f))
        PriceHeader("HARGA BELI", Modifier.weight(1.35f), Alignment.End)
        PriceHeader("HARGA RETAIL", Modifier.weight(1.35f), Alignment.End)
        PriceHeader("KONTRAKTOR", Modifier.weight(1.35f), Alignment.End)
        PriceHeader("DISKON", Modifier.weight(1f), Alignment.End)
        PriceHeader("AKSI", Modifier.weight(0.7f), Alignment.End)
    }
}

@Composable
private fun PriceTableRow(
    product: ProductStock,
    useAlternateBackground: Boolean,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (useAlternateBackground) ProductSoft.copy(alpha = 0.76f) else ProductSurface)
            .clickable(onClick = onEdit)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(3.1f).padding(end = 16.dp)) {
            Text(
                text = product.productName,
                color = ProductText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${product.sku} · ${product.categoryName}",
                color = ProductMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PriceValue(product.priceBuy.moneyText(), Modifier.weight(1.35f))
        PriceValue(product.priceRetail.moneyText(), Modifier.weight(1.35f), FontWeight.SemiBold)
        PriceValue(product.priceContractor.moneyText(), Modifier.weight(1.35f))
        DiscountValue(product.discount, Modifier.weight(1f))
        Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit harga", tint = ProductMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PriceValue(
    value: String,
    modifier: Modifier,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = value,
        modifier = modifier,
        color = ProductText,
        fontSize = 14.sp,
        fontWeight = fontWeight,
        textAlign = TextAlign.End
    )
}

@Composable
private fun DiscountValue(
    discount: BigDecimal,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (discount > BigDecimal.ZERO) Color(0xFFFEF2F2) else Color.Transparent
    ) {
        Text(
            text = if (discount > BigDecimal.ZERO) "-${discount.moneyText()}" else "-",
            color = if (discount > BigDecimal.ZERO) Color(0xFFEF4444) else ProductMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}

@Composable
private fun PriceHeader(
    text: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = ProductMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PriceManagementPagination(
    state: PriceManagementUiState,
    visibleCount: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false
) {
    val safePage = state.currentPage.coerceAtLeast(1)
    val safeTotalPages = state.totalPages.coerceAtLeast(1)
    val startItem = if (state.totalProducts == 0L) 0L else ((safePage - 1) * state.pageSize + 1L)
    val endItem = if (state.totalProducts == 0L) 0L else (startItem + visibleCount - 1L).coerceAtMost(state.totalProducts)
    val rangeText = if (state.selectedCategory == ALL_PRICE_CATEGORIES) {
        "Menampilkan $startItem-$endItem dari ${state.totalProducts} produk"
    } else {
        "Menampilkan $visibleCount produk kategori pada halaman ini"
    }

    TbPagination(
        currentPage = safePage,
        totalPages = safeTotalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (compact) "${state.totalProducts} produk" else rangeText,
        isLoading = state.isLoading,
        testTag = "price-management-pagination",
    )
}
