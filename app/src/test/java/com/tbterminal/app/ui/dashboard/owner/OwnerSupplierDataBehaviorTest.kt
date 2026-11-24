package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.suppliers.SupplierFilterSheet
import com.tbterminal.app.ui.suppliers.SupplierSortOrder
import com.tbterminal.app.ui.suppliers.SupplierStatusFilter
import com.tbterminal.app.ui.suppliers.SupplierUiState
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
class OwnerSupplierDataBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun dataListRendersRowsTermsStatusesAndClickingRowNavigatesToDetail() {
        var detailSupplierId: String? = null
        val sample = sampleReferenceSuppliers()

        composeRule.setContent {
            TbterminalappTheme {
                OwnerSupplierRoute(
                    uiState = SupplierUiState(
                        suppliers = sample,
                        totalSuppliers = 3L,
                        isLoading = false
                    ),
                    canManage = true,
                    isSearchExpanded = false,
                    sortOrder = SupplierSortOrder.NAME_ASC,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onAddSupplierClick = {},
                    onSupplierDetailClick = { detailSupplierId = it },
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = {},
                    onDashboardClick = {},
                    onDismissMessage = {},
                    loadingContent = {}
                )
            }
        }

        // Section header & CTA
        composeRule.onNodeWithText("Tambah supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Semua supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Urut: Nama A\u2013Z").assertIsDisplayed()

        // Row formatting matching Pelanggan list rows
        composeRule.onNodeWithText("PT Semen Nusantara Tbk").assertIsDisplayed()
        composeRule.onNodeWithText("0812-3456-7890 \u00B7 Jl. Industri Raya No. 12").assertIsDisplayed()
        composeRule.onNodeWithText("30 hari").assertIsDisplayed()
        composeRule.onNodeWithText("Tanpa nomor HP").assertIsDisplayed()
        composeRule.onNodeWithText("Tunai").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("PT Semen Nusantara Tbk, aktif").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("UD Sumber Keramik, nonaktif").assertIsDisplayed()
        composeRule.onNodeWithText("Semua supplier sudah ditampilkan").assertIsDisplayed()

        // Touch target >= 72dp and click routes to supplier detail
        val rowBounds = composeRule.onNodeWithTag("supplier-row-s1").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        composeRule.onNodeWithTag("supplier-row-s1").performClick()
        assertEquals("s1", detailSupplierId)

        composeRule.onNodeWithTag("supplier-row-s3").performClick()
        assertEquals("s3", detailSupplierId)

        // Filter & sort helper
        val sortedByTerm = filterAndSortOwnerSuppliers(
            suppliers = sample,
            searchQuery = "",
            statusFilter = SupplierStatusFilter.ACTIVE,
            sortOrder = SupplierSortOrder.TERM_DESC
        )
        assertEquals(listOf("PT Semen Nusantara Tbk", "CV Besi Jaya"), sortedByTerm.map { it.name })
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDataLayoutKeepsNavRailAndLazyList() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerSupplierDataScreen(
                    summary = SupplierEmptySummaryUiState(totalSuppliers = 3L, activeCount = 2, inactiveCount = 1),
                    suppliers = sampleReferenceSuppliers(),
                    sortLabel = SupplierSortOrder.NAME_ASC.label,
                    footerStatus = PelangganFooterStatus.END_OF_LIST,
                    hasMorePages = false,
                    canManage = true,
                    isSearchExpanded = false,
                    searchQuery = "",
                    isFiltered = false,
                    onBack = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onAddSupplierClick = {},
                    onSupplierClick = {},
                    onLoadMore = {},
                    onRetryLoadMore = {},
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-supplier-lazy-list").assertIsDisplayed()
        composeRule.onNodeWithText("PT Semen Nusantara Tbk").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun supplierFilterSheetMatchesPelangganLayoutAndActions() {
        var appliedStatus = SupplierStatusFilter.ALL
        var appliedSort = SupplierSortOrder.NAME_ASC
        var dismissCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                SupplierFilterSheet(
                    statusFilter = appliedStatus,
                    onApplyFilter = { appliedStatus = it },
                    onDismiss = { dismissCount++ },
                    sortOrder = appliedSort,
                    onSortApply = { appliedSort = it }
                )
            }
        }

        composeRule.onNodeWithText("Filter Supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Atur status dan urutan supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Status supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Urutan").assertIsDisplayed()

        // Horizontal pill cards (matching CustomerFilterBottomSheet)
        composeRule.onNodeWithTag("supplier-status-option-ALL").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-status-option-ACTIVE").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-status-option-INACTIVE").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-sort-option-NAME_ASC").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-sort-option-NAME_DESC").assertIsDisplayed()
        composeRule.onNodeWithTag("supplier-sort-option-TERM_DESC").assertIsDisplayed()

        // Verify status pills sit on the same horizontal row
        val allBounds = composeRule.onNodeWithTag("supplier-status-option-ALL").getBoundsInRoot()
        val inactiveBounds = composeRule.onNodeWithTag("supplier-status-option-INACTIVE").getBoundsInRoot()
        assertEquals(allBounds.top.value, inactiveBounds.top.value, 2f)

        composeRule.onNodeWithTag("supplier-status-option-INACTIVE").performClick()
        composeRule.onNodeWithTag("supplier-sort-option-TERM_DESC").performClick()
        composeRule.onNodeWithTag("supplier-filter-done").performClick()

        assertEquals(SupplierStatusFilter.INACTIVE, appliedStatus)
        assertEquals(SupplierSortOrder.TERM_DESC, appliedSort)
        assertEquals(1, dismissCount)

        composeRule.onNodeWithTag("supplier-filter-reset").performClick()
        assertEquals(SupplierStatusFilter.ALL, appliedStatus)
        assertEquals(SupplierSortOrder.NAME_ASC, appliedSort)
        assertEquals(2, dismissCount)
    }
}
