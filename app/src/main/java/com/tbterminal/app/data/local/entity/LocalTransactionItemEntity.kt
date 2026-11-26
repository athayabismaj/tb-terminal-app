package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.ScaledDecimal2
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
    val quantity: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val baseQuantity: BigDecimal? = null,
    val conversionFactor: BigDecimal? = null,
    val priceAtTransaction: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val cogsAtTransaction: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val discount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val subtotal: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val note: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
