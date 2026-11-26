package com.tbterminal.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.checkout.LocalCheckoutCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutItemCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutPaymentLineCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutResult
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.DiscountType
import com.tbterminal.app.ui.checkout.CartItem
import com.tbterminal.app.ui.checkout.PaymentMethod
import com.tbterminal.app.ui.checkout.SplitPaymentLineUi
import com.tbterminal.app.ui.checkout.checkoutChange
import com.tbterminal.app.ui.checkout.estimateCashRoundingCalculation
import com.tbterminal.app.ui.checkout.estimateCheckoutTotals
import com.tbterminal.app.ui.checkout.estimateTaxCalculation
import com.tbterminal.app.ui.checkout.validateSplitCheckoutPayment
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OfflineExactMoneyAndParityTest {
    private lateinit var database: TbTerminalDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun scaledDecimal2_ofExact_rejectsMoreThanTwoDecimalPlacesWithoutSilentRounding() {
        assertThrows(IllegalArgumentException::class.java) {
            ScaledDecimal2.ofExact(BigDecimal("12.345"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ScaledDecimal2.ofExact(BigDecimal("0.001"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ScaledDecimal2.ofExact(BigDecimal("19999.999999"))
        }

        // Trailing zeros beyond 2 decimals are exact after stripTrailingZeros()
        val exact = ScaledDecimal2.ofExact(BigDecimal("12.3400"))
        assertEquals(1234L, exact.minorUnits)
        assertEquals("12.34", exact.toBigDecimal().toPlainString())
    }

    @Test
    fun offlineCheckoutTotalChangeAndCashSessionDifference_areExactWithoutDoubleDrift() = runBlocking {
        val sessionService = LocalCashSessionService(database, deviceId = "android-dev-1")
        val expenseService = LocalCashExpenseService(database, deviceId = "android-dev-1")
        val checkoutService = LocalCheckoutService(database, deviceId = "android-dev-1")

        // 100000.10 + 19999.95 + 0.05 + 1000.20 - 500.10 - 250.20 = 120250.00
        val openResult = sessionService.openLocalSession(
            cashierUserId = "kasir-1",
            startingCash = BigDecimal("100000.10"),
        ) as LocalCashSessionResult.Success
        val sessionId = openResult.session.localId

        val checkoutResult = checkoutService.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = sessionId,
                paymentMethod = "tunai",
                payments = listOf(
                    LocalCheckoutPaymentLineCommand(method = "tunai", amount = BigDecimal("19999.95")),
                    LocalCheckoutPaymentLineCommand(method = "tunai", amount = BigDecimal("0.05")),
                ),
                subtotal = BigDecimal("20000.00"),
                total = BigDecimal("20000.00"),
                paidAmount = BigDecimal("20000.00"),
                items = listOf(
                    LocalCheckoutItemCommand(
                        productServerId = "p-1",
                        productNameSnapshot = "Paku",
                        quantity = BigDecimal("2"),
                        priceAtTransaction = BigDecimal("10000.00"),
                        subtotal = BigDecimal("20000.00"),
                    )
                )
            )
        )
        assertTrue(checkoutResult is LocalCheckoutResult.Success)

        assertTrue(
            expenseService.addLocalCashMovement(
                cashSessionLocalId = sessionId,
                type = CashMovementType.CASH_IN,
                amount = BigDecimal("1000.20"),
                description = "Tambah modal"
            ) is LocalCashExpenseResult.Success
        )
        assertTrue(
            expenseService.addLocalCashMovement(
                cashSessionLocalId = sessionId,
                type = CashMovementType.CASH_OUT,
                amount = BigDecimal("500.10"),
                description = "Plastik"
            ) is LocalCashExpenseResult.Success
        )
        assertTrue(
            expenseService.addLocalCashMovement(
                cashSessionLocalId = sessionId,
                type = CashMovementType.CASH_OUT,
                amount = BigDecimal("250.20"),
                description = "Lakban"
            ) is LocalCashExpenseResult.Success
        )

        val closeResult = sessionService.closeLocalSession(
            localId = sessionId,
            closingCash = BigDecimal("120249.75"),
            closingNote = "Cek selisih eksak"
        ) as LocalCashSessionResult.Success

        assertEquals("120250.00", closeResult.session.expectedCash!!.toBigDecimal().toPlainString())
        assertEquals("120249.75", closeResult.session.actualCash!!.toBigDecimal().toPlainString())
        assertEquals("-0.25", closeResult.session.difference!!.toBigDecimal().toPlainString())

        // Verify SQLite SUM & report projection produce the exact same integer totals
        val cashReport = database.offlineCashReportDao()
            .observeCashSessions(0L, Long.MAX_VALUE, 10)
            .first()
            .single()
        assertEquals("100000.10", cashReport.startingCash.toBigDecimal().toPlainString())
        assertEquals("20000.00", cashReport.totalCashSales.toBigDecimal().toPlainString())
        assertEquals("1750.50", cashReport.totalExpense.toBigDecimal().toPlainString())

        // Verify exact change (kembalian) calculations
        val change = checkoutChange(amountPaidInput = "50000", total = BigDecimal("19999.95"))
        assertEquals("30000.05", change.toPlainString())

        val splitRes = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.QRIS, amountInput = "10000"),
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "20000"),
            ),
            total = BigDecimal("25000.30"),
            isDpMode = false,
            customerSelected = false
        )
        assertEquals("4999.70", splitRes.changeAmount.toPlainString())
    }

    @Test
    fun offlineCheckoutCalculations_matchBackendHalfUpTaxDiscountAndRoundingParity() {
        // Case 1: Fractional price + percentage item discount + percentage tx discount + EXCLUDE 11% tax + 500 NEAREST rounding
        // Line 1: qty 3 * 1234.55 = 3703.65; item discount 12.5% = 3703.65 * 12.5 / 100 = 462.95625 -> HALF_UP = 462.96
        // Line 2: qty 2 * 999.95 = 1999.90; no discount
        // Gross = 5703.55; itemDiscount = 462.96; afterItem = 5240.59
        // Tx discount 5% = 5240.59 * 5 / 100 = 262.0295 -> HALF_UP = 262.03
        // Net = 5240.59 - 262.03 = 4978.56 (totalDiscount = 724.99)
        // Tax EXCLUDE 11%: taxAmount = 4978.56 * 11 / 100 = 547.6416 -> HALF_UP = 547.64
        // TotalWithTax = 5526.20
        // Rounding 500 NEAREST (HALF_UP): 5526.20 / 500 = 11.0524 -> 11 * 500 = 5500.00; roundingAmount = -26.20
        val items = listOf(
            CartItem(
                productId = "p-1",
                productName = "Baut",
                unitPrice = BigDecimal("1234.55"),
                quantity = 3,
                unitName = "Pcs",
                discountRequest = CheckoutDiscount(DiscountType.PERCENTAGE, BigDecimal("12.5"))
            ),
            CartItem(
                productId = "p-2",
                productName = "Mur",
                unitPrice = BigDecimal("999.95"),
                quantity = 2,
                unitName = "Pcs"
            )
        )
        val totals = estimateCheckoutTotals(
            items = items,
            transactionDiscount = CheckoutDiscount(DiscountType.PERCENTAGE, BigDecimal("5"))
        )
        assertEquals("5703.55", totals.grossSubtotal.toPlainString())
        assertEquals("462.96", totals.itemDiscountTotal.toPlainString())
        assertEquals("262.03", totals.transactionDiscountAmount.toPlainString())
        assertEquals("724.99", totals.totalDiscountAmount.toPlainString())
        assertEquals("4978.56", totals.netTotal.toPlainString())

        val taxExclude = estimateTaxCalculation(
            netGoodsTotal = totals.netTotal,
            taxEnabled = true,
            taxRate = BigDecimal("11"),
            taxMode = "EXCLUDE",
            taxLabel = "PPN"
        )
        assertEquals("4978.56", taxExclude.taxableBase.toPlainString())
        assertEquals("547.64", taxExclude.taxAmount.toPlainString())
        assertEquals("5526.20", taxExclude.totalWithTax.toPlainString())

        val round500Nearest = estimateCashRoundingCalculation(
            totalBeforeRounding = taxExclude.totalWithTax,
            hasCashPayment = true,
            roundingUnit = 500,
            roundingRule = "NEAREST"
        )
        assertEquals("-26.20", round500Nearest.roundingAmount.toPlainString())
        assertEquals("5500.00", round500Nearest.finalTotal.toPlainString())

        // Case 2: Tax INCLUDE 11% (divisor = 1 + (11.00 / 100 -> 0.110000) = 1.110000)
        // dpp = 10000.55 / 1.110000 = 9009.5045... -> HALF_UP = 9009.50; taxAmount = 10000.55 - 9009.50 = 991.05
        val taxInclude = estimateTaxCalculation(
            netGoodsTotal = BigDecimal("10000.55"),
            taxEnabled = true,
            taxRate = BigDecimal("11"),
            taxMode = "INCLUDE",
            taxLabel = "PPN"
        )
        assertEquals("9009.50", taxInclude.taxableBase.toPlainString())
        assertEquals("991.05", taxInclude.taxAmount.toPlainString())
        assertEquals("10000.55", taxInclude.totalWithTax.toPlainString())

        // Case 3: Rounding 1000 UP (CEILING) and DOWN (FLOOR)
        val roundUp = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("10000.01"),
            hasCashPayment = true,
            roundingUnit = 1000,
            roundingRule = "UP"
        )
        assertEquals("999.99", roundUp.roundingAmount.toPlainString())
        assertEquals("11000.00", roundUp.finalTotal.toPlainString())

        val roundDown = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("10999.99"),
            hasCashPayment = true,
            roundingUnit = 1000,
            roundingRule = "DOWN"
        )
        assertEquals("-999.99", roundDown.roundingAmount.toPlainString())
        assertEquals("10000.00", roundDown.finalTotal.toPlainString())
    }
}
