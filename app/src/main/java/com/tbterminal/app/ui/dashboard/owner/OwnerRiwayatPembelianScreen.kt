package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.purchasehistory.PurchaseError
import com.tbterminal.app.ui.purchasehistory.PurchaseHistoryUiState
import com.tbterminal.app.ui.purchasehistory.PurchaseSortOrder
import com.tbterminal.app.ui.purchasehistory.PurchaseSummaryBottomSheet
import com.tbterminal.app.ui.purchasehistory.filterAndSortPurchases
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OwnerAmberInk
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

private val OWNER_PURCHASE_TABLET_BREAKPOINT = 840.dp
private val PURCHASE_WIB_ZONE = ZoneId.of("Asia/Jakarta")
private val PurchaseLunasDotColor = Color(0xFF1F9D63)
private val PurchaseHutangDotColor = Color(0xFFD49A00)

@Immutable
internal data class OwnerPurchaseSummaryUiState(
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val totalNotes: Long = 0L,
    val paidTotal: BigDecimal = BigDecimal.ZERO,
    val debtTotal: BigDecimal = BigDecimal.ZERO
)

/**
 * TODO: Ringkasan header ("Lunas" & "Hutang") saat ini memakai sisa hutang terbuka per `purchaseId`
 * dari `getPayables` karena `PurchaseSummary` belum memiliki field status/`paidAmount`/`remainingAmount`
 * dan `PurchaseSummaryPage` belum mengembalikan agregat lintas halaman. Konfirmasikan definisi untuk
 * nota yang dibayar sebagian (DP): apakah "Lunas" menghitung porsi terbayar agar Total pembelian =
 * Lunas + Hutang, atau hanya nota yang berstatus Lunas penuh.
 */
internal fun computeOwnerPurchaseSummary(
    displayedPurchases: List<PurchaseSummary>,
    totalLoadedSourceCount: Int,
    totalPurchasesFromServer: Long,
    unpaidPayableByPurchaseId: Map<String, BigDecimal>
): OwnerPurchaseSummaryUiState {
    var totalAmount = BigDecimal.ZERO
    var paidTotal = BigDecimal.ZERO
    var debtTotal = BigDecimal.ZERO

    displayedPurchases.forEach { purchase ->
        val txTotal = purchase.total.coerceAtLeast(BigDecimal.ZERO)
        val remaining = (unpaidPayableByPurchaseId[purchase.id] ?: BigDecimal.ZERO)
            .coerceIn(BigDecimal.ZERO, txTotal)
        val paidPart = txTotal.subtract(remaining).coerceAtLeast(BigDecimal.ZERO)

        totalAmount = totalAmount.add(txTotal)
        paidTotal = paidTotal.add(paidPart)
        debtTotal = debtTotal.add(remaining)
    }

    val totalNotes = when {
        displayedPurchases.isEmpty() -> 0L
        displayedPurchases.size < totalLoadedSourceCount -> displayedPurchases.size.toLong()
        else -> totalPurchasesFromServer.coerceAtLeast(displayedPurchases.size.toLong())
    }

    return OwnerPurchaseSummaryUiState(
        totalAmount = totalAmount,
        totalNotes = totalNotes,
        paidTotal = paidTotal,
        debtTotal = debtTotal
    )
}

internal fun PurchaseHistoryUiState.toOwnerPurchaseSummary(): OwnerPurchaseSummaryUiState =
    computeOwnerPurchaseSummary(
        displayedPurchases = visiblePurchases,
        totalLoadedSourceCount = purchases.size,
        totalPurchasesFromServer = totalPurchases,
        unpaidPayableByPurchaseId = unpaidPayableByPurchaseId
    )

internal fun purchaseSortOrderShortLabel(order: PurchaseSortOrder): String = when (order) {
    PurchaseSortOrder.NEWEST -> "Urut: Terbaru"
    PurchaseSortOrder.NAME_ASC -> "Urut: Nama (A–Z)"
    PurchaseSortOrder.HIGHEST_AMOUNT -> "Urut: Terbesar"
    PurchaseSortOrder.DUE_NEAREST -> "Urut: Tempo"
}

@Immutable
internal data class OwnerPurchaseEmptySpec(
    val title: String,
    val description: String?
)

