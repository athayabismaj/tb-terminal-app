package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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
class OwnerTransaksiBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneTabsPrivacyTouchTargetsAndEmptyLoadingStatesWork() {
        var state by mutableStateOf(sampleReferenceTransaksiState(TransaksiTab.PENJUALAN, isTablet = false))
        var primaryActionTab: TransaksiTab? = null
        var seeHistoryTab: TransaksiTab? = null
        var clickedItemId: String? = null
        var clickedNav: DashboardNavDestination? = null
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                TransaksiScreen(
                    state = state,
                    onTabChange = { tab -> state = state.copy(tab = tab) },
                    onToggleHidden = { state = state.copy(balanceHidden = !state.balanceHidden) },
                    onPrimaryAction = { primaryActionTab = it },
                    onSeeHistory = { seeHistoryTab = it },
                    onItemClick = { clickedItemId = it },
                    onNavSelect = { clickedNav = it },
                    onProfile = { profileClicked++ }
                )
            }
        }

        // Sales mode content with "Transaksi" heading matching other hub screens, dropdown capsule, and profile avatar
        composeRule.onNode(hasText("Transaksi") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithTag("transaksi-type-dropdown").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList) and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Tertutup")
        )
        composeRule.onNodeWithContentDescription("Jenis transaksi: Penjualan").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Akun Pemilik Toko, tersinkron").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-sync-dot", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Ringkasan penjualan").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 2.450.000").assertIsDisplayed()
        composeRule.onNodeWithText("24 struk").assertIsDisplayed()
        composeRule.onNodeWithText("Catat jual").assertDoesNotExist()
        composeRule.onNodeWithText("Aktivitas penjualan").assertIsDisplayed()

        // Touch target verification: eye 48x48, avatar 48x48, dropdown >= 48h, Lihat riwayat 48h
        val eyeBounds = composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").getBoundsInRoot()
        assertTrue((eyeBounds.right - eyeBounds.left) >= 48.dp)
        assertTrue((eyeBounds.bottom - eyeBounds.top) >= 48.dp)

        val avatarBounds = composeRule.onNodeWithTag("dashboard-owner-avatar-btn").getBoundsInRoot()
        assertTrue((avatarBounds.right - avatarBounds.left) >= 48.dp)
        assertTrue((avatarBounds.bottom - avatarBounds.top) >= 48.dp)

        val dropdownBounds = composeRule.onNodeWithTag("transaksi-type-dropdown").getBoundsInRoot()
        assertTrue((dropdownBounds.bottom - dropdownBounds.top) >= 48.dp)
        assertTrue((dropdownBounds.right - dropdownBounds.left) < 200.dp)

        val historyBounds = composeRule.onNodeWithTag("transaksi-see-history").getBoundsInRoot()
        assertTrue((historyBounds.bottom - historyBounds.top) >= 48.dp)

        // Eye toggle hides big amount only; row amounts stay visible
        composeRule.onNodeWithContentDescription("Sembunyikan nominal").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").performClick()
        composeRule.onNodeWithContentDescription("Tampilkan nominal").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 2.450.000").assertDoesNotExist()
        composeRule.onNodeWithText("Rp 185.000").assertIsDisplayed()

        // Opening dropdown shows bottom sheet with handle; tapping scrim closes without changing selection
        composeRule.onNodeWithTag("transaksi-type-dropdown").performClick()
        composeRule.onNodeWithTag("transaksi-type-dropdown").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Terbuka")
        )
        composeRule.onNodeWithText("Jenis transaksi").assertIsDisplayed()
        composeRule.onNodeWithTag("transaksi-selector-handle", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("transaksi-option-PENJUALAN").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        ).assertIsSelected()
        composeRule.onNodeWithTag("transaksi-option-PEMBELIAN").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        ).assertIsNotSelected()

        // Tapping scrim closes selector without changing selection
        composeRule.onNodeWithTag("transaksi-selector-scrim").performClick()
        composeRule.onNodeWithTag("transaksi-selector-sheet").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Jenis transaksi: Penjualan").assertIsDisplayed()

        // Clicking active option closes without changing tab
        composeRule.onNodeWithTag("transaksi-type-dropdown").performClick()
        composeRule.onNodeWithTag("transaksi-option-PENJUALAN").performClick()
        composeRule.onNodeWithTag("transaksi-selector-sheet").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Jenis transaksi: Penjualan").assertIsDisplayed()

        // Selecting Pembelian updates summary, CTA ("Tambah barang masuk"), section title, and list
        composeRule.onNodeWithTag("transaksi-type-dropdown").performClick()
        composeRule.onNodeWithTag("transaksi-option-PEMBELIAN").performClick()
        composeRule.onNodeWithTag("transaksi-selector-sheet").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Jenis transaksi: Pembelian").assertIsDisplayed()
        composeRule.onNodeWithText("Total pembelian bulan ini").assertIsDisplayed()
        composeRule.onNodeWithText("Tambah barang masuk").assertIsDisplayed()
        val ctaBounds = composeRule.onNodeWithTag("transaksi-primary-cta").getBoundsInRoot()
        assertTrue((ctaBounds.bottom - ctaBounds.top) >= 48.dp)
        composeRule.onNodeWithText("Aktivitas pembelian").assertIsDisplayed()
        composeRule.onNodeWithText("14 Nov · Belum lunas").assertIsDisplayed()

        // Callbacks
        composeRule.onNodeWithTag("transaksi-primary-cta").performClick()
        assertEquals(TransaksiTab.PEMBELIAN, primaryActionTab)

        composeRule.onNodeWithTag("transaksi-see-history").performClick()
        assertEquals(TransaksiTab.PEMBELIAN, seeHistoryTab)

        composeRule.onNodeWithTag("transaksi-row-p1").performClick()
        assertEquals("p1", clickedItemId)

        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performClick()
        assertEquals(1, profileClicked)

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(DashboardNavDestination.BERANDA, clickedNav)

        // Empty states per tab
        state = state.copy(purchaseItems = emptyList(), salesItems = emptyList())
        composeRule.onNodeWithText("Belum ada pembelian").assertIsDisplayed()
        composeRule.onNodeWithText("Faktur barang masuk akan muncul di sini setelah dicatat.").assertIsDisplayed()

        composeRule.onNodeWithTag("transaksi-type-dropdown").performClick()
        composeRule.onNodeWithTag("transaksi-option-PENJUALAN").performClick()
        composeRule.onNodeWithText("Belum ada penjualan").assertIsDisplayed()
        composeRule.onNodeWithText("Penjualan kasir yang selesai akan muncul otomatis di sini.").assertIsDisplayed()

        // Loading state hides list items & displays header structure
        state = sampleReferenceTransaksiState().copy(loading = true)
        composeRule.onNodeWithText("Penjualan #0124").assertDoesNotExist()
        composeRule.onNodeWithText("Belum ada penjualan").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDisplaysEightSalesAndSixPurchasesWithRailAndCenteredDialog() {
        var state by mutableStateOf(
            sampleReferenceTransaksiState(TransaksiTab.PENJUALAN, isTablet = true).copy(
                userName = "Budi Santoso",
                synced = false
            )
        )
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                TransaksiScreen(
                    state = state,
                    onTabChange = { tab -> state = state.copy(tab = tab) },
                    onToggleHidden = {},
                    onPrimaryAction = {},
                    onSeeHistory = {},
                    onItemClick = {},
                    onNavSelect = {},
                    onProfile = { profileClicked++ }
                )
            }
        }

        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Akun Budi Santoso, offline").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performClick()
        assertEquals(1, profileClicked)
        composeRule.onNodeWithText("Penjualan #0117").assertIsDisplayed()

        // Open selector on tablet: centered 480dp dialog without drag handle
        composeRule.onNodeWithTag("transaksi-type-dropdown").performClick()
        composeRule.onNodeWithTag("transaksi-selector-handle").assertDoesNotExist()
        val sheetBounds = composeRule.onNodeWithTag("transaksi-selector-sheet").getBoundsInRoot()
        assertEquals(480.dp.value, (sheetBounds.right - sheetBounds.left).value, 1f)

        composeRule.onNodeWithTag("transaksi-option-PEMBELIAN").performClick()
        composeRule.onNodeWithText("Rp 6.800.000").assertIsDisplayed()
        composeRule.onNodeWithText("Faktur #0026").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun narrowScreenLargeFontKeepsCapsuleAndAvatarIntact() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                TbterminalappTheme {
                    TransaksiScreen(
                        state = sampleReferenceTransaksiState(TransaksiTab.PENJUALAN, isTablet = false),
                        onTabChange = {},
                        onToggleHidden = {},
                        onPrimaryAction = {},
                        onSeeHistory = {},
                        onItemClick = {},
                        onNavSelect = {}
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Jenis transaksi: Penjualan").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        val dropdownBounds = composeRule.onNodeWithTag("transaksi-type-dropdown").getBoundsInRoot()
        val avatarBounds = composeRule.onNodeWithTag("dashboard-owner-avatar-btn").getBoundsInRoot()
        assertTrue((avatarBounds.right - avatarBounds.left) >= 44.dp)
        assertTrue(dropdownBounds.right <= avatarBounds.left)
    }
}
