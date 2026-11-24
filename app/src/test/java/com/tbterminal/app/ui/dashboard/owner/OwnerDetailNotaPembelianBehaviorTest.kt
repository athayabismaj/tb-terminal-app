package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseItemDetail
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.LocalDate
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
class OwnerDetailNotaPembelianBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pureFormattersAndPaymentBreakdownMatchSpec() {
        assertEquals("Rp 0", formatRupiahWhole(BigDecimal.ZERO))
        assertEquals("Rp 380.000", formatRupiahWhole(BigDecimal("380000")))
        assertEquals("Rp 2.600.000", formatRupiahWhole(BigDecimal("2600000")))
        assertEquals("Rp 12.500.000", formatRupiahWhole(BigDecimal("12500000")))

        assertEquals("Rp 0", formatSalesColumnCompactRupiah(BigDecimal.ZERO))
        assertEquals("Rp 380 rb", formatSalesColumnCompactRupiah(BigDecimal("380000")))
        assertEquals("Rp 2,6 jt", formatSalesColumnCompactRupiah(BigDecimal("2600000")))
        assertEquals("Rp 12,5 jt", formatSalesColumnCompactRupiah(BigDecimal("12500000")))

        // 11:32 UTC = 18:32 WIB, tanpa sufiks WIB
        assertEquals("23 Nov 2026, 18:32", formatPurchaseDetailDateTime("2026-11-23T11:32:00Z"))

        val purchase = sampleReferencePurchaseDetail()
        val lunasBreakdown = resolvePurchasePaymentBreakdown(purchase, matchingPayable = null)
        assertEquals(BigDecimal("380000"), lunasBreakdown.paidAmount)
        assertEquals(BigDecimal.ZERO, lunasBreakdown.remainingAmount)
        assertFalse(lunasBreakdown.isUnpaid)
        assertNull(lunasBreakdown.dueDate)
        assertFalse(lunasBreakdown.isDueOverdue)