internal fun ownerPurchaseEmptySpec(
    hasActiveFilterOrSearch: Boolean
): OwnerPurchaseEmptySpec = if (hasActiveFilterOrSearch) {
    OwnerPurchaseEmptySpec(
        title = "Pembelian tidak ditemukan",
        description = "Tidak ada pembelian yang cocok dengan filter atau kata kunci saat ini."
    )
} else {
    // TODO: Desain final keadaan belum ada transaksi pembelian sama sekali (tanpa filter) mengikuti halaman Riwayat Penjualan; tanpa tombol tambahan.
    OwnerPurchaseEmptySpec(
        title = "Belum ada transaksi pembelian",
        description = null
    )
}

internal fun hasActivePurchaseFilterOrSearch(
    state: PurchaseHistoryUiState,
    todayStr: String = LocalDate.now(PURCHASE_WIB_ZONE).toString()
): Boolean {
    val period = state.selectedPeriodType.trim()
    val isDefaultToday = period.equals("Hari Ini", ignoreCase = true) || period.equals("Hari", ignoreCase = true)
    val isAll = period.equals("Semua", ignoreCase = true)
    val startDay = state.startDate?.take(10)
    val endDay = state.endDate?.take(10)
    val isNonDefaultDate = (!isDefaultToday && !isAll) ||
        (!isAll && ((startDay != null && startDay != todayStr) || (endDay != null && endDay != todayStr)))

    return state.searchQuery.isNotBlank() ||
        state.selectedSupplierId != null ||
        isNonDefaultDate
}

@Composable
internal fun OwnerPurchaseSummaryHero(
    summary: OwnerPurchaseSummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val countText = remember(summary.totalNotes) { formatHistoryCountLabel(summary.totalNotes, "nota") }
    val paidText = remember(summary.paidTotal) { formatSalesColumnCompactRupiah(summary.paidTotal) }
    val debtText = remember(summary.debtTotal) { formatSalesColumnCompactRupiah(summary.debtTotal) }

    OwnerHistoryHeroSummary(
        totalLabel = "Total pembelian",
        totalAmount = summary.totalAmount,
        totalTestTag = "purchase-summary-total-amount",
        metrics = listOf(
            OwnerHistoryHeroMetric("Total nota", countText, "purchase-summary-count"),
            OwnerHistoryHeroMetric("Lunas", paidText, "purchase-summary-paid"),
            OwnerHistoryHeroMetric("Hutang", debtText, "purchase-summary-debt")
        ),
        modifier = modifier,
        isTablet = isTablet
    )
}

