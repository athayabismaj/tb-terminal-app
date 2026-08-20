package com.tbterminal.app.data.local.mapper

import com.tbterminal.app.data.local.entity.AppSettingEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.AppSettingKeys

fun AppDataMode.toSettingEntity(updatedAt: Long = System.currentTimeMillis()): AppSettingEntity {
    return AppSettingEntity(
        key = AppSettingKeys.DATA_MODE,
        value = name,
        updatedAt = updatedAt
    )
}

fun AppSettingEntity.toAppDataMode(): AppDataMode {
    return runCatching { AppDataMode.valueOf(value) }.getOrDefault(AppDataMode.SYNC_SERVER)
}
