package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.purchasehistory.PurchaseHistoryUiState
import com.tbterminal.app.ui.purchasehistory.PurchaseSortOrder
import com.tbterminal.app.ui.theme.TbterminalappTheme
import org.junit.Assert.assertEquals
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
class OwnerRiwayatPembelianFilterBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneModalSupportsResetDraftWithoutApplyingAndAppliesOnTerapkan() {
        var appliedResult: List<Any?>? = null
        var dismissCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerPurchaseFilterModal(
                    state = PurchaseHistoryUiState(
                        suppliers = sampleReferenceSuppliers(),
                        selectedPeriodType = "Hari Ini",
                        selectedSupplierId = null,
                        sortOrder = PurchaseSortOrder.NEWEST
                    ),
                    onDismiss = { dismissCount++ },
                    onApplyFilter = { supplierId, startIso, endIso, periodType, sortOrder ->
                        appliedResult = listOf(supplierId, startIso, endIso, periodType, sortOrder)
                    }
                )
            }
        }

        composeRule.onNodeWithText("Filter pembelian").assertIsDisplayed()
        composeRule.onNodeWithText("Periode").assertIsDisplayed()
        composeRule.onNodeWithText("Supplier").assertIsDisplayed()
        composeRule.onNodeWithText("Urutkan").assertIsDisplayed()

        // Verify touch sizes & RadioButton role matching Filter Penjualan
        val chipBounds = composeRule.onNodeWithTag("purchase-filter-period-Hari Ini").getBoundsInRoot()
        assertEquals(48.dp, chipBounds.bottom - chipBounds.top)
        composeRule.onNodeWithTag("purchase-filter-period-Hari Ini")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()

        val footerResetBounds = composeRule.onNodeWithTag("purchase-history-filter-reset").getBoundsInRoot()
        val footerApplyBounds = composeRule.onNodeWithTag("purchase-history-filter-done").getBoundsInRoot()
        assertEquals(52.dp, footerResetBounds.bottom - footerResetBounds.top)
        assertEquals(52.dp, footerApplyBounds.bottom - footerApplyBounds.top)

        // Pick non-default options across all sections
        composeRule.onNodeWithTag("purchase-filter-period-Minggu Ini").performClick()
        composeRule.onNodeWithTag("purchase-filter-supplier-s2").performClick()
        composeRule.onNodeWithTag("purchase-filter-sort-highest").performClick()

        composeRule.onNodeWithTag("purchase-filter-period-Minggu Ini").assertIsSelected()
        composeRule.onNodeWithTag("purchase-filter-supplier-s2").assertIsSelected()
        composeRule.onNodeWithTag("purchase-filter-sort-highest").assertIsSelected()

        // Tap Reset -> resets draft to Semua + all suppliers + NEWEST without closing/applying
        composeRule.onNodeWithTag("purchase-history-filter-reset").performClick()
        assertNull(appliedResult)
        assertEquals(0, dismissCount)
        composeRule.onNodeWithTag("purchase-filter-period-Semua").assertIsSelected()
        composeRule.onNodeWithTag("purchase-filter-period-Hari Ini").assertIsNotSelected()
        composeRule.onNodeWithTag("purchase-filter-supplier-all").assertIsSelected()
        composeRule.onNodeWithTag("purchase-filter-sort-newest").assertIsSelected()

        // Pick supplier + sort, then Terapkan
        composeRule.onNodeWithTag("purchase-filter-supplier-s1").performClick()
        composeRule.onNodeWithTag("purchase-filter-sort-due").performClick()
        composeRule.onNodeWithTag("purchase-history-filter-done").performClick()

        assertEquals(
            listOf("s1", null, null, "Semua", PurchaseSortOrder.DUE_NEAREST),
            appliedResult
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDialogSupportsSupplierSearchAndCloseDiscardsDraft() {
        val manySuppliers = (1..6).map { i ->
            Supplier(
                id = "sup-$i",
                name = if (i == 6) "PT Baja Sakti" else "Supplier $i",
                phone = null,
                address = null,
                paymentTermDays = 14,
                isActive = true,
                createdAt = "2026-11-01T00:00:00Z",
                updatedAt = "2026-11-01T00:00:00Z"
            )
        }
        var appliedCount = 0
        var dismissCount = 0

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.4f)
            ) {
                TbterminalappTheme {
                    OwnerPurchaseFilterModal(
                        state = PurchaseHistoryUiState(suppliers = manySuppliers),
                        onDismiss = { dismissCount++ },
                        onApplyFilter = { _, _, _, _, _ -> appliedCount++ }
                    )
                }
            }
        }

        // Toggle supplier search when suppliers > 4
        composeRule.onNodeWithTag("purchase-filter-toggle-supplier-search").performClick()
        composeRule.onNodeWithTag("purchase-filter-supplier-search-input").performTextInput("Baja")
        composeRule.onNodeWithTag("purchase-filter-supplier-sup-6").assertIsDisplayed().performClick()

        // Footer stays pinned cleanly even at 1.4f font scale
        val doneBounds = composeRule.onNodeWithTag("purchase-history-filter-done").getBoundsInRoot()
        assertTrue(doneBounds.bottom <= 768.dp)

        // Close button dismisses without applying
        composeRule.onNodeWithTag("purchase-history-filter-close").performClick()
        assertEquals(1, dismissCount)
        assertEquals(0, appliedCount)
    }
}