@Composable
internal fun OwnerRiwayatPembelianRoute(
    uiState: PurchaseHistoryUiState,
    isSearchExpanded: Boolean,
    showFilterPopup: Boolean,
    showSummaryPopup: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onDismissFilter: () -> Unit,
    onApplyFilter: (String?, String?, String?, String, PurchaseSortOrder) -> Unit,
    onOpenSummary: () -> Unit,
    onDismissSummary: () -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryLoadMore: (Int) -> Unit,
    onBack: (() -> Unit)?,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismissError: () -> Unit,
    onSupplierClick: ((String) -> Unit)? = null
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val navigateBack: () -> Unit = onBack ?: {
        destinationNavigator?.invoke(AdminDestination.TransactionsHub) ?: onDashboardClick()
    }

    BackHandler(enabled = isSearchExpanded) {
        onCloseSearch()
    }
    BackHandler(enabled = !isSearchExpanded) {
        navigateBack()
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                navigateBack()
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub) ?: onReceivablesClick()
            DashboardNavDestination.STOK ->
                destinationNavigator?.invoke(AdminDestination.StockHub) ?: onProductsClick()
            DashboardNavDestination.MENU ->
                destinationNavigator?.invoke(AdminDestination.MoreHub) ?: onSettingsClick()
        }
    }

    // Akumulasi halaman dari PurchaseHistoryUiState agar mendukung infinite scroll seperti Pelanggan/Supplier
    val serverQueryKey = remember(
        uiState.selectedSupplierId,
        uiState.selectedPeriodType,
        uiState.startDate,
        uiState.endDate
    ) {
        listOf(uiState.selectedSupplierId, uiState.selectedPeriodType, uiState.startDate, uiState.endDate)
    }
    var accumulatedPurchases by remember { mutableStateOf<List<PurchaseSummary>>(emptyList()) }
    var lastLoadedPage by remember { mutableIntStateOf(0) }
    var lastQueryKey by remember { mutableStateOf(serverQueryKey) }

    LaunchedEffect(uiState.purchases, uiState.page, uiState.isLoading, uiState.errorMessage, serverQueryKey) {
        if (serverQueryKey != lastQueryKey) {
            accumulatedPurchases = emptyList()
            lastLoadedPage = 0
            lastQueryKey = serverQueryKey
        }
        if (!uiState.isLoading && uiState.errorMessage == null) {
            if (uiState.page <= 1) {
                accumulatedPurchases = uiState.purchases
                lastLoadedPage = 1
            } else if (uiState.page > lastLoadedPage) {
                accumulatedPurchases = (accumulatedPurchases + uiState.purchases).distinctBy { it.id }
                lastLoadedPage = uiState.page
            } else if (uiState.page == lastLoadedPage) {
                val updatedById = uiState.purchases.associateBy { it.id }
                accumulatedPurchases = accumulatedPurchases.map { existing ->
                    updatedById[existing.id] ?: existing
                }
            }
        }
    }

    val displayedPurchases = remember(
        accumulatedPurchases,
        uiState.searchQuery,
        uiState.selectedSupplierId,
        uiState.selectedPeriodType,
        uiState.startDate,
        uiState.endDate,
        uiState.sortOrder
    ) {
        filterAndSortPurchases(
            purchases = accumulatedPurchases,
            searchQuery = uiState.searchQuery,
            selectedSupplierId = uiState.selectedSupplierId,
            selectedPeriodType = uiState.selectedPeriodType,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            sortOrder = uiState.sortOrder
        )
    }

    val todayStr = remember { LocalDate.now(PURCHASE_WIB_ZONE).toString() }
    val hasActiveFilterOrSearch = remember(
        uiState.searchQuery,
        uiState.selectedSupplierId,
        uiState.selectedPeriodType,
        uiState.startDate,
        uiState.endDate,
        todayStr
    ) {
        hasActivePurchaseFilterOrSearch(uiState, todayStr)
    }

    val emptySpec = remember(hasActiveFilterOrSearch) {
        ownerPurchaseEmptySpec(hasActiveFilterOrSearch = hasActiveFilterOrSearch)
    }
    val summary = remember(
        displayedPurchases,
        accumulatedPurchases.size,
        uiState.totalPurchases,
        uiState.unpaidPayableByPurchaseId
    ) {
        computeOwnerPurchaseSummary(
            displayedPurchases = displayedPurchases,
            totalLoadedSourceCount = accumulatedPurchases.size,
            totalPurchasesFromServer = uiState.totalPurchases,
            unpaidPayableByPurchaseId = uiState.unpaidPayableByPurchaseId
        )
    }
    val sortLabel = remember(uiState.sortOrder) {
        purchaseSortOrderShortLabel(uiState.sortOrder)
    }

    val hasMorePages = uiState.page < uiState.totalPages
    val isLoadMoreError = uiState.errorMessage != null && accumulatedPurchases.isNotEmpty() && uiState.page > lastLoadedPage
    val footerStatus = when {
        accumulatedPurchases.isEmpty() -> PelangganFooterStatus.NONE
        uiState.isLoading -> PelangganFooterStatus.LOADING
        isLoadMoreError -> PelangganFooterStatus.ERROR
        !hasMorePages -> PelangganFooterStatus.END_OF_LIST
        else -> PelangganFooterStatus.NONE
    }

    val isInitialLoading = uiState.isLoading && accumulatedPurchases.isEmpty()
    val isInitialError = uiState.errorMessage != null && accumulatedPurchases.isEmpty() && !uiState.isLoading
    val showEmptyState = !isInitialLoading && !isInitialError && displayedPurchases.isEmpty()

    val showInlineDetail = uiState.isLoadingDetail || uiState.selectedPurchase != null
    if (showInlineDetail) {
        BackHandler {
            onDismissDetail()
        }
        val inlineDetailUiState = remember(uiState.selectedPurchase, uiState.isLoadingDetail, uiState.errorMessage) {
            val detail = uiState.selectedPurchase
            OwnerPurchaseDetailUiState(
                purchase = detail,
                matchingPayable = null,
                isLoading = uiState.isLoadingDetail,
                errorMessage = if (detail == null && !uiState.isLoadingDetail) uiState.errorMessage else null
            )
        }
        OwnerDetailNotaPembelianHost(
            uiState = inlineDetailUiState,
            onBack = onDismissDetail,
            onRetry = { uiState.selectedPurchase?.id?.let(onShowDetail) },
            onSupplierClick = onSupplierClick,
            onNavSelect = ::handleNavSelect
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.page <= 1 && accumulatedPurchases.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading || isInitialError || showEmptyState) {
                OwnerRiwayatPembelianScreen(
                    summary = summary,
                    sortLabel = sortLabel,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = uiState.searchQuery,
                    isLoading = isInitialLoading,
                    errorMessage = if (isInitialError) uiState.errorMessage else null,
                    purchases = emptyList(),
                    emptyTitle = emptySpec.title,
                    emptyDescription = emptySpec.description,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onOpenFilter = onOpenFilter,
                    onReportClick = {
                        // TODO: Ikon dokumen kanan membuka Ringkasan Pembelian (PurchaseSummaryBottomSheet); konfirmasikan bila perlu diarahkan ke halaman laporan penuh.
                        onOpenSummary()
                    },
                    onNavSelect = ::handleNavSelect,
                    onRetry = onRefresh
                )
            } else {
                OwnerRiwayatPembelianDataScreen(
                    summary = summary,
                    purchases = displayedPurchases,
                    unpaidPayableByPurchaseId = uiState.unpaidPayableByPurchaseId,
                    sortLabel = sortLabel,
                    footerStatus = footerStatus,
                    hasMorePages = hasMorePages,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = uiState.searchQuery,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onOpenFilter = onOpenFilter,
                    onReportClick = {
                        // TODO: Ikon dokumen kanan membuka Ringkasan Pembelian (PurchaseSummaryBottomSheet); konfirmasikan bila perlu diarahkan ke halaman laporan penuh.
                        onOpenSummary()
                    },
                    onPurchaseClick = onShowDetail,
                    onLoadMore = onLoadNextPage,
                    onRetryLoadMore = { onRetryLoadMore((lastLoadedPage + 1).coerceAtLeast(1)) },
                    onNavSelect = ::handleNavSelect
                )
            }
        }

        if (displayedPurchases.isNotEmpty() && !isLoadMoreError) {
            TopFloatingCapsule(
                message = null,
                error = uiState.errorMessage,
                onDismiss = onDismissError,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(99f)
            )
        }
    }

    if (showFilterPopup) {
        OwnerPurchaseFilterModal(
            state = uiState,
            onDismiss = onDismissFilter,
            onApplyFilter = onApplyFilter
        )
    }

    if (showSummaryPopup) {
        PurchaseSummaryBottomSheet(
            uiState = uiState.copy(purchases = accumulatedPurchases.ifEmpty { uiState.purchases }),
            onDismiss = onDismissSummary
        )
    }
}

