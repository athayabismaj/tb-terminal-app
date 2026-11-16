package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.ProductPriceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CheckoutTieredPricingTest {

    private val sampleTiers = listOf(
        ProductPriceTier(minQty = BigDecimal("10"), unitPrice = BigDecimal("61000")),
        ProductPriceTier(minQty = BigDecimal("50"), unitPrice = BigDecimal("58000")),
    )

    @Test
    fun `repriceCartItemsForCheckout steps up and down with quantity`() {
        val baseItem = CartItem(
            productId = "p-semen",
            productName = "Semen 50kg",
            unitPrice = BigDecimal("63000"),
            quantity = 9,
            unitName = "Sak",
            baseUnitPrice = BigDecimal("63000"),
            baseUnitName = "Sak",
            retailBaseUnitPrice = BigDecimal("63000"),
            contractorBaseUnitPrice = BigDecimal("57000"),
            priceTiers = sampleTiers,
        )

        val underTier = repriceCartItemsForCheckout(listOf(baseItem), isContractorCustomer = false).single()
        assertEquals(BigDecimal("63000"), underTier.unitPrice)
        assertNull(underTier.appliedPriceTierMinQty)
        assertFalse(underTier.isContractorPriceApplied)

        val tier10 = repriceCartItemsForCheckout(
            listOf(baseItem.copy(quantity = 10)),
            isContractorCustomer = false,
        ).single()
        assertEquals(BigDecimal("61000"), tier10.unitPrice)
        assertEquals(BigDecimal("10"), tier10.appliedPriceTierMinQty)

        val tier50 = repriceCartItemsForCheckout(
            listOf(baseItem.copy(quantity = 60)),
            isContractorCustomer = false,
        ).single()
        assertEquals(BigDecimal("58000"), tier50.unitPrice)
        assertEquals(BigDecimal("50"), tier50.appliedPriceTierMinQty)

        // Step back down to qty 5 -> reverts to retail price
        val reverted = repriceCartItemsForCheckout(
            listOf(tier50.copy(quantity = 5)),
            isContractorCustomer = false,
        ).single()
        assertEquals(BigDecimal("63000"), reverted.unitPrice)
        assertNull(reverted.appliedPriceTierMinQty)
    }

    @Test
    fun `secondary unit multiplies effective base qty and scales tiered display price`() {
        val item = CartItem(
            productId = "p-keramik",
            productName = "Keramik",
            unitPrice = BigDecimal("630000"),
            quantity = 1,
            unitName = "Palet",
            baseUnitPrice = BigDecimal("63000"),
            baseUnitName = "Dus",
            secondaryUnitId = "u-palet",
            secondaryUnitName = "Palet",
            secondaryUnitFactor = BigDecimal("10"),
            isSecondaryUnit = true,
            retailBaseUnitPrice = BigDecimal("63000"),
            contractorBaseUnitPrice = BigDecimal.ZERO,
            priceTiers = sampleTiers,
        )

        val repriced = repriceCartItemsForCheckout(listOf(item), isContractorCustomer = false).single()
        assertEquals(BigDecimal("61000"), repriced.baseUnitPrice)
        assertEquals(BigDecimal("610000"), repriced.unitPrice)
        assertEquals(BigDecimal("10"), repriced.appliedPriceTierMinQty)
    }

    @Test
    fun `contractor customer prioritizes positive contractor price over tiers`() {
        val item = CartItem(
            productId = "p-semen",
            productName = "Semen 50kg",
            unitPrice = BigDecimal("63000"),
            quantity = 20,
            unitName = "Sak",
            baseUnitPrice = BigDecimal("63000"),
            baseUnitName = "Sak",
            retailBaseUnitPrice = BigDecimal("63000"),
            contractorBaseUnitPrice = BigDecimal("57000"),
            priceTiers = sampleTiers,
        )

        val contractorRepriced = repriceCartItemsForCheckout(listOf(item), isContractorCustomer = true).single()
        assertEquals(BigDecimal("57000"), contractorRepriced.unitPrice)
        assertTrue(contractorRepriced.isContractorPriceApplied)
        assertNull(contractorRepriced.appliedPriceTierMinQty)

        // Zero contractor price falls back to wholesale tier
        val zeroContractorItem = item.copy(contractorBaseUnitPrice = BigDecimal.ZERO)
        val tierFallback = repriceCartItemsForCheckout(listOf(zeroContractorItem), isContractorCustomer = true).single()
        assertEquals(BigDecimal("61000"), tierFallback.unitPrice)
        assertFalse(tierFallback.isContractorPriceApplied)
        assertEquals(BigDecimal("10"), tierFallback.appliedPriceTierMinQty)
    }

    @Test
    fun `master product discount scales with qty and secondary unit unless overridden manually`() {
        val item = CartItem(
            productId = "p-cat",
            productName = "Cat Tembok 5kg",
            unitPrice = BigDecimal("120000"),
            quantity = 2,
            unitName = "Kaleng",
            baseUnitPrice = BigDecimal("120000"),
            baseUnitName = "Kaleng",
            secondaryUnitId = "u-dus",
            secondaryUnitName = "Dus",
            secondaryUnitFactor = BigDecimal("4"),
            isSecondaryUnit = false,
            retailBaseUnitPrice = BigDecimal("120000"),
            masterUnitDiscount = BigDecimal("5000"),
        )

        val repriced = repriceCartItemsForCheckout(listOf(item), isContractorCustomer = false).single()
        assertTrue(repriced.isMasterDiscountApplied)
        assertEquals(com.tbterminal.app.data.model.DiscountType.FIXED_AMOUNT, repriced.discountRequest?.type)
        assertEquals(0, BigDecimal("10000").compareTo(repriced.discountRequest?.value))

        // Secondary unit (1 Dus = 4 Kaleng) -> 4 x 5.000 = 20.000
        val secondaryRepriced = repriceCartItemsForCheckout(
            listOf(repriced.copy(quantity = 1, isSecondaryUnit = true)),
            isContractorCustomer = false,
        ).single()
        assertEquals(0, BigDecimal("20000").compareTo(secondaryRepriced.discountRequest?.value))

        // Manual override (e.g. removed or custom discount) stays intact on qty change
        val manualRemoved = secondaryRepriced.copy(
            discountRequest = null,
            hasManualDiscountOverride = true,
            isMasterDiscountApplied = false,
            quantity = 3,
        )
        val afterManual = repriceCartItemsForCheckout(listOf(manualRemoved), isContractorCustomer = false).single()
        assertFalse(afterManual.isMasterDiscountApplied)
        assertNull(afterManual.discountRequest)
    }
}
