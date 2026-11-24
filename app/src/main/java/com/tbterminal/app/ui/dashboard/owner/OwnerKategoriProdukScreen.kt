package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.products.ProductCategoryUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.util.Locale

@Immutable
internal data class KategoriSummaryUiState(
    val totalCategories: Long = 0L
)

@Immutable
internal data class KategoriEmptySpec(
    val title: String,
    val description: String,
    val isSearchNotFound: Boolean
)

internal fun isKategoriErrorMessage(message: String?): Boolean = isCatalogErrorMessage(message)

internal fun filterAndSortOwnerCategories(
    categories: List<ProductCategory>,
    searchQuery: String
): List<ProductCategory> {
    val trimmed = searchQuery.trim()
    val filtered = if (trimmed.isEmpty()) {
        categories
    } else {
        categories.filter { category ->
            category.name.contains(trimmed, ignoreCase = true)
        }
    }
    return filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
}

internal fun kategoriEmptyStateSpecFor(searchQuery: String): KategoriEmptySpec {
    val trimmed = searchQuery.trim()
    return if (trimmed.isNotEmpty()) {
        KategoriEmptySpec(
            title = "\"$trimmed\" tidak ditemukan",
            description = "Periksa ejaan atau coba kata kunci lain.",
            isSearchNotFound = true
        )
    } else {
        KategoriEmptySpec(
            title = "Belum ada kategori",
            description = "Tambahkan kategori produk agar barang lebih mudah dikelompokkan.",
            isSearchNotFound = false
        )
    }
}

/**
 * Route Owner untuk halaman Kategori produk, memakai tata letak, tipografi, dan pola infinite
 * scroll yang sama persis dengan halaman Satuan produk.
 */
@Composable
internal fun OwnerKategoriRoute(
    uiState: ProductCategoryUiState,
    isSearchExpanded: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (ProductCategory) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onBack: (() -> Unit)?,
    onStockClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    autoFocusSearch: Boolean = true
) {
    val (navigateBack, handleNavSelect) = rememberCatalogBackHandler(
        onBack = onBack,
        onStockClick = onStockClick,
        onDashboardClick = onDashboardClick,
        activeNavDestination = activeNavDestination
    )

    BackHandler(enabled = isSearchExpanded) { onCloseSearch() }
    BackHandler(enabled = !isSearchExpanded) { navigateBack() }

    val hasError = isKategoriErrorMessage(uiState.message)
    val accumulatedCategories = rememberAccumulatedCatalogItems(
        pageItems = uiState.categories,
        page = uiState.page,
        isLoading = uiState.isLoading,
        hasError = hasError,
        searchQuery = uiState.searchQuery,
        keyOf = ProductCategory::id
    )
    val displayedCategories = remember(accumulatedCategories, uiState.searchQuery) {
        filterAndSortOwnerCategories(
            categories = accumulatedCategories,
            searchQuery = uiState.searchQuery
        )
    }

    val summary = remember(uiState.totalCategories) {
        KategoriSummaryUiState(totalCategories = uiState.totalCategories)
    }

    val hasMorePages = uiState.page < uiState.totalPages
    val footerStatus = when {
        accumulatedCategories.isEmpty() -> PelangganFooterStatus.NONE
        uiState.isLoading -> PelangganFooterStatus.LOADING
        hasError -> PelangganFooterStatus.ERROR
        !hasMorePages -> PelangganFooterStatus.END_OF_LIST
        else -> PelangganFooterStatus.NONE
    }

    val isInitialLoading = uiState.isLoading && accumulatedCategories.isEmpty()
    val isInitialError = hasError && accumulatedCategories.isEmpty() && !uiState.isLoading
    val showEmptyState = !isInitialLoading && !isInitialError && displayedCategories.isEmpty()
    val emptySpec = remember(uiState.searchQuery) { kategoriEmptyStateSpecFor(uiState.searchQuery) }

    Box(modifier = modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && accumulatedCategories.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading || isInitialError || showEmptyState) {
                OwnerKategoriEmptyScreen(
                    summary = summary,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = uiState.searchQuery,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onNavSelect = handleNavSelect,
                    activeNavDestination = activeNavDestination,
                    centerPanelContent = isInitialError || showEmptyState,
                    autoFocusSearch = autoFocusSearch,
                    panelContent = { isTablet ->
                        when {
                            isInitialLoading -> OwnerKategoriLoadingSkeleton(compact = !isTablet)
                            isInitialError -> CatalogErrorPanel(
                                message = uiState.message ?: "Kategori gagal dimuat.",
                                retryTestTag = "kategori-error-retry",
                                onRetry = onRefresh
                            )
                            else -> EmptyState(
                                icon = if (emptySpec.isSearchNotFound) OwnerIcons.Search else OwnerIcons.CategoryShapes,
                                iconContainerColor = if (emptySpec.isSearchNotFound) ChipBg else SurfaceWhite,
                                iconTint = if (emptySpec.isSearchNotFound) Muted else Brand,
                                title = emptySpec.title,
                                description = emptySpec.description,
                                showAddButton = !emptySpec.isSearchNotFound,
                                onAddCustomerClick = onAddCategoryClick,
                                buttonTestTag = "category-add",
                                buttonLabel = "Tambah kategori",
                                modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                            )
                        }
                    }
                )
            } else {
                OwnerKategoriDataScreen(
                    summary = summary,
                    categories = displayedCategories,
                    footerStatus = footerStatus,
                    hasMorePages = hasMorePages,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = uiState.searchQuery,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onAddCategoryClick = onAddCategoryClick,
                    onCategoryClick = onCategoryClick,
                    onLoadMore = onLoadNextPage,
                    onRetryLoadMore = onRetryLoadMore,
                    onNavSelect = handleNavSelect,
                    activeNavDestination = activeNavDestination,
                    autoFocusSearch = autoFocusSearch
                )
            }
        }

        TopFloatingCapsule(
            message = if (!hasError) uiState.message else null,
            error = if (hasError && !isInitialError && accumulatedCategories.isEmpty()) uiState.message else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
        )
    }
}