@Composable
internal fun OwnerRiwayatPembelianScreen(
    summary: OwnerPurchaseSummaryUiState,
    sortLabel: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    errorMessage: String?,
    purchases: List<PurchaseSummary>,
    emptyTitle: String,
    emptyDescription: String?,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true,
    onRetry: () -> Unit = {},
    dataContent: @Composable (isTablet: Boolean) -> Unit = {}
) {
    OwnerHistoryResponsiveLayout(
        title = "Riwayat Pembelian",
        panelTitle = "Daftar pembelian",
        sortLabel = sortLabel,
        sortLabelTestTag = "purchase-history-sort-label",
        searchPlaceholder = "Cari nota atau supplier...",
        backTestTag = "purchase-history-back-btn",
        searchTestTag = "purchase-history-toggle-search",
        filterTestTag = "purchase-history-open-filters",
        reportTestTag = "purchase-history-summary",
        searchBackTestTag = "purchase-history-search-back",
        searchInputTestTag = "purchase-history-search",
        searchClearTestTag = "purchase-history-search-clear",
        searchContentDescription = "Cari pembelian",
        filterContentDescription = "Filter pembelian",
        reportContentDescription = "Ringkasan pembelian",
        isSearchExpanded = isSearchExpanded,
        searchQuery = searchQuery,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isEmpty = purchases.isEmpty(),
        emptyTitle = emptyTitle,
        emptyDescription = emptyDescription,
        onBack = onBack,
        onOpenSearch = onOpenSearch,
        onCloseSearch = onCloseSearch,
        onSearchChanged = onSearchChanged,
        onOpenFilter = onOpenFilter,
        onReportClick = onReportClick,
        onNavSelect = onNavSelect,
        heroSummary = { isTablet -> OwnerPurchaseSummaryHero(summary = summary, isTablet = isTablet) },
        loadingContent = { isTablet -> OwnerPurchaseLoadingSkeleton(compact = !isTablet) },
        errorContent = { message ->
            PurchaseError(message = message, onRefresh = onRetry)
        },
        dataContent = dataContent,
        modifier = modifier,
        autoFocusSearch = autoFocusSearch
    )
}

