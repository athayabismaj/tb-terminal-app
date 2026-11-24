package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal

@Entity(
    tableName = "local_transaction_items",
    indices = [
        Index("transactionLocalId"),
        Index("productLocalId"),
        Index("productServerId"),
        Index("syncStatus")
    ]
)
data class LocalTransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val transactionLocalId: Long,
    val transactionServerId: String? = null,
    val productLocalId: Long? = null,
    val productServerId: String? = null,
    val productNameSnapshot: String,
    val skuSnapshot: String? = null,
    val unitNameSnapshot: String? = null,
    val unitServerId: String? = null,
    val quantity: Double = 0.0,
    val baseQuantity: BigDecimal? = null,
    val conversionFactor: BigDecimal? = null,
    val priceAtTransaction: Double = 0.0,
    val cogsAtTransaction: Double = 0.0,
    val discount: Double = 0.0,
    val subtotal: Double = 0.0,
    val note: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
