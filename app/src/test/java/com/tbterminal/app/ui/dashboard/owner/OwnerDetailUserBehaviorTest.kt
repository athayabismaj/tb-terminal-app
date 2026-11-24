package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.users.StaffStatus
import com.tbterminal.app.ui.users.UserDetailUiState
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
class OwnerDetailUserBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun phoneDetailShowsUserInfoCopyUsernameActionsAndDeactivateFlow() {
        OwnerUserToastBridge.consumePendingSuccess()

        var state by mutableStateOf(
            UserDetailUiState(
                staff = sampleDetailUser(isActive = true),
                isLoading = false
            )
        )
        var backCount = 0
        var editedId: String? = null
        var submittedPassword: Pair<String, String>? = null
        var submittedPin: Pair<String, String>? = null
        var deactivateCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailUserRoute(
                    uiState = state,
                    onBack = { backCount++ },
                    onRetry = {},
                    onEditUserClick = { editedId = it },
                    onChangePassword = { newPass, confirmPass ->
                        submittedPassword = newPass to confirmPass
                        state = state.copy(credentialSuccessMessage = "Kata sandi berhasil diperbarui")
                    },
                    onChangePin = { newPin, confirmPin ->
                        submittedPin = newPin to confirmPin
                        state = state.copy(credentialSuccessMessage = "PIN transaksi berhasil diperbarui")
                    },
                    onClearCredentialMessages = {
                        state = state.copy(credentialErrorMessage = null, credentialSuccessMessage = null)
                    },
                    onDeactivateConfirm = {
                        deactivateCount++
                        state = state.copy(
                            staff = state.staff?.copy(status = StaffStatus.Inactive),
                            isDeactivated = true
                        )
                    },
                    onClearActionError = { state = state.copy(actionErrorMessage = null) },
                    onDeactivatedReset = { state = state.copy(isDeactivated = false) },
                    onDashboardClick = {}
                )
            }
        }

        // Secondary page hides bottom nav
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()

        // Hero: avatar dot + role badge, no redundant hero status badge
        composeRule.onNode(hasText("Detail pengguna") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("AP").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-name").assertIsDisplayed().assertTextEquals("Andi Pratama")
        composeRule.onNodeWithTag("detail-user-status-dot").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-role-badge").assertIsDisplayed()

        // Touch targets
        val backBounds = composeRule.onNodeWithTag("detail-user-back-btn").getBoundsInRoot()
        assertTrue((backBounds.right - backBounds.left) >= 48.dp)
        assertTrue((backBounds.bottom - backBounds.top) >= 48.dp)

        val copyBounds = composeRule.onNodeWithTag("detail-user-copy-username-btn").getBoundsInRoot()
        assertTrue((copyBounds.right - copyBounds.left) >= 44.dp)
        assertTrue((copyBounds.bottom - copyBounds.top) >= 44.dp)

        // Info sections + bottom status row
        composeRule.onNodeWithText("Informasi akun").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-username").assertTextEquals("@andi.admin")
        composeRule.onNodeWithTag("detail-user-email").assertTextEquals("andi@tbterminal.id")
        composeRule.onNodeWithTag("detail-user-role").assertTextEquals("Admin")
        composeRule.onNodeWithText("Aktivitas & status").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-last-login").assertTextEquals("15 menit lalu")
        composeRule.onNodeWithTag("detail-user-status").assertTextEquals("Aktif")

        // Copy username toast
        composeRule.onNodeWithTag("detail-user-copy-username-btn").performClick()
        composeRule.onNodeWithTag("detail-user-copy-toast").assertIsDisplayed()

        // Edit button
        composeRule.onNodeWithTag("detail-user-edit-btn").performClick()
        assertEquals("u1", editedId)

        // Keamanan menu (matching Detail Profile): Ubah kata sandi modal (no current password)
        composeRule.onNodeWithText("Keamanan").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-password-btn").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Ubah kata sandi").assertCountEquals(2)
        composeRule.onNodeWithTag("credential-close-btn").assertIsDisplayed()
        composeRule.onNodeWithTag("credential-current").assertDoesNotExist()
        composeRule.onNodeWithTag("credential-new").performTextReplacement("barokah25")
        composeRule.onNodeWithTag("credential-confirmation").performTextReplacement("barokah25")
        composeRule.onNodeWithTag("credential-save").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals("barokah25" to "barokah25", submittedPassword)
        composeRule.onNodeWithText("Kata sandi berhasil diperbarui").assertIsDisplayed()

        // Ubah PIN transaksi modal (2 steps: Buat PIN baru -> Ulangi PIN baru)
        composeRule.onNodeWithTag("detail-user-pin-btn").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Ubah PIN transaksi").assertCountEquals(2)
        composeRule.onNodeWithText("Langkah 1 dari 2").assertIsDisplayed()
        composeRule.onNodeWithText("Buat PIN baru").assertIsDisplayed()
        composeRule.onNodeWithTag("pin-forgot-link").assertDoesNotExist()
        listOf("3", "5", "7", "9", "2", "4").forEach { digit ->
            composeRule.onNodeWithTag("pin-key-$digit").performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Langkah 2 dari 2").assertIsDisplayed()
        composeRule.onNodeWithText("Ulangi PIN baru").assertIsDisplayed()
        listOf("3", "5", "7", "9", "2", "4").forEach { digit ->
            composeRule.onNodeWithTag("pin-key-$digit").performClick()
        }
        composeRule.waitForIdle()
        assertEquals("357924" to "357924", submittedPin)
        composeRule.onNodeWithText("PIN transaksi berhasil diperbarui").assertIsDisplayed()

        // Deactivate flow
        composeRule.onNodeWithTag("detail-user-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-user-deactivate-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-deactivate-cancel").performClick()
        composeRule.onNodeWithTag("detail-user-deactivate-dialog").assertDoesNotExist()
        assertEquals(0, deactivateCount)

        composeRule.onNodeWithTag("detail-user-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-user-deactivate-confirm").performClick()
        composeRule.waitForIdle()

        assertEquals(1, deactivateCount)
        assertEquals(1, backCount)
        assertEquals("Pengguna dinonaktifkan", OwnerUserToastBridge.consumePendingSuccess())
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutReactivatesInactiveUserAndSupportsUndo() {
        OwnerUserToastBridge.consumePendingSuccess()

        var state by mutableStateOf(
            UserDetailUiState(
                staff = sampleDetailUser(isActive = false).copy(
                    rawEmail = null,
                    email = "Email belum diisi"
                ),
                isLoading = false
            )
        )
        var activateCount = 0
        var undoCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailUserRoute(
                    uiState = state,
                    onBack = {},
                    onRetry = {},
                    onEditUserClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = {},
                    onDeactivatedReset = {},
                    onDashboardClick = {},
                    onActivateClick = {
                        activateCount++
                        state = state.copy(
                            staff = state.staff?.copy(status = StaffStatus.Active),
                            isReactivated = true
                        )
                    },
                    onUndoActivateClick = {
                        undoCount++
                        state = state.copy(
                            staff = state.staff?.copy(status = StaffStatus.Inactive),
                            isReactivated = false
                        )
                    },
                    onReactivatedReset = { state = state.copy(isReactivated = false) },
                    isOnlineOverride = true
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-email").assertTextEquals("Belum diisi")
        composeRule.onNodeWithTag("detail-user-status").assertTextEquals("Nonaktif")
        composeRule.onNodeWithTag("detail-user-deactivate-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-user-inactive-banner").assertIsDisplayed()

        composeRule.onNodeWithTag("detail-user-activate-btn")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(1, activateCount)
        composeRule.onNodeWithTag("detail-user-status").assertTextEquals("Aktif")
        composeRule.onNodeWithTag("detail-user-reactivated-toast").assertIsDisplayed()

        composeRule.onNodeWithText("Batalkan").performClick()
        composeRule.waitForIdle()
        assertEquals(1, undoCount)
        composeRule.onNodeWithTag("detail-user-status").assertTextEquals("Nonaktif")
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun offlineLoadingAndErrorStatesBehaveCorrectly() {
        var state by mutableStateOf(
            UserDetailUiState(
                staff = sampleDetailUser(isActive = false),
                isLoading = false
            )
        )
        var retryCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailUserRoute(
                    uiState = state,
                    onBack = {},
                    onRetry = { retryCount++ },
                    onEditUserClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = {},
                    onDeactivatedReset = {},
                    onDashboardClick = {},
                    isOnlineOverride = false
                )
            }
        }

        composeRule.onNodeWithTag("detail-user-activate-btn").assertIsNotEnabled()
        composeRule.onNodeWithText("Butuh koneksi internet untuk mengaktifkan pengguna.").assertIsDisplayed()

        state = UserDetailUiState(staff = null, isLoading = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail-user-loading-skeleton").assertIsDisplayed()

        state = UserDetailUiState(staff = null, isLoading = false, errorMessage = "Server error")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail-user-error-block").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-user-retry-btn").performClick()
        assertEquals(1, retryCount)
    }
}
