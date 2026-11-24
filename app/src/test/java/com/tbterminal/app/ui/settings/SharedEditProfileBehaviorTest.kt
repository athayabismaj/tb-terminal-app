package com.tbterminal.app.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class SharedEditProfileBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun editProfilePopulatesAsyncAndAllowsEditingNameUsernameAndEmail() {
        var uiState by mutableStateOf(ProfileUiState(isLoading = true, profile = null))
        var savedTuple: Triple<String, String, String?>? = null

        composeRule.setContent {
            TbterminalappTheme {
                SharedEditProfileScreen(
                    uiState = uiState,
                    onSave = { n, u, e -> savedTuple = Triple(n, u, e) },
                    onBack = {},
                    onSaved = {},
                    onClearMessage = {}
                )
            }
        }

        // Simulate profile finishing load after loading skeleton
        uiState = ProfileUiState(
            isLoading = false,
            profile = UserProfile(
                id = "KSR-646",
                username = "owner.lama",
                name = "Pemilik Toko",
                role = "OWNER",
                email = "owner@gmail.com",
                isActive = true,
                joinedAt = "2026-01-01",
                lastLoginAt = null
            )
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ubah informasi profil").assertIsDisplayed()
        composeRule.onNodeWithTag("edit-profile-name").assertTextContains("Pemilik Toko")
        composeRule.onNodeWithTag("edit-profile-username").assertTextContains("owner.lama")
        composeRule.onNodeWithTag("edit-profile-email").assertTextContains("owner@gmail.com")
        composeRule.onNodeWithTag("edit-profile-readonly-id").assertDoesNotExist()
        composeRule.onNodeWithTag("edit-profile-readonly-role").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("edit-profile-save").assertIsNotEnabled()

        // Edit only username first -> save button enables and persists new username!
        composeRule.onNodeWithTag("edit-profile-username").performTextReplacement("owner.barokah")
        composeRule.onNodeWithTag("edit-profile-save").performScrollTo().assertIsEnabled().performClick()
        assertEquals(Triple("Pemilik Toko", "owner.barokah", "owner@gmail.com"), savedTuple)

        // Edit name and email too
        composeRule.onNodeWithTag("edit-profile-name").performTextReplacement("Haji Ahmad")
        composeRule.onNodeWithTag("edit-profile-email").performTextReplacement("ahmad@barokah.id")
        composeRule.onNodeWithTag("edit-profile-save").performScrollTo().assertIsEnabled().performClick()
        assertEquals(Triple("Haji Ahmad", "owner.barokah", "ahmad@barokah.id"), savedTuple)
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun invalidUsernameShowsValidationErrorAndDisablesSave() {
        composeRule.setContent {
            TbterminalappTheme {
                SharedEditProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = UserProfile(
                            id = "KSR-646",
                            username = "owner.lama",
                            name = "Pemilik Toko",
                            role = "OWNER",
                            email = "owner@gmail.com",
                            isActive = true,
                            joinedAt = "2026-01-01",
                            lastLoginAt = null
                        )
                    ),
                    onSave = { _, _, _ -> },
                    onBack = {},
                    onSaved = {},
                    onClearMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("edit-profile-username").performTextReplacement("ab")
        composeRule.onNodeWithText("Username 3–50 karakter (huruf, angka, ., _, - tanpa spasi)").assertIsDisplayed()
        composeRule.onNodeWithTag("edit-profile-save").assertIsNotEnabled()
    }
}
