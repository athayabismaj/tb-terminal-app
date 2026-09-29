package com.tbterminal.app.ui.products

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

@Preview(name = "Produk - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Produk - Phone Landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Preview(name = "Produk - Font Besar", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Preview(name = "Produk - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Produk - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun ProductListPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Produk",
        ) { contentModifier ->
            ProductListContent(
                modifier = contentModifier,
                uiState = productPreviewState(),
                onSearchChanged = {},
                onCategorySelected = {},
                onRetry = {},
                onAddProductClick = {},
                onImportProductClick = {},
                onEditProductClick = {},
                onProductDetailClick = {},
                onCategoriesClick = {},
                onUnitsClick = {},
                onToggleProductClick = {},
                onPreviousPage = {},
                onNextPage = {},
                onDismissMessage = {},
            )
        }
    }
}

internal fun productPreviewState() = ProductListUiState(
    products = listOf(
        previewProduct("product-1", "SEMEN-001", "Semen Portland 50 Kg", "Bahan Bangunan", "sak", "24", "10", "72000"),
        previewProduct("product-2", "CAT-001", "Cat Tembok Putih 5 Kg", "Cat", "kaleng", "7", "8", "145000"),
    ),
    categories = listOf(
        ProductCategory("category-1", "Bahan Bangunan", "2026-01-01T08:00:00Z"),
        ProductCategory("category-2", "Cat", "2026-01-01T08:00:00Z"),
    ),
    page = 1,
    totalPages = 3,
    totalProducts = 24,
)

private fun previewProduct(
    id: String,
    sku: String,
    name: String,
    category: String,
    unit: String,
    quantity: String,
    minStock: String,
    retailPrice: String,
) = ProductStock(
    productId = id,
    sku = sku,
    productName = name,
    categoryName = category,
    unitName = unit,
    quantity = BigDecimal(quantity),
    minStock = BigDecimal(minStock),
    priceBuy = BigDecimal(retailPrice).subtract(BigDecimal("10000")),
    priceRetail = BigDecimal(retailPrice),
    priceContractor = BigDecimal(retailPrice).subtract(BigDecimal("5000")),
    discount = BigDecimal.ZERO,
    isActive = true,
)
