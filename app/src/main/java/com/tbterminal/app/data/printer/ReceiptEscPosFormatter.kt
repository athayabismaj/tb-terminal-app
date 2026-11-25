package com.tbterminal.app.data.printer

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.model.remainingAmount
import java.math.BigDecimal
import java.math.RoundingMode
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
        if (qty.compareTo(BigDecimal.ZERO) == 0) return "0"
        val stripped = qty.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            stripped.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
        } else {
            stripped.toPlainString()
        }
    }

    internal fun formatItemPriceLines(
        quantity: BigDecimal,
        unitName: String?,
        unitPrice: BigDecimal,
        subtotal: BigDecimal,
        maxCharsPerLine: Int,
    ): List<String> {
        val qtyText = formatQty(quantity)
        val cleanUnit = unitName?.trim()?.takeIf(String::isNotBlank)
        val priceStr = formatMoney(unitPrice)
        val subtotalStr = formatMoney(subtotal)
        val qtyUnitText = if (cleanUnit != null) "$qtyText $cleanUnit" else qtyText
        val singleLineLeft = " $qtyUnitText x $priceStr"

        if (singleLineLeft.length + subtotalStr.length + 1 <= maxCharsPerLine) {
            val spaceCount = maxCharsPerLine - singleLineLeft.length - subtotalStr.length
            return listOf(singleLineLeft + " ".repeat(spaceCount) + subtotalStr)
        }

        val priceSuffix = " x $priceStr"
        val leftLine = if (singleLineLeft.length <= maxCharsPerLine) {
            singleLineLeft
        } else if (cleanUnit != null) {
            val fixedLen = 1 + qtyText.length + 1 + priceSuffix.length
            val availableUnitChars = maxCharsPerLine - fixedLen
            if (availableUnitChars >= 1) {
                " $qtyText ${cleanUnit.take(availableUnitChars)}$priceSuffix"
            } else {
                return wrapEscPosLeftAndRight(" $qtyText$priceSuffix", subtotalStr, maxCharsPerLine)
            }
        } else {
            return wrapEscPosLeftAndRight(singleLineLeft, subtotalStr, maxCharsPerLine)
        }

        val rightLine = if (subtotalStr.length <= maxCharsPerLine) {
            " ".repeat(maxCharsPerLine - subtotalStr.length) + subtotalStr
        } else {
            subtotalStr.take(maxCharsPerLine)
        }
        return listOf(leftLine, rightLine)
    }

    private fun wrapEscPosLeftAndRight(
        left: String,
        right: String,
        maxCharsPerLine: Int,
    ): List<String> {
        val lines = left.chunked(maxCharsPerLine).toMutableList()
        val rightLine = if (right.length <= maxCharsPerLine) {
            " ".repeat(maxCharsPerLine - right.length) + right
        } else {
            right.take(maxCharsPerLine)
        }
        lines += rightLine
        return lines
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

            // Line 2: Qty [Unit] x Price = Subtotal (respecting 32/48 column width)
            formatItemPriceLines(
                quantity = item.quantity,
                unitName = item.unitName,
                unitPrice = item.priceAtTransaction,
                subtotal = item.subtotal,
                maxCharsPerLine = builder.maxCharsPerLine,
            ).forEach { line ->
                builder.textLine(line)
            }
            item.note?.trim()?.takeIf { it.isNotBlank() }?.let { note ->
                builder.textLine("  Cat: $note")
            }
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
        val excludedTax = if (transaction.taxEnabled && transaction.taxMode.equals("EXCLUDE", ignoreCase = true)) {
            transaction.taxAmount
        } else {
            BigDecimal.ZERO
        }
        val diskonNota = if (transaction.transactionDiscountAmount > BigDecimal.ZERO) {
            transaction.transactionDiscountAmount
        } else {
            val baseForNoteDiscount = totalItemSubtotal.add(deliveryFee).add(excludedTax).add(transaction.roundingAmount)
            baseForNoteDiscount.subtract(transaction.total).coerceAtLeast(BigDecimal.ZERO)
        }
        val hasTaxInfo = transaction.taxEnabled &&
            (transaction.taxAmount > BigDecimal.ZERO || transaction.taxableBase > BigDecimal.ZERO)
        val hasRounding = transaction.roundingAmount.compareTo(BigDecimal.ZERO) != 0

        if (itemDiscount > BigDecimal.ZERO || diskonNota > BigDecimal.ZERO || deliveryFee > BigDecimal.ZERO || hasTaxInfo || hasRounding) {
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
            if (hasTaxInfo) {
                val taxName = transaction.taxLabel?.takeIf { it.isNotBlank() } ?: "PPN"
                val rateLabel = "$taxName ${transaction.taxRate.stripTrailingZeros().toPlainString()}%"
                if (transaction.taxableBase > BigDecimal.ZERO) {
                    builder.twoColumn("DPP", formatMoney(transaction.taxableBase))
                }
                if (transaction.taxMode.equals("INCLUDE", ignoreCase = true)) {
                    builder.twoColumn("Incl. $rateLabel", formatMoney(transaction.taxAmount))
                } else {
                    builder.twoColumn(rateLabel, "+${formatMoney(transaction.taxAmount)}")
                }
            }
            if (hasRounding) {
                val sign = if (transaction.roundingAmount > BigDecimal.ZERO) "+" else "-"
                builder.twoColumn("Pembulatan", "$sign${formatMoney(transaction.roundingAmount.abs())}")
            }
        }

        builder.bold(true)
            .twoColumn("TOTAL", formatMoney(transaction.total))
            .bold(false)

        val checkoutPayments = transaction.payments.filter {
            it.amount > BigDecimal.ZERO &&
                !it.paymentSource.equals("VOID_REVERSAL", ignoreCase = true) &&
                !it.paymentSource.equals("REFUND_REVERSAL", ignoreCase = true)
        }
        fun formatMethodName(method: String): String = when (method.lowercase()) {
            "cash", "tunai" -> "Tunai"
            "qris" -> "QRIS"
            "transfer", "bank_transfer" -> "Transfer"
            "hutang", "credit" -> "Hutang"
            "dp", "down_payment" -> "Uang Muka (DP)"
            else -> method.uppercase()
        }

        val paymentMethodDisplay = if (checkoutPayments.isNotEmpty()) {
            checkoutPayments.map { formatMethodName(it.method) }.distinct().joinToString(" + ")
        } else if (transaction.paymentMethods.isNotEmpty()) {
            transaction.paymentMethods.joinToString(", ") { formatMethodName(it) }
        } else {
            "Tunai"
        }
        builder.twoColumn("Metode Bayar", paymentMethodDisplay)

        if (checkoutPayments.size > 1 || checkoutPayments.any { !it.reference.isNullOrBlank() }) {
            checkoutPayments.forEach { p ->
                builder.twoColumn("  - ${formatMethodName(p.method)}", formatMoney(p.amount))
                p.reference?.trim()?.takeIf { it.isNotBlank() }?.let { ref ->
                    builder.textLine("    Ref: $ref")
                }
            }
        }

        val isCash = checkoutPayments.any { it.method.lowercase() in setOf("tunai", "cash") } ||
            transaction.paymentMethods.any { it.lowercase() in setOf("tunai", "cash") } ||
            (checkoutPayments.isEmpty() && transaction.paymentMethods.isEmpty())
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
