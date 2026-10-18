package com.tbterminal.app.data.printer

import android.content.Context
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThermalPrinterManager(
    private val localAppSettingsDataSource: LocalAppSettingsDataSource
) {

    suspend fun getAvailablePrinters(context: Context, type: PrinterConnectionType): List<PrinterDeviceItem> {
        return withContext(Dispatchers.IO) {
            when (type) {
                PrinterConnectionType.BLUETOOTH -> BluetoothPrinterDriver.getPairedDevices(context)
                PrinterConnectionType.USB -> UsbPrinterDriver.getConnectedUsbPrinters(context)
                PrinterConnectionType.SYSTEM -> emptyList()
            }
        }
    }

    suspend fun printReceipt(
        context: Context,
        transaction: CashTransactionDetail,
        storeSettings: StoreSettings? = null
    ): PrinterResult = withContext(Dispatchers.IO) {
        val prefs = localAppSettingsDataSource.getDevicePreferences()

        if (prefs.printerConnectionType == "SYSTEM") {
            // Caller should open Android Print Framework dialog
            return@withContext PrinterResult.Error("USE_SYSTEM_PRINT")
        }

        val receiptBytes = ReceiptEscPosFormatter.buildReceiptBytes(
            transaction = transaction,
            storeName = storeSettings?.storeName ?: "TB TERMINAL",
            storeAddress = storeSettings?.address.orEmpty(),
            storePhone = storeSettings?.phone.orEmpty(),
            receiptHeader = storeSettings?.receiptHeader.orEmpty(),
            receiptFooter = storeSettings?.receiptFooter.orEmpty().ifBlank { "Terima kasih telah berbelanja." },
            paperSize = prefs.paperSize,
            openDrawer = prefs.openCashDrawer,
            autoCut = prefs.autoCutPaper
        )

        sendDataToConfiguredPrinter(context, prefs.printerConnectionType, prefs.printerBluetoothAddress, receiptBytes)
    }

    suspend fun printTestReceipt(
        context: Context,
        storeSettings: StoreSettings? = null
    ): PrinterResult = withContext(Dispatchers.IO) {
        val prefs = localAppSettingsDataSource.getDevicePreferences()

        if (prefs.printerConnectionType == "SYSTEM") {
            return@withContext PrinterResult.Error("USE_SYSTEM_PRINT")
        }

        val testBytes = ReceiptEscPosFormatter.buildTestReceipt(
            storeName = storeSettings?.storeName ?: "TB TERMINAL",
            paperSize = prefs.paperSize,
            openDrawer = prefs.openCashDrawer,
            autoCut = prefs.autoCutPaper
        )

        sendDataToConfiguredPrinter(context, prefs.printerConnectionType, prefs.printerBluetoothAddress, testBytes)
    }

    suspend fun testOpenCashDrawer(context: Context): PrinterResult = withContext(Dispatchers.IO) {
        val prefs = localAppSettingsDataSource.getDevicePreferences()
        if (prefs.printerConnectionType == "SYSTEM") {
            return@withContext PrinterResult.Error("Buka laci kasir hanya didukung pada printer thermal Bluetooth atau USB.")
        }
        val pulseBytes = ReceiptEscPosFormatter.buildCashDrawerPulse()
        sendDataToConfiguredPrinter(context, prefs.printerConnectionType, prefs.printerBluetoothAddress, pulseBytes)
    }

    private suspend fun sendDataToConfiguredPrinter(
        context: Context,
        connectionType: String,
        address: String,
        data: ByteArray
    ): PrinterResult {
        return when (connectionType) {
            "BLUETOOTH" -> {
                if (address.isBlank()) {
                    PrinterResult.Error("Printer Bluetooth belum dipilih. Silakan pilih printer di menu Pengaturan.")
                } else {
                    BluetoothPrinterDriver.print(context, address, data)
                }
            }
            "USB" -> {
                if (address.isBlank()) {
                    // Try first connected printer
                    val usbDevices = UsbPrinterDriver.getConnectedUsbPrinters(context)
                    if (usbDevices.isEmpty()) {
                        PrinterResult.Error("Tidak ada printer USB yang terdeteksi. Pastikan kabel OTG terhubung.")
                    } else {
                        UsbPrinterDriver.print(context, usbDevices.first().address, data)
                    }
                } else {
                    UsbPrinterDriver.print(context, address, data)
                }
            }
            else -> PrinterResult.Error("Tipe koneksi printer tidak dikenali: $connectionType")
        }
    }
}
