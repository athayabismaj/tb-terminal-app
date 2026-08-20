package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalCategoryEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM local_categories WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeCategories(): Flow<List<LocalCategoryEntity>>

    @Query("SELECT * FROM local_categories WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getCachedCategories(): List<LocalCategoryEntity>

    @Query(
        """
        SELECT * FROM local_categories
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getCachedCategoryPage(
        query: String?,
        limit: Int,
        offset: Int
    ): List<LocalCategoryEntity>

    @Query(
        """
        SELECT COUNT(*) FROM local_categories
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%')
        """
    )
    suspend fun countCachedCategories(query: String?): Long

    @Query("SELECT * FROM local_categories WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalCategoryEntity?

    @Query("SELECT * FROM local_categories WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: LocalCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<LocalCategoryEntity>): Unit

    @Query("UPDATE local_categories SET syncStatus = :status, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(
        localId: Long,
        status: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query("UPDATE local_categories SET deletedAt = :deletedAt, syncStatus = :status WHERE localId = :localId")
    suspend fun markDeleted(
        localId: Long,
        deletedAt: Long,
        status: SyncStatus
    ): Unit
}
