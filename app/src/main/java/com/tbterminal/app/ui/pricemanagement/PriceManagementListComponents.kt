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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.math.BigDecimal

@Composable
internal fun PriceManagementToolbar(
    state: PriceManagementUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    ProductListToolbar(
        searchQuery = state.searchQuery,
        categories = state.availableCategories,
        selectedCategory = state.selectedCategory,
        onSearchChanged = onSearchChanged,
        onCategorySelected = onCategorySelected
    )
}

@Composable
internal fun PriceManagementTable(
    products: List<ProductStock>,
    onEdit: (ProductStock) -> Unit,
    compact: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!compact) PriceTableHeader()
        products.forEachIndexed { index, product ->
            if (compact) PriceMobileRow(product) { onEdit(product) } else PriceTableRow(
                product = product,
                useAlternateBackground = index % 2 != 0,
                onEdit = { onEdit(product) }
            )
        }
    }
}

@Composable
private fun PriceMobileRow(product: ProductStock, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(product.productName, color = ProductText, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${product.sku} · ${product.categoryName}", color = ProductMuted, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.Edit, "Edit harga", tint = ProductMuted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PriceMobileValue("Beli", product.priceBuy.moneyText())
            PriceMobileValue("Jual", product.priceRetail.moneyText(), Alignment.CenterHorizontally)
            PriceMobileValue("Kontraktor", product.priceContractor.moneyText(), Alignment.End)
        }
        if (product.discount > BigDecimal.ZERO) Text("Diskon ${product.discount.moneyText()}", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = ProductLine)
}

@Composable
private fun PriceMobileValue(label: String, value: String, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment) { Text(label, color = ProductMuted, fontSize = 10.sp); Text(value, color = ProductText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
        PriceHeader("INFORMASI PRODUK", Modifier.weight(2.3f))
        PriceHeader("SKU & KATEGORI", Modifier.weight(1.8f))
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
        Text(
            text = product.productName,
            modifier = Modifier.weight(2.3f),
            color = ProductText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Column(modifier = Modifier.weight(1.8f)) {
            Text(product.sku, color = ProductText, fontSize = 14.sp)
            Text(product.categoryName, color = ProductMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
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
    HorizontalDivider(color = ProductLine)
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

    val controls: @Composable () -> Unit = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PricePageButton(Icons.Default.ChevronLeft, safePage > 1, onPreviousPage)
        Text("$safePage / $safeTotalPages", color = ProductText, fontWeight = FontWeight.Bold)
        PricePageButton(Icons.Default.ChevronRight, safePage < safeTotalPages, onNextPage)
    } }
    if (compact) Column(Modifier.fillMaxWidth().background(ProductSoft).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${state.totalProducts} produk", color = ProductMuted, fontSize = 12.sp)
        controls()
    } else Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProductSoft.copy(alpha = 0.72f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(rangeText, color = ProductText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "Maksimal ${state.pageSize} produk per halaman",
                color = ProductMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        controls()
    }
}

@Composable
private fun PricePageButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProductLine),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) ProductText else ProductMuted.copy(alpha = 0.35f))
    }
}
