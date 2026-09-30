package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreatePurchaseCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.PurchaseItemCommand
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseReceipt
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.data.model.PurchaseSummaryPage
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.model.SupplierPayablePage
import com.tbterminal.app.data.model.SupplierPage
import com.tbterminal.app.data.model.SupplierPaymentReceipt
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.PayableResponseDto
import com.tbterminal.app.data.remote.PurchaseItemRequestDto
import com.tbterminal.app.data.remote.PurchaseRequestDto
import com.tbterminal.app.data.remote.PurchaseResponseDto
import com.tbterminal.app.data.remote.PurchaseSummaryDto
import com.tbterminal.app.data.remote.PurchasingApi
import com.tbterminal.app.data.remote.SupplierPaymentRequestDto
import com.tbterminal.app.data.remote.SupplierPaymentResponseDto
import com.tbterminal.app.data.remote.SupplierRequestDto
import com.tbterminal.app.data.remote.SupplierResponseDto
import com.tbterminal.app.data.remote.safeApiCall

interface PurchasingRepository {
    suspend fun getSuppliers(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ): RepositoryResult<SupplierPage>

    suspend fun createSupplier(
        name: String,
        phone: String? = null,
        address: String? = null,
        paymentTermDays: Int = 30
    ): RepositoryResult<Supplier>

    suspend fun updateSupplier(
        id: String,
        name: String,
        phone: String? = null,
        address: String? = null,
        paymentTermDays: Int = 30
    ): RepositoryResult<Supplier>

    suspend fun deleteSupplier(id: String): RepositoryResult<Unit>

    suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt>

    suspend fun getPurchases(
        page: Int = 1,
        limit: Int = 20,
        supplierId: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): RepositoryResult<PurchaseSummaryPage>

    suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail>

    suspend fun getPayables(
        page: Int = 1,
        limit: Int = 20,
        supplierId: String? = null,
        status: String? = null
    ): RepositoryResult<SupplierPayablePage>

    suspend fun getPayableById(id: String): RepositoryResult<SupplierPayable>

    suspend fun createSupplierPayment(
        command: CreateSupplierPaymentCommand
    ): RepositoryResult<SupplierPaymentReceipt>
}

class RemotePurchasingRepository(
    private val purchasingApi: PurchasingApi
) : PurchasingRepository {
    override suspend fun getSuppliers(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<SupplierPage> {
        return safeApiCall {
            purchasingApi.getSuppliers(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val supplierPage = response.data
            if (!response.success || supplierPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "SUPPLIERS_FAILED",
                    message = response.message ?: response.error ?: "Supplier gagal dimuat."
                )
            } else {
                RepositoryResult.Success(supplierPage.toSupplierPage())
            }
        }
    }

    override suspend fun createSupplier(
        name: String,
        phone: String?,
        address: String?,
        paymentTermDays: Int
    ): RepositoryResult<Supplier> {
        val request = SupplierRequestDto(
            name = name,
            phone = phone,
            address = address,
            paymentTermDays = paymentTermDays
        )

        return safeApiCall { purchasingApi.createSupplier(request) }
            .toRepositoryResult { response ->
                val supplier = response.data
                if (!response.success || supplier == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_SUPPLIER_FAILED",
                        message = response.message ?: response.error ?: "Supplier gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(supplier.toSupplier())
                }
            }
    }

    override suspend fun updateSupplier(
        id: String,
        name: String,
        phone: String?,
        address: String?,
        paymentTermDays: Int
    ): RepositoryResult<Supplier> {
        val request = SupplierRequestDto(
            name = name,
            phone = phone,
            address = address,
            paymentTermDays = paymentTermDays
        )

        return safeApiCall { purchasingApi.updateSupplier(id, request) }
            .toRepositoryResult { response ->
                val supplier = response.data
                if (!response.success || supplier == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_SUPPLIER_FAILED",
                        message = response.message ?: response.error ?: "Supplier gagal diperbarui."
                    )
                } else {
                    RepositoryResult.Success(supplier.toSupplier())
                }
            }
    }

    override suspend fun deleteSupplier(id: String): RepositoryResult<Unit> {
        return safeApiCall { purchasingApi.deleteSupplier(id) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "DELETE_SUPPLIER_FAILED",
                        message = response.message ?: response.error ?: "Supplier gagal dinonaktifkan."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }

    override suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt> {
        val request = PurchaseRequestDto(
            supplierId = command.supplierId,
            invoiceNo = command.invoiceNo?.takeIf(String::isNotBlank),
            paymentMethod = command.paymentMethod,
            amountPaid = command.amountPaid,
            notes = command.notes?.takeIf(String::isNotBlank),
            dueDays = command.dueDays,
            items = command.items.map(PurchaseItemCommand::toRequestDto)
        )

        return safeApiCall { purchasingApi.createPurchase(request) }
            .toRepositoryResult { response ->
                val purchase = response.data
                if (!response.success || purchase == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_PURCHASE_FAILED",
                        message = response.message ?: response.error ?: "Barang masuk gagal dicatat."
                    )
                } else {
                    RepositoryResult.Success(purchase.toPurchaseReceipt())
                }
            }
    }

    override suspend fun getPurchases(
        page: Int,
        limit: Int,
        supplierId: String?,
        startDate: String?,
        endDate: String?
    ): RepositoryResult<PurchaseSummaryPage> {
        return safeApiCall {
            purchasingApi.getPurchases(
                page = page,
                limit = limit,
                supplierId = supplierId?.takeIf(String::isNotBlank),
                startDate = startDate,
                endDate = endDate
            )
        }.toRepositoryResult { response ->
            val purchasePage = response.data
            if (!response.success || purchasePage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PURCHASES_FAILED",
                    message = response.message ?: response.error ?: "Nota pembelian gagal dimuat."
                )
            } else {
                RepositoryResult.Success(purchasePage.toPurchaseSummaryPage())
            }
        }
    }

    override suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail> {
        return safeApiCall { purchasingApi.getPurchaseById(id) }
            .toRepositoryResult { response ->
                val purchase = response.data
                if (!response.success || purchase == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "PURCHASE_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail nota pembelian gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(purchase.toPurchaseDetail())
                }
            }
    }

    override suspend fun getPayables(
        page: Int,
        limit: Int,
        supplierId: String?,
        status: String?
    ): RepositoryResult<SupplierPayablePage> {
        return safeApiCall {
            purchasingApi.getPayables(
                page = page,
                limit = limit,
                supplierId = supplierId?.takeIf(String::isNotBlank),
                status = status?.takeIf(String::isNotBlank)
            )
        }.toRepositoryResult { response ->
            val payablePage = response.data
            if (!response.success || payablePage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PAYABLES_FAILED",
                    message = response.message ?: response.error ?: "Utang supplier gagal dimuat."
                )
            } else {
                RepositoryResult.Success(payablePage.toSupplierPayablePage())
            }
        }
    }

    override suspend fun getPayableById(id: String): RepositoryResult<SupplierPayable> {
        return safeApiCall { purchasingApi.getPayableById(id) }
            .toRepositoryResult { response ->
                val payable = response.data
                if (!response.success || payable == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "PAYABLE_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail utang gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(payable.toSupplierPayable())
                }
            }
    }

    override suspend fun createSupplierPayment(
        command: CreateSupplierPaymentCommand
    ): RepositoryResult<SupplierPaymentReceipt> {
        val request = SupplierPaymentRequestDto(
            payableId = command.payableId,
            amount = command.amount,
            method = command.method,
            reference = command.reference?.takeIf(String::isNotBlank),
            notes = command.notes?.takeIf(String::isNotBlank)
        )

        return safeApiCall { purchasingApi.createSupplierPayment(request) }
            .toRepositoryResult { response ->
                val payment = response.data
                if (!response.success || payment == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "SUPPLIER_PAYMENT_FAILED",
                        message = response.message ?: response.error ?: "Pembayaran utang gagal dicatat."
                    )
                } else {
                    RepositoryResult.Success(payment.toSupplierPaymentReceipt())
                }
            }
    }
}

