package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.DiscountType
import com.tbterminal.app.data.model.ProductPriceTier
import java.math.BigDecimal
import java.math.RoundingMode

data class ResolvedCartItemPricing(
    val baseUnitPrice: BigDecimal,
    val displayUnitPrice: BigDecimal,
    val appliedTierMinQty: BigDecimal? = null,
    val isContractorPriceApplied: Boolean = false,
)

fun resolveCartItemPricing(
    retailBaseUnitPrice: BigDecimal,
    contractorBaseUnitPrice: BigDecimal,
    isContractorCustomer: Boolean,
    effectiveBaseQty: BigDecimal,
    priceTiers: List<ProductPriceTier>,
    secondaryUnitFactor: BigDecimal? = null,
    isSecondaryUnit: Boolean = false,
): ResolvedCartItemPricing {
    val basePrice: BigDecimal
    val tierMinQty: BigDecimal?
    val contractorApplied: Boolean

    if (isContractorCustomer && contractorBaseUnitPrice > BigDecimal.ZERO) {
        basePrice = contractorBaseUnitPrice
        tierMinQty = null
        contractorApplied = true
    } else {
        val matchedTier = priceTiers
            .filter { effectiveBaseQty.compareTo(it.minQty) >= 0 }
            .maxByOrNull { it.minQty }
        if (matchedTier != null) {
            basePrice = matchedTier.unitPrice
            tierMinQty = matchedTier.minQty
            contractorApplied = false
        } else {
            basePrice = retailBaseUnitPrice
            tierMinQty = null
            contractorApplied = false
        }
    }

    val displayPrice = if (isSecondaryUnit && secondaryUnitFactor != null) {
        basePrice.multiply(secondaryUnitFactor)
    } else {
        basePrice
    }

    return ResolvedCartItemPricing(
        baseUnitPrice = basePrice,
        displayUnitPrice = displayPrice,
        appliedTierMinQty = tierMinQty,
        isContractorPriceApplied = contractorApplied,
    )
}

fun repriceCartItemsForCheckout(
    items: List<CartItem>,
    isContractorCustomer: Boolean,
): List<CartItem> {
    if (items.isEmpty()) return items
    return items.map { item ->
        val resolved = resolveCartItemPricing(
            retailBaseUnitPrice = item.retailBaseUnitPrice,
            contractorBaseUnitPrice = item.contractorBaseUnitPrice,
            isContractorCustomer = isContractorCustomer,
            effectiveBaseQty = item.equivalentBaseQuantityBigDecimal,
            priceTiers = item.priceTiers,
            secondaryUnitFactor = item.secondaryUnitFactor,
            isSecondaryUnit = item.isSecondaryUnit,
        )
        val lineGross = resolved.displayUnitPrice.multiply(item.quantity.toBigDecimal())
        val (effectiveDiscount, masterApplied) = if (!item.hasManualDiscountOverride && item.masterUnitDiscount > BigDecimal.ZERO) {
            val totalPromo = item.masterUnitDiscount
                .multiply(item.equivalentBaseQuantityBigDecimal)
                .setScale(2, RoundingMode.HALF_UP)
                .coerceAtMost(lineGross)
                .coerceAtLeast(BigDecimal.ZERO)
            if (totalPromo > BigDecimal.ZERO) {
                CheckoutDiscount(type = DiscountType.FIXED_AMOUNT, value = totalPromo) to true
            } else {
                null to false
            }
        } else {
            item.discountRequest to false
        }
        item.copy(
            baseUnitPrice = resolved.baseUnitPrice,
            unitPrice = resolved.displayUnitPrice,
            appliedPriceTierMinQty = resolved.appliedTierMinQty,
            isContractorPriceApplied = resolved.isContractorPriceApplied,
            discountRequest = effectiveDiscount,
            isMasterDiscountApplied = masterApplied,
        )
    }
}
