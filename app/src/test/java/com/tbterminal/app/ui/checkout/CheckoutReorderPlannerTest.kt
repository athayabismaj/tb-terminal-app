package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashTransactionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CheckoutReorderPlannerTest {

    private fun sampleProduct(
        id: String,
        name: String,
        price: String = "50000",
        stock: String = "10",
        unit: String = "sak",
        isActive: Boolean = true,
    ) = ReorderResolvedProduct(
        product = Product(
            productId = id,
            name = name,
            unitPrice = BigDecimal(price),
            sku = "SKU-$id",
            unitName = unit,
            stockQty = BigDecimal(stock),
        ),
        isActive = isActive,
    )

    private fun sampleTxItem(
        productId: String,
        name: String,
        qty: String,
        priceAtTransaction: String = "50000",
        note: String? = null,
    ) = CashTransactionItem(
        productId = productId,
        productName = name,
        unitId = "u1",
        unitName = "sak",
        quantity = BigDecimal(qty),
        priceAtTransaction = BigDecimal(priceAtTransaction),
        subtotal = BigDecimal(priceAtTransaction).multiply(BigDecimal(qty)),
        note = note,
    )

    @Test
    fun `buildCheckoutReorderPlan copies items with notes and uses latest prices`() {
        val plan = buildCheckoutReorderPlan(
            transactionItems = listOf(
                sampleTxItem("p1", "Semen 50kg", "3", priceAtTransaction = "48000", note = "Antar pagi"),
                sampleTxItem("p2", "Pasir 1m3", "1", priceAtTransaction = "250000", note = null),
            ),
            resolvedProducts = mapOf(
                "p1" to sampleProduct("p1", "Semen 50kg", price = "52000", stock = "10"),
                "p2" to sampleProduct("p2", "Pasir 1m3", price = "250000", stock = "5"),
            ),
        )

        assertEquals(2, plan.items.size)
        assertEquals(3, plan.items[0].quantity)
        assertEquals(0, BigDecimal("52000").compareTo(plan.items[0].unitPrice))
        assertEquals("Antar pagi", plan.items[0].note)
        assertEquals(listOf("Semen 50kg"), plan.priceUpdatedItems)
        assertTrue(plan.skippedItems.isEmpty())
        assertTrue(plan.adjustedItems.isEmpty())
        assertTrue(plan.buildFeedbackMessage().contains("2 item disalin ke keranjang"))
    }

    @Test
    fun `buildCheckoutReorderPlan caps quantities to stock and skips empty or inactive products`() {
        val plan = buildCheckoutReorderPlan(
            transactionItems = listOf(
                sampleTxItem("p1", "Cat Tembok", "6"),
                sampleTxItem("p2", "Besi Beton", "2"),
                sampleTxItem("p3", "Pipa PVC", "1"),
                sampleTxItem("p4", "Produk Hilang", "1"),
            ),
            resolvedProducts = mapOf(
                "p1" to sampleProduct("p1", "Cat Tembok", stock = "4"),
                "p2" to sampleProduct("p2", "Besi Beton", stock = "0"),
                "p3" to sampleProduct("p3", "Pipa PVC", stock = "10", isActive = false),
            ),
        )

        assertEquals(1, plan.items.size)
        assertEquals(4, plan.items[0].quantity)
        assertEquals(1, plan.adjustedItems.size)
        assertEquals(3, plan.skippedItems.size)
        val msg = plan.buildFeedbackMessage()
        assertTrue(msg.contains("1 qty disesuaikan stok"))
        assertTrue(msg.contains("3 item dilewati"))
    }

    @Test
    fun `buildCheckoutReorderPlan tracks stock across multiple lines of same product`() {
        val plan = buildCheckoutReorderPlan(
            transactionItems = listOf(
                sampleTxItem("p1", "Kabel NYM", "3", note = "Potong 3m"),
                sampleTxItem("p1", "Kabel NYM", "4", note = "Potong 4m"),
            ),
            resolvedProducts = mapOf(
                "p1" to sampleProduct("p1", "Kabel NYM", stock = "5", unit = "m"),
            ),
        )

        assertEquals(2, plan.items.size)
        assertEquals(3, plan.items[0].quantity)
        assertEquals(2, plan.items[1].quantity)
        assertEquals("Potong 3m", plan.items[0].note)
        assertEquals("Potong 4m", plan.items[1].note)
        assertEquals(1, plan.adjustedItems.size)
    }

    @Test
    fun `mergeReorderItemsIntoCart merges matching notes and caps at available stock`() {
        val resolved = mapOf(
            "p1" to sampleProduct("p1", "Semen", price = "55000", stock = "6"),
        )
        val existing = listOf(
            CartItem(
                productId = "p1",
                productName = "Semen",
                unitPrice = BigDecimal("50000"),
                quantity = 4,
                cartItemId = "p1",
            )
        )
        val incoming = listOf(
            CartItem(
                productId = "p1",
                productName = "Semen",
                unitPrice = BigDecimal("55000"),
                quantity = 5,
                cartItemId = "p1",
            )
        )

        val merged = mergeReorderItemsIntoCart(existing, incoming, resolved)
        assertEquals(1, merged.items.size)
        assertEquals(6, merged.items[0].quantity)
        assertEquals(0, BigDecimal("55000").compareTo(merged.items[0].unitPrice))
        assertEquals(listOf("Semen"), merged.cappedItems)
    }

    @Test
    fun `PosReorderBridge enqueues and atomically consumes transaction once`() {
        val bridge = PosReorderBridge()
        val tx = com.tbterminal.app.data.model.CashTransactionDetail(
            id = "tx-1",
            receiptId = "TRX-001",
            sessionId = "s1",
            customerId = null,
            customerName = null,
            type = "SALE",
            status = "completed",
            total = BigDecimal("50000"),
            paidAmount = BigDecimal("50000"),
            amountTendered = BigDecimal("50000"),
            changeAmount = BigDecimal.ZERO,
            createdAt = "2026-11-12T10:00:00Z",
            items = listOf(sampleTxItem("p1", "Semen", "1")),
        )

        bridge.enqueue(tx)
        assertEquals("tx-1", bridge.pendingReorder.value?.id)
        val firstConsume = bridge.consume()
        assertEquals("tx-1", firstConsume?.id)
        assertEquals(null, bridge.pendingReorder.value)
        assertEquals(null, bridge.consume())
    }
}
