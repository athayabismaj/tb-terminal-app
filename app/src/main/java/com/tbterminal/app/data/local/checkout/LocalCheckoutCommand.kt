package com.tbterminal.app.data.local.checkout

data class LocalCheckoutPaymentLineCommand(
    val method: String,
    val amount: Double,
    val reference: String? = null
)

data class LocalCheckoutCommand(
    val cashierUserId: String,
    val cashSessionLocalId: Long? = null,
    val cashSessionServerId: String? = null,
    val customerLocalId: Long? = null,
    val customerServerId: String? = null,
    val customerName: String? = null,
    val paymentMethod: String,
    val payments: List<LocalCheckoutPaymentLineCommand>? = null,
    val subtotal: Double,
    val discount: Double = 0.0,
    val taxEnabled: Boolean = false,
    val taxMode: String? = null,
    val taxRate: Double = 0.0,
    val taxLabel: String? = null,
    val taxableBase: Double = 0.0,
    val taxAmount: Double = 0.0,
    val roundingAmount: Double = 0.0,
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
    val baseQuantity: Double = quantity,
    val conversionFactor: Double = 1.0,
    val priceAtTransaction: Double,
    val cogsAtTransaction: Double = 0.0,
    val discount: Double = 0.0,
    val subtotal: Double,
    val note: String? = null
)
