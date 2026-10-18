package com.tbterminal.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.model.StoreSettings
import com.tbterminal.app.data.model.UpdateStoreSettingsCommand
import com.tbterminal.app.data.printer.PrinterConnectionType
import com.tbterminal.app.data.printer.PrinterDeviceItem
import com.tbterminal.app.data.printer.PrinterResult
import com.tbterminal.app.data.printer.ThermalPrinterManager
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.repository.SystemRepository
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val storeName: String = "TB Terminal",
    val address: String = "",
    val phone: String = "",
    val receiptHeader: String = "TB Terminal",
    val receiptFooter: String = "Terima kasih telah berbelanja.",
    val printerSize: String = "58mm",
    val cashTolerance: String = "0",
    val autoLockMinutes: String = "15",
    val autoPrintReceipt: Boolean = true,
    val selectedPrinterName: String = "Bluetooth Printer",
    val printerConnectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    val printerBluetoothAddress: String = "",
    val printerBluetoothName: String = "",
    val autoCutPaper: Boolean = true,
    val openCashDrawer: Boolean = false,
    val availablePrinters: List<PrinterDeviceItem> = emptyList(),
    val isScanningPrinters: Boolean = false,
    val isPrintingTest: Boolean = false
)

class SettingsViewModel(
    private val systemRepository: SystemRepository,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val loadRemoteSettings: Boolean = true,
) : ViewModel() {
    private val thermalPrinterManager = ThermalPrinterManager(localAppSettingsDataSource)
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = loadRemoteSettings))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (loadRemoteSettings) loadSettings()
        loadLocalPreferences()
    }

    fun reload() {
        if (loadRemoteSettings) loadSettings() else loadLocalPreferences()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            when (val result = systemRepository.getStoreSettings()) {
                is NetworkResult.Success -> {
                    val response = result.data
                    val settings = response.data
                    if (response.success && settings != null) {
                        applyStoreSettings(settings)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = response.message ?: response.error ?: "Pengaturan sistem gagal dimuat."
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.e.message ?: "Pengaturan sistem gagal dimuat karena koneksi bermasalah."
                        )
                    }
                }
            }
        }
    }

    fun saveStoreSettings() {
        val state = _uiState.value
        val storeName = state.storeName.trim()
        if (storeName.isBlank()) {
            _uiState.update { it.copy(error = "Nama toko tidak boleh kosong.", message = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            val command = UpdateStoreSettingsCommand(
                storeName = storeName,
                address = state.address.trim().takeIf(String::isNotBlank),
                phone = state.phone.trim().takeIf(String::isNotBlank),
                receiptHeader = state.receiptHeader.trim().takeIf(String::isNotBlank),
                receiptFooter = state.receiptFooter.trim().takeIf(String::isNotBlank),
                printerSize = state.printerSize
            )

            when (val result = systemRepository.updateStoreSettings(command)) {
                is NetworkResult.Success -> {
                    val response = result.data
                    val settings = response.data
                    if (response.success && settings != null) {
                        applyStoreSettings(settings, "Pengaturan toko dan struk berhasil disimpan.")
                    } else {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = response.message ?: response.error ?: "Pengaturan gagal disimpan."
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.e.message ?: "Pengaturan gagal disimpan karena koneksi bermasalah."
                        )
                    }
                }
            }
        }
    }

    fun saveLocalPreferences() {
        val state = _uiState.value
        val validated = validateDevicePreferences(
            paperSize = state.printerSize,
            cashTolerance = state.cashTolerance,
            autoLockMinutes = state.autoLockMinutes,
            printerConnectionType = state.printerConnectionType.name,
            printerBluetoothAddress = state.printerBluetoothAddress,
            printerBluetoothName = state.printerBluetoothName,
            autoCutPaper = state.autoCutPaper,
            openCashDrawer = state.openCashDrawer,
        )
        if (validated.error != null) {
            _uiState.update { it.copy(error = validated.error, message = null) }
            return
        }
        val preferences = validated.preferences ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            runCatching {
                localAppSettingsDataSource.setDevicePreferences(
                    preferences.copy(autoPrintReceipt = state.autoPrintReceipt)
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        cashTolerance = preferences.cashTolerance,
                        autoLockMinutes = preferences.autoLockMinutes.toString(),
                        message = "Preferensi perangkat berhasil disimpan pada terminal ini."
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isSaving = false, error = "Preferensi perangkat gagal disimpan.")
                }
            }
        }
    }

    private fun loadLocalPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            runCatching { localAppSettingsDataSource.getDevicePreferences() }
                .onSuccess { preferences ->
                    val connType = when (preferences.printerConnectionType) {
                        "USB" -> PrinterConnectionType.USB
                        "SYSTEM" -> PrinterConnectionType.SYSTEM
                        else -> PrinterConnectionType.BLUETOOTH
                    }
                    val displayName = when (connType) {
                        PrinterConnectionType.BLUETOOTH -> preferences.printerBluetoothName.ifBlank { "Belum ada printer Bluetooth" }
                        PrinterConnectionType.USB -> "Printer USB OTG"
                        PrinterConnectionType.SYSTEM -> "Android Print Dialog"
                    }
                    _uiState.update {
                        it.copy(
                            printerSize = preferences.paperSize,
                            printerConnectionType = connType,
                            printerBluetoothAddress = preferences.printerBluetoothAddress,
                            printerBluetoothName = preferences.printerBluetoothName,
                            selectedPrinterName = displayName,
                            autoCutPaper = preferences.autoCutPaper,
                            openCashDrawer = preferences.openCashDrawer,
                            cashTolerance = preferences.cashTolerance,
                            autoLockMinutes = preferences.autoLockMinutes.toString(),
                            autoPrintReceipt = preferences.autoPrintReceipt
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(error = "Preferensi perangkat gagal dimuat.") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun onStoreNameChanged(value: String) = _uiState.update { it.copy(storeName = value, error = null) }
    fun onAddressChanged(value: String) = _uiState.update { it.copy(address = value, error = null) }
    fun onPhoneChanged(value: String) = _uiState.update { it.copy(phone = value, error = null) }
    fun onReceiptHeaderChanged(value: String) = _uiState.update { it.copy(receiptHeader = value, error = null) }
    fun onReceiptFooterChanged(value: String) = _uiState.update { it.copy(receiptFooter = value, error = null) }
    fun onPrinterSizeChanged(value: String) = _uiState.update { it.copy(printerSize = value, error = null) }
    fun onCashToleranceChanged(value: String) = _uiState.update {
        it.copy(cashTolerance = sanitizeMoneyInput(value), error = null)
    }
    fun onAutoLockMinutesChanged(value: String) = _uiState.update { it.copy(autoLockMinutes = value.filter(Char::isDigit), error = null) }
    fun onAutoPrintReceiptChanged(value: Boolean) = _uiState.update { it.copy(autoPrintReceipt = value, error = null) }

    fun onPrinterConnectionTypeChanged(type: PrinterConnectionType) {
        _uiState.update {
            val displayName = when (type) {
                PrinterConnectionType.BLUETOOTH -> it.printerBluetoothName.ifBlank { "Belum ada printer Bluetooth" }
                PrinterConnectionType.USB -> "Printer USB OTG"
                PrinterConnectionType.SYSTEM -> "Android Print Dialog"
            }
            it.copy(
                printerConnectionType = type,
                selectedPrinterName = displayName,
                error = null
            )
        }
    }

    fun onPrinterDeviceSelected(device: PrinterDeviceItem) {
        _uiState.update {
            it.copy(
                printerBluetoothAddress = device.address,
                printerBluetoothName = device.name,
                selectedPrinterName = device.name,
                error = null
            )
        }
    }

    fun onAutoCutPaperChanged(value: Boolean) = _uiState.update { it.copy(autoCutPaper = value, error = null) }
    fun onOpenCashDrawerChanged(value: Boolean) = _uiState.update { it.copy(openCashDrawer = value, error = null) }

    fun scanPrinters(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningPrinters = true, error = null) }
            val devices = thermalPrinterManager.getAvailablePrinters(context, _uiState.value.printerConnectionType)
            _uiState.update {
                it.copy(
                    availablePrinters = devices,
                    isScanningPrinters = false,
                    message = if (devices.isEmpty()) "Tidak ada perangkat printer yang ditemukan." else "Ditemukan ${devices.size} perangkat."
                )
            }
        }
    }

    fun testPrint(context: Context) {
        val state = _uiState.value
        if (state.printerConnectionType == PrinterConnectionType.SYSTEM) {
            if (launchAndroidPrintDialog(context, state.printerSize)) {
                onPrinterFrameworkOpened()
            } else {
                onPrinterFrameworkFailed()
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingTest = true, error = null, message = null) }
            val store = StoreSettings(
                storeName = state.storeName,
                address = state.address,
                phone = state.phone,
                receiptHeader = state.receiptHeader,
                receiptFooter = state.receiptFooter,
                printerSize = state.printerSize
            )
            val result = thermalPrinterManager.printTestReceipt(context, store)
            _uiState.update {
                when (result) {
                    is PrinterResult.Success -> it.copy(
                        isPrintingTest = false,
                        message = "Struk uji coba berhasil dikirim ke printer!"
                    )
                    is PrinterResult.Error -> it.copy(
                        isPrintingTest = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun testCashDrawer(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }
            val result = thermalPrinterManager.testOpenCashDrawer(context)
            _uiState.update {
                when (result) {
                    is PrinterResult.Success -> it.copy(
                        message = "Perintah buka laci kasir berhasil dikirim!"
                    )
                    is PrinterResult.Error -> it.copy(
                        error = result.message
                    )
                }
            }
        }
    }

    fun onPrinterFrameworkOpened() = _uiState.update {
        it.copy(
            selectedPrinterName = "Android Print Framework",
            message = "Dialog cetak Android dibuka. Pilih printer dari layanan cetak perangkat.",
            error = null
        )
    }

    fun onPrinterFrameworkFailed() = _uiState.update {
        it.copy(error = "Layanan cetak Android tidak tersedia pada perangkat ini.", message = null)
    }

    private fun applyStoreSettings(settings: StoreSettings, message: String? = null) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSaving = false,
                error = null,
                message = message,
                storeName = settings.storeName,
                address = settings.address.orEmpty(),
                phone = settings.phone.orEmpty(),
                receiptHeader = settings.receiptHeader.orEmpty(),
                receiptFooter = settings.receiptFooter.orEmpty(),
                printerSize = it.printerSize
            )
        }
    }

    companion object {
        fun factory(
            systemRepository: SystemRepository,
            localAppSettingsDataSource: LocalAppSettingsDataSource,
            loadRemoteSettings: Boolean = true,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                SettingsViewModel(systemRepository, localAppSettingsDataSource, loadRemoteSettings)
            }
        }
    }
}