@Composable
internal fun OwnerKategoriDataScreen(
    summary: KategoriSummaryUiState,
    categories: List<ProductCategory>,
    footerStatus: PelangganFooterStatus,
    hasMorePages: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (ProductCategory) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    autoFocusSearch: Boolean = true
) {
    OwnerCatalogDataScreen(
        items = categories,
        itemKey = ProductCategory::id,
        footerStatus = footerStatus,
        hasMorePages = hasMorePages,
        listTestTag = "owner-kategori-lazy-list",
        footerLoadingText = "Memuat kategori lainnya\u2026",
        footerEndText = "Semua kategori sudah ditampilkan",
        footerTestTag = "kategori-list-footer",
        footerRetryTestTag = "kategori-footer-retry",
        headerBar = {
            CatalogHeaderBar(
                title = "Kategori produk",
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                searchPlaceholder = "Cari kategori...",
                searchContentDescription = "Cari kategori",
                backTestTag = "kategori-back-btn",
                searchToggleTestTag = "category-toggle-search",
                searchBackTestTag = "category-search-back",
                searchInputTestTag = "category-search-input",
                searchClearTestTag = "category-search-clear",
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                autoFocusSearch = autoFocusSearch
            )
        },
        summaryStats = { isTablet -> KategoriSummaryStats(summary = summary, isTablet = isTablet) },
        listHeaderSection = {
            CatalogListHeaderSection(
                addButtonLabel = "Tambah kategori",
                addButtonTestTag = "category-add",
                sectionTitle = "Semua kategori",
                sortLabel = "Urut: Nama A\u2013Z",
                sortTestTag = "kategori-sort-label",
                onAddClick = onAddCategoryClick
            )
        },
        rowContent = { category ->
            CategoryRow(
                category = category,
                onClick = { onCategoryClick(category) }
            )
        },
        onLoadMore = onLoadMore,
        onRetryLoadMore = onRetryLoadMore,
        onNavSelect = onNavSelect,
        modifier = modifier,
        activeNavDestination = activeNavDestination
    )
}

