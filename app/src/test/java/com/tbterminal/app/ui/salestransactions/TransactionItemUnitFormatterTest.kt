package com.tbterminal.app.ui.salestransactions

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.printer.DeliveryOrderFormatter
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionItemUnitFormatterTest {
    @Test
    fun shareTextAndWhatsAppText_showServerUnitNameWhenPresentAndLegacyFormatWhenNull() {
        val trxWithUnit = sampleTransaction(
            items = listOf(
                CashTransactionItem(
                    productId = "p1",
                    productName = "Paku 5 cm",
                    unitId = "u1",
                    unitName = "Pcs",
                    quantity = BigDecimal("24.00"),
                    priceAtTransaction = BigDecimal("10000"),
                    subtotal = BigDecimal("240000"),
                )
            )
        )

        val shareWithUnit = buildReceiptShareText(trxWithUnit)
        assertTrue(shareWithUnit.contains("  24 Pcs x "))

        val waWithUnit = WhatsAppReceiptHelper.buildWhatsAppReceiptText(trxWithUnit)
        assertTrue(waWithUnit.contains("  24 Pcs x "))

        val doWithUnit = DeliveryOrderFormatter.buildDeliveryOrderWhatsAppText(trxWithUnit)
        assertTrue(doWithUnit.contains("1. [ ] 24 Pcs - Paku 5 cm"))

        val trxLegacyNull = sampleTransaction(
            items = listOf(
                CashTransactionItem(
                    productId = "p1",
                    productName = "Paku 5 cm",
                    unitId = "u1",
                    unitName = null,
                    quantity = BigDecimal("24.00"),
                    priceAtTransaction = BigDecimal("10000"),
                    subtotal = BigDecimal("240000"),
                )
            )
        )

        val shareLegacy = buildReceiptShareText(trxLegacyNull)
        assertTrue(shareLegacy.contains("  24 x "))
        assertFalse(shareLegacy.contains("  24 pcs"))

        val waLegacy = WhatsAppReceiptHelper.buildWhatsAppReceiptText(trxLegacyNull)
        assertTrue(waLegacy.contains("  24 x "))

        val doLegacy = DeliveryOrderFormatter.buildDeliveryOrderWhatsAppText(trxLegacyNull)
        assertTrue(doLegacy.contains("1. [ ] 24 pcs - Paku 5 cm"))
    }

    @Test
    fun summaryUnitLabelAndInvoiceUnitLabel_respectUnitNameAndFallbackOnlyWhenNull() {
        val uniformSak = listOf(
            CashTransactionItem("p1", "Semen", "u1", "Sak", BigDecimal("2"), BigDecimal("65000"), BigDecimal("130000")),
            CashTransactionItem("p2", "Mortar", "u1", "Sak", BigDecimal("3"), BigDecimal("50000"), BigDecimal("150000")),
        )
        assertEquals("Sak", resolveSummaryTotalUnitLabel(uniformSak))

        val mixedUnits = listOf(
            CashTransactionItem("p1", "Semen", "u1", "Sak", BigDecimal("2"), BigDecimal("65000"), BigDecimal("130000")),
            CashTransactionItem("p2", "Paku", "u2", "Kg", BigDecimal("3"), BigDecimal("20000"), BigDecimal("60000")),
        )
        assertEquals("Pcs", resolveSummaryTotalUnitLabel(mixedUnits))

        val nullUnit = listOf(
            CashTransactionItem("p1", "Semen", "u1", null, BigDecimal("2"), BigDecimal("65000"), BigDecimal("130000")),
        )
        assertEquals("Pcs", resolveSummaryTotalUnitLabel(nullUnit))

        assertEquals("Sak", resolveInvoiceUnitLabel("Sak"))
        assertEquals("pcs", resolveInvoiceUnitLabel(null))
        assertEquals("pcs", resolveInvoiceUnitLabel("   "))
        assertEquals("7.5 Kaleng", formatTransactionQtyWithOptionalUnit(BigDecimal("7.50"), "Kaleng"))
        assertEquals("7.5", formatTransactionQtyWithOptionalUnit(BigDecimal("7.50"), null))
    }

    private fun sampleTransaction(items: List<CashTransactionItem>) = CashTransactionDetail(
        id = "trx-1",
        receiptId = "TRX-20261125-001",
        sessionId = "sess-1",
        customerId = null,
        customerName = "Budi",
        cashierName = "Kasir",
        paymentMethods = listOf("tunai"),
        type = "sale",
        status = "settled",
        total = BigDecimal("240000"),
        paidAmount = BigDecimal("240000"),
        amountTendered = BigDecimal("240000"),
        changeAmount = BigDecimal.ZERO,
        createdAt = "2026-11-25T10:00:00Z",
        items = items,
    )
}
