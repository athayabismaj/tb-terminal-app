package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
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
    val finalTotal: BigDecimal = BigDecimal.ZERO,
    val checkoutPreview: CheckoutPreview? = null,
    val approvedManagerApprovalId: String? = null,
    val hasAmbiguousCheckout: Boolean = false,
    val isPreviewLoading: Boolean = false,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val amountPaidInput: String = "",
    val activeCashSession: ActiveCashSessionUi? = null,
    val hasActiveCashSession: Boolean = false,
    val isCashSessionLoading: Boolean = false,
    val isOpeningCashSession: Boolean = false,
    val startingCashInput: String = "",
    val cashSessionError: UiText? = null,
    val isProductLoading: Boolean = false,
    val isCustomerLoading: Boolean = false,
    val isLoading: Boolean = false,
    val productError: UiText? = null,
    val customerError: UiText? = null,
    val errorEvent: UiText? = null
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
    val unitName: String = "",
    val categoryName: String = "",
    val stockQty: BigDecimal = BigDecimal.ZERO
)

data class CartItem(
    val productId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val sku: String = "",
    val unitName: String = "",
    val discountRequest: CheckoutDiscount? = null,
    val cartItemId: String = productId
)

enum class PaymentMethod {
    TUNAI,
    TRANSFER,
    QRIS,
    HUTANG,
    DP
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

fun PaymentMethod.displayName(): String {
    return when (this) {
        PaymentMethod.TUNAI -> "Tunai"
        PaymentMethod.TRANSFER -> "Transfer"
        PaymentMethod.QRIS -> "QRIS"
        PaymentMethod.HUTANG -> "Hutang"
        PaymentMethod.DP -> "DP"
    }
}
