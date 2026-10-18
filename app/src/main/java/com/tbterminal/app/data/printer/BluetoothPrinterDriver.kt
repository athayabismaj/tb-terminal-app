package com.tbterminal.app.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

object BluetoothPrinterDriver {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<PrinterDeviceItem> {
        val adapter = getBluetoothAdapter(context) ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        if (!hasBluetoothPermission(context)) return emptyList()

        return runCatching {
            adapter.bondedDevices.map { device ->
                PrinterDeviceItem(
                    name = device.name ?: device.address,
                    address = device.address,
                    type = PrinterConnectionType.BLUETOOTH
                )
            }.sortedBy { it.name }
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    suspend fun print(context: Context, macAddress: String, data: ByteArray): PrinterResult = withContext(Dispatchers.IO) {
        if (macAddress.isBlank()) {
            return@withContext PrinterResult.Error("Alamat printer Bluetooth belum dipilih.")
        }
        val adapter = getBluetoothAdapter(context)
            ?: return@withContext PrinterResult.Error("Perangkat ini tidak memiliki adaptor Bluetooth.")

        if (!adapter.isEnabled) {
            return@withContext PrinterResult.Error("Bluetooth pada perangkat belum diaktifkan.")
        }

        if (!hasBluetoothPermission(context)) {
            return@withContext PrinterResult.Error("Izin Bluetooth belum diberikan pada aplikasi.")
        }

        var socket: BluetoothSocket? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()

            // Small delay to ensure all bytes buffer to hardware printer
            kotlinx.coroutines.delay(150)
            PrinterResult.Success
        } catch (e: SecurityException) {
            PrinterResult.Error("Izin akses Bluetooth ditolak sistem: ${e.localizedMessage}")
        } catch (e: IOException) {
            PrinterResult.Error("Gagal terhubung ke printer Bluetooth ($macAddress). Pastikan printer menyala dan dalam jangkauan.")
        } catch (e: Exception) {
            PrinterResult.Error("Kesalahan saat mencetak ke Bluetooth: ${e.localizedMessage ?: "Unknown error"}")
        } finally {
            runCatching { socket?.close() }
        }
    }
}
