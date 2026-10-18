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

    fun build(): ByteArray = buffer.toByteArray()
}
