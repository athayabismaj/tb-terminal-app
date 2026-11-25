package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.local.checkout.exceedsMaxBaseQuantityScale
import com.tbterminal.app.data.local.checkout.normalizeExactDecimal
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPaymentLine
import com.tbterminal.app.data.model.CheckoutSubmitItem
import java.math.BigDecimal

sealed interface OnlineCheckoutItemsResult {
    data class Success(val items: List<CheckoutSubmitItem>) : OnlineCheckoutItemsResult
    data class Error(val message: String) : OnlineCheckoutItemsResult
}

internal fun resolveOnlineCheckoutItems(cartItems: List<CartItem>): OnlineCheckoutItemsResult {
    val seenProductIds = mutableSetOf<String>()
    val resolvedItems = mutableListOf<CheckoutSubmitItem>()

    for (item in cartItems) {
        if (!seenProductIds.add(item.productId.trim().lowercase())) {
            return OnlineCheckoutItemsResult.Error(
                "Produk yang sama tidak boleh muncul lebih dari sekali dalam keranjang"
            )
        }

        val effectiveQty = if (item.isSecondaryUnit) {
            val factor = item.secondaryUnitFactor?.takeIf { it > BigDecimal.ZERO }
                ?: return OnlineCheckoutItemsResult.Error(
                    "Faktor konversi satuan sekunder ${item.productName} belum tersedia."
                )
            normalizeExactDecimal(item.quantity.toBigDecimal().multiply(factor))
        } else {
            normalizeExactDecimal(item.quantity.toBigDecimal())
        }

        if (effectiveQty <= BigDecimal.ZERO) {
            return OnlineCheckoutItemsResult.Error("Qty ${item.productName} belum valid.")
        }

        if (exceedsMaxBaseQuantityScale(effectiveQty)) {
            return OnlineCheckoutItemsResult.Error(
                "Jumlah satuan dasar ${item.productName} melebihi 2 angka desimal"
            )
        }

        resolvedItems += CheckoutSubmitItem(
            productId = item.productId,
            quantity = effectiveQty,
            discountRequest = item.discountRequest,
            note = item.note,
        )
    }

    return OnlineCheckoutItemsResult.Success(resolvedItems)
}

internal fun buildOnlineCheckoutIdempotencyFingerprint(
    items: List<CartItem>,
    paymentMethod: PaymentMethod,
    amountPaid: BigDecimal,
    paymentLines: List<CheckoutPaymentLine>?,
    isSplitDpMode: Boolean,
    customerId: String?,
    notes: String?,
    transactionDiscount: CheckoutDiscount?,
    checkoutAttemptId: String?,
    managerApprovalId: String?,
): String = buildString {
    append(paymentMethod.apiValue()).append('|').append(amountPaid.toPlainString()).append('|')
    append(isSplitDpMode).append('|')
    paymentLines.orEmpty().forEach { line ->
        append(line.method).append(':').append(line.amount).append(':').append(line.reference.orEmpty()).append(',')
    }
    append('|')
    append(customerId.orEmpty()).append('|').append(notes.orEmpty()).append('|')
    append(checkoutAttemptId.orEmpty()).append('|').append(managerApprovalId.orEmpty()).append('|')
    append(transactionDiscount.fingerprintPart()).append('|')
    items.sortedBy(CartItem::cartItemId).forEach { item ->
        append(item.cartItemId).append(':').append(item.quantity).append(':')
            .append(item.discountRequest.fingerprintPart()).append(':')
            .append(item.isSecondaryUnit).append(':')
            .append(item.secondaryUnitFactor?.let(::normalizeExactDecimal)?.toPlainString().orEmpty()).append(':')
            .append(item.note.orEmpty()).append(';')
    }
}

private fun CheckoutDiscount?.fingerprintPart(): String = this?.let { discount ->
    "${discount.type.name}:${discount.value.stripTrailingZeros().toPlainString()}"
}.orEmpty()
