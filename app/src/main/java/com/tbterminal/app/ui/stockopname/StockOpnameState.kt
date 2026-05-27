package com.tbterminal.app.ui.stockopname

import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.StockAdjustment
import java.math.BigDecimal

data class StockOpnameUiState(
    val products: List<ProductStock> = emptyList(),
    val latestAdjustmentsByProductId: Map<String, StockAdjustment> = emptyMap(),
    val selectedProduct: ProductStock? = null,
    val searchQuery: String = "",
    val categoryFilter: String? = null,
    val actualQtyInput: String = "",
    val notesInput: String = "",
    val adjustmentType: StockAdjustmentType = StockAdjustmentType.OPNAME,
    val currentPage: Int = 1,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
) {
    val actualQty: BigDecimal?
        get() = actualQtyInput.toBigDecimalOrNull()

    val difference: BigDecimal?
        get() = selectedProduct?.let { product -> actualQty?.subtract(product.quantity) }

    val categoryOptions: List<String>
        get() = products.map(ProductStock::categoryName).distinct().sorted()

    val tableProducts: List<ProductStock>
        get() = products.filter { product ->
            categoryFilter == null || product.categoryName == categoryFilter
        }

    val tablePage: Int
        get() = currentPage.coerceIn(1, totalTablePages)

    val tablePageProducts: List<ProductStock>
        get() = tableProducts
            .drop((tablePage - 1) * STOCK_OPNAME_TABLE_PAGE_SIZE)
            .take(STOCK_OPNAME_TABLE_PAGE_SIZE)

    val totalTableProducts: Int
        get() = tableProducts.size

    val totalTablePages: Int
        get() = ((totalTableProducts + STOCK_OPNAME_TABLE_PAGE_SIZE - 1) / STOCK_OPNAME_TABLE_PAGE_SIZE)
            .coerceAtLeast(1)

    val tableStartIndex: Int
        get() = if (totalTableProducts == 0) 0 else ((tablePage - 1) * STOCK_OPNAME_TABLE_PAGE_SIZE) + 1

    val tableEndIndex: Int
        get() = minOf(tablePage * STOCK_OPNAME_TABLE_PAGE_SIZE, totalTableProducts)
}

enum class StockAdjustmentType(
    val apiValue: String,
    val label: String,
    val description: String
) {
    OPNAME("OPNAME", "Opname", "Hasil hitung fisik rutin"),
    CORRECTION("CORRECTION", "Koreksi", "Perbaikan data administrasi"),
    DAMAGE("DAMAGE", "Rusak/Retur", "Barang rusak atau retur supplier")
}

internal const val STOCK_OPNAME_PAGE_SIZE = 50
internal const val STOCK_OPNAME_TABLE_PAGE_SIZE = 10
internal const val STOCK_OPNAME_ADJUSTMENT_LOOKUP_LIMIT = 200

internal fun String.quantityInput(): String {
    return filter { char -> char.isDigit() || char == '.' || char == ',' }
        .replace(',', '.')
}