internal data class DevicePreferencesValidation(
    val preferences: DevicePreferences? = null,
    val error: String? = null
)

internal fun validateDevicePreferences(
    paperSize: String,
    cashTolerance: String,
    autoLockMinutes: String,
    printerConnectionType: String = "BLUETOOTH",
    printerBluetoothAddress: String = "",
    printerBluetoothName: String = "",
    autoCutPaper: Boolean = true,
    openCashDrawer: Boolean = false,
): DevicePreferencesValidation {
    if (paperSize !in setOf("58mm", "80mm")) {
        return DevicePreferencesValidation(error = "Ukuran kertas harus 58mm atau 80mm.")
    }
    val normalizedTolerance = cashTolerance.trim().replace(',', '.')
    val tolerance = normalizedTolerance.toBigDecimalOrNull()
        ?: return DevicePreferencesValidation(error = "Toleransi selisih kas harus berupa angka.")
    if (tolerance < BigDecimal.ZERO || tolerance.scale() > 2) {
        return DevicePreferencesValidation(error = "Toleransi kas tidak boleh negatif dan maksimal dua desimal.")
    }
    val lockMinutes = autoLockMinutes.toIntOrNull()
        ?.takeIf { it in 1..120 }
        ?: return DevicePreferencesValidation(error = "Auto-lock harus antara 1 dan 120 menit.")
    return DevicePreferencesValidation(
        preferences = DevicePreferences(
            paperSize = paperSize,
            printerConnectionType = printerConnectionType,
            printerBluetoothAddress = printerBluetoothAddress,
            printerBluetoothName = printerBluetoothName,
            autoCutPaper = autoCutPaper,
            openCashDrawer = openCashDrawer,
            cashTolerance = tolerance.stripTrailingZeros().toPlainString(),
            autoLockMinutes = lockMinutes
        )
    )
}

private fun sanitizeMoneyInput(value: String): String {
    val normalized = value.replace(',', '.')
    val result = StringBuilder()
    var separatorSeen = false
    var decimals = 0
    normalized.forEach { character ->
        when {
            character.isDigit() && (!separatorSeen || decimals < 2) -> {
                result.append(character)
                if (separatorSeen) decimals += 1
            }
            character == '.' && !separatorSeen -> {
                result.append(character)
                separatorSeen = true
            }
        }
    }
    return result.toString()
}
