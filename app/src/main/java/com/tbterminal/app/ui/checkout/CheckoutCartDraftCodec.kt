package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.sync.runSuspendCatching
import java.math.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class PersistedActiveCartDraft(
    val items: List<PersistedCartItemDraft> = emptyList(),
    val customer: PersistedCustomerSnapshot? = null,
    val customerSearchQuery: String = "",
    val transactionDiscountType: String? = null,
    val transactionDiscountValue: String? = null,
    val selectedPaymentMethod: String = PaymentMethod.TUNAI.name,
    val isDeliveryEnabled: Boolean = false,
    val deliveryFee: String = "0",
    val deliveryFeeInput: String = "",
    val deliveryAddress: String = "",
    val deliveryCourier: String = "",
    val deliveryFleet: String = "",
    val deliveryRecipientPhone: String = "",
    val deliveryVehiclePlate: String = "",
    val deliverySpecialInstructions: String = "",
    val activeQuotationId: String? = null,
    val activeQuotationNumber: String? = null
)

@Serializable
internal data class PersistedHeldCartDraft(
    val id: String,
    val note: String,
    val createdAt: Long,
    val items: List<PersistedCartItemDraft> = emptyList(),
    val customer: PersistedCustomerSnapshot? = null,
    val transactionDiscountType: String? = null,
    val transactionDiscountValue: String? = null,
    val isDeliveryEnabled: Boolean = false,
    val deliveryFee: String = "0",
    val deliveryAddress: String = "",
    val deliveryCourier: String = "",
    val deliveryFleet: String = "",
    val deliveryRecipientPhone: String = "",
    val deliveryVehiclePlate: String = "",
    val deliverySpecialInstructions: String = "",
    val subtotal: String = "0",
    val finalTotal: String = "0"
)

@Serializable
internal data class PersistedHeldCartsDraft(
    val carts: List<PersistedHeldCartDraft> = emptyList()
)

internal object CheckoutCartDraftCodec {
    fun encodeActiveCart(state: CheckoutUiState): String? {
        val hasDraftContent = state.cartItems.isNotEmpty() ||
            state.selectedCustomer != null ||
            state.transactionDiscount != null ||
            state.isDeliveryEnabled ||
            !state.activeQuotationId.isNullOrBlank()
        if (!hasDraftContent) return null

        val dto = PersistedActiveCartDraft(
            items = state.cartItems.map { it.toPersistedDraft() },
            customer = state.selectedCustomer?.toPersistedSnapshot(),
            customerSearchQuery = state.customerSearchQuery,
            transactionDiscountType = state.transactionDiscount?.type?.name,
            transactionDiscountValue = state.transactionDiscount?.value?.stripTrailingZeros()?.toPlainString(),
            selectedPaymentMethod = state.selectedPaymentMethod.name,
            isDeliveryEnabled = state.isDeliveryEnabled,
            deliveryFee = state.deliveryFee.toPlainString(),
            deliveryFeeInput = state.deliveryFeeInput,
            deliveryAddress = state.deliveryAddress,
            deliveryCourier = state.deliveryCourier,
            deliveryFleet = state.deliveryFleet,
            deliveryRecipientPhone = state.deliveryRecipientPhone,
            deliveryVehiclePlate = state.deliveryVehiclePlate,
            deliverySpecialInstructions = state.deliverySpecialInstructions,
            activeQuotationId = state.activeQuotationId,
            activeQuotationNumber = state.activeQuotationNumber
        )
        return CheckoutPersistenceJson.encodeToString(dto)
    }

    fun encodeHeldCarts(heldCarts: List<HeldCart>): String? {
        if (heldCarts.isEmpty()) return null
        val dto = PersistedHeldCartsDraft(
            carts = heldCarts.map { held ->
                PersistedHeldCartDraft(
                    id = held.id,
                    note = held.note,
                    createdAt = held.createdAt,
                    items = held.items.map { it.toPersistedDraft() },
                    customer = held.customer?.toPersistedSnapshot(),
                    transactionDiscountType = held.transactionDiscount?.type?.name,
                    transactionDiscountValue = held.transactionDiscount?.value?.stripTrailingZeros()?.toPlainString(),
                    isDeliveryEnabled = held.isDeliveryEnabled,
                    deliveryFee = held.deliveryFee.toPlainString(),
                    deliveryAddress = held.deliveryAddress,
                    deliveryCourier = held.deliveryCourier,
                    deliveryFleet = held.deliveryFleet,
                    deliveryRecipientPhone = held.deliveryRecipientPhone,
                    deliveryVehiclePlate = held.deliveryVehiclePlate,
                    deliverySpecialInstructions = held.deliverySpecialInstructions,
                    subtotal = held.subtotal.toPlainString(),
                    finalTotal = held.finalTotal.toPlainString()
                )
            }
        )
        return CheckoutPersistenceJson.encodeToString(dto)
    }

    fun decodeActiveCart(raw: String?): PersistedActiveCartDraft? {
        val text = raw?.takeIf(String::isNotBlank) ?: return null
        val decoded = runSuspendCatching {
            CheckoutPersistenceJson.decodeFromString<PersistedActiveCartDraft>(text)
        }.getOrNull() ?: return null

        val validItems = decoded.items.mapNotNull { draft ->
            draft.takeIf { it.toFallbackCartItemOrNull() != null }
        }
        if (decoded.items.isNotEmpty() && validItems.isEmpty()) {
            return null
        }
        return decoded.copy(items = validItems)
    }

    fun decodeHeldCarts(raw: String?): List<PersistedHeldCartDraft>? {
        val text = raw?.takeIf(String::isNotBlank) ?: return null
        val decoded = runSuspendCatching {
            CheckoutPersistenceJson.decodeFromString<PersistedHeldCartsDraft>(text)
        }.getOrNull() ?: return null

        val validCarts = decoded.carts.mapNotNull { cart ->
            if (cart.id.isBlank() || cart.note.isBlank()) return@mapNotNull null
            val validItems = cart.items.mapNotNull { draft ->
                draft.takeIf { it.toFallbackCartItemOrNull() != null }
            }
            if (validItems.isEmpty()) return@mapNotNull null
            cart.copy(items = validItems)
        }
        if (decoded.carts.isNotEmpty() && validCarts.isEmpty()) {
            return null
        }
        return validCarts
    }
}

internal fun PersistedActiveCartDraft.parsedTransactionDiscount(): CheckoutDiscount? =
    parseDiscountPair(transactionDiscountType, transactionDiscountValue)

internal fun PersistedHeldCartDraft.parsedTransactionDiscount(): CheckoutDiscount? =
    parseDiscountPair(transactionDiscountType, transactionDiscountValue)

internal fun PersistedActiveCartDraft.parsedDeliveryFee(): BigDecimal =
    deliveryFee.toBigDecimalOrNull()?.coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO

internal fun PersistedHeldCartDraft.parsedDeliveryFee(): BigDecimal =
    deliveryFee.toBigDecimalOrNull()?.coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO

internal fun PersistedCustomerSnapshot?.toFallbackOrNull(): Customer? =
    this?.toFallbackCustomerOrNull()
