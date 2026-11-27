package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.local.database.PendingSubmissionRecord
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPaymentLine
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CheckoutSubmitItem
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.sync.runSuspendCatching
import java.math.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class PersistedCheckoutSubmitItem(
    val productId: String,
    val quantity: String,
    val discountType: String? = null,
    val discountValue: String? = null,
    val note: String? = null
)

@Serializable
internal data class PersistedCheckoutPaymentLine(
    val method: String,
    val amount: String,
    val reference: String? = null
)

@Serializable
internal data class PersistedCheckoutSubmitCommand(
    val idempotencyKey: String,
    val items: List<PersistedCheckoutSubmitItem>,
    val paymentMethod: String,
    val amountPaid: String,
    val payments: List<PersistedCheckoutPaymentLine>? = null,
    val customerId: String? = null,
    val notes: String? = null,
    val dueDays: Int = 30,
    val transactionDiscountType: String? = null,
    val transactionDiscountValue: String? = null,
    val checkoutAttemptId: String? = null,
    val managerApprovalId: String? = null
)

@Serializable
internal data class PersistedCheckoutPendingSubmission(
    val command: PersistedCheckoutSubmitCommand,
    val cartItems: List<PersistedCartItemDraft> = emptyList(),
    val customer: PersistedCustomerSnapshot? = null,
    val customerSearchQuery: String = "",
    val transactionDiscountType: String? = null,
    val transactionDiscountValue: String? = null,
    val selectedPaymentMethod: String = PaymentMethod.TUNAI.name,
    val amountPaidInput: String = "",
    val isSplitPaymentEnabled: Boolean = false,
    val isSplitDpMode: Boolean = false,
    val splitPaymentLines: List<PersistedSplitPaymentLine> = emptyList(),
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

internal data class RestoredAmbiguousCheckout(
    val idempotencyKey: String,
    val fingerprint: String,
    val command: CheckoutSubmitCommand,
    val cartItems: List<CartItem>,
    val customer: Customer?,
    val customerSearchQuery: String,
    val transactionDiscount: CheckoutDiscount?,
    val selectedPaymentMethod: PaymentMethod,
    val amountPaidInput: String,
    val isSplitPaymentEnabled: Boolean,
    val isSplitDpMode: Boolean,
    val splitPaymentLines: List<SplitPaymentLineUi>,
    val isDeliveryEnabled: Boolean,
    val deliveryFee: BigDecimal,
    val deliveryFeeInput: String,
    val deliveryAddress: String,
    val deliveryCourier: String,
    val deliveryFleet: String,
    val deliveryRecipientPhone: String,
    val deliveryVehiclePlate: String,
    val deliverySpecialInstructions: String,
    val activeQuotationId: String?,
    val activeQuotationNumber: String?
)

internal class CheckoutPendingSubmissionPersistence(
    private val store: PendingSubmissionStore,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private var activeKey: String? = null
    private var activeCreatedAt: Long? = null

    suspend fun load(): RestoredAmbiguousCheckout? {
        val record = store.load(PendingSubmissionScopes.CHECKOUT) ?: return null
        val payloadRaw = record.payloadJson?.takeIf(String::isNotBlank)
        if (payloadRaw == null) {
            store.clear(PendingSubmissionScopes.CHECKOUT)
            return null
        }

        val payload = runSuspendCatching {
            CheckoutPersistenceJson.decodeFromString<PersistedCheckoutPendingSubmission>(payloadRaw)
        }.getOrElse {
            store.clear(PendingSubmissionScopes.CHECKOUT)
            return null
        }

        val command = payload.command.toDomainOrNull(expectedKey = record.idempotencyKey)
        if (command == null) {
            store.clear(PendingSubmissionScopes.CHECKOUT)
            return null
        }

        val cartItems = payload.cartItems.mapNotNull { it.toFallbackCartItemOrNull() }
        if (payload.cartItems.isNotEmpty() && cartItems.isEmpty()) {
            store.clear(PendingSubmissionScopes.CHECKOUT)
            return null
        }

        activeKey = record.idempotencyKey
        activeCreatedAt = record.createdAt

        return RestoredAmbiguousCheckout(
            idempotencyKey = record.idempotencyKey,
            fingerprint = record.intentJson,
            command = command,
            cartItems = cartItems,
            customer = payload.customer?.toFallbackCustomerOrNull(),
            customerSearchQuery = payload.customerSearchQuery,
            transactionDiscount = parseDiscountPair(
                payload.transactionDiscountType,
                payload.transactionDiscountValue
            ),
            selectedPaymentMethod = parsePaymentMethodOrDefault(payload.selectedPaymentMethod),
            amountPaidInput = payload.amountPaidInput,
            isSplitPaymentEnabled = payload.isSplitPaymentEnabled,
            isSplitDpMode = payload.isSplitDpMode,
            splitPaymentLines = payload.splitPaymentLines.map { it.toSplitPaymentLineUi() },
            isDeliveryEnabled = payload.isDeliveryEnabled,
            deliveryFee = payload.deliveryFee.toBigDecimalOrNull()?.coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO,
            deliveryFeeInput = payload.deliveryFeeInput,
            deliveryAddress = payload.deliveryAddress,
            deliveryCourier = payload.deliveryCourier,
            deliveryFleet = payload.deliveryFleet,
            deliveryRecipientPhone = payload.deliveryRecipientPhone,
            deliveryVehiclePlate = payload.deliveryVehiclePlate,
            deliverySpecialInstructions = payload.deliverySpecialInstructions,
            activeQuotationId = payload.activeQuotationId,
            activeQuotationNumber = payload.activeQuotationNumber
        )
    }

    suspend fun saveInFlight(
        command: CheckoutSubmitCommand,
        fingerprint: String,
        state: CheckoutUiState
    ) {
        save(command, fingerprint, state, PendingSubmissionStatus.IN_FLIGHT)
    }

    suspend fun markAmbiguous(
        command: CheckoutSubmitCommand,
        fingerprint: String,
        state: CheckoutUiState
    ) {
        save(command, fingerprint, state, PendingSubmissionStatus.AMBIGUOUS)
    }

    suspend fun saveForOwner(
        ownerUserId: String,
        command: CheckoutSubmitCommand,
        fingerprint: String,
        state: CheckoutUiState,
        status: PendingSubmissionStatus = PendingSubmissionStatus.AMBIGUOUS,
    ) {
        val resolvedOwner = ownerUserId.trim().takeIf(String::isNotBlank) ?: return
        val record = buildRecord(command, fingerprint, state, status)
        store.saveForOwner(resolvedOwner, record)
    }

    suspend fun clear() {
        activeKey = null
        activeCreatedAt = null
        store.clear(PendingSubmissionScopes.CHECKOUT)
    }

    private suspend fun save(
        command: CheckoutSubmitCommand,
        fingerprint: String,
        state: CheckoutUiState,
        status: PendingSubmissionStatus
    ) {
        store.save(buildRecord(command, fingerprint, state, status))
    }

    private fun buildRecord(
        command: CheckoutSubmitCommand,
        fingerprint: String,
        state: CheckoutUiState,
        status: PendingSubmissionStatus,
    ): PendingSubmissionRecord {
        val now = clockMillis()
        val createdAt = if (activeKey == command.idempotencyKey) {
            activeCreatedAt ?: now
        } else {
            now
        }
        activeKey = command.idempotencyKey
        activeCreatedAt = createdAt

        val payload = PersistedCheckoutPendingSubmission(
            command = command.toPersisted(),
            cartItems = state.cartItems.map { it.toPersistedDraft() },
            customer = state.selectedCustomer?.toPersistedSnapshot(),
            customerSearchQuery = state.customerSearchQuery,
            transactionDiscountType = state.transactionDiscount?.type?.name,
            transactionDiscountValue = state.transactionDiscount?.value?.stripTrailingZeros()?.toPlainString(),
            selectedPaymentMethod = state.selectedPaymentMethod.name,
            amountPaidInput = state.amountPaidInput,
            isSplitPaymentEnabled = state.isSplitPaymentEnabled,
            isSplitDpMode = state.isSplitDpMode,
            splitPaymentLines = state.splitPaymentLines.map { it.toPersistedLine() },
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

        val payloadJson = CheckoutPersistenceJson.encodeToString(payload)
        return PendingSubmissionRecord(
            scope = PendingSubmissionScopes.CHECKOUT,
            idempotencyKey = command.idempotencyKey,
            intentJson = fingerprint,
            payloadJson = payloadJson,
            status = status,
            createdAt = createdAt,
            updatedAt = now
        )
    }
}

private fun CheckoutSubmitCommand.toPersisted(): PersistedCheckoutSubmitCommand {
    return PersistedCheckoutSubmitCommand(
        idempotencyKey = idempotencyKey,
        items = items.map { item ->
            PersistedCheckoutSubmitItem(
                productId = item.productId,
                quantity = item.quantity.stripTrailingZeros().toPlainString(),
                discountType = item.discountRequest?.type?.name,
                discountValue = item.discountRequest?.value?.stripTrailingZeros()?.toPlainString(),
                note = item.note
            )
        },
        paymentMethod = paymentMethod,
        amountPaid = amountPaid,
        payments = payments?.map { line ->
            PersistedCheckoutPaymentLine(
                method = line.method,
                amount = line.amount,
                reference = line.reference
            )
        },
        customerId = customerId,
        notes = notes,
        dueDays = dueDays,
        transactionDiscountType = transactionDiscount?.type?.name,
        transactionDiscountValue = transactionDiscount?.value?.stripTrailingZeros()?.toPlainString(),
        checkoutAttemptId = checkoutAttemptId,
        managerApprovalId = managerApprovalId
    )
}

private fun PersistedCheckoutSubmitCommand.toDomainOrNull(expectedKey: String): CheckoutSubmitCommand? {
    if (idempotencyKey.isBlank() || idempotencyKey != expectedKey || items.isEmpty()) {
        return null
    }
    if (amountPaid.toBigDecimalOrNull() == null) return null

    val domainItems = items.map { item ->
        val qty = item.quantity.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
        val itemDiscount = parseDiscountPair(item.discountType, item.discountValue)
        if (item.discountType != null && itemDiscount == null) return null
        CheckoutSubmitItem(
            productId = item.productId,
            quantity = qty,
            discountRequest = itemDiscount,
            note = item.note
        )
    }

    val txDiscount = parseDiscountPair(transactionDiscountType, transactionDiscountValue)
    if (transactionDiscountType != null && txDiscount == null) return null

    val domainPayments = payments?.map { line ->
        if (line.amount.toBigDecimalOrNull() == null) return null
        CheckoutPaymentLine(
            method = line.method,
            amount = line.amount,
            reference = line.reference
        )
    }

    return CheckoutSubmitCommand(
        idempotencyKey = idempotencyKey,
        items = domainItems,
        paymentMethod = paymentMethod,
        amountPaid = amountPaid,
        payments = domainPayments,
        customerId = customerId,
        notes = notes,
        dueDays = dueDays,
        transactionDiscount = txDiscount,
        checkoutAttemptId = checkoutAttemptId,
        managerApprovalId = managerApprovalId
    )
}
