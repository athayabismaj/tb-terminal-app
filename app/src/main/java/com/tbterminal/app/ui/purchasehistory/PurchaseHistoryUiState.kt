package com.tbterminal.app.ui.purchasehistory

import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.data.model.Supplier
import java.math.BigDecimal

data class PurchaseHistoryUiState(
    val purchases: List<PurchaseSummary> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: String? = null,
    val selectedPurchase: PurchaseDetail? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalPurchases: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null
) {
    val pageTotal: BigDecimal
        get() = purchases.fold(BigDecimal.ZERO) { total, purchase -> total.add(purchase.total) }
}
