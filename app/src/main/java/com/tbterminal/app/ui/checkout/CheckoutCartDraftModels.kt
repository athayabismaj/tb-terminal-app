package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.DiscountType
import java.math.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val CheckoutPersistenceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
internal data class PersistedCartItemDraft(
    val productId: String,
    val cartItemId: String,
    val quantity: Int,
    val isSecondaryUnit: Boolean = false,
    val baseUnitId: String? = null,
    val secondaryUnitId: String? = null,
    val secondaryUnitFactor: String? = null,
    val discountType: String? = null,
    val discountValue: String? = null,
    val hasManualDiscountOverride: Boolean = false,
    val note: String? = null,
    // Fallback snapshot fields so items remain renderable when offline/uncached on restore.
    val productName: String,
    val sku: String = "",
    val unitName: String = "",
    val baseUnitName: String = "",
    val secondaryUnitName: String? = null,
    val unitPrice: String,
    val baseUnitPrice: String,
    val retailBaseUnitPrice: String,
    val contractorBaseUnitPrice: String = "0",
    val masterUnitDiscount: String = "0",
    val isMasterDiscountApplied: Boolean = false,
    val isContractorPriceApplied: Boolean = false
)

@Serializable
internal data class PersistedCustomerSnapshot(
    val id: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val isContractor: Boolean = false,
    val creditLimit: String = "0",
    val paymentTermDays: Int = 0
)

@Serializable
internal data class PersistedSplitPaymentLine(
    val id: String,
    val method: String,
    val amountInput: String,
    val reference: String = ""
)

internal fun CartItem.toPersistedDraft(): PersistedCartItemDraft {
    return PersistedCartItemDraft(
        productId = productId,
        cartItemId = cartItemId,
        quantity = quantity,
        isSecondaryUnit = isSecondaryUnit,
        baseUnitId = baseUnitId,
        secondaryUnitId = secondaryUnitId,
        secondaryUnitFactor = secondaryUnitFactor?.stripTrailingZeros()?.toPlainString(),
        discountType = discountRequest?.type?.name,
        discountValue = discountRequest?.value?.stripTrailingZeros()?.toPlainString(),
        hasManualDiscountOverride = hasManualDiscountOverride,
        note = note,
        productName = productName,
        sku = sku,
        unitName = unitName,
        baseUnitName = baseUnitName,
        secondaryUnitName = secondaryUnitName,
        unitPrice = unitPrice.toPlainString(),
        baseUnitPrice = baseUnitPrice.toPlainString(),
        retailBaseUnitPrice = retailBaseUnitPrice.toPlainString(),
        contractorBaseUnitPrice = contractorBaseUnitPrice.toPlainString(),
        masterUnitDiscount = masterUnitDiscount.toPlainString(),
        isMasterDiscountApplied = isMasterDiscountApplied,
        isContractorPriceApplied = isContractorPriceApplied
    )
}

internal fun PersistedCartItemDraft.toCheckoutDiscountOrNull(): CheckoutDiscount? {
    val rawType = discountType?.trim()?.takeIf(String::isNotBlank) ?: return null
    val rawValue = discountValue?.trim()?.takeIf(String::isNotBlank) ?: return null
    val parsedType = runCatching { DiscountType.valueOf(rawType) }.getOrNull() ?: return null
    val parsedValue = rawValue.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return null
    return CheckoutDiscount(type = parsedType, value = parsedValue)
}

internal fun PersistedCartItemDraft.toFallbackCartItemOrNull(): CartItem? {
    if (productId.isBlank() || cartItemId.isBlank() || productName.isBlank() || quantity <= 0) {
        return null
    }
    val parsedUnitPrice = unitPrice.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return null
    val parsedBaseUnitPrice = baseUnitPrice.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: parsedUnitPrice
    val parsedRetailPrice = retailBaseUnitPrice.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: parsedBaseUnitPrice
    val parsedContractorPrice = contractorBaseUnitPrice.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: BigDecimal.ZERO
    val parsedMasterDiscount = masterUnitDiscount.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: BigDecimal.ZERO
    val parsedSecondaryFactor = secondaryUnitFactor?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
    val resolvedSecondaryMode = isSecondaryUnit && parsedSecondaryFactor != null

    return CartItem(
        productId = productId,
        productName = productName,
        unitPrice = parsedUnitPrice,
        quantity = quantity,
        sku = sku,
        unitName = unitName,
        discountRequest = toCheckoutDiscountOrNull(),
        note = note,
        cartItemId = cartItemId,
        baseUnitPrice = parsedBaseUnitPrice,
        baseUnitName = baseUnitName.ifBlank { unitName },
        secondaryUnitId = secondaryUnitId,
        secondaryUnitName = secondaryUnitName,
        secondaryUnitFactor = parsedSecondaryFactor,
        isSecondaryUnit = resolvedSecondaryMode,
        retailBaseUnitPrice = parsedRetailPrice,
        contractorBaseUnitPrice = parsedContractorPrice,
        priceTiers = emptyList(),
        isContractorPriceApplied = isContractorPriceApplied,
        masterUnitDiscount = parsedMasterDiscount,
        hasManualDiscountOverride = hasManualDiscountOverride,
        isMasterDiscountApplied = isMasterDiscountApplied,
        baseUnitId = baseUnitId
    )
}

internal fun Customer.toPersistedSnapshot(): PersistedCustomerSnapshot {
    return PersistedCustomerSnapshot(
        id = id,
        name = name,
        phone = phone,
        address = address,
        isContractor = isContractor,
        creditLimit = creditLimit.toPlainString(),
        paymentTermDays = paymentTermDays
    )
}

internal fun PersistedCustomerSnapshot.toFallbackCustomerOrNull(): Customer? {
    if (id.isBlank() || name.isBlank()) return null
    return Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        isContractor = isContractor,
        creditLimit = creditLimit.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        paymentTermDays = paymentTermDays.coerceAtLeast(0),
        isActive = true,
        createdAt = "",
        updatedAt = ""
    )
}

internal fun parseDiscountPair(typeRaw: String?, valueRaw: String?): CheckoutDiscount? {
    val typeName = typeRaw?.trim()?.takeIf(String::isNotBlank) ?: return null
    val valueText = valueRaw?.trim()?.takeIf(String::isNotBlank) ?: return null
    val type = runCatching { DiscountType.valueOf(typeName) }.getOrNull() ?: return null
    val value = valueText.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return null
    return CheckoutDiscount(type = type, value = value)
}

internal fun parsePaymentMethodOrDefault(raw: String?): PaymentMethod {
    return PaymentMethod.entries.firstOrNull {
        it.name.equals(raw?.trim(), ignoreCase = true)
    } ?: PaymentMethod.TUNAI
}

internal fun SplitPaymentLineUi.toPersistedLine(): PersistedSplitPaymentLine {
    return PersistedSplitPaymentLine(
        id = id,
        method = method.name,
        amountInput = amountInput,
        reference = reference
    )
}

internal fun PersistedSplitPaymentLine.toSplitPaymentLineUi(): SplitPaymentLineUi {
    return SplitPaymentLineUi(
        id = id.ifBlank { java.util.UUID.randomUUID().toString() },
        method = parsePaymentMethodOrDefault(method),
        amountInput = amountInput,
        reference = reference
    )
}
