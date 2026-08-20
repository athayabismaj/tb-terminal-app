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
}
