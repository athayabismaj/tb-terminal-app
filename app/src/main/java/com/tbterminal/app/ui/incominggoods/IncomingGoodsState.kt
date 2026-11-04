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
    val categoryFilter: String? = null,
    val stockStatusFilter: IncomingStockStatusFilter = IncomingStockStatusFilter.ALL,
    val sortOrder: IncomingProductSortOrder = IncomingProductSortOrder.NEWEST,
    val currentPage: Int = 1,
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

    val movingAverageResult: com.tbterminal.app.data.costing.MovingAverageResult?
        get() {
            val product = selectedProduct ?: return null
            val qty = quantity ?: return null
            val price = buyPrice ?: return null
            if (qty <= BigDecimal.ZERO || price <= BigDecimal.ZERO) return null
            return com.tbterminal.app.data.costing.MovingAverageCalculator.calculateDetails(
                existingStock = product.quantity,
                existingCogs = product.priceBuy,
                incomingQuantity = qty,
                incomingPrice = price,
                scale = 0
            )
        }

    val estimatedNewCogs: BigDecimal?
        get() = movingAverageResult?.newCogs

    val cogsDifference: BigDecimal?
        get() = movingAverageResult?.cogsDifference

    val cogsPercentChange: BigDecimal?
        get() = movingAverageResult?.percentChange

    val inventoryValuationSummary: com.tbterminal.app.data.costing.InventoryValuationSummary
        get() = com.tbterminal.app.data.costing.MovingAverageCalculator.calculateInventoryValuation(products)

    val categoryOptions: List<String>
        get() = products.map(ProductStock::categoryName).distinct().sorted()

    val tableProducts: List<ProductStock>
        get() {
            var filtered = products.filter { product ->
                categoryFilter == null || product.categoryName.equals(categoryFilter, ignoreCase = true)
            }
            when (stockStatusFilter) {
                IncomingStockStatusFilter.ALL -> Unit
                IncomingStockStatusFilter.SAFE -> filtered = filtered.filter { it.quantity > it.minStock }
                IncomingStockStatusFilter.LOW -> filtered = filtered.filter { it.quantity > BigDecimal.ZERO && it.quantity <= it.minStock }
                IncomingStockStatusFilter.OUT_OF_STOCK -> filtered = filtered.filter { it.quantity <= BigDecimal.ZERO }
            }
            return when (sortOrder) {
                IncomingProductSortOrder.NEWEST -> filtered
                IncomingProductSortOrder.NAME_ASC -> filtered.sortedBy { it.productName.lowercase() }
                IncomingProductSortOrder.STOCK_DESC -> filtered.sortedByDescending { it.quantity }
                IncomingProductSortOrder.PRICE_ASC -> filtered.sortedBy { it.priceBuy }
            }
        }

    val isFiltered: Boolean
        get() = categoryFilter != null ||
            stockStatusFilter != IncomingStockStatusFilter.ALL ||
            sortOrder != IncomingProductSortOrder.NEWEST

    val tablePage: Int
        get() = currentPage.coerceIn(1, totalTablePages)

    val tablePageProducts: List<ProductStock>
        get() = tableProducts
            .drop((tablePage - 1) * INCOMING_GOODS_TABLE_PAGE_SIZE)
            .take(INCOMING_GOODS_TABLE_PAGE_SIZE)

    val totalTableProducts: Int
        get() = tableProducts.size

    val totalTablePages: Int
        get() = ((totalTableProducts + INCOMING_GOODS_TABLE_PAGE_SIZE - 1) / INCOMING_GOODS_TABLE_PAGE_SIZE)
            .coerceAtLeast(1)

    val tableStartIndex: Int
        get() = if (totalTableProducts == 0) 0 else ((tablePage - 1) * INCOMING_GOODS_TABLE_PAGE_SIZE) + 1

    val tableEndIndex: Int
        get() = minOf(tablePage * INCOMING_GOODS_TABLE_PAGE_SIZE, totalTableProducts)
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
internal const val INCOMING_GOODS_TABLE_PAGE_SIZE = 10
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


enum class IncomingStockStatusFilter(val label: String) {
    ALL("Semua"),
    SAFE("Aman"),
    LOW("Menipis"),
    OUT_OF_STOCK("Habis");
}

enum class IncomingProductSortOrder(val label: String) {
    NEWEST("Terbaru"),
    NAME_ASC("Nama (A–Z)"),
    STOCK_DESC("Stok Terbanyak"),
    PRICE_ASC("Harga Terendah");
}
