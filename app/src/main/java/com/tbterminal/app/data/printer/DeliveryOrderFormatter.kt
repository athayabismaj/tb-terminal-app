package com.tbterminal.app.data.printer

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter Surat Jalan / Bukti Pengiriman Barang (Delivery Order)
 * untuk printer thermal Bluetooth / USB (ESC/POS), dokumen resmi PDF (A5/A4),
 * dan format pesan terstruktur WhatsApp.
 *
 * Khusus toko bahan bangunan & ritel material konstruksi, surat jalan
 * memuat daftar fisik barang yang dikirim, alamat proyek/tujuan,
 * identitas armada & supir, serta 3 tanda tangan resmi:
 * 1. Kasir / Toko (Pengirim yang menerbitkan nota)
 * 2. Sopir / Pengemudi (Bagian gudang & armada pengangkut)
 * 3. Penerima Barang / Mandor Proyek di lokasi
 */
object DeliveryOrderFormatter {

    data class DeliveryParsedInfo(
        val isDelivery: Boolean,
        val deliveryFeeText: String?,
        val address: String?,
        val courier: String?,
        val recipientPhone: String? = null,
        val vehiclePlate: String? = null,
        val specialInstructions: String? = null
    )

    data class DeliveryOrderData(
        val recipientName: String = "",
        val recipientPhone: String = "",
        val address: String = "",
        val courier: String = "",
        val vehiclePlate: String = "",
        val deliveryFeeText: String = "",
        val specialInstructions: String = ""
    )

    fun DeliveryParsedInfo.toDeliveryOrderData(customerName: String? = null): DeliveryOrderData {
        return DeliveryOrderData(
            recipientName = customerName?.ifBlank { null } ?: "Pelanggan Umum",
            recipientPhone = this.recipientPhone ?: "",
            address = this.address ?: "",
            courier = this.courier ?: "Armada Toko",
            vehiclePlate = this.vehiclePlate ?: "",
            deliveryFeeText = this.deliveryFeeText ?: "",
            specialInstructions = this.specialInstructions ?: ""
        )
    }

    /**
     * Konversi bilangan rupiah ke teks terbilang bahasa Indonesia.
     */
    fun numberToWordsIndonesian(amount: Long): String {
        if (amount == 0L) return "Nol"
        val units = arrayOf(
            "", "Satu", "Dua", "Tiga", "Empat", "Lima", "Enam", "Tujuh", "Delapan", "Sembilan",
            "Sepuluh", "Sebelas"
        )
        fun convert(n: Long): String {
            return when {
                n < 12L -> units[n.toInt()]
                n < 20L -> "${convert(n - 10L)} Belas"
                n < 100L -> "${units[(n / 10L).toInt()]} Puluh ${convert(n % 10L)}".trim()
                n < 200L -> "Seratus ${convert(n - 100L)}".trim()
                n < 1000L -> "${units[(n / 100L).toInt()]} Ratus ${convert(n % 100L)}".trim()
                n < 2000L -> "Seribu ${convert(n - 1000L)}".trim()
                n < 1000000L -> "${convert(n / 1000L)} Ribu ${convert(n % 1000L)}".trim()
                n < 1000000000L -> "${convert(n / 1000000L)} Juta ${convert(n % 1000000L)}".trim()
                n < 1000000000000L -> "${convert(n / 1000000000L)} Miliar ${convert(n % 1000000000L)}".trim()
                else -> "${convert(n / 1000000000000L)} Triliun ${convert(n % 1000000000000L)}".trim()
            }
        }
        val words = convert(kotlin.math.abs(amount))
        return if (amount < 0) "Minus $words" else words
    }

