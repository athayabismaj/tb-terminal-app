package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.local.database.CheckoutDraftStore
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.Customer
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal data class RestoredCheckoutDraft(
    val activeItems: List<CartItem>,
    val selectedCustomer: Customer?,
    val customerSearchQuery: String,
    val transactionDiscount: CheckoutDiscount?,
    val selectedPaymentMethod: PaymentMethod,
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
    val activeQuotationNumber: String?,
    val heldCarts: List<HeldCart>
)

@OptIn(FlowPreview::class)
internal class CheckoutCartDraftCoordinator(
    private val store: CheckoutDraftStore,
    private val restorer: CheckoutCartDraftRestorer,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS
) {
    private var lastSavedPair: Pair<String?, String?>? = null
    private var observeJob: Job? = null
    @Volatile
    private var stoppedForLogout: Boolean = false

    suspend fun restore(): RestoredCheckoutDraft? {
        val record = store.load() ?: run {
            lastSavedPair = null to null
            return null
        }

        val activeRaw = record.activeCartJson
        val heldRaw = record.heldCartsJson
        val decodedActive = CheckoutCartDraftCodec.decodeActiveCart(activeRaw)
        val decodedHeld = CheckoutCartDraftCodec.decodeHeldCarts(heldRaw)

        val hadCorruptActive = !activeRaw.isNullOrBlank() && decodedActive == null
        val hadCorruptHeld = !heldRaw.isNullOrBlank() && decodedHeld == null
        val sanitizedPair = Pair(
            if (hadCorruptActive) null else activeRaw,
            if (hadCorruptHeld) null else heldRaw
        )
        if (hadCorruptActive || hadCorruptHeld) {
            store.save(sanitizedPair.first, sanitizedPair.second)
        }
        lastSavedPair = sanitizedPair

        if (decodedActive == null && decodedHeld.isNullOrEmpty()) {
            return null
        }

        val activeItems = decodedActive?.let { restorer.restoreCartItems(it.items) }.orEmpty()
        val activeCustomer = decodedActive?.customer?.let { restorer.restoreCustomer(it) }
        val heldCarts = decodedHeld?.let { restorer.restoreHeldCarts(it) }.orEmpty()

        return RestoredCheckoutDraft(
            activeItems = activeItems,
            selectedCustomer = activeCustomer,
            customerSearchQuery = decodedActive?.customerSearchQuery.orEmpty(),
            transactionDiscount = decodedActive?.parsedTransactionDiscount(),
            selectedPaymentMethod = parsePaymentMethodOrDefault(decodedActive?.selectedPaymentMethod),
            isDeliveryEnabled = decodedActive?.isDeliveryEnabled ?: false,
            deliveryFee = decodedActive?.parsedDeliveryFee() ?: BigDecimal.ZERO,
            deliveryFeeInput = decodedActive?.deliveryFeeInput.orEmpty(),
            deliveryAddress = decodedActive?.deliveryAddress.orEmpty(),
            deliveryCourier = decodedActive?.deliveryCourier.orEmpty(),
            deliveryFleet = decodedActive?.deliveryFleet.orEmpty(),
            deliveryRecipientPhone = decodedActive?.deliveryRecipientPhone.orEmpty(),
            deliveryVehiclePlate = decodedActive?.deliveryVehiclePlate.orEmpty(),
            deliverySpecialInstructions = decodedActive?.deliverySpecialInstructions.orEmpty(),
            activeQuotationId = decodedActive?.activeQuotationId,
            activeQuotationNumber = decodedActive?.activeQuotationNumber,
            heldCarts = heldCarts
        )
    }

    fun startObserving(scope: CoroutineScope, uiState: StateFlow<CheckoutUiState>) {
        stoppedForLogout = false
        lastSavedPair = encodePair(uiState.value)
        observeJob?.cancel()
        observeJob = scope.launch {
            val mapped = uiState
                .map { state -> encodePair(state) }
                .distinctUntilChanged()
            val debounced = if (debounceMillis > 0L) {
                mapped.debounce(debounceMillis)
            } else {
                mapped
            }
            debounced.collect { pair ->
                persistIfChanged(pair)
            }
        }
    }

    suspend fun flushNow(state: CheckoutUiState) {
        persistIfChanged(encodePair(state))
    }

    suspend fun flushAndStopForOwner(ownerUserId: String?, state: CheckoutUiState) {
        stoppedForLogout = true
        observeJob?.cancel()
        observeJob = null
        val resolvedOwner = ownerUserId?.trim()?.takeIf(String::isNotBlank) ?: return
        val pair = encodePair(state)
        lastSavedPair = pair
        store.saveForOwner(resolvedOwner, pair.first, pair.second)
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
    }

    private fun encodePair(state: CheckoutUiState): Pair<String?, String?> {
        return CheckoutCartDraftCodec.encodeActiveCart(state) to
            CheckoutCartDraftCodec.encodeHeldCarts(state.heldCarts)
    }

    private suspend fun persistIfChanged(pair: Pair<String?, String?>) {
        if (stoppedForLogout) return
        if (lastSavedPair == pair) return
        lastSavedPair = pair
        store.save(pair.first, pair.second)
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS: Long = 400L
    }
}
