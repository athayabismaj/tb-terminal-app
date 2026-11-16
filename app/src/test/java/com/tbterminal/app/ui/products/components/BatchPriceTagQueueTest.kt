package com.tbterminal.app.ui.products.components

import com.tbterminal.app.data.model.ProductStock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BatchPriceTagQueueTest {

    private fun stock(id: String, name: String, active: Boolean = true) = ProductStock(
        productId = id,
        sku = "SKU-$id",
        productName = name,
        categoryName = "Material",
        unitName = "Pcs",
        quantity = BigDecimal("10"),
        minStock = BigDecimal("2"),
        priceBuy = BigDecimal("10000"),
        priceRetail = BigDecimal("15000"),
        priceContractor = BigDecimal("14000"),
        discount = BigDecimal.ZERO,
        isActive = active,
    )

    @Test
    fun `queue helpers toggle update copies and ignore inactive on selectAll`() {
        val p1 = stock("1", "Paku 5cm", active = true)
        val p2 = stock("2", "Semen 50kg", active = true)
        val p3 = stock("3", "Produk Arsip", active = false)

        var queue = toggleProductInPrintQueue(emptyMap(), p1)
        assertTrue("1" in queue)
        assertEquals(1, totalPrintQueueLabels(queue))

        queue = updatePrintQueueCopies(queue, "1", 5)
        assertEquals(5, totalPrintQueueLabels(queue))

        // Clamps above max 50
        queue = updatePrintQueueCopies(queue, "1", 99)
        assertEquals(50, totalPrintQueueLabels(queue))

        queue = selectAllProductsForPrintQueue(queue, listOf(p1, p2, p3))
        assertTrue("2" in queue)
        assertFalse("3" in queue)
        // Existing custom copy count for p1 preserved
        assertEquals(50, queue["1"]?.copies)
        assertEquals(1, queue["2"]?.copies)
        assertEquals(51, totalPrintQueueLabels(queue))

        val items = toPriceTagItems(queue)
        assertEquals(2, items.size)
    }
}
