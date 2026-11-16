package com.tbterminal.app.data.printer

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PriceTagItem(
    val productId: String = "",
    val productName: String,
    val sku: String,
    val barcode: String? = null,
    val price: BigDecimal,
    val unitName: String = "Pcs",
    val copies: Int = 1,
)

/**
 * Formatter perintah ESC/POS untuk mencetak label harga & barcode rak toko (shelf price tag)
 * pada printer thermal 58mm atau 80mm menggunakan perintah barcode 1D ESC/POS (GS k).
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
        copies: Int = 1,
        symbology: EscPosBuilder.BarcodeSymbology = EscPosBuilder.BarcodeSymbology.AUTO,
    ): ByteArray {
        return buildBatchPriceTagBytes(
            items = listOf(
                PriceTagItem(
                    productName = productName,
                    sku = sku,
                    barcode = barcode,
                    price = price,
                    unitName = unitName,
                    copies = copies,
                )
            ),
            storeName = storeName,
            paperSize = paperSize,
            symbology = symbology,
        )
    }

    fun buildBatchPriceTagBytes(
        items: List<PriceTagItem>,
        storeName: String = "TB TERMINAL",
        paperSize: String = "58mm",
        symbology: EscPosBuilder.BarcodeSymbology = EscPosBuilder.BarcodeSymbology.AUTO,
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)
        val localeId = Locale.forLanguageTag("id-ID")
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", localeId)
        val formattedDate = dateFormatter.format(Date())
        val numberFormatter = NumberFormat.getNumberInstance(localeId)
        val cleanStoreName = storeName.ifBlank { "TB TERMINAL" }

        items.forEach { item ->
            val totalCopies = item.copies.coerceIn(1, 50)
            val formattedPrice = "Rp " + numberFormatter.format(item.price)
            val resolvedBarcode = item.barcode?.trim().orEmpty().ifBlank { item.sku.trim() }
            val resolvedSku = item.sku.trim()

            repeat(totalCopies) {
                builder.reset()
                builder.align(EscPosBuilder.Alignment.CENTER)

                // Header Toko
                builder.bold(true)
                    .textLine("* $cleanStoreName *")
                    .bold(false)
                    .lineSeparator('=')

                // Nama Produk
                builder.bold(true)
                    .doubleSize(false)
                    .textLine(item.productName.uppercase().take(builder.maxCharsPerLine * 2))
                    .bold(false)

                if (resolvedSku.isNotBlank() && resolvedSku != resolvedBarcode) {
                    builder.textLine("SKU: $resolvedSku")
                }

                // Barcode grafis ESC/POS (GS k) dengan teks HRI di bawahnya
                if (resolvedBarcode.isNotBlank()) {
                    builder.barcode(
                        content = resolvedBarcode,
                        symbology = symbology,
                        heightDots = 56,
                        moduleWidth = 2,
                        hriBelow = true,
                    )
                }

                builder.lineSeparator('-')

                // Harga Utama (Ukuran Besar & Tebal)
                builder.bold(true)
                    .doubleSize(true)
                    .textLine(formattedPrice)
                    .doubleSize(false)
                    .bold(false)

                // Satuan Unit
                builder.textLine("Satuan: ${item.unitName.ifBlank { "Pcs" }}")

                builder.lineSeparator('-')
                builder.align(EscPosBuilder.Alignment.RIGHT)
                    .textLine("Cetak: $formattedDate")

                // Jeda pemotongan kertas antar label
                builder.feed(3)
                    .cutPaper(fullCut = false)
            }
        }

        return builder.build()
    }
}
