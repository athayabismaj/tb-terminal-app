package com.tbterminal.app.data.local.checkout

import java.math.BigDecimal

data class LocalCheckoutPaymentLineCommand(
    val method: String,
    val amount: BigDecimal,
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
    val subtotal: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    val taxEnabled: Boolean = false,
    val taxMode: String? = null,
    val taxRate: BigDecimal = BigDecimal.ZERO,
    val taxLabel: String? = null,
    val taxableBase: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val roundingAmount: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal? = null,
    val items: List<LocalCheckoutItemCommand>,
    val occurredAt: Long? = null,
    val note: String? = null
)

data class LocalCheckoutItemCommand(
    val productLocalId: Long? = null,
    val productServerId: String? = null,
    val productNameSnapshot: String,
    val skuSnapshot: String? = null,
    val unitNameSnapshot: String? = null,
    val quantity: BigDecimal,
    val baseQuantity: BigDecimal = quantity,
    val conversionFactor: BigDecimal = BigDecimal.ONE,
    val priceAtTransaction: BigDecimal,
    val cogsAtTransaction: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val subtotal: BigDecimal,
    val note: String? = null,
    val unitServerId: String? = null,
    val baseQuantityExact: BigDecimal? = null,
    val conversionFactorExact: BigDecimal? = null
) {
    val resolvedBaseQuantity: BigDecimal
        get() = normalizeExactDecimal(baseQuantityExact ?: baseQuantity)

    val resolvedConversionFactor: BigDecimal
        get() = normalizeExactDecimal(conversionFactorExact ?: conversionFactor)
}
