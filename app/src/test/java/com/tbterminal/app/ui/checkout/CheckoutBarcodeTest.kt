package com.tbterminal.app.ui.checkout

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CheckoutBarcodeTest {

    @Test
    fun `product matches scanned barcode by sku or barcode field`() {
        val product = Product(
            productId = "prod-galvalum",
            name = "Seng Galvalum",
            unitPrice = BigDecimal("85000"),
            sku = "GAL-144978",
            barcode = "8991234567890",
            unitName = "Lembar",
            stockQty = BigDecimal("100")
        )

        // Cari via SKU
        val matchBySku = listOf(product).firstOrNull {
            it.sku.equals("GAL-144978", ignoreCase = true) || it.barcode.equals("GAL-144978", ignoreCase = true)
        }
        assertNotNull(matchBySku)
        assertEquals("Seng Galvalum", matchBySku?.name)

        // Cari via Barcode angka
        val matchByBarcode = listOf(product).firstOrNull {
            it.sku.equals("8991234567890", ignoreCase = true) || it.barcode.equals("8991234567890", ignoreCase = true)
        }
        assertNotNull(matchByBarcode)
        assertEquals("Seng Galvalum", matchByBarcode?.name)

        // Barcode tidak cocok
        val noMatch = listOf(product).firstOrNull {
            it.sku.equals("UNKNOWN-999", ignoreCase = true) || it.barcode.equals("UNKNOWN-999", ignoreCase = true)
        }
        assertNull(noMatch)
    }

    @Test
    fun `uiState supports scanNotification banner state`() {
        val state = CheckoutUiState(
            scanNotification = "Ditambahkan: Seng Galvalum (+1)"
        )

        assertEquals("Ditambahkan: Seng Galvalum (+1)", state.scanNotification)

        val clearedState = state.copy(scanNotification = null)
        assertNull(clearedState.scanNotification)
    }
}