        val hutangPurchase = purchase.copy(id = "p-detail-2", total = BigDecimal("2600000"))
        val payable = sampleReferencePurchasePayable()
        val hutangBreakdown = resolvePurchasePaymentBreakdown(
            purchase = hutangPurchase,
            matchingPayable = payable,
            today = LocalDate.of(2026, 11, 23)
        )
        assertEquals(BigDecimal("600000"), hutangBreakdown.paidAmount)
        assertEquals(BigDecimal("2000000"), hutangBreakdown.remainingAmount)
        assertTrue(hutangBreakdown.isUnpaid)
        assertEquals("2026-11-15T00:00:00Z", hutangBreakdown.dueDate)
        assertTrue(hutangBreakdown.isDueOverdue)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun phoneReferenceLunasDetailMatchesSpecAndOmitsDeletedElements() {
        var backClicks = 0
        var copiedInvoice = ""
        var clickedSupplierId = ""

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailNotaPembelianScreen(
                    uiState = OwnerPurchaseDetailUiState(
                        purchase = sampleReferencePurchaseDetail(),
                        matchingPayable = null,
                        isLoading = false
                    ),
                    onBack = { backClicks++ },
                    onRetry = {},
                    onCopyInvoice = { copiedInvoice = it },
                    onSupplierClick = { clickedSupplierId = it },
                    onNavSelect = {}
                )
            }
        }

        composeRule.onNodeWithText("Detail nota pembelian").assertIsDisplayed()
        val titleBounds = composeRule.onNodeWithText("Detail nota pembelian").getBoundsInRoot()
        assertTrue(titleBounds.top >= 56.dp)
        composeRule.onNodeWithTag("detail-purchase-status").assertTextEquals("Lunas")
        composeRule.onNodeWithTag("detail-purchase-invoice-no").assertTextEquals("BM-20261123-001")
        composeRule.onNodeWithTag("detail-purchase-total-amount").assertTextEquals("380.000")
        composeRule.onNodeWithTag("detail-purchase-paid").assertTextEquals("Rp 380 rb")
        composeRule.onNodeWithTag("detail-purchase-debt").assertTextEquals("Rp 0")

        composeRule.onNodeWithTag("detail-purchase-supplier", useUnmergedTree = true).assertTextEquals("Supri")
        composeRule.onNodeWithTag("detail-purchase-received-at").assertTextEquals("23 Nov 2026, 18:32")
        composeRule.onNodeWithTag("detail-purchase-item-count").assertTextEquals("1 macam")
        composeRule.onNodeWithTag("detail-purchase-item-0").assertIsDisplayed()
        composeRule.onNodeWithText("Semen Merdeka").assertIsDisplayed()
        composeRule.onNodeWithText("10 \u00D7 Rp 38.000").assertIsDisplayed()

        // Deleted elements must not appear
        composeRule.onNodeWithText("No. nota").assertDoesNotExist()
        composeRule.onNodeWithText("Status pembayaran").assertDoesNotExist()
        composeRule.onNodeWithText("Ringkasan pembayaran").assertDoesNotExist()
        composeRule.onNodeWithText("Total item").assertDoesNotExist()
        composeRule.onNodeWithText("Catatan").assertDoesNotExist()
        composeRule.onNodeWithText("Tidak ada catatan").assertDoesNotExist()
        composeRule.onAllNodesWithText("WIB", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText(",00", substring = true).assertCountEquals(0)

        composeRule.onNodeWithTag("detail-purchase-copy-invoice-btn").performClick()
        assertEquals("BM-20261123-001", copiedInvoice)

        composeRule.onNodeWithTag("detail-purchase-supplier-row").performClick()
        assertEquals("s1", clickedSupplierId)

        composeRule.onNodeWithTag("detail-purchase-back-btn").performClick()
        assertEquals(1, backClicks)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun phoneHutangWithMultipleItemsDueDateLongNotesAndLargeFontRendersCleanly() {
        val hutangDetail = PurchaseDetail(
            id = "p-detail-2",
            supplierId = "s2",
            supplierName = "PT Distributor Bahan Bangunan Nusantara Sejahtera Abadi",
            invoiceNo = "BM-20261101-009",
            total = BigDecimal("2600000"),
            notes = "Pengiriman dibagi dua tahap karena akses proyek sempit dan harus diturunkan di gudang belakang.",
            receivedAt = "2026-11-01T03:15:00Z",
            createdAt = "2026-11-01T03:15:00Z",
            items = listOf(
                PurchaseItemDetail(
                    productId = "pr-1",
                    productName = "Besi Beton Ulir SNI 12mm Panjang Standar Proyek 12 Meter",
                    quantity = BigDecimal("20"),
                    priceAtTransaction = BigDecimal("95000"),
                    subtotal = BigDecimal("1900000")
                ),
                PurchaseItemDetail(
                    productId = "pr-2",
                    productName = "Kawat Bendrat Galvanis",
                    quantity = BigDecimal("35"),
                    priceAtTransaction = BigDecimal("20000"),
                    subtotal = BigDecimal("700000")
                )
            )
        )

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.4f)
            ) {
                TbterminalappTheme {
                    OwnerDetailNotaPembelianScreen(
                        uiState = OwnerPurchaseDetailUiState(
                            purchase = hutangDetail,
                            matchingPayable = sampleReferencePurchasePayable(),
                            isLoading = false
                        ),
                        onBack = {},
                        onRetry = {},
                        onCopyInvoice = {},
                        onSupplierClick = {},
                        onNavSelect = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("detail-purchase-status").assertTextEquals("Hutang")
        composeRule.onNodeWithTag("detail-purchase-invoice-no").assertTextEquals("BM-20261101-009")
        composeRule.onNodeWithTag("detail-purchase-total-amount").assertTextEquals("2.600.000")
        composeRule.onNodeWithTag("detail-purchase-paid").assertTextEquals("Rp 600 rb")
        composeRule.onNodeWithTag("detail-purchase-debt").assertTextEquals("Rp 2 jt")

        composeRule.onNodeWithTag("detail-purchase-due-date").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail-purchase-notes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail-purchase-item-count").performScrollTo().assertTextEquals("2 macam")
        composeRule.onNodeWithTag("detail-purchase-item-0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail-purchase-item-1").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText(",00", substring = true).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletDetailNotaShowsNavRailAndErrorRetryState() {
        var retryClicks = 0
        val navSelections = mutableListOf<DashboardNavDestination>()

        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailNotaPembelianScreen(
                    uiState = OwnerPurchaseDetailUiState(
                        purchase = null,
                        isLoading = false,
                        errorMessage = "Koneksi bermasalah"
                    ),
                    onBack = {},
                    onRetry = { retryClicks++ },
                    onCopyInvoice = {},
                    onSupplierClick = {},
                    onNavSelect = { navSelections.add(it) }
                )
            }
        }

        composeRule.onNodeWithTag("nav-TRANSACTIONS").assertIsDisplayed()
        composeRule.onNodeWithText("Nota pembelian gagal dimuat").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-purchase-retry-btn").performClick()
        assertEquals(1, retryClicks)

        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(listOf(DashboardNavDestination.BERANDA), navSelections)
    }
}
