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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Straighten
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
import com.tbterminal.app.ui.components.TbPagination
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS DESIGN TOKENS (TB Terminal Design System)
// Harmonized with Forest Green (#10533C), Slate 900/800/500, Emerald, and Amber
// ═════════════════════════════════════════════════════════════════════════════
private val BrandForest = Color(0xFF10533C)
private val BrandForestDark = Color(0xFF0A3B2B)
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

private val Emerald800 = Color(0xFF065F46)
private val Emerald600 = Color(0xFF059669)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald50 = Color(0xFFECFDF5)

private val Amber800 = Color(0xFF92400E)
private val Amber600 = Color(0xFFD97706)
private val Amber200 = Color(0xFFFDE68A)
private val Amber50 = Color(0xFFFFFBEB)

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

    // 155.dp adapts into exactly 2 columns on 360-420dp mobile phones (grid-cols-2),
    // and responsive 3-4 columns on tablet / wider screens
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 155.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 88.dp) // Spacing for floating bottom cart dock
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

/**
 * Modern Industrial Product Card matching the POS HTML Mockup.
 * Features:
 * - 2-Column responsive touch grid compatibility
 * - Aspect [4/3] visual banner with centered material category icon
 * - Floating stock pill in bottom-right corner of visual banner
 * - Uppercase tracking-wider category header
 * - Single-line bold product title
 * - Price & Unit layout (Rp 45.000 /Pcs)
 * - Dynamic bottom action: Circular brand green '+' button when cartQty == 0,
 *   full-width pill stepper with '-' & '+' when cartQty > 0, disabled state when out-of-stock.
 */
@Composable
internal fun ProductMinimalCard(
    product: Product,
    cartQty: Int,
    onAddClick: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stockQty <= BigDecimal.ZERO
    val isInCart = cartQty > 0
    val onCardClick = if (cartQty > 0) onIncreaseQty else onAddClick

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isOutOfStock, onClick = onCardClick),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isInCart) BrandForest.copy(alpha = 0.5f) else Slate200.copy(alpha = 0.8f)
        ),
        shadowElevation = if (isInCart) 1.5.dp else 0.5.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── TOP: Aspect [4/3] Image / Product Visual Banner with Floating Stock Badge ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                // Centered subtle category icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.8f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(product.categoryName),
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Floating Stock Pill Badge at Bottom Right (like HTML mockup)
                FloatingStockBadge(
                    stockQty = product.stockQty,
                    isOutOfStock = isOutOfStock,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                )
            }

            // ── DETAILS: Category, Title, Secondary unit, Price, & Action ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Category Name (uppercase tracking-wider text-slate-400)
                Text(
                    text = product.categoryName.ifBlank { "Umum" }.uppercase(),
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Product Title (font-semibold text-slate-900 text-sm leading-snug line-clamp-1)
                Text(
                    text = product.name,
                    color = Slate900,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Secondary Unit Conversion (if configured)
                if (product.secondaryUnitFactor != null && product.secondaryUnitFactor > BigDecimal.ONE) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "1 ${product.secondaryUnitName ?: "Satuan 2"} = ${product.secondaryUnitFactor.stripTrailingZeros().toPlainString()} ${product.unitName}",
                        color = BrandForest,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price and Unit
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = product.unitPrice.moneyText(),
                        color = Slate900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    if (product.unitName.isNotBlank()) {
                        Text(
                            text = "/${product.unitName}",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action: Out-of-Stock / Stepper (In Cart) / Circular Plus Button (Not In Cart)
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

/**
 * Floating Stock Pill Badge positioned on bottom-right of product thumbnail.
 * Renders pulse-like dot + text (Stok N / Sisa N / Habis).
 */
@Composable
private fun FloatingStockBadge(
    stockQty: BigDecimal,
    isOutOfStock: Boolean,
    modifier: Modifier = Modifier
) {
    val isLowStock = !isOutOfStock && stockQty <= BigDecimal(5)
    val stockBorder = when {
        isOutOfStock -> DangerBorder
        isLowStock -> Amber200
        else -> Emerald200.copy(alpha = 0.8f)
    }
    val stockDotColor = when {
        isOutOfStock -> DangerRed
        isLowStock -> Amber600
        else -> Emerald600
    }
    val stockTextColor = when {
        isOutOfStock -> DangerRed
        isLowStock -> Amber800
        else -> Emerald800
    }
    val stockText = when {
        isOutOfStock -> "Habis"
        isLowStock -> "Sisa ${stockQty.qtyText()}"
        else -> "Stok ${stockQty.qtyText()}"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = Color.White.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, stockBorder),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(stockDotColor)
            )
            Text(
                text = stockText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = stockTextColor
            )
        }
    }
}

/**
 * Action button inside the Product Card.
 * When in cart: full-width pill stepper with '-' & '+' buttons.
 * When not in cart: circular brand '+' button aligned right.
 * When out of stock: full-width disabled badge.
 */
@Composable
private fun ProductCardAction(
    cartQty: Int,
    isOutOfStock: Boolean,
    onAddClick: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit
) {
    when {
        isOutOfStock -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(100.dp),
                color = Slate100
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Stok Habis",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        cartQty > 0 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kurangi button: circular white with subtle border
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Slate200, CircleShape)
                        .clickable(onClick = onDecreaseQty),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (cartQty == 1) Icons.Outlined.Delete else Icons.Outlined.Remove,
                        contentDescription = "Kurangi kuantitas",
                        tint = if (cartQty == 1) DangerRed else Slate700,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = cartQty.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                // Tambah button: circular brand green
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BrandForest)
                        .clickable(onClick = onIncreaseQty),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Tambah kuantitas",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandForest)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Tambah ke keranjang",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Returns a thematic Material icon based on the product category name.
 */
private fun getCategoryIcon(categoryName: String): ImageVector {
    val lower = categoryName.lowercase()
    return when {
        lower.contains("besi") || lower.contains("baja") || lower.contains("galvalum") -> Icons.Outlined.Hardware
        lower.contains("semen") || lower.contains("mortar") -> Icons.Outlined.Layers
        lower.contains("cat") || lower.contains("thinner") || lower.contains("kuas") -> Icons.Outlined.FormatPaint
        lower.contains("kayu") || lower.contains("triplek") || lower.contains("papan") -> Icons.Outlined.Straighten
        lower.contains("pasir") || lower.contains("batu") || lower.contains("bata") || lower.contains("hebel") -> Icons.Outlined.Landscape
        lower.contains("alat") || lower.contains("pertukangan") -> Icons.Outlined.Construction
        else -> Icons.Outlined.Inventory2
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
