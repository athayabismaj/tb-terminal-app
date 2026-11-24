package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
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
class OwnerDashboardBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1320dp-mdpi")
    fun phoneLayoutMatchesStructureAndInteractions() {
        var state by mutableStateOf(sampleReferenceDashboardState(recentCount = 3))
        var lastQuickAction: QuickActionType? = null
        var lastTile: DashboardTileType? = null
        var seeAllClicked = 0
        var lastNav: DashboardNavDestination? = null
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                DashboardScreen(
                    state = state,
                    onPeriodChange = { period, customRange ->
                        state = state.copy(period = period, customRange = customRange ?: state.customRange)
                    },
                    onToggleHidden = { state = state.copy(balanceHidden = !state.balanceHidden) },
                    onQuickAction = { lastQuickAction = it },
                    onTileClick = { lastTile = it },
                    onSeeAll = { seeAllClicked++ },
                    onNavSelect = { lastNav = it },
                    onProfile = { profileClicked++ }
                )
            }
        }

        // Greeting, hero stats, comparison label
        composeRule.onNodeWithText("Halo, Pemilik Toko").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 2.450.000").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 612 rb").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 2,1 jt").assertIsDisplayed()
        composeRule.onNodeWithText("dari kemarin", substring = true).assertIsDisplayed()

        // Privacy toggle hides nominals in header and updates accessibility label
        composeRule.onNodeWithContentDescription("Sembunyikan nominal").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").performClick()
        composeRule.onNodeWithContentDescription("Tampilkan nominal").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 2.450.000").assertDoesNotExist()
        composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").performClick()
        composeRule.onNodeWithText("Rp 2.450.000").assertIsDisplayed()

        // Touch target checks (>= 48dp for avatar, eye toggle, and period dropdown)
        val eyeBounds = composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").getBoundsInRoot()
        assertTrue((eyeBounds.right - eyeBounds.left) >= 48.dp)
        assertTrue((eyeBounds.bottom - eyeBounds.top) >= 48.dp)

        val avatarBounds = composeRule.onNodeWithTag("dashboard-owner-avatar-btn").getBoundsInRoot()
        assertTrue((avatarBounds.right - avatarBounds.left) >= 48.dp)
        assertTrue((avatarBounds.bottom - avatarBounds.top) >= 48.dp)

        val periodDropdownBounds = composeRule.onNodeWithTag("owner-period-dropdown").getBoundsInRoot()
        assertTrue((periodDropdownBounds.bottom - periodDropdownBounds.top) >= 48.dp)
        assertTrue(periodDropdownBounds.right <= avatarBounds.left)
        assertTrue(periodDropdownBounds.top < avatarBounds.bottom && avatarBounds.top < periodDropdownBounds.bottom)

        // Period dropdown semantics, bottom sheet selection, scrim dismiss, and comparison label updates
        composeRule.onNodeWithTag("owner-period-dropdown").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList) and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Tertutup")
        )
        composeRule.onNodeWithContentDescription("Periode: Hari").assertIsDisplayed()

        composeRule.onNodeWithTag("owner-period-dropdown").performClick()
        composeRule.onNodeWithTag("owner-period-dropdown").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Terbuka")
        )
        composeRule.onNodeWithText("Pilih periode").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-period-selector-handle", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("owner-period-HARI").assertIsSelected()
        composeRule.onNodeWithTag("owner-period-MINGGU").assertIsNotSelected()

        // Tapping scrim closes without changing selection
        composeRule.onNodeWithTag("owner-period-selector-scrim").performClick()
        composeRule.onNodeWithTag("owner-period-selector-sheet").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Periode: Hari").assertIsDisplayed()

        // Select Minggu
        composeRule.onNodeWithTag("owner-period-dropdown").performClick()
        composeRule.onNodeWithTag("owner-period-MINGGU").performClick()
        composeRule.onNodeWithTag("owner-period-selector-sheet").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Periode: Minggu").assertIsDisplayed()
        composeRule.onNodeWithText("dari minggu lalu", substring = true).assertIsDisplayed()

        // Select Bulan
        composeRule.onNodeWithTag("owner-period-dropdown").performClick()
        composeRule.onNodeWithTag("owner-period-BULAN").performClick()
        composeRule.onNodeWithContentDescription("Periode: Bulan").assertIsDisplayed()
        composeRule.onNodeWithText("dari bulan lalu", substring = true).assertIsDisplayed()

        // Kustom opens DateRangePicker dialog
        composeRule.onNodeWithTag("owner-period-dropdown").performClick()
        composeRule.onNodeWithTag("owner-period-KUSTOM").performClick()
        composeRule.onNodeWithText("Terapkan").assertIsDisplayed()
        composeRule.onNodeWithText("Batal").performClick()

        // Quick actions (without POS "Catat jual"), attention tiles, see all, profile, bottom nav
        composeRule.onNodeWithText("Catat jual").assertDoesNotExist()
        composeRule.onNodeWithTag("owner-qa-CATAT_BELI").performClick()
        assertEquals(QuickActionType.CATAT_BELI, lastQuickAction)

        composeRule.onNodeWithTag("owner-kpi-receivables").performClick()
        assertEquals(DashboardTileType.PIUTANG_AKTIF, lastTile)

        composeRule.onNodeWithTag("owner-see-all-transactions").performScrollTo().performClick()
        assertEquals(1, seeAllClicked)

        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performClick()
        assertEquals(1, profileClicked)

        composeRule.onNodeWithTag("nav-TRANSACTIONS").performClick()
        assertEquals(DashboardNavDestination.TRANSAKSI, lastNav)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutDisplaysSplitColumnsAndEmptyAndCustomRangeStates() {
        var state by mutableStateOf(
            sampleReferenceDashboardState(recentCount = 5).copy(
                period = Period.KUSTOM,
                customRange = 1762905600000L to 1763424000000L, // 12–18 Nov UTC
                synced = false
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                DashboardScreen(
                    state = state,
                    onPeriodChange = { period, customRange ->
                        state = state.copy(period = period, customRange = customRange)
                    },
                    onToggleHidden = {},
                    onQuickAction = {},
                    onTileClick = {},
                    onSeeAll = {},
                    onNavSelect = {},
                    onProfile = {}
                )
            }
        }

        // Tablet shows "Beranda", custom date range label "12–18 Nov", and "dari periode sebelumnya"
        composeRule.onNodeWithText("Beranda").assertIsDisplayed()
        composeRule.onNodeWithText("12–18 Nov").assertIsDisplayed()
        composeRule.onNodeWithText("dari periode sebelumnya", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-sync-dot", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("owner-period-dropdown").performClick()
        composeRule.onNodeWithTag("owner-period-selector-handle", useUnmergedTree = true).assertDoesNotExist()
        val sheetBounds = composeRule.onNodeWithTag("owner-period-selector-sheet").getBoundsInRoot()
        assertEquals(480.dp.value, (sheetBounds.right - sheetBounds.left).value, 1f)
        composeRule.onNodeWithTag("owner-period-selector-scrim").performClick()
        composeRule.onNodeWithText("Penjualan #0120").assertIsDisplayed()

        // Empty state messaging
        state = state.copy(recent = emptyList(), chart = emptyList())
        composeRule.onNodeWithText("Belum ada transaksi pada periode ini").assertIsDisplayed()
        composeRule.onNodeWithText("Transaksi kasir yang selesai akan muncul otomatis di sini.").assertIsDisplayed()

        // Loading state hides figures and shows skeleton boxes
        state = sampleReferenceDashboardState().copy(isLoading = true)
        composeRule.onNodeWithText("Rp 2.450.000").assertDoesNotExist()
        composeRule.onNodeWithText("Penjualan #0124").assertDoesNotExist()

        // Error state displays AppAlert
        state = sampleReferenceDashboardState().copy(error = "Koneksi server terputus")
        composeRule.onNodeWithText("Gagal memuat data").assertIsDisplayed()
        composeRule.onNodeWithText("Koneksi server terputus").assertIsDisplayed()
    }
}
