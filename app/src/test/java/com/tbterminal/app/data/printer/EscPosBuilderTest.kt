package com.tbterminal.app.data.printer

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class EscPosBuilderTest {

    @Test
    fun testInitCommandGenerated() {
        val bytes = EscPosBuilder("58mm").build()
        // ESC @ -> 0x1B, 0x40
        assertTrue(bytes.size >= 2)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testAlignmentCommands() {
        val left = EscPosBuilder("58mm").align(EscPosBuilder.Alignment.LEFT).build()
        assertEquals(0x1B.toByte(), left[2])
        assertEquals(0x61.toByte(), left[3])
        assertEquals(0x00.toByte(), left[4])

        val center = EscPosBuilder("58mm").align(EscPosBuilder.Alignment.CENTER).build()
        assertEquals(0x1B.toByte(), center[2])
        assertEquals(0x61.toByte(), center[3])
        assertEquals(0x01.toByte(), center[4])

        val right = EscPosBuilder("58mm").align(EscPosBuilder.Alignment.RIGHT).build()
        assertEquals(0x1B.toByte(), right[2])
        assertEquals(0x61.toByte(), right[3])
        assertEquals(0x02.toByte(), right[4])
    }

    @Test
    fun testCutPaperCommand() {
        val partialCut = EscPosBuilder("58mm").cutPaper(fullCut = false).build()
        // GS V 1 -> 0x1D, 0x56, 0x01
        val last3 = partialCut.takeLast(3)
        assertEquals(0x1D.toByte(), last3[0])
        assertEquals(0x56.toByte(), last3[1])
        assertEquals(0x01.toByte(), last3[2])
    }

    @Test
    fun testCashDrawerPulseCommand() {
        val pulse = ReceiptEscPosFormatter.buildCashDrawerPulse()
        assertTrue(pulse.isNotEmpty())
        // Should contain ESC p 0 25 250 -> 0x1B, 0x70
        var foundEscP = false
        for (i in 0 until pulse.size - 1) {
            if (pulse[i] == 0x1B.toByte() && pulse[i + 1] == 0x70.toByte()) {
                foundEscP = true
                break
            }
        }
        assertTrue("Cash drawer pulse ESC p should be present", foundEscP)
    }

    @Test
    fun testBuildReceiptBytesFor58mm() {
        val transaction = CashTransactionDetail(
            id = "tx-123",
            receiptId = "TRX-20261014-001",
            sessionId = "sess-1",
            customerId = null,
            customerName = "Budi Santoso",
            cashierName = "Kasir 1",
            paymentMethods = listOf("TUNAI"),
            type = "PENJUALAN",
            status = "LUNAS",
            total = BigDecimal("150000"),
            paidAmount = BigDecimal("150000"),
            amountTendered = BigDecimal("200000"),
            changeAmount = BigDecimal("50000"),
            createdAt = "2026-10-14T10:30:00Z",
            items = listOf(
                CashTransactionItem(
                    productId = "prod-1",
                    productName = "Semen Tiga Roda 50kg",
                    unitId = "sak",
                    quantity = BigDecimal("2"),
                    priceAtTransaction = BigDecimal("75000"),
                    subtotal = BigDecimal("150000")
                )
            )
        )

        val bytes = ReceiptEscPosFormatter.buildReceiptBytes(
            transaction = transaction,
            storeName = "TB TERMINAL",
            storeAddress = "Jl. Raya No. 123",
            storePhone = "081234567890",
            paperSize = "58mm",
            openDrawer = true,
            autoCut = true
        )

        assertTrue(bytes.isNotEmpty())
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.contains("TB TERMINAL"))
        assertTrue(text.contains("TRX-20261014-001"))
        assertTrue(text.contains("Semen Tiga Roda 50kg"))
        assertTrue(text.contains("TOTAL"))
    }
}
