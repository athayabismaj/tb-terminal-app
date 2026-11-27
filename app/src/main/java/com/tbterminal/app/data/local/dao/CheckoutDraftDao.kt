package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.CheckoutDraftEntity

@Dao
interface CheckoutDraftDao {
    @Query("SELECT * FROM checkout_drafts WHERE ownerUserId = :ownerUserId LIMIT 1")
    suspend fun get(ownerUserId: String): CheckoutDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CheckoutDraftEntity)

    @Query("DELETE FROM checkout_drafts WHERE ownerUserId = :ownerUserId")
    suspend fun delete(ownerUserId: String)
}
