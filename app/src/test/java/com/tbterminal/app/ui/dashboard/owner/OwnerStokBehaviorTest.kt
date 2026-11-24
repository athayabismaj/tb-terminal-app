package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
class OwnerStokBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1118dp-mdpi")
    fun phoneLayoutPrivacyMenuCallbacksAndZeroRestockStateWork() {
        var state by mutableStateOf(sampleReferenceStokState())
        var clickedAction: StokActionId? = null
        var clickedNav: DashboardNavDestination? = null
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                StokScreen(
                    state = state,
                    onToggleHidden = { state = state.copy(balanceHidden = !state.balanceHidden) },
                    onActionClick = { clickedAction = it },
                    onNavSelect = { clickedNav = it },
                    onProfile = { profileClicked++ }
                )
            }
        }

        // Hero header and 3-column metrics
        composeRule.onNode(hasText("Stok") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Akun Pemilik Toko, tersinkron").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-sync-dot", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Total nilai aset (HPP)").assertIsDisplayed()
        composeRule.onNodeWithText("3 perlu restok").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 18.640.000").assertIsDisplayed()
        composeRule.onNodeWithText("Katalog aktif").assertIsDisplayed()
        composeRule.onNodeWithText("128 item").assertIsDisplayed()
        composeRule.onNodeWithText("Perlu restok").assertIsDisplayed()
        composeRule.onNodeWithText("3 item").assertIsDisplayed()
        composeRule.onNodeWithText("Status sync").assertIsDisplayed()
        composeRule.onNodeWithText("Tersinkron").assertIsDisplayed()

        // No search or scan buttons on Stok header
        composeRule.onNodeWithContentDescription("Cari produk").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Scan barcode").assertDoesNotExist()

        // Primary CTA button & touch target checks
        composeRule.onNodeWithText("Tambah produk").assertIsDisplayed()
        val addBounds = composeRule.onNodeWithTag("stok-primary-add-product").getBoundsInRoot()
        assertTrue((addBounds.bottom - addBounds.top) >= 56.dp)

        val eyeBounds = composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").getBoundsInRoot()
        assertTrue((eyeBounds.right - eyeBounds.left) >= 48.dp)
        assertTrue((eyeBounds.bottom - eyeBounds.top) >= 48.dp)

        val avatarBounds = composeRule.onNodeWithTag("dashboard-owner-avatar-btn").getBoundsInRoot()
        assertTrue((avatarBounds.right - avatarBounds.left) >= 48.dp)
        assertTrue((avatarBounds.bottom - avatarBounds.top) >= 48.dp)

        val rowBounds = composeRule.onNodeWithTag("stok-menu-PRODUCTS_STOCK").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        // 3 menu group headings & 6 rows
        composeRule.onNodeWithText("Katalog & nilai jual").assertIsDisplayed()
        composeRule.onNodeWithText("Daftar produk").assertIsDisplayed()
        composeRule.onNodeWithText("128 produk aktif").assertIsDisplayed()
        composeRule.onNodeWithText("Harga & margin").assertIsDisplayed()
        composeRule.onNodeWithText("Harga jual, HPP, dan margin").assertIsDisplayed()

        composeRule.onNodeWithText("Audit & kontrol stok").assertIsDisplayed()
        composeRule.onNodeWithText("Kartu stok").assertIsDisplayed()
        composeRule.onNodeWithText("3 produk di bawah stok minimum").assertIsDisplayed()
        composeRule.onNodeWithText("Sesuaikan stok").assertIsDisplayed()
        composeRule.onNodeWithText("Opname, koreksi, retur, saldo awal").assertIsDisplayed()

        composeRule.onNodeWithTag("stok-menu-UNITS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Data produk").assertIsDisplayed()
        composeRule.onNodeWithText("Kategori produk").assertIsDisplayed()
        composeRule.onNodeWithText("12 kategori").assertIsDisplayed()
        composeRule.onNodeWithText("Satuan produk").assertIsDisplayed()
        composeRule.onNodeWithText("13 satuan").assertIsDisplayed()

        // Privacy eye toggle hides asset valuation nominal
        composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").performClick()
        composeRule.onNodeWithText("Rp 18.640.000").assertDoesNotExist()
        composeRule.onNodeWithText("Rp ••••••").assertIsDisplayed()

        // Action callbacks
        composeRule.onNodeWithTag("stok-primary-add-product").performClick()
        assertEquals(StokActionId.ADD_PRODUCT, clickedAction)

        composeRule.onNodeWithTag("stok-menu-PRODUCTS_STOCK").performScrollTo().performClick()
        assertEquals(StokActionId.PRODUCTS_STOCK, clickedAction)

        composeRule.onNodeWithTag("stok-menu-PRODUCT_PRICES").performScrollTo().performClick()
        assertEquals(StokActionId.PRODUCT_PRICES, clickedAction)

        composeRule.onNodeWithTag("stok-menu-STOCK_CARD").performScrollTo().performClick()
        assertEquals(StokActionId.STOCK_CARD, clickedAction)

        composeRule.onNodeWithTag("stok-menu-ADJUST_STOCK").performScrollTo().performClick()
        assertEquals(StokActionId.ADJUST_STOCK, clickedAction)

        composeRule.onNodeWithTag("stok-menu-CATEGORIES").performScrollTo().performClick()
        assertEquals(StokActionId.CATEGORIES, clickedAction)

        composeRule.onNodeWithTag("stok-menu-UNITS").performScrollTo().performClick()
        assertEquals(StokActionId.UNITS, clickedAction)

        // When lowStockCount == 0: chip becomes "Stok aman", Kartu stok subtitle hides
        state = state.copy(lowStockCount = 0, balanceHidden = false)
        composeRule.onNodeWithText("Stok aman").assertIsDisplayed()
        composeRule.onNodeWithText("3 perlu restok").assertDoesNotExist()
        composeRule.onNodeWithText("3 produk di bawah stok minimum").assertDoesNotExist()

        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performScrollTo().performClick()
        assertEquals(1, profileClicked)

        // Bottom nav switches
        composeRule.onNodeWithTag("nav-FINANCE").performClick()
        assertEquals(DashboardNavDestination.KEUANGAN, clickedNav)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutDisplaysHeroSummaryWhiteCtaAndMenuGroups() {
        var clickedAction: StokActionId? = null
        var clickedNav: DashboardNavDestination? = null
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                StokScreen(
                    state = sampleReferenceStokState().copy(
                        userName = "Budi Santoso",
                        synced = false
                    ),
                    onToggleHidden = {},
                    onActionClick = { clickedAction = it },
                    onNavSelect = { clickedNav = it },
                    onProfile = { profileClicked++ }
                )
            }
        }

        composeRule.onNode(hasText("Stok") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Akun Budi Santoso, offline").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performClick()
        assertEquals(1, profileClicked)
        composeRule.onNodeWithTag("stok-hero-valuation").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 18.640.000").assertIsDisplayed()
        composeRule.onNodeWithTag("stok-primary-add-product").assertIsDisplayed()
        composeRule.onNodeWithText("Katalog & nilai jual").assertIsDisplayed()
        composeRule.onNodeWithText("Audit & kontrol stok").assertIsDisplayed()
        composeRule.onNodeWithText("Data produk").assertIsDisplayed()

        composeRule.onNodeWithTag("stok-primary-add-product").performClick()
        assertEquals(StokActionId.ADD_PRODUCT, clickedAction)

        composeRule.onNodeWithTag("stok-menu-STOCK_CARD").performClick()
        assertEquals(StokActionId.STOCK_CARD, clickedAction)

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(DashboardNavDestination.BERANDA, clickedNav)
    }
}
