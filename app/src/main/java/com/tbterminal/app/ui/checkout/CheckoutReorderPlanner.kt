package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashTransactionItem
import java.math.BigDecimal
import java.math.RoundingMode

data class ReorderResolvedProduct(
    val product: Product,
    val isActive: Boolean = true,
)

data class CheckoutReorderPlan(
    val items: List<CartItem>,
    val skippedItems: List<String> = emptyList(),
    val adjustedItems: List<String> = emptyList(),
    val priceUpdatedItems: List<String> = emptyList(),
) {
    fun buildFeedbackMessage(): String {
        if (items.isEmpty()) {
            val reason = skippedItems.firstOrNull()
            return if (reason != null) {
                "Tidak ada barang yang dapat disalin: $reason"
            } else {
                "Tidak ada barang pada transaksi ini untuk disalin."
            }
        }

        val parts = mutableListOf("${items.size} item disalin ke keranjang")
        if (adjustedItems.isNotEmpty()) {
            parts += "${adjustedItems.size} qty disesuaikan stok"
        }
        if (skippedItems.isNotEmpty()) {
            parts += "${skippedItems.size} item dilewati (stok kosong/nonaktif)"
        }
        if (priceUpdatedItems.isNotEmpty()) {
            parts += "harga mengikuti tarif terbaru"
        }
        return parts.joinToString(" • ") + "."
    }
}

internal fun buildCheckoutReorderPlan(
    transactionItems: List<CashTransactionItem>,
    resolvedProducts: Map<String, ReorderResolvedProduct>,
): CheckoutReorderPlan {
    if (transactionItems.isEmpty()) {
        return CheckoutReorderPlan(items = emptyList())
    }

    val remainingStockByProduct = mutableMapOf<String, BigDecimal>()
    val resultItems = mutableListOf<CartItem>()
    val skipped = mutableListOf<String>()
    val adjusted = mutableListOf<String>()
    val priceUpdated = mutableListOf<String>()
    val seenCartIds = mutableSetOf<String>()

    transactionItems.forEachIndexed { index, txItem ->
        val displayName = txItem.productName.ifBlank { "Produk" }
        val resolved = resolvedProducts[txItem.productId]
        if (resolved == null) {
            skipped += "$displayName (tidak ditemukan)"
            return@forEachIndexed
        }
        if (!resolved.isActive) {
            skipped += "${resolved.product.name} (nonaktif)"
            return@forEachIndexed
        }

        val product = resolved.product
        val availableStock = remainingStockByProduct.getOrPut(product.productId) {
            product.stockQty.coerceAtLeast(BigDecimal.ZERO)
        }
        val availableWholeQty = availableStock.setScale(0, RoundingMode.DOWN).toInt()
        if (availableWholeQty <= 0) {
            skipped += "${product.name} (stok habis)"
            return@forEachIndexed
        }

        val requestedQty = txItem.quantity.setScale(0, RoundingMode.HALF_UP).toInt().coerceAtLeast(1)
        val acceptedQty = minOf(requestedQty, availableWholeQty)
        if (acceptedQty < requestedQty) {
            adjusted += "${product.name} ($requestedQty -> $acceptedQty)"
        }
        remainingStockByProduct[product.productId] = availableStock.subtract(BigDecimal(acceptedQty))

        if (txItem.priceAtTransaction.compareTo(product.unitPrice) != 0) {
            priceUpdated += product.name
        }

        val cleanNote = txItem.note?.trim()?.takeIf { it.isNotBlank() }
        val existingSameIdx = resultItems.indexOfFirst {
            it.productId == product.productId && !it.isSecondaryUnit && (it.note ?: "") == (cleanNote ?: "")
        }
        if (existingSameIdx >= 0) {
            val prev = resultItems[existingSameIdx]
            resultItems[existingSameIdx] = prev.copy(quantity = prev.quantity + acceptedQty)
        } else {
            val candidateId = if (seenCartIds.add(product.productId)) {
                product.productId
            } else {
                "${product.productId}__reorder_$index"
            }
            seenCartIds.add(candidateId)
            resultItems += CartItem(
                productId = product.productId,
                productName = product.name,
                unitPrice = product.unitPrice,
                quantity = acceptedQty,
                sku = product.sku,
                unitName = product.unitName,
                note = cleanNote,
                cartItemId = candidateId,
                baseUnitPrice = product.unitPrice,
                baseUnitName = product.unitName,
                secondaryUnitId = product.secondaryUnitId,
                secondaryUnitName = product.secondaryUnitName,
                secondaryUnitFactor = product.secondaryUnitFactor,
                isSecondaryUnit = false,
                retailBaseUnitPrice = product.unitPrice,
                contractorBaseUnitPrice = product.contractorPrice,
                priceTiers = product.priceTiers,
                masterUnitDiscount = product.masterDiscount,
                baseUnitId = product.baseUnitId,
            )
        }
    }

    return CheckoutReorderPlan(
        items = resultItems,
        skippedItems = skipped.distinct(),
        adjustedItems = adjusted,
        priceUpdatedItems = priceUpdated.distinct(),
    )
}

internal data class MergeReorderResult(
    val items: List<CartItem>,
    val cappedItems: List<String> = emptyList(),
)

internal fun mergeReorderItemsIntoCart(
    existingItems: List<CartItem>,
    incomingItems: List<CartItem>,
    resolvedProducts: Map<String, ReorderResolvedProduct>,
): MergeReorderResult {
    val merged = existingItems.toMutableList()
    val capped = mutableListOf<String>()

    incomingItems.forEach { incoming ->
        val stockLimit = resolvedProducts[incoming.productId]?.product?.stockQty?.coerceAtLeast(BigDecimal.ZERO)
        val currentlyConsumedBase = merged
            .filter { it.productId == incoming.productId }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.equivalentBaseQuantityBigDecimal) }

        val allowedWholeAdd = if (stockLimit != null) {
            stockLimit.subtract(currentlyConsumedBase)
                .coerceAtLeast(BigDecimal.ZERO)
                .setScale(0, RoundingMode.DOWN)
                .toInt()
        } else {
            incoming.quantity
        }
        val addQty = minOf(incoming.quantity, allowedWholeAdd)
        if (addQty <= 0) {
            capped += incoming.productName
            return@forEach
        }
        if (addQty < incoming.quantity) {
            capped += incoming.productName
        }

        val matchIdx = merged.indexOfFirst {
            it.productId == incoming.productId &&
                !it.isSecondaryUnit &&
                (it.note ?: "") == (incoming.note ?: "")
        }
        if (matchIdx >= 0) {
            val prev = merged[matchIdx]
            merged[matchIdx] = prev.copy(
                quantity = prev.quantity + addQty,
                unitPrice = incoming.unitPrice,
                baseUnitPrice = incoming.baseUnitPrice,
            )
        } else {
            val existingIds = merged.mapTo(mutableSetOf()) { it.cartItemId }
            var cartId = incoming.cartItemId
            var suffix = 1
            while (cartId in existingIds) {
                cartId = "${incoming.productId}__merge_$suffix"
                suffix++
            }
            merged += incoming.copy(cartItemId = cartId, quantity = addQty)
        }
    }

    return MergeReorderResult(items = merged, cappedItems = capped.distinct())
}
