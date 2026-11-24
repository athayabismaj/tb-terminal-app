package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.suppliers.SupplierUiState
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
class OwnerTambahSupplierBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneFormMatchesTambahPelangganLayoutValidationAndDiscardDialog() {
        var state by mutableStateOf(SupplierUiState(isLoading = false))
        var saveCount = 0
        var cancelCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSupplierRoute(
                    uiState = state,
                    onNameChanged = { state = state.copy(nameInput = it, message = null) },
                    onPhoneChanged = { state = state.copy(phoneInput = it, message = null) },
                    onAddressChanged = { state = state.copy(addressInput = it, message = null) },
                    onPaymentTermChanged = { state = state.copy(paymentTermInput = it, message = null) },
                    onSave = { saveCount++ },
                    onSavedReset = { state = state.copy(isSaved = false) },
                    onCancel = { cancelCount++ },
                    onDashboardClick = {},
                    onDismissMessage = { state = state.copy(message = null) }
                )
            }
        }

        // Secondary header & form headings
        composeRule.onNode(hasText("Tambah supplier") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Data supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Pembayaran").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan supplier").assertIsDisplayed()

        // Touch target check
        val backBounds = composeRule.onNodeWithTag("tambah-supplier-back-btn").getBoundsInRoot()
        assertTrue((backBounds.right - backBounds.left) >= 48.dp)
        assertTrue((backBounds.bottom - backBounds.top) >= 48.dp)
        val saveBounds = composeRule.onNodeWithTag("tambah-supplier-save").getBoundsInRoot()
        assertTrue((saveBounds.bottom - saveBounds.top) >= 56.dp)

        // Blank name keeps submit disabled
        composeRule.onNodeWithTag("tambah-supplier-save").performClick()
        assertEquals(0, saveCount)

        // Clicking back with no changes returns immediately to Supplier without discard dialog
        composeRule.onNodeWithTag("tambah-supplier-back-btn").performClick()
        assertEquals(1, cancelCount)
        composeRule.onNodeWithTag("discard-supplier-dialog").assertDoesNotExist()

        // Filling valid name enables submit
        composeRule.onNodeWithTag("tambah-supplier-name").performTextReplacement("PT Sumber Makmur")
        composeRule.onNodeWithTag("tambah-supplier-save").performClick()
        assertEquals(1, saveCount)

        // Leaving with unsaved input triggers discard dialog
        composeRule.onNodeWithTag("tambah-supplier-back-btn").performClick()
        composeRule.onNodeWithTag("discard-supplier-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Buang data supplier?").assertIsDisplayed()

        composeRule.onNodeWithTag("discard-supplier-confirm").performClick()
        assertEquals(2, cancelCount)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletFormRendersNavRailGreenHeroAndSideBySideFields() {
        var selectedNav: DashboardNavDestination? = null

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSupplierScreen(
                    name = "",
                    phone = "",
                    paymentTermDigits = "30",
                    address = "",
                    onNameChange = {},
                    onPhoneChange = {},
                    onPaymentTermChange = {},
                    onAddressChange = {},
                    onBack = {},
                    onSubmit = {},
                    onNavSelect = { selectedNav = it }
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithText("Supplier baru").assertIsDisplayed()
        composeRule.onNodeWithText("Data supplier").assertIsDisplayed()

        // Name and phone side by side
        val nameBounds = composeRule.onNodeWithTag("tambah-supplier-name").getBoundsInRoot()
        val phoneBounds = composeRule.onNodeWithTag("tambah-supplier-phone").getBoundsInRoot()
        assertTrue(phoneBounds.left > nameBounds.right)

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(DashboardNavDestination.BERANDA, selectedNav)
    }
}
