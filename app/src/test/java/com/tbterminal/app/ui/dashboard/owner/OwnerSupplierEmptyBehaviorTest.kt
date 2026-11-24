package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
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
class OwnerSupplierEmptyBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneEmptyStateMatchesPelangganDesignAndTouchTargets() {
        var backClicked = false
        var addClicked = false
        var filterClicked = false
        var isSearchExpanded by mutableStateOf(false)
        var searchQuery by mutableStateOf("")

        composeRule.setContent {
            TbterminalappTheme {
                val spec = supplierEmptyStateSpecFor(searchQuery = searchQuery, isFiltered = false)
                OwnerSupplierEmptyScreen(
                    summary = SupplierEmptySummaryUiState(),
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = searchQuery,
                    isFiltered = false,
                    onBack = { backClicked = true },
                    onOpenSearch = { isSearchExpanded = true },
                    onCloseSearch = {
                        isSearchExpanded = false
                        searchQuery = ""
                    },
                    onSearchChanged = { searchQuery = it },
                    onOpenFilter = { filterClicked = true },
                    onNavSelect = {},
                    autoFocusSearch = false,
                    panelContent = { isTablet ->
                        EmptyState(
                            icon = spec.icon,
                            iconContainerColor = spec.iconContainerColor,
                            iconTint = spec.iconTint,
                            title = spec.title,
                            description = spec.description,
                            showAddButton = true,
                            onAddCustomerClick = { addClicked = true },
                            buttonTestTag = "supplier-add",
                            buttonLabel = "Tambah supplier",
                            modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                        )
                    }
                )
            }
        }

        // Secondary page: bottom nav bar must not appear on phone
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()

        // Header & hero stats
        composeRule.onNode(hasText("Supplier") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Total supplier").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-total-count").assertIsDisplayed()
        composeRule.onNodeWithText("Aktif").assertIsDisplayed()
        composeRule.onNodeWithText("Nonaktif").assertIsDisplayed()

        // Empty state content
        composeRule.onNodeWithText("Belum ada supplier").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Tambahkan supplier untuk pencatatan transaksi pembelian, termin bayar, dan restok barang."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Tambah supplier").assertIsDisplayed()

        // Touch targets >= 48dp (button >= 56dp)
        listOf("supplier-back-btn", "supplier-toggle-search", "supplier-open-filter").forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getBoundsInRoot()
            assertTrue("$tag width < 48dp", (bounds.right - bounds.left) >= 48.dp)
            assertTrue("$tag height < 48dp", (bounds.bottom - bounds.top) >= 48.dp)
        }
        val addBounds = composeRule.onNodeWithTag("supplier-add").getBoundsInRoot()
        assertTrue((addBounds.bottom - addBounds.top) >= 56.dp)

        // Actions
        composeRule.onNodeWithTag("supplier-add").performClick()
        assertTrue(addClicked)

        composeRule.onNodeWithTag("supplier-open-filter").performClick()
        assertTrue(filterClicked)

        // Search mode updates empty state copy
        composeRule.onNodeWithTag("supplier-toggle-search").performClick()
        composeRule.onNodeWithTag("supplier-search-input").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-search-clear").assertDoesNotExist()

        composeRule.onNodeWithTag("supplier-search-input").performTextReplacement("PT Maju")
        composeRule.onNodeWithTag("supplier-search-clear").assertIsDisplayed()
        composeRule.onNodeWithText("\"PT Maju\" tidak ditemukan").assertIsDisplayed()
        composeRule.onNodeWithText("Periksa ejaan atau cari dengan nomor HP.").assertIsDisplayed()

        // Close search -> restores default title and empty state
        composeRule.onNodeWithTag("supplier-search-back").performClick()
        composeRule.onNode(hasText("Supplier") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Belum ada supplier").assertIsDisplayed()

        composeRule.onNodeWithTag("supplier-back-btn").performClick()
        assertTrue(backClicked)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutKeepsNavRailWithMenuActiveAndMaxWidthEmptyGroup() {
        var selectedNav: DashboardNavDestination? = null

        composeRule.setContent {
            TbterminalappTheme {
                val spec = supplierEmptyStateSpecFor(searchQuery = "", isFiltered = false)
                OwnerSupplierEmptyScreen(
                    summary = SupplierEmptySummaryUiState(),
                    isSearchExpanded = false,
                    searchQuery = "",
                    isFiltered = false,
                    onBack = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onNavSelect = { selectedNav = it },
                    panelContent = { isTablet ->
                        EmptyState(
                            icon = spec.icon,
                            iconContainerColor = spec.iconContainerColor,
                            iconTint = spec.iconTint,
                            title = spec.title,
                            description = spec.description,
                            showAddButton = true,
                            onAddCustomerClick = {},
                            buttonTestTag = "supplier-add",
                            buttonLabel = "Tambah supplier",
                            modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()

        val addBounds = composeRule.onNodeWithTag("supplier-add").getBoundsInRoot()
        assertTrue((addBounds.right - addBounds.left) <= 360.dp + 0.5.dp)

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(DashboardNavDestination.BERANDA, selectedNav)
    }
}
