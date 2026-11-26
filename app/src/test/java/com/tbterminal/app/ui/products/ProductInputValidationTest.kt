package com.tbterminal.app.ui.products

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class ProductInputValidationTest {
    private val valid = ProductFormInput(
        sku = "sku-01", name = "Semen", categoryId = "category", baseUnitId = "unit",
        priceBuy = "50000", priceRetail = "60000", priceContractor = "58000", minStock = "5"
    )

    @Test
    fun validInputUsesSameNormalizedSkuRuleAsBackend() {
        assertEquals("SKU-01", normalizeProductSku(valid.sku))
        assertNull(validateProductFormInput(valid, isEditMode = false))
    }

    @Test
    fun invalidSkuNegativePriceAndPrecisionAreBlocked() {
        assertNotNull(validateProductFormInput(valid.copy(sku = "SKU 01"), false))
        assertNotNull(validateProductFormInput(valid.copy(priceBuy = "-1"), false))
        assertNotNull(validateProductFormInput(valid.copy(minStock = "1.001"), false))
    }

    @Test
    fun secondaryUnitRequiresDifferentUnitAndValidFactor() {
        val withSecondary = valid.copy(
            usesSecondaryUnit = true,
            secondaryUnitId = "box",
            secondaryUnitFactor = "12"
        )

        assertNull(validateProductFormInput(withSecondary, false))
        assertNotNull(validateProductFormInput(withSecondary.copy(secondaryUnitId = "unit"), false))
        assertNotNull(validateProductFormInput(withSecondary.copy(secondaryUnitFactor = "0"), false))
        assertNotNull(validateProductFormInput(withSecondary.copy(secondaryUnitFactor = "1.00001"), false))
    }

    @Test
    fun idrFormattingEnforcesDecimalBuyPriceAndWholeRupiahSellingPrices() {
        assertNull(
            validateProductFormInput(
                valid.copy(priceBuy = "1.333,33", priceRetail = "60.000", priceContractor = "58000,00"),
                isEditMode = false
            )
        )
        assertEquals("33%", "20.000".marginText("15.000"))

        assertNotNull(validateProductFormInput(valid.copy(priceBuy = "1333.33"), false))
        assertNotNull(validateProductFormInput(valid.copy(priceBuy = "1.333,333"), false))
        assertNotNull(validateProductFormInput(valid.copy(priceRetail = "60000,5"), false))
        assertNotNull(validateProductFormInput(valid.copy(priceContractor = "58000.50"), false))
    }
}
