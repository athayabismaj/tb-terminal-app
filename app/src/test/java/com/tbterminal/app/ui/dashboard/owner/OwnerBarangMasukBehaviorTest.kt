package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.incominggoods.IncomingPaymentMethod
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.LocalDate
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
class OwnerBarangMasukBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1605dp-mdpi")
    fun phoneFormPaymentMethodsAutoSyncManualEditAndDebtVisibility() {
        val product = sampleReferenceBarangMasukProduct()
        val supplier = sampleReferenceBarangMasukSupplier()

        composeRule.setContent {
            TbterminalappTheme {
                var qty by remember { mutableStateOf("200") }
                var price by remember { mutableStateOf("38000") }
                var method by remember { mutableStateOf(IncomingPaymentMethod.TRANSFER) }
                var paid by remember { mutableStateOf("5000000") }
                var manualPaid by remember { mutableStateOf(true) }
                var dueDate by remember { mutableStateOf<LocalDate?>(LocalDate.of(2026, 12, 23)) }

                val calc = calculateOwnerBarangMasuk(product, qty, price, paid)
                val valid = isOwnerBarangMasukFormValid(product, supplier, calc, dueDate)

                OwnerBarangMasukScreen(
                    selectedProduct = product,
                    selectedSupplier = supplier,
                    invoiceNo = "",
                    quantityDigits = qty,
                    unitPriceDigits = price,
                    paymentMethod = method,
                    amountPaidDigits = paid,
                    dueDate = dueDate,
                    notes = "",
                    calculation = calc,
                    submitEnabled = valid,
                    isSubmitting = false,
                    onBack = {},
                    onProductFieldClick = {},
                    onSupplierFieldClick = {},
                    onAddSupplierClick = {},
                    onInvoiceNoChange = {},
                    onGenerateInvoiceNo = {},
                    onQuantityChange = {
                        qty = it
                        if (!manualPaid) {
                            paid = initialAmountPaidDigitsForMethod(
                                method,
                                calculateOwnerBarangMasuk(product, qty, price, paid).total
                            )
                        }
                    },
                    onUnitPriceChange = {
                        price = it
                        if (!manualPaid) {
                            paid = initialAmountPaidDigitsForMethod(
                                method,
                                calculateOwnerBarangMasuk(product, qty, price, paid).total
                            )
                        }
                    },
                    onPaymentMethodSelect = { chosen ->
                        method = chosen
                        manualPaid = false
                        paid = initialAmountPaidDigitsForMethod(chosen, calc.total)
                    },
                    onAmountPaidChange = {
                        manualPaid = true
                        paid = it
                    },
                    onPayFullClick = {
                        manualPaid = false
                        paid = initialAmountPaidDigitsForMethod(IncomingPaymentMethod.CASH, calc.total)
                    },
                    onDueDateFieldClick = {},
                    onNotesChange = {},
                    onSubmit = {},
                    onNavSelect = {}
                )
            }
        }

        // Initial partial payment state matches HTML & PNG
        composeRule.onNodeWithTag("nav-HOME").assertDoesNotExist()
        composeRule.onNodeWithText("Pilih Pembayaran").assertDoesNotExist()
        composeRule.onNodeWithTag("barang-masuk-method-transfer").assertIsSelected()
        composeRule.onNodeWithTag("barang-masuk-phone-total").assertTextEquals("Rp 7.600.000")
        composeRule.onNodeWithTag("barang-masuk-sisa-utang").assertTextEquals("Rp 2.600.000")
        composeRule.onNodeWithText("23 Des 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Mengikuti termin supplier, 30 hari.").assertIsDisplayed()

        // Tap Bayar penuh -> sets amountPaid = total and hides Sisa utang + Jatuh tempo
        composeRule.onNodeWithTag("barang-masuk-pay-full").performClick()
        composeRule.onNodeWithTag("barang-masuk-sisa-utang").assertDoesNotExist()
        composeRule.onNodeWithText("Jatuh tempo").assertDoesNotExist()

        // Switch to Hutang -> sets amountPaid = 0 and shows full debt + Jatuh tempo
        composeRule.onNodeWithTag("barang-masuk-method-hutang").performClick()
        composeRule.onNodeWithTag("barang-masuk-sisa-utang").assertTextEquals("Rp 7.600.000")
        composeRule.onNodeWithTag("barang-masuk-due-date").assertIsDisplayed()

        // Enter over-payment -> shows inline error text without crashing
        composeRule.onNodeWithTag("barang-masuk-amount-paid-input").performScrollTo().performTextReplacement("9000000")
        composeRule.onNodeWithText("Tidak boleh melebihi total pembelian").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletLayoutShowsNavRailWithStockActiveAndLiveGreenSummary() {
        val navClicks = mutableListOf<DashboardNavDestination>()
        var addSupplierClicks = 0
        val outOfStockProduct = ProductStock(
            productId = "p-2",
            sku = "BSI-10SNI",
            productName = "Besi Beton Polos SNI Diameter 10mm Panjang 12 Meter Merk Krakatau",
            categoryName = "Besi & Baja",
            unitName = "Batang",
            quantity = BigDecimal.ZERO,
            minStock = BigDecimal("20"),
            priceBuy = BigDecimal("65000"),
            priceRetail = BigDecimal("78000"),
            priceContractor = BigDecimal("74000"),
            discount = BigDecimal.ZERO,
            isActive = true
        )
        val noTermSupplier = sampleReferenceBarangMasukSupplier().copy(
            name = "PT Baja Utama",
            paymentTermDays = 0
        )
        val calc = calculateOwnerBarangMasuk(
            selectedProduct = outOfStockProduct,
            quantityDigits = "50",
            unitPriceDigits = "65000",
            amountPaidDigits = "1000000"
        )

        composeRule.setContent {
            TbterminalappTheme {
                OwnerBarangMasukScreen(
                    selectedProduct = outOfStockProduct,
                    selectedSupplier = noTermSupplier,
                    invoiceNo = "BM-202611-001",
                    quantityDigits = "50",
                    unitPriceDigits = "65000",
                    paymentMethod = IncomingPaymentMethod.TRANSFER,
                    amountPaidDigits = "1000000",
                    dueDate = null,
                    notes = "",
                    calculation = calc,
                    submitEnabled = false,
                    isSubmitting = false,
                    onBack = {},
                    onProductFieldClick = {},
                    onSupplierFieldClick = {},
                    onAddSupplierClick = { addSupplierClicks++ },
                    onInvoiceNoChange = {},
                    onGenerateInvoiceNo = {},
                    onQuantityChange = {},
                    onUnitPriceChange = {},
                    onPaymentMethodSelect = {},
                    onAmountPaidChange = {},
                    onPayFullClick = {},
                    onDueDateFieldClick = {},
                    onNotesChange = {},
                    onSubmit = {},
                    onNavSelect = { navClicks.add(it) }
                )
            }
        }

        // Nav rail visible with Stok
        composeRule.onNodeWithTag("nav-STOCK").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-HOME").performClick()
        assertEquals(listOf(DashboardNavDestination.BERANDA), navClicks)

        // Live summary in green panel
        composeRule.onNodeWithTag("barang-masuk-tablet-total").assertTextEquals("3.250.000")
        composeRule.onNodeWithTag("barang-masuk-tablet-paid").assertTextEquals("Rp 1 jt")
        composeRule.onNodeWithTag("barang-masuk-tablet-remaining").assertTextEquals("Rp 2,2 jt")
        composeRule.onNodeWithTag("barang-masuk-tablet-impact-stock").assertTextEquals("0 \u2192 50 Batang")
        composeRule.onNodeWithTag("barang-masuk-tablet-impact-hpp").assertTextEquals("Rp 65.000")

        // Out-of-stock status & tablet hides phone metrics / invoice helper text
        composeRule.onNodeWithText("Habis").assertIsDisplayed()
        composeRule.onNodeWithText("Stok sekarang").assertDoesNotExist()
        composeRule.onNodeWithText("Nomor nota dibuat otomatis dan tetap dapat diubah.").assertDoesNotExist()
        composeRule.onNodeWithText("Jatuh tempo wajib dipilih").assertIsDisplayed()

        // Tap + Tambah supplier baru
        composeRule.onNodeWithTag("barang-masuk-add-supplier").performClick()
        assertEquals(1, addSupplierClicks)
    }
}
