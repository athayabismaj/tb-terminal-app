package com.tbterminal.app.ui.products

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Tambah Produk - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Tambah Produk - Phone Landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Preview(name = "Tambah Produk - Font Besar", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Composable
private fun AddProductPreview() {
    ProductFormPreview(isEditMode = false)
}

@Preview(name = "Edit Produk - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Edit Produk - Tablet Landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun EditProductPreview() {
    ProductFormPreview(isEditMode = true)
}

@Composable
private fun ProductFormPreview(isEditMode: Boolean) {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = if (isEditMode) "Edit produk" else "Tambah produk",
            onBack = {},
        ) { contentModifier ->
            ProductFormContent(
                modifier = contentModifier,
                uiState = productFormPreviewState(isEditMode),
                onInputChanged = {},
                onSave = {},
                onCancel = {},
            )
        }
    }
}

internal fun productFormPreviewState(isEditMode: Boolean = false) = ProductFormUiState(
    input = ProductFormInput(
        sku = if (isEditMode) "SEMEN-001" else "",
        name = if (isEditMode) "Semen Portland 50 Kg" else "",
        categoryId = "category-1",
        baseUnitId = "unit-1",
        usesSecondaryUnit = true,
        secondaryUnitId = "unit-2",
        secondaryUnitFactor = "40",
        priceBuy = if (isEditMode) "62000" else "",
        priceRetail = if (isEditMode) "72000" else "",
        priceContractor = if (isEditMode) "67000" else "",
        minStock = if (isEditMode) "10" else "",
    ),
    categories = listOf(ProductCategory("category-1", "Bahan Bangunan", "2026-01-01T08:00:00Z")),
    units = listOf(
        ProductUnit("unit-1", "Sak", "sak", "2026-01-01T08:00:00Z"),
        ProductUnit("unit-2", "Kilogram", "kg", "2026-01-01T08:00:00Z"),
    ),
    isEditMode = isEditMode,
    isLoading = false,
)
