package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class OwnerUserDataBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1064dp-mdpi")
    fun dataListRendersRowsStatusesAndRowClickNavigatesToDetail() {
        var detailUserId: String? = null
        val sample = sampleReferenceUsers()

        composeRule.setContent {
            TbterminalappTheme {
                OwnerUserRoute(
                    uiState = UserManagementUiState(
                        staffMembers = sample,
                        totalStaff = sample.size.toLong(),
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
                    onAddUserClick = {},
                    onUserDetailClick = { detailUserId = it },
                    onRefresh = {},
                    onBack = {},
                    onDashboardClick = {},
                    onDismissActionMessage = {}
                )
            }
        }

        // Stats & section header
        composeRule.onNodeWithTag("user-total-count").assertTextEquals("3")
        composeRule.onNodeWithText("2 orang").assertIsDisplayed()
        composeRule.onNodeWithText("1 orang").assertIsDisplayed()
        composeRule.onNodeWithText("Tambah pengguna").assertIsDisplayed()
        composeRule.onNodeWithText("Semua pengguna").assertIsDisplayed()
        composeRule.onNodeWithText("Urut: Nama A\u2013Z").assertIsDisplayed()

        // Row formatting matching Pelanggan & Supplier rows
        composeRule.onNodeWithText("Andi Pratama").assertIsDisplayed()
        composeRule.onNodeWithText("@andi.admin \u00B7 andi@tbterminal.id").assertIsDisplayed()
        composeRule.onNodeWithText("15 menit lalu").assertIsDisplayed()
        composeRule.onNodeWithText("Budi Kasir").assertIsDisplayed()
        composeRule.onNodeWithText("@budi.kasir").assertIsDisplayed()
        composeRule.onNodeWithText("Semua pengguna sudah ditampilkan").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Andi Pratama, aktif").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sari Wijaya, nonaktif").assertIsDisplayed()

        // Row touch target >= 72dp and tap routes to user detail
        val rowBounds = composeRule.onNodeWithTag("user-row-u1").getBoundsInRoot()
        assertTrue((rowBounds.bottom - rowBounds.top) >= 72.dp)

        composeRule.onNodeWithTag("user-row-u1").performClick()
        assertEquals("u1", detailUserId)

        composeRule.onNodeWithTag("user-row-u3").performClick()
        assertEquals("u3", detailUserId)

        // Filter & sort helper
        val sortedRecent = filterAndSortOwnerUsers(
            staffMembers = sample,
            searchQuery = "",
            statusFilter = UserStatusFilter.Active,
            roleFilter = UserRoleFilter.All,
            sortOrder = UserSortOrder.RECENT_LOGIN
        )
        assertEquals(listOf("Budi Kasir", "Andi Pratama"), sortedRecent.map { it.name })
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDataLayoutShowsNavRailAndLazyList() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerUserDataScreen(
                    summary = UserEmptySummaryUiState(totalUsers = 3L, activeCount = 2, inactiveCount = 1),
                    staffMembers = sampleReferenceUsers(),
                    sortLabel = UserSortOrder.NAME_ASC.label,
                    isSearchExpanded = false,
                    searchQuery = "",
                    isFiltered = false,
                    onBack = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchChanged = {},
                    onOpenFilter = {},
                    onAddUserClick = {},
                    onStaffClick = {},
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithTag("owner-user-lazy-list").assertIsDisplayed()
        composeRule.onNodeWithText("Andi Pratama").assertIsDisplayed()
    }
}
