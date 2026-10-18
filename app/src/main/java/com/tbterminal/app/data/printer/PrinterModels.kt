package com.tbterminal.app.data.printer

enum class PrinterConnectionType(val label: String) {
    BLUETOOTH("Bluetooth"),
    USB("USB OTG"),
    SYSTEM("Android Print Dialog")
}

data class PrinterDeviceItem(
    val name: String,
    val address: String,
    val type: PrinterConnectionType
)

sealed class PrinterResult {
    data object Success : PrinterResult()
    data class Error(val message: String) : PrinterResult()
}