@Composable
internal fun OwnerRiwayatPembelianDataScreen(
    summary: OwnerPurchaseSummaryUiState,
    purchases: List<PurchaseSummary>,
    unpaidPayableByPurchaseId: Map<String, BigDecimal>,
    sortLabel: String,
    footerStatus: PelangganFooterStatus,
    hasMorePages: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    onPurchaseClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true,
    listState: LazyListState = rememberLazyListState()
) {
    LaunchedEffect(listState, purchases.size, hasMorePages, footerStatus) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= (totalItems - 3).coerceAtLeast(0) && totalItems > 1
        }.collect { nearBottom ->
            if (nearBottom && hasMorePages && footerStatus == PelangganFooterStatus.NONE) {
                onLoadMore()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_PURCHASE_TABLET_BREAKPOINT
        if (isTablet) {
            TabletOwnerPurchaseDataLayout(
                summary = summary,
                purchases = purchases,
                unpaidPayableByPurchaseId = unpaidPayableByPurchaseId,
                sortLabel = sortLabel,
                footerStatus = footerStatus,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                listState = listState,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = onReportClick,
                onPurchaseClick = onPurchaseClick,
                onRetryLoadMore = onRetryLoadMore,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch
            )
        } else {
            PhoneOwnerPurchaseDataLayout(
                summary = summary,
                purchases = purchases,
                unpaidPayableByPurchaseId = unpaidPayableByPurchaseId,
                sortLabel = sortLabel,
                footerStatus = footerStatus,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                listState = listState,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = onReportClick,
                onPurchaseClick = onPurchaseClick,
                onRetryLoadMore = onRetryLoadMore,
                autoFocusSearch = autoFocusSearch
            )
        }
    }
}

@Composable
private fun PhoneOwnerPurchaseDataLayout(
    summary: OwnerPurchaseSummaryUiState,
    purchases: List<PurchaseSummary>,
    unpaidPayableByPurchaseId: Map<String, BigDecimal>,
    sortLabel: String,
    footerStatus: PelangganFooterStatus,
    isSearchExpanded: Boolean,
    searchQuery: String,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    onPurchaseClick: (String) -> Unit,
    onRetryLoadMore: () -> Unit,
    autoFocusSearch: Boolean
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .testTag("owner-purchase-lazy-list")
    ) {
        item(key = "purchase-header-cap") {
            Layout(
                content = {
                    HistoryPhoneGreenHeader(
                        title = "Riwayat Pembelian",
                        searchPlaceholder = "Cari nota atau supplier...",
                        backTestTag = "purchase-history-back-btn",
                        searchTestTag = "purchase-history-toggle-search",
                        filterTestTag = "purchase-history-open-filters",
                        reportTestTag = "purchase-history-summary",
                        searchBackTestTag = "purchase-history-search-back",
                        searchInputTestTag = "purchase-history-search",
                        searchClearTestTag = "purchase-history-search-clear",
                        searchContentDescription = "Cari pembelian",
                        filterContentDescription = "Filter pembelian",
                        reportContentDescription = "Ringkasan pembelian",
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        onBack = onBack,
                        onOpenSearch = onOpenSearch,
                        onCloseSearch = onCloseSearch,
                        onSearchChanged = onSearchChanged,
                        onOpenFilter = onOpenFilter,
                        onReportClick = onReportClick,
                        autoFocusSearch = autoFocusSearch,
                        heroSummary = { OwnerPurchaseSummaryHero(summary = summary) }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 4.dp)
                    ) {
                        HistoryPanelHeaderRow(
                            panelTitle = "Daftar pembelian",
                            sortLabel = sortLabel,
                            sortLabelTestTag = "purchase-history-sort-label"
                        )
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val capPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + capPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)
                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    capPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }

        itemsIndexed(
            items = purchases,
            key = { _, purchase -> purchase.id }
        ) { index, purchase ->
            val remaining = (unpaidPayableByPurchaseId[purchase.id] ?: BigDecimal.ZERO)
                .coerceAtLeast(BigDecimal.ZERO)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 20.dp)
            ) {
                PurchaseRow(
                    purchase = purchase,
                    isUnpaid = remaining > BigDecimal.ZERO,
                    onClick = { onPurchaseClick(purchase.id) }
                )
                if (index < purchases.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp)
                            .height(1.dp)
                            .background(OwnerDivider)
                    )
                }
            }
        }

        item(key = "purchase-footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp)
            ) {
                ListFooterState(
                    status = footerStatus,
                    onRetry = onRetryLoadMore,
                    loadingText = "Memuat pembelian lainnya\u2026",
                    endText = "Semua pembelian sudah ditampilkan",
                    footerTestTag = "purchase-list-footer",
                    retryTestTag = "purchase-footer-retry"
                )
            }
        }
    }
}

