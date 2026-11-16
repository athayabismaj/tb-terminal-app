package com.tbterminal.app.data.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosBuilder(val paperSize: String = "58mm") {
    val maxCharsPerLine: Int = if (paperSize.equals("80mm", ignoreCase = true)) 48 else 32
    private val buffer = ByteArrayOutputStream()
    private val charset = Charset.forName("CP437")

    enum class Alignment(val byteValue: Byte) {
        LEFT(0),
        CENTER(1),
        RIGHT(2)
    }

    enum class BarcodeSymbology {
        AUTO,
        CODE128,
        EAN13
    }

    init {
        reset()
    }

    fun reset(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x40)) // ESC @ (Initialize printer)
    }

    fun align(alignment: Alignment): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x61, alignment.byteValue)) // ESC a n
    }

    fun bold(enable: Boolean): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x45, if (enable) 1 else 0)) // ESC E n
    }

    fun doubleSize(enable: Boolean): EscPosBuilder = apply {
        // GS ! n (Bit 4: double height, Bit 5: double width)
        val mode = if (enable) 0x11.toByte() else 0x00.toByte()
        buffer.write(byteArrayOf(0x1D, 0x21, mode))
    }

    fun text(text: String): EscPosBuilder = apply {
        buffer.write(text.toByteArray(charset))
    }

    fun textLine(line: String = ""): EscPosBuilder = apply {
        buffer.write(line.toByteArray(charset))
        buffer.write(0x0A) // LF
    }

    fun lineSeparator(char: Char = '-'): EscPosBuilder = apply {
        val line = char.toString().repeat(maxCharsPerLine)
        textLine(line)
    }

    /**
     * Formats two columns with spacing in between, e.g. "Total" on left and "Rp 50.000" on right.
     * If left text is long, it wraps cleanly.
     */
    fun twoColumn(left: String, right: String): EscPosBuilder = apply {
        if (left.length + right.length + 1 <= maxCharsPerLine) {
            val spaceCount = maxCharsPerLine - left.length - right.length
            textLine(left + " ".repeat(spaceCount.coerceAtLeast(1)) + right)
        } else {
            val maxLeft = maxCharsPerLine - right.length - 1
            if (maxLeft > 0) {
                val truncatedLeft = left.take(maxLeft)
                val spaceCount = maxCharsPerLine - truncatedLeft.length - right.length
                textLine(truncatedLeft + " ".repeat(spaceCount.coerceAtLeast(1)) + right)
            } else {
                textLine(left)
                textLine(" ".repeat((maxCharsPerLine - right.length).coerceAtLeast(0)) + right)
            }
        }
    }

    /**
     * Formats three columns with equal width, e.g. for multiple signatures.
     */
    fun threeColumn(col1: String, col2: String, col3: String): EscPosBuilder = apply {
        val colWidth = maxCharsPerLine / 3
        val f1 = col1.take(colWidth).padEnd(colWidth)
        val f2 = col2.take(colWidth).padEnd(colWidth)
        val f3 = col3.take(colWidth).padEnd(colWidth)
        textLine(f1 + f2 + f3)
    }

    fun feed(lines: Int = 1): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x64, lines.coerceIn(1, 10).toByte())) // ESC d n
    }

    fun cutPaper(fullCut: Boolean = false): EscPosBuilder = apply {
        // GS V m (0 for full cut, 1 for partial cut)
        buffer.write(byteArrayOf(0x1D, 0x56, if (fullCut) 0x00 else 0x01))
    }

    fun openCashDrawer(): EscPosBuilder = apply {
        // ESC p m t1 t2 (m: pin 0 or 1, t1: pulse on time, t2: pulse off time)
        // Pulse pin 2
        buffer.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
        // Pulse pin 5
        buffer.write(byteArrayOf(0x1B, 0x70, 0x01, 0x19, 0xFA.toByte()))
    }

    /** Sets barcode vertical height in printer dots (`GS h n`, range 1..255). */
    fun barcodeHeight(dots: Int = 60): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1D, 0x68, dots.coerceIn(1, 255).toByte()))
    }

    /** Sets barcode module width (`GS w n`, range 2..6). */
    fun barcodeModuleWidth(width: Int = 2): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1D, 0x77, width.coerceIn(2, 6).toByte()))
    }

    /** Sets HRI text position (`GS H n`: 0=none, 1=above, 2=below, 3=both). */
    fun barcodeHriPosition(position: Int = 2): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1D, 0x48, position.coerceIn(0, 3).toByte()))
    }

    /**
     * Emits a 1D graphic barcode via ESC/POS `GS k m n d1..dn`.
     * Supports EAN-13 (`m = 67`) and Code 128 Set B (`m = 73`).
     */
    fun barcode(
        content: String,
        symbology: BarcodeSymbology = BarcodeSymbology.AUTO,
        heightDots: Int = 60,
        moduleWidth: Int = 2,
        hriBelow: Boolean = true,
    ): EscPosBuilder = apply {
        val clean = content.trim()
            .filter { it.code in 0x20..0x7E }
            .take(48)
        if (clean.isEmpty()) return@apply

        barcodeHeight(heightDots)
        barcodeModuleWidth(moduleWidth)
        barcodeHriPosition(if (hriBelow) 2 else 0)

        val useEan13 = when (symbology) {
            BarcodeSymbology.EAN13 -> clean.length in 12..13 && clean.all(Char::isDigit)
            BarcodeSymbology.AUTO -> isValidEan13Input(clean)
            BarcodeSymbology.CODE128 -> false
        }

        if (useEan13) {
            val digits = clean.take(13).toByteArray(Charsets.US_ASCII)
            buffer.write(byteArrayOf(0x1D, 0x6B, 67, digits.size.toByte()))
            buffer.write(digits)
        } else {
            val escaped = clean.replace("{", "{{")
            val dataBytes = ("{B$escaped").toByteArray(Charsets.US_ASCII)
            val length = dataBytes.size.coerceAtMost(255)
            buffer.write(byteArrayOf(0x1D, 0x6B, 73, length.toByte()))
            buffer.write(dataBytes, 0, length)
        }
    }

    internal companion object {
        internal fun isValidEan13Input(code: String): Boolean {
            if (!code.all(Char::isDigit)) return false
            if (code.length == 12) return true
            if (code.length != 13) return false
            var sum = 0
            for (i in 0 until 12) {
                val d = code[i] - '0'
                sum += if (i % 2 == 0) d else d * 3
            }
            val expectedCheck = (10 - (sum % 10)) % 10
            return (code[12] - '0') == expectedCheck
        }
    }

    fun build(): ByteArray = buffer.toByteArray()
}