    fun parseDeliveryInfo(notes: String?): DeliveryParsedInfo {
        if (notes.isNullOrBlank()) {
            return DeliveryParsedInfo(false, null, null, null)
        }

        val hasDeliveryTag = notes.contains("[PENGIRIMAN]", ignoreCase = true)
        if (!hasDeliveryTag) {
            return DeliveryParsedInfo(false, null, null, null)
        }

        var fee: String? = null
        var addr: String? = null
        var cour: String? = null
        var phone: String? = null
        var plate: String? = null
        var instructions: String? = null

        val feeMatch = Regex("""(?:Ongkir|Biaya Kirim):\s*Rp\s*([\d\.,]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (feeMatch != null) {
            fee = "Rp " + feeMatch.groupValues[1]
        }

        val addrMatch = Regex("""Alamat:\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (addrMatch != null) {
            addr = addrMatch.groupValues[1].trim()
        }

        val courMatch = Regex("""(?:Armada|Supir|Sopir|Kurir):\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (courMatch != null) {
            cour = courMatch.groupValues[1].trim()
        }

        val phoneMatch = Regex("""(?:Telp|WA|No HP|Telepon):\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (phoneMatch != null) {
            phone = phoneMatch.groupValues[1].trim()
        }

        val plateMatch = Regex("""(?:Plat|No Polisi|Nopol):\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (plateMatch != null) {
            plate = plateMatch.groupValues[1].trim()
        }

        val instrMatch = Regex("""(?:Catatan|Instruksi):\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (instrMatch != null) {
            instructions = instrMatch.groupValues[1].trim()
        }

        return DeliveryParsedInfo(
            isDelivery = true,
            deliveryFeeText = fee,
            address = addr,
            courier = cour,
            recipientPhone = phone,
            vehiclePlate = plate,
            specialInstructions = instructions
        )
    }

    private fun formatTimestamp(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
            val date = inputFormat.parse(isoDate.take(19)) ?: Date()
            outputFormat.format(date)
        } catch (_: Exception) {
            isoDate.take(16).replace('T', ' ')
        }
    }

    private fun formatQty(qty: BigDecimal): String {
        return if (qty.stripTrailingZeros().scale() <= 0) {
            qty.setScale(0, RoundingMode.HALF_UP).toPlainString()
        } else {
            qty.stripTrailingZeros().toPlainString()
        }
    }

    /**
     * Menghasilkan byte command ESC/POS untuk dicetak ke printer thermal 58mm / 80mm.
     */
    fun buildDeliveryOrderBytes(
        transaction: CashTransactionDetail,
        storeName: String = "TB TERMINAL",
        storeAddress: String = "",
        storePhone: String = "",
        paperSize: String = "58mm",
        deliveryData: DeliveryOrderData? = null
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)
        val delivery = deliveryData ?: parseDeliveryInfo(transaction.notes).toDeliveryOrderData(transaction.customerName)

        // 1. Header Surat Jalan
        builder.reset()
        builder.align(EscPosBuilder.Alignment.CENTER)
            .bold(true)
            .doubleSize(true)
            .textLine(storeName.ifBlank { "TB TERMINAL" }.uppercase())
            .doubleSize(false)
            .bold(false)

        if (storeAddress.isNotBlank()) {
            builder.textLine(storeAddress)
        }
        if (storePhone.isNotBlank()) {
            builder.textLine("Telp: $storePhone")
        }

        builder.lineSeparator('=')
        builder.bold(true)
        builder.textLine("*** SURAT JALAN & PENGIRIMAN ***")
        builder.bold(false)
        builder.lineSeparator('=')

        // 2. Metadata Pengiriman
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.twoColumn("No. Surat Jalan", "SJ-${transaction.receiptNumber()}")
        builder.twoColumn("No. Referensi", transaction.receiptNumber())
        builder.twoColumn("Tanggal", formatTimestamp(transaction.createdAt))
        val recipientDisplay = delivery.recipientName.ifBlank { transaction.customerName ?: "Pelanggan Umum" }
        builder.twoColumn("Penerima", recipientDisplay)
        if (delivery.recipientPhone.isNotBlank()) {
            builder.twoColumn("Telp/WA", delivery.recipientPhone)
        }

        val targetAddress = delivery.address.ifBlank { "Diambil di Toko / Alamat Tidak Tercatat" }
        builder.textLine("Tujuan: $targetAddress")

        val courierDisplay = buildString {
            append(delivery.courier.ifBlank { "Supir / Armada Toko" })
            if (delivery.vehiclePlate.isNotBlank()) {
                append(" (${delivery.vehiclePlate})")
            }
        }
        builder.twoColumn("Armada/Supir", courierDisplay)

        builder.lineSeparator('-')

        // 3. Header Tabel Barang (Fokus pemeriksaan fisik barang: Checkbox & Qty)
        builder.bold(true)
        builder.textLine("[ ]  QTY   NAMA BARANG")
        builder.bold(false)
        builder.lineSeparator('-')

        // 4. Daftar Item
        for (item in transaction.items) {
            val qtyStr = formatQty(item.quantity)
            val unitStr = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
            val itemLine = "[ ]  ${qtyStr.padStart(3)} $unitStr  ${item.productName}"
            builder.textLine(itemLine)
        }

        builder.lineSeparator('-')

        // 5. Catatan / Instruksi Pengiriman
        if (delivery.deliveryFeeText.isNotBlank()) {
            builder.twoColumn("Biaya Kirim", delivery.deliveryFeeText)
        }
        val cleanNotes = delivery.specialInstructions.ifBlank {
            transaction.notes?.replace(Regex("""\[PENGIRIMAN\].*"""), "")?.trim().orEmpty()
        }
        if (cleanNotes.isNotBlank()) {
            builder.textLine("Catatan: $cleanNotes")
        }

        builder.feed(1)

        // 6. Kolom Tanda Tangan Fisik (Kasir Toko, Sopir/Gudang, Penerima)
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.textLine("TANDA TERIMA PENGIRIMAN:")
        builder.feed(1)

        if (paperSize.equals("80mm", ignoreCase = true)) {
            builder.threeColumn("Kasir Toko", "Sopir/Gudang", "Penerima")
            builder.feed(2)
            builder.threeColumn("(............)", "(............)", "(............)")
        } else {
            // 58mm: Vertikal bertingkat
            builder.align(EscPosBuilder.Alignment.LEFT)
            builder.twoColumn("1. Kasir (Toko)", ": (..............)")
            builder.twoColumn("2. Sopir / Gudang", ": (..............)")
            builder.twoColumn("3. Penerima Barang", ": (..............)")
        }

        builder.feed(2)
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.textLine("Barang telah diterima dalam kondisi baik & cukup.")
        builder.feed(3)
        builder.cutPaper(fullCut = false)

        return builder.build()
    }

    /**
     * Menghasilkan teks surat jalan terstruktur untuk dikirimkan ke WhatsApp Supir atau Pelanggan.
     */
    fun buildDeliveryOrderWhatsAppText(
        transaction: CashTransactionDetail,
        storeName: String = "TB TERMINAL",
        deliveryData: DeliveryOrderData? = null
    ): String {
        val delivery = deliveryData ?: parseDeliveryInfo(transaction.notes).toDeliveryOrderData(transaction.customerName)
        val customerName = delivery.recipientName.ifBlank { transaction.customerName ?: "Pelanggan Umum" }
        val address = delivery.address.ifBlank { "-" }
        val courier = delivery.courier.ifBlank { "Armada Toko" }
        val plate = delivery.vehiclePlate.ifBlank { "" }
        val phone = delivery.recipientPhone.ifBlank { "-" }

        val sb = StringBuilder()
        sb.appendLine("*$storeName - SURAT JALAN / BUKTI KIRIM*")
        sb.appendLine("No. Surat Jalan : SJ-${transaction.receiptNumber()}")
        sb.appendLine("No. Ref Nota    : ${transaction.receiptNumber()}")
        sb.appendLine("Tanggal         : ${formatTimestamp(transaction.createdAt)}")
        sb.appendLine("Pelanggan       : $customerName")
        if (phone != "-") {
            sb.appendLine("No. Telepon     : $phone")
        }
        sb.appendLine("Alamat Kirim    : $address")
        val courierText = if (plate.isNotBlank()) "$courier ($plate)" else courier
        sb.appendLine("Supir / Armada  : $courierText")
        sb.appendLine("--------------------------------")
        sb.appendLine("*DAFTAR BARANG YANG DIKIRIM:*")

        transaction.items.forEachIndexed { index, item ->
            val qty = formatQty(item.quantity)
            val unit = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
            sb.appendLine("${index + 1}. [ ] $qty $unit - ${item.productName}")
        }

        sb.appendLine("--------------------------------")
        if (delivery.deliveryFeeText.isNotBlank()) {
            sb.appendLine("Biaya Ongkir    : ${delivery.deliveryFeeText}")
        }
        val cleanNotes = delivery.specialInstructions.ifBlank {
            transaction.notes?.replace(Regex("""\[PENGIRIMAN\].*"""), "")?.trim().orEmpty()
        }
        if (cleanNotes.isNotBlank()) {
            sb.appendLine("Catatan Khusus  : $cleanNotes")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Mohon periksa barang saat tiba. Bukti serah terima fisik ditandatangani saat barang diterima.")

        return sb.toString().trimEnd()
    }

    /**
     * Mencetak Surat Jalan Resmi Dokumen PDF (ISO A5/A4) menggunakan Android PrintManager.
     */
    fun printDeliveryOrderPdf(
        context: Context,
        transaction: CashTransactionDetail,
        deliveryData: DeliveryOrderData
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            "SuratJalan-${transaction.receiptNumber()}",
            DeliveryOrderPrintAdapter(context, transaction, deliveryData),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }
}

/**
 * Android PrintDocumentAdapter untuk mencetak dokumen Surat Jalan resmi A5/A4
 * standar logistik perdagangan bahan bangunan di Indonesia.
 *
 * Kolom Tanda Tangan:
 * 1. Kasir / Toko (Pengirim nota)
 * 2. Sopir / Pengemudi (Bagian gudang & armada pengangkut jadi satu)
 * 3. Penerima Barang / Mandor Proyek di lokasi
 */
class DeliveryOrderPrintAdapter(
    private val context: Context,
    private val transaction: CashTransactionDetail,
    private val deliveryData: DeliveryOrderFormatter.DeliveryOrderData
) : PrintDocumentAdapter() {
    private lateinit var attributes: PrintAttributes

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        signal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (signal.isCanceled) return callback.onLayoutCancelled()
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("surat-jalan-${transaction.receiptNumber()}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        signal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = PrintedPdfDocument(context, attributes)
        try {
            if (signal.isCanceled) return callback.onWriteCancelled()
            val page = document.startPage(0)
            drawSuratJalan(page)
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message ?: "Surat jalan gagal dicetak")
        } finally {
            document.close()
        }
    }

    private fun drawSuratJalan(page: PdfDocument.Page) {
        val canvas = page.canvas
        val pageWidth = page.info.pageWidth.toFloat()
        val pageHeight = page.info.pageHeight.toFloat()

        val left = 28f
        val right = pageWidth - 28f
        val printableWidth = right - left

        // Colors
        val colorPrimary = Color.rgb(2, 132, 199) // Sky Blue 600 #0284C7
        val colorDark = Color.rgb(15, 23, 42) // Slate 900
        val colorMuted = Color.rgb(100, 116, 139) // Slate 500
        val colorBorder = Color.rgb(203, 213, 225) // Slate 300
        val colorHeaderBg = Color.rgb(241, 245, 249) // Slate 100
        val colorAltRow = Color.rgb(248, 250, 252) // Slate 50

        // Paints
        val storeTitlePaint = Paint().apply {
            color = colorDark
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val storeSubPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val docTitlePaint = Paint().apply {
            color = colorPrimary
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val docSubPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val rightMutedPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = colorBorder
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val thickLinePaint = Paint().apply {
            color = colorPrimary
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val metaLabelPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val metaValPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableCellPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val tableCellBoldPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var y = 28f

        // 1. Header Bagian Atas
        canvas.drawText("TB TERMINAL BAROKAH JAYA", left, y, storeTitlePaint)
        canvas.drawText("SURAT JALAN PENGIRIMAN", right, y, docTitlePaint)
        y += 11f

        canvas.drawText("Pusat Perlengkapan & Bahan Bangunan Berkualitas", left, y, storeSubPaint)
        canvas.drawText("BUKTI SERAH TERIMA BARANG (DO)", right, y, docSubPaint)
        y += 10f

        canvas.drawText("Jl. Raya Terminal No. 45 | Telp: (031) 8921123 / WA: 0812-3456-7890", left, y, storeSubPaint)
        canvas.drawText("No. SJ: SJ-${transaction.receiptNumber()}", right, y, metaValPaint.apply { textAlign = Paint.Align.RIGHT })
        y += 8f

        // Garis batas header
        canvas.drawLine(left, y, right, y, thickLinePaint)
        y += 8f

        // 2. Kotak Metadata Pengiriman (2 Panel Berdampingan)
        val boxHeight = 54f
        fillPaint.color = Color.rgb(250, 250, 250)
        canvas.drawRoundRect(RectF(left, y, right, y + boxHeight), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(left, y, right, y + boxHeight), 4f, 4f, linePaint)

        val splitX = left + (printableWidth * 0.52f)
        canvas.drawLine(splitX, y, splitX, y + boxHeight, linePaint)

        // Panel Kiri: Penerima & Alamat
        val leftPad = left + 8f
        var yMeta = y + 12f
        canvas.drawText("TUJUAN PENGIRIMAN / PENERIMA:", leftPad, yMeta, tableHeaderPaint.apply { textSize = 7.5f })
        yMeta += 11f
        canvas.drawText("Penerima", leftPad, yMeta, metaLabelPaint)
        val recipientName = deliveryData.recipientName.ifBlank { transaction.customerName ?: "Pelanggan Umum" }
        canvas.drawText(": $recipientName", leftPad + 50f, yMeta, metaValPaint.apply { textAlign = Paint.Align.LEFT })
        yMeta += 11f
        canvas.drawText("No. Telp/WA", leftPad, yMeta, metaLabelPaint)
        val recipientPhone = deliveryData.recipientPhone.ifBlank { "-" }
        canvas.drawText(": $recipientPhone", leftPad + 50f, yMeta, metaValPaint)
        yMeta += 11f
        canvas.drawText("Alamat Kirim", leftPad, yMeta, metaLabelPaint)
        val address = deliveryData.address.ifBlank { "Diambil di Toko / Alamat Tidak Dicatat" }
        canvas.drawText(": ${address.take(46)}", leftPad + 50f, yMeta, tableCellPaint)

        // Panel Kanan: Ekspedisi & Armada
        val rightPad = splitX + 8f
        yMeta = y + 12f
        canvas.drawText("LOGISTIK & EKSPEDISI TOKO:", rightPad, yMeta, tableHeaderPaint.apply { textSize = 7.5f })
        yMeta += 11f
        canvas.drawText("No. Faktur Ref", rightPad, yMeta, metaLabelPaint)
        canvas.drawText(": ${transaction.receiptNumber()}", rightPad + 60f, yMeta, metaValPaint)
        yMeta += 11f
        canvas.drawText("Armada / Supir", rightPad, yMeta, metaLabelPaint)
        val courierName = deliveryData.courier.ifBlank { "Supir Armada Toko" }
        canvas.drawText(": ${courierName.take(28)}", rightPad + 60f, yMeta, metaValPaint)
        yMeta += 11f
        canvas.drawText("No. Plat / Nopol", rightPad, yMeta, metaLabelPaint)
        val vehiclePlate = deliveryData.vehiclePlate.ifBlank { "-" }
        canvas.drawText(": $vehiclePlate", rightPad + 60f, yMeta, metaValPaint)

        y += boxHeight + 10f

        // 3. Tabel Checklist Fisik Barang
        val colCheckW = 26f
        val colNoW = 20f
        val colQtyW = 34f
        val colUnitW = 36f
        val colCondW = 75f
        val colNameW = printableWidth - (colCheckW + colNoW + colQtyW + colUnitW + colCondW)

        val xCheck = left
        val xNo = xCheck + colCheckW
        val xName = xNo + colNoW
        val xQty = xName + colNameW
        val xUnit = xQty + colQtyW
        val xCond = xUnit + colUnitW

        // Header Tabel
        fillPaint.color = colorHeaderBg
        canvas.drawRect(left, y, right, y + 18f, fillPaint)
        canvas.drawRect(left, y, right, y + 18f, linePaint)

        canvas.drawText("CEK", xCheck + (colCheckW / 2f), y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("NO", xNo + (colNoW / 2f), y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("NAMA BARANG & SPESIFIKASI", xName + 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("QTY", xQty + colQtyW - 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("SATUAN", xUnit + 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("KONDISI FISIK", xCond + (colCondW / 2f), y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.CENTER })

        y += 18f

        // Baris Item Barang
        val rowHeight = 16f
        transaction.items.forEachIndexed { index, item ->
            if (y < pageHeight - 150f) {
                if (index % 2 == 1) {
                    fillPaint.color = colorAltRow
                    canvas.drawRect(left, y, right, y + rowHeight, fillPaint)
                }
                canvas.drawLine(left, y + rowHeight, right, y + rowHeight, linePaint)

                // Kotak Centang Checkbox [ ]
                val boxSize = 8f
                val cbLeft = xCheck + (colCheckW - boxSize) / 2f
                val cbTop = y + (rowHeight - boxSize) / 2f
                canvas.drawRect(cbLeft, cbTop, cbLeft + boxSize, cbTop + boxSize, linePaint)

                // Kolom No
                canvas.drawText("${index + 1}", xNo + (colNoW / 2f), y + 11.5f, tableCellPaint.apply { textAlign = Paint.Align.CENTER })

                // Kolom Nama Barang
                canvas.drawText(item.productName.take(38), xName + 4f, y + 11.5f, tableCellBoldPaint.apply { textAlign = Paint.Align.LEFT })

                // Kolom Qty
                val qtyStr = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                    item.quantity.setScale(0, RoundingMode.HALF_UP).toPlainString()
                } else {
                    item.quantity.stripTrailingZeros().toPlainString()
                }
                canvas.drawText(qtyStr, xQty + colQtyW - 4f, y + 11.5f, tableCellBoldPaint.apply { textAlign = Paint.Align.RIGHT })

                // Kolom Satuan
                val unitStr = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
                canvas.drawText(unitStr, xUnit + 4f, y + 11.5f, tableCellPaint.apply { textAlign = Paint.Align.LEFT })

                // Kolom Kondisi
                canvas.drawText("Baik & Utuh", xCond + (colCondW / 2f), y + 11.5f, tableCellPaint.apply { textAlign = Paint.Align.CENTER; textSize = 7.5f })

                y += rowHeight
            }
        }

        // Garis batas tabel vertikal luar
        canvas.drawLine(left, y, right, y, thickLinePaint)
        y += 6f

        // 4. Catatan Khusus & Ongkos Kirim
        if (deliveryData.deliveryFeeText.isNotBlank() || deliveryData.specialInstructions.isNotBlank()) {
            val noteText = buildString {
                if (deliveryData.deliveryFeeText.isNotBlank()) {
                    append("Biaya Ongkir: ${deliveryData.deliveryFeeText}  |  ")
                }
                if (deliveryData.specialInstructions.isNotBlank()) {
                    append("Instruksi: ${deliveryData.specialInstructions}")
                }
            }
            canvas.drawText(noteText.take(90), left, y + 8f, metaValPaint.apply { textAlign = Paint.Align.LEFT })
            y += 14f
        }

        // Box Klausul Serah Terima
        fillPaint.color = Color.rgb(254, 252, 232) // Amber 50
        val disclaimerRect = RectF(left, y, right, y + 24f)
        canvas.drawRoundRect(disclaimerRect, 3f, 3f, fillPaint)
        val disclaimerBorder = Paint().apply { color = Color.rgb(253, 230, 138); strokeWidth = 0.5f; style = Paint.Style.STROKE }
        canvas.drawRoundRect(disclaimerRect, 3f, 3f, disclaimerBorder)

        val disclaimerPaint = Paint().apply {
            color = Color.rgb(146, 64, 14) // Amber 800
            textSize = 6.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText("PERHATIAN: Harap periksa barang secara seksama bersama sopir sebelum menandatangani surat jalan ini.", left + 6f, y + 9f, disclaimerPaint)
        canvas.drawText("Barang yang telah diterima dan ditandatangani dianggap lengkap dan baik. Klaim setelah serah terima tidak dilayani.", left + 6f, y + 18f, disclaimerPaint)
        y += 32f

        // 5. 3 Kolom Tanda Tangan Resmi: Kasir Toko, Sopir/Gudang, Penerima Proyek
        val sigColW = printableWidth / 3f
        val xSig1 = left + (sigColW * 0.5f)
        val xSig2 = left + sigColW + (sigColW * 0.5f)
        val xSig3 = left + (sigColW * 2f) + (sigColW * 0.5f)

        val sigHeaderPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val sigLinePaint = Paint().apply {
            color = colorDark
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawText("Kasir / Toko (Pengirim),", xSig1, y, sigHeaderPaint)
        canvas.drawText("Sopir / Pengantar (Gudang),", xSig2, y, sigHeaderPaint)
        canvas.drawText("Penerima / Mandor Proyek,", xSig3, y, sigHeaderPaint)

        y += 34f

        val cashierSign = "( ${transaction.cashierName ?: "Kasir TB Terminal"} )"
        canvas.drawText(cashierSign, xSig1, y, sigLinePaint)
        val driverSign = if (deliveryData.courier.isNotBlank() && !deliveryData.courier.contains("Toko", true)) {
            "( ${deliveryData.courier.take(18)} )"
        } else {
            "( ................................... )"
        }
        canvas.drawText(driverSign, xSig2, y, sigLinePaint)
        val recipientSign = if (recipientName != "Pelanggan Umum") {
            "( ${recipientName.take(18)} )"
        } else {
            "( ................................... )"
        }
        canvas.drawText(recipientSign, xSig3, y, sigLinePaint)

        y += 10f
        canvas.drawText("Tgl: ____/____/2026", xSig1, y, rightMutedPaint.apply { textAlign = Paint.Align.CENTER })
        val plateText = if (deliveryData.vehiclePlate.isNotBlank()) "Plat: ${deliveryData.vehiclePlate}" else "No. Polisi: ..........."
        canvas.drawText(plateText, xSig2, y, rightMutedPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("Tgl & Cap: ____/____/2026", xSig3, y, rightMutedPaint.apply { textAlign = Paint.Align.CENTER })

        y += 14f
        // 6. Footer Lembar Distribusi
        val footerPaint = Paint().apply {
            color = colorMuted
            textSize = 6.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Lembar 1: Kasir / Arsip Toko (Putih)   |   Lembar 2: Penerima / Pelanggan (Merah)   |   Lembar 3: Sopir / Pengantar (Kuning)", pageWidth / 2f, y, footerPaint)
    }
}
