package com.tbterminal.app.ui.settings

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RepositoryResult
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
class SharedProfilePinCallbacksTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun sharedProfileScreen_wiresVerifyPinAndForceLogoutCallbacks() {
        val verifiedPins = mutableListOf<String>()
        var forceLogoutCalled = false

        composeRule.setContent {
            TbterminalappTheme {
                SharedProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = UserProfile("u1", "kasir", "Siti", "KASIR", null, true, "2026-01-01", null),
                    ),
                    onReload = {},
                    onChangePassword = { _, _, _ -> },
                    onChangePin = { _, _, _ -> },
                    onClearMessage = {},
                    onVerifyCurrentPin = { pin, onResult ->
                        verifiedPins += pin
                        if (pin == "135790") {
                            onResult(PinVerifyOutcome.Verified)
                        } else {
                            onResult(PinVerifyOutcome.ForceLogout("Sesi dicabut"))
                        }
                    },
                    onResetPinWithPassword = { _, _, _, _ -> },
                    onForceLogout = { forceLogoutCalled = true },
                )
            }
        }

        composeRule.onNodeWithTag("profile-action-pin").performClick()
        composeRule.onNodeWithText("Masukkan PIN saat ini").assertIsDisplayed()

        tapPin("135790")
        assertEquals(listOf("135790"), verifiedPins)
        composeRule.onNodeWithText("Buat PIN baru").assertIsDisplayed()

        composeRule.onNodeWithTag("credential-close-btn").performClick()
        composeRule.onNodeWithTag("profile-action-pin").performClick()

        tapPin("999999")
        assertTrue(forceLogoutCalled)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun cashierProfileScreen_wiresChangeAndResetPinFlowsAnd429ErrorToProfileViewModel() {
        val repo = FakeCashierProfileAuthRepository().apply {
            resetPinResponse = RepositoryResult.Error(
                code = "TOO_MANY_ATTEMPTS",
                message = "Terlalu banyak percobaan gagal. Coba lagi dalam 2 menit.",
                retryAfterSeconds = 120L,
            )
        }
        val vm = ProfileViewModel(repo)
        var logoutCalled = false

        composeRule.setContent {
            TbterminalappTheme {
                CashierProfileScreen(
                    userName = "Siti",
                    role = "KASIR",
                    authRepository = repo,
                    onLogout = { logoutCalled = true },
                    profileViewModel = vm,
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile-action-pin").performClick()
        composeRule.onNodeWithTag("pin-forgot-link").performClick()
        composeRule.onNodeWithTag("reset-pin-dialog").assertIsDisplayed()

        composeRule.onNodeWithTag("reset-pin-password").performTextReplacement("rahasia123")
        composeRule.onNodeWithTag("reset-pin-new").performTextReplacement("2581")
        composeRule.onNodeWithTag("reset-pin-confirm").performTextReplacement("2581")
        composeRule.onNodeWithTag("reset-pin-save").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("rahasia123" to "2581"), repo.resetCalls)
        assertTrue(
            composeRule.onAllNodesWithText("Terlalu banyak percobaan gagal. Coba lagi dalam 2 menit.")
                .fetchSemanticsNodes().isNotEmpty()
        )

        // Succeed on next reset attempt after clearing inputs
        repo.resetPinResponse = RepositoryResult.Success(Unit)
        composeRule.onNodeWithTag("reset-pin-password").performTextReplacement("rahasia123")
        composeRule.onNodeWithTag("reset-pin-new").performTextReplacement("258147")
        composeRule.onNodeWithTag("reset-pin-confirm").performTextReplacement("258147")
        composeRule.onNodeWithTag("reset-pin-save").performClick()
        composeRule.waitForIdle()

        assertEquals(2, repo.resetCalls.size)
        assertEquals("258147", repo.resetCalls.last().second)
        composeRule.onNodeWithTag("reset-pin-dialog").assertDoesNotExist()
        assertEquals(false, logoutCalled)
    }

    private fun tapPin(digits: String) {
        digits.forEach { digit ->
            composeRule.onNodeWithTag("pin-key-$digit").performClick()
        }
    }
}

private class FakeCashierProfileAuthRepository : AuthRepository {
    val unlockCalls = mutableListOf<String>()
    val resetCalls = mutableListOf<Pair<String, String>>()
    var unlockResponse: RepositoryResult<PinUnlockResult> = RepositoryResult.Success(PinUnlockResult.Unlocked)
    var resetPinResponse: RepositoryResult<Unit> = RepositoryResult.Success(Unit)

    override suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession> = error("unused")
    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> {
        unlockCalls += pin
        return unlockResponse
    }
    override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> {
        resetCalls += currentPassword to newPin
        return resetPinResponse
    }
    override suspend fun getProfile(): RepositoryResult<UserProfile> = RepositoryResult.Success(
        UserProfile("u1", "kasir", "Siti", "KASIR", null, true, "2026-01-01", null)
    )
    override suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit> =
        RepositoryResult.Success(Unit)
    override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> =
        RepositoryResult.Success(Unit)
}
