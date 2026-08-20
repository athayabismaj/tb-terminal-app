package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalCustomerEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM local_customers WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeCustomers(): Flow<List<LocalCustomerEntity>>

    @Query(
        """
        SELECT * FROM local_customers
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getCachedCustomers(
        query: String?,
        limit: Int,
        offset: Int
    ): List<LocalCustomerEntity>

    @Query(
        """
        SELECT COUNT(*) FROM local_customers
        WHERE deletedAt IS NULL
            AND (:query IS NULL OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
        """
    )
    suspend fun countCachedCustomers(query: String?): Long

    @Query("SELECT * FROM local_customers WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalCustomerEntity?

    @Query("SELECT * FROM local_customers WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalCustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: LocalCustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(customers: List<LocalCustomerEntity>): Unit

    @Query("UPDATE local_customers SET syncStatus = :status, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(
        localId: Long,
        status: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query("UPDATE local_customers SET deletedAt = :deletedAt, syncStatus = :status WHERE localId = :localId")
    suspend fun markDeleted(
        localId: Long,
        deletedAt: Long,
        status: SyncStatus
    ): Unit
}