private fun PaginatedResponse<SupplierResponseDto>.toSupplierPage(): SupplierPage {
    return SupplierPage(
        data = data.map(SupplierResponseDto::toSupplier),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun SupplierResponseDto.toSupplier(): Supplier {
    return Supplier(
        id = id,
        name = name,
        phone = phone,
        address = address,
        paymentTermDays = paymentTermDays,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun PaginatedResponse<PurchaseSummaryDto>.toPurchaseSummaryPage(): PurchaseSummaryPage {
    return PurchaseSummaryPage(
        data = data.map(PurchaseSummaryDto::toPurchaseSummary),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun PurchaseSummaryDto.toPurchaseSummary(): PurchaseSummary {
    return PurchaseSummary(
        id = id,
        supplierId = supplierId,
        supplierName = supplierName,
        invoiceNo = invoiceNo,
        total = total,
        receivedAt = receivedAt,
        createdAt = createdAt
    )
}

private fun PurchaseResponseDto.toPurchaseDetail(): PurchaseDetail {
    return PurchaseDetail(
        id = id,
        supplierId = supplierId,
        supplierName = supplierName,
        invoiceNo = invoiceNo,
        total = total,
        notes = notes,
        receivedAt = receivedAt,
        createdAt = createdAt,
        items = items.map {
            com.tbterminal.app.data.model.PurchaseItemDetail(
                productId = it.productId,
                productName = it.productName,
                quantity = it.quantity,
                priceAtTransaction = it.priceAtTransaction,
                subtotal = it.subtotal
            )
        }
    )
}

private fun PurchaseItemCommand.toRequestDto(): PurchaseItemRequestDto {
    return PurchaseItemRequestDto(
        productId = productId,
        qty = qty,
        price = price
    )
}

private fun PurchaseResponseDto.toPurchaseReceipt(): PurchaseReceipt {
    return PurchaseReceipt(
        id = id,
        supplierId = supplierId,
        supplierName = supplierName,
        invoiceNo = invoiceNo,
        total = total,
        receivedAt = receivedAt
    )
}

private fun PaginatedResponse<PayableResponseDto>.toSupplierPayablePage(): SupplierPayablePage {
    return SupplierPayablePage(
        data = data.map(PayableResponseDto::toSupplierPayable),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun PayableResponseDto.toSupplierPayable(): SupplierPayable {
    return SupplierPayable(
        id = id,
        supplierId = supplierId,
        supplierName = supplierName,
        purchaseId = purchaseId,
        amount = amount,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        dueDate = dueDate,
        status = status,
        createdAt = createdAt
    )
}

private fun SupplierPaymentResponseDto.toSupplierPaymentReceipt(): SupplierPaymentReceipt {
    return SupplierPaymentReceipt(
        id = id,
        payableId = payableId,
        amount = amount,
        method = method,
        reference = reference,
        notes = notes,
        paidAt = paidAt,
        payableStatus = payableStatus,
        payableRemainingAmount = payableRemainingAmount
    )
}
