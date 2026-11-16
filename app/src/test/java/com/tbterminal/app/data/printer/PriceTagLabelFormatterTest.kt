package com.tbterminal.app.data.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PriceTagLabelFormatterTest {

    private fun ByteArray.indexOfSequence(seq: ByteArray): Int {
        outer@ for (i in 0..size - seq.size) {
            for (j in seq.indices) {
                if (this[i + j] != seq[j]) continue@outer
            }
            return i
        }
        return -1
    }

    @Test
    fun `barcode emits GS k Code128 Set B command for alphanumeric SKU`() {
        val bytes = EscPosBuilder("58mm")
            .barcode("SMN-TR-50KG")
            .build()

        // GS h 60 -> 1D 68 3C, GS w 2 -> 1D 77 02, GS H 2 -> 1D 48 02
        assertTrue(bytes.indexOfSequence(byteArrayOf(0x1D, 0x68, 60)) >= 0)
        assertTrue(bytes.indexOfSequence(byteArrayOf(0x1D, 0x77, 2)) >= 0)
        assertTrue(bytes.indexOfSequence(byteArrayOf(0x1D, 0x48, 2)) >= 0)

        // GS k 73 n {BSMN-TR-50KG (length 13)
        val payload = "{BSMN-TR-50KG".toByteArray(Charsets.US_ASCII)
        val header = byteArrayOf(0x1D, 0x6B, 73, payload.size.toByte()) + payload
        assertTrue(bytes.indexOfSequence(header) >= 0)
    }

    @Test
    fun `barcode AUTO selects EAN13 for valid checksum 13 digits and falls back to Code128 otherwise`() {
        assertTrue(EscPosBuilder.isValidEan13Input("8991002101012"))
        assertFalse(EscPosBuilder.isValidEan13Input("8991002101019"))

        val eanBytes = EscPosBuilder("58mm")
            .barcode("8991002101012", symbology = EscPosBuilder.BarcodeSymbology.AUTO)
            .build()
        val eanHeader = byteArrayOf(0x1D, 0x6B, 67, 13) + "8991002101012".toByteArray(Charsets.US_ASCII)
        assertTrue(eanBytes.indexOfSequence(eanHeader) >= 0)

        val fallbackBytes = EscPosBuilder("58mm")
            .barcode("8991002101019", symbology = EscPosBuilder.BarcodeSymbology.AUTO)
            .build()
        val code128Header = byteArrayOf(0x1D, 0x6B, 73, 15) + "{B8991002101019".toByteArray(Charsets.US_ASCII)
        assertTrue(fallbackBytes.indexOfSequence(code128Header) >= 0)
    }

    @Test
    fun `buildBatchPriceTagBytes includes every label copy with graphic barcode and cut commands`() {
        val bytes = PriceTagLabelFormatter.buildBatchPriceTagBytes(
            items = listOf(
                PriceTagItem(
                    productId = "p1",
                    productName = "Semen 50kg",
                    sku = "SMN-50",
                    price = BigDecimal("65000"),
                    unitName = "Sak",
                    copies = 2,
                ),
                PriceTagItem(
                    productId = "p2",
                    productName = "Cat Tembok",
                    sku = "CAT-01",
                    price = BigDecimal("125000"),
                    unitName = "Pail",
                    copies = 1,
                ),
            ),
            storeName = "TB MAJU",
            paperSize = "58mm",
        )

        val text = String(bytes, Charsets.ISO_8859_1)
        assertEquals(2, Regex("SEMEN 50KG").findAll(text).count())
        assertEquals(1, Regex("CAT TEMBOK").findAll(text).count())
        assertTrue(bytes.indexOfSequence(byteArrayOf(0x1D, 0x6B, 73)) >= 0)
        // 3 paper partial cuts (2 + 1 copies)
        var cutCount = 0
        for (i in 0..bytes.size - 3) {
            if (bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x56.toByte() && bytes[i + 2] == 0x01.toByte()) {
                cutCount++
            }
        }
        assertEquals(3, cutCount)
    }
}
