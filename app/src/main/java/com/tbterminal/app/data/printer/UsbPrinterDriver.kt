package com.tbterminal.app.data.printer

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsbPrinterDriver {

    fun getConnectedUsbPrinters(context: Context): List<PrinterDeviceItem> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        val deviceList = usbManager.deviceList ?: return emptyList()

        val results = mutableListOf<PrinterDeviceItem>()
        for ((name, device) in deviceList) {
            val isPrinter = (0 until device.interfaceCount).any { i ->
                val iface = device.getInterface(i)
                iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER || iface.interfaceClass == 7
            }
            if (isPrinter || device.vendorId != 0) {
                val displayName = device.productName?.takeIf(String::isNotBlank)
                    ?: device.deviceName.takeIf(String::isNotBlank)
                    ?: "USB Printer (VID:${device.vendorId})"
                results.add(
                    PrinterDeviceItem(
                        name = displayName,
                        address = name,
                        type = PrinterConnectionType.USB
                    )
                )
            }
        }
        return results
    }

    suspend fun print(context: Context, deviceName: String, data: ByteArray): PrinterResult = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return@withContext PrinterResult.Error("Layanan USB tidak tersedia pada perangkat.")

        val deviceList = usbManager.deviceList
        val targetDevice: UsbDevice = deviceList[deviceName]
            ?: deviceList.values.firstOrNull { it.deviceName == deviceName || it.productName == deviceName }
            ?: return@withContext PrinterResult.Error("Printer USB ($deviceName) tidak ditemukan. Periksa kabel USB.")

        if (!usbManager.hasPermission(targetDevice)) {
            return@withContext PrinterResult.Error("Izin akses printer USB belum diberikan oleh sistem Android.")
        }

        var usbInterface: UsbInterface? = null
        var endpointOut: UsbEndpoint? = null

        // Find bulk OUT endpoint on the device
        for (i in 0 until targetDevice.interfaceCount) {
            val iface = targetDevice.getInterface(i)
            for (j in 0 until iface.endpointCount) {
                val endpoint = iface.getEndpoint(j)
                if (endpoint.direction == UsbConstants.USB_DIR_OUT &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                ) {
                    usbInterface = iface
                    endpointOut = endpoint
                    break
                }
            }
            if (endpointOut != null) break
        }

        if (usbInterface == null || endpointOut == null) {
            return@withContext PrinterResult.Error("Interface transfer data printer USB tidak ditemukan.")
        }

        val connection = usbManager.openDevice(targetDevice)
            ?: return@withContext PrinterResult.Error("Gagal membuka koneksi ke printer USB.")

        try {
            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext PrinterResult.Error("Gagal mengklaim interface printer USB.")
            }

            val transferred = connection.bulkTransfer(endpointOut, data, data.size, 5000)
            if (transferred < 0) {
                PrinterResult.Error("Gagal mengirim data cetak ke printer USB (transfer timeout).")
            } else {
                PrinterResult.Success
            }
        } catch (e: Exception) {
            PrinterResult.Error("Kesalahan saat mencetak ke USB: ${e.localizedMessage}")
        } finally {
            runCatching {
                connection.releaseInterface(usbInterface)
                connection.close()
            }
        }
    }
}
