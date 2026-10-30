package com.tbterminal.app.ui.checkout.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.EnumMap

/**
 * Utilitas untuk membuat payload QRIS berstandar nasional EMVCo / Bank Indonesia
 * serta merender QR Code Bitmap resolusi tinggi menggunakan ZXing.
 */
object QrisGenerator {

    private const val DEFAULT_NMID = "ID1020038829101"
    private const val DEFAULT_STORE_NAME = "TB TERMINAL"
    private const val DEFAULT_CITY = "INDONESIA"

    /**
     * Menghasilkan bitmap QR Code dari konten string payload QRIS.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = AndroidColor.BLACK,
        backgroundColor: Int = AndroidColor.WHITE
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
        }

        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /**
     * Membangun payload string QRIS dinamis berstandar EMVCo QR Code Specification.
     */
    fun buildQrisPayload(
        storeName: String = DEFAULT_STORE_NAME,
        nmid: String = DEFAULT_NMID,
        amount: BigDecimal,
        invoiceNo: String? = null
    ): String {
        val cleanStoreName = storeName.trim().take(25).ifBlank { DEFAULT_STORE_NAME }
        val cleanNmid = nmid.trim().ifBlank { DEFAULT_NMID }
        val formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

        val rawBuilder = StringBuilder()
        rawBuilder.append(tlv("00", "01")) // Payload Format Indicator
        rawBuilder.append(tlv("01", "12")) // 12 = Dynamic QR (dengan nominal tertera)

        // Tag 26: Merchant Account Information
        val subTag00 = tlv("00", "ID.CO.QRIS.WWW")
        val subTag01 = tlv("01", cleanNmid)
        val subTag02 = tlv("02", "01")
        val subTag03 = tlv("03", "UMI")
        val tag26Value = subTag00 + subTag01 + subTag02 + subTag03
        rawBuilder.append(tlv("26", tag26Value))

        rawBuilder.append(tlv("51", tlv("00", "ID.CO.QRIS.WWW") + tlv("02", cleanNmid)))
        rawBuilder.append(tlv("52", "5211")) // 5211 = Building Materials, Hardware Stores
        rawBuilder.append(tlv("53", "360"))  // 360 = IDR (Rupiah)
        rawBuilder.append(tlv("54", formattedAmount))
        rawBuilder.append(tlv("58", "ID"))   // Country Code Indonesia
        rawBuilder.append(tlv("59", cleanStoreName))
        rawBuilder.append(tlv("60", DEFAULT_CITY))

        // Tag 62: Additional Data (No Invoice / Struk)
        if (!invoiceNo.isNullOrBlank()) {
            val invoiceTag = tlv("01", invoiceNo.take(25))
            rawBuilder.append(tlv("62", invoiceTag))
        }

        rawBuilder.append("6304") // Tag 63 (CRC) with length 04

        val crc = calculateCrc16Ccitt(rawBuilder.toString())
        return rawBuilder.toString() + crc
    }

    private fun tlv(tag: String, value: String): String {
        return "%s%02d%s".format(tag, value.length, value)
    }

    /**
     * Menghitung CRC16-CCITT (Polynomial 0x1021, Init 0xFFFF).
     */
    fun calculateCrc16Ccitt(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray(Charsets.ISO_8859_1)

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return "%04X".format(crc)
    }
}
