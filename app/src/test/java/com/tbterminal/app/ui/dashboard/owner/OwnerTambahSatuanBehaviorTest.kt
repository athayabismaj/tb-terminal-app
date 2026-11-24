package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.products.ProductUnitUiState
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
class OwnerTambahSatuanBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun addUnitPhoneValidatesAndSubmits() {
        var saveCalls = 0
        var uiState by mutableStateOf(
            ProductUnitUiState(
                isFormVisible = true,
                isLoading = false
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSatuanRoute(
                    uiState = uiState,
                    onNameChanged = { uiState = uiState.copy(nameInput = it, message = null) },
                    onSymbolChanged = { uiState = uiState.copy(symbolInput = it, message = null) },
                    onSave = { saveCalls++ },
                    onDelete = {},
                    onCancel = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {}
                )
            }
        }

        composeRule.onNodeWithText("Tambah satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Data satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan satuan").assertIsDisplayed()
        composeRule.onAllNodesWithTag("unit-delete-btn").assertCountEquals(0)

        // Submit disabled while empty
        composeRule.onNodeWithTag("unit-save").assertIsNotEnabled()

        // Fill name & symbol, verify live badge preview and submit
        composeRule.onNodeWithTag("unit-name").performTextInput("Kilogram")
        composeRule.onNodeWithTag("unit-symbol").performTextInput("kg")
        composeRule.onNodeWithContentDescription("Singkatan kg").assertIsDisplayed()

        composeRule.onNodeWithTag("unit-save").performClick()
        assertEquals(1, saveCalls)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun addUnitBackWithUnsavedInputPromptsDiscardDialog() {
        var cancelCalls = 0
        var uiState by mutableStateOf(
            ProductUnitUiState(
                nameInput = "Sak",
                isFormVisible = true,
                isLoading = false
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSatuanRoute(
                    uiState = uiState,
                    onNameChanged = { uiState = uiState.copy(nameInput = it) },
                    onSymbolChanged = { uiState = uiState.copy(symbolInput = it) },
                    onSave = {},
                    onDelete = {},
                    onCancel = { cancelCalls++ },
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("tambah-satuan-back-btn").performClick()
        composeRule.onNodeWithTag("discard-satuan-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Buang data satuan?").assertIsDisplayed()

        composeRule.onNodeWithTag("discard-satuan-continue").performClick()
        composeRule.onAllNodesWithTag("discard-satuan-dialog").assertCountEquals(0)
        assertEquals(0, cancelCalls)

        composeRule.onNodeWithTag("tambah-satuan-back-btn").performClick()
        composeRule.onNodeWithTag("discard-satuan-confirm").performClick()
        assertEquals(1, cancelCalls)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun editUnitPhoneTracksChangesAndSupportsDeleteConfirmation() {
        val unit = ProductUnit("u1", "Kilogram", "kg", "2026-11-18T10:00:00Z")
        var saveCalls = 0
        var deletedUnit: ProductUnit? = null
        var uiState by mutableStateOf(
            ProductUnitUiState(
                editingUnit = unit,
                nameInput = unit.name,
                symbolInput = unit.symbol,
                isFormVisible = true,
                isLoading = false
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSatuanRoute(
                    uiState = uiState,
                    onNameChanged = { uiState = uiState.copy(nameInput = it, message = null) },
                    onSymbolChanged = { uiState = uiState.copy(symbolInput = it, message = null) },
                    onSave = { saveCalls++ },
                    onDelete = { deletedUnit = it },
                    onCancel = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = {}
                )
            }
        }

        composeRule.onNodeWithText("Edit satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan perubahan").assertIsDisplayed()
        composeRule.onNodeWithTag("unit-save").assertIsNotEnabled()
        composeRule.onNodeWithTag("unit-delete-btn").assertIsDisplayed()

        // Editing symbol enables save button
        composeRule.onNodeWithTag("unit-symbol").performTextReplacement("kilo")
        composeRule.onNodeWithTag("unit-save").performClick()
        assertEquals(1, saveCalls)

        // Tapping delete button opens confirmation dialog
        composeRule.onNodeWithTag("unit-delete-btn").performClick()
        composeRule.onNodeWithTag("delete-satuan-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Hapus satuan?").assertIsDisplayed()
        composeRule.onNodeWithTag("delete-satuan-confirm").performClick()
        assertEquals("u1", deletedUnit?.id)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun serverErrorsRouteToFieldOrNetworkToast() {
        val unit = ProductUnit("u1", "Kilogram", "kg", "2026-11-18T10:00:00Z")
        var uiState by mutableStateOf(
            ProductUnitUiState(
                editingUnit = unit,
                nameInput = unit.name,
                symbolInput = unit.symbol,
                message = "Simbol satuan sudah terdaftar.",
                isFormVisible = true,
                isLoading = false
            )
        )

        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSatuanRoute(
                    uiState = uiState,
                    onNameChanged = { uiState = uiState.copy(nameInput = it) },
                    onSymbolChanged = { uiState = uiState.copy(symbolInput = it) },
                    onSave = {},
                    onDelete = {},
                    onCancel = {},
                    onStockClick = {},
                    onDashboardClick = {},
                    onDismissMessage = { uiState = uiState.copy(message = null) }
                )
            }
        }

        composeRule.onNodeWithText("Simbol satuan sudah terdaftar.").assertIsDisplayed()

        uiState = uiState.copy(message = "Satuan gagal disimpan karena koneksi bermasalah.")
        composeRule.onNodeWithTag("tambah-satuan-error-toast").assertIsDisplayed()
        composeRule.onNodeWithText("Tidak dapat terhubung").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutShowsNavRailHeroPanelAndSideBySideFields() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerTambahSatuanScreen(
                    name = "",
                    symbol = "",
                    isEditMode = false,
                    onNameChange = {},
                    onSymbolChange = {},
                    onBack = {},
                    onSubmit = {},
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithTag("nav-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-STOCK").assertIsDisplayed()
        composeRule.onNodeWithText("Satuan baru").assertIsDisplayed()
        composeRule.onNodeWithText("Nama satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Singkatan satuan").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan satuan").assertIsDisplayed()
    }
}
