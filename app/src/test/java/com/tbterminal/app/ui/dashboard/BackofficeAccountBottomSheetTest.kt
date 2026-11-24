package com.tbterminal.app.ui.dashboard

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class BackofficeAccountBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ownerProfilePopupRemovesDuplicateMenuRows() {
        composeRule.setContent {
            TbterminalappTheme {
                BackofficeAccountBottomSheet(
                    userName = "Pemilik Toko",
                    role = "OWNER",
                    offlineStatus = OfflineStatus(),
                    devicePreferences = DevicePreferences(),
                    onDismiss = {},
                    onProfileClick = {},
                    onSettingsClick = {},
                    onSyncCenterClick = {},
                    onReportsClick = {},
                    onStoreSettingsClick = {},
                    onLogout = {}
                )
            }
        }

        composeRule.onNodeWithTag("account-row-profile").assertIsDisplayed()
        composeRule.onNodeWithTag("account-row-settings").assertIsDisplayed()
        composeRule.onNodeWithTag("account-row-store-settings").assertIsDisplayed()

        // Duplicate entries pointing to Profile, Settings, or Keuangan Reports are gone
        composeRule.onNodeWithTag("account-row-pin").assertDoesNotExist()
        composeRule.onNodeWithTag("account-row-printer").assertDoesNotExist()
        composeRule.onNodeWithTag("account-row-reports").assertDoesNotExist()
        composeRule.onNodeWithText("Ganti PIN / Kata Sandi").assertDoesNotExist()
        composeRule.onNodeWithText("Printer Struk (58mm)").assertDoesNotExist()
        composeRule.onNodeWithText("Laporan Bisnis & Analitik").assertDoesNotExist()
    }
}
