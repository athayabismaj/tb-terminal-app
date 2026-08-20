package com.tbterminal.app.ui.receivablepayments

import android.content.Context
import android.graphics.Paint
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ReceivablePaymentReceiptDialog(
    receipt: ReceivablePaymentReceipt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (receipt.entryType == "REVERSAL") "Bukti Reversal" else "Bukti Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptLine("Nomor", receipt.paymentNumber)
                ReceiptLine("Pelanggan", receipt.customerName)
                ReceiptLine("Tanggal", receipt.paidAt.receiptDate())
                ReceiptLine("Jenis", receipt.entryType)
                ReceiptLine("Metode", receipt.method.uppercase())
                ReceiptLine("Nominal", receipt.amount.receiptCurrency())
                ReceiptLine("Penerima", receipt.receivedByName)
                ReceiptLine("Saldo sebelum", receipt.balanceBefore.receiptCurrency())
                ReceiptLine("Saldo sesudah", receipt.balanceAfter.receiptCurrency())
                ReceiptLine("Referensi", receipt.reference ?: "-")
                ReceiptLine("Catatan", receipt.notes ?: "-")
            }
        },
        confirmButton = {
            Button(onClick = { printReceivablePaymentReceipt(context, receipt) }) {
                Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Cetak")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

internal fun printReceivablePaymentReceipt(context: Context, receipt: ReceivablePaymentReceipt) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print(
        "Bukti-${receipt.paymentNumber}",
        ReceivableReceiptPrintAdapter(context, receipt),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
    )
}

private class ReceivableReceiptPrintAdapter(
    private val context: Context,
    private val receipt: ReceivablePaymentReceipt
) : PrintDocumentAdapter() {
    private lateinit var attributes: PrintAttributes

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("bukti-${receipt.paymentNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = PrintedPdfDocument(context, attributes)
        try {
            if (cancellationSignal.isCanceled) return callback.onWriteCancelled()
            val page = document.startPage(0)
            drawReceipt(page, receipt)
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message ?: "Bukti pembayaran gagal dicetak")
        } finally {
            document.close()
        }
    }

    private fun drawReceipt(page: PdfDocument.Page, receipt: ReceivablePaymentReceipt) {
        val paint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 12f }
        val bold = Paint(paint).apply { isFakeBoldText = true; textSize = 17f }
        var y = 48f
        page.canvas.drawText("BUKTI ${receipt.entryType}", 36f, y, bold)
        y += 30f
        val lines = listOf(
            "Nomor: ${receipt.paymentNumber}",
            "Pelanggan: ${receipt.customerName}",
            "Tanggal: ${receipt.paidAt.receiptDate()}",
            "Metode: ${receipt.method.uppercase()}",
            "Nominal: ${receipt.amount.receiptCurrency()}",
            "Penerima: ${receipt.receivedByName}",
            "Saldo sebelum: ${receipt.balanceBefore.receiptCurrency()}",
            "Saldo sesudah: ${receipt.balanceAfter.receiptCurrency()}",
            "Referensi: ${receipt.reference ?: "-"}",
            "Catatan: ${receipt.notes ?: "-"}"
        )
        lines.forEach { line ->
            page.canvas.drawText(line.take(90), 36f, y, paint)
            y += 22f
        }
    }
}

private fun java.math.BigDecimal.receiptCurrency(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.receiptDate(): String = runCatching {
    OffsetDateTime.parse(this).format(
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))
    )
}.getOrDefault(this)
