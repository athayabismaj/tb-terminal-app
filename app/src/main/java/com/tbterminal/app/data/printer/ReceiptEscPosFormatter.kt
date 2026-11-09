package com.tbterminal.app.data.printer

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.model.remainingAmount
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptEscPosFormatter {

    private fun formatMoney(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0", symbols)
        return "Rp " + formatter.format(amount)
    }

    private fun formatQty(qty: BigDecimal): String {
        return qty.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
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

    fun buildReceiptBytes(
        transaction: CashTransactionDetail,
        storeName: String = "TB TERMINAL",
        storeAddress: String = "",
        storePhone: String = "",
        receiptHeader: String = "",
        receiptFooter: String = "Terima kasih telah berbelanja",
        paperSize: String = "58mm",
        openDrawer: Boolean = false,
        autoCut: Boolean = true
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)

        // 1. Kick cash drawer at start if enabled
        if (openDrawer) {
            builder.openCashDrawer()
        }

        // 2. Header
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
        if (receiptHeader.isNotBlank()) {
            builder.textLine(receiptHeader)
        }

        builder.lineSeparator('=')

        // 3. Transaction Meta
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.twoColumn("No. Struk", transaction.receiptNumber())
        builder.twoColumn("Tanggal", formatTimestamp(transaction.createdAt))
        builder.twoColumn("Kasir", transaction.cashierName ?: "-")
        builder.twoColumn("Pelanggan", transaction.customerName ?: "Umum")

        val isVoided = transaction.status.equals("voided", true)
        val isSettled = !isVoided && transaction.remainingAmount() <= BigDecimal.ZERO
        val primaryPaymentMethod = transaction.paymentMethods.firstOrNull().orEmpty().lowercase()
        val statusLabel = when {
            isVoided -> "BATAL / VOID"
            isSettled -> "LUNAS"
            primaryPaymentMethod in listOf("dp", "partial") -> "DP (UANG MUKA)"
            primaryPaymentMethod in listOf("hutang", "tempo", "credit") -> "HUTANG"
            else -> transaction.status.uppercase()
        }
        builder.twoColumn("Status", statusLabel)

        if (isVoided && !transaction.voidReason.isNullOrBlank()) {
            builder.textLine("Alasan: ${transaction.voidReason}")
        }

        builder.lineSeparator('-')

        // 4. Items List
        transaction.items.forEach { item ->
            // Line 1: Product Name (Left)
            builder.align(EscPosBuilder.Alignment.LEFT)
                .bold(true)
                .textLine(item.productName)
                .bold(false)

            // Line 2: Qty x Price = Subtotal
            val qtyStr = "${formatQty(item.quantity)} x ${formatMoney(item.priceAtTransaction)}"
            val subtotalStr = formatMoney(item.subtotal)
            builder.twoColumn(" $qtyStr", subtotalStr)
        }

        builder.lineSeparator('-')

        // 5. Totals & Payment Summary
        val grossSubtotal = transaction.items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.priceAtTransaction.multiply(item.quantity))
        }
        val totalItemSubtotal = transaction.items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.subtotal)
        }
        val itemDiscount = grossSubtotal.subtract(totalItemSubtotal).coerceAtLeast(BigDecimal.ZERO)
        val deliveryInfo = DeliveryOrderFormatter.parseDeliveryInfo(transaction.notes)
        val deliveryFee = deliveryInfo.deliveryFeeText
            ?.replace("Rp", "", ignoreCase = true)
            ?.replace(".", "")
            ?.replace(",", ".")
            ?.trim()
            ?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val baseForNoteDiscount = totalItemSubtotal.add(deliveryFee)
        val diskonNota = baseForNoteDiscount.subtract(transaction.total).coerceAtLeast(BigDecimal.ZERO)

        if (itemDiscount > BigDecimal.ZERO || diskonNota > BigDecimal.ZERO || deliveryFee > BigDecimal.ZERO) {
            builder.twoColumn("Subtotal", formatMoney(grossSubtotal))
            if (itemDiscount > BigDecimal.ZERO) {
                builder.twoColumn("Diskon Item", "-${formatMoney(itemDiscount)}")
            }
            if (diskonNota > BigDecimal.ZERO) {
                builder.twoColumn("Diskon Nota", "-${formatMoney(diskonNota)}")
            }
            if (deliveryFee > BigDecimal.ZERO) {
                builder.twoColumn("Ongkos Kirim", "+${formatMoney(deliveryFee)}")
            }
        }

        builder.bold(true)
            .twoColumn("TOTAL", formatMoney(transaction.total))
            .bold(false)

        val paymentMethodDisplay = if (transaction.paymentMethods.isNotEmpty()) {
            transaction.paymentMethods.joinToString(", ") { method ->
                when (method.lowercase()) {
                    "cash", "tunai" -> "Tunai"
                    "qris" -> "QRIS"
                    "transfer" -> "Transfer"
                    "hutang", "credit" -> "Hutang"
                    "dp" -> "Uang Muka (DP)"
                    else -> method.uppercase()
                }
            }
        } else "Tunai"
        builder.twoColumn("Metode Bayar", paymentMethodDisplay)

        val isCash = transaction.paymentMethods.any { it.lowercase() in setOf("tunai", "cash") } || transaction.paymentMethods.isEmpty()
        val tenderedAmount = if (transaction.amountTendered > BigDecimal.ZERO) transaction.amountTendered else transaction.paidAmount

        if (!isVoided) {
            if (isCash || transaction.amountTendered > BigDecimal.ZERO) {
                builder.twoColumn("Uang Diterima", formatMoney(tenderedAmount))
                if (transaction.changeAmount > BigDecimal.ZERO || isCash) {
                    builder.twoColumn("Kembalian", formatMoney(transaction.changeAmount))
                }
            } else {
                builder.twoColumn("Dibayar", formatMoney(transaction.paidAmount))
            }
            if (transaction.remainingAmount() > BigDecimal.ZERO) {
                builder.bold(true)
                    .twoColumn("Sisa Hutang", formatMoney(transaction.remainingAmount()))
                    .bold(false)
            }
        }

        builder.lineSeparator('=')

        // 6. Footer
        builder.align(EscPosBuilder.Alignment.CENTER)
        if (receiptFooter.isNotBlank()) {
            builder.textLine(receiptFooter)
        }
        builder.textLine("Simpan struk ini sebagai bukti pembayaran.")
        builder.textLine("---")

        // 7. Feed & Cut
        builder.feed(3)
        if (autoCut) {
            builder.cutPaper(fullCut = false)
        }

        return builder.build()
    }

    fun buildTestReceipt(
        storeName: String = "TB TERMINAL",
        paperSize: String = "58mm",
        openDrawer: Boolean = false,
        autoCut: Boolean = true
    ): ByteArray {
        val builder = EscPosBuilder(paperSize)

        if (openDrawer) {
            builder.openCashDrawer()
        }

        builder.align(EscPosBuilder.Alignment.CENTER)
            .bold(true)
            .doubleSize(true)
            .textLine(storeName.ifBlank { "TB TERMINAL" }.uppercase())
            .doubleSize(false)
            .bold(false)
            .textLine("Uji Coba Printer Thermal")
            .textLine("Ukuran Kertas: $paperSize")
            .lineSeparator('=')

        builder.align(EscPosBuilder.Alignment.LEFT)
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
        builder.twoColumn("Waktu Uji", now)
        builder.twoColumn("Koneksi", "Direct ESC/POS")
        builder.twoColumn("Status Printer", "NORMAL / OK")
        builder.lineSeparator('-')

        builder.bold(true).textLine("Contoh Format Belanja:").bold(false)
        builder.textLine("Semen Tiga Roda 50kg")
        builder.twoColumn(" 1 x Rp 65.000", "Rp 65.000")
        builder.textLine("Paku Kayu 3 Inch")
        builder.twoColumn(" 2 x Rp 15.000", "Rp 30.000")
        builder.lineSeparator('-')

        builder.bold(true).twoColumn("TOTAL", "Rp 95.000").bold(false)
        builder.lineSeparator('=')

        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.textLine("Printer thermal siap digunakan!")
        builder.textLine("Aplikasi TB Terminal POS")

        builder.feed(3)
        if (autoCut) {
            builder.cutPaper(fullCut = false)
        }

        return builder.build()
    }

    fun buildCashDrawerPulse(): ByteArray {
        return EscPosBuilder("58mm").openCashDrawer().build()
    }
}
