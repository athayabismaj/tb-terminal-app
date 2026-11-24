package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.users.UserManagementUiState
import com.tbterminal.app.ui.users.UserRoleFilter
import com.tbterminal.app.ui.users.UserSortOrder
import com.tbterminal.app.ui.users.UserStatusFilter
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
class OwnerUserEmptyBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneEmptyLayoutMatchesPelangganEmptyDesignAndTriggersAddUser() {
        var addCount = 0
        var backCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerUserRoute(
                    uiState = UserManagementUiState(
                        staffMembers = emptyList(),
                        totalStaff = 0L,
                        isLoading = false
                    ),
                    isSearchExpanded = false,
                    searchQuery = "",
                    statusFilter = UserStatusFilter.All,
                    roleFilter = UserRoleFilter.All,
                    sortOrder = UserSortOrder.NAME_ASC,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onAddUserClick = { addCount++ },
                    onUserDetailClick = {},
                    onRefresh = {},
                    onBack = { backCount++ },
                    onDashboardClick = {},
                    onDismissActionMessage = {}
                )
            }
        }

        // Secondary page hides bottom nav
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithText("Pengguna & akses").assertIsDisplayed()
        composeRule.onNodeWithText("Total pengguna").assertIsDisplayed()
        composeRule.onNodeWithTag("user-total-count").assertTextEquals("0")
        composeRule.onNodeWithText("Belum ada pengguna").assertIsDisplayed()

        val ctaBounds = composeRule.onNodeWithTag("add-user").getBoundsInRoot()
        assertTrue((ctaBounds.bottom - ctaBounds.top) >= 48.dp)

        composeRule.onNodeWithTag("add-user").performClick()
        assertEquals(1, addCount)

        composeRule.onNodeWithTag("user-back-btn").performClick()
        assertEquals(1, backCount)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletEmptyShowsNavRailAndSearchNotFoundAndErrorStates() {
        var retryCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerUserRoute(
                    uiState = UserManagementUiState(
                        staffMembers = emptyList(),
                        totalStaff = 0L,
                        isLoading = false,
                        errorMessage = "Koneksi ke server bermasalah."
                    ),
                    isSearchExpanded = false,
                    searchQuery = "",
                    statusFilter = UserStatusFilter.All,
                    roleFilter = UserRoleFilter.All,
                    sortOrder = UserSortOrder.NAME_ASC,
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onAddUserClick = {},
                    onUserDetailClick = {},
                    onRefresh = { retryCount++ },
                    onBack = {},
                    onDashboardClick = {},
                    onDismissActionMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithTag("user-error-block").assertIsDisplayed()
        composeRule.onNodeWithTag("user-retry-btn").performClick()
        assertEquals(1, retryCount)

        val searchSpec = userEmptyStateSpecFor(searchQuery = "agus", isFiltered = false)
        assertEquals("\"agus\" tidak ditemukan", searchSpec.title)

        val filterSpec = userEmptyStateSpecFor(searchQuery = "", isFiltered = true)
        assertEquals("Pengguna tidak ditemukan", filterSpec.title)
    }
}
