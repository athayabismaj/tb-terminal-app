package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.AppSettingDao
import com.tbterminal.app.data.local.entity.AppSettingEntity
import com.tbterminal.app.data.local.mapper.toAppDataMode
import com.tbterminal.app.data.local.mapper.toSettingEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.AppSettingKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class DevicePreferences(
    val autoPrintReceipt: Boolean = true,
    val paperSize: String = "58mm",
    val cashTolerance: String = "0",
    val autoLockMinutes: Int = 15
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
            cashTolerance = appSettingDao.getValue(AppSettingKeys.CASH_TOLERANCE)
                ?.takeIf(String::isNotBlank) ?: "0",
            autoLockMinutes = appSettingDao.getValue(AppSettingKeys.AUTO_LOCK_MINUTES)
                ?.toIntOrNull()?.takeIf { it in 1..120 } ?: 15
        )
    }

    suspend fun setDevicePreferences(preferences: DevicePreferences) {
        val now = System.currentTimeMillis()
        appSettingDao.upsertAll(
            listOf(
                AppSettingEntity(AppSettingKeys.AUTO_PRINT_RECEIPT, preferences.autoPrintReceipt.toString(), now),
                AppSettingEntity(AppSettingKeys.PAPER_SIZE, preferences.paperSize, now),
                AppSettingEntity(AppSettingKeys.CASH_TOLERANCE, preferences.cashTolerance, now),
                AppSettingEntity(AppSettingKeys.AUTO_LOCK_MINUTES, preferences.autoLockMinutes.toString(), now)
            )
        )
    }
}
