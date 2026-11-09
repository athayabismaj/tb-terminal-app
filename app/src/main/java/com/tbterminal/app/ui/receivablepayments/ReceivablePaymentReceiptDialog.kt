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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    val slate50 = Color(0xFFF8FAFC)
    val slate100 = Color(0xFFF1F5F9)
    val slate200 = Color(0xFFE2E8F0)
    val slate500 = Color(0xFF64748B)
    val slate600 = Color(0xFF475569)
    val slate700 = Color(0xFF334155)
    val slate900 = Color(0xFF0F172A)
    val brand50 = Color(0xFFF0FDF4)
    val brand100 = Color(0xFFDCFCE7)
    val brand700 = Color(0xFF15803D)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(brand100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = brand700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (receipt.entryType == "REVERSAL") "Bukti Reversal" else "Bukti Pembayaran",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slate900
                                )
                                Text(
                                    text = "Pembayaran piutang tercatat di sistem",
                                    fontSize = 11.sp,
                                    color = slate500
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(slate100)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = slate500,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nominal Banner Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = brand50,
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TOTAL NOMINAL PEMBAYARAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = slate500,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = receipt.amount.receiptCurrency(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = brand700
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Text(
                                    text = "${receipt.method.uppercase()} • ${receipt.entryType}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brand700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detail Table
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = slate50,
                        border = BorderStroke(1.dp, slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReceiptLine("Nomor Bukti", receipt.paymentNumber, isMonospace = true)
                            HorizontalDivider(color = slate200, thickness = 0.5.dp)
                            ReceiptLine("Pelanggan", receipt.customerName)
                            ReceiptLine("Tanggal", receipt.paidAt.receiptDate())
                            ReceiptLine("Kasir / Penerima", receipt.receivedByName)
                            HorizontalDivider(color = slate200, thickness = 0.5.dp)
                            ReceiptLine("Saldo Sebelum", receipt.balanceBefore.receiptCurrency())
                            ReceiptLine("Saldo Sesudah", receipt.balanceAfter.receiptCurrency())
                            if (!receipt.reference.isNullOrBlank()) {
                                HorizontalDivider(color = slate200, thickness = 0.5.dp)
                                ReceiptLine("Referensi", receipt.reference)
                            }
                            if (!receipt.notes.isNullOrBlank()) {
                                HorizontalDivider(color = slate200, thickness = 0.5.dp)
                                ReceiptLine("Catatan", receipt.notes)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = slate100,
                                contentColor = slate700
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                "Tutup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = slate700
                            )
                        }

                        Button(
                            onClick = { printReceivablePaymentReceipt(context, receipt) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = brand700)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Print,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak Struk", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
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
