package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class OwnerProfileDetailBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun phoneLayoutHidesBottomNavChecksTouchTargetsLongTextAndActions() {
        var backClicked = false
        var copyClicked = false
        var editClicked = false
        var passwordClicked = false
        var pinClicked = false
        var logoutClicked = false

        composeRule.setContent {
            TbterminalappTheme {
                DetailProfilScreen(
                    state = sampleReferenceProfileState(),
                    onBack = { backClicked = true },
                    onCopyUserId = { copyClicked = true },
                    onEditProfileClick = { editClicked = true },
                    onChangePasswordClick = { passwordClicked = true },
                    onChangePinClick = { pinClicked = true },
                    onLogoutClick = { logoutClicked = true },
                    onNavSelect = {}
                )
            }
        }

        // Secondary page: bottom navigation MUST NOT be shown on phone
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()

        // Header & identity
        composeRule.onNode(hasText("Detail profil") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("PT").assertIsDisplayed()
        composeRule.onNodeWithText("Pemilik Toko").assertIsDisplayed()
        composeRule.onAllNodesWithText("Owner").assertCountEquals(2)
        composeRule.onNodeWithText("owner@gmail.com").assertIsDisplayed()

        // Info akun
        composeRule.onNodeWithText("Info akun").assertIsDisplayed()
        composeRule.onNodeWithText("KSR-646").assertIsDisplayed()
        composeRule.onNodeWithTag("profile-role-value").assertIsDisplayed().assertTextEquals("Owner")
        composeRule.onNodeWithText("Terminal Barokah Jaya").assertIsDisplayed()

        assertEquals(null, resolveDisplayStoreName("TB TERMINAL"))
        assertEquals("Owner", formatProfileAccountRoleLabel("OWNER"))
        assertEquals("Admin", formatProfileAccountRoleLabel("ADMIN"))
        assertEquals("Kasir", formatProfileAccountRoleLabel("KASIR"))
        assertEquals("OWN-789", formatShortProfileUserId("a1b2c3d4-e5f6-7890-1234-567890abcd789", "OWNER", "owner"))
        assertEquals("KSR-646", formatShortProfileUserId("KSR-646", "OWNER", "owner"))

        // Touch targets
        val backBounds = composeRule.onNodeWithTag("profile-back-btn").getBoundsInRoot()
        assertTrue((backBounds.right - backBounds.left) >= 48.dp)
        assertTrue((backBounds.bottom - backBounds.top) >= 48.dp)

        val copyBounds = composeRule.onNodeWithTag("profile-copy-id-btn").getBoundsInRoot()
        assertTrue((copyBounds.right - copyBounds.left) >= 44.dp)
        assertTrue((copyBounds.bottom - copyBounds.top) >= 44.dp)

        val rowBounds = composeRule.onNodeWithTag("profile-menu-edit").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        val logoutBounds = composeRule.onNodeWithTag("profile-logout-btn").getBoundsInRoot()
        assertTrue((logoutBounds.bottom - logoutBounds.top) >= 52.dp)

        // Click interactions
        composeRule.onNodeWithTag("profile-back-btn").performClick()
        assertTrue(backClicked)

        composeRule.onNodeWithTag("profile-copy-id-btn").performClick()
        assertTrue(copyClicked)

        composeRule.onNodeWithTag("profile-menu-edit").performClick()
        composeRule.onNodeWithTag("profile-menu-password").performClick()
        composeRule.onNodeWithTag("profile-menu-pin").performClick()
        assertTrue(editClicked && passwordClicked && pinClicked)

        composeRule.onNodeWithTag("profile-logout-btn").performScrollTo().performClick()
        assertTrue(logoutClicked)
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun longNameAndLongEmailTruncateCleanlyWithinBounds() {
        val longState = ProfileDetailUiState(
            userName = "Muhammad Abdurrahman Hidayatullah Kusumanegara Putra Wijaya",
            role = "OWNER",
            email = "owner.super.panjang.barokah.jaya.bangunan@gmail.com",
            userId = "USR-998877",
            storeName = "Terminal Barokah Jaya",
            synced = true
        )

        composeRule.setContent {
            TbterminalappTheme {
                DetailProfilScreen(
                    state = longState,
                    onBack = {},
                    onCopyUserId = {},
                    onEditProfileClick = {},
                    onChangePasswordClick = {},
                    onChangePinClick = {},
                    onLogoutClick = {},
                    onNavSelect = {}
                )
            }
        }

        val nameBounds = composeRule.onNodeWithTag("profile-detail-name").getBoundsInRoot()
        val badgeBounds = composeRule.onNodeWithTag("profile-role-badge").getBoundsInRoot()
        val emailBounds = composeRule.onNodeWithTag("profile-detail-email").getBoundsInRoot()

        assertTrue(nameBounds.right <= 371.dp)
        assertTrue(emailBounds.left >= badgeBounds.right)
        assertTrue(emailBounds.right <= 371.dp)
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun blankEmailHidesEmailNodeWithoutTodoPlaceholder() {
        composeRule.setContent {
            TbterminalappTheme {
                DetailProfilScreen(
                    state = sampleReferenceProfileState().copy(email = null),
                    onBack = {},
                    onCopyUserId = {},
                    onEditProfileClick = {},
                    onChangePasswordClick = {},
                    onChangePinClick = {},
                    onLogoutClick = {},
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithTag("profile-detail-email").assertDoesNotExist()
        composeRule.onNodeWithText("— TODO: email akun").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun bottomBarHidesOnDetailProfilAndSecondaryRoutesThenReturnsOnMenu() {
        var showDetailProfil by mutableStateOf(false)

        composeRule.setContent {
            TbterminalappTheme {
                if (showDetailProfil) {
                    DetailProfilScreen(
                        state = sampleReferenceProfileState(),
                        onBack = { showDetailProfil = false },
                        onCopyUserId = {},
                        onEditProfileClick = {},
                        onChangePasswordClick = {},
                        onChangePinClick = {},
                        onLogoutClick = {},
                        onNavSelect = {}
                    )
                } else {
                    MenuScreen(
                        state = sampleReferenceMenuState(),
                        onProfileClick = { showDetailProfil = true },
                        onDestinationClick = {},
                        onNavSelect = {}
                    )
                }
            }
        }

        // Visible on Menu
        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()

        // Open Detail profil -> hidden
        composeRule.onNodeWithTag("owner-menu-avatar-btn").performClick()
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()

        // Back to Menu -> visible again
        composeRule.onNodeWithTag("profile-back-btn").performClick()
        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun adminDashboardShellHidesBottomBarOnSecondaryDestinations() {
        composeRule.setContent {
            TbterminalappTheme {
                AdminDashboardShell(
                    userName = "Pemilik Toko",
                    role = "OWNER",
                    activeDestination = AdminDestination.Products,
                    onDashboardClick = {},
                    onProductsClick = {},
                    onLogout = {}
                ) {}
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun logoutConfirmationDialogCancelsAndConfirms() {
        var showDialog by mutableStateOf(true)
        var confirmed = false

        composeRule.setContent {
            TbterminalappTheme {
                if (showDialog) {
                    ProfileLogoutConfirmDialog(
                        isBusy = false,
                        onDismiss = { showDialog = false },
                        onConfirm = {
                            showDialog = false
                            confirmed = true
                        }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Keluar dari akun?").assertIsDisplayed()
        composeRule.onNodeWithText("Kamu perlu masuk lagi dengan nama pengguna dan kata sandi.").assertIsDisplayed()
        composeRule.onNodeWithTag("profile-logout-cancel").performClick()
        assertFalse(confirmed)

        showDialog = true
        composeRule.onNodeWithTag("account-confirm-logout").performClick()
        assertTrue(confirmed)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutKeepsNavRailWithMenuActive() {
        composeRule.setContent {
            TbterminalappTheme {
                DetailProfilScreen(
                    state = sampleReferenceProfileState(),
                    onBack = {},
                    onCopyUserId = {},
                    onEditProfileClick = {},
                    onChangePasswordClick = {},
                    onChangePinClick = {},
                    onLogoutClick = {},
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNode(hasText("Detail profil") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Info akun").assertIsDisplayed()
        composeRule.onNodeWithText("Keamanan").assertIsDisplayed()
        composeRule.onNodeWithTag("profile-logout-btn").assertIsDisplayed()
    }
}
