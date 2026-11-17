package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateQuotationCommand
import com.tbterminal.app.data.model.DiscountType
import com.tbterminal.app.data.model.QuotationDetail
import com.tbterminal.app.data.model.QuotationItemDetail
import com.tbterminal.app.data.model.QuotationPage
import com.tbterminal.app.data.model.QuotationStatus
import com.tbterminal.app.data.model.QuotationSummary
import com.tbterminal.app.data.remote.CreateQuotationItemRequestDto
import com.tbterminal.app.data.remote.CreateQuotationRequestDto
import com.tbterminal.app.data.remote.DiscountRequestDto
import com.tbterminal.app.data.remote.QuotationDetailDto
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.remote.UpdateQuotationStatusRequestDto
import com.tbterminal.app.data.remote.safeApiCall

interface QuotationRepository {
    suspend fun getQuotations(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null,
        startDate: String? = null,
        endDate: String? = null,
    ): RepositoryResult<QuotationPage>

    suspend fun createQuotation(command: CreateQuotationCommand): RepositoryResult<QuotationDetail>

    suspend fun getQuotationById(id: String): RepositoryResult<QuotationDetail>

    suspend fun updateQuotationStatus(
        id: String,
        status: QuotationStatus,
        convertedTransactionId: String? = null,
    ): RepositoryResult<QuotationDetail>
}

class RemoteQuotationRepository(
    private val salesApi: SalesApi,
) : QuotationRepository {

    override suspend fun getQuotations(
        page: Int,
        limit: Int,
        status: String?,
        search: String?,
        startDate: String?,
        endDate: String?,
    ): RepositoryResult<QuotationPage> {
        return safeApiCall {
            salesApi.getQuotations(
                page = page,
                limit = limit,
                status = status?.takeIf(String::isNotBlank),
                search = search?.takeIf(String::isNotBlank),
                startDate = startDate?.takeIf(String::isNotBlank),
                endDate = endDate?.takeIf(String::isNotBlank),
            )
        }.toRepositoryResult { response ->
            val paged = response.data
            if (!response.success || paged == null) {
                RepositoryResult.Error(
                    code = response.code ?: "QUOTATIONS_FAILED",
                    message = response.message ?: response.error ?: "Penawaran harga gagal dimuat."
                )
            } else {
                RepositoryResult.Success(
                    QuotationPage(
                        data = paged.data.map { dto ->
                            QuotationSummary(
                                id = dto.id,
                                quotationNumber = dto.quotationNumber,
                                userId = dto.userId,
                                cashierName = dto.cashierName,
                                customerId = dto.customerId,
                                customerName = dto.customerName,
                                status = QuotationStatus.fromApi(dto.status),
                                validUntil = dto.validUntil,
                                total = dto.total,
                                itemCount = dto.itemCount,
                                notes = dto.notes,
                                convertedTransactionId = dto.convertedTransactionId,
                                createdAt = dto.createdAt,
                            )
                        },
                        total = paged.total,
                        page = paged.page,
                        limit = paged.limit,
                        totalPages = paged.totalPages,
                    )
                )
            }
        }
    }

    override suspend fun createQuotation(command: CreateQuotationCommand): RepositoryResult<QuotationDetail> {
        val request = CreateQuotationRequestDto(
            customerId = command.customerId,
            customerName = command.customerName,
            items = command.items.map { item ->
                CreateQuotationItemRequestDto(
                    productId = item.productId,
                    qty = item.qty,
                    discountRequest = item.discountRequest?.let {
                        DiscountRequestDto(type = it.type.name, value = it.value.toPlainString())
                    },
                    note = item.note,
                )
            },
            transactionDiscount = command.transactionDiscount?.let {
                DiscountRequestDto(type = it.type.name, value = it.value.toPlainString())
            },
            validDays = command.validDays,
            notes = command.notes,
        )
        return safeApiCall { salesApi.createQuotation(request) }
            .toRepositoryResult { response ->
                val dto = response.data
                if (!response.success || dto == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_QUOTATION_FAILED",
                        message = response.message ?: response.error ?: "Penawaran harga gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(dto.toDomain())
                }
            }
    }

    override suspend fun getQuotationById(id: String): RepositoryResult<QuotationDetail> {
        return safeApiCall { salesApi.getQuotationById(id) }
            .toRepositoryResult { response ->
                val dto = response.data
                if (!response.success || dto == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "QUOTATION_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail penawaran harga tidak ditemukan."
                    )
                } else {
                    RepositoryResult.Success(dto.toDomain())
                }
            }
    }

    override suspend fun updateQuotationStatus(
        id: String,
        status: QuotationStatus,
        convertedTransactionId: String?,
    ): RepositoryResult<QuotationDetail> {
        val request = UpdateQuotationStatusRequestDto(
            status = status.name,
            convertedTransactionId = convertedTransactionId,
        )
        return safeApiCall { salesApi.updateQuotationStatus(id, request) }
            .toRepositoryResult { response ->
                val dto = response.data
                if (!response.success || dto == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "QUOTATION_STATUS_FAILED",
                        message = response.message ?: response.error ?: "Status penawaran gagal diperbarui."
                    )
                } else {
                    RepositoryResult.Success(dto.toDomain())
                }
            }
    }
}

private fun QuotationDetailDto.toDomain(): QuotationDetail = QuotationDetail(
    id = id,
    quotationNumber = quotationNumber,
    userId = userId,
    cashierName = cashierName,
    customerId = customerId,
    customerName = customerName,
    status = QuotationStatus.fromApi(status),
    validUntil = validUntil,
    notes = notes,
    grossSubtotal = grossSubtotal,
    itemDiscountTotal = itemDiscountTotal,
    transactionDiscountType = transactionDiscountType?.let { DiscountType.valueOf(it) },
    transactionDiscountValue = transactionDiscountValue,
    transactionDiscountAmount = transactionDiscountAmount,
    totalDiscountAmount = totalDiscountAmount,
    taxEnabled = taxEnabled,
    taxMode = taxMode,
    taxRate = taxRate,
    taxLabel = taxLabel,
    taxableBase = taxableBase,
    taxAmount = taxAmount,
    total = total,
    convertedTransactionId = convertedTransactionId,
    convertedAt = convertedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    items = items.map { item ->
        QuotationItemDetail(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            unitId = item.unitId,
            unitName = item.unitName,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            discountType = item.discountType?.let { DiscountType.valueOf(it) },
            discountValue = item.discountValue,
            discountAmount = item.discountAmount,
            subtotal = item.subtotal,
            note = item.note,
        )
    },
)
