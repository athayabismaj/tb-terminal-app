package com.tbterminal.app.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
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
class LoginScreenBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneToggleHas44dpTouchTargetAndTogglesDescription() {
        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    state = LoginUiState(initialPassword = "katasandi"),
                    onLogin = { _, _ -> },
                    onForgot = {},
                    onRetry = {}
                )
            }
        }

        val toggle = composeRule.onNodeWithTag("login-password-toggle")
        toggle.assertWidthIsEqualTo(44.dp)
        toggle.assertHeightIsEqualTo(44.dp)
        composeRule.onNodeWithContentDescription("Tampilkan kata sandi").assertIsDisplayed()

        toggle.performClick()
        composeRule.onNodeWithContentDescription("Sembunyikan kata sandi").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletToggleHas48dpTouchTarget() {
        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    state = LoginUiState(initialPassword = "katasandi"),
                    onLogin = { _, _ -> },
                    onForgot = {},
                    onRetry = {}
                )
            }
        }

        val toggle = composeRule.onNodeWithTag("login-password-toggle")
        toggle.assertWidthIsEqualTo(48.dp)
        toggle.assertHeightIsEqualTo(48.dp)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun loginForgotAndRetryCallbacksWorkStatelessly() {
        var submittedUsername = ""
        var submittedPassword = ""
        var forgotClicked = false
        var retryClicked = false

        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    state = LoginUiState(
                        alert = LoginAlertState(
                            type = AlertType.Error,
                            title = "Tidak dapat terhubung",
                            message = "Periksa koneksi internet.",
                            actionLabel = "Coba lagi"
                        )
                    ),
                    onLogin = { u, p ->
                        submittedUsername = u
                        submittedPassword = p
                    },
                    onForgot = { forgotClicked = true },
                    onRetry = { retryClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("login-username-input").performTextInput("admin.toko")
        composeRule.onNodeWithTag("login-password-input").performTextInput("rahasia123")
        composeRule.onNodeWithTag("login-submit-button").performClick()

        assertEquals("admin.toko", submittedUsername)
        assertEquals("rahasia123", submittedPassword)

        composeRule.onNodeWithTag("login-forgot-password-link").performClick()
        assertTrue(forgotClicked)

        composeRule.onNodeWithTag("app-alert-action").performClick()
        assertTrue(retryClicked)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun imeDoneOnPasswordSubmitsLogin() {
        var submittedUsername = ""
        var submittedPassword = ""

        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    state = LoginUiState(),
                    onLogin = { u, p ->
                        submittedUsername = u
                        submittedPassword = p
                    },
                    onForgot = {},
                    onRetry = {}
                )
            }
        }

        composeRule.onNodeWithTag("login-username-input").performTextInput("kasir1")
        composeRule.onNodeWithTag("login-username-input").performImeAction()
        composeRule.onNodeWithTag("login-password-input").performTextInput("sandi123")
        composeRule.onNodeWithTag("login-password-input").performImeAction()

        assertEquals("kasir1", submittedUsername)
        assertEquals("sandi123", submittedPassword)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun idlePhoneDoesNotShowVersionFooter() {
        composeRule.setContent {
            TbterminalappTheme {
                LoginContent(
                    state = LoginUiState(),
                    imeVisible = false,
                    onLogin = { _, _ -> },
                    onForgot = {},
                    onRetry = {}
                )
            }
        }
        composeRule.onNodeWithText("Terminal · v1.0.0").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun keyboardOpenHidesFooterAndDisplaysFieldError() {
        composeRule.setContent {
            TbterminalappTheme {
                LoginContent(
                    state = LoginUiState(
                        initialUsername = "owner",
                        initialPassword = "123",
                        passwordError = "Kata sandi minimal 6 karakter"
                    ),
                    imeVisible = true,
                    onLogin = { _, _ -> },
                    onForgot = {},
                    onRetry = {}
                )
            }
        }

        composeRule.onNodeWithText("Kata sandi minimal 6 karakter").assertIsDisplayed()
        composeRule.onNodeWithText("Sistem Kasir & Inventaris").assertDoesNotExist()
        composeRule.onNodeWithText("Terminal · v1.0.0").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun successAlertAutoDismissesAfter4Seconds() {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            TbterminalappTheme {
                AppAlert(
                    type = AlertType.Success,
                    title = "Berhasil",
                    message = "Data tersimpan.",
                    visible = visible,
                    onDismiss = { visible = false }
                )
            }
        }

        composeRule.onNodeWithText("Berhasil").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(4100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Berhasil").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun submittingEmptyFieldsHighlightsBothInputsWithInlineErrorsAndNoPopupAlert() {
        val viewModel = AuthViewModel(FakeLoginAuthRepository())

        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("login-submit-button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("login-username-input")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        composeRule.onNodeWithTag("login-password-input")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        composeRule.onNodeWithText("Nama pengguna wajib diisi").assertIsDisplayed()
        composeRule.onNodeWithText("Kata sandi wajib diisi").assertIsDisplayed()
        composeRule.onNodeWithText("Kolom wajib diisi").assertDoesNotExist()
        composeRule.onNodeWithTag("app-alert-card").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun wrongCredentialsHighlightsBothInputsWithSingleMessageUnderPassword() {
        val viewModel = AuthViewModel(
            FakeLoginAuthRepository(
                loginResult = RepositoryResult.Error("UNAUTHORIZED", "Username atau Password salah")
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("login-username-input").performTextInput("kasir1")
        composeRule.onNodeWithTag("login-password-input").performTextInput("salah123")
        composeRule.onNodeWithTag("login-submit-button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("login-username-input")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        composeRule.onNodeWithTag("login-password-input")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        composeRule.onNodeWithTag("login-username-input-error").assertDoesNotExist()
        composeRule.onNodeWithTag("login-password-input-error").assertIsDisplayed()
        composeRule.onNodeWithText("Nama pengguna atau kata sandi salah").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun passwordChangedNotificationDisplaysLoginMessageWithoutErrorHighlights() {
        val viewModel = AuthViewModel(FakeLoginAuthRepository())

        composeRule.setContent {
            TbterminalappTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { _, _ -> }
                )
            }
        }

        composeRule.runOnIdle {
            viewModel.notifyPasswordChanged()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("app-alert-card").assertIsDisplayed()
        composeRule.onNodeWithText("Password diubah").assertIsDisplayed()
        composeRule.onNodeWithText("Password diubah, silakan login kembali").assertIsDisplayed()
    }
}

private class FakeLoginAuthRepository(
    private val loginResult: RepositoryResult<AuthenticatedSession> = RepositoryResult.Error("UNAUTHORIZED", "Username atau Password salah")
) : AuthRepository {
    override suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession> = loginResult
    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> = error("not used")
    override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> = error("not used")
    override suspend fun getProfile(): RepositoryResult<UserProfile> = error("not used")
    override suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit> = error("not used")
    override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> = error("not used")
}
