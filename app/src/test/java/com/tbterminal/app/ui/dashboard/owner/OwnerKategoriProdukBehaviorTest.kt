package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.products.ProductCategoryUiState
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
class OwnerKategoriProdukBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun phoneReferenceDataScreenMatchesSatuanLayoutAndOmitsDeletedElements() {
        var clickedCategory: ProductCategory? = null
        var addClicked = 0
        var searchOpened = 0
        var backClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerKategoriRoute(
                    uiState = ProductCategoryUiState(
                        categories = sampleReferenceKategoriList(),
                        page = 1,
                        totalPages = 1,
                        totalCategories = 7L,
                        isLoading = false
                    ),
                    isSearchExpanded = false,
                    onOpenSearch = { searchOpened++ },
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onAddCategoryClick = { addClicked++ },
                    onCategoryClick = { clickedCategory = it },
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

        composeRule.onNodeWithText("Kategori produk").assertIsDisplayed()
        composeRule.onNodeWithText("Total kategori").assertIsDisplayed()
        composeRule.onNodeWithTag("kategori-total-count").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("kategori").assertIsDisplayed()

        // Deleted elements must not appear
        composeRule.onAllNodesWithContentDescription("Aksi kategori").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Filter pelanggan").assertCountEquals(0)

        composeRule.onNodeWithText("Tambah kategori").assertIsDisplayed()
        composeRule.onNodeWithText("Semua kategori").assertIsDisplayed()
        composeRule.onNodeWithText("Urut: Nama A\u2013Z").assertIsDisplayed()

        composeRule.onNodeWithText("Atap & Genteng").assertIsDisplayed()
        composeRule.onNodeWithText("Semen & Perekat").assertIsDisplayed()
        composeRule.onAllNodesWithText("Dibuat 18 Nov 2026").assertCountEquals(7)
        composeRule.onNodeWithText("Semua kategori sudah ditampilkan").assertIsDisplayed()

        val rowBounds = composeRule.onNodeWithTag("category-row-k1").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        composeRule.onNodeWithTag("category-row-k3").performClick()
        assertEquals("k3", clickedCategory?.id)

        composeRule.onNodeWithTag("category-add").performClick()
        assertEquals(1, addClicked)

        composeRule.onNodeWithTag("category-toggle-search").performClick()
        assertEquals(1, searchOpened)

        composeRule.onNodeWithTag("kategori-back-btn").performClick()
        assertEquals(1, backClicked)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutKeepsNavRailAndScrollableList() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerKategoriDataScreen(
                    summary = KategoriSummaryUiState(totalCategories = 7L),
                    categories = sampleReferenceKategoriList().take(6),
                    footerStatus = PelangganFooterStatus.END_OF_LIST,
                    hasMorePages = false,
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

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-STOCK").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-kategori-lazy-list").assertIsDisplayed()
        composeRule.onNodeWithText("Atap & Genteng").assertIsDisplayed()
        composeRule.onNodeWithText("Pipa & Sanitasi").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun pagingAndSearchFlowAccumulateCategoriesAndShowNotFoundState() {
        val page1 = (1..7).map { i ->
            ProductCategory("k$i", "Kategori %03d".format(i), "2026-11-18T10:00:00Z")
        }
        val page2 = (8..14).map { i ->
            ProductCategory("k$i", "Kategori %03d".format(i), "2026-11-18T10:00:00Z")
        }
        var nextPageCalls = 0
        var uiState by mutableStateOf(
            ProductCategoryUiState(
                categories = page1,
                page = 1,
                totalPages = 15,
                totalCategories = 105L,
                isLoading = false
            )
        )
        var searchExpanded by mutableStateOf(false)

        composeRule.setContent {
            TbterminalappTheme {
                OwnerKategoriRoute(
                    uiState = uiState,
                    isSearchExpanded = searchExpanded,
                    onOpenSearch = { searchExpanded = true },
                    onCloseSearch = { searchExpanded = false },
                    onSearchChanged = { query -> uiState = uiState.copy(searchQuery = query) },
                    onAddCategoryClick = {},
                    onCategoryClick = {},
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

        uiState = uiState.copy(isLoading = true)
        composeRule.onNodeWithTag("owner-kategori-lazy-list")
            .performScrollToNode(hasText("Memuat kategori lainnya\u2026"))
        composeRule.onNodeWithText("Memuat kategori lainnya\u2026").assertIsDisplayed()

        uiState = uiState.copy(categories = page2, page = 2, isLoading = false)
        composeRule.onNodeWithText("105").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-kategori-lazy-list")
            .performScrollToNode(hasText("Kategori 008"))
        composeRule.onNodeWithText("Kategori 008").assertIsDisplayed()

        searchExpanded = true
        composeRule.onNodeWithTag("category-search-input").performTextInput("zzzz")
        composeRule.onNodeWithText("\"zzzz\" tidak ditemukan").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi", fontScale = 1.5f)
    fun smallScreenWithLargeSystemFontAndEmptyStateRenderCleanly() {
        var emptyState by mutableStateOf(true)
        composeRule.setContent {
            TbterminalappTheme {
                OwnerKategoriRoute(
                    uiState = ProductCategoryUiState(
                        categories = if (emptyState) emptyList() else sampleReferenceKategoriList().take(2),
                        page = 1,
                        totalPages = 1,
                        totalCategories = if (emptyState) 0L else 2L,
                        isLoading = false
                    ),
                    isSearchExpanded = false,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onAddCategoryClick = { emptyState = false },
                    onCategoryClick = {},
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

        composeRule.onNodeWithText("Belum ada kategori").assertIsDisplayed()
        composeRule.onNodeWithTag("category-add").performClick()
        composeRule.onNodeWithText("Atap & Genteng").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun addAndEditCategoryFormValidatesSubmitsAndDeletes() {
        val existing = ProductCategory("k3", "Cat & Pelapis", "2026-11-18T10:00:00Z")
        var saveCalls = 0
        var deletedCategory: ProductCategory? = null
        var uiState by mutableStateOf(
            ProductCategoryUiState(
                isFormVisible = true,
                isLoading = false
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahKategoriRoute(
                    uiState = uiState,
                    onNameChanged = { uiState = uiState.copy(nameInput = it, message = null) },
                    onSave = { saveCalls++ },
                    onDelete = { deletedCategory = it },
                    onCancel = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {}
                )
            }
        }

        composeRule.onNodeWithText("Tambah kategori").assertIsDisplayed()
        composeRule.onNodeWithTag("category-save").assertIsNotEnabled()
        composeRule.onAllNodesWithTag("category-delete-btn").assertCountEquals(0)

        composeRule.onNodeWithTag("category-name").performTextInput("Cat Tembok")
        composeRule.onNodeWithTag("category-save").performClick()
        assertEquals(1, saveCalls)

        // Switch to Edit mode
        uiState = ProductCategoryUiState(
            editingCategory = existing,
            nameInput = existing.name,
            isFormVisible = true,
            isLoading = false
        )

        composeRule.onNodeWithText("Edit kategori").assertIsDisplayed()
        composeRule.onNodeWithTag("category-save").assertIsNotEnabled()
        composeRule.onNodeWithTag("category-delete-btn").assertIsDisplayed()

        composeRule.onNodeWithTag("category-name").performTextReplacement("Cat & Finishing")
        composeRule.onNodeWithTag("category-save").performClick()
        assertEquals(2, saveCalls)

        composeRule.onNodeWithTag("category-delete-btn").performClick()
        composeRule.onNodeWithTag("delete-kategori-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("delete-kategori-confirm").performClick()
        assertEquals("k3", deletedCategory?.id)
    }
}
