package com.tbterminal.app.ui.barcode

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent

/**
 * Pelacak input hardware barcode scanner (USB / Bluetooth HID Keyboard Emulation).
 *
 * Scanner fisik mengirimkan karakter barcode secara beruntun dengan interval sangat cepat
 * (< 100ms per karakter) dan diakhiri dengan tombol ENTER (KEYCODE_ENTER).
 */
class HardwareBarcodeScannerTracker(
    private val maxDelayBetweenKeysMs: Long = 100L,
    private val minBarcodeLength: Int = 2,
    private val onBarcodeScanned: (String) -> Unit
) {
    private val buffer = StringBuilder()
    private var lastKeyTimestamp = 0L

    fun processKey(
        action: Int,
        keyCode: Int,
        unicodeChar: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        if (action != KeyEvent.ACTION_DOWN) {
            return false
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (buffer.length >= minBarcodeLength) {
                val scanned = buffer.toString().trim()
                buffer.clear()
                lastKeyTimestamp = 0L
                onBarcodeScanned(scanned)
                return true
            }
            buffer.clear()
            lastKeyTimestamp = 0L
            return false
        }

        val char = unicodeChar.toChar()
        if (char.isLetterOrDigit() || char in "-_./#") {
            if (lastKeyTimestamp != 0L && (timestamp - lastKeyTimestamp) > maxDelayBetweenKeysMs) {
                // Jeda terlalu lama (misal pengetikan manual manusia), reset buffer
                buffer.clear()
            }
            buffer.append(char)
            lastKeyTimestamp = timestamp
            return false
        }

        return false
    }

    fun clear() {
        buffer.clear()
        lastKeyTimestamp = 0L
    }

    val currentBuffer: String
        get() = buffer.toString()
}

/**
 * Modifier Composable untuk menangkap input scanner barcode fisik secara global di layar kasir.
 */
@Composable
fun Modifier.hardwareBarcodeScanner(
    enabled: Boolean = true,
    onBarcodeScanned: (String) -> Unit
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val tracker = remember(onBarcodeScanned) {
        HardwareBarcodeScannerTracker(onBarcodeScanned = onBarcodeScanned)
    }

    LaunchedEffect(enabled) {
        if (enabled) {
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {
            }
        }
    }

    return this
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { event ->
            if (!enabled) return@onKeyEvent false
            tracker.processKey(
                action = event.nativeKeyEvent.action,
                keyCode = event.nativeKeyEvent.keyCode,
                unicodeChar = event.nativeKeyEvent.unicodeChar,
                timestamp = System.currentTimeMillis()
            )
        }
}
