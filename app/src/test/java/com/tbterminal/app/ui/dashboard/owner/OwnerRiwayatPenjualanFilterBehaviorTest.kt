package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryUiState
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.time.LocalDate
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
class OwnerRiwayatPenjualanFilterBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun customDateLabelFormatterFormatsRangeMatchingDesign() {
        assertEquals(
            "1\u201315 Nov",
            formatSalesFilterCustomDateLabel(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15)
            )
        )
        assertEquals(
            "15 Nov",
            formatSalesFilterCustomDateLabel(
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 15)
            )
        )
        assertEquals("Pilih tanggal", formatSalesFilterCustomDateLabel(null, null))
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneModalSupportsResetDraftWithoutApplyingAndAppliesOnTerapkan() {
        var appliedResult: List<Any?>? = null
        var dismissCount = 0

        composeRule.setContent {
            TbterminalappTheme {
                OwnerSalesFilterModal(
                    state = AdminTransactionHistoryUiState(
                        selectedPreset = "Hari ini",
                        statusFilter = "Semua",
                        paymentMethodFilter = "Semua",
                        sortOrder = SalesSortOrder.Newest
                    ),
                    onDismiss = { dismissCount++ },
                    onApplyFilter = { status, payment, start, end, preset, sortOrder ->
                        appliedResult = listOf(status, payment, start, end, preset, sortOrder)
                    }
                )
            }
        }

        // Verify no forbidden texts
        composeRule.onNodeWithText("Filter penjualan").assertIsDisplayed()
        composeRule.onNodeWithText("Batal").assertDoesNotExist()
        composeRule.onNodeWithText("Atur periode, status transaksi, dan metode pembayaran").assertDoesNotExist()

        // Verify touch sizes and RadioButton role
        val chipBounds = composeRule.onNodeWithTag("sales-filter-period-Hari ini").getBoundsInRoot()
        assertEquals(48.dp, chipBounds.bottom - chipBounds.top)
        composeRule.onNodeWithTag("sales-filter-period-Hari ini")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()

        val footerResetBounds = composeRule.onNodeWithTag("sales-history-filter-reset").getBoundsInRoot()
        val footerApplyBounds = composeRule.onNodeWithTag("sales-history-filter-done").getBoundsInRoot()
        assertEquals(52.dp, footerResetBounds.bottom - footerResetBounds.top)
        assertEquals(52.dp, footerApplyBounds.bottom - footerApplyBounds.top)

        // Pick non-default options across all sections
        composeRule.onNodeWithTag("sales-filter-period-Minggu ini").performClick()
        composeRule.onNodeWithTag("sales-filter-status-Hutang").performClick()
        composeRule.onNodeWithTag("sales-filter-payment-qris").performClick()
        composeRule.onNodeWithTag("sales-filter-sort-highest").performClick()

        composeRule.onNodeWithTag("sales-filter-period-Minggu ini").assertIsSelected()
        composeRule.onNodeWithTag("sales-filter-status-Hutang").assertIsSelected()
        composeRule.onNodeWithTag("sales-filter-payment-qris").assertIsSelected()
        composeRule.onNodeWithTag("sales-filter-sort-highest").assertIsSelected()

        // Tap Reset -> resets draft only, does not apply or dismiss
        composeRule.onNodeWithTag("sales-history-filter-reset").performClick()
        assertNull(appliedResult)
        assertEquals(0, dismissCount)
        composeRule.onNodeWithTag("sales-filter-period-Semua").assertIsSelected()
        composeRule.onNodeWithTag("sales-filter-period-Hari ini").assertIsNotSelected()
        composeRule.onNodeWithTag("sales-filter-status-Semua").assertIsSelected()
        composeRule.onNodeWithTag("sales-filter-payment-Semua").assertIsSelected()
        composeRule.onNodeWithTag("sales-filter-sort-newest").assertIsSelected()

        // Choose Voided + Hutang + No. nota, then Terapkan
        composeRule.onNodeWithTag("sales-filter-status-voided").performClick()
        composeRule.onNodeWithTag("sales-filter-payment-hutang").performClick()
        composeRule.onNodeWithTag("sales-filter-sort-receipt").performClick()
        composeRule.onNodeWithTag("sales-history-filter-done").performClick()

        assertEquals(
            listOf("Voided", "hutang", null, null, "Semua", SalesSortOrder.ReceiptAsc),
            appliedResult
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDialogCloseDiscardsDraftAndFooterStaysPinnedWithLargeFont() {
        var appliedCount = 0
        var dismissCount = 0

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.5f)
            ) {
                TbterminalappTheme {
                    OwnerSalesFilterModal(
                        state = AdminTransactionHistoryUiState(),
                        onDismiss = { dismissCount++ },
                        onApplyFilter = { _, _, _, _, _, _ -> appliedCount++ }
                    )
                }
            }
        }

        val sheetBounds = composeRule.onNodeWithTag("sales-history-filter-sheet").getBoundsInRoot()
        assertEquals(560.dp, sheetBounds.right - sheetBounds.left)
        assertTrue(sheetBounds.bottom <= 768.dp)

        composeRule.onNodeWithTag("sales-filter-status-DP").performClick()
        composeRule.onNodeWithTag("sales-history-filter-close").performClick()

        assertEquals(1, dismissCount)
        assertEquals(0, appliedCount)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun customRangeDisplayShowsRangeChipSelected() {
        composeRule.setContent {
            TbterminalappTheme {
                var draft by remember {
                    mutableStateOf(
                        SalesFilterDraftState(
                            preset = "Kustom",
                            startDate = LocalDate.of(2026, 11, 1),
                            endDate = LocalDate.of(2026, 11, 15),
                            statusFilter = "Semua",
                            paymentMethodFilter = "Semua",
                            sortOrder = SalesSortOrder.Newest
                        )
                    )
                }
                OwnerSalesFilterFormBody(
                    draft = draft,
                    isTablet = false,
                    today = LocalDate.of(2026, 11, 23),
                    onDraftChange = { draft = it },
                    onOpenDatePicker = {}
                )
            }
        }

        composeRule.onNodeWithText("1\u201315 Nov").assertIsDisplayed()
        composeRule.onNodeWithTag("sales-history-custom-date-btn").assertIsSelected()
    }
}
