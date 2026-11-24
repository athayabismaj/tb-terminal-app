package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.products.ProductUnitUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.util.Locale

private const val MAX_UNIT_ABBREVIATION_CHARS = 5

@Immutable
internal data class SatuanSummaryUiState(
    val totalUnits: Long = 0L,
    val usedByProductsCount: Long? = null,
    val unusedCount: Long? = null
)

@Immutable
internal data class SatuanEmptySpec(
    val title: String,
    val description: String,
    val isSearchNotFound: Boolean
)

internal fun isSatuanErrorMessage(message: String?): Boolean = isCatalogErrorMessage(message)

internal fun filterAndSortOwnerUnits(
    units: List<ProductUnit>,
    searchQuery: String
): List<ProductUnit> {
    val trimmed = searchQuery.trim()
    val filtered = if (trimmed.isEmpty()) {
        units
    } else {
        units.filter { unit ->
            unit.name.contains(trimmed, ignoreCase = true) ||
                unit.symbol.contains(trimmed, ignoreCase = true)
        }
    }
    return filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
}

internal fun formatUnitAbbreviation(rawSymbol: String): String {
    val trimmed = rawSymbol.trim()
    return when {
        trimmed.isEmpty() -> "-"
        trimmed.length > MAX_UNIT_ABBREVIATION_CHARS -> "${trimmed.take(MAX_UNIT_ABBREVIATION_CHARS)}\u2026"
        else -> trimmed
    }
}

internal fun formatSatuanCreatedDate(rawCreatedAt: String): String =
    formatCatalogCreatedDate(rawCreatedAt)

internal fun satuanEmptyStateSpecFor(searchQuery: String): SatuanEmptySpec {
    val trimmed = searchQuery.trim()
    return if (trimmed.isNotEmpty()) {
        SatuanEmptySpec(
            title = "\"$trimmed\" tidak ditemukan",
            description = "Periksa ejaan atau cari dengan singkatan satuan.",
            isSearchNotFound = true
        )
    } else {
        SatuanEmptySpec(
            title = "Belum ada satuan",
            description = "Tambahkan satuan produk untuk menentukan takaran atau ukuran barang.",
            isSearchNotFound = false
        )
    }
}

/**
 * Route Owner untuk halaman Satuan produk, menghubungkan [ProductUnitUiState] ke UI Owner
 * dengan pola header ringkasan hijau, infinite scroll, pencarian, dan keadaan kosong Pelanggan.
 */
@Composable
internal fun OwnerSatuanRoute(
    uiState: ProductUnitUiState,
    isSearchExpanded: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onAddUnitClick: () -> Unit,
    onUnitClick: (ProductUnit) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onBack: (() -> Unit)?,
    onStockClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    summaryOverride: SatuanSummaryUiState? = null,
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

    val hasError = isSatuanErrorMessage(uiState.message)
    val accumulatedUnits = rememberAccumulatedCatalogItems(
        pageItems = uiState.units,
        page = uiState.page,
        isLoading = uiState.isLoading,
        hasError = hasError,
        searchQuery = uiState.searchQuery,
        keyOf = ProductUnit::id
    )
    val displayedUnits = remember(accumulatedUnits, uiState.searchQuery) {
        filterAndSortOwnerUnits(units = accumulatedUnits, searchQuery = uiState.searchQuery)
    }

    // TODO: Tampilkan 2 kolom ringkasan ("Dipakai produk" & "Belum dipakai") saat backend/ProductUnitViewModel
    // menyediakan angka agregat tersebut; saat ini nilainya null sehingga kolom dan garis pemisah dihapus agar tidak menampilkan angka palsu.
    val summary = summaryOverride ?: remember(uiState.totalUnits) {
        SatuanSummaryUiState(totalUnits = uiState.totalUnits)
    }

    val hasMorePages = uiState.page < uiState.totalPages
    val footerStatus = when {
        accumulatedUnits.isEmpty() -> PelangganFooterStatus.NONE
        uiState.isLoading -> PelangganFooterStatus.LOADING
        hasError -> PelangganFooterStatus.ERROR
        !hasMorePages -> PelangganFooterStatus.END_OF_LIST
        else -> PelangganFooterStatus.NONE
    }

    val isInitialLoading = uiState.isLoading && accumulatedUnits.isEmpty()
    val isInitialError = hasError && accumulatedUnits.isEmpty() && !uiState.isLoading
    val showEmptyState = !isInitialLoading && !isInitialError && displayedUnits.isEmpty()
    val emptySpec = remember(uiState.searchQuery) { satuanEmptyStateSpecFor(uiState.searchQuery) }

    Box(modifier = modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && accumulatedUnits.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading || isInitialError || showEmptyState) {
                OwnerSatuanEmptyScreen(
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
                            isInitialLoading -> OwnerSatuanLoadingSkeleton(compact = !isTablet)
                            isInitialError -> CatalogErrorPanel(
                                message = uiState.message ?: "Satuan gagal dimuat.",
                                retryTestTag = "satuan-error-retry",
                                onRetry = onRefresh
                            )
                            else -> EmptyState(
                                icon = if (emptySpec.isSearchNotFound) OwnerIcons.Search else OwnerIcons.RulerUnits,
                                iconContainerColor = if (emptySpec.isSearchNotFound) ChipBg else SurfaceWhite,
                                iconTint = if (emptySpec.isSearchNotFound) Muted else Brand,
                                title = emptySpec.title,
                                description = emptySpec.description,
                                showAddButton = !emptySpec.isSearchNotFound,
                                onAddCustomerClick = onAddUnitClick,
                                buttonTestTag = "unit-add",
                                buttonLabel = "Tambah satuan",
                                modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                            )
                        }
                    }
                )
            } else {
                OwnerSatuanDataScreen(
                    summary = summary,
                    units = displayedUnits,
                    footerStatus = footerStatus,
                    hasMorePages = hasMorePages,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = uiState.searchQuery,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onAddUnitClick = onAddUnitClick,
                    onUnitClick = onUnitClick,
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
            error = if (hasError && !isInitialError && accumulatedUnits.isEmpty()) uiState.message else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
        )
    }
}

