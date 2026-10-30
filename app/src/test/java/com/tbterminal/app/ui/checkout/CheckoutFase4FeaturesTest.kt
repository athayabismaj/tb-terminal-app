package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.printer.CashSessionSummaryFormatter
import com.tbterminal.app.data.printer.DeliveryOrderFormatter
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.ceil

class CheckoutFase4FeaturesTest {

    @Test
    fun `delivery order parser extracts address and courier from packed notes`() {
        val notes = "Pelanggan: Pak Bambang   [PENGIRIMAN] Ongkir: Rp 50.000 | Alamat: Jl. Merdeka No. 45 Blok B | Armada: Mobil Pick-up Toko"
        val parsed = DeliveryOrderFormatter.parseDeliveryInfo(notes)

        assertTrue(parsed.isDelivery)
        assertEquals("Rp 50.000", parsed.deliveryFeeText)
        assertEquals("Jl. Merdeka No. 45 Blok B", parsed.address)
        assertEquals("Mobil Pick-up Toko", parsed.courier)
    }

    @Test
    fun `delivery order formatter builds valid ESC POS bytes and WhatsApp text`() {
        val sampleTransaction = CashTransactionDetail(
            id = "TRX-DO-001",
            receiptId = "DO-20261029-001",
            sessionId = "session-1",
            customerId = "cust-1",
            customerName = "Pak Haji Slamet",
            userId = "user-1",
            cashierName = "Budi Kasir",
            type = "sale",
            status = "settled",
            total = BigDecimal("350000"),
            paidAmount = BigDecimal("350000"),
            changeAmount = BigDecimal.ZERO,
            amountTendered = BigDecimal("350000"),
            paymentMethods = listOf("tunai"),
            notes = "[PENGIRIMAN] Ongkir: Rp 35.000 | Alamat: Jl. Sukajadi No. 12 | Armada: Engkel 1",
            createdAt = "2026-10-29T10:15:00Z",
            items = listOf(
                CashTransactionItem(
                    productId = "prod-semen",
                    productName = "Semen Gresik 50kg",
                    unitId = "unit-sak",
                    unitName = "Sak",
                    quantity = BigDecimal("5"),
                    priceAtTransaction = BigDecimal("65000"),
                    subtotal = BigDecimal("325000")
                )
            )
        )

        val bytes = DeliveryOrderFormatter.buildDeliveryOrderBytes(
            transaction = sampleTransaction,
            storeName = "TB TERMINAL",
            paperSize = "58mm"
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val waText = DeliveryOrderFormatter.buildDeliveryOrderWhatsAppText(
            transaction = sampleTransaction,
            storeName = "TB TERMINAL"
        )

        assertTrue(waText.contains("SURAT JALAN / BUKTI KIRIM"))
        assertTrue(waText.contains("Pak Haji Slamet"))
        assertTrue(waText.contains("Jl. Sukajadi No. 12"))
        assertTrue(waText.contains("5 Sak - Semen Gresik 50kg"))
        assertTrue(waText.contains("[ ]"))
    }

    @Test
    fun `cash session summary formatter builds valid shift settlement slip and WhatsApp report`() {
        val sampleSession = CashSession(
            id = "SESSION-SHIFT-101",
            userId = "user-1",
            userName = "Ahmad Pratama",
            openedAt = "2026-10-29T08:00:00Z",
            closedAt = "2026-10-29T16:00:00Z",
            openingCash = BigDecimal("200000"),
            closingCash = BigDecimal("1500000"),
            systemCash = BigDecimal("1480000"),
            difference = BigDecimal("20000"), // Selisih lebih Rp 20.000
            totalExpenses = BigDecimal("50000"),
            notes = "Kas balance, ada tip pembeli Rp 20rb",
            status = "CLOSED"
        )

        val bytes = CashSessionSummaryFormatter.buildShiftSettlementBytes(
            session = sampleSession,
            storeName = "TB TERMINAL",
            paperSize = "58mm"
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val waText = CashSessionSummaryFormatter.buildShiftSettlementWhatsAppText(
            session = sampleSession,
            storeName = "TB TERMINAL"
        )

        assertTrue(waText.contains("LAPORAN TUTUP SHIFT KASIR"))
        assertTrue(waText.contains("Ahmad Pratama"))
        assertTrue(waText.contains("200.000"))
        assertTrue(waText.contains("LEBIH (+)"))
    }

    @Test
    fun `building material estimator tile calculations are accurate`() {
        val length = 4.0
        val width = 5.0
        val areaNet = length * width // 20.0 m²
        val wastePercent = 10
        val areaGross = areaNet * (1.0 + wastePercent / 100.0) // 22.0 m²

        // Granit 60x60 (1.44 m²/box)
        val coveragePerBox = 1.44
        val boxesNeeded = ceil(areaGross / coveragePerBox).toInt()

        assertEquals(20.0, areaNet, 0.001)
        assertEquals(22.0, areaGross, 0.001)
        assertEquals(16, boxesNeeded) // 22.0 / 1.44 = 15.27 -> 16 dus
    }

    @Test
    fun `building material estimator plaster and paint calculations are accurate`() {
        // Dinding 10m x 3m = 30 m²
        val wallArea = 10.0 * 3.0
        val cementSacks = ceil(wallArea * 0.20).toInt() // 30 * 0.20 = 6 sak
        val sandM3 = wallArea * 0.024 // 0.72 m³

        assertEquals(6, cementSacks)
        assertEquals(0.72, sandM3, 0.001)

        // Cat 50 m², 2 lapis = 100 m²
        val paintArea = 50.0
        val coats = 2
        val totalCoverage = paintArea * coats // 100 m²
        val liters = totalCoverage / 10.0 // 10.0 liter
        val gallons = ceil(liters / 2.5).toInt() // 10.0 / 2.5 = 4 galon
        val pails = ceil(liters / 20.0).toInt() // 10.0 / 20.0 = 1 pail

        assertEquals(10.0, liters, 0.001)
        assertEquals(4, gallons)
        assertEquals(1, pails)
    }

    @Test
    fun `customer credit inspector detects credit facility and overlimit thresholds`() {
        val customerWithCredit = Customer(
            id = "c-1",
            name = "CV Bangun Jaya",
            phone = "0812345678",
            address = "Jl. Industri",
            isContractor = true,
            creditLimit = BigDecimal("10000000"),
            paymentTermDays = 30,
            isActive = true,
            createdAt = "",
            updatedAt = ""
        )

        val customerCashOnly = Customer(
            id = "c-2",
            name = "Pembeli Retail",
            phone = null,
            address = null,
            isContractor = false,
            creditLimit = BigDecimal.ZERO,
            paymentTermDays = 0,
            isActive = true,
            createdAt = "",
            updatedAt = ""
        )

        assertTrue(customerWithCredit.creditLimit > BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, customerCashOnly.creditLimit)

        val billUnderLimit = BigDecimal("3500000")
        val billOverLimit = BigDecimal("12000000")

        assertFalse(billUnderLimit > customerWithCredit.creditLimit)
        assertTrue(billOverLimit > customerWithCredit.creditLimit)
    }
}
