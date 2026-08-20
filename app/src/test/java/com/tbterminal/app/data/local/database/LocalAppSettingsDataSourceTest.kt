package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.AppSettingDao
import com.tbterminal.app.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalAppSettingsDataSourceTest {
    @Test
    fun devicePreferencesPersistAsOneBatchAndCanBeReloaded() = runBlocking {
        val dao = FakeAppSettingDao()
        val source = LocalAppSettingsDataSource(dao)
        val expected = DevicePreferences(
            autoPrintReceipt = false,
            paperSize = "80mm",
            cashTolerance = "1250.50",
            autoLockMinutes = 30
        )

        source.setDevicePreferences(expected)

        assertEquals(1, dao.batchWrites)
        assertEquals(expected, source.getDevicePreferences())
        assertFalse(source.getDevicePreferences().autoPrintReceipt)
    }
}

private class FakeAppSettingDao : AppSettingDao {
    private val values = linkedMapOf<String, AppSettingEntity>()
    private val flow = MutableStateFlow<List<AppSettingEntity>>(emptyList())
    var batchWrites: Int = 0

    override fun observeSettings(): Flow<List<AppSettingEntity>> = flow
    override suspend fun get(key: String): AppSettingEntity? = values[key]
    override suspend fun getValue(key: String): String? = values[key]?.value
    override suspend fun upsert(setting: AppSettingEntity) {
        values[setting.key] = setting
        flow.value = values.values.toList()
    }
    override suspend fun upsertAll(settings: List<AppSettingEntity>) {
        batchWrites += 1
        settings.forEach { values[it.key] = it }
        flow.value = values.values.toList()
    }
}
