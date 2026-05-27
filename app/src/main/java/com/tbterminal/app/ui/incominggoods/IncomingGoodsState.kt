package com.tbterminal.app.ui.incominggoods

import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import java.math.BigDecimal

data class IncomingGoodsUiState(
    val products: List<ProductStock> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedProduct: ProductStock? = null,
    val selectedSupplier: Supplier? = null,
    val productSearchQuery: String = "",
    val newSupplierNameInput: String = "",
    val invoiceNoInput: String = "",
    val quantityInput: String = "",
    val buyPriceInput: String = "",
    val amountPaidInput: String = "",
    val dueDaysInput: String = DEFAULT_DUE_DAYS.toString(),
    val notesInput: String = "",
    val paymentMethod: IncomingPaymentMethod = IncomingPaymentMethod.CASH,
    val isLoadingProducts: Boolean = false,
    val isLoadingSuppliers: Boolean = false,
    val isSavingSupplier: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
) {
    val quantity: BigDecimal?
        get() = quantityInput.toBigDecimalOrNull()

    val buyPrice: BigDecimal?
        get() = buyPriceInput.toBigDecimalOrNull()

    val total: BigDecimal?
        get() {
            val currentQuantity = quantity ?: return null
            val currentBuyPrice = buyPrice ?: return null
            return currentQuantity.multiply(currentBuyPrice)
        }

    val dueDays: Int?
        get() = dueDaysInput.toIntOrNull()

    val effectiveAmountPaid: BigDecimal?
        get() = when (paymentMethod) {
            IncomingPaymentMethod.CASH,
            IncomingPaymentMethod.TRANSFER,
            IncomingPaymentMethod.QRIS -> total
            IncomingPaymentMethod.DEBT -> BigDecimal.ZERO
            IncomingPaymentMethod.DOWN_PAYMENT -> amountPaidInput.toBigDecimalOrNull()
        }

    val payableAmount: BigDecimal?
        get() {
            val currentTotal = total ?: return null
            val paid = effectiveAmountPaid ?: return null
            return currentTotal.subtract(paid)
        }
}

enum class IncomingPaymentMethod(
    val apiValue: String,
    val label: String,
    val description: String
) {
    CASH("tunai", "Tunai", "Dibayar lunas di kas"),
    TRANSFER("transfer", "Transfer", "Dibayar lunas via bank"),
    QRIS("qris", "QRIS", "Dibayar lunas via QRIS"),
    DEBT("hutang", "Utang", "Masuk ke daftar utang"),
    DOWN_PAYMENT("dp", "DP", "Sebagian dibayar")
}

internal const val INCOMING_GOODS_PRODUCT_LIMIT = 50
internal const val INCOMING_GOODS_SUPPLIER_LIMIT = 50
internal const val DEFAULT_DUE_DAYS = 30

internal fun IncomingGoodsUiState.withSyncedCashPayment(): IncomingGoodsUiState {
    return when (paymentMethod) {
        IncomingPaymentMethod.CASH,
        IncomingPaymentMethod.TRANSFER,
        IncomingPaymentMethod.QRIS -> copy(amountPaidInput = total?.toInputText().orEmpty())
        IncomingPaymentMethod.DEBT,
        IncomingPaymentMethod.DOWN_PAYMENT -> this
    }
}

internal fun String.decimalInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}

internal fun BigDecimal.toInputText(): String {
    return stripTrailingZeros().toPlainString()
}
