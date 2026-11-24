package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.ui.purchasehistory.PurchaseHistoryUiState
import com.tbterminal.app.ui.purchasehistory.PurchaseSortOrder
import com.tbterminal.app.ui.purchasehistory.todayPurchaseDateString
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class OwnerRiwayatPembelianBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pureFormattersMatchSpecForFullCompactAndDates() {
        assertEquals("Rp 0", formatRupiahWhole(BigDecimal.ZERO))
        assertEquals("Rp 850.000", formatRupiahWhole(BigDecimal("850000")))
        assertEquals("Rp 2.900.000", formatRupiahWhole(BigDecimal("2900000")))
        assertEquals("Rp 12.500.000", formatRupiahWhole(BigDecimal("12500000")))
        assertEquals("Rp 1.200.000.000", formatRupiahWhole(BigDecimal("1200000000")))

        assertEquals("Rp 0", formatSalesColumnCompactRupiah(BigDecimal.ZERO))
        assertEquals("Rp 850 rb", formatSalesColumnCompactRupiah(BigDecimal("850000")))
        assertEquals("Rp 2,9 jt", formatSalesColumnCompactRupiah(BigDecimal("2900000")))
        assertEquals("Rp 12,5 jt", formatSalesColumnCompactRupiah(BigDecimal("12500000")))
        assertEquals("Rp 1,2 M", formatSalesColumnCompactRupiah(BigDecimal("1200000000")))

        assertEquals("0", formatSalesHeroAmount(BigDecimal.ZERO))
        assertEquals("13.130.000", formatSalesHeroAmount(BigDecimal("13130000")))

        // 11:32 UTC = 18:32 WIB
        assertEquals("23 Nov, 18:32", formatPurchaseShortDateTime("2026-11-23T11:32:00Z", currentYear = 2026))
        assertEquals("23 Nov 2025, 18:32", formatPurchaseShortDateTime("2025-11-23T11:32:00Z", currentYear = 2026))
    }

    @Test
    fun purchaseSummaryMaintainsTotalEqualsPaidPlusDebt() {
        val state = PurchaseHistoryUiState(
            purchases = listOf(
                PurchaseSummary(
                    id = "p1",
                    supplierId = "s1",
                    supplierName = "PT Semen Indonesia",
                    invoiceNo = "INV-001",
                    total = BigDecimal("850000"),
                    receivedAt = "2026-11-23T08:00:00Z",
                    createdAt = "2026-11-23T08:00:00Z"
                ),
                PurchaseSummary(
                    id = "p2",
                    supplierId = "s2",
                    supplierName = "CV Baja Mandiri",
                    invoiceNo = "INV-002",
                    total = BigDecimal("1000000"),
                    receivedAt = "2026-11-23T09:00:00Z",
                    createdAt = "2026-11-23T09:00:00Z"
                )
            ),
            selectedPeriodType = "Semua",
            totalPurchases = 2L,
            unpaidPayableByPurchaseId = mapOf("p2" to BigDecimal("600000"))
        )

        val summary = state.toOwnerPurchaseSummary()
        assertEquals(BigDecimal("1850000"), summary.totalAmount)
        assertEquals(BigDecimal("1250000"), summary.paidTotal)
        assertEquals(BigDecimal("600000"), summary.debtTotal)
        assertEquals(summary.totalAmount, summary.paidTotal.add(summary.debtTotal))
        assertEquals(2L, summary.totalNotes)
    }

    @Test
    fun hasActivePurchaseFilterDistinguishesDefaultTodayVsFilteredSearch() {
        val today = todayPurchaseDateString()
        val defaultState = PurchaseHistoryUiState(
            purchases = emptyList(),
            isLoading = false,
            selectedSupplierId = null,
            selectedPeriodType = "Hari Ini",
            startDate = today,
            endDate = today,
            searchQuery = ""
        )
        assertFalse(hasActivePurchaseFilterOrSearch(defaultState, today))

        val allResetState = defaultState.copy(selectedPeriodType = "Semua", startDate = null, endDate = null)
        assertFalse(hasActivePurchaseFilterOrSearch(allResetState, today))

        assertTrue(hasActivePurchaseFilterOrSearch(defaultState.copy(searchQuery = "semen"), today))
        assertTrue(hasActivePurchaseFilterOrSearch(defaultState.copy(selectedSupplierId = "sup-1"), today))
        assertTrue(hasActivePurchaseFilterOrSearch(defaultState.copy(selectedPeriodType = "Minggu Ini"), today))
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneDefaultEmptyStateMatchesSalesSpecAndHasNoBottomNavOrExtraButtons() {
        var backClicks = 0
        var searchClicks = 0
        var filterClicks = 0
        var reportClicks = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPembelianRoute(
                    uiState = PurchaseHistoryUiState(
                        purchases = emptyList(),
                        isLoading = false,
                        totalPurchases = 0L
                    ),
                    isSearchExpanded = false,
                    showFilterPopup = false,
                    showSummaryPopup = false,
                    onOpenSearch = { searchClicks++ },
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = { filterClicks++ },
                    onDismissFilter = {},
                    onApplyFilter = { _, _, _, _, _ -> },
                    onOpenSummary = { reportClicks++ },
                    onDismissSummary = {},
                    onRefresh = {},
                    onShowDetail = {},
                    onDismissDetail = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = { backClicks++ },
                    onDashboardClick = {},
                    onProductsClick = {},
                    onReceivablesClick = {},
                    onSettingsClick = {},
                    onDismissError = {}
                )
            }
        }

        // Secondary page hides bottom nav
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-TRANSACTIONS").assertDoesNotExist()

        // Exact header, summary, and empty result copy mirroring Riwayat Penjualan
        composeRule.onNodeWithText("Riwayat Pembelian").assertIsDisplayed()
        composeRule.onNodeWithText("Total pembelian").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-summary-total-amount").assertTextEquals("0")
        composeRule.onNodeWithTag("purchase-summary-count").assertTextEquals("0 nota")
        composeRule.onNodeWithTag("purchase-summary-paid").assertTextEquals("Rp 0")
        composeRule.onNodeWithTag("purchase-summary-debt").assertTextEquals("Rp 0")
        composeRule.onNodeWithText("Daftar pembelian").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-history-sort-label").assertTextEquals("Urut: Terbaru")
        composeRule.onNodeWithText("Belum ada transaksi pembelian").assertIsDisplayed()
        composeRule.onNodeWithText("Pembelian tidak ditemukan").assertDoesNotExist()
        composeRule.onNodeWithText("Catat").assertDoesNotExist()
        composeRule.onNodeWithText("Reset Filter").assertDoesNotExist()

        // Verify touch target dimensions (48x48 back, 44x48 action buttons)
        val backBounds = composeRule.onNodeWithTag("purchase-history-back-btn").getBoundsInRoot()
        assertEquals(48.dp, backBounds.right - backBounds.left)
        assertEquals(48.dp, backBounds.bottom - backBounds.top)

        listOf(
            "purchase-history-toggle-search",
            "purchase-history-open-filters",
            "purchase-history-summary"
        ).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getBoundsInRoot()
            assertEquals(44.dp, bounds.right - bounds.left)
            assertEquals(48.dp, bounds.bottom - bounds.top)
        }

        composeRule.onNodeWithTag("purchase-history-back-btn").performClick()
        composeRule.onNodeWithTag("purchase-history-toggle-search").performClick()
        composeRule.onNodeWithTag("purchase-history-open-filters").performClick()
        composeRule.onNodeWithTag("purchase-history-summary").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, searchClicks)
        assertEquals(1, filterClicks)
        assertEquals(1, reportClicks)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneFilteredEmptyStateShowsNotFoundTitleAndDescriptionWithoutResetButton() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPembelianRoute(
                    uiState = PurchaseHistoryUiState(
                        purchases = emptyList(),
                        isLoading = false,
                        selectedSupplierId = "sup-1",
                        totalPurchases = 0L
                    ),
                    isSearchExpanded = false,
                    showFilterPopup = false,
                    showSummaryPopup = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onDismissFilter = {},
                    onApplyFilter = { _, _, _, _, _ -> },
                    onOpenSummary = {},
                    onDismissSummary = {},
                    onRefresh = {},
                    onShowDetail = {},
                    onDismissDetail = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = {},
                    onDashboardClick = {},
                    onProductsClick = {},
                    onReceivablesClick = {},
                    onSettingsClick = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithText("Pembelian tidak ditemukan").assertIsDisplayed()
        composeRule.onNodeWithText("Tidak ada pembelian yang cocok dengan filter atau kata kunci saat ini.").assertIsDisplayed()
        composeRule.onNodeWithText("Belum ada transaksi pembelian").assertDoesNotExist()
        composeRule.onNodeWithText("Reset Filter").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletEmptyStateKeepsNavRailWithTransaksiActive() {
        val navSelections = mutableListOf<DashboardNavDestination>()
        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPembelianScreen(
                    summary = OwnerPurchaseSummaryUiState(
                        totalAmount = BigDecimal("12500000"),
                        totalNotes = 14L,
                        paidTotal = BigDecimal("10000000"),
                        debtTotal = BigDecimal("2500000")
                    ),
                    sortLabel = purchaseSortOrderShortLabel(PurchaseSortOrder.HIGHEST_AMOUNT),
                    isSearchExpanded = false,
                    searchQuery = "",
                    isLoading = false,
                    errorMessage = null,
                    purchases = emptyList(),
                    emptyTitle = "Pembelian tidak ditemukan",
                    emptyDescription = "Tidak ada pembelian yang cocok dengan filter atau kata kunci saat ini.",
                    onBack = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onReportClick = {},
                    onNavSelect = { navSelections.add(it) },
                    autoFocusSearch = false
                )
            }
        }

        composeRule.onNodeWithTag("nav-TRANSACTIONS").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-summary-total-amount").assertTextEquals("12.500.000")
        composeRule.onNodeWithTag("purchase-summary-count").assertTextEquals("14 nota")
        composeRule.onNodeWithTag("purchase-summary-paid").assertTextEquals("Rp 10 jt")
        composeRule.onNodeWithTag("purchase-summary-debt").assertTextEquals("Rp 2,5 jt")
        composeRule.onNodeWithTag("purchase-history-sort-label").assertTextEquals("Urut: Terbesar")

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(listOf(DashboardNavDestination.BERANDA), navSelections)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDataStateRendersPelangganStyleLazyListWithoutPageButtons() {
        val clickedIds = mutableListOf<String>()
        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPembelianRoute(
                    uiState = PurchaseHistoryUiState(
                        purchases = sampleReferencePurchases(),
                        selectedPeriodType = "Semua",
                        startDate = null,
                        endDate = null,
                        page = 1,
                        totalPages = 1,
                        totalPurchases = 6L,
                        isLoading = false,
                        unpaidPayableByPurchaseId = sampleReferencePurchasePayables()
                    ),
                    isSearchExpanded = false,
                    showFilterPopup = false,
                    showSummaryPopup = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onDismissFilter = {},
                    onApplyFilter = { _, _, _, _, _ -> },
                    onOpenSummary = {},
                    onDismissSummary = {},
                    onRefresh = {},
                    onShowDetail = { clickedIds.add(it) },
                    onDismissDetail = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = {},
                    onDashboardClick = {},
                    onProductsClick = {},
                    onReceivablesClick = {},
                    onSettingsClick = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-TRANSACTIONS").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-purchase-lazy-list").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-row-p1").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-row-p2").assertIsDisplayed()
        composeRule.onNodeWithText("CV Sinar Baja").assertIsDisplayed()
        composeRule.onNodeWithText("BM-20261122-003 \u00B7 Hutang").assertIsDisplayed()
        composeRule.onNodeWithText("BM-20261123-001 \u00B7 Lunas").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 380.000").assertIsDisplayed()
        composeRule.onNodeWithText("23 Nov, 18:32").assertIsDisplayed()
        composeRule.onNodeWithText("Semua pembelian sudah ditampilkan").assertIsDisplayed()
        composeRule.onNodeWithText("Sebelumnya").assertDoesNotExist()
        composeRule.onNodeWithText("Berikutnya").assertDoesNotExist()
        composeRule.onNodeWithText("Belum lunas").assertDoesNotExist()

        composeRule.onNodeWithTag("purchase-row-p2").performClick()
        assertEquals(listOf("p2"), clickedIds)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun edgeCasesLongSupplierMissingSupplierAndLargeNominalDoNotTruncateInvoice() {
        val edgePurchases = listOf(
            PurchaseSummary(
                id = "e1",
                supplierId = "s1",
                supplierName = "PT Distributor Bahan Bangunan Nusantara Sejahtera Abadi Tbk",
                invoiceNo = "BM-20261123-001",
                total = BigDecimal("125000000"),
                receivedAt = "2026-11-23T11:32:00Z",
                createdAt = "2026-11-23T11:32:00Z"
            ),
            PurchaseSummary(
                id = "e2",
                supplierId = "",
                supplierName = "",
                invoiceNo = "BM-20251201-009",
                total = BigDecimal("450000"),
                receivedAt = "2025-12-01T04:15:00Z",
                createdAt = "2025-12-01T04:15:00Z"
            )
        )
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.4f)
            ) {
                TbterminalappTheme {
                    OwnerRiwayatPembelianDataScreen(
                        summary = computeOwnerPurchaseSummary(
                            displayedPurchases = edgePurchases,
                            totalLoadedSourceCount = edgePurchases.size,
                            totalPurchasesFromServer = 2L,
                            unpaidPayableByPurchaseId = mapOf("e2" to BigDecimal("450000"))
                        ),
                        purchases = edgePurchases,
                        unpaidPayableByPurchaseId = mapOf("e2" to BigDecimal("450000")),
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
        }

        composeRule.onNodeWithText("Tanpa Supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 125.000.000").assertIsDisplayed()
        composeRule.onNodeWithText("BM-20261123-001 \u00B7 Lunas").assertIsDisplayed()
        composeRule.onNodeWithText("BM-20251201-009 \u00B7 Hutang").assertIsDisplayed()
        composeRule.onAllNodesWithText(",00", substring = true).assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Lunas", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Hutang", useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun lazyLoadingAccumulatesPagesOnScrollAndSupportsRetryOnLoadMoreError() {
        val page1 = (1..10).map { i ->
            PurchaseSummary(
                id = "p$i",
                supplierId = "s$i",
                supplierName = "Supplier $i",
                invoiceNo = "INV-00$i",
                total = BigDecimal("100000"),
                receivedAt = "2026-11-23T0${(9 - (i % 10)).coerceAtLeast(0)}:00:00Z",
                createdAt = "2026-11-23T08:00:00Z"
            )
        }
        val page2 = listOf(
            PurchaseSummary(
                id = "p11",
                supplierId = "s11",
                supplierName = "Supplier Sebelas",
                invoiceNo = "INV-011",
                total = BigDecimal("250000"),
                receivedAt = "2026-11-22T08:00:00Z",
                createdAt = "2026-11-22T08:00:00Z"
            )
        )

        var state by mutableStateOf(
            PurchaseHistoryUiState(
                purchases = page1,
                selectedPeriodType = "Semua",
                startDate = null,
                endDate = null,
                page = 1,
                totalPages = 2,
                totalPurchases = 11L,
                isLoading = false
            )
        )
        var loadNextCount = 0
        val retryPages = mutableListOf<Int>()

        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPembelianRoute(
                    uiState = state,
                    isSearchExpanded = false,
                    showFilterPopup = false,
                    showSummaryPopup = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onDismissFilter = {},
                    onApplyFilter = { _, _, _, _, _ -> },
                    onOpenSummary = {},
                    onDismissSummary = {},
                    onRefresh = {},
                    onShowDetail = {},
                    onDismissDetail = {},
                    onLoadNextPage = { loadNextCount++ },
                    onRetryLoadMore = { retryPages.add(it) },
                    onBack = {},
                    onDashboardClick = {},
                    onProductsClick = {},
                    onReceivablesClick = {},
                    onSettingsClick = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithTag("owner-purchase-lazy-list").performScrollToIndex(9)
        composeRule.waitForIdle()
        assertEquals(1, loadNextCount)

        // Simulate page 2 load error -> footer displays Coba lagi and retries page 2
        state = state.copy(page = 2, isLoading = false, errorMessage = "Koneksi terputus")
        composeRule.onNodeWithTag("owner-purchase-lazy-list").performScrollToIndex(10)
        composeRule.onNodeWithTag("purchase-footer-retry").assertIsDisplayed().performClick()
        assertEquals(listOf(2), retryPages)

        // Simulate page 2 success -> both page 1 and page 2 accumulated & end footer displayed
        state = state.copy(purchases = page2, page = 2, isLoading = false, errorMessage = null)
        composeRule.onNodeWithTag("owner-purchase-lazy-list").performScrollToIndex(11)
        composeRule.onNodeWithTag("purchase-row-p11").assertIsDisplayed()
        composeRule.onNodeWithText("Semua pembelian sudah ditampilkan").assertIsDisplayed()
        composeRule.onNode(hasTestTag("purchase-row-p1")).assertExists()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun narrow360AndLargeFontKeepTitleAndHeroNumbersSingleLine() {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.4f)
            ) {
                TbterminalappTheme {
                    OwnerRiwayatPembelianScreen(
                        summary = OwnerPurchaseSummaryUiState(
                            totalAmount = BigDecimal("12500000000"),
                            totalNotes = 999L,
                            paidTotal = BigDecimal("11250000000"),
                            debtTotal = BigDecimal("1250000000")
                        ),
                        sortLabel = "Urut: Terbaru",
                        isSearchExpanded = false,
                        searchQuery = "",
                        isLoading = false,
                        errorMessage = null,
                        purchases = emptyList(),
                        emptyTitle = "Belum ada transaksi pembelian",
                        emptyDescription = null,
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
        }

        val titleBounds = composeRule.onNodeWithText("Riwayat Pembelian").getBoundsInRoot()
        assertTrue(titleBounds.top >= 56.dp)
        assertTrue((titleBounds.bottom - titleBounds.top) < 42.dp)

        val heroBounds = composeRule.onNodeWithTag("purchase-summary-total-amount").getBoundsInRoot()
        assertTrue((heroBounds.bottom - heroBounds.top) < 64.dp)

        val paidBounds = composeRule.onNodeWithTag("purchase-summary-paid").getBoundsInRoot()
        assertTrue((paidBounds.bottom - paidBounds.top) < 36.dp)
    }
}
