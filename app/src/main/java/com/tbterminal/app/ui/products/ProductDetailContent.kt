package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun ProductDetailContent(
    modifier: Modifier,
    uiState: ProductDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onEditProductClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProductBackground)
            .verticalScroll(rememberScrollState())
            .padding(40.dp)
    ) {
        ProductDetailHeader(
            onBack = onBack,
            onEdit = uiState.detail?.let { detail ->
                { onEditProductClick(detail.product.id) }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        when {
            uiState.isLoading -> ProductLoadingState(modifier = Modifier.height(260.dp))
            uiState.errorMessage != null -> ProductErrorState(uiState.errorMessage, onRetry = onRetry)
            uiState.detail != null -> ProductDetailCards(detail = uiState.detail)
        }
    }
}

@Composable
private fun ProductDetailHeader(
    onBack: () -> Unit,
    onEdit: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Detail Produk",
            modifier = Modifier.weight(1f),
            color = ProductText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Kembali", color = ProductMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        if (onEdit != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onEdit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProductPrimaryDark,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Produk", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ProductDetailCards(detail: ProductDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProductMasterInfoCard(
            detail = detail,
            modifier = Modifier
                .weight(1.45f)
                .heightIn(min = 372.dp)
        )
        ProductStockPositionCard(
            stock = detail.stock,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 314.dp)
        )
    }
}

@Composable
private fun ProductMasterInfoCard(
    detail: ProductDetail,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        border = BorderStroke(1.dp, ProductLine)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(detail.product.name, color = ProductText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("SKU ${detail.product.sku}", color = ProductMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = ProductLine)
            Spacer(modifier = Modifier.height(10.dp))
            ProductDetailRow("Harga beli", detail.product.priceBuy.moneyText())
            ProductDetailRow("Harga retail", detail.product.priceRetail.moneyText())
            ProductDetailRow("Harga kontraktor", detail.product.priceContractor.moneyText())
            ProductDetailRow("Stok minimum", detail.product.minStock.quantityText())
            ProductDetailRow("Status", if (detail.product.isActive) "Aktif" else "Nonaktif")
        }
    }
}

@Composable
private fun ProductStockPositionCard(
    stock: ProductStock?,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        border = BorderStroke(1.dp, ProductLine)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text("Posisi Stok", color = ProductText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            if (stock == null) {
                Text("Data stok belum tersedia.", color = ProductMuted, fontSize = 14.sp)
            } else {
                ProductDetailRow("Kategori", stock.categoryName)
                ProductDetailRow("Satuan", stock.unitName)
                ProductDetailRow("Quantity", stock.quantity.quantityText())
                ProductDetailRow("Minimum", stock.minStock.quantityText())
                ProductStockConditionRow(stock)
            }
        }
    }
}

@Composable
private fun ProductStockConditionRow(stock: ProductStock) {
    val needsRestock = stock.quantity <= stock.minStock
    val background = if (needsRestock) ProductDanger.copy(alpha = 0.1f) else ProductPrimary.copy(alpha = 0.12f)
    val content = if (needsRestock) ProductDanger else ProductPrimaryDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Kondisi", modifier = Modifier.weight(1f), color = ProductMuted, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .background(background, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (needsRestock) "Perlu restock" else "Aman",
                color = content,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProductDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = ProductMuted, fontSize = 14.sp)
        Text(value, color = ProductText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
