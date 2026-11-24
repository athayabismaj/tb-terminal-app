package com.tbterminal.app.ui.sync

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.tbterminal.app.data.sync.OfflineSessionAgePolicy
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cashier.session.CashierCashSessionScreen
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OfflineSessionAgeBannerUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cashierCashSessionScreen_displays48hWarningBanner() {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        val warningAlert = OfflineSessionAgePolicy.evaluateOpenedAt(openedAt, openedAt + 48 * hourMs)!!

        composeRule.setContent {
            TbterminalappTheme {
                CashierCashSessionScreen(
                    uiState = CashReconciliationUiState(
                        isUsingLocalActiveSession = true,
                        offlineSessionAgeAlert = warningAlert
                    ),
                    onOpeningCashChanged = {},
                    onOpenSession = {},
                    onClosingCashChanged = {},
                    onClosingNotesChanged = {},
                    onCloseSession = {},
                    onShowExpenseDialog = {},
                    onTransactionHistoryClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("offline-session-age-warning-banner").assertIsDisplayed()
        composeRule.onNodeWithText(warningAlert.message).assertIsDisplayed()
        composeRule.onNodeWithText("Segera sambungkan internet agar transaksi bisa tersinkron", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun cashierCashSessionScreen_displays66hCriticalBanner() {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        val criticalAlert = OfflineSessionAgePolicy.evaluateOpenedAt(openedAt, openedAt + 66 * hourMs)!!

        composeRule.setContent {
            TbterminalappTheme {
                CashierCashSessionScreen(
                    uiState = CashReconciliationUiState(
                        isUsingLocalActiveSession = true,
                        offlineSessionAgeAlert = criticalAlert
                    ),
                    onOpeningCashChanged = {},
                    onOpenSession = {},
                    onClosingCashChanged = {},
                    onClosingNotesChanged = {},
                    onCloseSession = {},
                    onShowExpenseDialog = {},
                    onTransactionHistoryClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("offline-session-age-critical-banner").assertIsDisplayed()
        composeRule.onNodeWithText(criticalAlert.message).assertIsDisplayed()
        composeRule.onNodeWithText("Segera sambungkan internet agar transaksi bisa tersinkron", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun syncCenterScreen_displays48hWarningBanner() {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        val warningAlert = OfflineSessionAgePolicy.evaluateOpenedAt(openedAt, openedAt + 48 * hourMs)!!

        composeRule.setContent {
            TbterminalappTheme {
                SyncCenterScreen(
                    uiState = SyncCenterUiState(
                        isInitialLoading = false,
                        offlineSessionAgeAlert = warningAlert
                    ),
                    onRetryItem = {},
                    onMarkConflictReviewed = {},
                    onRetryAllPending = {},
                    onRetryAllFailed = {},
                    onDismissMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("offline-session-age-warning-banner").assertIsDisplayed()
        composeRule.onNodeWithText(warningAlert.message).assertIsDisplayed()
        composeRule.onNodeWithText("Segera sambungkan internet agar transaksi bisa tersinkron", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun syncCenterScreen_displays66hCriticalBanner() {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        val criticalAlert = OfflineSessionAgePolicy.evaluateOpenedAt(openedAt, openedAt + 66 * hourMs)!!

        composeRule.setContent {
            TbterminalappTheme {
                SyncCenterScreen(
                    uiState = SyncCenterUiState(
                        isInitialLoading = false,
                        offlineSessionAgeAlert = criticalAlert
                    ),
                    onRetryItem = {},
                    onMarkConflictReviewed = {},
                    onRetryAllPending = {},
                    onRetryAllFailed = {},
                    onDismissMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("offline-session-age-critical-banner").assertIsDisplayed()
        composeRule.onNodeWithText(criticalAlert.message).assertIsDisplayed()
        composeRule.onNodeWithText("Segera sambungkan internet agar transaksi bisa tersinkron", substring = true)
            .assertIsDisplayed()
    }
}
