package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.AppSettingDao
import com.tbterminal.app.data.local.entity.AppSettingEntity
import com.tbterminal.app.data.local.mapper.toAppDataMode
import com.tbterminal.app.data.local.mapper.toSettingEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.AppSettingKeys
import com.tbterminal.app.data.model.StoreSettings
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class DevicePreferences(
    val autoPrintReceipt: Boolean = true,
    val paperSize: String = "58mm",
    val printerConnectionType: String = "BLUETOOTH",
    val printerBluetoothAddress: String = "",
    val printerBluetoothName: String = "",
    val autoCutPaper: Boolean = true,
    val openCashDrawer: Boolean = false,
    val cashTolerance: String = "0",
    val autoLockMinutes: Int = 15,
    val scannerSoundFeedback: Boolean = true,
    val scannerVibrateFeedback: Boolean = true
)

data class CachedStoreConfig(
    val storeName: String = "TB TERMINAL",
    val storeNpwp: String? = null,
    val taxEnabled: Boolean = false,
    val taxRate: BigDecimal = BigDecimal("11.00"),
    val taxMode: String = "EXCLUDE",
    val taxLabel: String = "PPN",
    val cashRoundingUnit: Int = 0,
    val cashRoundingRule: String = "NEAREST"
)

class LocalAppSettingsDataSource(
    private val appSettingDao: AppSettingDao
) {
    suspend fun getDataMode(): AppDataMode {
        return appSettingDao.get(AppSettingKeys.DATA_MODE)?.toAppDataMode()
            ?: AppDataMode.SYNC_SERVER
    }

    fun observeDataMode(): Flow<AppDataMode> {
        return appSettingDao.observeSettings()
            .map { settings ->
                settings.firstOrNull { setting -> setting.key == AppSettingKeys.DATA_MODE }?.toAppDataMode()
                    ?: AppDataMode.SYNC_SERVER
            }
            .distinctUntilChanged()
    }

    suspend fun setDataMode(mode: AppDataMode) {
        appSettingDao.upsert(mode.toSettingEntity())
    }

    suspend fun getServerBaseUrl(): String? {
        return appSettingDao.getValue(AppSettingKeys.SERVER_BASE_URL)
    }

    suspend fun setServerBaseUrl(baseUrl: String) {
        appSettingDao.upsert(
            AppSettingEntity(
                key = AppSettingKeys.SERVER_BASE_URL,
                value = baseUrl
            )
        )
    }

    suspend fun getDevicePreferences(): DevicePreferences {
        return DevicePreferences(
            autoPrintReceipt = appSettingDao.getValue(AppSettingKeys.AUTO_PRINT_RECEIPT)
                ?.toBooleanStrictOrNull() ?: true,
            paperSize = appSettingDao.getValue(AppSettingKeys.PAPER_SIZE)
                ?.takeIf { it == "58mm" || it == "80mm" } ?: "58mm",
            printerConnectionType = appSettingDao.getValue(AppSettingKeys.PRINTER_CONNECTION_TYPE)
                ?.takeIf { it in listOf("BLUETOOTH", "USB", "SYSTEM") } ?: "BLUETOOTH",
            printerBluetoothAddress = appSettingDao.getValue(AppSettingKeys.PRINTER_BLUETOOTH_ADDRESS).orEmpty(),
            printerBluetoothName = appSettingDao.getValue(AppSettingKeys.PRINTER_BLUETOOTH_NAME).orEmpty(),
            autoCutPaper = appSettingDao.getValue(AppSettingKeys.PRINTER_AUTO_CUT)
                ?.toBooleanStrictOrNull() ?: true,
            openCashDrawer = appSettingDao.getValue(AppSettingKeys.PRINTER_OPEN_DRAWER)
                ?.toBooleanStrictOrNull() ?: false,
            cashTolerance = appSettingDao.getValue(AppSettingKeys.CASH_TOLERANCE)
                ?.takeIf(String::isNotBlank) ?: "0",
            autoLockMinutes = appSettingDao.getValue(AppSettingKeys.AUTO_LOCK_MINUTES)
                ?.toIntOrNull()?.takeIf { it in 1..120 } ?: 15,
            scannerSoundFeedback = appSettingDao.getValue(AppSettingKeys.SCANNER_SOUND_FEEDBACK)
                ?.toBooleanStrictOrNull() ?: true,
            scannerVibrateFeedback = appSettingDao.getValue(AppSettingKeys.SCANNER_VIBRATE_FEEDBACK)
                ?.toBooleanStrictOrNull() ?: true
        )
    }

    suspend fun setDevicePreferences(preferences: DevicePreferences) {
        val now = System.currentTimeMillis()
        appSettingDao.upsertAll(
            listOf(
                AppSettingEntity(AppSettingKeys.AUTO_PRINT_RECEIPT, preferences.autoPrintReceipt.toString(), now),
                AppSettingEntity(AppSettingKeys.PAPER_SIZE, preferences.paperSize, now),
                AppSettingEntity(AppSettingKeys.PRINTER_CONNECTION_TYPE, preferences.printerConnectionType, now),
                AppSettingEntity(AppSettingKeys.PRINTER_BLUETOOTH_ADDRESS, preferences.printerBluetoothAddress, now),
                AppSettingEntity(AppSettingKeys.PRINTER_BLUETOOTH_NAME, preferences.printerBluetoothName, now),
                AppSettingEntity(AppSettingKeys.PRINTER_AUTO_CUT, preferences.autoCutPaper.toString(), now),
                AppSettingEntity(AppSettingKeys.PRINTER_OPEN_DRAWER, preferences.openCashDrawer.toString(), now),
                AppSettingEntity(AppSettingKeys.CASH_TOLERANCE, preferences.cashTolerance, now),
                AppSettingEntity(AppSettingKeys.AUTO_LOCK_MINUTES, preferences.autoLockMinutes.toString(), now),
                AppSettingEntity(AppSettingKeys.SCANNER_SOUND_FEEDBACK, preferences.scannerSoundFeedback.toString(), now),
                AppSettingEntity(AppSettingKeys.SCANNER_VIBRATE_FEEDBACK, preferences.scannerVibrateFeedback.toString(), now)
            )
        )
    }

    suspend fun getCachedStoreConfig(): CachedStoreConfig {
        return CachedStoreConfig(
            storeName = appSettingDao.getValue(AppSettingKeys.CACHED_STORE_NAME)
                ?.takeIf(String::isNotBlank) ?: "TB TERMINAL",
            storeNpwp = appSettingDao.getValue(AppSettingKeys.CACHED_STORE_NPWP)
                ?.takeIf(String::isNotBlank),
            taxEnabled = appSettingDao.getValue(AppSettingKeys.CACHED_TAX_ENABLED)
                ?.toBooleanStrictOrNull() ?: false,
            taxRate = appSettingDao.getValue(AppSettingKeys.CACHED_TAX_RATE)
                ?.toBigDecimalOrNull() ?: BigDecimal("11.00"),
            taxMode = appSettingDao.getValue(AppSettingKeys.CACHED_TAX_MODE)
                ?.takeIf { it == "EXCLUDE" || it == "INCLUDE" } ?: "EXCLUDE",
            taxLabel = appSettingDao.getValue(AppSettingKeys.CACHED_TAX_LABEL)
                ?.takeIf(String::isNotBlank) ?: "PPN",
            cashRoundingUnit = appSettingDao.getValue(AppSettingKeys.CACHED_CASH_ROUNDING_UNIT)
                ?.toIntOrNull()?.takeIf { it in setOf(0, 100, 500, 1000) } ?: 0,
            cashRoundingRule = appSettingDao.getValue(AppSettingKeys.CACHED_CASH_ROUNDING_RULE)
                ?.takeIf { it in setOf("NEAREST", "DOWN", "UP") } ?: "NEAREST"
        )
    }

    suspend fun setCachedStoreConfig(settings: StoreSettings) {
        val now = System.currentTimeMillis()
        appSettingDao.upsertAll(
            listOf(
                AppSettingEntity(AppSettingKeys.CACHED_STORE_NAME, settings.storeName, now),
                AppSettingEntity(AppSettingKeys.CACHED_STORE_NPWP, settings.storeNpwp.orEmpty(), now),
                AppSettingEntity(AppSettingKeys.CACHED_TAX_ENABLED, settings.taxEnabled.toString(), now),
                AppSettingEntity(AppSettingKeys.CACHED_TAX_RATE, settings.taxRate.toPlainString(), now),
                AppSettingEntity(AppSettingKeys.CACHED_TAX_MODE, settings.taxMode, now),
                AppSettingEntity(AppSettingKeys.CACHED_TAX_LABEL, settings.taxLabel, now),
                AppSettingEntity(AppSettingKeys.CACHED_CASH_ROUNDING_UNIT, settings.cashRoundingUnit.toString(), now),
                AppSettingEntity(AppSettingKeys.CACHED_CASH_ROUNDING_RULE, settings.cashRoundingRule, now)
            )
        )
    }
}
