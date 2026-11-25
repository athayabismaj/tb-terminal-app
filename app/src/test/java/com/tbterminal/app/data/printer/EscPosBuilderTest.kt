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

    @Test
    fun testBuildReceiptBytesShowsUnitNameAndFallbackWhenNull() {
        val withUnit = sampleReceiptTransaction(
            quantity = BigDecimal("7.50"),
            unitName = "Kaleng",
            price = BigDecimal("20000"),
            subtotal = BigDecimal("150000"),
        )
        val textWithUnit = String(
            ReceiptEscPosFormatter.buildReceiptBytes(withUnit, paperSize = "58mm"),
            Charsets.ISO_8859_1,
        )
        assertTrue(textWithUnit.contains("7.5 Kaleng x Rp 20.000"))

        val withNullUnit = sampleReceiptTransaction(
            quantity = BigDecimal("2.00"),
            unitName = null,
            price = BigDecimal("75000"),
            subtotal = BigDecimal("150000"),
        )
        val textNullUnit = String(
            ReceiptEscPosFormatter.buildReceiptBytes(withNullUnit, paperSize = "58mm"),
            Charsets.ISO_8859_1,
        )
        assertTrue(textNullUnit.contains("2 x Rp 75.000"))
    }

    @Test
    fun testReceiptEscPosWrapsLongUnitLinesWithin32And48Columns() {
        val longUnitTx = sampleReceiptTransaction(
            quantity = BigDecimal("7.50"),
            unitName = "Kaleng Super Besar Ekstra 25Kg",
            price = BigDecimal("125000"),
            subtotal = BigDecimal("937500"),
        )

        val lines58 = ReceiptEscPosFormatter.formatItemPriceLines(
            quantity = BigDecimal("7.50"),
            unitName = "Kaleng Super Besar Ekstra 25Kg",
            unitPrice = BigDecimal("125000"),
            subtotal = BigDecimal("937500"),
            maxCharsPerLine = 32,
        )
        assertTrue(lines58.size >= 2)
        lines58.forEach { line ->
            assertTrue("58mm line exceeded 32 chars: '$line' (${line.length})", line.length <= 32)
        }
        assertTrue(lines58.any { it.contains("Rp 125.000") })
        assertTrue(lines58.any { it.contains("Rp 937.500") })

        val lines80 = ReceiptEscPosFormatter.formatItemPriceLines(
            quantity = BigDecimal("7.50"),
            unitName = "Satuan Kemasan Super Panjang Sekali Lebih Dari Batas",
            unitPrice = BigDecimal("125000"),
            subtotal = BigDecimal("937500"),
            maxCharsPerLine = 48,
        )
        assertTrue(lines80.size >= 2)
        lines80.forEach { line ->
            assertTrue("80mm line exceeded 48 chars: '$line' (${line.length})", line.length <= 48)
        }
        assertTrue(lines80.any { it.contains("Rp 125.000") })
        assertTrue(lines80.any { it.contains("Rp 937.500") })

        val bytes58 = ReceiptEscPosFormatter.buildReceiptBytes(longUnitTx, paperSize = "58mm")
        val itemLines58 = extractItemSectionLines(extractPrintableTextLines(bytes58), 32)
        assertTrue(itemLines58.isNotEmpty())
        itemLines58.forEach { line ->
            assertTrue("58mm item line exceeded 32 chars: '$line' (${line.length})", line.length <= 32)
        }

        val bytes80 = ReceiptEscPosFormatter.buildReceiptBytes(longUnitTx, paperSize = "80mm")
        val itemLines80 = extractItemSectionLines(extractPrintableTextLines(bytes80), 48)
        assertTrue(itemLines80.isNotEmpty())
        itemLines80.forEach { line ->
            assertTrue("80mm item line exceeded 48 chars: '$line' (${line.length})", line.length <= 48)
        }

        val doBytes58 = DeliveryOrderFormatter.buildDeliveryOrderBytes(longUnitTx, paperSize = "58mm")
        val doItemLines58 = extractItemSectionLines(extractPrintableTextLines(doBytes58), 32)
        assertTrue(doItemLines58.isNotEmpty())
        doItemLines58.forEach { line ->
            assertTrue("58mm DO item line exceeded 32 chars: '$line' (${line.length})", line.length <= 32)
        }
    }

    private fun extractItemSectionLines(lines: List<String>, maxChars: Int): List<String> {
        val separator = "-".repeat(maxChars)
        val firstIdx = lines.indexOf(separator)
        val secondIdx = if (firstIdx >= 0) {
            lines.drop(firstIdx + 1).indexOf(separator).takeIf { it >= 0 }?.plus(firstIdx + 1) ?: -1
        } else {
            -1
        }
        return if (firstIdx >= 0 && secondIdx > firstIdx) {
            lines.subList(firstIdx + 1, secondIdx)
        } else {
            emptyList()
        }
    }

    private fun sampleReceiptTransaction(
        quantity: BigDecimal,
        unitName: String?,
        price: BigDecimal,
        subtotal: BigDecimal,
    ) = CashTransactionDetail(
        id = "tx-unit-test",
        receiptId = "TRX-20261125-002",
        sessionId = "sess-1",
        customerId = null,
        customerName = "Budi",
        cashierName = "Kasir",
        paymentMethods = listOf("TUNAI"),
        type = "PENJUALAN",
        status = "LUNAS",
        total = subtotal,
        paidAmount = subtotal,
        amountTendered = subtotal,
        changeAmount = BigDecimal.ZERO,
        createdAt = "2026-11-25T10:30:00Z",
        items = listOf(
            CashTransactionItem(
                productId = "prod-1",
                productName = "Cat Besi",
                unitId = "unit-1",
                unitName = unitName,
                quantity = quantity,
                priceAtTransaction = price,
                subtotal = subtotal,
            )
        ),
    )

    private fun extractPrintableTextLines(bytes: ByteArray): List<String> {
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            when (b) {
                0x1B -> { // ESC
                    val cmd = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else -1
                    i += when (cmd) {
                        0x40 -> 2 // ESC @
                        0x70 -> 5 // ESC p m t1 t2
                        else -> 3 // ESC a n, ESC E n, ESC d n
                    }
                }
                0x1D -> { // GS
                    val cmd = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else -1
                    i += when (cmd) {
                        0x6B -> {
                            val len = if (i + 3 < bytes.size) bytes[i + 3].toInt() and 0xFF else 0
                            4 + len
                        }
                        else -> 3 // GS ! n, GS V m, GS h n, etc.
                    }
                }
                0x0A -> {
                    lines += current.toString()
                    current.setLength(0)
                    i++
                }
                else -> {
                    if (b >= 0x20) current.append(b.toChar())
                    i++
                }
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }
}
