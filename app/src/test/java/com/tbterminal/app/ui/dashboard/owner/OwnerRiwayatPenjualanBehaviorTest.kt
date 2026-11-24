package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryUiState
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
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
class OwnerRiwayatPenjualanBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactAndHeroFormattersMatchDesignSpec() {
        assertEquals("0", formatSalesHeroAmount(BigDecimal.ZERO))
        assertEquals("12.500.000", formatSalesHeroAmount(BigDecimal("12500000")))
        assertEquals("Rp 0", formatSalesColumnCompactRupiah(BigDecimal.ZERO))
        assertEquals("Rp 850 rb", formatSalesColumnCompactRupiah(BigDecimal("850000")))
        assertEquals("Rp 12,5 jt", formatSalesColumnCompactRupiah(BigDecimal("12500000")))
        assertEquals("Rp 1,2 M", formatSalesColumnCompactRupiah(BigDecimal("1200000000")))
        assertEquals("0 transaksi", formatSalesCountLabel(0L))
    }

    @Test
    fun salesSummaryMaintainsTotalEqualsPaidPlusDebtDo() {
        val state = AdminTransactionHistoryUiState(
            transactions = listOf(
                CashTransaction(
                    id = "1",
                    receiptId = "TRX-001",
                    sessionId = "s1",
                    customerId = null,
                    customerName = null,
                    type = "SALE",
                    status = "Lunas",
                    total = BigDecimal("850000"),
                    paidAmount = BigDecimal("850000"),
                    remainingAmount = BigDecimal.ZERO,
                    createdAt = "2026-11-23T08:00:00Z"
                ),
                CashTransaction(
                    id = "2",
                    receiptId = "TRX-002",
                    sessionId = "s1",
                    customerId = "c1",
                    customerName = "Pak Budi",
                    type = "SALE",
                    status = "DP",
                    total = BigDecimal("1000000"),
                    paidAmount = BigDecimal("400000"),
                    remainingAmount = BigDecimal("600000"),
                    createdAt = "2026-11-23T09:00:00Z"
                ),
                CashTransaction(
                    id = "3",
                    receiptId = "TRX-003",
                    sessionId = "s1",
                    customerId = "c2",
                    customerName = "CV Maju",
                    type = "SALE",
                    status = "Voided",
                    total = BigDecimal("500000"),
                    paidAmount = BigDecimal.ZERO,
                    remainingAmount = BigDecimal.ZERO,
                    createdAt = "2026-11-23T09:30:00Z"
                )
            ),
            total = 2L
        )

        val summary = state.toOwnerSalesSummary()
        assertEquals(BigDecimal("1850000"), summary.totalSales)
        assertEquals(BigDecimal("1250000"), summary.paidTotal)
        assertEquals(BigDecimal("600000"), summary.debtDoTotal)
        assertEquals(summary.totalSales, summary.paidTotal.add(summary.debtDoTotal))
        assertEquals(2L, summary.totalTransactions)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneFilteredEmptyStateMatchesSpecAndHasNoBottomNavOrResetButton() {
        var backClicks = 0
        var searchClicks = 0
        var filterClicks = 0
        var reportClicks = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPenjualanRoute(
                    uiState = AdminTransactionHistoryUiState(
                        transactions = emptyList(),
                        isLoading = false,
                        statusFilter = "Hutang",
                        total = 0L
                    ),
                    isSearchExpanded = false,
                    showFilterPopup = false,
                    showSummaryPopup = false,
                    onOpenSearch = { searchClicks++ },
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = { filterClicks++ },
                    onDismissFilter = {},
                    onApplyFilter = { _, _, _, _, _, _ -> },
                    onResetAllFilters = {},
                    onOpenSummary = { reportClicks++ },
                    onDismissSummary = {},
                    onSyncClick = {},
                    onSyncAllClick = {},
                    onRefresh = {},
                    onPreviousPage = {},
                    onNextPage = {},
                    onReceiptClick = {},
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

        // Exact header, summary, and empty result copy
        composeRule.onNodeWithText("Riwayat Penjualan").assertIsDisplayed()
        composeRule.onNodeWithText("Total penjualan").assertIsDisplayed()
        composeRule.onNodeWithTag("sales-summary-total-amount").assertTextEquals("0")
        composeRule.onNodeWithTag("sales-summary-count").assertTextEquals("0 transaksi")
        composeRule.onNodeWithTag("sales-summary-paid").assertTextEquals("Rp 0")
        composeRule.onNodeWithTag("sales-summary-debt").assertTextEquals("Rp 0")
        composeRule.onNodeWithText("Daftar transaksi").assertIsDisplayed()
        composeRule.onNodeWithTag("sales-history-sort-label").assertTextEquals("Urut: Terbaru")
        composeRule.onNodeWithText("Transaksi tidak ditemukan").assertIsDisplayed()
        composeRule.onNodeWithText("Tidak ada transaksi yang cocok dengan filter atau kata kunci saat ini.").assertIsDisplayed()
        composeRule.onNodeWithText("Reset filter").assertDoesNotExist()
        composeRule.onNodeWithText("Reset Filter").assertDoesNotExist()

        // Verify touch target dimensions
        val backBounds = composeRule.onNodeWithTag("sales-history-back-btn").getBoundsInRoot()
        assertEquals(48.dp, backBounds.right - backBounds.left)
        assertEquals(48.dp, backBounds.bottom - backBounds.top)

        listOf(
            "sales-history-toggle-search",
            "sales-history-open-filters",
            "sales-history-report-btn"
        ).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getBoundsInRoot()
            assertEquals(44.dp, bounds.right - bounds.left)
            assertEquals(48.dp, bounds.bottom - bounds.top)
        }

        composeRule.onNodeWithTag("sales-history-back-btn").performClick()
        composeRule.onNodeWithTag("sales-history-toggle-search").performClick()
        composeRule.onNodeWithTag("sales-history-open-filters").performClick()
        composeRule.onNodeWithTag("sales-history-report-btn").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, searchClicks)
        assertEquals(1, filterClicks)
        assertEquals(1, reportClicks)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun unfilteredEmptyStateShowsPlaceholderEmptyTextWithoutButtons() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPenjualanRoute(
                    uiState = AdminTransactionHistoryUiState(
                        transactions = emptyList(),
                        isLoading = false,
                        query = "",
                        statusFilter = "Semua",
                        paymentMethodFilter = "Semua",
                        selectedPreset = "Hari ini",
                        total = 0L
                    ),
                    isSearchExpanded = false,
                    showFilterPopup = false,
                    showSummaryPopup = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onDismissFilter = {},
                    onApplyFilter = { _, _, _, _, _, _ -> },
                    onResetAllFilters = {},
                    onOpenSummary = {},
                    onDismissSummary = {},
                    onSyncClick = {},
                    onSyncAllClick = {},
                    onRefresh = {},
                    onPreviousPage = {},
                    onNextPage = {},
                    onReceiptClick = {},
                    onBack = {},
                    onDashboardClick = {},
                    onProductsClick = {},
                    onReceivablesClick = {},
                    onSettingsClick = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithText("Belum ada transaksi penjualan").assertIsDisplayed()
        composeRule.onNodeWithText("Transaksi tidak ditemukan").assertDoesNotExist()
        composeRule.onNodeWithText("Reset filter").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletEmptyStateKeepsNavRailWithTransaksiActive() {
        var navSelections = mutableListOf<DashboardNavDestination>()
        composeRule.setContent {
            TbterminalappTheme {
                OwnerRiwayatPenjualanScreen(
                    summary = OwnerSalesSummaryUiState(
                        totalSales = BigDecimal("12500000"),
                        totalTransactions = 14L,
                        paidTotal = BigDecimal("10000000"),
                        debtDoTotal = BigDecimal("2500000")
                    ),
                    sortLabel = salesSortOrderShortLabel(SalesSortOrder.HighestAmount),
                    isSearchExpanded = false,
                    searchQuery = "",
                    isLoading = false,
                    errorMessage = null,
                    transactions = emptyList(),
                    emptyTitle = "Transaksi tidak ditemukan",
                    emptyDescription = "Tidak ada transaksi yang cocok dengan filter atau kata kunci saat ini.",
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
        composeRule.onNodeWithTag("sales-summary-total-amount").assertTextEquals("12.500.000")
        composeRule.onNodeWithTag("sales-summary-count").assertTextEquals("14 transaksi")
        composeRule.onNodeWithTag("sales-summary-paid").assertTextEquals("Rp 10 jt")
        composeRule.onNodeWithTag("sales-summary-debt").assertTextEquals("Rp 2,5 jt")
        composeRule.onNodeWithTag("sales-history-sort-label").assertTextEquals("Urut: Terbesar")

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(listOf(DashboardNavDestination.BERANDA), navSelections)
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
                    OwnerRiwayatPenjualanScreen(
                        summary = OwnerSalesSummaryUiState(
                            totalSales = BigDecimal("12500000000"),
                            totalTransactions = 999L,
                            paidTotal = BigDecimal("11250000000"),
                            debtDoTotal = BigDecimal("1250000000")
                        ),
                        sortLabel = "Urut: Terbaru",
                        isSearchExpanded = false,
                        searchQuery = "",
                        isLoading = false,
                        errorMessage = null,
                        transactions = emptyList(),
                        emptyTitle = "Transaksi tidak ditemukan",
                        emptyDescription = "Tidak ada transaksi yang cocok dengan filter atau kata kunci saat ini.",
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

        val titleBounds = composeRule.onNodeWithText("Riwayat Penjualan").getBoundsInRoot()
        assertTrue(titleBounds.top >= 56.dp)
        assertTrue((titleBounds.bottom - titleBounds.top) < 42.dp)

        val heroBounds = composeRule.onNodeWithTag("sales-summary-total-amount").getBoundsInRoot()
        assertTrue((heroBounds.bottom - heroBounds.top) < 64.dp)

        val paidBounds = composeRule.onNodeWithTag("sales-summary-paid").getBoundsInRoot()
        assertTrue((paidBounds.bottom - paidBounds.top) < 36.dp)
    }
}
