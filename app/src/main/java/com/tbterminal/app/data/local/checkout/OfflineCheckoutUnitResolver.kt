package com.tbterminal.app.data.local.checkout

import java.math.BigDecimal

fun normalizeExactDecimal(value: BigDecimal): BigDecimal {
    return if (value.compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal.ZERO
    } else {
        value.stripTrailingZeros()
    }
}

fun exceedsMaxBaseQuantityScale(baseQuantity: BigDecimal): Boolean {
    return normalizeExactDecimal(baseQuantity).scale() > 2
}

sealed interface OfflineItemUnitResolution {
    data class Success(
        val unitServerId: String?,
        val baseQuantity: BigDecimal,
        val conversionFactor: BigDecimal
    ) : OfflineItemUnitResolution

    data class Error(val reason: String) : OfflineItemUnitResolution
}

/**
 * Resolves the authoritative sold unitId, exact unrounded baseQuantity, and conversion factor
 * for an offline checkout item, enforcing the backend rule of at most 2 decimal places for
 * secondary unit conversions.
 */
fun resolveOfflineCheckoutItemUnit(
    productName: String,
    quantity: Int,
    isSecondaryUnit: Boolean,
    cartBaseUnitId: String?,
    cartSecondaryUnitId: String?,
    cartSecondaryUnitFactor: BigDecimal?,
    cachedBaseUnitId: String?,
    cachedSecondaryUnitId: String?,
    cachedSecondaryUnitFactor: BigDecimal?
): OfflineItemUnitResolution {
    if (quantity <= 0) {
        return OfflineItemUnitResolution.Error("Qty $productName belum valid.")
    }

    if (isSecondaryUnit) {
        val factor = cartSecondaryUnitFactor?.takeIf { it > BigDecimal.ZERO }
            ?: cachedSecondaryUnitFactor?.takeIf { it > BigDecimal.ZERO }
            ?: return OfflineItemUnitResolution.Error(
                "Faktor konversi satuan sekunder $productName belum tersedia."
            )
        val unitId = cartSecondaryUnitId?.trim()?.takeIf(String::isNotBlank)
            ?: cachedSecondaryUnitId?.trim()?.takeIf(String::isNotBlank)
            ?: return OfflineItemUnitResolution.Error(
                "Data satuan sekunder $productName belum lengkap. Segarkan produk saat online."
            )
        val baseQuantity = normalizeExactDecimal(quantity.toBigDecimal().multiply(factor))
        if (exceedsMaxBaseQuantityScale(baseQuantity)) {
            return OfflineItemUnitResolution.Error(
                "Jumlah satuan dasar $productName (${baseQuantity.toPlainString()}) melebihi 2 angka desimal dan tidak didukung untuk transaksi offline."
            )
        }
        return OfflineItemUnitResolution.Success(
            unitServerId = unitId,
            baseQuantity = baseQuantity,
            conversionFactor = normalizeExactDecimal(factor)
        )
    }

    val baseUnitId = cartBaseUnitId?.trim()?.takeIf(String::isNotBlank)
        ?: cachedBaseUnitId?.trim()?.takeIf(String::isNotBlank)
    return OfflineItemUnitResolution.Success(
        unitServerId = baseUnitId,
        baseQuantity = normalizeExactDecimal(quantity.toBigDecimal()),
        conversionFactor = BigDecimal.ONE
    )
}