@Composable
internal fun OwnerSatuanDataScreen(
    summary: SatuanSummaryUiState,
    units: List<ProductUnit>,
    footerStatus: PelangganFooterStatus,
    hasMorePages: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onAddUnitClick: () -> Unit,
    onUnitClick: (ProductUnit) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    autoFocusSearch: Boolean = true
) {
    OwnerCatalogDataScreen(
        items = units,
        itemKey = ProductUnit::id,
        footerStatus = footerStatus,
        hasMorePages = hasMorePages,
        listTestTag = "owner-satuan-lazy-list",
        footerLoadingText = "Memuat satuan lainnya\u2026",
        footerEndText = "Semua satuan sudah ditampilkan",
        footerTestTag = "satuan-list-footer",
        footerRetryTestTag = "satuan-footer-retry",
        headerBar = {
            CatalogHeaderBar(
                title = "Satuan produk",
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                searchPlaceholder = "Cari satuan atau singkatan...",
                searchContentDescription = "Cari satuan",
                backTestTag = "satuan-back-btn",
                searchToggleTestTag = "unit-toggle-search",
                searchBackTestTag = "unit-search-back",
                searchInputTestTag = "unit-search-input",
                searchClearTestTag = "unit-search-clear",
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                autoFocusSearch = autoFocusSearch
            )
        },
        summaryStats = { isTablet -> SatuanSummaryStats(summary = summary, isTablet = isTablet) },
        listHeaderSection = {
            CatalogListHeaderSection(
                addButtonLabel = "Tambah satuan",
                addButtonTestTag = "unit-add",
                sectionTitle = "Semua satuan",
                sortLabel = "Urut: Nama A\u2013Z",
                sortTestTag = "satuan-sort-label",
                onAddClick = onAddUnitClick
            )
        },
        rowContent = { unit -> UnitRow(unit = unit, onClick = { onUnitClick(unit) }) },
        onLoadMore = onLoadMore,
        onRetryLoadMore = onRetryLoadMore,
        onNavSelect = onNavSelect,
        modifier = modifier,
        activeNavDestination = activeNavDestination
    )
}

@Composable
internal fun OwnerSatuanEmptyScreen(
    summary: SatuanSummaryUiState,
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
                title = "Satuan produk",
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                searchPlaceholder = "Cari satuan atau singkatan...",
                searchContentDescription = "Cari satuan",
                backTestTag = "satuan-back-btn",
                searchToggleTestTag = "unit-toggle-search",
                searchBackTestTag = "unit-search-back",
                searchInputTestTag = "unit-search-input",
                searchClearTestTag = "unit-search-clear",
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                autoFocusSearch = autoFocusSearch
            )
        },
        summaryStats = { isTablet -> SatuanSummaryStats(summary = summary, isTablet = isTablet) },
        onNavSelect = onNavSelect,
        centerPanelContent = centerPanelContent,
        panelContent = panelContent,
        modifier = modifier,
        activeNavDestination = activeNavDestination
    )
}

