package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun ProductDetailContent(
    modifier: Modifier,
    uiState: ProductDetailUiState,
    onRetry: () -> Unit,
    onEditProductClick: (String) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(ProductBackground).testTag("product-detail-screen")) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 1120.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp),
        ) {
            val detail = uiState.detail
            when {
                uiState.isLoading -> ProductLoadingState(modifier = Modifier.height(260.dp))
                uiState.errorMessage != null -> ProductErrorState(uiState.errorMessage, onRetry = onRetry)
                detail != null -> ProductDetailCards(
                    detail = detail,
                    compact = compact,
                    onEdit = { onEditProductClick(detail.product.id) },
                )
            }
        }
    }
}

@Composable
private fun ProductDetailCards(detail: ProductDetail, compact: Boolean, onEdit: () -> Unit) {
    ProductIdentityCard(detail = detail, onEdit = onEdit)
    if (compact) {
        ProductPricesCard(detail = detail, modifier = Modifier.fillMaxWidth())
        ProductStockCard(detail = detail, modifier = Modifier.fillMaxWidth())
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            ProductPricesCard(detail = detail, modifier = Modifier.weight(1f))
            ProductStockCard(detail = detail, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProductIdentityCard(detail: ProductDetail, onEdit: () -> Unit) {
    ProductDetailCard(modifier = Modifier.fillMaxWidth().testTag("product-detail-identity")) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    detail.product.name,
                    color = ProductText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(detail.product.sku, detail.stock?.categoryName?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    color = ProductMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp).testTag("product-detail-edit")) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit produk", tint = ProductPrimaryDark, modifier = Modifier.size(20.dp))
            }
        }
        ProductDetailBadge(
            label = if (detail.product.isActive) "Aktif" else "Nonaktif",
            color = if (detail.product.isActive) ProductPrimaryDark else ProductDanger,
        )
    }
}

@Composable
private fun ProductPricesCard(detail: ProductDetail, modifier: Modifier) {
    ProductDetailCard(modifier = modifier.testTag("product-detail-prices")) {
        Text("Harga", color = ProductText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Harga jual", color = ProductMuted, style = MaterialTheme.typography.bodySmall)
            Text(
                detail.product.priceRetail.moneyText(),
                color = ProductPrimaryDark,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = ProductLine.copy(alpha = 0.58f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProductDetailMetric("Harga beli", detail.product.priceBuy.moneyText(), Modifier.weight(1f))
            ProductDetailMetric("Kontraktor", detail.product.priceContractor.moneyText(), Modifier.weight(1f), alignment = Alignment.End)
        }
        if (detail.product.discount.signum() > 0) {
            ProductDetailPair("Diskon", detail.product.discount.moneyText(), ProductDanger)
        }
    }
}

@Composable
private fun ProductStockCard(detail: ProductDetail, modifier: Modifier) {
    val stock = detail.stock
    ProductDetailCard(modifier = modifier.testTag("product-detail-stock")) {
        Text("Stok", color = ProductText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (stock == null) {
            Text("Data stok belum tersedia.", color = ProductMuted, style = MaterialTheme.typography.bodyMedium)
            ProductDetailPair("Stok minimum", detail.product.minStock.quantityText())
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProductDetailMetric(
                    "Saat ini",
                    "${stock.quantity.quantityText()} ${stock.unitName}",
                    Modifier.weight(1f),
                    if (stock.quantity <= stock.minStock) ProductDanger else ProductText,
                )
                ProductDetailMetric(
                    "Minimum",
                    "${stock.minStock.quantityText()} ${stock.unitName}",
                    Modifier.weight(1f),
                    alignment = Alignment.End,
                )
            }
            val needsRestock = stock.quantity <= stock.minStock
            ProductDetailBadge(
                label = if (needsRestock) "Perlu restock" else "Stok aman",
                color = if (needsRestock) ProductDanger else ProductPrimaryDark,
            )
            HorizontalDivider(color = ProductLine.copy(alpha = 0.58f))
            ProductDetailPair("Kategori", stock.categoryName)
            ProductDetailPair("Satuan", stock.unitName)
        }
    }
}

@Composable
private fun ProductDetailCard(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.72f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun ProductDetailMetric(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = ProductText,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = ProductMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductDetailPair(label: String, value: String, valueColor: Color = ProductText) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = ProductMuted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProductDetailBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(999.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
