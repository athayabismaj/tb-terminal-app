package com.tbterminal.app.ui.checkout.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.CartItem
import com.tbterminal.app.ui.checkout.Error
import com.tbterminal.app.ui.checkout.ErrorContainer
import com.tbterminal.app.ui.checkout.OnSecondaryContainer
import com.tbterminal.app.ui.checkout.OnSurface
import com.tbterminal.app.ui.checkout.OnSurfaceVariant
import com.tbterminal.app.ui.checkout.Outline
import com.tbterminal.app.ui.checkout.OutlineVariant
import com.tbterminal.app.ui.checkout.Primary
import com.tbterminal.app.ui.checkout.Product
import com.tbterminal.app.ui.checkout.SecondaryContainer
import com.tbterminal.app.ui.checkout.SurfaceContainerLow
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun ProductCardGrid(
    products: List<Product>,
    cartItems: List<CartItem>,
    onProductClick: (Product) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        ProductGridEmptyState(modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(products, key = { product -> product.productId }) { product ->
            val cartItem = cartItems.firstOrNull { item -> item.productId == product.productId }
            ProductMinimalCard(
                product = product,
                cartQty = cartItem?.quantity ?: 0,
                onAddClick = { onProductClick(product) },
                onDecreaseQty = {
                    if (cartItem != null) {
                        onUpdateQty(cartItem.cartItemId, cartItem.quantity - 1)
                    }
                },
                onIncreaseQty = {
                    if (cartItem != null) {
                        onUpdateQty(cartItem.cartItemId, cartItem.quantity + 1)
                    } else {
                        onProductClick(product)
                    }
                }
            )
        }
    }
}

@Composable
internal fun ProductPaginationBar(
    page: Int,
    totalPages: Int,
    totalItems: Long,
    limit: Int,
    isLoading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safePage = page.coerceAtLeast(1)
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val firstItem = if (totalItems == 0L) 0L else ((safePage.toLong() - 1L) * limit) + 1L
    val lastItem = if (totalItems == 0L) 0L else minOf(safePage.toLong() * limit, totalItems)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (totalItems == 0L) {
                    "Tidak ada produk"
                } else {
                    "Menampilkan $firstItem-$lastItem dari $totalItems produk"
                },
                color = OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = safePage > 1 && !isLoading,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Sebelumnya", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = SurfaceContainerLow,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Halaman $safePage/$safeTotalPages",
                        color = OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Button(
                    onClick = onNext,
                    enabled = safePage < safeTotalPages && !isLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Berikutnya", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProductErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = Error, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat ulang")
        }
    }
}

@Composable
private fun ProductGridEmptyState(modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tidak ada produk pada kategori ini.",
                color = Outline,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProductMinimalCard(
    product: Product,
    cartQty: Int,
    onAddClick: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit
) {
    val isOutOfStock = product.stockQty <= BigDecimal.ZERO
    val onCardClick = if (cartQty > 0) onIncreaseQty else onAddClick

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(enabled = !isOutOfStock, onClick = onCardClick),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.42f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.categoryName.ifBlank { product.unitName.ifBlank { "Tanpa kategori" } },
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SKU: ${product.sku.ifBlank { "-" }}",
                        color = Outline,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StockPill(
                    stockQty = product.stockQty,
                    isOutOfStock = isOutOfStock
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column {
                Text(
                    text = product.unitPrice.moneyText(),
                    color = Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductCardAction(
                    cartQty = cartQty,
                    isOutOfStock = isOutOfStock,
                    onAddClick = onAddClick,
                    onDecreaseQty = onDecreaseQty,
                    onIncreaseQty = onIncreaseQty
                )
            }
        }
    }
}

@Composable
private fun StockPill(
    stockQty: BigDecimal,
    isOutOfStock: Boolean,
    modifier: Modifier = Modifier
) {
    val isLowStock = !isOutOfStock && stockQty <= BigDecimal(5)
    val backgroundColor = when {
        isOutOfStock -> ErrorContainer
        isLowStock -> SecondaryContainer.copy(alpha = 0.18f)
        else -> Primary.copy(alpha = 0.1f)
    }
    val textColor = when {
        isOutOfStock -> Error
        isLowStock -> OnSecondaryContainer
        else -> Primary
    }
    val text = when {
        isOutOfStock -> "Habis"
        isLowStock -> "Tipis: ${stockQty.qtyText()}"
        else -> stockQty.qtyText()
    }

    Box(modifier = modifier) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ProductCardAction(
    cartQty: Int,
    isOutOfStock: Boolean,
    onAddClick: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when {
            isOutOfStock -> Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Habis", color = Outline, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            cartQty > 0 -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(SurfaceContainerLow, RoundedCornerShape(10.dp))
                    .border(1.dp, OutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuantityActionButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = "Kurangi",
                    onClick = onDecreaseQty
                )
                Text(
                    text = cartQty.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                QuantityActionButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "Tambah",
                    onClick = onIncreaseQty,
                    containerColor = Primary.copy(alpha = 0.1f),
                    borderColor = Color.Transparent
                )
            }

            else -> Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Keranjang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun QuantityActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    borderColor: Color = OutlineVariant.copy(alpha = 0.35f)
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Primary,
            modifier = Modifier.size(17.dp)
        )
    }
}

private fun BigDecimal.moneyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

private fun BigDecimal.qtyText(): String {
    return stripTrailingZeros().toPlainString()
}
