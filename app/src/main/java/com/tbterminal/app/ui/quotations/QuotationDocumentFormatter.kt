package com.tbterminal.app.ui.quotations

import android.content.Context
import android.content.Intent
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
import com.tbterminal.app.data.model.QuotationDetail
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

fun formatQuotationMoney(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "Rp ${formatter.format(amount.setScale(0, RoundingMode.HALF_UP))}"
}

fun formatQuotationDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    return runCatching {
        OffsetDateTime.parse(iso).atZoneSameInstant(WIB_ZONE).format(formatter)
    }.recoverCatching {
        Instant.parse(iso).atZone(WIB_ZONE).format(formatter)
    }.getOrDefault(iso.take(10))
}

fun buildQuotationShareText(
    quotation: QuotationDetail,
    storeName: String = "TB TERMINAL",
): String {
    val sb = StringBuilder()
    sb.appendLine("*$storeName*")
    sb.appendLine("*PENAWARAN HARGA (RAB)*")
    sb.appendLine("================================")
    sb.appendLine("No. Penawaran : ${quotation.quotationNumber}")
    sb.appendLine("Tanggal       : ${formatQuotationDate(quotation.createdAt)}")
    sb.appendLine("Berlaku s/d   : ${formatQuotationDate(quotation.validUntil)}")
    sb.appendLine("Pelanggan     : ${quotation.customerName ?: "Pelanggan Umum"}")
    sb.appendLine("Dibuat oleh   : ${quotation.cashierName ?: "-"}")
    sb.appendLine("Status        : ${quotation.status.displayLabel.uppercase()}")
    sb.appendLine("--------------------------------")
    sb.appendLine("*RINCIAN PENAWARAN BARANG:*")
    quotation.items.forEach { item ->
        val qtyStr = item.quantity.stripTrailingZeros().toPlainString()
        val unit = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
        sb.appendLine("• ${item.productName}")
        sb.appendLine("  $qtyStr $unit x ${formatQuotationMoney(item.unitPrice)} = ${formatQuotationMoney(item.subtotal)}")
        if (item.discountAmount > BigDecimal.ZERO) {
            sb.appendLine("  Diskon item: -${formatQuotationMoney(item.discountAmount)}")
        }
        item.note?.trim()?.takeIf { it.isNotBlank() }?.let { note ->
            sb.appendLine("  Catatan: $note")
        }
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("Subtotal      : ${formatQuotationMoney(quotation.grossSubtotal)}")
    if (quotation.itemDiscountTotal > BigDecimal.ZERO) {
        sb.appendLine("Diskon Item   : -${formatQuotationMoney(quotation.itemDiscountTotal)}")
    }
    if (quotation.transactionDiscountAmount > BigDecimal.ZERO) {
        sb.appendLine("Diskon Nota   : -${formatQuotationMoney(quotation.transactionDiscountAmount)}")
    }
    if (quotation.taxEnabled && (quotation.taxAmount > BigDecimal.ZERO || quotation.taxableBase > BigDecimal.ZERO)) {
        val label = quotation.taxLabel?.takeIf { it.isNotBlank() } ?: "PPN"
        val rateText = "$label ${quotation.taxRate.stripTrailingZeros().toPlainString()}%"
        sb.appendLine("DPP           : ${formatQuotationMoney(quotation.taxableBase)}")
        if (quotation.taxMode.equals("INCLUDE", ignoreCase = true)) {
            sb.appendLine("$rateText (Incl.): ${formatQuotationMoney(quotation.taxAmount)}")
        } else {
            sb.appendLine("$rateText      : +${formatQuotationMoney(quotation.taxAmount)}")
        }
    }
    sb.appendLine("*ESTIMASI TOTAL: ${formatQuotationMoney(quotation.total)}*")
    quotation.notes?.trim()?.takeIf { it.isNotBlank() }?.let { note ->
        sb.appendLine("Catatan RAB   : $note")
    }
    sb.appendLine("================================")
    sb.appendLine("_Dokumen ini merupakan PENAWARAN HARGA (RAB), bukan nota lunas / bukti pembayaran._")
    return sb.toString()
}

fun shareQuotationText(
    context: Context,
    quotation: QuotationDetail,
    storeName: String = "TB TERMINAL",
) {
    val text = buildQuotationShareText(quotation, storeName)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Penawaran Harga ${quotation.quotationNumber}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Kirim Penawaran Harga (RAB)"))
}

fun printQuotationPdf(
    context: Context,
    quotation: QuotationDetail,
    storeName: String = "TB TERMINAL",
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val jobName = "Penawaran_${quotation.quotationNumber}"
    printManager.print(
        jobName,
        QuotationPdfAdapter(context, quotation, storeName),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
    )
}

private class QuotationPdfAdapter(
    private val context: Context,
    private val quotation: QuotationDetail,
    private val storeName: String,
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

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
        val info = PrintDocumentInfo.Builder("penawaran_${quotation.quotationNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
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
        val page = doc.startPage(0)
        drawQuotationPdf(page.canvas)
        doc.finishPage(page)

        try {
            FileOutputStream(destination.fileDescriptor).use { out -> doc.writeTo(out) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            doc.close()
            pdfDocument = null
        }
    }

    private fun drawQuotationPdf(canvas: Canvas) {
        val pageWidth = canvas.width.toFloat()
        val pageHeight = canvas.height.toFloat()
        val left = 28f
        val right = pageWidth - 28f
        val width = right - left
        var y = 30f

        val brand = Color.rgb(16, 83, 60)
        val amberText = Color.rgb(180, 83, 9)
        val amberBg = Color.rgb(254, 243, 199)
        val dark = Color.rgb(15, 23, 42)
        val muted = Color.rgb(100, 116, 139)

        val titlePaint = Paint().apply {
            color = brand
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val docPaint = Paint().apply {
            color = amberText
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val labelPaint = Paint().apply { color = muted; textSize = 8f; isAntiAlias = true }
        val valPaint = Paint().apply {
            color = dark
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply { color = Color.rgb(203, 213, 225); strokeWidth = 0.8f }
        val fillPaint = Paint().apply { style = Paint.Style.FILL }

        canvas.drawText(storeName.uppercase(), left, y + 12f, titlePaint)
        canvas.drawText("PENAWARAN HARGA (RAB)", right, y + 12f, docPaint)
        y += 20f

        val badgeRect = RectF(right - 120f, y, right, y + 16f)
        fillPaint.color = amberBg
        canvas.drawRoundRect(badgeRect, 3f, 3f, fillPaint)
        val badgePaint = Paint().apply {
            color = amberText
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("BUKAN NOTA LUNAS", (badgeRect.left + badgeRect.right) / 2f, y + 11f, badgePaint)

        canvas.drawText("No. Penawaran : ${quotation.quotationNumber}", left, y + 11f, valPaint)
        y += 14f
        canvas.drawText("Tanggal : ${formatQuotationDate(quotation.createdAt)}   |   Berlaku s/d : ${formatQuotationDate(quotation.validUntil)}", left, y + 8f, labelPaint)
        y += 12f
        canvas.drawText("Pelanggan : ${quotation.customerName ?: "Pelanggan Umum"}   |   Kasir : ${quotation.cashierName ?: "-"}", left, y + 8f, labelPaint)
        y += 16f

        canvas.drawLine(left, y, right, y, linePaint)
        y += 10f

        // Table Header
        val colNo = 24f
        val colQty = 46f
        val colUnit = 42f
        val colPrice = 74f
        val colSub = 82f
        val colName = width - (colNo + colQty + colUnit + colPrice + colSub)
        fillPaint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(left, y, right, y + 18f, fillPaint)
        val thPaint = Paint().apply {
            color = dark
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("NO", left + 8f, y + 12f, thPaint)
        canvas.drawText("NAMA BARANG", left + colNo + 4f, y + 12f, thPaint)
        canvas.drawText("QTY", left + colNo + colName + colQty - 4f, y + 12f, Paint(thPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("SATUAN", left + colNo + colName + colQty + 4f, y + 12f, thPaint)
        canvas.drawText("HARGA", right - colSub - 4f, y + 12f, Paint(thPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("SUBTOTAL", right - 4f, y + 12f, Paint(thPaint).apply { textAlign = Paint.Align.RIGHT })
        y += 18f

        val cellPaint = Paint().apply { color = dark; textSize = 8f; isAntiAlias = true }
        val cellRightPaint = Paint(cellPaint).apply { textAlign = Paint.Align.RIGHT }
        val notePaint = Paint().apply { color = muted; textSize = 6.8f; isAntiAlias = true }

        quotation.items.forEachIndexed { index, item ->
            val hasNote = !item.note.isNullOrBlank()
            val rowH = if (hasNote) 25f else 17f
            if (y < pageHeight - 130f) {
                canvas.drawText("${index + 1}", left + 8f, y + 12f, cellPaint)
                canvas.drawText(item.productName.take(34), left + colNo + 4f, y + 12f, valPaint)
                if (hasNote) {
                    canvas.drawText("Cat: ${item.note!!.trim().take(42)}", left + colNo + 4f, y + 21f, notePaint)
                }
                canvas.drawText(
                    item.quantity.stripTrailingZeros().toPlainString(),
                    left + colNo + colName + colQty - 4f,
                    y + 12f,
                    cellRightPaint
                )
                canvas.drawText(
                    item.unitName?.ifBlank { "pcs" } ?: "pcs",
                    left + colNo + colName + colQty + 4f,
                    y + 12f,
                    cellPaint
                )
                canvas.drawText(formatQuotationMoney(item.unitPrice), right - colSub - 4f, y + 12f, cellRightPaint)
                canvas.drawText(formatQuotationMoney(item.subtotal), right - 4f, y + 12f, Paint(valPaint).apply { textAlign = Paint.Align.RIGHT })
                canvas.drawLine(left, y + rowH, right, y + rowH, linePaint)
                y += rowH
            }
        }

        y += 12f
        val sumX = right - 200f
        fun drawSummaryRow(lbl: String, value: String, bold: Boolean = false) {
            val p = if (bold) valPaint else labelPaint
            val vp = Paint(if (bold) valPaint else cellPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(lbl, sumX, y + 10f, p)
            canvas.drawText(value, right - 4f, y + 10f, vp)
            y += 14f
        }

        drawSummaryRow("Subtotal", formatQuotationMoney(quotation.grossSubtotal))
        if (quotation.itemDiscountTotal > BigDecimal.ZERO) {
            drawSummaryRow("Diskon Item", "-${formatQuotationMoney(quotation.itemDiscountTotal)}")
        }
        if (quotation.transactionDiscountAmount > BigDecimal.ZERO) {
            drawSummaryRow("Diskon Nota", "-${formatQuotationMoney(quotation.transactionDiscountAmount)}")
        }
        if (quotation.taxEnabled && (quotation.taxAmount > BigDecimal.ZERO || quotation.taxableBase > BigDecimal.ZERO)) {
            drawSummaryRow("DPP", formatQuotationMoney(quotation.taxableBase))
            val lbl = quotation.taxLabel?.takeIf { it.isNotBlank() } ?: "PPN"
            val rate = quotation.taxRate.stripTrailingZeros().toPlainString()
            drawSummaryRow("$lbl $rate%", formatQuotationMoney(quotation.taxAmount))
        }
        y += 4f
        fillPaint.color = Color.rgb(240, 253, 244)
        canvas.drawRoundRect(RectF(sumX - 8f, y, right, y + 20f), 4f, 4f, fillPaint)
        canvas.drawText("ESTIMASI TOTAL", sumX, y + 13f, Paint(titlePaint).apply { textSize = 9.5f })
        canvas.drawText(formatQuotationMoney(quotation.total), right - 4f, y + 13f, Paint(titlePaint).apply { textSize = 10f; textAlign = Paint.Align.RIGHT })
        y += 30f

        quotation.notes?.trim()?.takeIf { it.isNotBlank() }?.let { note ->
            canvas.drawText("Catatan: $note", left, y, labelPaint)
            y += 14f
        }
        canvas.drawText("Keterangan: Penawaran harga (RAB) ini berlaku hingga ${formatQuotationDate(quotation.validUntil)} dan tidak mengikat stok barang.", left, pageHeight - 30f, notePaint)
    }
}
