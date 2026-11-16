package com.tbterminal.app.data.model

import java.math.BigDecimal

enum class QuotationStatus(val displayLabel: String) {
    DRAFT("Draft"),
    SENT("Terkirim"),
    CONVERTED("Jadi Transaksi"),
    CANCELLED("Dibatalkan");

    companion object {
        fun fromApi(raw: String?): QuotationStatus =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: DRAFT
    }
}

data class QuotationItemDetail(
    val id: String,
    val productId: String,
    val productName: String,
    val unitId: String,
    val unitName: String? = null,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val subtotal: BigDecimal,
    val note: String? = null,
)

data class QuotationSummary(
    val id: String,
    val quotationNumber: String,
    val userId: String,
    val cashierName: String? = null,
    val customerId: String? = null,
    val customerName: String? = null,
    val status: QuotationStatus,
    val validUntil: String? = null,
    val total: BigDecimal,
    val itemCount: Int,
    val notes: String? = null,
    val convertedTransactionId: String? = null,
    val createdAt: String,
)

data class QuotationPage(
    val data: List<QuotationSummary>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
)

data class QuotationDetail(
    val id: String,
    val quotationNumber: String,
    val userId: String,
    val cashierName: String? = null,
    val customerId: String? = null,
    val customerName: String? = null,
    val status: QuotationStatus,
    val validUntil: String? = null,
    val notes: String? = null,
    val grossSubtotal: BigDecimal,
    val itemDiscountTotal: BigDecimal,
    val transactionDiscountType: DiscountType? = null,
    val transactionDiscountValue: BigDecimal = BigDecimal.ZERO,
    val transactionDiscountAmount: BigDecimal = BigDecimal.ZERO,
    val totalDiscountAmount: BigDecimal,
    val taxEnabled: Boolean = false,
    val taxMode: String? = null,
    val taxRate: BigDecimal = BigDecimal.ZERO,
    val taxLabel: String? = null,
    val taxableBase: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal,
    val convertedTransactionId: String? = null,
    val convertedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val items: List<QuotationItemDetail>,
)

data class CreateQuotationItemCommand(
    val productId: String,
    val qty: BigDecimal,
    val discountRequest: CheckoutDiscount? = null,
    val note: String? = null,
)

data class CreateQuotationCommand(
    val customerId: String? = null,
    val customerName: String? = null,
    val items: List<CreateQuotationItemCommand>,
    val transactionDiscount: CheckoutDiscount? = null,
    val validDays: Int = 14,
    val notes: String? = null,
)
