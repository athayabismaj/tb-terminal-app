package com.tbterminal.app.ui.checkout.components

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.TbPagination
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.CartItem
import com.tbterminal.app.ui.checkout.Product
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS DESIGN TOKENS (TB Terminal Design System)
// Harmonized with Forest Green, Slate 900/800/500, Emerald, and Amber
// ═════════════════════════════════════════════════════════════════════════════
private val BrandForest = Color(0xFF10533C)
private val BrandForestDark = Color(0xFF0B3828)
private val BrandGreenLight = Color(0xFFF0FDF4)
private val BrandGreenBorder = Color(0xFFBBF7D0)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val Emerald700 = Color(0xFF15803D)
private val Emerald50 = Color(0xFFECFDF5)

private val Amber700 = Color(0xFFB45309)
private val AmberBg = Color(0xFFFEF3C7)

private val DangerRed = Color(0xFFDC2626)
private val DangerBg = Color(0xFFFEF2F2)
private val DangerBorder = Color(0xFFFECACA)

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

    // 155.dp adapts into exactly 2 columns on 360-420dp mobile phones,
    // and 3-4 columns on wider tablets/desktops
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 155.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Spacing for bottom cart dock
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

    TbPagination(
        currentPage = safePage,
        totalPages = safeTotalPages,
        onPreviousPage = onPrevious,
        onNextPage = onNext,
        modifier = modifier.padding(vertical = 4.dp),
        supportingText = if (totalItems == 0L) "Tidak ada produk"
        else "$firstItem-$lastItem dari $totalItems produk",
        isLoading = isLoading,
        testTag = "pos-product-pagination",
    )
}

@Composable
internal fun ProductErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.tbterminal.app.ui.components.AppErrorState(
        message = message,
        onRetry = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun ProductGridEmptyState(modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Tidak Ada Produk",
                    color = Slate900,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tidak ada produk pada kategori atau pencarian ini.",
                    color = Slate500,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
internal fun ProductMinimalCard(
    product: Product,
    cartQty: Int,
    onAddClick: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit
) {
    val isOutOfStock = product.stockQty <= BigDecimal.ZERO
    val onCardClick = if (cartQty > 0) onIncreaseQty else onAddClick
    val isInCart = cartQty > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock, onClick = onCardClick),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isInCart) BrandForest else Slate200
        ),
        shadowElevation = if (isInCart) 2.dp else 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Category & Stock Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.categoryName.ifBlank { "Umum" },
                    color = Slate500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                StockPill(
                    stockQty = product.stockQty,
                    isOutOfStock = isOutOfStock
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Product Name
            Text(
                text = product.name,
                color = Slate900,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(34.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Row 3: SKU / Secondary Unit
            if (product.secondaryUnitFactor != null && product.secondaryUnitFactor > BigDecimal.ONE) {
                Surface(
                    color = BrandGreenLight,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, BrandGreenBorder)
                ) {
                    Text(
                        text = "1 ${product.secondaryUnitName ?: "Satuan 2"} = ${product.secondaryUnitFactor.stripTrailingZeros().toPlainString()} ${product.unitName}",
                        color = BrandForest,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "SKU: ${product.sku.ifBlank { "-" }}",
                    color = Slate400,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Price & Unit
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = product.unitPrice.moneyText(),
                    color = BrandForest,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.unitName.isNotBlank()) {
                    Text(
                        text = "/ ${product.unitName}",
                        color = Slate500,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 2.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 5: Action Button / Qty Stepper
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

@Composable
private fun StockPill(
    stockQty: BigDecimal,
    isOutOfStock: Boolean,
    modifier: Modifier = Modifier
) {
    val isLowStock = !isOutOfStock && stockQty <= BigDecimal(5)
    val backgroundColor = when {
        isOutOfStock -> DangerBg
        isLowStock -> AmberBg
        else -> Emerald50
    }
    val textColor = when {
        isOutOfStock -> DangerRed
        isLowStock -> Amber700
        else -> Emerald700
    }
    val text = when {
        isOutOfStock -> "Habis"
        isLowStock -> "Tipis: ${stockQty.qtyText()}"
        else -> "Stok ${stockQty.qtyText()}"
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
    Box(modifier = Modifier.fillMaxWidth()) {
        when {
            isOutOfStock -> {
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stok Habis",
                            color = Slate400,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            cartQty > 0 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(BrandGreenLight, RoundedCornerShape(8.dp))
                        .border(1.dp, BrandGreenBorder, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuantityActionButton(
                        icon = if (cartQty == 1) Icons.Outlined.Delete else Icons.Outlined.Remove,
                        contentDescription = "Kurangi",
                        onClick = onDecreaseQty,
                        tint = if (cartQty == 1) DangerRed else BrandForest
                    )
                    Text(
                        text = cartQty.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = BrandForest
                    )
                    QuantityActionButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Tambah",
                        onClick = onIncreaseQty,
                        containerColor = BrandForest,
                        tint = Color.White
                    )
                }
            }

            else -> {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandForest,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tambah",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color = Color.White,
    tint: Color = BrandForest
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(0.5.dp, Slate200, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(15.dp)
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