@Composable
private fun TabletOwnerPurchaseDataLayout(
    summary: OwnerPurchaseSummaryUiState,
    purchases: List<PurchaseSummary>,
    unpaidPayableByPurchaseId: Map<String, BigDecimal>,
    sortLabel: String,
    footerStatus: PelangganFooterStatus,
    isSearchExpanded: Boolean,
    searchQuery: String,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    onPurchaseClick: (String) -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.TRANSAKSI,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            HistoryTabletGreenPanel(
                title = "Riwayat Pembelian",
                searchPlaceholder = "Cari nota atau supplier...",
                backTestTag = "purchase-history-back-btn",
                searchTestTag = "purchase-history-toggle-search",
                filterTestTag = "purchase-history-open-filters",
                reportTestTag = "purchase-history-summary",
                searchBackTestTag = "purchase-history-search-back",
                searchInputTestTag = "purchase-history-search",
                searchClearTestTag = "purchase-history-search-clear",
                searchContentDescription = "Cari pembelian",
                filterContentDescription = "Filter pembelian",
                reportContentDescription = "Ringkasan pembelian",
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = onReportClick,
                autoFocusSearch = autoFocusSearch,
                heroSummary = { OwnerPurchaseSummaryHero(summary = summary, isTablet = true) }
            )

            Box(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("owner-purchase-lazy-list"),
                    contentPadding = PaddingValues(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
                ) {
                    item(key = "purchase-top-controls") {
                        HistoryPanelHeaderRow(
                            panelTitle = "Daftar pembelian",
                            sortLabel = sortLabel,
                            sortLabelTestTag = "purchase-history-sort-label",
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    itemsIndexed(
                        items = purchases,
                        key = { _, purchase -> purchase.id }
                    ) { index, purchase ->
                        val remaining = (unpaidPayableByPurchaseId[purchase.id] ?: BigDecimal.ZERO)
                            .coerceAtLeast(BigDecimal.ZERO)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            PurchaseRow(
                                purchase = purchase,
                                isUnpaid = remaining > BigDecimal.ZERO,
                                onClick = { onPurchaseClick(purchase.id) }
                            )
                            if (index < purchases.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp)
                                        .height(1.dp)
                                        .background(OwnerDivider)
                                )
                            }
                        }
                    }

                    item(key = "purchase-footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            ListFooterState(
                                status = footerStatus,
                                onRetry = onRetryLoadMore,
                                loadingText = "Memuat pembelian lainnya\u2026",
                                endText = "Semua pembelian sudah ditampilkan",
                                footerTestTag = "purchase-list-footer",
                                retryTestTag = "purchase-footer-retry"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PurchaseRow(
    purchase: PurchaseSummary,
    isUnpaid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val supplierTitle = purchase.supplierName.ifBlank { "Tanpa Supplier" }
    val invoiceLabel = purchase.invoiceNo?.trim()?.takeIf { it.isNotEmpty() } ?: "Tanpa no. nota"
    val statusLabel = if (isUnpaid) "Hutang" else "Lunas"
    val statusColor = if (isUnpaid) OwnerAmberInk else Brand
    val totalLabel = formatRupiahWhole(purchase.total)
    val dateLabel = formatPurchaseShortDateTime(purchase.receivedAt.ifBlank { purchase.createdAt })

    val subtitleAnnotated = remember(invoiceLabel, statusLabel, statusColor) {
        buildAnnotatedString {
            append(invoiceLabel)
            append(" \u00B7 ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = statusColor)) {
                append(statusLabel)
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var leftColWidthPx by remember { mutableIntStateOf(0) }
    val subtitleSize = remember(subtitleAnnotated, leftColWidthPx, density.fontScale) {
        if (leftColWidthPx <= 0) {
            13.sp
        } else {
            val candidates = listOf(13f, 12f, 11f, 10f)
            candidates.firstOrNull { sizeSp ->
                textMeasurer.measure(
                    text = subtitleAnnotated,
                    style = ownerTextStyle(
                        fontSize = sizeSp.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Muted,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    softWrap = false
                ).size.width <= leftColWidthPx
            }?.sp ?: 10.sp
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$supplierTitle, $invoiceLabel, $statusLabel, $totalLabel, $dateLabel"
            }
            .testTag("purchase-row-${purchase.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PurchaseStatusAvatar(isUnpaid = isUnpaid)

        Column(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { leftColWidthPx = it.width }
        ) {
            Text(
                text = supplierTitle,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleAnnotated,
                style = ownerTextStyle(
                    fontSize = subtitleSize,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.cssLineBoxHeight(18.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = totalLabel,
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    tabularNumbers = true
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateLabel,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(18.dp)
            )
        }

        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronRight,
                tint = Muted,
                size = 20.dp
            )
        }
    }
}

@Composable
private fun PurchaseStatusAvatar(
    isUnpaid: Boolean,
    modifier: Modifier = Modifier
) {
    val statusDesc = if (isUnpaid) "Hutang" else "Lunas"
    Box(
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = statusDesc }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .border(1.dp, OwnerBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.Truck,
                tint = Brand,
                size = 22.dp
            )
        }

        PurchaseStatusDot(
            isUnpaid = isUnpaid,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 1.dp)
        )
    }
}

@Composable
internal fun PurchaseStatusDot(
    isUnpaid: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (isUnpaid) PurchaseHutangDotColor else PurchaseLunasDotColor)
            .border(2.dp, SurfaceWhite, CircleShape)
    )
}

@Composable
private fun OwnerPurchaseLoadingSkeleton(compact: Boolean) {
    val count = if (compact) 4 else 6
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(count) { index ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp)
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ChipBg)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipBg)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipBg)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(15.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipBg)
                    )
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipBg)
                    )
                }
            }
        }
    }
}

