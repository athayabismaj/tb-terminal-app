package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.local.checkout.LocalCheckoutItemCommand
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.remote.StockDetailResponseDto
import com.tbterminal.app.data.repository.toProductStock
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutMultiUnitTest {

    @Test
    fun `stockDetailResponseDto maps secondary unit fields to ProductStock`() {
        val dto = StockDetailResponseDto(
            productId = "prod-cement",
            sku = "CEM-50",
            productName = "Semen Padang 50kg",
            categoryName = "Semen & Mortar",
            unitName = "Kg",
            quantity = BigDecimal("500"),
            minStock = BigDecimal("50"),
            priceBuy = BigDecimal("1200"),
            priceRetail = BigDecimal("1500"),
            priceContractor = BigDecimal("1400"),
            isActive = true,
            secondaryUnitId = "unit-sak",
            secondaryUnitName = "Sak",
            secondaryUnitFactor = "50"
        )

        val productStock = dto.toProductStock()

        assertEquals("unit-sak", productStock.secondaryUnitId)
        assertEquals("Sak", productStock.secondaryUnitName)
        assertEquals(0, BigDecimal("50").compareTo(productStock.secondaryUnitFactor))
    }

    @Test
    fun `cartItem equivalentBaseQuantity calculates correctly for base and secondary units`() {
        val baseItem = CartItem(
            productId = "prod-cement",
            productName = "Semen Padang",
            unitPrice = BigDecimal("1500"),
            quantity = 5,
            unitName = "Kg",
            cartItemId = "prod-cement",
            baseUnitPrice = BigDecimal("1500"),
            baseUnitName = "Kg",
            secondaryUnitId = "unit-sak",
            secondaryUnitName = "Sak",
            secondaryUnitFactor = BigDecimal("50"),
            isSecondaryUnit = false
        )

        val secondaryItem = CartItem(
            productId = "prod-cement",
            productName = "Semen Padang",
            unitPrice = BigDecimal("75000"),
            quantity = 2,
            unitName = "Sak",
            cartItemId = "prod-cement__sec",
            baseUnitPrice = BigDecimal("1500"),
            baseUnitName = "Kg",
            secondaryUnitId = "unit-sak",
            secondaryUnitName = "Sak",
            secondaryUnitFactor = BigDecimal("50"),
            isSecondaryUnit = true
        )

        assertTrue(baseItem.hasSecondaryUnit)
        assertFalse(baseItem.isSecondaryUnit)
        assertEquals(5.0, baseItem.equivalentBaseQuantity, 0.001)
        assertEquals(0, BigDecimal("5").compareTo(baseItem.equivalentBaseQuantityBigDecimal))

        assertTrue(secondaryItem.hasSecondaryUnit)
        assertTrue(secondaryItem.isSecondaryUnit)
        // 2 Sak * 50 = 100 Kg base quantity
        assertEquals(100.0, secondaryItem.equivalentBaseQuantity, 0.001)
        assertEquals(0, BigDecimal("100").compareTo(secondaryItem.equivalentBaseQuantityBigDecimal))
    }

    @Test
    fun `estimateCheckoutTotals calculates correct gross with mixed multi-unit items`() {
        // Line 1: 2 Sak Semen @ Rp 75.000 = Rp 150.000
        val line1 = CartItem(
            productId = "prod-cement",
            productName = "Semen Padang",
            unitPrice = BigDecimal("75000"),
            quantity = 2,
            unitName = "Sak",
            cartItemId = "prod-cement__sec",
            baseUnitPrice = BigDecimal("1500"),
            baseUnitName = "Kg",
            secondaryUnitFactor = BigDecimal("50"),
            isSecondaryUnit = true
        )

        // Line 2: 5 Kg Semen Eceran @ Rp 1.500 = Rp 7.500
        val line2 = CartItem(
            productId = "prod-cement",
            productName = "Semen Padang",
            unitPrice = BigDecimal("1500"),
            quantity = 5,
            unitName = "Kg",
            cartItemId = "prod-cement",
            baseUnitPrice = BigDecimal("1500"),
            baseUnitName = "Kg",
            secondaryUnitFactor = BigDecimal("50"),
            isSecondaryUnit = false
        )

        val totals = estimateCheckoutTotals(listOf(line1, line2), null)

        // Gross = 150,000 + 7,500 = 157,500.00
        assertEquals(0, BigDecimal("157500.00").compareTo(totals.grossSubtotal))
        assertEquals(0, BigDecimal("157500.00").compareTo(totals.netTotal))
        assertEquals(0, BigDecimal.ZERO.compareTo(totals.totalDiscountAmount))
    }

    @Test
    fun `localCheckoutItemCommand carries baseQuantity and conversionFactor`() {
        val command = LocalCheckoutItemCommand(
            productLocalId = 101L,
            productServerId = "prod-cement",
            productNameSnapshot = "Semen Padang",
            skuSnapshot = "CEM-50",
            unitNameSnapshot = "Sak",
            quantity = 2.0,
            baseQuantity = 100.0,
            conversionFactor = 50.0,
            priceAtTransaction = 75000.0,
            cogsAtTransaction = 60000.0,
            discount = 0.0,
            subtotal = 150000.0
        )

        assertEquals(2.0, command.quantity, 0.001)
        assertEquals(100.0, command.baseQuantity, 0.001)
        assertEquals(50.0, command.conversionFactor, 0.001)
        assertEquals(75000.0, command.priceAtTransaction, 0.001)
    }
}
