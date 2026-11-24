package com.tbterminal.app.ui.settings

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
class CredentialChangeModalTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun passwordModalChecklistMismatchForgotAndSubmitWork() {
        var submitted: Triple<String, String, String>? = null
        var forgotClicked = false

        composeRule.setContent {
            TbterminalappTheme {
                CredentialChangeDialog(
                    type = CredentialDialog.PASSWORD,
                    isSaving = false,
                    errorMessage = null,
                    onInputChanged = {},
                    onDismiss = {},
                    onSubmit = { o, n, c -> submitted = Triple(o, n, c) },
                    onForgotPasswordClick = { forgotClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("Ubah kata sandi").assertIsDisplayed()
        composeRule.onNodeWithText("Batal").assertDoesNotExist()
        composeRule.onNodeWithText("Minimal 8 karakter").assertIsDisplayed()
        composeRule.onNodeWithText("Ada huruf dan angka").assertIsDisplayed()
        composeRule.onNodeWithText("Berbeda dari kata sandi saat ini").assertIsDisplayed()
        composeRule.onNodeWithTag("credential-save").assertIsNotEnabled()

        val closeBounds = composeRule.onNodeWithTag("credential-close-btn").getBoundsInRoot()
        assertTrue((closeBounds.right - closeBounds.left) >= 48.dp)
        assertTrue((closeBounds.bottom - closeBounds.top) >= 48.dp)

        val saveBounds = composeRule.onNodeWithTag("credential-save").getBoundsInRoot()
        assertTrue((saveBounds.bottom - saveBounds.top) >= 52.dp)

        composeRule.onNodeWithTag("credential-current").performTextReplacement("kasirlama1")
        composeRule.onNodeWithTag("credential-new").performTextReplacement("barokah25")
        composeRule.onNodeWithTag("credential-confirmation").performTextReplacement("barokah2")

        composeRule.onNodeWithText("Kata sandi tidak sama").assertIsDisplayed()
        composeRule.onNodeWithTag("credential-save").assertIsNotEnabled()

        composeRule.onNodeWithTag("credential-confirmation").performTextReplacement("barokah25")
        composeRule.onNodeWithText("Kata sandi tidak sama").assertDoesNotExist()
        composeRule.onNodeWithTag("credential-save").assertIsEnabled().performClick()
        assertEquals(Triple("kasirlama1", "barokah25", "barokah25"), submitted)

        val over72 = "a1".repeat(37)
        composeRule.onNodeWithTag("credential-new").performTextReplacement(over72)
        composeRule.onNodeWithTag("credential-confirmation").performTextReplacement(over72)
        composeRule.onNodeWithText("Password maksimal 72 karakter").assertIsDisplayed()
        composeRule.onNodeWithTag("credential-save").assertIsNotEnabled()

        composeRule.onNodeWithTag("password-forgot-link").performClick()
        assertTrue(forgotClicked)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun passwordModalShowsWrongCurrentPasswordErrorUnderFirstField() {
        composeRule.setContent {
            TbterminalappTheme {
                CredentialChangeDialog(
                    type = CredentialDialog.PASSWORD,
                    isSaving = false,
                    errorMessage = "Kata sandi saat ini salah",
                    onInputChanged = {},
                    onDismiss = {},
                    onSubmit = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Kata sandi saat ini salah").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun pinThreeStepFlowHandlesErrorsRejectionsAndAutoSubmits() {
        var submittedPin: Triple<String, String, String>? = null
        var attemptCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                CredentialChangeDialog(
                    type = CredentialDialog.PIN,
                    isSaving = false,
                    errorMessage = null,
                    onInputChanged = {},
                    onDismiss = {},
                    onVerifyCurrentPin = { pin, onResult ->
                        attemptCount++
                        if (pin == "135790") {
                            onResult(PinVerifyOutcome.Verified)
                        } else {
                            onResult(PinVerifyOutcome.Rejected(attemptsLeft = 4, isLocked = false, message = "PIN salah"))
                        }
                    },
                    onSubmit = { o, n, c -> submittedPin = Triple(o, n, c) }
                )
            }
        }

        composeRule.onNodeWithText("Ubah PIN transaksi").assertIsDisplayed()
        composeRule.onNodeWithText("Langkah 1 dari 3").assertIsDisplayed()
        composeRule.onNodeWithText("Masukkan PIN saat ini").assertIsDisplayed()

        val keyBounds = composeRule.onNodeWithTag("pin-key-1").getBoundsInRoot()
        assertTrue((keyBounds.bottom - keyBounds.top) >= 52.dp)
        val forgotBounds = composeRule.onNodeWithTag("pin-forgot-link").getBoundsInRoot()
        assertTrue((forgotBounds.bottom - forgotBounds.top) >= 44.dp)

        fun tapPin(digits: String) {
            digits.forEach { d ->
                composeRule.onNodeWithTag("pin-key-$d").performClick()
            }
        }

        // Wrong Step 1 PIN
        tapPin("999999")
        composeRule.onNodeWithText("PIN salah. Sisa 4 percobaan.").assertIsDisplayed()
        composeRule.onNodeWithText("Langkah 1 dari 3").assertIsDisplayed()

        // Valid Step 1 PIN -> advances to Step 2
        tapPin("135790")
        composeRule.onNodeWithText("Langkah 2 dari 3").assertIsDisplayed()
        composeRule.onNodeWithText("Buat PIN baru").assertIsDisplayed()

        // Sequential PIN rejected
        tapPin("123456")
        composeRule.onNodeWithText("PIN terlalu mudah ditebak").assertIsDisplayed()
        composeRule.onNodeWithText("Langkah 2 dari 3").assertIsDisplayed()

        // All identical PIN rejected
        tapPin("111111")
        composeRule.onNodeWithText("PIN terlalu mudah ditebak").assertIsDisplayed()

        // Same as current PIN rejected
        tapPin("135790")
        composeRule.onNodeWithText("PIN baru harus berbeda").assertIsDisplayed()

        // Valid new PIN -> advances to Step 3
        tapPin("258147")
        composeRule.onNodeWithText("Langkah 3 dari 3").assertIsDisplayed()
        composeRule.onNodeWithText("Ulangi PIN baru").assertIsDisplayed()

        // Mismatch in Step 3 -> resets back to Step 2 with error
        tapPin("258148")
        composeRule.onNodeWithText("PIN tidak sama. Ulangi dari awal.").assertIsDisplayed()
        composeRule.onNodeWithText("Langkah 2 dari 3").assertIsDisplayed()

        // Re-enter Step 2 & Step 3 matching -> auto-submits!
        tapPin("258147")
        tapPin("258147")
        assertEquals(Triple("135790", "258147", "258147"), submittedPin)
        assertTrue(attemptCount >= 2)
    }
}
