package com.tbterminal.app.data.local.database

import androidx.room.TypeConverter
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus

class LocalConverters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus =
        runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.PENDING)

    @TypeConverter
    fun fromSyncOperation(value: SyncOperation): String = value.name

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation =
        runCatching { SyncOperation.valueOf(value) }.getOrDefault(SyncOperation.UPDATE)

    @TypeConverter
    fun fromSyncEntityType(value: SyncEntityType): String = value.name

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType =
        runCatching { SyncEntityType.valueOf(value) }.getOrDefault(SyncEntityType.UNSUPPORTED)

    @TypeConverter
    fun fromAppDataMode(value: AppDataMode): String = value.name

    @TypeConverter
    fun toAppDataMode(value: String): AppDataMode =
        runCatching { AppDataMode.valueOf(value) }.getOrDefault(AppDataMode.SYNC_SERVER)
}
