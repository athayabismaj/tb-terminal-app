package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.offline.LocalPendingTransactionsCard
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryUiState
import com.tbterminal.app.ui.salestransactions.SalesHistoryError
import com.tbterminal.app.ui.salestransactions.SalesHistoryLoading
import com.tbterminal.app.ui.salestransactions.SalesHistoryPagination
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import com.tbterminal.app.ui.salestransactions.SalesSummaryBottomSheet
import com.tbterminal.app.ui.salestransactions.SalesTransactionMobileItem
import com.tbterminal.app.ui.salestransactions.SalesTransactionTable
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.time.LocalDate
import java.time.ZoneId

private val SALES_WIB_ZONE = ZoneId.of("Asia/Jakarta")

@Immutable
internal data class OwnerSalesEmptySpec(
    val title: String,
    val description: String?
)

internal fun ownerSalesEmptySpec(
    hasActiveFilterOrSearch: Boolean
): OwnerSalesEmptySpec = if (hasActiveFilterOrSearch) {
    OwnerSalesEmptySpec(
        title = "Transaksi tidak ditemukan",
        description = "Tidak ada transaksi yang cocok dengan filter atau kata kunci saat ini."
    )
} else {
    // TODO: Desain final keadaan belum ada transaksi penjualan sama sekali (tanpa filter) belum dibuat; sementara gunakan layout yang sama tanpa menambah komponen baru.
    OwnerSalesEmptySpec(
        title = "Belum ada transaksi penjualan",
        description = null
    )
}

@Composable
internal fun OwnerRiwayatPenjualanRoute(
    uiState: AdminTransactionHistoryUiState,
    isSearchExpanded: Boolean,
    showFilterPopup: Boolean,
    showSummaryPopup: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onDismissFilter: () -> Unit,
    onApplyFilter: (String, String, String?, String?, String?, SalesSortOrder) -> Unit,
    onResetAllFilters: () -> Unit,
    onOpenSummary: () -> Unit,
    onDismissSummary: () -> Unit,
    onSyncClick: (Long) -> Unit,
    onSyncAllClick: () -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onBack: (() -> Unit)?,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismissError: () -> Unit
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

    val todayStr = remember { LocalDate.now(SALES_WIB_ZONE).toString() }
    val isNonDefaultDate = (uiState.selectedPreset != "Hari ini" && uiState.selectedPreset != "Semua") ||
        (uiState.selectedPreset != "Semua" && ((uiState.startDate != null && uiState.startDate != todayStr) || (uiState.endDate != null && uiState.endDate != todayStr)))
    val hasActiveFilterOrSearch = uiState.query.isNotBlank() ||
        uiState.statusFilter != "Semua" ||
        uiState.paymentMethodFilter != "Semua" ||
        isNonDefaultDate

    val emptySpec = remember(hasActiveFilterOrSearch) {
        ownerSalesEmptySpec(hasActiveFilterOrSearch = hasActiveFilterOrSearch)
    }
    val summary = remember(uiState.transactions, uiState.total) {
        uiState.toOwnerSalesSummary()
    }
    val sortLabel = remember(uiState.sortOrder) {
        salesSortOrderShortLabel(uiState.sortOrder)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            OwnerRiwayatPenjualanScreen(
                summary = summary,
                sortLabel = sortLabel,
                isSearchExpanded = isSearchExpanded,
                searchQuery = uiState.query,
                isLoading = uiState.isLoading && uiState.transactions.isEmpty(),
                errorMessage = uiState.error.takeIf { uiState.transactions.isEmpty() },
                transactions = uiState.transactions,
                emptyTitle = emptySpec.title,
                emptyDescription = emptySpec.description,
                onBack = navigateBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = {
                    // TODO: Ikon Laporan membuka tujuan ikon dokumen biasa di AdminTransactionHistoryScreen saat ini (SalesSummaryBottomSheet); onReportsClick (AppRoute.Reports) belum dipakai di top bar. Konfirmasi bila perlu diarahkan ke AppRoute.Reports.
                    onOpenSummary()
                },
                onNavSelect = ::handleNavSelect,
                dataContent = { isTablet ->
                    OwnerSalesDataFallbackContent(
                        state = uiState,
                        compact = !isTablet,
                        onSyncClick = onSyncClick,
                        onSyncAllClick = onSyncAllClick,
                        onReceiptClick = onReceiptClick,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage
                    )
                },
                onRetry = onRefresh
            )
        }

        if (uiState.transactions.isNotEmpty()) {
            TopFloatingCapsule(
                message = null,
                error = uiState.error,
                onDismiss = onDismissError,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(99f)
            )
        }
    }

    if (showFilterPopup) {
        OwnerSalesFilterModal(
            state = uiState,
            onDismiss = onDismissFilter,
            onApplyFilter = { status, payment, start, end, preset, sortOrder ->
                onApplyFilter(status, payment, start, end, preset, sortOrder)
                onDismissFilter()
            }
        )
    }

    if (showSummaryPopup) {
        SalesSummaryBottomSheet(
            state = uiState,
            onDismiss = onDismissSummary
        )
    }
}

