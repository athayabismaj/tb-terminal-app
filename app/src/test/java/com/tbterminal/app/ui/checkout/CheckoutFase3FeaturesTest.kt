package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.printer.PriceTagLabelFormatter
import com.tbterminal.app.data.worker.StockAlertWorker
import com.tbterminal.app.ui.salestransactions.WhatsAppReceiptHelper
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutFase3FeaturesTest {

    @Test
    fun whatsAppReceiptHelper_formatsPhoneNumberCorrectly() {
        assertEquals("628123456789", WhatsAppReceiptHelper.formatPhoneNumber("08123456789"))
        assertEquals("628123456789", WhatsAppReceiptHelper.formatPhoneNumber("628123456789"))
        assertEquals("628123456789", WhatsAppReceiptHelper.formatPhoneNumber("+62 812-3456-789"))
        assertEquals("62857123456", WhatsAppReceiptHelper.formatPhoneNumber("0857-123-456"))
    }

    @Test
    fun whatsAppReceiptHelper_buildsStructuredReceiptText() {
        val transaction = CashTransactionDetail(
            id = "trx-101",
            receiptId = "TRX-20261029-001",
            sessionId = "session-1",
            customerId = "cust-1",
            customerName = "Pak Hendra",
            userId = "user-1",
            cashierName = "Budi",
            type = "sale",
            status = "completed",
            total = BigDecimal("215000"),
            paidAmount = BigDecimal("250000"),
            amountTendered = BigDecimal("250000"),
            changeAmount = BigDecimal("35000"),
            paymentMethods = listOf("tunai"),
            items = listOf(
                CashTransactionItem(
                    productId = "prod-1",
                    productName = "Semen Gresik 50kg",
                    unitId = "unit-sak",
                    quantity = BigDecimal("2"),
                    priceAtTransaction = BigDecimal("70000"),
                    subtotal = BigDecimal("140000")
                ),
                CashTransactionItem(
                    productId = "prod-2",
                    productName = "Paku 5cm",
                    unitId = "unit-kg",
                    quantity = BigDecimal("5"),
                    priceAtTransaction = BigDecimal("15000"),
                    subtotal = BigDecimal("75000")
                )
            ),
            createdAt = "2026-10-29T10:30:00Z"
        )

        val receiptText = WhatsAppReceiptHelper.buildWhatsAppReceiptText(
            transaction = transaction,
            storeName = "TB TERMINAL"
        )

        assertNotNull(receiptText)
        assertTrue(receiptText.contains("*TB TERMINAL*"))
        assertTrue(receiptText.contains("TRX-20261029-001"))
        assertTrue(receiptText.contains("Pak Hendra"))
        assertTrue(receiptText.contains("Budi"))
        assertTrue(receiptText.contains("Semen Gresik 50kg"))
        assertTrue(receiptText.contains("Paku 5cm"))
        assertTrue(receiptText.contains("LUNAS"))
        assertTrue(receiptText.contains("Tunai"))
    }

    @Test
    fun priceTagLabelFormatter_buildsEscPosCommands() {
        val bytes = PriceTagLabelFormatter.buildPriceTagBytes(
            productName = "Cat Avian 1kg Putih",
            sku = "CAT-AVI-1KG",
            price = BigDecimal("65000"),
            unitName = "Kaleng",
            storeName = "TB TERMINAL",
            copies = 2
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        // Memeriksa keberadaan byte ESC @ (0x1B, 0x40) untuk inisialisasi printer thermal
        val containsEscAt = bytes.toList().windowed(2).any { it[0] == 0x1B.toByte() && it[1] == 0x40.toByte() }
        assertTrue(containsEscAt)

        // Memeriksa keberadaan byte GS V (0x1D, 0x56) untuk pemotongan kertas
        val containsCutPaper = bytes.toList().windowed(2).any { it[0] == 0x1D.toByte() && it[1] == 0x56.toByte() }
        assertTrue(containsCutPaper)

        // Memeriksa nama produk dalam charset CP437/ASCII
        val textRepresentation = String(bytes, Charsets.ISO_8859_1)
        assertTrue(textRepresentation.contains("CAT AVIAN 1KG PUTIH"))
        assertTrue(textRepresentation.contains("TB TERMINAL"))
    }

    @Test
    fun stockAlertWorker_constantsAreDefined() {
        assertEquals("tbterminal_stock_alerts", StockAlertWorker.CHANNEL_ID)
        assertEquals("Peringatan Stok Kritis", StockAlertWorker.CHANNEL_NAME)
        assertEquals(2001, StockAlertWorker.NOTIFICATION_ID)
        assertEquals("TB_STOCK_ALERT_WORKER", StockAlertWorker.UNIQUE_WORK_NAME)
    }
}
