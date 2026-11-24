package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.customers.CustomerCategoryFilter
import com.tbterminal.app.ui.customers.CustomerFilterBottomSheet
import com.tbterminal.app.ui.customers.CustomerListUiState
import com.tbterminal.app.ui.customers.CustomerStatusFilter
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
class OwnerPelangganBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneEmptyStateMatchesReferenceRemovesLegacyChromeAndChecksTouchTargets() {
        var backClicked = false
        var addClicked = false
        var filterClicked = false
        var isSearchExpanded by mutableStateOf(false)
        var searchQuery by mutableStateOf("")

        composeRule.setContent {
            TbterminalappTheme {
                val spec = emptyStateSpecFor(searchQuery = searchQuery, isFiltered = false)
                OwnerPelangganScreen(
                    summary = PelangganSummaryUiState(),
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
                    centerPanelContent = true,
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
                            modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                        )
                    }
                )
            }
        }

        // Secondary page: bottom bar MUST NOT appear on phone
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()
        composeRule.onNodeWithTag("customer-summary").assertDoesNotExist()
        composeRule.onNodeWithText("Daftar pelanggan").assertDoesNotExist()
        composeRule.onNodeWithText("Belum Ada Pelanggan").assertDoesNotExist()

        // Header & hero stats
        composeRule.onNode(hasText("Pelanggan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Total pelanggan").assertIsDisplayed()
        composeRule.onNodeWithTag("pelanggan-total-count").assertIsDisplayed()
        composeRule.onNodeWithText("orang").assertIsDisplayed()
        composeRule.onNodeWithText("Umum").assertIsDisplayed()
        composeRule.onNodeWithText("Kontraktor").assertIsDisplayed()
        composeRule.onNodeWithText("Limit kredit").assertDoesNotExist()
        composeRule.onNodeWithText("Rp 0").assertDoesNotExist()

        // Empty state content
        composeRule.onNodeWithText("Belum ada pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Tambahkan pelanggan untuk mencatat piutang, limit kredit, dan riwayat belanja.").assertIsDisplayed()
        composeRule.onNodeWithText("Tambah pelanggan").assertIsDisplayed()

        // Touch targets >= 48dp (button >= 56dp)
        listOf("pelanggan-back-btn", "customer-toggle-search", "customer-open-filters").forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getBoundsInRoot()
            assertTrue("$tag width < 48dp", (bounds.right - bounds.left) >= 48.dp)
            assertTrue("$tag height < 48dp", (bounds.bottom - bounds.top) >= 48.dp)
        }
        val addBounds = composeRule.onNodeWithTag("customer-add").getBoundsInRoot()
        assertTrue((addBounds.bottom - addBounds.top) >= 56.dp)

        // Actions
        composeRule.onNodeWithTag("customer-add").performClick()
        assertTrue(addClicked)

        composeRule.onNodeWithTag("customer-open-filters").performClick()
        assertTrue(filterClicked)

        // Open search mode -> SearchTopBar replaces header title/actions
        composeRule.onNodeWithTag("customer-toggle-search").performClick()
        composeRule.onNodeWithTag("customer-search-input").assertIsDisplayed()
        composeRule.onNodeWithTag("customer-search-clear").assertDoesNotExist()

        // Type query -> clear button visible + not-found empty state renders
        composeRule.onNodeWithTag("customer-search-input").performTextReplacement("Pak Budi")
        composeRule.onNodeWithTag("customer-search-clear").assertIsDisplayed()
        composeRule.onNodeWithText("\"Pak Budi\" tidak ditemukan").assertIsDisplayed()
        composeRule.onNodeWithText("Periksa ejaan atau cari dengan nomor HP.").assertIsDisplayed()
        composeRule.onNodeWithText("Tambah pelanggan").assertIsDisplayed()

        // Close search -> restores Pelanggan title and original empty state
        composeRule.onNodeWithTag("customer-search-back").performClick()
        composeRule.onNode(hasText("Pelanggan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Belum ada pelanggan").assertIsDisplayed()

        // Back chevron calls onBack
        composeRule.onNodeWithTag("pelanggan-back-btn").performClick()
        assertTrue(backClicked)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutKeepsNavRailWithMenuActiveAndMaxWidthEmptyGroup() {
        var selectedNav: DashboardNavDestination? = null

        composeRule.setContent {
            TbterminalappTheme {
                val spec = emptyStateSpecFor(searchQuery = "", isFiltered = false)
                OwnerPelangganScreen(
                    summary = PelangganSummaryUiState(),
                    isSearchExpanded = false,
                    searchQuery = "",
                    isFiltered = false,
                    onBack = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onNavSelect = { selectedNav = it },
                    centerPanelContent = true,
                    panelContent = { isTablet ->
                        EmptyState(
                            icon = spec.icon,
                            iconContainerColor = spec.iconContainerColor,
                            iconTint = spec.iconTint,
                            title = spec.title,
                            description = spec.description,
                            showAddButton = true,
                            onAddCustomerClick = {},
                            modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                        )
                    }
                )
            }
        }

        // Rail remains visible with all destinations including Menu
        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-TRANSACTIONS").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-FINANCE").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-STOCK").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()

        val addBounds = composeRule.onNodeWithTag("customer-add").getBoundsInRoot()
        assertTrue((addBounds.right - addBounds.left) <= 360.dp + 0.5.dp)

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(DashboardNavDestination.BERANDA, selectedNav)
        composeRule.onAllNodesWithText("0 orang").let { }
    }

    @Test
    fun summaryMapperComputesGeneralAndContractorCounts() {
        val customers = listOf(
            Customer(
                id = "1",
                name = "Andi",
                phone = "081234567890",
                address = null,
                isContractor = false,
                creditLimit = BigDecimal("500000"),
                paymentTermDays = 0,
                isActive = true,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z"
            ),
            Customer(
                id = "2",
                name = "CV Jaya",
                phone = "081298765432",
                address = null,
                isContractor = true,
                creditLimit = BigDecimal("14500000"),
                paymentTermDays = 14,
                isActive = true,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z"
            )
        )
        val summary = CustomerListUiState(
            customers = customers,
            totalCustomers = 2L
        ).toPelangganSummary()

        assertEquals(2L, summary.totalCustomers)
        assertEquals(1, summary.generalCount)
        assertEquals(1, summary.contractorCount)
    }

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun dataListRendersRowsStatusesPhonesLimitsLongNamesAndClicksWithoutLegacyChrome() {
        var clickedCustomerId: String? = null
        val longNameCustomer = Customer(
            id = "c-long",
            name = "PT Konstruksi Infrastruktur Nusantara Abadi Jaya Tbk",
            phone = "081987654321",
            address = "Jl. Rahasia Alamat Tidak Tampil 99",
            isContractor = true,
            creditLimit = BigDecimal("850000"),
            paymentTermDays = 7,
            isActive = true,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        val customers = sampleReferenceCustomers() + longNameCustomer

        composeRule.setContent {
            TbterminalappTheme {
                OwnerPelangganDataScreen(
                    summary = PelangganSummaryUiState(totalCustomers = 7L, generalCount = 4, contractorCount = 3),
                    customers = customers,
                    sortLabel = CustomerSortOrder.NAME_ASC.label,
                    footerStatus = PelangganFooterStatus.LOADING,
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
                    onAddCustomerClick = {},
                    onCustomerClick = { clickedCustomerId = it },
                    onLoadMore = {},
                    onRetryLoadMore = {},
                    onNavSelect = {}
                )
            }
        }

        // Legacy UI MUST NOT appear
        composeRule.onNodeWithText("Daftar pelanggan").assertDoesNotExist()
        composeRule.onNodeWithText("Rp0,00").assertDoesNotExist()
        composeRule.onNodeWithText("0 hari").assertDoesNotExist()
        composeRule.onNodeWithText("Jl. Rahasia Alamat Tidak Tampil 99").assertDoesNotExist()
        composeRule.onNodeWithText("Limit kredit").assertDoesNotExist()

        // New controls & section header
        composeRule.onNodeWithText("Tambah pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Semua pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Urut: Nama A\u2013Z").assertIsDisplayed()

        // Content formatting
        composeRule.onNodeWithText("Budi Santoso").assertIsDisplayed()
        composeRule.onNodeWithText("Kontraktor \u00B7 0812-3456-7890").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 5 jt").assertIsDisplayed()
        composeRule.onNodeWithText("Limit \u00B7 14 hari").assertIsDisplayed()

        // Inactive customer + accessibility status description
        composeRule.onNodeWithContentDescription("Dewi Wulandari, nonaktif").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Budi Santoso, aktif").assertIsDisplayed()

        // Empty phone + single-word initials
        composeRule.onNodeWithText("Umum \u00B7 Tanpa nomor HP").assertIsDisplayed()
        composeRule.onNodeWithText("Umum \u00B7 0891-5454-644").assertIsDisplayed()

        // Row touch target >= 72dp and click opens detail
        val rowBounds = composeRule.onNodeWithTag("customer-row-c1").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)
        composeRule.onNodeWithTag("customer-row-c1").performClick()
        assertEquals("c1", clickedCustomerId)

        // Loading footer
        composeRule.onNodeWithText("Memuat pelanggan lainnya\u2026").assertIsDisplayed()
    }

    @Test
    fun listFooterStatesAndFilterSortHelpersWorkCorrectly() {
        var retryCount = 0
        var footerStatus by mutableStateOf(PelangganFooterStatus.LOADING)

        composeRule.setContent {
            TbterminalappTheme {
                Column {
                    ListFooterState(
                        status = footerStatus,
                        onRetry = { retryCount++ }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Memuat pelanggan lainnya\u2026").assertIsDisplayed()

        footerStatus = PelangganFooterStatus.END_OF_LIST
        composeRule.onNodeWithText("Semua pelanggan sudah ditampilkan").assertIsDisplayed()

        footerStatus = PelangganFooterStatus.ERROR
        composeRule.onNodeWithText("Gagal memuat").assertIsDisplayed()
        composeRule.onNodeWithTag("pelanggan-footer-retry").performClick()
        assertEquals(1, retryCount)

        // Filter by phone digits + sort checks
        val customers = sampleReferenceCustomers()
        val byPhone = filterAndSortOwnerCustomers(
            customers = customers,
            searchQuery = "08915454",
            categoryFilter = CustomerCategoryFilter.ALL,
            statusFilter = CustomerStatusFilter.ALL,
            sortOrder = CustomerSortOrder.NAME_ASC
        )
        assertEquals(listOf("Jagad"), byPhone.map { it.name })

        val byCreditDesc = filterAndSortOwnerCustomers(
            customers = customers,
            searchQuery = "",
            categoryFilter = CustomerCategoryFilter.CONTRACTOR,
            statusFilter = CustomerStatusFilter.ACTIVE,
            sortOrder = CustomerSortOrder.CREDIT_DESC
        )
        assertEquals(listOf("CV Maju Bangun", "Budi Santoso"), byCreditDesc.map { it.name })
    }

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun statusFilterLivesInFilterPopupAndAppliesActiveOrInactiveFilter() {
        var selectedCategory by mutableStateOf(CustomerCategoryFilter.ALL)
        var selectedFilter by mutableStateOf(CustomerStatusFilter.ALL)
        var sortOrder by mutableStateOf(CustomerSortOrder.NAME_ASC)
        var showFilterPopup by mutableStateOf(false)
        val allCustomers = sampleReferenceCustomers()

        composeRule.setContent {
            TbterminalappTheme {
                val filteredForState = when (selectedFilter) {
                    CustomerStatusFilter.ALL -> allCustomers
                    CustomerStatusFilter.ACTIVE -> allCustomers.filter { it.isActive }
                    CustomerStatusFilter.INACTIVE -> allCustomers.filter { !it.isActive }
                }
                OwnerPelangganRoute(
                    uiState = CustomerListUiState(
                        customers = filteredForState,
                        totalCustomers = filteredForState.size.toLong(),
                        categoryFilter = selectedCategory,
                        statusFilter = selectedFilter,
                        isLoading = false
                    ),
                    canManage = true,
                    isSearchExpanded = false,
                    sortOrder = sortOrder,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = { showFilterPopup = true },
                    onAddCustomerClick = {},
                    onCustomerDetailClick = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRetryLoadMore = {},
                    onBack = {},
                    onDashboardClick = {},
                    onDismissMessage = {},
                    loadingContent = {},
                    errorContent = {}
                )
                if (showFilterPopup) {
                    CustomerFilterBottomSheet(
                        uiState = CustomerListUiState(
                            categoryFilter = selectedCategory,
                            statusFilter = selectedFilter,
                            isLoading = false
                        ),
                        onDismiss = { showFilterPopup = false },
                        onApply = { category, status ->
                            selectedCategory = category
                            selectedFilter = status
                        },
                        sortOrder = sortOrder,
                        onSortApply = { sortOrder = it }
                    )
                }
            }
        }

        // Inline status pills must NOT appear on the main screen
        composeRule.onNodeWithTag("pelanggan-status-filter-row").assertDoesNotExist()
        composeRule.onNodeWithText("Semua pelanggan").assertIsDisplayed()

        // Open filter popup -> Semua / Aktif / Nonaktif live inside popup
        composeRule.onNodeWithTag("customer-open-filters").performClick()
        composeRule.onNodeWithTag("customer-filter-status-all").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("customer-filter-status-active").assertIsDisplayed()
        composeRule.onNodeWithTag("customer-filter-status-inactive").assertIsDisplayed()

        // Select Nonaktif and apply -> shows inactive customers under "Pelanggan nonaktif"
        composeRule.onNodeWithTag("customer-filter-status-inactive").performClick()
        composeRule.onNodeWithTag("customer-filter-apply").performClick()
        assertEquals(CustomerStatusFilter.INACTIVE, selectedFilter)
        composeRule.onNodeWithText("Pelanggan nonaktif").assertIsDisplayed()
        composeRule.onNodeWithText("Dewi Wulandari").assertIsDisplayed()
        composeRule.onNodeWithText("Budi Santoso").assertDoesNotExist()

        // Re-open popup and Reset -> returns to Semua pelanggan
        composeRule.onNodeWithTag("customer-open-filters").performClick()
        composeRule.onNodeWithTag("customer-filter-reset").performClick()
        assertEquals(CustomerStatusFilter.ALL, selectedFilter)
        composeRule.onNodeWithText("Semua pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Budi Santoso").assertIsDisplayed()
    }
}
