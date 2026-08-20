package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_customers",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("phone")
    ]
)
data class LocalCustomerEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val isContractor: Boolean = false,
    val creditAllowed: Boolean = false,
    val creditLimit: Double = 0.0,
    val paymentTermDays: Int = 0,
    val isActive: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val remoteCreatedAt: String? = null,
    val remoteUpdatedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
