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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onRefresh: () -> Unit,
    onSelectProduct: (ProductStock) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = IncomingSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IncomingLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProductToolbar(uiState.productSearchQuery, onSearchChanged, onRefresh)
            HorizontalDivider(color = IncomingLine)
            ProductHeader()
            HorizontalDivider(color = IncomingLine)
            ProductRows(uiState, onSelectProduct)
        }
    }
}

@Composable
private fun ProductToolbar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari produk atau SKU...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IncomingTextFieldColors()
        )
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(IncomingSoft)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Muat ulang", tint = IncomingPrimaryDark)
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
        uiState.isLoadingProducts -> LoadingState()
        uiState.products.isEmpty() -> EmptyState("Tidak ada produk aktif yang cocok.")
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = uiState.products, key = ProductStock::productId) { product ->
                ProductRow(
                    product = product,
                    isSelected = product.productId == uiState.selectedProduct?.productId,
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) IncomingPrimary.copy(alpha = 0.08f) else Color.Transparent)
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
