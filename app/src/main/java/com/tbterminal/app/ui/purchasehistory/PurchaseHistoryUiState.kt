package com.tbterminal.app.ui.purchasehistory

import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.data.model.Supplier
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

fun todayPurchaseDateString(): String = LocalDate.now(WIB_ZONE).toString()

enum class PurchaseSortOrder(val label: String) {
    NEWEST("Terbaru"),
    NAME_ASC("Nama (A–Z)"),
    HIGHEST_AMOUNT("Nominal Terbesar"),
    DUE_NEAREST("Tempo Terdekat")
}

data class PurchaseHistoryUiState(
    val purchases: List<PurchaseSummary> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: String? = null,
    val startDate: String? = todayPurchaseDateString(),
    val endDate: String? = todayPurchaseDateString(),
    val selectedPeriodType: String = "Hari Ini",
    val sortOrder: PurchaseSortOrder = PurchaseSortOrder.NEWEST,
    val searchQuery: String = "",
    val selectedPurchase: PurchaseDetail? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalPurchases: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null
) {
    val isFiltered: Boolean
        get() {
            val today = todayPurchaseDateString()
            val hasSupplierFilter = selectedSupplierId != null
            val hasDateFilter = when (selectedPeriodType) {
                "Semua" -> true
                "Hari Ini", "Hari" -> startDate != today || endDate != today
                else -> true
            }
            val hasSortFilter = sortOrder != PurchaseSortOrder.NEWEST
            return hasSupplierFilter || hasDateFilter || hasSortFilter
        }

    val visiblePurchases: List<PurchaseSummary>
        get() {
            val query = searchQuery.trim()
            var list = if (query.isBlank()) purchases else {
                purchases.filter { purchase ->
                    purchase.invoiceNo.orEmpty().contains(query, ignoreCase = true) ||
                        purchase.supplierName.contains(query, ignoreCase = true)
                }
            }

            if (selectedSupplierId != null) {
                list = list.filter { it.supplierId == selectedSupplierId }
            }

            if (selectedPeriodType != "Semua") {
                val start = startDate?.take(10)
                val end = endDate?.take(10)
                if (start != null || end != null) {
                    list = list.filter { purchase ->
                        val purchaseDate = purchase.receivedAt.take(10)
                        val matchesStart = start == null || purchaseDate >= start
                        val matchesEnd = end == null || purchaseDate <= end
                        matchesStart && matchesEnd
                    }
                }
            }

            return when (sortOrder) {
                PurchaseSortOrder.NEWEST -> list.sortedByDescending { it.receivedAt }
                PurchaseSortOrder.NAME_ASC -> list.sortedBy { it.supplierName.lowercase() }
                PurchaseSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.total }
                PurchaseSortOrder.DUE_NEAREST -> list.sortedBy { it.receivedAt }
            }
        }

    val effectiveTotalPurchases: Long
        get() {
            if (visiblePurchases.isEmpty() || purchases.isEmpty()) return 0L
            if (visiblePurchases.size < purchases.size) {
                return visiblePurchases.size.toLong()
            }
            return totalPurchases
        }

    val pageTotal: BigDecimal
        get() = visiblePurchases.fold(BigDecimal.ZERO) { total, purchase -> total.add(purchase.total) }

    val pageStartIndex: Long
        get() = if (visiblePurchases.isEmpty() || effectiveTotalPurchases == 0L) 0L else ((page - 1L) * pageSize) + 1L

    val pageEndIndex: Long
        get() = if (visiblePurchases.isEmpty() || effectiveTotalPurchases == 0L) 0L else minOf(page.toLong() * pageSize, effectiveTotalPurchases)
}
