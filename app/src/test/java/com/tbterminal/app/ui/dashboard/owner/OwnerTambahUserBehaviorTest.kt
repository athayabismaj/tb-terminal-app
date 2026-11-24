package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.users.AddUserUiState
import com.tbterminal.app.ui.users.EditUserUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class OwnerTambahUserBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roles = listOf(
        UserRole(id = "r-admin", name = "ADMIN"),
        UserRole(id = "r-kasir", name = "KASIR")
    )

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun addUserValidatesFieldsShowsDiscardDialogAndSubmits() {
        var state by mutableStateOf(
            AddUserUiState(
                roles = roles,
                selectedRoleId = "r-kasir",
                isLoadingRoles = false
            )
        )
        var saveCount = 0
        var backCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahUserRoute(
                    uiState = state,
                    onFullNameChanged = { state = state.copy(fullName = it, errorMessage = null) },
                    onUsernameChanged = { state = state.copy(username = it, errorMessage = null) },
                    onEmailChanged = { state = state.copy(email = it, errorMessage = null) },
                    onPasswordChanged = { state = state.copy(password = it, errorMessage = null) },
                    onPinChanged = { state = state.copy(pin = it, errorMessage = null) },
                    onRoleSelected = { state = state.copy(selectedRoleId = it, errorMessage = null) },
                    onSave = { saveCount++ },
                    onRetryRoles = {},
                    onBack = { backCount++ },
                    onDashboardClick = {}
                )
            }
        }

        // Only Admin & Kasir radio cards exist, no Owner
        composeRule.onNodeWithTag("user-role-admin").assertIsDisplayed()
        composeRule.onNodeWithTag("user-role-kasir").assertIsDisplayed()
        composeRule.onNodeWithTag("user-role-owner").assertDoesNotExist()
        composeRule.onNodeWithTag("tambah-user-save").assertIsNotEnabled()

        // Fill fields
        composeRule.onNodeWithTag("tambah-user-fullname").performTextInput("Budi Santoso")
        composeRule.onNodeWithTag("tambah-user-username").performTextInput("budi.kasir")
        composeRule.onNodeWithTag("tambah-user-password").performScrollTo().performTextInput("secret123")
        composeRule.onNodeWithTag("tambah-user-pin").performScrollTo().performTextInput("123456")

        // Back prompts discard when dirty
        composeRule.onNodeWithTag("tambah-user-back-btn").performClick()
        composeRule.onNodeWithTag("discard-user-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("discard-user-continue").performClick()
        assertEquals(0, backCount)

        // Select Admin and submit
        composeRule.onNodeWithTag("user-role-admin").performScrollTo().performClick()
        composeRule.onNodeWithTag("tambah-user-save")
            .assertIsEnabled()
            .performClick()

        assertEquals(1, saveCount)
        assertEquals("r-admin", state.selectedRoleId)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun editUserTabletShowsNavRailHidesPasswordPinAndDetectsDirtyState() {
        var state by mutableStateOf(
            EditUserUiState(
                userId = "u1",
                fullName = "Andi Pratama",
                email = "andi@toko.id",
                username = "andi.admin",
                selectedRoleId = "r-admin",
                isActive = true,
                roles = roles,
                isLoadingUser = false,
                isLoadingRoles = false
            )
        )
        var saveCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerEditUserRoute(
                    uiState = state,
                    onFullNameChanged = { state = state.copy(fullName = it) },
                    onUsernameChanged = { state = state.copy(username = it) },
                    onEmailChanged = { state = state.copy(email = it) },
                    onRoleSelected = { state = state.copy(selectedRoleId = it) },
                    onSave = { saveCount++ },
                    onRetryLoad = {},
                    onBack = {},
                    onDashboardClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan perubahan").assertIsDisplayed()
        composeRule.onNodeWithTag("tambah-user-password").assertDoesNotExist()
        composeRule.onNodeWithTag("tambah-user-pin").assertDoesNotExist()
        composeRule.onNodeWithTag("tambah-user-save").assertIsNotEnabled()

        composeRule.onNodeWithTag("tambah-user-fullname").performTextInput(" Jr")
        composeRule.onNodeWithTag("tambah-user-save")
            .assertIsEnabled()
            .performClick()

        assertEquals(1, saveCount)
    }

    @Test
    fun ownerTambahUserValidatorsEnforce8To72PasswordAnd4To6NumericPin() {
        assertNotNull(validateOwnerUserPassword("1234567"))
        assertNotNull(validateOwnerUserPassword("x".repeat(73)))
        assertNull(validateOwnerUserPassword("12345678"))
        assertNull(validateOwnerUserPassword("x".repeat(72)))

        assertNotNull(validateOwnerUserPin("123"))
        assertNotNull(validateOwnerUserPin("1234567"))
        assertNotNull(validateOwnerUserPin("12ab"))
        assertNull(validateOwnerUserPin("1234"))
        assertNull(validateOwnerUserPin("12345"))
        assertNull(validateOwnerUserPin("123456"))
    }
}
