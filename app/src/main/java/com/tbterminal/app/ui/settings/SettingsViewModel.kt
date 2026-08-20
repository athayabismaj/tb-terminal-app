package com.tbterminal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.model.StoreSettings
import com.tbterminal.app.data.model.UpdateStoreSettingsCommand
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
    val defaultCreditLimit: String = "1000000",
    val defaultTermDays: String = "30",
    val cashTolerance: String = "0",
    val autoLockMinutes: String = "15",
    val autoPrintReceipt: Boolean = true,
    val barcodeScannerEnabled: Boolean = true,
    val offlineCacheEnabled: Boolean = false,
    val selectedPrinterName: String = "Android Print Framework"
)

class SettingsViewModel(
    private val systemRepository: SystemRepository,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadLocalPreferences()
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
            autoLockMinutes = state.autoLockMinutes
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
            runCatching { localAppSettingsDataSource.getDevicePreferences() }
                .onSuccess { preferences ->
                    _uiState.update {
                        it.copy(
                            printerSize = preferences.paperSize,
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
    fun onDefaultCreditLimitChanged(value: String) = _uiState.update { it.copy(defaultCreditLimit = value.filter(Char::isDigit), error = null) }
    fun onDefaultTermDaysChanged(value: String) = _uiState.update { it.copy(defaultTermDays = value.filter(Char::isDigit), error = null) }
    fun onCashToleranceChanged(value: String) = _uiState.update {
        it.copy(cashTolerance = sanitizeMoneyInput(value), error = null)
    }
    fun onAutoLockMinutesChanged(value: String) = _uiState.update { it.copy(autoLockMinutes = value.filter(Char::isDigit), error = null) }
    fun onAutoPrintReceiptChanged(value: Boolean) = _uiState.update { it.copy(autoPrintReceipt = value, error = null) }
    fun onBarcodeScannerChanged(value: Boolean) = _uiState.update { it.copy(barcodeScannerEnabled = value, error = null) }
    fun onOfflineCacheChanged(value: Boolean) = _uiState.update { it.copy(offlineCacheEnabled = value, error = null) }
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
            localAppSettingsDataSource: LocalAppSettingsDataSource
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                SettingsViewModel(systemRepository, localAppSettingsDataSource)
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
    autoLockMinutes: String
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
