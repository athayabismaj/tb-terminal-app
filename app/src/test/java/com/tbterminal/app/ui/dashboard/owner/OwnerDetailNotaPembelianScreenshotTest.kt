package com.tbterminal.app.ui.dashboard.owner

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseItemDetail
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
class OwnerDetailNotaPembelianScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureDetailNotaPembelianPhone() {
        composeRule.setContent {
            OwnerDetailNotaPembelianPhonePreview()
        }
        val output = File("build/roborazzi/detail_nota_pembelian_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureDetailNotaPembelianTablet() {
        composeRule.setContent {
            OwnerDetailNotaPembelianTabletPreview()
        }
        val output = File("build/roborazzi/detail_nota_pembelian_tablet.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun captureDetailNotaPembelianHutangPhone() {
        composeRule.setContent {
            TbterminalappTheme {
                OwnerDetailNotaPembelianScreen(
                    uiState = OwnerPurchaseDetailUiState(
                        purchase = PurchaseDetail(
                            id = "p-detail-2",
                            supplierId = "s2",
                            supplierName = "CV Sinar Baja",
                            invoiceNo = "BM-20261122-003",
                            total = BigDecimal("2600000"),
                            notes = "Diantar bertahap ke gudang belakang.",
                            receivedAt = "2026-11-22T09:41:00Z",
                            createdAt = "2026-11-22T09:41:00Z",
                            items = listOf(
                                PurchaseItemDetail(
                                    productId = "p1",
                                    productName = "Besi Beton Ulir 10mm",
                                    quantity = BigDecimal("20"),
                                    priceAtTransaction = BigDecimal("95000"),
                                    subtotal = BigDecimal("1900000")
                                ),
                                PurchaseItemDetail(
                                    productId = "p2",
                                    productName = "Kawat Bendrat",
                                    quantity = BigDecimal("35"),
                                    priceAtTransaction = BigDecimal("20000"),
                                    subtotal = BigDecimal("700000")
                                )
                            )
                        ),
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
        val output = File("build/roborazzi/detail_nota_pembelian_hutang_ponsel.png").apply { parentFile?.mkdirs() }
        composeRule.onRoot().captureRoboImage(
            file = output,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
        )
    }
}
