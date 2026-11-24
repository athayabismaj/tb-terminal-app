package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.ProductPriceTier
import com.tbterminal.app.ui.common.UiText
import java.math.BigDecimal

data class CheckoutUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val productPage: Int = 1,
    val productLimit: Int = 24,
    val productTotal: Long = 0,
    val productTotalPages: Int = 1,
    val customers: List<Customer> = emptyList(),
    val customerSearchQuery: String = "",
    val selectedCustomer: Customer? = null,
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val itemDiscountTotal: BigDecimal = BigDecimal.ZERO,
    val transactionDiscount: CheckoutDiscount? = null,
    val transactionDiscountAmount: BigDecimal = BigDecimal.ZERO,
    val totalDiscount: BigDecimal = BigDecimal.ZERO,
    val storeName: String = "TB TERMINAL",
    val storeNpwp: String? = null,
    val taxEnabled: Boolean = false,
    val taxMode: String = "EXCLUDE",
    val taxRate: BigDecimal = BigDecimal("11.00"),
    val taxLabel: String = "PPN",
    val taxableBase: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val cashRoundingUnit: Int = 0,
    val cashRoundingRule: String = "NEAREST",
    val roundingAmount: BigDecimal = BigDecimal.ZERO,
    val finalTotal: BigDecimal = BigDecimal.ZERO,
    val checkoutPreview: CheckoutPreview? = null,
    val approvedManagerApprovalId: String? = null,
    val hasAmbiguousCheckout: Boolean = false,
    val isPreviewLoading: Boolean = false,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val amountPaidInput: String = "",
    val isSplitPaymentEnabled: Boolean = false,
    val isSplitDpMode: Boolean = false,
    val splitPaymentLines: List<SplitPaymentLineUi> = emptyList(),
    val activeCashSession: ActiveCashSessionUi? = null,
    val hasActiveCashSession: Boolean = false,
    val isCashSessionLoading: Boolean = false,
    val isOpeningCashSession: Boolean = false,
    val startingCashInput: String = "",
    val cashSessionError: UiText? = null,
    val isProductLoading: Boolean = false,
    val isLoadingMoreProducts: Boolean = false,
    val isCustomerLoading: Boolean = false,
    val isLoading: Boolean = false,
    val productError: UiText? = null,
    val customerError: UiText? = null,
    val errorEvent: UiText? = null,
    val scanNotification: String? = null,
    val heldCarts: List<HeldCart> = emptyList(),
    val isHoldCartDialogOpen: Boolean = false,
    val isHeldCartsSheetOpen: Boolean = false,
    val isDeliveryEnabled: Boolean = false,
    val deliveryFee: BigDecimal = BigDecimal.ZERO,
    val deliveryFeeInput: String = "",
    val deliveryAddress: String = "",
    val deliveryCourier: String = "",
    val deliveryFleet: String = "",
    val deliveryRecipientPhone: String = "",
    val deliveryVehiclePlate: String = "",
    val deliverySpecialInstructions: String = "",
    val isScreenLocked: Boolean = false,
    val lockPinInput: String = "",
    val lockPinError: String? = null,
    val isQuickAddCustomerOpen: Boolean = false,
    val isQuickCustomerSubmitting: Boolean = false,
    val isQrisDialogOpen: Boolean = false,
    val isCameraScannerOpen: Boolean = false,
    val quickFeedbackMessage: String? = null,
    val isPettyCashDialogOpen: Boolean = false,
    val pettyCashType: com.tbterminal.app.data.local.cashexpense.CashMovementType = com.tbterminal.app.data.local.cashexpense.CashMovementType.CASH_OUT,
    val pettyCashAmountInput: String = "",
    val pettyCashCategoryInput: String = "Bensin Armada",
    val pettyCashDescriptionInput: String = "",
    val isPettyCashSubmitting: Boolean = false,
    val pettyCashError: String? = null,
    val isReordering: Boolean = false,
    val pendingReorderConflict: PendingReorderConflict? = null,
    val activeQuotationId: String? = null,
    val activeQuotationNumber: String? = null,
    val isSaveQuotationDialogOpen: Boolean = false,
    val isSavingQuotation: Boolean = false,
    val quotationValidDaysInput: String = "14",
    val quotationNotesInput: String = "",
)

data class PendingReorderConflict(
    val receiptNumber: String,
    val plan: CheckoutReorderPlan,
    val customer: Customer? = null,
    val resolvedProducts: Map<String, ReorderResolvedProduct> = emptyMap(),
    val quotationId: String? = null,
    val quotationNumber: String? = null,
    val transactionDiscount: CheckoutDiscount? = null,
)

