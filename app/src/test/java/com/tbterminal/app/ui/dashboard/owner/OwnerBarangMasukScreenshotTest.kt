package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.incominggoods.IncomingPaymentMethod
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.io.File
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class OwnerBarangMasukScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1605dp-mdpi")
    fun captureBarangMasukPhoneFull() {
        composeRule.setContent {
            OwnerBarangMasukPhonePreview()
        }
        val output = File("build/roborazzi/barang_masuk_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureBarangMasukTablet() {
        composeRule.setContent {
            OwnerBarangMasukTabletPreview()
        }
        val output = File("build/roborazzi/barang_masuk_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureBarangMasukOutOfStockHutangAndLargeFont() {
        val longOutOfStock = ProductStock(
            productId = "p-long",
            sku = "CAT-EKST-25KG",
            productName = "Cat Tembok Eksterior Tahan Cuaca 25Kg Warna Putih Bersih",
            categoryName = "Cat & Pelapis Dinding",
            unitName = "Pail",
            quantity = BigDecimal.ZERO,
            minStock = BigDecimal("5"),
            priceBuy = BigDecimal("650000"),
            priceRetail = BigDecimal("780000"),
            priceContractor = BigDecimal("740000"),
            discount = BigDecimal.ZERO,
            isActive = true
        )
        val supZeroTerm = sampleReferenceBarangMasukSupplier().copy(
            name = "CV Karya Bangunan Nusantara",
            paymentTermDays = 0
        )
        val calc = calculateOwnerBarangMasuk(longOutOfStock, "10", "650000", "0")

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.4f)
            ) {
                TbterminalappTheme {
                    OwnerBarangMasukScreen(
                        selectedProduct = longOutOfStock,
                        selectedSupplier = supZeroTerm,
                        invoiceNo = "",
                        quantityDigits = "10",
                        unitPriceDigits = "650000",
                        paymentMethod = IncomingPaymentMethod.DEBT,
                        amountPaidDigits = "0",
                        dueDate = null,
                        notes = "",
                        calculation = calc,
                        submitEnabled = false,
                        isSubmitting = false,
                        onBack = {},
                        onProductFieldClick = {},
                        onSupplierFieldClick = {},
                        onAddSupplierClick = {},
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
                        onNavSelect = {}
                    )
                }
            }
        }
        val output = File("build/roborazzi/barang_masuk_habis_hutang_font_besar.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}
