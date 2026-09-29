package com.tbterminal.app.ui.products

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Kategori - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Kategori - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun CategoryListPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Kategori produk",
            onBack = {},
        ) { modifier -> CategoryPreviewContent(modifier, categoryPreviewState()) }
    }
}

@Preview(name = "Tambah Kategori - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Tambah Kategori - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun CategoryFormPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Tambah kategori",
            onBack = {},
        ) { modifier -> CategoryPreviewContent(modifier, ProductCategoryUiState(isLoading = false, isFormVisible = true)) }
    }
}

@Preview(name = "Satuan - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Satuan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun UnitListPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Satuan produk",
            onBack = {},
        ) { modifier -> UnitPreviewContent(modifier, unitPreviewState()) }
    }
}

@Preview(name = "Tambah Satuan - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Tambah Satuan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun UnitFormPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.STOCK,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Tambah satuan",
            onBack = {},
        ) { modifier -> UnitPreviewContent(modifier, ProductUnitUiState(isLoading = false, isFormVisible = true)) }
    }
}

@Composable
private fun CategoryPreviewContent(modifier: Modifier, state: ProductCategoryUiState) {
    ProductCategoryContent(modifier, state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}

@Composable
private fun UnitPreviewContent(modifier: Modifier, state: ProductUnitUiState) {
    ProductUnitContent(modifier, state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}

internal fun categoryPreviewState() = ProductCategoryUiState(
    categories = listOf(
        ProductCategory("category-1", "Bahan bangunan", "2026-09-01T08:00:00Z"),
        ProductCategory("category-2", "Cat dan pelapis", "2026-09-02T08:00:00Z"),
        ProductCategory("category-3", "Peralatan", "2026-09-03T08:00:00Z"),
    ),
    totalCategories = 23,
    totalPages = 3,
    isLoading = false,
)

internal fun unitPreviewState() = ProductUnitUiState(
    units = listOf(
        ProductUnit("unit-1", "Buah", "pcs", "2026-09-01T08:00:00Z"),
        ProductUnit("unit-2", "Kilogram", "kg", "2026-09-02T08:00:00Z"),
        ProductUnit("unit-3", "Dus", "dus", "2026-09-03T08:00:00Z"),
    ),
    totalUnits = 18,
    totalPages = 2,
    isLoading = false,
)