internal fun formatPurchaseShortDateTime(
    raw: String,
    currentYear: Int = LocalDate.now(PURCHASE_WIB_ZONE).year
): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    runCatching {
        val zdt = OffsetDateTime.parse(trimmed).atZoneSameInstant(PURCHASE_WIB_ZONE)
        val localDate = zdt.toLocalDate()
        val dateText = if (localDate.year == currentYear) {
            "${localDate.dayOfMonth} ${indonesianShortMonth(localDate.monthValue)}"
        } else {
            formatIndonesianDateShort(localDate)
        }
        val timeText = String.format(Locale.ROOT, "%02d:%02d", zdt.hour, zdt.minute)
        return "$dateText, $timeText"
    }
    runCatching {
        val date = LocalDate.parse(trimmed.take(10))
        return if (date.year == currentYear) {
            "${date.dayOfMonth} ${indonesianShortMonth(date.monthValue)}"
        } else {
            formatIndonesianDateShort(date)
        }
    }
    return trimmed.take(16)
}

internal fun sampleReferencePurchases(): List<PurchaseSummary> = listOf(
    PurchaseSummary(
        id = "p1",
        supplierId = "s1",
        supplierName = "Supri",
        invoiceNo = "BM-20261123-001",
        total = BigDecimal("380000"),
        receivedAt = "2026-11-23T11:32:00Z",
        createdAt = "2026-11-23T11:32:00Z"
    ),
    PurchaseSummary(
        id = "p2",
        supplierId = "s2",
        supplierName = "CV Sinar Baja",
        invoiceNo = "BM-20261122-003",
        total = BigDecimal("2600000"),
        receivedAt = "2026-11-22T09:41:00Z",
        createdAt = "2026-11-22T09:41:00Z"
    ),
    PurchaseSummary(
        id = "p3",
        supplierId = "s3",
        supplierName = "Toko Bangunan Jaya",
        invoiceNo = "BM-20261122-002",
        total = BigDecimal("1250000"),
        receivedAt = "2026-11-22T07:05:00Z",
        createdAt = "2026-11-22T07:05:00Z"
    ),
    PurchaseSummary(
        id = "p4",
        supplierId = "s1",
        supplierName = "Supri",
        invoiceNo = "BM-20261121-001",
        total = BigDecimal("760000"),
        receivedAt = "2026-11-21T02:48:00Z",
        createdAt = "2026-11-21T02:48:00Z"
    ),
    PurchaseSummary(
        id = "p5",
        supplierId = "s4",
        supplierName = "PT Sumber Bangunan",
        invoiceNo = "BM-20261120-002",
        total = BigDecimal("7600000"),
        receivedAt = "2026-11-20T09:20:00Z",
        createdAt = "2026-11-20T09:20:00Z"
    ),
    PurchaseSummary(
        id = "p6",
        supplierId = "s5",
        supplierName = "UD Maju Jaya",
        invoiceNo = "BM-20261119-001",
        total = BigDecimal("540000"),
        receivedAt = "2026-11-19T03:12:00Z",
        createdAt = "2026-11-19T03:12:00Z"
    )
)

