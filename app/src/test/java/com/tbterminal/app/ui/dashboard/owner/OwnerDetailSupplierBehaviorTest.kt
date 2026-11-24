package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.suppliers.SupplierDetailUiState
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
class OwnerDetailSupplierBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun phoneDetailShowsSupplierInfoCopyPhoneEditAndDeactivateFlow() {
        OwnerSupplierToastBridge.consumePendingSuccess()

        var state by mutableStateOf(
            SupplierDetailUiState(
                supplier = sampleDetailSupplier(isActive = true),
                isLoading = false
            )
        )
        var backCount = 0
        var editedId: String? = null
        var deactivateConfirmCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailSupplierRoute(
                    uiState = state,
                    canManage = true,
                    onBack = { backCount++ },
                    onRetry = {},
                    onEditSupplierClick = { editedId = it },
                    onDeactivateConfirm = {
                        deactivateConfirmCount++
                        state = state.copy(
                            supplier = state.supplier?.copy(isActive = false),
                            isDeactivated = true
                        )
                    },
                    onClearActionError = { state = state.copy(actionErrorMessage = null) },
                    onDeactivatedReset = { state = state.copy(isDeactivated = false) },
                    onDashboardClick = {}
                )
            }
        }

        // Secondary page: bottom nav is hidden
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithTag("nav-MENU").assertDoesNotExist()

        // Header & identity
        composeRule.onNode(hasText("Detail supplier") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("PS").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-supplier-name").assertIsDisplayed().assertTextEquals("PT Semen Merdeka")
        composeRule.onNodeWithTag("detail-supplier-type-badge").assertIsDisplayed()
        composeRule.onNodeWithText("Supplier").assertIsDisplayed()

        // Touch targets
        val backBounds = composeRule.onNodeWithTag("detail-supplier-back-btn").getBoundsInRoot()
        assertTrue((backBounds.right - backBounds.left) >= 48.dp)
        assertTrue((backBounds.bottom - backBounds.top) >= 48.dp)

        val editBounds = composeRule.onNodeWithTag("detail-supplier-edit-btn").getBoundsInRoot()
        assertTrue((editBounds.bottom - editBounds.top) >= 48.dp)

        val copyBounds = composeRule.onNodeWithTag("detail-supplier-copy-phone-btn").getBoundsInRoot()
        assertTrue((copyBounds.right - copyBounds.left) >= 44.dp)
        assertTrue((copyBounds.bottom - copyBounds.top) >= 44.dp)

        // Info sections
        composeRule.onNodeWithText("Kontak & alamat").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-supplier-phone").assertTextEquals("0812-3456-7890")
        composeRule.onNodeWithTag("detail-supplier-address").assertIsDisplayed()
        composeRule.onNodeWithText("Ketentuan pembayaran").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-supplier-term").assertTextEquals("30 hari")
        composeRule.onNodeWithTag("detail-supplier-status-badge").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-supplier-status-dot").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-supplier-status").assertTextEquals("Aktif")

        // Copy phone shows toast
        composeRule.onNodeWithTag("detail-supplier-copy-phone-btn").performClick()
        composeRule.onNodeWithTag("detail-supplier-copy-toast").assertIsDisplayed()

        // Edit button routes with supplier ID
        composeRule.onNodeWithTag("detail-supplier-edit-btn").performClick()
        assertEquals("sup-01", editedId)

        // Deactivate flow: open dialog, cancel first, then confirm
        composeRule.onNodeWithTag("detail-supplier-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-supplier-deactivate-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Nonaktifkan supplier?").assertIsDisplayed()

        composeRule.onNodeWithTag("detail-supplier-deactivate-cancel").performClick()
        composeRule.onNodeWithTag("detail-supplier-deactivate-dialog").assertDoesNotExist()
        assertEquals(0, deactivateConfirmCount)

        composeRule.onNodeWithTag("detail-supplier-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-supplier-deactivate-confirm").performClick()

        composeRule.waitForIdle()
        assertEquals(1, deactivateConfirmCount)
        assertEquals(1, backCount)
        assertEquals("Supplier dinonaktifkan", OwnerSupplierToastBridge.consumePendingSuccess())
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutShowsNavRailAndReactivatesInactiveSupplierWithUndo() {
        OwnerSupplierToastBridge.consumePendingSuccess()

        var state by mutableStateOf(
            SupplierDetailUiState(
                supplier = sampleDetailSupplier(isActive = false).copy(
                    phone = null,
                    address = null,
                    paymentTermDays = 0
                ),
                isLoading = false
            )
        )
        var activateCount = 0
        var undoCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailSupplierRoute(
                    uiState = state,
                    canManage = true,
                    onBack = {},
                    onRetry = {},
                    onEditSupplierClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = {},
                    onDeactivatedReset = {},
                    onDashboardClick = {},
                    onActivateClick = {
                        activateCount++
                        state = state.copy(
                            supplier = state.supplier?.copy(isActive = true),
                            isReactivated = true
                        )
                    },
                    onUndoActivateClick = {
                        undoCount++
                        state = state.copy(
                            supplier = state.supplier?.copy(isActive = false),
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

        composeRule.onNodeWithTag("detail-supplier-phone").assertTextEquals("Belum diisi")
        composeRule.onNodeWithTag("detail-supplier-address").assertTextEquals("Belum diisi")
        composeRule.onNodeWithTag("detail-supplier-term").assertTextEquals("0 hari (tunai)")
        composeRule.onNodeWithTag("detail-supplier-status-badge").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-supplier-status").assertTextEquals("Nonaktif")

        composeRule.onNodeWithTag("detail-supplier-deactivate-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-supplier-inactive-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-supplier-activate-btn")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeRule.waitForIdle()
        assertEquals(1, activateCount)
        composeRule.onNodeWithTag("detail-supplier-status").assertTextEquals("Aktif")
        composeRule.onNodeWithTag("detail-supplier-reactivated-toast").assertIsDisplayed()

        composeRule.onNodeWithText("Batalkan").performClick()
        composeRule.waitForIdle()
        assertEquals(1, undoCount)
        composeRule.onNodeWithTag("detail-supplier-status").assertTextEquals("Nonaktif")
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun offlineDisablesActivateAndReadOnlyHidesMutationActions() {
        var canManage by mutableStateOf(true)
        var online by mutableStateOf(false)

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailSupplierRoute(
                    uiState = SupplierDetailUiState(
                        supplier = sampleDetailSupplier(isActive = false),
                        isLoading = false
                    ),
                    canManage = canManage,
                    onBack = {},
                    onRetry = {},
                    onEditSupplierClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = {},
                    onDeactivatedReset = {},
                    onDashboardClick = {},
                    isOnlineOverride = online
                )
            }
        }

        composeRule.onNodeWithTag("detail-supplier-activate-btn").assertIsNotEnabled()
        composeRule.onNodeWithText("Butuh koneksi internet untuk mengaktifkan supplier.").assertIsDisplayed()

        canManage = false
        online = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-supplier-edit-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-supplier-activate-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-supplier-deactivate-btn").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun loadingSkeletonAndErrorRetryStatesWorkCorrectly() {
        var state by mutableStateOf(SupplierDetailUiState(supplier = null, isLoading = true))
        var retryCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailSupplierRoute(
                    uiState = state,
                    canManage = true,
                    onBack = {},
                    onRetry = { retryCount++ },
                    onEditSupplierClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = {},
                    onDeactivatedReset = {},
                    onDashboardClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("detail-supplier-loading-skeleton").assertIsDisplayed()

        state = SupplierDetailUiState(
            supplier = null,
            isLoading = false,
            errorMessage = "Server sedang sibuk"
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-supplier-error-block").assertIsDisplayed()
        composeRule.onNodeWithText("Server sedang sibuk").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-supplier-retry-btn").performClick()
        assertEquals(1, retryCount)
    }
}
