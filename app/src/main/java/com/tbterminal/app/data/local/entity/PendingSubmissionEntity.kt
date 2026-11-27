package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "pending_submissions",
    primaryKeys = ["ownerUserId", "scope"],
    indices = [
        Index("createdAt"),
        Index("updatedAt")
    ]
)
data class PendingSubmissionEntity(
    val ownerUserId: String,
    val scope: String,
    val idempotencyKey: String,
    val intentJson: String,
    val payloadJson: String? = null,
    val status: String,
    val schemaVersion: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
    val createdAtElapsedMillis: Long? = null,
    val bootCount: Long? = null,
)
