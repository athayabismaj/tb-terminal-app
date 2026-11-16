package com.tbterminal.app.data.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object PriceTagPrintHelper {

    fun generateCode128Bitmap(content: String, widthPx: Int = 400, heightPx: Int = 100): Bitmap? {
        return runCatching {
            val clean = content.filter { it.code in 32..126 }.ifBlank { "000000" }
            val bitMatrix = MultiFormatWriter().encode(clean, BarcodeFormat.CODE_128, widthPx, heightPx)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }.getOrNull()
    }

    suspend fun printThermalLabels(
        context: Context,
        items: List<PriceTagItem>,
        storeName: String = "TB TERMINAL",
        thermalPrinterManager: ThermalPrinterManager? = null,
    ): PrinterResult {
        if (items.isEmpty()) {
            return PrinterResult.Error("Antrean label masih kosong.")
        }
        val paperSize = thermalPrinterManager?.getPaperSize() ?: "58mm"
        val bytes = PriceTagLabelFormatter.buildBatchPriceTagBytes(
            items = items,
            storeName = storeName,
            paperSize = paperSize,
        )
        if (thermalPrinterManager != null) {
            return thermalPrinterManager.printCustomBytes(context, bytes)
        }
        val pairedPrinters = BluetoothPrinterDriver.getPairedDevices(context)
        if (pairedPrinters.isEmpty()) {
            return PrinterResult.Error("Tidak ada printer Bluetooth yang terhubung. Silakan pasangkan printer di Pengaturan.")
        }
        return BluetoothPrinterDriver.print(context, pairedPrinters.first().address, bytes)
    }

    fun printPriceTagSheetPdf(
        context: Context,
        items: List<PriceTagItem>,
        storeName: String = "TB TERMINAL",
    ) {
        if (items.isEmpty()) return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = if (items.size == 1) {
            "Label_Harga_${items.first().sku.ifBlank { "Produk" }}"
        } else {
            "Label_Harga_Massal_${items.size}_Item"
        }
        printManager.print(
            jobName,
            PriceTagSheetPrintAdapter(
                context = context,
                items = items,
                storeName = storeName.ifBlank { "TB TERMINAL" },
            ),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build()
        )
    }
}

private data class FlattenedPriceTagLabel(
    val productName: String,
    val code: String,
    val formattedPrice: String,
    val unitName: String,
)

private class PriceTagSheetPrintAdapter(
    private val context: Context,
    items: List<PriceTagItem>,
    private val storeName: String,
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null
    private val expandedLabels: List<FlattenedPriceTagLabel>
    private val cols = 3
    private val rowsPerPage = 6
    private val perPage = cols * rowsPerPage

    init {
        val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
        expandedLabels = items.flatMap { item ->
            val cleanCode = item.barcode?.trim().orEmpty().ifBlank { item.sku.trim() }.ifBlank { "000000" }
            val priceText = "Rp ${formatter.format(item.price)}"
            val label = FlattenedPriceTagLabel(
                productName = item.productName,
                code = cleanCode,
                formattedPrice = priceText,
                unitName = item.unitName.ifBlank { "Pcs" },
            )
            List(item.copies.coerceIn(1, 50)) { label }
        }
    }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val pageCount = ((expandedLabels.size + perPage - 1) / perPage).coerceAtLeast(1)
        val info = PrintDocumentInfo.Builder("label_harga_tb_terminal.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val doc = pdfDocument ?: return
        val pageCount = ((expandedLabels.size + perPage - 1) / perPage).coerceAtLeast(1)
        val bitmapCache = mutableMapOf<String, Bitmap?>()

        for (pageIndex in 0 until pageCount) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                doc.close()
                pdfDocument = null
                return
            }
            val page = doc.startPage(pageIndex)
            drawPage(
                canvas = page.canvas,
                pageIndex = pageIndex,
                bitmapCache = bitmapCache,
            )
            doc.finishPage(page)
        }

        try {
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            bitmapCache.values.forEach { it?.recycle() }
            doc.close()
            pdfDocument = null
        }
    }

    private fun drawPage(
        canvas: Canvas,
        pageIndex: Int,
        bitmapCache: MutableMap<String, Bitmap?>,
    ) {
        val pageWidth = canvas.width.toFloat()
        val pageHeight = canvas.height.toFloat()
        val margin = 20f
        val gap = 8f
        val cellW = (pageWidth - margin * 2 - gap * (cols - 1)) / cols
        val cellH = (pageHeight - margin * 2 - gap * (rowsPerPage - 1)) / rowsPerPage

        val borderPaint = Paint().apply {
            color = Color.rgb(180, 185, 195)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val fillPaint = Paint().apply {
            color = Color.rgb(254, 252, 232)
            style = Paint.Style.FILL
        }
        val storePaint = Paint().apply {
            color = Color.rgb(133, 77, 14)
            textSize = 6.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val codePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 6.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val pricePaint = Paint().apply {
            color = Color.rgb(16, 83, 60)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val unitPaint = Paint().apply {
            color = Color.GRAY
            textSize = 6.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val startIndex = pageIndex * perPage
        val endIndex = minOf(startIndex + perPage, expandedLabels.size)
        for (idx in startIndex until endIndex) {
            val localIdx = idx - startIndex
            val row = localIdx / cols
            val col = localIdx % cols
            val left = margin + col * (cellW + gap)
            val top = margin + row * (cellH + gap)
            val right = left + cellW
            val bottom = top + cellH
            val centerX = (left + right) / 2f
            val label = expandedLabels[idx]

            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
            canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

            canvas.drawText(storeName.take(22), centerX, top + 12f, storePaint)
            canvas.drawText(label.productName.take(26), centerX, top + 24f, titlePaint)

            val barcodeBmp = bitmapCache.getOrPut(label.code) {
                PriceTagPrintHelper.generateCode128Bitmap(label.code, widthPx = 280, heightPx = 56)
            }
            if (barcodeBmp != null) {
                val bmpRect = RectF(left + 12f, top + 30f, right - 12f, top + 62f)
                canvas.drawBitmap(barcodeBmp, null, bmpRect, null)
            }
            canvas.drawText(label.code.take(22), centerX, top + 71f, codePaint)
            canvas.drawText(label.formattedPrice, centerX, bottom - 18f, pricePaint)
            canvas.drawText("per ${label.unitName}", centerX, bottom - 7f, unitPaint)
        }
    }
}
