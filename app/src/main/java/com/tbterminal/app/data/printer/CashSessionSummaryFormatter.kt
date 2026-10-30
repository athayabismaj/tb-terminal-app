package com.tbterminal.app.data.printer

import com.tbterminal.app.data.model.CashSession
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter untuk mencetak Slip Rekapitulasi Penutupan Kas / Shift Kasir (Shift Settlement)
 * pada printer thermal Bluetooth / USB (ESC/POS) dan format laporan teks WhatsApp ke Pemilik Toko (Owner).
 */
object CashSessionSummaryFormatter {

    private fun formatMoney(amount: BigDecimal?): String {
        val safeAmount = amount ?: BigDecimal.ZERO
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0", symbols)
        return "Rp " + formatter.format(safeAmount)
    }

    private fun formatTimestamp(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "-"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
            val date = inputFormat.parse(isoDate.take(19)) ?: Date()
            outputFormat.format(date)
        } catch (_: Exception) {
            isoDate.take(16).replace('T', ' ')
        }
    }

    /**
     * Membangun byte command ESC/POS untuk dicetak ke printer thermal 58mm / 80mm.
     */
    fun buildShiftSettlementBytes(
        session: CashSession,
        storeName: String = "TB TERMINAL",
        paperSize: String = "58mm"
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)

        // 1. Header Rekapitulasi Shift
        builder.reset()
        builder.align(EscPosBuilder.Alignment.CENTER)
            .bold(true)
            .doubleSize(true)
            .textLine(storeName.ifBlank { "TB TERMINAL" }.uppercase())
            .doubleSize(false)
            .bold(false)

        builder.lineSeparator('=')
        builder.bold(true)
        builder.textLine("*** REKAPITULASI TUTUP SHIFT ***")
        builder.bold(false)
        builder.lineSeparator('=')

        // 2. Info Kasir & Waktu
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.twoColumn("Kasir", session.userName ?: session.userId)
        builder.twoColumn("ID Sesi", session.id.take(12))
        builder.twoColumn("Waktu Buka", formatTimestamp(session.openedAt))
        builder.twoColumn("Waktu Tutup", formatTimestamp(session.closedAt))
        builder.twoColumn("Status", session.status.uppercase())

        builder.lineSeparator('-')

        // 3. Rincian Saldo Kas
        builder.twoColumn("Modal Kas Awal", formatMoney(session.openingCash))

        val systemCash = session.systemCash ?: session.openingCash
        val cashSales = systemCash.subtract(session.openingCash).coerceAtLeast(BigDecimal.ZERO)
        builder.twoColumn("Penjualan Tunai", formatMoney(cashSales))

        if (session.totalExpenses > BigDecimal.ZERO) {
            builder.twoColumn("Pengeluaran Kas", "- " + formatMoney(session.totalExpenses))
        }

        builder.lineSeparator('-')
        builder.bold(true)
        builder.twoColumn("Kas Diharapkan", formatMoney(systemCash))
        builder.twoColumn("Kas Fisik Dihitung", formatMoney(session.closingCash ?: systemCash))
        builder.bold(false)

        val difference = session.difference ?: BigDecimal.ZERO
        val diffLabel = when {
            difference.compareTo(BigDecimal.ZERO) > 0 -> "LEBIH (+)"
            difference.compareTo(BigDecimal.ZERO) < 0 -> "KURANG (-)"
            else -> "PAS / SESUAI"
        }

        builder.lineSeparator('-')
        builder.bold(true)
        builder.twoColumn("Selisih Kas", "$diffLabel ${formatMoney(difference.abs())}")
        builder.bold(false)

        if (!session.notes.isNullOrBlank()) {
            builder.lineSeparator('-')
            builder.textLine("Catatan Kasir: ${session.notes}")
        }

        builder.feed(1)

        // 4. Kolom Tanda Tangan
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.textLine("LEMBAR PERTANGGUNGJAWABAN:")
        builder.feed(1)

        if (paperSize.equals("80mm", ignoreCase = true)) {
            builder.twoColumn("   Kasir Bertugas   ", "Supervisor / Owner")
            builder.feed(2)
            builder.twoColumn(" (................) ", " (................) ")
        } else {
            builder.align(EscPosBuilder.Alignment.LEFT)
            builder.twoColumn("1. Kasir Shift", ": (..............)")
            builder.twoColumn("2. Verifikator", ": (..............)")
        }

        builder.feed(2)
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.textLine("Dicetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date())}")
        builder.feed(3)
        builder.cutPaper(fullCut = false)

        return builder.build()
    }

    /**
     * Membangun pesan WhatsApp ringkasan shift untuk dikirim ke Owner.
     */
    fun buildShiftSettlementWhatsAppText(
        session: CashSession,
        storeName: String = "TB TERMINAL"
    ): String {
        val cashier = session.userName ?: session.userId
        val systemCash = session.systemCash ?: session.openingCash
        val cashSales = systemCash.subtract(session.openingCash).coerceAtLeast(BigDecimal.ZERO)
        val closingCash = session.closingCash ?: systemCash
        val difference = session.difference ?: BigDecimal.ZERO
        val diffStatus = when {
            difference.compareTo(BigDecimal.ZERO) > 0 -> "LEBIH (+) " + formatMoney(difference.abs())
            difference.compareTo(BigDecimal.ZERO) < 0 -> "KURANG (-) " + formatMoney(difference.abs())
            else -> "SESUAI / BALANCE (Rp 0)"
        }

        val sb = StringBuilder()
        sb.appendLine("*$storeName - LAPORAN TUTUP SHIFT KASIR*")
        sb.appendLine("ID Sesi      : ${session.id}")
        sb.appendLine("Kasir        : $cashier")
        sb.appendLine("Waktu Buka   : ${formatTimestamp(session.openedAt)}")
        sb.appendLine("Waktu Tutup  : ${formatTimestamp(session.closedAt)}")
        sb.appendLine("--------------------------------")
        sb.appendLine("*RINGKASAN ARUS KAS:*")
        sb.appendLine("• Modal Awal        : ${formatMoney(session.openingCash)}")
        sb.appendLine("• Penjualan Tunai   : ${formatMoney(cashSales)}")
        if (session.totalExpenses > BigDecimal.ZERO) {
            sb.appendLine("• Pengeluaran Kas   : -${formatMoney(session.totalExpenses)}")
        }
        sb.appendLine("• Kas Seharusnya    : ${formatMoney(systemCash)}")
        sb.appendLine("• Kas Fisik Nyata   : *${formatMoney(closingCash)}*")
        sb.appendLine("--------------------------------")
        sb.appendLine("*STATUS SELISIH KAS : $diffStatus*")
        if (!session.notes.isNullOrBlank()) {
            sb.appendLine("Catatan Shift       : ${session.notes}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Laporan dikirim otomatis dari TB Terminal App.")

        return sb.toString().trimEnd()
    }
}
