package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal

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
    val unitName: String? = null,
    val secondaryUnitServerId: String? = null,
    val secondaryUnitName: String? = null,
    val secondaryUnitFactor: BigDecimal? = null,
    val priceSell: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val priceBuy: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val priceRetail: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val priceContractor: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val discount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val stock: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val minimumStock: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val photoFilename: String? = null,
    val isActive: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val remoteCreatedAt: String? = null,
    val remoteUpdatedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
