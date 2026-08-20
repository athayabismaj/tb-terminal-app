package com.tbterminal.app.data.local.checkout

data class LocalCheckoutCommand(
    val cashierUserId: String,
    val cashSessionLocalId: Long? = null,
    val cashSessionServerId: String? = null,
    val customerLocalId: Long? = null,
    val customerServerId: String? = null,
    val customerName: String? = null,
    val paymentMethod: String,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double? = null,
    val items: List<LocalCheckoutItemCommand>,
    val occurredAt: Long = System.currentTimeMillis(),
    val note: String? = null
)

data class LocalCheckoutItemCommand(
    val productLocalId: Long? = null,
    val productServerId: String? = null,
    val productNameSnapshot: String,
    val skuSnapshot: String? = null,
    val unitNameSnapshot: String? = null,
    val quantity: Double,
    val priceAtTransaction: Double,
    val cogsAtTransaction: Double = 0.0,
    val discount: Double = 0.0,
    val subtotal: Double
)
