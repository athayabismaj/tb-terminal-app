package com.tbterminal.app.ui.users

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class UserFilterSheetBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun filterPenggunaPopupShowsSemuaAktifNonaktifAndFiltersUsers() {
        var statusFilter by mutableStateOf(UserStatusFilter.All)
        var roleFilter by mutableStateOf(UserRoleFilter.All)
        var showFilterPopup by mutableStateOf(true)

        val staff = listOf(
            StaffMember(
                id = "u-1",
                name = "Budi Aktif",
                email = "budi@tbterminal.id",
                rawEmail = "budi@tbterminal.id",
                username = "budi",
                roleId = "r-kasir",
                role = StaffRole.Kasir,
                status = StaffStatus.Active,
                lastLogin = "Hari ini",
                initials = "BA"
            ),
            StaffMember(
                id = "u-2",
                name = "Sari Nonaktif",
                email = "sari@tbterminal.id",
                rawEmail = "sari@tbterminal.id",
                username = "sari",
                roleId = "r-admin",
                role = StaffRole.Admin,
                status = StaffStatus.Inactive,
                lastLogin = "Kemarin",
                initials = "SN"
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                UserManagementContent(
                    uiState = UserManagementUiState(
                        staffMembers = staff,
                        totalStaff = staff.size.toLong()
                    ),
                    statusFilter = statusFilter,
                    onStatusFilterChanged = { statusFilter = it },
                    roleFilter = roleFilter,
                    onRoleFilterChanged = { roleFilter = it },
                    showFilterPopup = showFilterPopup,
                    onDismissFilter = { showFilterPopup = false }
                )
            }
        }

        composeRule.onNodeWithTag("user-filter-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("user-role-filter-Owner").assertDoesNotExist()
        composeRule.onNodeWithTag("user-role-filter-All").assertIsDisplayed().assertTextEquals("Semua")
        composeRule.onNodeWithTag("user-role-filter-Admin").assertIsDisplayed().assertTextEquals("Admin")
        composeRule.onNodeWithTag("user-role-filter-Kasir").assertIsDisplayed().assertTextEquals("Kasir")
        composeRule.onNodeWithTag("user-status-option-All").assertIsDisplayed().assertTextEquals("Semua")
        composeRule.onNodeWithTag("user-status-option-Active").assertIsDisplayed().assertTextEquals("Aktif")
        composeRule.onNodeWithTag("user-status-option-Inactive").assertIsDisplayed().assertTextEquals("Nonaktif")

        // Filter ke Nonaktif
        composeRule.onNodeWithTag("user-status-option-Inactive").performClick()
        composeRule.onNodeWithTag("user-filter-done").performClick()

        assertEquals(UserStatusFilter.Inactive, statusFilter)
        composeRule.onNodeWithText("Sari Nonaktif").assertIsDisplayed()
        composeRule.onNodeWithText("Budi Aktif").assertDoesNotExist()
    }
}
