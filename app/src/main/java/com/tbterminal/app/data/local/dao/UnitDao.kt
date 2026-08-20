package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalUnitEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM local_units WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeUnits(): Flow<List<LocalUnitEntity>>

    @Query("SELECT * FROM local_units WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getCachedUnits(): List<LocalUnitEntity>

    @Query(
        """
        SELECT * FROM local_units
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%' OR symbol LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getCachedUnitPage(
        query: String?,
        limit: Int,
        offset: Int
    ): List<LocalUnitEntity>

    @Query(
        """
        SELECT COUNT(*) FROM local_units
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%' OR symbol LIKE '%' || :query || '%')
        """
    )
    suspend fun countCachedUnits(query: String?): Long

    @Query("SELECT * FROM local_units WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalUnitEntity?

    @Query("SELECT * FROM local_units WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unit: LocalUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnits(units: List<LocalUnitEntity>): Unit

    @Query("UPDATE local_units SET syncStatus = :status, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(
        localId: Long,
        status: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query("UPDATE local_units SET deletedAt = :deletedAt, syncStatus = :status WHERE localId = :localId")
    suspend fun markDeleted(
        localId: Long,
        deletedAt: Long,
        status: SyncStatus
    ): Unit
}
