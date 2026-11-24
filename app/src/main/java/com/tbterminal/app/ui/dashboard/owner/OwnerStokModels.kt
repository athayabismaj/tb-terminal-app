package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.runtime.Immutable

enum class StokActionId {
    ADD_PRODUCT,
    PRODUCTS_STOCK,
    PRODUCT_PRICES,
    STOCK_CARD,
    ADJUST_STOCK,
    CATEGORIES,
    UNITS
}

@Immutable
data class StokUiState(
    val userName: String = "Pemilik Toko",
    val synced: Boolean = true,
    val totalAssetValuation: Double = 0.0,
    val totalProductsCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalCategoriesCount: Int = 0,
    val totalUnitsCount: Int = 0,
    val syncStatusText: String = "Tersinkron",
    val isSyncHealthy: Boolean = true,
    val balanceHidden: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val selectedNav: DashboardNavDestination = DashboardNavDestination.STOK
)

internal fun sampleReferenceStokState(): StokUiState = StokUiState(
    totalAssetValuation = 18_640_000.0,
    totalProductsCount = 128,
    lowStockCount = 3,
    totalCategoriesCount = 12,
    totalUnitsCount = 13,
    syncStatusText = "Tersinkron",
    isSyncHealthy = true,
    balanceHidden = false,
    loading = false,
    error = null,
    selectedNav = DashboardNavDestination.STOK
)