/**
 * Ringkasan hero Satuan produk di header hijau; menyembunyikan baris 2 kolom dan garis pemisah
 * bila backend belum menyediakan data "Dipakai produk" / "Belum dipakai".
 */
@Composable
internal fun SatuanSummaryStats(
    summary: SatuanSummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val breakdown = remember(summary.usedByProductsCount, summary.unusedCount) {
        if (summary.usedByProductsCount != null && summary.unusedCount != null) {
            listOf(
                CatalogBreakdownMetric("Dipakai produk", "${summary.usedByProductsCount} satuan"),
                CatalogBreakdownMetric("Belum dipakai", "${summary.unusedCount} satuan")
            )
        } else emptyList()
    }
    CatalogSummaryStats(
        totalLabel = "Total satuan",
        totalCount = summary.totalUnits,
        unitLabel = "satuan",
        countTestTag = "satuan-total-count",
        breakdownMetrics = breakdown,
        modifier = modifier,
        isTablet = isTablet
    )
}

/**
 * Lingkaran singkatan satuan 40x40dp berborder 1dp dengan teks brand 12/600 maksimal 5 karakter.
 */
@Composable
internal fun AbbreviationBadge(
    symbol: String,
    modifier: Modifier = Modifier
) {
    val trimmed = symbol.trim().ifEmpty { "-" }
    val displayText = formatUnitAbbreviation(symbol)

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .border(1.dp, OwnerBorder, CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = "Singkatan $trimmed"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Brand,
                letterSpacing = (-0.01).em
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clearAndSetSemantics { }
        )
    }
}

/**
 * Baris daftar satuan produk minimal 72dp dengan lingkaran singkatan, nama satuan, tanggal dibuat, dan chevron.
 */
@Composable
internal fun UnitRow(
    unit: ProductUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CatalogListRow(
        title = unit.name,
        subtitle = "Dibuat ${formatSatuanCreatedDate(unit.createdAt)}",
        onClick = onClick,
        testTag = "unit-row-${unit.id}",
        leading = { AbbreviationBadge(symbol = unit.symbol) },
        modifier = modifier
    )
}

@Composable
internal fun OwnerSatuanLoadingSkeleton(compact: Boolean) {
    CatalogLoadingSkeleton(compact = compact, testTag = "unit-skeleton")
}

internal fun sampleReferenceSatuanUnits(): List<ProductUnit> = listOf(
    ProductUnit("u1", "Batang", "btg", "2026-11-18T10:00:00Z"),
    ProductUnit("u2", "Dus", "dus", "2026-11-18T10:00:00Z"),
    ProductUnit("u3", "Galon", "galon", "2026-11-18T10:00:00Z"),
    ProductUnit("u4", "Kilogram", "kg", "2026-11-18T10:00:00Z"),
    ProductUnit("u5", "Lembar", "lbr", "2026-11-18T10:00:00Z"),
    ProductUnit("u6", "Liter", "L", "2026-11-18T10:00:00Z"),
    ProductUnit("u7", "Meter", "m", "2026-11-18T10:00:00Z")
)

@Preview(
    name = "OwnerSatuanDataPhone",
    widthDp = 390,
    heightDp = 1064,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
internal fun OwnerSatuanDataPhonePreview() {
    TbterminalappTheme {
        OwnerSatuanDataScreen(
            summary = SatuanSummaryUiState(
                totalUnits = 10L,
                usedByProductsCount = 8L,
                unusedCount = 2L
            ),
            units = sampleReferenceSatuanUnits(),
            footerStatus = PelangganFooterStatus.LOADING,
            hasMorePages = true,
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onAddUnitClick = {},
            onUnitClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "OwnerSatuanDataTablet",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
internal fun OwnerSatuanDataTabletPreview() {
    TbterminalappTheme {
        OwnerSatuanDataScreen(
            summary = SatuanSummaryUiState(
                totalUnits = 10L,
                usedByProductsCount = 8L,
                unusedCount = 2L
            ),
            units = sampleReferenceSatuanUnits().take(6),
            footerStatus = PelangganFooterStatus.LOADING,
            hasMorePages = true,
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onAddUnitClick = {},
            onUnitClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "OwnerSatuanEmptyPhone",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
internal fun OwnerSatuanEmptyPhonePreview() {
    TbterminalappTheme {
        val spec = satuanEmptyStateSpecFor("")
        OwnerSatuanEmptyScreen(
            summary = SatuanSummaryUiState(totalUnits = 0L),
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
                    icon = OwnerIcons.RulerUnits,
                    iconContainerColor = SurfaceWhite,
                    iconTint = Brand,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "unit-add",
                    buttonLabel = "Tambah satuan"
                )
            }
        )
    }
}
