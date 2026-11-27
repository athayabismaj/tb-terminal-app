package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity

@Dao
interface PendingSubmissionDao {
    @Query("SELECT * FROM pending_submissions WHERE ownerUserId = :ownerUserId AND scope = :scope LIMIT 1")
    suspend fun get(ownerUserId: String, scope: String): PendingSubmissionEntity?

    @Query("SELECT * FROM pending_submissions WHERE ownerUserId = :ownerUserId AND scope LIKE :scopePrefix || '%'")
    suspend fun getByScopePrefix(ownerUserId: String, scopePrefix: String): List<PendingSubmissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingSubmissionEntity)

    @Query("DELETE FROM pending_submissions WHERE ownerUserId = :ownerUserId AND scope = :scope")
    suspend fun delete(ownerUserId: String, scope: String)

    @Query("DELETE FROM pending_submissions WHERE createdAt < :cutoffEpochMillis")
    suspend fun deleteExpired(cutoffEpochMillis: Long): Int

    @Query("DELETE FROM pending_submissions WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllForOwner(ownerUserId: String)
}
