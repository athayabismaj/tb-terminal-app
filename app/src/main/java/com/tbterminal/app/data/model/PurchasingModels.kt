package com.tbterminal.app.data.model

import java.math.BigDecimal

data class Supplier(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val paymentTermDays: Int,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class SupplierPage(
    val data: List<Supplier>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class PurchaseItemCommand(
    val productId: String,
    val qty: BigDecimal,
    val price: BigDecimal
)

data class CreatePurchaseCommand(
    val supplierId: String,
    val invoiceNo: String?,
    val paymentMethod: String,
    val amountPaid: BigDecimal,
    val notes: String?,
    val dueDays: Int,
    val items: List<PurchaseItemCommand>
)

data class PurchaseReceipt(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val invoiceNo: String?,
    val total: BigDecimal,
    val receivedAt: String
)

data class PurchaseSummary(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val invoiceNo: String?,
    val total: BigDecimal,
    val receivedAt: String,
    val createdAt: String
)

data class PurchaseSummaryPage(
    val data: List<PurchaseSummary>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class PurchaseDetail(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val invoiceNo: String?,
    val total: BigDecimal,
    val notes: String?,
    val receivedAt: String,
    val createdAt: String,
    val items: List<PurchaseItemDetail>
)

data class PurchaseItemDetail(
    val productId: String,
    val productName: String,
    val quantity: BigDecimal,
    val priceAtTransaction: BigDecimal,
    val subtotal: BigDecimal
)

data class SupplierPayable(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val purchaseId: String,
    val amount: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val dueDate: String,
    val status: String,
    val createdAt: String
)

data class SupplierPayablePage(
    val data: List<SupplierPayable>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class CreateSupplierPaymentCommand(
    val payableId: String,
    val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?
)

data class SupplierPaymentReceipt(
    val id: String,
    val payableId: String,
    val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val payableStatus: String,
    val payableRemainingAmount: BigDecimal
)
