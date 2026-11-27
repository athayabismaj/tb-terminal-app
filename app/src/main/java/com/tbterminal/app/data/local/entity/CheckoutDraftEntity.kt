package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checkout_drafts",
    indices = [
        Index("updatedAt")
    ]
)
data class CheckoutDraftEntity(
    @PrimaryKey val ownerUserId: String,
    val activeCartJson: String? = null,
    val heldCartsJson: String? = null,
    val schemaVersion: Int = 1,
    val updatedAt: Long
)
