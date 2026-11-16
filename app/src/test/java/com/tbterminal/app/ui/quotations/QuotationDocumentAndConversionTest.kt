package com.tbterminal.app.ui.quotations

import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.model.QuotationDetail
import com.tbterminal.app.data.model.QuotationItemDetail
import com.tbterminal.app.data.model.QuotationStatus
import com.tbterminal.app.ui.checkout.PosReorderBridge
import com.tbterminal.app.ui.checkout.Product
import com.tbterminal.app.ui.checkout.ReorderResolvedProduct
import com.tbterminal.app.ui.checkout.buildCheckoutReorderPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class QuotationDocumentAndConversionTest {

    private fun sampleDetail() = QuotationDetail(
        id = "q-1",
        quotationNumber = "RAB-20261112-AB12",
        userId = "u-1",
        cashierName = "Kasir Andi",
        customerId = "c-1",
        customerName = "CV Karya Bangunan",
        status = QuotationStatus.DRAFT,
        validUntil = "2026-11-26T10:00:00+07:00",
        notes = "Penawaran proyek ruko",
        grossSubtotal = BigDecimal("200000"),
        itemDiscountTotal = BigDecimal("10000"),
        transactionDiscountAmount = BigDecimal.ZERO,
        totalDiscountAmount = BigDecimal("10000"),
        taxEnabled = true,
        taxMode = "EXCLUDE",
        taxRate = BigDecimal("11"),
        taxLabel = "PPN",
        taxableBase = BigDecimal("190000"),
        taxAmount = BigDecimal("20900"),
        total = BigDecimal("210900"),
        createdAt = "2026-11-12T10:00:00+07:00",
        updatedAt = "2026-11-12T10:00:00+07:00",
        items = listOf(
            QuotationItemDetail(
                id = "qi-1",
                productId = "p-1",
                productName = "Besi Beton 10mm",
                unitId = "u-batang",
                unitName = "btg",
                quantity = BigDecimal("2"),
                unitPrice = BigDecimal("100000"),
                discountAmount = BigDecimal("10000"),
                subtotal = BigDecimal("190000"),
                note = "Potong 4 meter",
            )
        ),
    )

    @Test
    fun `buildQuotationShareText renders RAB header, items, notes, tax, and disclaimer`() {
        val text = buildQuotationShareText(sampleDetail(), storeName = "TB MAJU")

        assertTrue(text.contains("TB MAJU"))
        assertTrue(text.contains("PENAWARAN HARGA (RAB)"))
        assertTrue(text.contains("RAB-20261112-AB12"))
        assertTrue(text.contains("Besi Beton 10mm"))
        assertTrue(text.contains("Potong 4 meter"))
        assertTrue(text.contains("PPN 11%"))
        assertTrue(text.contains("bukan nota lunas"))
    }

    @Test
    fun `QuotationStatus fromApi maps known statuses case-insensitively`() {
        assertEquals(QuotationStatus.SENT, QuotationStatus.fromApi("sent"))
        assertEquals(QuotationStatus.CONVERTED, QuotationStatus.fromApi("CONVERTED"))
        assertEquals(QuotationStatus.CANCELLED, QuotationStatus.fromApi("cancelled"))
        assertEquals(QuotationStatus.DRAFT, QuotationStatus.fromApi("unknown"))
    }

    @Test
    fun `converting quotation via PosReorderBridge and reorder planner preserves notes and validates stock`() {
        val detail = sampleDetail()
        val bridge = PosReorderBridge()
        bridge.enqueueQuotation(detail)
        val consumed = bridge.consumeQuotation()
        assertEquals("q-1", consumed?.id)
        assertNull(bridge.consumeQuotation())

        val mapped = detail.items.map { item ->
            CashTransactionItem(
                productId = item.productId,
                productName = item.productName,
                unitId = item.unitId,
                unitName = item.unitName,
                quantity = item.quantity,
                priceAtTransaction = item.unitPrice,
                subtotal = item.subtotal,
                note = item.note,
            )
        }
        val plan = buildCheckoutReorderPlan(
            transactionItems = mapped,
            resolvedProducts = mapOf(
                "p-1" to ReorderResolvedProduct(
                    product = Product(
                        productId = "p-1",
                        name = "Besi Beton 10mm",
                        unitPrice = BigDecimal("105000"),
                        unitName = "btg",
                        stockQty = BigDecimal("15"),
                    ),
                    isActive = true,
                )
            ),
        )

        assertEquals(1, plan.items.size)
        assertEquals(2, plan.items[0].quantity)
        assertEquals("Potong 4 meter", plan.items[0].note)
        assertEquals(0, BigDecimal("105000").compareTo(plan.items[0].unitPrice))
    }
}
