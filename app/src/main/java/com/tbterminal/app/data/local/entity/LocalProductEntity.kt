package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_products",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("categoryLocalId"),
        Index("unitLocalId")
    ]
)
data class LocalProductEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
    val categoryLocalId: Long? = null,
    val categoryServerId: String? = null,
    val unitLocalId: Long? = null,
    val unitServerId: String? = null,
    val priceSell: Double = 0.0,
    val priceBuy: Double = 0.0,
    val priceRetail: Double = 0.0,
    val priceContractor: Double = 0.0,
    val discount: Double = 0.0,
    val stock: Double = 0.0,
    val minimumStock: Double = 0.0,
    val photoFilename: String? = null,
    val isActive: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val remoteCreatedAt: String? = null,
    val remoteUpdatedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
