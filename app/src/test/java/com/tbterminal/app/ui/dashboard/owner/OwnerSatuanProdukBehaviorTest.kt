package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.products.ProductUnitUiState
import com.tbterminal.app.ui.theme.TbterminalappTheme
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
class OwnerSatuanProdukBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun phoneReferenceDataScreenMatchesSpecAndOmitsDeletedElements() {
        var clickedUnit: ProductUnit? = null
        var addClicked = 0
        var searchOpened = 0
        var backClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerSatuanRoute(
                    uiState = ProductUnitUiState(
                        units = sampleReferenceSatuanUnits(),
                        page = 1,
                        totalPages = 1,
                        totalUnits = 10L,
                        isLoading = false
                    ),
                    isSearchExpanded = false,
                    onOpenSearch = { searchOpened++ },
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onAddUnitClick = { addClicked++ },
                    onUnitClick = { clickedUnit = it },
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = { backClicked++ },
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {},
                    autoFocusSearch = false
                )
            }
        }

        // Header & summary without fake usage counts when backend doesn't provide them
        composeRule.onNodeWithText("Satuan produk").assertIsDisplayed()
        composeRule.onNodeWithText("Total satuan").assertIsDisplayed()
        composeRule.onNodeWithTag("satuan-total-count").assertIsDisplayed()
        composeRule.onNodeWithText("10").assertIsDisplayed()
        composeRule.onNodeWithText("satuan").assertIsDisplayed()
        composeRule.onAllNodesWithText("Dipakai produk").assertCountEquals(0)
        composeRule.onAllNodesWithText("Belum dipakai").assertCountEquals(0)

        // Deleted elements must not appear
        composeRule.onAllNodesWithContentDescription("Aksi satuan").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Filter pelanggan").assertCountEquals(0)

        // Primary CTA & section row
        composeRule.onNodeWithText("Tambah satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Semua satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Urut: Nama A\u2013Z").assertIsDisplayed()

        // Units & abbreviation badges (1-char, 2-char, 3-char, 5-char)
        composeRule.onNodeWithText("Batang").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Singkatan btg").assertIsDisplayed()
        composeRule.onNodeWithText("Galon").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Singkatan galon").assertIsDisplayed()
        composeRule.onNodeWithText("Liter").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Singkatan L").assertIsDisplayed()
        composeRule.onNodeWithText("Meter").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Singkatan m").assertIsDisplayed()
        composeRule.onAllNodesWithText("Dibuat 18 Nov 2026").assertCountEquals(7)
        composeRule.onNodeWithText("Semua satuan sudah ditampilkan").assertIsDisplayed()

        // Touch target height >= 72dp and row tap opens edit action
        val rowBounds = composeRule.onNodeWithTag("unit-row-u1").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        composeRule.onNodeWithTag("unit-row-u3").performClick()
        assertEquals("u3", clickedUnit?.id)

        composeRule.onNodeWithTag("unit-add").performClick()
        assertEquals(1, addClicked)

        composeRule.onNodeWithTag("unit-toggle-search").performClick()
        assertEquals(1, searchOpened)

        composeRule.onNodeWithTag("satuan-back-btn").performClick()
        assertEquals(1, backClicked)
    }

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun summaryOverrideDisplaysUsageBreakdownColumns() {
        composeRule.setContent {
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

        composeRule.onNodeWithText("Dipakai produk").assertIsDisplayed()
        composeRule.onNodeWithText("8 satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Belum dipakai").assertIsDisplayed()
        composeRule.onNodeWithText("2 satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Memuat satuan lainnya\u2026").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutKeepsNavRailAndScrollableList() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerSatuanDataScreen(
                    summary = SatuanSummaryUiState(totalUnits = 10L),
                    units = sampleReferenceSatuanUnits().take(6),
                    footerStatus = PelangganFooterStatus.END_OF_LIST,
                    hasMorePages = false,
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

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-STOCK").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-satuan-lazy-list").assertIsDisplayed()
        composeRule.onNodeWithText("Batang").assertIsDisplayed()
        composeRule.onNodeWithText("Liter").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun edgeCasesLongNameOverFiveCharSymbolAndSingleUnit() {
        val longNameUnit = ProductUnit(
            id = "u-long",
            name = "Sak Semen Serbaguna Kemasan Industri Besar Ekstra",
            symbol = "karung",
            createdAt = "2026-08-05T10:00:00Z"
        )
        assertEquals("karun\u2026", formatUnitAbbreviation("karung"))
        assertEquals("5 Agt 2026", formatSatuanCreatedDate("2026-08-05T10:00:00Z"))

        composeRule.setContent {
            TbterminalappTheme {
                OwnerSatuanRoute(
                    uiState = ProductUnitUiState(
                        units = listOf(longNameUnit),
                        page = 1,
                        totalPages = 1,
                        totalUnits = 1L,
                        isLoading = false
                    ),
                    isSearchExpanded = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onAddUnitClick = {},
                    onUnitClick = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {},
                    autoFocusSearch = false
                )
            }
        }

        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Singkatan karung").assertIsDisplayed()
        composeRule.onNodeWithText("Sak Semen Serbaguna Kemasan Industri Besar Ekstra").assertIsDisplayed()
        composeRule.onNodeWithText("Dibuat 5 Agt 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Semua satuan sudah ditampilkan").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun pagingAndSearchFlowAccumulateUnitsAndShowNotFoundState() {
        val page1 = (1..7).map { i ->
            ProductUnit("u$i", "Satuan %03d".format(i), "s$i", "2026-11-18T10:00:00Z")
        }
        val page2 = (8..14).map { i ->
            ProductUnit("u$i", "Satuan %03d".format(i), "s$i", "2026-11-18T10:00:00Z")
        }
        var nextPageCalls = 0
        var uiState by mutableStateOf(
            ProductUnitUiState(
                units = page1,
                page = 1,
                totalPages = 15,
                totalUnits = 105L,
                isLoading = false
            )
        )
        var searchExpanded by mutableStateOf(false)

        composeRule.setContent {
            TbterminalappTheme {
                OwnerSatuanRoute(
                    uiState = uiState,
                    isSearchExpanded = searchExpanded,
                    onOpenSearch = { searchExpanded = true },
                    onCloseSearch = { searchExpanded = false },
                    onSearchChanged = { query -> uiState = uiState.copy(searchQuery = query) },
                    onAddUnitClick = {},
                    onUnitClick = {},
                    onRefresh = {},
                    onLoadNextPage = { nextPageCalls++ },
                    onRetryLoadMore = {},
                    onBack = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {},
                    autoFocusSearch = false
                )
            }
        }

        composeRule.waitForIdle()
        assertTrue(nextPageCalls >= 1)

        // Simulate loading next page and scroll to footer
        uiState = uiState.copy(isLoading = true)
        composeRule.onNodeWithTag("owner-satuan-lazy-list")
            .performScrollToNode(hasText("Memuat satuan lainnya\u2026"))
        composeRule.onNodeWithText("Memuat satuan lainnya\u2026").assertIsDisplayed()

        // Simulate page 2 loaded and scroll to newly appended item
        uiState = uiState.copy(units = page2, page = 2, isLoading = false)
        composeRule.onNodeWithText("105").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-satuan-lazy-list")
            .performScrollToNode(hasText("Satuan 008"))
        composeRule.onNodeWithText("Satuan 008").assertIsDisplayed()

        // Search missing query -> empty search not found state
        searchExpanded = true
        composeRule.onNodeWithTag("unit-search-input").performTextInput("zzzz")
        composeRule.onNodeWithText("\"zzzz\" tidak ditemukan").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi", fontScale = 1.5f)
    fun smallScreenWithLargeSystemFontRendersHeaderAndRowsCleanly() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerSatuanRoute(
                    uiState = ProductUnitUiState(
                        units = sampleReferenceSatuanUnits().take(3),
                        page = 1,
                        totalPages = 1,
                        totalUnits = 3L,
                        isLoading = false
                    ),
                    isSearchExpanded = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onAddUnitClick = {},
                    onUnitClick = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {},
                    autoFocusSearch = false
                )
            }
        }

        composeRule.onNodeWithText("Satuan produk").assertIsDisplayed()
        composeRule.onNodeWithText("Tambah satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Batang").assertIsDisplayed()
    }
}
