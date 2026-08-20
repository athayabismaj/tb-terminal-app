package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM local_products WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeProducts(): Flow<List<LocalProductEntity>>

    @Query("SELECT * FROM local_products WHERE deletedAt IS NULL AND isActive = 1 ORDER BY name ASC")
    fun observeActiveProducts(): Flow<List<LocalProductEntity>>

    @Query(
        """
        SELECT * FROM local_products
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getCachedProducts(
        query: String?,
        limit: Int,
        offset: Int
    ): List<LocalProductEntity>

    @Query(
        """
        SELECT COUNT(*) FROM local_products
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')
        """
    )
    suspend fun countCachedProducts(query: String?): Long

    @Query("SELECT * FROM local_products WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalProductEntity?

    @Query("SELECT * FROM local_products WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: LocalProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<LocalProductEntity>): Unit

    @Query("UPDATE local_products SET syncStatus = :status, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(
        localId: Long,
        status: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query("UPDATE local_products SET stock = stock - :quantity, syncStatus = :status, updatedAt = :updatedAt WHERE localId = :localId AND deletedAt IS NULL")
    suspend fun decrementStock(
        localId: Long,
        quantity: Double,
        status: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query("UPDATE local_products SET deletedAt = :deletedAt, syncStatus = :status WHERE localId = :localId")
    suspend fun markDeleted(
        localId: Long,
        deletedAt: Long,
        status: SyncStatus
    ): Unit
}
