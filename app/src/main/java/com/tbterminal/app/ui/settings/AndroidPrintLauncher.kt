package com.tbterminal.app.ui.settings

import android.content.Context
import android.graphics.Paint
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
import java.io.FileOutputStream

fun launchAndroidPrintDialog(context: Context, paperSize: String): Boolean {
    return runCatching {
        val widthMils = if (paperSize == "80mm") 3_150 else 2_283
        val mediaSize = PrintAttributes.MediaSize(
            "TB_TERMINAL_$paperSize",
            "Struk $paperSize",
            widthMils,
            5_000
        )
        val attributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            "TB Terminal - Uji Printer",
            SettingsTestPrintAdapter(context, paperSize),
            attributes
        )
    }.isSuccess
}

private class SettingsTestPrintAdapter(
    private val context: Context,
    private val paperSize: String
) : PrintDocumentAdapter() {
    private var document: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        document?.close()
        document = PrintedPdfDocument(context, newAttributes)
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("tb-terminal-printer-test.pdf")
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
        val pdf = document ?: run {
            callback.onWriteFailed("Dokumen uji cetak belum siap.")
            return
        }
        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }
        runCatching {
            val page = pdf.startPage(0)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            page.canvas.drawText("TB TERMINAL", 20f, 40f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.MONOSPACE
            page.canvas.drawText("Uji Android Print Framework", 20f, 66f, paint)
            page.canvas.drawText("Ukuran kertas: $paperSize", 20f, 88f, paint)
            page.canvas.drawText("----------------------------", 20f, 110f, paint)
            page.canvas.drawText("Printer siap digunakan.", 20f, 132f, paint)
            pdf.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(pdf::writeTo)
        }.onSuccess {
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }.onFailure {
            callback.onWriteFailed("Dokumen uji cetak gagal dibuat.")
        }
    }

    override fun onFinish() {
        document?.close()
        document = null
    }
}
