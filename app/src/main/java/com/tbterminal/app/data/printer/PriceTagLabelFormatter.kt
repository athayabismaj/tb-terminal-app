package com.tbterminal.app.data.printer

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter perintah ESC/POS untuk mencetak label harga rak toko (shelf price tag)
 * pada printer thermal label / kasir 58mm atau 80mm.
 */
object PriceTagLabelFormatter {

    fun buildPriceTagBytes(
        productName: String,
        sku: String,
        barcode: String? = null,
        price: BigDecimal,
        unitName: String = "Pcs",
        storeName: String = "TB TERMINAL",
        paperSize: String = "58mm",
        copies: Int = 1
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
        val formattedDate = dateFormatter.format(Date())
        val formattedPrice = "Rp " + java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(price)

        val totalCopies = copies.coerceIn(1, 20)

        for (i in 1..totalCopies) {
            builder.reset()
            builder.align(EscPosBuilder.Alignment.CENTER)

            // Header Toko
            builder.bold(true)
            builder.textLine("* $storeName *")
            builder.bold(false)
            builder.lineSeparator('=')

            // Nama Produk
            builder.bold(true)
            builder.doubleSize(false)
            builder.textLine(productName.uppercase())
            builder.bold(false)

            // SKU & Barcode
            val cleanSku = sku.ifBlank { barcode.orEmpty() }
            if (cleanSku.isNotBlank()) {
                builder.textLine("SKU: $cleanSku")
            }

            builder.lineSeparator('-')

            // Harga Utama (Ukuran Besar & Tebal)
            builder.bold(true)
            builder.doubleSize(true)
            builder.textLine(formattedPrice)
            builder.doubleSize(false)
            builder.bold(false)

            // Satuan Unit
            builder.textLine("Satuan: $unitName")

            builder.lineSeparator('-')
            builder.align(EscPosBuilder.Alignment.RIGHT)
            builder.textLine("Cetak: $formattedDate")

            // Jeda pemotongan kertas
            builder.feed(3)
            builder.cutPaper(fullCut = false)
        }

        return builder.build()
    }
}
