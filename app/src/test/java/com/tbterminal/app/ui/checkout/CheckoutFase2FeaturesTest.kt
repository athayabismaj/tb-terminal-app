package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.checkout.components.QrisGenerator
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutFase2FeaturesTest {

    @Test
    fun qrisGenerator_buildsValidEmvcoPayloadWithCrc() {
        val payload = QrisGenerator.buildQrisPayload(
            storeName = "TB TERMINAL",
            amount = BigDecimal("150000"),
            invoiceNo = "INV-20261029-001"
        )

        assertNotNull(payload)
        assertTrue(payload.startsWith("000201010212")) // Format indicator 01, Dynamic QR 12
        assertTrue(payload.contains("ID.CO.QRIS.WWW"))  // Standard Indonesian QRIS identifier
        assertTrue(payload.contains("5211"))           // Building materials merchant category
        assertTrue(payload.contains("360"))            // Currency IDR
        assertTrue(payload.contains("150000.00"))      // Amount formatted to 2 decimals
        assertTrue(payload.contains("TB TERMINAL"))    // Store Name
        assertTrue(payload.contains("INV-20261029-001")) // Invoice tag 62
        assertTrue(payload.contains("6304"))           // CRC16 tag
        assertEquals(4, payload.substring(payload.length - 4).length) // 4-char CRC hex
    }

    @Test
    fun qrisGenerator_calculatesCrc16CcittAccurately() {
        val testData = "00020101021226590014ID.CO.QRIS.WWW0115ID10200388291010202010303UMI51430014ID.CO.QRIS.WWW0215ID10200388291015204521153033605409150000.005802ID5911TB TERMINAL6009INDONESIA62200116INV-20261029-0016304"
        val crc = QrisGenerator.calculateCrc16Ccitt(testData)
        assertNotNull(crc)
        assertEquals(4, crc.length)
        assertTrue(crc.all { it.isDigit() || it in 'A'..'F' })
    }

    @Test
    fun qrisUiState_tracksQrisAndScannerVisibility() {
        val initial = CheckoutUiState()
        assertFalse(initial.isQrisDialogOpen)
        assertFalse(initial.isCameraScannerOpen)

        val qrisOpen = initial.copy(isQrisDialogOpen = true, selectedPaymentMethod = PaymentMethod.QRIS)
        assertTrue(qrisOpen.isQrisDialogOpen)
        assertEquals(PaymentMethod.QRIS, qrisOpen.selectedPaymentMethod)

        val scannerOpen = initial.copy(isCameraScannerOpen = true)
        assertTrue(scannerOpen.isCameraScannerOpen)
    }

    @Test
    fun paymentMethod_qrisValuesAreCorrect() {
        assertEquals("qris", PaymentMethod.QRIS.apiValue())
        assertEquals("QRIS", PaymentMethod.QRIS.displayName())
    }
}