@Composable
internal fun OwnerKategoriEmptyScreen(
    summary: KategoriSummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    centerPanelContent: Boolean,
    panelContent: @Composable (isTablet: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    autoFocusSearch: Boolean = true
) {
    OwnerCatalogEmptyScreen(
        headerBar = {
            CatalogHeaderBar(
                title = "Kategori produk",
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                searchPlaceholder = "Cari kategori...",
                searchContentDescription = "Cari kategori",
                backTestTag = "kategori-back-btn",
                searchToggleTestTag = "category-toggle-search",
                searchBackTestTag = "category-search-back",
                searchInputTestTag = "category-search-input",
                searchClearTestTag = "category-search-clear",
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                autoFocusSearch = autoFocusSearch
            )
        },
        summaryStats = { isTablet -> KategoriSummaryStats(summary = summary, isTablet = isTablet) },
        onNavSelect = onNavSelect,
        centerPanelContent = centerPanelContent,
        panelContent = panelContent,
        modifier = modifier,
        activeNavDestination = activeNavDestination
    )
}

@Composable
internal fun KategoriSummaryStats(
    summary: KategoriSummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    CatalogSummaryStats(
        totalLabel = "Total kategori",
        totalCount = summary.totalCategories,
        unitLabel = "kategori",
        countTestTag = "kategori-total-count",
        breakdownMetrics = emptyList(),
        modifier = modifier,
        isTablet = isTablet
    )
}

@Composable
internal fun CategoryRow(
    category: ProductCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CatalogListRow(
        title = category.name,
        subtitle = "Dibuat ${formatCatalogCreatedDate(category.createdAt)}",
        onClick = onClick,
        testTag = "category-row-${category.id}",
        leading = { CatalogIconCircleBadge(icon = OwnerIcons.CategoryShapes) },
        modifier = modifier
    )
}

@Composable
internal fun OwnerKategoriLoadingSkeleton(compact: Boolean) {
    CatalogLoadingSkeleton(compact = compact, testTag = "category-skeleton")
}

internal fun sampleReferenceKategoriList(): List<ProductCategory> = listOf(
    ProductCategory("k1", "Atap & Genteng", "2026-11-18T10:00:00Z"),
    ProductCategory("k2", "Besi & Baja", "2026-11-18T10:00:00Z"),
    ProductCategory("k3", "Cat & Pelapis", "2026-11-18T10:00:00Z"),
    ProductCategory("k4", "Kayu & Triplex", "2026-11-18T10:00:00Z"),
    ProductCategory("k5", "Keramik & Granit", "2026-11-18T10:00:00Z"),
    ProductCategory("k6", "Pipa & Sanitasi", "2026-11-18T10:00:00Z"),
    ProductCategory("k7", "Semen & Perekat", "2026-11-18T10:00:00Z")
)

@Preview(
    name = "OwnerKategoriDataPhone",
    widthDp = 390,
    heightDp = 1064,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
internal fun OwnerKategoriDataPhonePreview() {
    TbterminalappTheme {
        OwnerKategoriDataScreen(
            summary = KategoriSummaryUiState(totalCategories = 7L),
            categories = sampleReferenceKategoriList(),
            footerStatus = PelangganFooterStatus.LOADING,
            hasMorePages = true,
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onAddCategoryClick = {},
            onCategoryClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "OwnerKategoriDataTablet",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
internal fun OwnerKategoriDataTabletPreview() {
    TbterminalappTheme {
        OwnerKategoriDataScreen(
            summary = KategoriSummaryUiState(totalCategories = 7L),
            categories = sampleReferenceKategoriList().take(6),
            footerStatus = PelangganFooterStatus.LOADING,
            hasMorePages = true,
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onAddCategoryClick = {},
            onCategoryClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "OwnerKategoriEmptyPhone",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
internal fun OwnerKategoriEmptyPhonePreview() {
    TbterminalappTheme {
        val spec = kategoriEmptyStateSpecFor("")
        OwnerKategoriEmptyScreen(
            summary = KategoriSummaryUiState(totalCategories = 0L),
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onNavSelect = {},
            centerPanelContent = true,
            autoFocusSearch = false,
            panelContent = {
                EmptyState(
                    icon = OwnerIcons.CategoryShapes,
                    iconContainerColor = SurfaceWhite,
                    iconTint = Brand,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "category-add",
                    buttonLabel = "Tambah kategori"
                )
            }
        )
    }
}
