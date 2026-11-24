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
class OwnerKeuanganBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1030dp-mdpi")
    fun phoneLayoutPrivacyMenuCallbacksAndActiveSessionWork() {
        var state by mutableStateOf(sampleReferenceKeuanganState())
        var clickedAction: KeuanganActionId? = null
        var clickedNav: DashboardNavDestination? = null
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                KeuanganScreen(
                    state = state,
                    onToggleHidden = { state = state.copy(balanceHidden = !state.balanceHidden) },
                    onActionClick = { clickedAction = it },
                    onNavSelect = { clickedNav = it },
                    onProfile = { profileClicked++ }
                )
            }
        }

        // Hero header and 3-column metrics
        composeRule.onNode(hasText("Keuangan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Akun Pemilik Toko, tersinkron").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-sync-dot", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Saldo kas sesi terakhir").assertIsDisplayed()
        composeRule.onNodeWithText("Kas siap").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 3.250.000").assertIsDisplayed()
        composeRule.onNodeWithText("Kas hari ini").assertIsDisplayed()
        composeRule.onNodeWithText("+Rp 480.000").assertIsDisplayed()
        composeRule.onNodeWithText("Piutang aktif").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 380.000").assertIsDisplayed()
        composeRule.onNodeWithText("Hutang tempo").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 1,25 jt").assertIsDisplayed()

        // 3 group headings & 6 menu rows
        composeRule.onNodeWithText("Kas & pengeluaran").assertIsDisplayed()
        composeRule.onNodeWithText("Kas harian").assertIsDisplayed()
        composeRule.onNodeWithText("Sesi ditutup").assertIsDisplayed()
        composeRule.onNodeWithText("Pengeluaran kas").assertIsDisplayed()
        composeRule.onNodeWithText("Hari ini Rp 120.000").assertIsDisplayed()

        composeRule.onNodeWithText("Piutang & hutang").assertIsDisplayed()
        composeRule.onNodeWithText("Piutang pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("3 nota · Rp 380.000").assertIsDisplayed()
        composeRule.onNodeWithText("Pembayaran piutang").assertIsDisplayed()
        composeRule.onNodeWithText("Hutang supplier").assertIsDisplayed()
        composeRule.onNodeWithText("2 nota · Rp 1.250.000").assertIsDisplayed()

        composeRule.onNodeWithTag("keuangan-menu-FINANCE_REPORTS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Laporan bisnis & penjualan").assertIsDisplayed()

        // Touch target verification: eye 48x48dp, menu row >= 72dp
        val eyeBounds = composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").getBoundsInRoot()
        assertTrue((eyeBounds.right - eyeBounds.left) >= 48.dp)
        assertTrue((eyeBounds.bottom - eyeBounds.top) >= 48.dp)

        val avatarBounds = composeRule.onNodeWithTag("dashboard-owner-avatar-btn").getBoundsInRoot()
        assertTrue((avatarBounds.right - avatarBounds.left) >= 48.dp)
        assertTrue((avatarBounds.bottom - avatarBounds.top) >= 48.dp)

        val rowBounds = composeRule.onNodeWithTag("keuangan-menu-DAILY_CASH").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        // Toggle privacy hides nominal hero & menu amounts
        composeRule.onNodeWithContentDescription("Sembunyikan nominal").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-dashboard-toggle-privacy").performClick()
        composeRule.onNodeWithContentDescription("Tampilkan nominal").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 3.250.000").assertDoesNotExist()
        composeRule.onNodeWithText("Hari ini Rp ••••••").assertIsDisplayed()
        composeRule.onNodeWithText("3 nota · Rp ••••••").assertIsDisplayed()

        // Menu action & stat callbacks
        composeRule.onNodeWithTag("keuangan-menu-DAILY_CASH").performClick()
        assertEquals(KeuanganActionId.DAILY_CASH, clickedAction)

        composeRule.onNodeWithTag("keuangan-menu-CASH_EXPENSES").performClick()
        assertEquals(KeuanganActionId.CASH_EXPENSES, clickedAction)

        composeRule.onNodeWithTag("keuangan-menu-RECEIVABLE_PAYMENTS").performClick()
        assertEquals(KeuanganActionId.RECEIVABLE_PAYMENTS, clickedAction)

        composeRule.onNodeWithTag("keuangan-menu-FINANCE_REPORTS").performScrollTo().performClick()
        assertEquals(KeuanganActionId.FINANCE_REPORTS, clickedAction)

        composeRule.onNodeWithTag("keuangan-stat-receivables").performClick()
        assertEquals(KeuanganActionId.RECEIVABLES, clickedAction)

        composeRule.onNodeWithTag("keuangan-stat-payables").performClick()
        assertEquals(KeuanganActionId.SUPPLIER_DEBTS, clickedAction)

        // Active session label switch
        state = state.copy(hasActiveSession = true, balanceHidden = false)
        composeRule.onNodeWithText("Saldo kas aktif").assertIsDisplayed()
        composeRule.onNodeWithText("Kas aktif").assertIsDisplayed()
        composeRule.onNodeWithText("Sesi aktif").assertIsDisplayed()

        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performScrollTo().performClick()
        assertEquals(1, profileClicked)

        // Bottom nav callbacks
        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(DashboardNavDestination.BERANDA, clickedNav)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutDisplaysHeroSummaryAndAllMenuGroups() {
        var clickedAction: KeuanganActionId? = null
        var clickedNav: DashboardNavDestination? = null
        var profileClicked = 0

        composeRule.setContent {
            TbterminalappTheme {
                KeuanganScreen(
                    state = sampleReferenceKeuanganState().copy(
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

        composeRule.onNode(hasText("Keuangan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Akun Budi Santoso, offline").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-owner-avatar-btn").performClick()
        assertEquals(1, profileClicked)
        composeRule.onNodeWithTag("keuangan-hero-balance").assertIsDisplayed()
        composeRule.onNodeWithText("Rp 3.250.000").assertIsDisplayed()
        composeRule.onNodeWithText("Kas & pengeluaran").assertIsDisplayed()
        composeRule.onNodeWithText("Piutang & hutang").assertIsDisplayed()
        composeRule.onNodeWithText("Laporan & analitik").assertIsDisplayed()

        composeRule.onNodeWithTag("keuangan-menu-SUPPLIER_DEBTS").performClick()
        assertEquals(KeuanganActionId.SUPPLIER_DEBTS, clickedAction)

        composeRule.onNodeWithTag("nav-TRANSACTIONS").performClick()
        assertEquals(DashboardNavDestination.TRANSAKSI, clickedNav)
    }

    @Test
    fun heroMetricFormattingMatchesIndonesianCompactAndSpacedRules() {
        assertEquals("+Rp 480.000", formatKeuanganCashDelta(480_000.0))
        assertEquals("-Rp 50.000", formatKeuanganCashDelta(-50_000.0))
        assertEquals("Rp 380.000", formatKeuanganHeroMetric(380_000.0))
        assertEquals("Rp 1,25 jt", formatKeuanganHeroMetric(1_250_000.0))
    }
}
