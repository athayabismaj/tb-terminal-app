package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.customers.CustomerFormInput
import com.tbterminal.app.ui.customers.CustomerFormUiState
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class OwnerTambahPelangganBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun phoneFormValidationSaveContractorThousandSeparatorAndDiscardDialog() {
        var latestInput = CustomerFormInput()
        var saveCount = 0
        var backCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahPelangganRoute(
                    uiState = CustomerFormUiState(input = latestInput),
                    onInputChanged = { latestInput = it },
                    onSave = { saveCount++ },
                    onSavedReset = {},
                    onBack = { backCount++ },
                    onDashboardClick = {}
                )
            }
        }

        // Secondary page: no bottom nav, no old uppercase labels / Switch / Cancel button
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithText("NAMA PELANGGAN").assertDoesNotExist()
        composeRule.onNodeWithText("Pelanggan Kontraktor").assertDoesNotExist()
        composeRule.onNodeWithText("Batal").assertDoesNotExist()

        composeRule.onNode(hasText("Tambah pelanggan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Jenis pelanggan").assertIsDisplayed()
        composeRule.onNodeWithText("Kredit").assertIsDisplayed()
        composeRule.onNodeWithText("Biarkan 0 jika pelanggan selalu bayar tunai.").assertIsDisplayed()

        // Touch target sizing
        val backBounds = composeRule.onNodeWithTag("tambah-pelanggan-back-btn").getBoundsInRoot()
        assertTrue((backBounds.right - backBounds.left) >= 48.dp)
        assertTrue((backBounds.bottom - backBounds.top) >= 48.dp)

        listOf("customer-type-umum", "customer-type-kontraktor").forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getBoundsInRoot()
            assertTrue((bounds.bottom - bounds.top) >= 76.dp)
        }
        val saveBounds = composeRule.onNodeWithTag("tambah-pelanggan-save").getBoundsInRoot()
        assertTrue((saveBounds.bottom - saveBounds.top) >= 56.dp)

        // Back immediately when untouched
        composeRule.onNodeWithTag("tambah-pelanggan-back-btn").performClick()
        assertEquals(1, backCount)

        // Save button disabled until Name >= 2 chars trimmed
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsNotEnabled()
        composeRule.onNodeWithTag("tambah-pelanggan-name").performTextReplacement(" A ")
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsNotEnabled()

        // Invalid phone should NOT show error while typing
        composeRule.onNodeWithTag("tambah-pelanggan-phone").performTextReplacement("07123")
        composeRule.onNodeWithText("Nomor HP tidak valid.").assertDoesNotExist()

        // Moving focus to credit limit blurs phone -> error appears
        composeRule.onNodeWithTag("tambah-pelanggan-credit-limit").performClick()
        composeRule.onNodeWithText("Nomor HP tidak valid.").assertIsDisplayed()

        // Fix phone & enter 1.500.000 credit limit & select Kontraktor
        composeRule.onNodeWithTag("tambah-pelanggan-phone").performTextReplacement("081234567890")
        composeRule.onNodeWithText("Nomor HP tidak valid.").assertDoesNotExist()
        composeRule.onNodeWithTag("tambah-pelanggan-credit-limit").performTextReplacement("1500000")
        composeRule.onNodeWithTag("tambah-pelanggan-credit-limit").assertTextContains("1.500.000")
        composeRule.onNodeWithTag("tambah-pelanggan-payment-term").performTextReplacement("14")
        composeRule.onNodeWithTag("customer-type-kontraktor").performClick()

        // Back with dirty form -> discard dialog
        composeRule.onNodeWithTag("tambah-pelanggan-back-btn").performClick()
        composeRule.onNodeWithTag("discard-customer-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Buang data pelanggan?").assertIsDisplayed()
        composeRule.onNodeWithText("Data yang sudah diisi akan hilang.").assertIsDisplayed()
        composeRule.onNodeWithTag("discard-customer-continue").performClick()
        composeRule.onNodeWithTag("discard-customer-dialog").assertDoesNotExist()

        // Complete valid name & save
        composeRule.onNodeWithTag("tambah-pelanggan-name").performTextReplacement("CV Perkasa Mulia")
        composeRule.onNodeWithTag("tambah-pelanggan-address").performScrollTo().performTextInput("Jl. Merdeka 10")
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsEnabled().performClick()

        assertEquals(1, saveCount)
        assertEquals("CV Perkasa Mulia", latestInput.name)
        assertEquals("081234567890", latestInput.phone)
        assertTrue(latestInput.isContractor)
        assertEquals("1500000", latestInput.creditLimit)
        assertEquals("14", latestInput.paymentTermDays)
        assertEquals("Jl. Merdeka 10", latestInput.address)
    }

    @Test
    @Config(qualifiers = "w390dp-h520dp-mdpi")
    fun saveButtonRemainsVisibleInCompactKeyboardViewport() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahPelangganPhonePreview()
            }
        }
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsDisplayed()
        val saveBounds = composeRule.onNodeWithTag("tambah-pelanggan-save").getBoundsInRoot()
        assertTrue(saveBounds.bottom <= 520.dp + 1.dp)
    }

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun serverErrorsAndSavingStateRenderCorrectly() {
        var uiState by mutableStateOf(CustomerFormUiState())
        var saveCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahPelangganRoute(
                    uiState = uiState,
                    onInputChanged = { uiState = uiState.copy(input = it, errorMessage = null) },
                    onSave = { saveCount++ },
                    onSavedReset = {},
                    onBack = {},
                    onDashboardClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("tambah-pelanggan-name").performTextReplacement("Toko Jaya")
        composeRule.onNodeWithTag("tambah-pelanggan-phone").performTextReplacement("081234567890")

        // Saving state disables button and shows loading indicator
        uiState = uiState.copy(isSaving = true)
        composeRule.onNodeWithTag("tambah-pelanggan-loading").assertIsDisplayed()
        composeRule.onNodeWithTag("tambah-pelanggan-save").performClick()
        assertEquals(0, saveCount)

        // Duplicate phone error appears inline under phone field
        uiState = uiState.copy(isSaving = false, errorMessage = "Nomor HP sudah terdaftar")
        composeRule.onNodeWithText("Nomor HP sudah terdaftar").assertIsDisplayed()

        // Network error shows retry toast
        uiState = uiState.copy(errorMessage = "Koneksi bermasalah. Pelanggan gagal disimpan.")
        composeRule.onNodeWithText("Tidak dapat terhubung").assertIsDisplayed()
        composeRule.onNodeWithText("Coba lagi").performClick()
        assertEquals(1, saveCount)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutKeepsNavRailAndShowsHeroHighlights() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahPelangganTabletPreview()
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-MENU").assertIsDisplayed()
        composeRule.onNodeWithText("Pelanggan baru").assertIsDisplayed()
        composeRule.onNodeWithText("Hanya nama yang wajib diisi").assertIsDisplayed()
        composeRule.onNodeWithText("Limit dan termin bisa diubah nanti").assertIsDisplayed()
        composeRule.onNodeWithText("Kontraktor otomatis dapat harga khusus").assertIsDisplayed()
        composeRule.onNodeWithText("Data pelanggan").assertIsDisplayed()
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h920dp-mdpi")
    fun editModeLoadsExistingCustomerTracksChangesShowsDiscardAndSubmitsUpdates() {
        var uiState by mutableStateOf(CustomerFormUiState(isEditMode = true, isLoading = true))
        var saveCount = 0
        var backCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahPelangganRoute(
                    uiState = uiState,
                    onInputChanged = { uiState = uiState.copy(input = it, errorMessage = null) },
                    onSave = { saveCount++ },
                    onSavedReset = {},
                    onBack = { backCount++ },
                    onDashboardClick = {}
                )
            }
        }

        // Loading skeleton while fetching existing customer
        composeRule.onNode(hasText("Edit pelanggan") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithTag("customer-form-skeleton").assertIsDisplayed()
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsNotEnabled()

        // Loaded customer populates fields and keeps Simpan perubahan disabled until modified
        uiState = uiState.copy(
            isLoading = false,
            input = CustomerFormInput(
                name = "Budi Santoso",
                phone = "081234567890",
                address = "Jl. Sudirman No. 45",
                isContractor = true,
                creditLimit = "5000000",
                paymentTermDays = "14"
            )
        )

        composeRule.onNodeWithTag("customer-form-skeleton").assertDoesNotExist()
        composeRule.onNodeWithTag("tambah-pelanggan-name").assertTextContains("Budi Santoso")
        composeRule.onNodeWithTag("tambah-pelanggan-credit-limit").assertTextContains("5.000.000")
        composeRule.onNodeWithText("Simpan perubahan").assertIsDisplayed()
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsNotEnabled()

        // Back immediately when untouched
        composeRule.onNodeWithTag("tambah-pelanggan-back-btn").performClick()
        assertEquals(1, backCount)

        // Modify credit limit -> Save enabled, Back asks confirmation ("Buang perubahan?")
        composeRule.onNodeWithTag("tambah-pelanggan-credit-limit").performTextReplacement("7500000")
        composeRule.onNodeWithTag("tambah-pelanggan-credit-limit").assertTextContains("7.500.000")
        composeRule.onNodeWithTag("tambah-pelanggan-save").assertIsEnabled()

        composeRule.onNodeWithTag("tambah-pelanggan-back-btn").performClick()
        composeRule.onNodeWithText("Buang perubahan?").assertIsDisplayed()
        composeRule.onNodeWithText("Perubahan yang belum disimpan akan hilang.").assertIsDisplayed()
        composeRule.onNodeWithTag("discard-customer-continue").performClick()

        // Submit updated customer
        composeRule.onNodeWithTag("tambah-pelanggan-save").performClick()
        assertEquals(1, saveCount)
        assertEquals("7500000", uiState.input.creditLimit)
        assertEquals("Budi Santoso", uiState.input.name)
    }

    @Test
    fun phoneAndThousandsFormattersValidateExpectedRules() {
        assertNull(validateOwnerCustomerPhone(""))
        assertNull(validateOwnerCustomerPhone("081234567890"))
        assertNull(validateOwnerCustomerPhone("628123456789"))
        assertEquals("Nomor HP tidak valid.", validateOwnerCustomerPhone("0712345678"))
        assertEquals("Nomor HP tidak valid.", validateOwnerCustomerPhone("08123"))
        assertEquals("Nomor HP tidak valid.", validateOwnerCustomerPhone("08123456789012"))
        assertEquals("Nomor HP tidak valid.", validateOwnerCustomerPhone("0812abc67890"))
        assertEquals("1.500.000", formatThousandsDots("1500000"))
        assertFalse(validateOwnerCustomerName("CV").orEmpty().isNotEmpty())
    }
}