data class SplitPaymentLineUi(
    val id: String = java.util.UUID.randomUUID().toString(),
    val method: PaymentMethod = PaymentMethod.TUNAI,
    val amountInput: String = "",
    val reference: String = ""
)

data class HeldCart(
    val id: String = java.util.UUID.randomUUID().toString(),
    val note: String,
    val customer: Customer? = null,
    val items: List<CartItem>,
    val transactionDiscount: CheckoutDiscount? = null,
    val deliveryFee: BigDecimal = BigDecimal.ZERO,
    val deliveryAddress: String = "",
    val deliveryCourier: String = "",
    val deliveryFleet: String = "",
    val deliveryRecipientPhone: String = "",
    val deliveryVehiclePlate: String = "",
    val deliverySpecialInstructions: String = "",
    val isDeliveryEnabled: Boolean = false,
    val subtotal: BigDecimal,
    val finalTotal: BigDecimal,
    val createdAt: Long = System.currentTimeMillis()
)

data class ActiveCashSessionUi(
    val localId: Long? = null,
    val serverId: String,
    val cashierUserId: String,
    val openedAt: String,
    val startingCash: BigDecimal,
    val status: String
)

data class Product(
    val productId: String,
    val name: String,
    val unitPrice: BigDecimal,
    val sku: String = "",
    val barcode: String = "",
    val unitName: String = "",
    val categoryName: String = "",
    val stockQty: BigDecimal = BigDecimal.ZERO,
    val secondaryUnitId: String? = null,
    val secondaryUnitName: String? = null,
    val secondaryUnitFactor: BigDecimal? = null,
    val contractorPrice: BigDecimal = BigDecimal.ZERO,
    val priceTiers: List<ProductPriceTier> = emptyList(),
    val masterDiscount: BigDecimal = BigDecimal.ZERO,
    val photoUrl: String? = null
)

data class CartItem(
    val productId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val sku: String = "",
    val unitName: String = "",
    val discountRequest: CheckoutDiscount? = null,
    val note: String? = null,
    val cartItemId: String = productId,
    val baseUnitPrice: BigDecimal = unitPrice,
    val baseUnitName: String = unitName,
    val secondaryUnitId: String? = null,
    val secondaryUnitName: String? = null,
    val secondaryUnitFactor: BigDecimal? = null,
    val isSecondaryUnit: Boolean = false,
    val retailBaseUnitPrice: BigDecimal = baseUnitPrice,
    val contractorBaseUnitPrice: BigDecimal = BigDecimal.ZERO,
    val priceTiers: List<ProductPriceTier> = emptyList(),
    val appliedPriceTierMinQty: BigDecimal? = null,
    val isContractorPriceApplied: Boolean = false,
    val masterUnitDiscount: BigDecimal = BigDecimal.ZERO,
    val hasManualDiscountOverride: Boolean = false,
    val isMasterDiscountApplied: Boolean = false
) {
    val hasSecondaryUnit: Boolean
        get() = secondaryUnitFactor != null && secondaryUnitFactor > BigDecimal.ONE

    val equivalentBaseQuantity: Double
        get() = if (isSecondaryUnit && secondaryUnitFactor != null) {
            quantity.toDouble() * secondaryUnitFactor.toDouble()
        } else {
            quantity.toDouble()
        }

    val equivalentBaseQuantityBigDecimal: BigDecimal
        get() = if (isSecondaryUnit && secondaryUnitFactor != null) {
            quantity.toBigDecimal().multiply(secondaryUnitFactor)
        } else {
            quantity.toBigDecimal()
        }
}

enum class PaymentMethod {
    TUNAI,
    TRANSFER,
    QRIS,
    HUTANG,
    DP;

    fun displayName(): String {
        return when (this) {
            TUNAI -> "Tunai"
            TRANSFER -> "Transfer"
            QRIS -> "QRIS"
            HUTANG -> "Hutang"
            DP -> "DP"
        }
    }
}

fun PaymentMethod.apiValue(): String {
    return when (this) {
        PaymentMethod.TUNAI -> "tunai"
        PaymentMethod.TRANSFER -> "transfer"
        PaymentMethod.QRIS -> "qris"
        PaymentMethod.HUTANG -> "hutang"
        PaymentMethod.DP -> "dp"
    }
}

fun PaymentMethod.displayName(): String = this.displayName()