internal fun sampleReferencePurchasePayables(): Map<String, BigDecimal> = mapOf(
    "p2" to BigDecimal("2600000"),
    "p5" to BigDecimal("7600000")
)

@Preview(
    name = "Barokah Jaya Riwayat Pembelian - Kosong Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPembelianEmptyPhonePreview() {
    TbterminalappTheme {
        val spec = ownerPurchaseEmptySpec(hasActiveFilterOrSearch = false)
        OwnerRiwayatPembelianScreen(
            summary = OwnerPurchaseSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            purchases = emptyList(),
            emptyTitle = spec.title,
            emptyDescription = spec.description,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onReportClick = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "Barokah Jaya Riwayat Pembelian - Tidak Ditemukan Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPembelianNotFoundPhonePreview() {
    TbterminalappTheme {
        val spec = ownerPurchaseEmptySpec(hasActiveFilterOrSearch = true)
        OwnerRiwayatPembelianScreen(
            summary = OwnerPurchaseSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            purchases = emptyList(),
            emptyTitle = spec.title,
            emptyDescription = spec.description,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onReportClick = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "Barokah Jaya Riwayat Pembelian - Kosong Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPembelianEmptyTabletPreview() {
    TbterminalappTheme {
        val spec = ownerPurchaseEmptySpec(hasActiveFilterOrSearch = false)
        OwnerRiwayatPembelianScreen(
            summary = OwnerPurchaseSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            purchases = emptyList(),
            emptyTitle = spec.title,
            emptyDescription = spec.description,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onReportClick = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "Barokah Jaya Riwayat Pembelian - Data Ponsel (390x880)",
    widthDp = 390,
    heightDp = 880,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPembelianDataPhonePreview() {
    TbterminalappTheme {
        val sample = sampleReferencePurchases()
        val payables = sampleReferencePurchasePayables()
        val summary = computeOwnerPurchaseSummary(
            displayedPurchases = sample,
            totalLoadedSourceCount = sample.size,
            totalPurchasesFromServer = sample.size.toLong(),
            unpaidPayableByPurchaseId = payables
        )
        OwnerRiwayatPembelianDataScreen(
            summary = summary,
            purchases = sample,
            unpaidPayableByPurchaseId = payables,
            sortLabel = "Urut: Terbaru",
            footerStatus = PelangganFooterStatus.END_OF_LIST,
            hasMorePages = false,
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onReportClick = {},
            onPurchaseClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "Barokah Jaya Riwayat Pembelian - Data Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPembelianDataTabletPreview() {
    TbterminalappTheme {
        val sample = sampleReferencePurchases()
        val payables = sampleReferencePurchasePayables()
        val summary = computeOwnerPurchaseSummary(
            displayedPurchases = sample,
            totalLoadedSourceCount = sample.size,
            totalPurchasesFromServer = sample.size.toLong(),
            unpaidPayableByPurchaseId = payables
        )
        OwnerRiwayatPembelianDataScreen(
            summary = summary,
            purchases = sample,
            unpaidPayableByPurchaseId = payables,
            sortLabel = "Urut: Terbaru",
            footerStatus = PelangganFooterStatus.END_OF_LIST,
            hasMorePages = false,
            isSearchExpanded = false,
            searchQuery = "",
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onReportClick = {},
            onPurchaseClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}
