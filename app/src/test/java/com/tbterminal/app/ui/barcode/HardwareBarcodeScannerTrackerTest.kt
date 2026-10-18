package com.tbterminal.app.ui.barcode

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareBarcodeScannerTrackerTest {

    @Test
    fun processKey_whenFastKeySequenceWithEnter_emitsScannedBarcode() {
        var scannedBarcode: String? = null
        val tracker = HardwareBarcodeScannerTracker(
            maxDelayBetweenKeysMs = 100L,
            minBarcodeLength = 3,
            onBarcodeScanned = { scannedBarcode = it }
        )

        var time = 1000L
        val barcodeInput = "8991234567890"

        // Simulasi burst keystrokes scanner fisik (interval 10ms per tombol)
        for (char in barcodeInput) {
            val handled = tracker.processKey(
                action = KeyEvent.ACTION_DOWN,
                keyCode = KeyEvent.KEYCODE_0,
                unicodeChar = char.code,
                timestamp = time
            )
            assertFalse(handled)
            time += 10L
        }

        // Tombol Enter penutup dari barcode gun
        val enterHandled = tracker.processKey(
            action = KeyEvent.ACTION_DOWN,
            keyCode = KeyEvent.KEYCODE_ENTER,
            unicodeChar = '\n'.code,
            timestamp = time + 10L
        )

        assertTrue(enterHandled)
        assertEquals("8991234567890", scannedBarcode)
        assertEquals("", tracker.currentBuffer)
    }

    @Test
    fun processKey_withSpecialCharsLikeSKU_emitsCorrectBarcode() {
        var scannedBarcode: String? = null
        val tracker = HardwareBarcodeScannerTracker(
            maxDelayBetweenKeysMs = 100L,
            minBarcodeLength = 3,
            onBarcodeScanned = { scannedBarcode = it }
        )

        var time = 2000L
        val sku = "GAL-144978"

        for (char in sku) {
            tracker.processKey(
                action = KeyEvent.ACTION_DOWN,
                keyCode = 0,
                unicodeChar = char.code,
                timestamp = time
            )
            time += 15L
        }

        val enterHandled = tracker.processKey(
            action = KeyEvent.ACTION_DOWN,
            keyCode = KeyEvent.KEYCODE_ENTER,
            unicodeChar = 10,
            timestamp = time + 10L
        )

        assertTrue(enterHandled)
        assertEquals("GAL-144978", scannedBarcode)
    }

    @Test
    fun processKey_whenTypingTooSlow_resetsBuffer() {
        var scannedBarcode: String? = null
        val tracker = HardwareBarcodeScannerTracker(
            maxDelayBetweenKeysMs = 100L,
            minBarcodeLength = 3,
            onBarcodeScanned = { scannedBarcode = it }
        )

        // Pengetikan lambat oleh manusia (jeda 600ms > maxDelayBetweenKeysMs 100ms)
        tracker.processKey(KeyEvent.ACTION_DOWN, 0, 'A'.code, timestamp = 1000L)
        tracker.processKey(KeyEvent.ACTION_DOWN, 0, 'B'.code, timestamp = 1600L) // reset buffer to 'B'
        tracker.processKey(KeyEvent.ACTION_DOWN, 0, 'C'.code, timestamp = 1610L) // 'BC'
        tracker.processKey(KeyEvent.ACTION_DOWN, 0, 'D'.code, timestamp = 1620L) // 'BCD'

        tracker.processKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 10, timestamp = 1630L)

        // Harus hanya 'BCD', bukan 'ABCD'
        assertEquals("BCD", scannedBarcode)
    }

    @Test
    fun processKey_whenInputTooShort_doesNotEmit() {
        var scannedBarcode: String? = null
        val tracker = HardwareBarcodeScannerTracker(
            maxDelayBetweenKeysMs = 100L,
            minBarcodeLength = 5,
            onBarcodeScanned = { scannedBarcode = it }
        )

        // Hanya 2 karakter
        tracker.processKey(KeyEvent.ACTION_DOWN, 0, '1'.code, timestamp = 1000L)
        tracker.processKey(KeyEvent.ACTION_DOWN, 0, '2'.code, timestamp = 1010L)
        val enterHandled = tracker.processKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 10, timestamp = 1020L)

        assertFalse(enterHandled)
        assertEquals(null, scannedBarcode)
    }

    @Test
    fun processKey_ignoresActionUp() {
        var scannedBarcode: String? = null
        val tracker = HardwareBarcodeScannerTracker(
            onBarcodeScanned = { scannedBarcode = it }
        )

        val handled = tracker.processKey(
            action = KeyEvent.ACTION_UP,
            keyCode = KeyEvent.KEYCODE_ENTER,
            unicodeChar = 10,
            timestamp = 1000L
        )

        assertFalse(handled)
        assertEquals(null, scannedBarcode)
    }
}
