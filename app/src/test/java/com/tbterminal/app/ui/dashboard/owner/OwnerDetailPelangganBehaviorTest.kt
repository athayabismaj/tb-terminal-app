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
import com.tbterminal.app.ui.customers.CustomerDetailUiState
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
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
class OwnerDetailPelangganBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun phoneDetailShowsCustomerInfoCopyPhoneEditAndDeactivateFlow() {
        OwnerCustomerToastBridge.consumePendingSuccess()

        var state by mutableStateOf(
            CustomerDetailUiState(
                customer = sampleDetailPelangganCustomer(isActive = true),
                isLoading = false
            )
        )
        var backCount = 0
        var editedId: String? = null
        var deactivateConfirmCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailPelangganRoute(
                    uiState = state,
                    canManage = true,
                    onBack = { backCount++ },
                    onRetry = {},
                    onEditCustomerClick = { editedId = it },
                    onDeactivateConfirm = {
                        deactivateConfirmCount++
                        state = state.copy(
                            customer = state.customer?.copy(isActive = false),
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
        composeRule.onNode(hasText("Detail pelanggan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("BS").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-name").assertIsDisplayed().assertTextEquals("Budi Santoso")
        composeRule.onNodeWithTag("detail-pelanggan-type-badge").assertIsDisplayed()
        composeRule.onNodeWithText("Kontraktor").assertIsDisplayed()

        // Touch targets
        val backBounds = composeRule.onNodeWithTag("detail-pelanggan-back-btn").getBoundsInRoot()
        assertTrue((backBounds.right - backBounds.left) >= 48.dp)
        assertTrue((backBounds.bottom - backBounds.top) >= 48.dp)

        val editBounds = composeRule.onNodeWithTag("detail-pelanggan-edit-btn").getBoundsInRoot()
        assertTrue((editBounds.bottom - editBounds.top) >= 48.dp)

        val copyBounds = composeRule.onNodeWithTag("detail-pelanggan-copy-phone-btn").getBoundsInRoot()
        assertTrue((copyBounds.right - copyBounds.left) >= 44.dp)
        assertTrue((copyBounds.bottom - copyBounds.top) >= 44.dp)

        // Info sections
        composeRule.onNodeWithText("Kontak & alamat").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-phone").assertTextEquals("0812-3456-7890")
        composeRule.onNodeWithTag("detail-pelanggan-address").assertIsDisplayed()
        composeRule.onNodeWithText("Ketentuan kredit").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-credit-limit").assertTextEquals("Rp 5.000.000")
        composeRule.onNodeWithTag("detail-pelanggan-term").assertTextEquals("14 hari")
        composeRule.onNodeWithTag("detail-pelanggan-status-badge").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-status-dot").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-status").assertTextEquals("Aktif")

        // Copy phone shows toast
        composeRule.onNodeWithTag("detail-pelanggan-copy-phone-btn").performClick()
        composeRule.onNodeWithTag("detail-pelanggan-copy-toast").assertIsDisplayed()

        // Edit button routes with customer ID
        composeRule.onNodeWithTag("detail-pelanggan-edit-btn").performClick()
        assertEquals("cust-01", editedId)

        // Deactivate flow: open dialog, cancel first, then confirm
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Nonaktifkan pelanggan?").assertIsDisplayed()

        composeRule.onNodeWithTag("detail-pelanggan-deactivate-cancel").performClick()
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-dialog").assertDoesNotExist()
        assertEquals(0, deactivateConfirmCount)

        composeRule.onNodeWithTag("detail-pelanggan-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-confirm").performClick()

        composeRule.waitForIdle()
        assertEquals(1, deactivateConfirmCount)
        assertEquals(1, backCount)
        assertEquals("Pelanggan dinonaktifkan", OwnerCustomerToastBridge.consumePendingSuccess())
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutShowsNavRailAndHidesDeactivateWhenInactiveOrReadOnly() {
        val inactiveCashCustomer = sampleDetailPelangganCustomer(isActive = false).copy(
            isContractor = false,
            phone = null,
            address = null,
            creditLimit = BigDecimal.ZERO,
            paymentTermDays = 0
        )

        var state by mutableStateOf(
            CustomerDetailUiState(customer = inactiveCashCustomer, isLoading = false)
        )
        var canManage by mutableStateOf(true)

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailPelangganRoute(
                    uiState = state,
                    canManage = canManage,
                    onBack = {},
                    onRetry = {},
                    onEditCustomerClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = {},
                    onDeactivatedReset = {},
                    onDashboardClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithText("Umum").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-phone").assertTextEquals("Belum diisi")
        composeRule.onNodeWithTag("detail-pelanggan-address").assertTextEquals("Belum diisi")
        composeRule.onNodeWithTag("detail-pelanggan-credit-limit").assertTextEquals("Rp 0 (tunai)")
        composeRule.onNodeWithTag("detail-pelanggan-term").assertTextEquals("0 hari (tunai)")
        composeRule.onNodeWithTag("detail-pelanggan-status-badge").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-status").assertTextEquals("Nonaktif")
        composeRule.onNodeWithTag("detail-pelanggan-copy-phone-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-inactive-banner").assertIsDisplayed()
        // Default feature flag CUSTOMER_REACTIVATE_ENABLED = true displays Aktifkan action when canManage = true
        composeRule.onNodeWithTag("detail-pelanggan-activate-btn").assertIsDisplayed()

        // Active customer without MANAGE_CUSTOMERS capability hides edit & deactivate
        state = CustomerDetailUiState(
            customer = sampleDetailPelangganCustomer(isActive = true),
            isLoading = false
        )
        canManage = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-pelanggan-edit-btn").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-btn").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun loadingErrorRetryAndDeactivateFailureToastWorkProperly() {
        var state by mutableStateOf(CustomerDetailUiState(isLoading = true))
        var retryCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailPelangganRoute(
                    uiState = state,
                    canManage = true,
                    onBack = {},
                    onRetry = { retryCount++ },
                    onEditCustomerClick = {},
                    onDeactivateConfirm = {
                        state = state.copy(actionErrorMessage = "Pelanggan masih memiliki piutang aktif.")
                    },
                    onClearActionError = { state = state.copy(actionErrorMessage = null) },
                    onDeactivatedReset = {},
                    onDashboardClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("detail-pelanggan-loading-skeleton").assertIsDisplayed()

        state = CustomerDetailUiState(
            isLoading = false,
            errorMessage = "Koneksi bermasalah. Detail pelanggan gagal dimuat."
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-pelanggan-error-block").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-retry-btn").performClick()
        assertEquals(1, retryCount)

        state = CustomerDetailUiState(
            customer = sampleDetailPelangganCustomer(isActive = true),
            isLoading = false
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-pelanggan-deactivate-btn").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-confirm").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-pelanggan-action-error-toast").assertIsDisplayed()
        composeRule.onNodeWithText("Pelanggan masih memiliki piutang aktif.").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun reactivateBannerOfflineDisabledAndOnlineStaysOnDetailWithUndoToast() {
        var state by mutableStateOf(
            CustomerDetailUiState(
                customer = sampleDetailPelangganCustomer(isActive = false),
                isLoading = false
            )
        )
        var isOnline by mutableStateOf(false)
        var backCount = 0
        var activateCount = 0
        var undoCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailPelangganRoute(
                    uiState = state,
                    canManage = true,
                    onBack = { backCount++ },
                    onRetry = {},
                    onEditCustomerClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = { state = state.copy(actionErrorMessage = null, isNotFoundError = false) },
                    onDeactivatedReset = { state = state.copy(isDeactivated = false) },
                    onDashboardClick = {},
                    onActivateClick = {
                        activateCount++
                        state = state.copy(
                            customer = state.customer?.copy(isActive = true),
                            isReactivated = true
                        )
                    },
                    onUndoActivateClick = {
                        undoCount++
                        state = state.copy(
                            customer = state.customer?.copy(isActive = false),
                            isDeactivated = false
                        )
                    },
                    onReactivatedReset = { state = state.copy(isReactivated = false) },
                    reactivateEnabled = true,
                    isOnlineOverride = isOnline
                )
            }
        }

        // Offline: banner tells user internet is needed and button is disabled
        composeRule.onNodeWithTag("detail-pelanggan-inactive-banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Butuh koneksi internet untuk mengaktifkan pelanggan.").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-activate-btn").assertIsNotEnabled()

        // Online: tap Aktifkan immediately activates without confirmation dialog & stays on Detail
        isOnline = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail-pelanggan-activate-btn").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals(1, activateCount)
        assertEquals(0, backCount)
        composeRule.onNodeWithTag("detail-pelanggan-inactive-banner").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-status-badge").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-pelanggan-status").assertTextEquals("Aktif")
        composeRule.onNodeWithTag("detail-pelanggan-deactivate-btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-reactivated-toast").assertIsDisplayed()
        composeRule.onNodeWithText("Pelanggan diaktifkan kembali").assertIsDisplayed()

        // Tap Batalkan on toast reverts customer back to Nonaktif while staying on Detail
        composeRule.onNodeWithTag("app-alert-action").performClick()
        composeRule.waitForIdle()

        assertEquals(1, undoCount)
        assertEquals(0, backCount)
        composeRule.onNodeWithTag("detail-pelanggan-status").assertTextEquals("Nonaktif")
        composeRule.onNodeWithTag("detail-pelanggan-inactive-banner").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h880dp-mdpi")
    fun reactivateNotFound404ShowsBackActionAndReadOnlyHidesActivate() {
        var state by mutableStateOf(
            CustomerDetailUiState(
                customer = sampleDetailPelangganCustomer(isActive = false),
                isLoading = false
            )
        )
        var canManage by mutableStateOf(false)
        var backCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailPelangganRoute(
                    uiState = state,
                    canManage = canManage,
                    onBack = { backCount++ },
                    onRetry = {},
                    onEditCustomerClick = {},
                    onDeactivateConfirm = {},
                    onClearActionError = { state = state.copy(actionErrorMessage = null, isNotFoundError = false) },
                    onDeactivatedReset = {},
                    onDashboardClick = {},
                    onActivateClick = {
                        state = state.copy(
                            actionErrorMessage = "Pelanggan sudah dihapus atau tidak ditemukan.",
                            isNotFoundError = true
                        )
                    },
                    reactivateEnabled = true,
                    isOnlineOverride = true
                )
            }
        }

        // Read-only (e.g. Cashier) cannot see Aktifkan even when reactivateEnabled = true
        composeRule.onNodeWithTag("detail-pelanggan-inactive-banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail-pelanggan-activate-btn").assertDoesNotExist()

        // Switch to manager and simulate 404 from server
        canManage = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail-pelanggan-activate-btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-pelanggan-action-error-toast").assertIsDisplayed()
        composeRule.onNodeWithText("Pelanggan tidak ditemukan").assertIsDisplayed()
        composeRule.onNodeWithTag("app-alert-action").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }
}