@Composable
internal fun OwnerRiwayatPenjualanScreen(
    summary: OwnerSalesSummaryUiState,
    sortLabel: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    errorMessage: String?,
    transactions: List<CashTransaction>,
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
        title = "Riwayat Penjualan",
        panelTitle = "Daftar transaksi",
        sortLabel = sortLabel,
        sortLabelTestTag = "sales-history-sort-label",
        searchPlaceholder = "Cari struk, pelanggan, atau kasir...",
        backTestTag = "sales-history-back-btn",
        searchTestTag = "sales-history-toggle-search",
        filterTestTag = "sales-history-open-filters",
        reportTestTag = "sales-history-report-btn",
        searchBackTestTag = "sales-history-search-back",
        searchInputTestTag = "sales-history-search",
        searchClearTestTag = "sales-history-search-clear",
        searchContentDescription = "Cari transaksi",
        filterContentDescription = "Filter transaksi",
        reportContentDescription = "Laporan penjualan",
        isSearchExpanded = isSearchExpanded,
        searchQuery = searchQuery,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isEmpty = transactions.isEmpty(),
        emptyTitle = emptyTitle,
        emptyDescription = emptyDescription,
        onBack = onBack,
        onOpenSearch = onOpenSearch,
        onCloseSearch = onCloseSearch,
        onSearchChanged = onSearchChanged,
        onOpenFilter = onOpenFilter,
        onReportClick = onReportClick,
        onNavSelect = onNavSelect,
        heroSummary = { isTablet -> SalesSummary(summary = summary, isTablet = isTablet) },
        loadingContent = { isTablet -> SalesHistoryLoading(compact = !isTablet) },
        errorContent = { message -> SalesHistoryError(message = message, onRetry = onRetry) },
        dataContent = dataContent,
        modifier = modifier,
        autoFocusSearch = autoFocusSearch
    )
}

/**
 * TODO: Keadaan berisi data (daftar transaksi) akan dikirim desain terpisah; sementara tampilkan
 * daftar transaksi yang sudah ada di dalam panel putih di bawah header baru.
 */
@Composable
private fun OwnerSalesDataFallbackContent(
    state: AdminTransactionHistoryUiState,
    compact: Boolean,
    onSyncClick: (Long) -> Unit,
    onSyncAllClick: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (
            state.localPendingTransactions.isNotEmpty() ||
            !state.localSyncMessage.isNullOrBlank() ||
            !state.bulkSyncProgressMessage.isNullOrBlank() ||
            state.isBulkSyncing
        ) {
            LocalPendingTransactionsCard(
                transactions = state.localPendingTransactions,
                message = state.localSyncMessage,
                bulkProgressMessage = state.bulkSyncProgressMessage,
                isBulkSyncing = state.isBulkSyncing,
                onSyncClick = onSyncClick,
                onSyncAllClick = onSyncAllClick
            )
        }

        if (compact) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales-transaction-grouped-list"),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
                shadowElevation = 1.dp
            ) {
                Column {
                    state.transactions.forEachIndexed { index, transaction ->
                        SalesTransactionMobileItem(transaction, onReceiptClick)
                        if (index < state.transactions.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        } else {
            SalesTransactionTable(state.transactions, onReceiptClick)
        }

        SalesHistoryPagination(state, compact, onPreviousPage, onNextPage)
    }
}

@Preview(
    name = "Barokah Jaya Riwayat Penjualan - Hasil Kosong Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPenjualanEmptyPhonePreview() {
    TbterminalappTheme {
        val spec = ownerSalesEmptySpec(hasActiveFilterOrSearch = true)
        OwnerRiwayatPenjualanScreen(
            summary = OwnerSalesSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            transactions = emptyList(),
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
    name = "Barokah Jaya Riwayat Penjualan - Hasil Kosong Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerRiwayatPenjualanEmptyTabletPreview() {
    TbterminalappTheme {
        val spec = ownerSalesEmptySpec(hasActiveFilterOrSearch = true)
        OwnerRiwayatPenjualanScreen(
            summary = OwnerSalesSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            transactions = emptyList(),
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
