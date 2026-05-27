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
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ProductHeader(
            title = "Detail Produk",
            subtitle = "Ringkasan master produk, harga, dan posisi stok terakhir.",
            actions = {
                TextButton(onClick = onBack) { Text("Kembali") }
                Spacer(modifier = Modifier.width(12.dp))
                uiState.detail?.let { detail ->
                    Button(
                        onClick = { onEditProductClick(detail.product.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ProductPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Produk")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> ProductLoadingState(modifier = Modifier.height(260.dp))
            uiState.errorMessage != null -> ProductErrorState(uiState.errorMessage, onRetry = onRetry)
            uiState.detail != null -> ProductDetailCard(uiState.detail)
        }
    }
}

@Composable
internal fun ProductDetailCard(detail: ProductDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Card(
            modifier = Modifier.weight(1.4f),
            colors = CardDefaults.cardColors(containerColor = ProductSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ProductLine)
        ) {
            Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(detail.product.name, color = ProductText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("SKU ${detail.product.sku}", color = ProductMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(color = ProductLine)
                ProductInfoRow("Harga beli", detail.product.priceBuy.moneyText())
                ProductInfoRow("Harga retail", detail.product.priceRetail.moneyText())
                ProductInfoRow("Harga kontraktor", detail.product.priceContractor.moneyText())
                ProductInfoRow("Stok minimum", detail.product.minStock.quantityText())
                ProductInfoRow("Status", if (detail.product.isActive) "Aktif" else "Nonaktif")
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .height(320.dp),
            colors = CardDefaults.cardColors(containerColor = ProductSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ProductLine)
        ) {
            Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Posisi Stok", color = ProductText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                val stock = detail.stock
                if (stock == null) {
                    Text("Data stok belum tersedia.", color = ProductMuted)
                } else {
                    ProductInfoRow("Kategori", stock.categoryName)
                    ProductInfoRow("Satuan", stock.unitName)
                    ProductInfoRow("Quantity", stock.quantity.quantityText())
                    ProductInfoRow("Minimum", stock.minStock.quantityText())
                    ProductInfoRow(
                        "Kondisi",
                        if (stock.quantity <= stock.minStock) "Perlu restock" else "Aman"
                    )
                }
            }
        }
    }
}

