package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
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
class OwnerMenuBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h988dp-mdpi")
    fun phoneLayoutRendersHeaderMenuGroupsTouchTargetsAndCallbacks() {
        var profileClicked = false
        val clickedDestinations = mutableListOf<AdminDestination>()
        var clickedNav: DashboardNavDestination? = null

        composeRule.setContent {
            TbterminalappTheme {
                MenuScreen(
                    state = sampleReferenceMenuState(),
                    onProfileClick = { profileClicked = true },
                    onDestinationClick = { clickedDestinations += it },
                    onNavSelect = { clickedNav = it }
                )
            }
        }

        // Header profile
        composeRule.onNode(hasText("Menu") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Pemilik Toko").assertIsDisplayed()
        composeRule.onNodeWithText("Owner").assertIsDisplayed()
        composeRule.onNodeWithText("· Terminal Barokah Jaya").assertIsDisplayed()

        // Touch target sizing checks
        val avatarBounds = composeRule.onNodeWithTag("owner-menu-avatar-btn").getBoundsInRoot()
        assertTrue((avatarBounds.right - avatarBounds.left) >= 48.dp)
        assertTrue((avatarBounds.bottom - avatarBounds.top) >= 48.dp)

        val rowBounds = composeRule.onNodeWithTag("owner-menu-item-customers").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        // Two plain menu groups & 7 rows
        composeRule.onNodeWithText("Kelola toko").assertIsDisplayed()
        composeRule.onNodeWithText("Pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Pengguna & akses").assertIsDisplayed()

        composeRule.onNodeWithText("Sistem & keamanan").assertIsDisplayed()
        composeRule.onNodeWithText("Sinkronisasi").assertIsDisplayed()
        composeRule.onNodeWithText("Terakhir 2 menit lalu").assertIsDisplayed()
        composeRule.onNodeWithText("Cadangan data").assertIsDisplayed()
        composeRule.onNodeWithText("Riwayat aktivitas").assertIsDisplayed()
        composeRule.onNodeWithText("Log keamanan").assertIsDisplayed()

        // Click interactions
        composeRule.onNodeWithTag("owner-menu-avatar-btn").performClick()
        assertTrue(profileClicked)

        composeRule.onNodeWithTag("owner-menu-item-customers").performClick()
        composeRule.onNodeWithTag("owner-menu-item-security").performClick()
        assertEquals(listOf(AdminDestination.Customers, AdminDestination.SecurityLog), clickedDestinations)

        composeRule.onNodeWithTag("nav-FINANCE").performClick()
        assertEquals(DashboardNavDestination.KEUANGAN, clickedNav)
    }

    @Test
    @Config(qualifiers = "w390dp-h988dp-mdpi")
    fun longUserNameAndStoreNameTruncateWithoutCollidingWithAvatarOrBadge() {
        val longState = OwnerMenuUiState(
            userName = "Muhammad Abdurrahman Hidayatullah Kusumanegara Putra Wijaya",
            role = "OWNER",
            storeName = "Terminal Bangunan Barokah Jaya Cabang Utama Timur",
            synced = true,
            syncSubtitle = "Terakhir 2 menit lalu"
        )

        composeRule.setContent {
            TbterminalappTheme {
                MenuScreen(
                    state = longState,
                    onProfileClick = {},
                    onDestinationClick = {},
                    onNavSelect = {}
                )
            }
        }

        val avatarBounds = composeRule.onNodeWithTag("owner-menu-avatar-btn").getBoundsInRoot()
        val nameBounds = composeRule.onNodeWithTag("owner-menu-profile-name").getBoundsInRoot()
        val badgeBounds = composeRule.onNodeWithTag("owner-menu-role-badge").getBoundsInRoot()
        val storeBounds = composeRule.onNodeWithTag("owner-menu-store-name").getBoundsInRoot()

        assertTrue(nameBounds.top >= avatarBounds.bottom)
        assertTrue(nameBounds.right <= 371.dp)
        assertTrue(storeBounds.left >= badgeBounds.right)
        assertTrue(storeBounds.right <= 371.dp)
    }

    @Test
    @Config(qualifiers = "w390dp-h988dp-mdpi")
    fun cashierRoleHidesUserManagementBackupAndSecurityLog() {
        composeRule.setContent {
            TbterminalappTheme {
                MenuScreen(
                    state = sampleReferenceMenuState(role = "KASIR", userName = "Kasir Pagi"),
                    onProfileClick = {},
                    onDestinationClick = {},
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithText("Kasir").assertIsDisplayed()
        composeRule.onNodeWithText("Pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Pengguna & akses").assertDoesNotExist()
        composeRule.onNodeWithText("Cadangan data").assertDoesNotExist()
        composeRule.onNodeWithText("Log keamanan").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutRendersNavRailGreenPanelAndWhiteMenuPanel() {
        var clickedDestination: AdminDestination? = null

        composeRule.setContent {
            TbterminalappTheme {
                MenuScreen(
                    state = sampleReferenceMenuState(),
                    onProfileClick = {},
                    onDestinationClick = { clickedDestination = it },
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNode(hasText("Menu") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Pemilik Toko").assertIsDisplayed()
        composeRule.onNodeWithText("Kelola toko").assertIsDisplayed()
        composeRule.onNodeWithText("Sistem & keamanan").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-menu-item-sync").performClick()
        assertEquals(AdminDestination.SyncCenter, clickedDestination)
    }
}
