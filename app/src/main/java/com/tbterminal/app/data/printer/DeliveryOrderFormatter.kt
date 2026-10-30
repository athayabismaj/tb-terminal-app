package com.tbterminal.app.data.printer

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter Surat Jalan / Bukti Pengiriman Barang (Delivery Order)
 * untuk printer thermal Bluetooth / USB (ESC/POS) dan format teks WhatsApp.
 *
 * Khusus toko bahan bangunan/ritel, surat jalan memuat daftar fisik barang
 * yang harus diantar oleh armada/supir toko beserta alamat tujuan dan
 * kolom tanda tangan pengirim, sopir, dan penerima barang.
 */
object DeliveryOrderFormatter {

    data class DeliveryParsedInfo(
        val isDelivery: Boolean,
        val deliveryFeeText: String?,
        val address: String?,
        val courier: String?
    )

    fun parseDeliveryInfo(notes: String?): DeliveryParsedInfo {
        if (notes.isNullOrBlank()) {
            return DeliveryParsedInfo(false, null, null, null)
        }

        val hasDeliveryTag = notes.contains("[PENGIRIMAN]", ignoreCase = true)
        if (!hasDeliveryTag) {
            return DeliveryParsedInfo(false, null, null, null)
        }

        // Contoh: [PENGIRIMAN] Ongkir: Rp 50.000 | Alamat: Jl. Merdeka No. 45 | Armada: Pick-up Engkel Toko
        var fee: String? = null
        var addr: String? = null
        var cour: String? = null

        val feeMatch = Regex("""Ongkir:\s*Rp\s*([\d\.,]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (feeMatch != null) {
            fee = "Rp " + feeMatch.groupValues[1]
        }

        val addrMatch = Regex("""Alamat:\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (addrMatch != null) {
            addr = addrMatch.groupValues[1].trim()
        }

        val courMatch = Regex("""(?:Armada|Supir):\s*([^|]+)""", RegexOption.IGNORE_CASE).find(notes)
        if (courMatch != null) {
            cour = courMatch.groupValues[1].trim()
        }

        return DeliveryParsedInfo(
            isDelivery = true,
            deliveryFeeText = fee,
            address = addr,
            courier = cour
        )
    }

    private fun formatTimestamp(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
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
        paperSize: String = "58mm"
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)
        val deliveryInfo = parseDeliveryInfo(transaction.notes)

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
        builder.textLine("*** SURAT JALAN / BUKTI PENGIRIMAN ***")
        builder.bold(false)
        builder.lineSeparator('=')

        // 2. Metadata Pengiriman
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.twoColumn("No. Referensi", transaction.receiptNumber())
        builder.twoColumn("Tanggal", formatTimestamp(transaction.createdAt))
        builder.twoColumn("Penerima", transaction.customerName ?: "Pelanggan Umum")

        val targetAddress = deliveryInfo.address?.ifBlank { null } ?: "Diambil di Toko / Alamat Tidak Tercatat"
        builder.textLine("Tujuan: $targetAddress")

        val courierName = deliveryInfo.courier?.ifBlank { null } ?: "Supir / Armada Toko"
        builder.twoColumn("Armada/Supir", courierName)

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
        if (deliveryInfo.deliveryFeeText != null) {
            builder.twoColumn("Biaya Kirim", deliveryInfo.deliveryFeeText)
        }
        val cleanNotes = transaction.notes?.replace(Regex("""\[PENGIRIMAN\].*"""), "")?.trim()
        if (!cleanNotes.isNullOrBlank()) {
            builder.textLine("Catatan: $cleanNotes")
        }

        builder.feed(1)

        // 6. Kolom Tanda Tangan Fisik (Pengirim, Sopir, Penerima)
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.textLine("TANDA TERIMA PENGIRIMAN:")
        builder.feed(1)

        if (paperSize.equals("80mm", ignoreCase = true)) {
            builder.threeColumn("Pengirim/Gudang", "Sopir/Armada", "Penerima")
            builder.feed(2)
            builder.threeColumn("(............)", "(............)", "(............)")
        } else {
            // 58mm: Vertikal bertingkat
            builder.align(EscPosBuilder.Alignment.LEFT)
            builder.twoColumn("1. Pengirim / Kasir", ": (..............)")
            builder.twoColumn("2. Sopir / Armada", ": (..............)")
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
        storeName: String = "TB TERMINAL"
    ): String {
        val deliveryInfo = parseDeliveryInfo(transaction.notes)
        val customerName = transaction.customerName ?: "Pelanggan Umum"
        val address = deliveryInfo.address ?: "-"
        val courier = deliveryInfo.courier ?: "Armada Toko"

        val sb = StringBuilder()
        sb.appendLine("*$storeName - SURAT JALAN / BUKTI KIRIM*")
        sb.appendLine("No. Surat Jalan : ${transaction.receiptNumber()}")
        sb.appendLine("Tanggal         : ${formatTimestamp(transaction.createdAt)}")
        sb.appendLine("Pelanggan       : $customerName")
        sb.appendLine("Alamat Kirim    : $address")
        sb.appendLine("Supir / Armada  : $courier")
        sb.appendLine("--------------------------------")
        sb.appendLine("*DAFTAR BARANG YANG DIKIRIM:*")

        transaction.items.forEachIndexed { index, item ->
            val qty = formatQty(item.quantity)
            val unit = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
            sb.appendLine("${index + 1}. [ ] $qty $unit - ${item.productName}")
        }

        sb.appendLine("--------------------------------")
        if (deliveryInfo.deliveryFeeText != null) {
            sb.appendLine("Biaya Ongkir    : ${deliveryInfo.deliveryFeeText}")
        }
        val cleanNotes = transaction.notes?.replace(Regex("""\[PENGIRIMAN\].*"""), "")?.trim()
        if (!cleanNotes.isNullOrBlank()) {
            sb.appendLine("Catatan Khusus  : $cleanNotes")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Mohon periksa barang saat tiba. Bukti serah terima fisik ditandatangani saat barang diterima.")

        return sb.toString().trimEnd()
    }
}
