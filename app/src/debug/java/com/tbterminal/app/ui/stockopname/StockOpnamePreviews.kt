package com.tbterminal.app.ui.stockopname

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

@Preview(name = "Sesuaikan Stok - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Sesuaikan Stok - Phone Landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Preview(name = "Sesuaikan Stok - Font Besar", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Composable
private fun StockAdjustmentListPreview() {
    PreviewShell {
        StockOpnameListScreen(
            modifier = it,
            uiState = previewStockAdjustmentState(),
            onSearchChanged = {},
            onCategoryFilterChanged = {},
            onOpenForm = {},
            onSelectProduct = {},
            onPreviousPage = {},
            onNextPage = {},
            onDismissMessage = {},
        )
    }
}

@Preview(name = "Form Stok - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Form Stok - Tablet Landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun StockAdjustmentFormPreview() {
    PreviewShell {
        StockOpnameFormScreen(
            modifier = it,
            uiState = previewStockAdjustmentState(),
            onSelectProduct = {},
            onActualQtyChanged = {},
            onNotesChanged = {},
            onOpeningDateChanged = {},
            onAdjustmentTypeChanged = {},
            onSubmit = {},
            onDismissMessage = {},
        )
    }
}

@Composable
private fun PreviewShell(content: @Composable (androidx.compose.ui.Modifier) -> Unit) {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Sesuaikan stok",
            onBack = {},
            content = content,
        )
    }
}

internal fun previewStockAdjustmentState(): StockOpnameUiState {
    val products = listOf(
        previewStockProduct("product-1", "SEMEN-001", "Semen Portland 50 Kg", "24", "sak"),
        previewStockProduct("product-2", "CAT-001", "Cat Tembok Putih 5 Kg", "7", "kaleng"),
    )
    return StockOpnameUiState(
        products = products,
        selectedProduct = products.first(),
        actualQtyInput = "22",
        notesInput = "Hasil hitung fisik gudang utama",
        adjustmentType = StockAdjustmentType.OPNAME,
    )
}

private fun previewStockProduct(
    id: String,
    sku: String,
    name: String,
    quantity: String,
    unit: String,
) = ProductStock(
    productId = id,
    sku = sku,
    productName = name,
    categoryName = "Bahan Bangunan",
    unitName = unit,
    quantity = BigDecimal(quantity),
    minStock = BigDecimal("10"),
    priceBuy = BigDecimal("62000"),
    priceRetail = BigDecimal("72000"),
    priceContractor = BigDecimal("67000"),
    discount = BigDecimal.ZERO,
    isActive = true,
)
