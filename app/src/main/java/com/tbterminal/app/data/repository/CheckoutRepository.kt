package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutPreviewItem
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.DiscountType
import com.tbterminal.app.data.remote.CheckoutApi
import com.tbterminal.app.data.remote.CheckoutPaymentLineRequestDto
import com.tbterminal.app.data.remote.CheckoutPreviewRequest
import com.tbterminal.app.data.remote.CheckoutLineItemRequest
import com.tbterminal.app.data.remote.CheckoutRequest
import com.tbterminal.app.data.remote.DiscountRequestDto
import com.tbterminal.app.data.remote.safeApiCall

interface CheckoutRepository {
    suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview>
    suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt>
}

class RemoteCheckoutRepository(
    private val checkoutApi: CheckoutApi
) : CheckoutRepository {
    override suspend fun previewCheckout(
        command: CheckoutPreviewCommand,
    ): RepositoryResult<CheckoutPreview> {
        val request = CheckoutPreviewRequest(
            items = command.items.map { item ->
                CheckoutLineItemRequest(
                    productId = item.productId,
                    qty = item.quantity.toString(),
                    discountRequest = item.discountRequest.toDto(),
                    note = item.note,
                )
            },
            transactionDiscount = command.transactionDiscount.toDto(),
            customerId = command.customerId,
        )
        return safeApiCall { checkoutApi.previewCheckout(request) }
            .toRepositoryResult { response ->
                val preview = response.data
                if (!response.success || preview == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CHECKOUT_PREVIEW_FAILED",
                        message = response.message ?: response.error ?: "Preview checkout gagal.",
                    )
                } else {
                    runCatching {
                        CheckoutPreview(
                            checkoutAttemptId = preview.checkoutAttemptId,
                            discountFingerprint = preview.discountFingerprint,
                            items = preview.items.map { item ->
                                CheckoutPreviewItem(
                                    productId = item.productId,
                                    quantity = item.quantity.toBigDecimal(),
                                    unitPrice = item.unitPrice.toBigDecimal(),
                                    grossLineTotal = item.grossLineTotal.toBigDecimal(),
                                    discountType = item.discountType?.let(DiscountType::valueOf),
                                    discountValue = item.discountValue.toBigDecimal(),
                                    discountAmount = item.discountAmount.toBigDecimal(),
                                    netLineTotal = item.netLineTotal.toBigDecimal(),
                                )
                            },
                            grossSubtotal = preview.grossSubtotal.toBigDecimal(),
                            itemDiscountTotal = preview.itemDiscountTotal.toBigDecimal(),
                            transactionDiscountAmount = preview.transactionDiscountAmount.toBigDecimal(),
                            totalDiscountAmount = preview.totalDiscountAmount.toBigDecimal(),
                            effectiveDiscountPercent = preview.effectiveDiscountPercent.toBigDecimal(),
                            netTotal = preview.netTotal.toBigDecimal(),
                            cashierDiscountLimitPercent = preview.cashierDiscountLimitPercent.toBigDecimal(),
                            approvalRequired = preview.approvalRequired,
                            expiresAt = preview.expiresAt,
                            taxEnabled = preview.taxEnabled,
                            taxMode = preview.taxMode,
                            taxRate = preview.taxRate.toBigDecimal(),
                            taxLabel = preview.taxLabel,
                            taxableBase = preview.taxableBase.toBigDecimal(),
                            taxAmount = preview.taxAmount.toBigDecimal(),
                            totalWithTax = preview.totalWithTax.toBigDecimal(),
                            cashRoundingUnit = preview.cashRoundingUnit,
                            cashRoundingRule = preview.cashRoundingRule,
                            estimatedCashRoundingAmount = preview.estimatedCashRoundingAmount.toBigDecimal(),
                            estimatedTotalWithCashRounding = preview.estimatedTotalWithCashRounding.toBigDecimal(),
                        )
                    }.fold(
                        onSuccess = { RepositoryResult.Success(it) },
                        onFailure = {
                            RepositoryResult.Error(
                                code = "INVALID_RESPONSE",
                                message = "Nilai preview checkout dari server tidak valid.",
                            )
                        },
                    )
                }
            }
    }

    override suspend fun submitCheckout(
        command: CheckoutSubmitCommand
    ): RepositoryResult<CheckoutReceipt> {
        val request = CheckoutRequest(
            idempotencyKey = command.idempotencyKey,
            customerId = command.customerId,
            items = command.items.map { item ->
                CheckoutLineItemRequest(
                    productId = item.productId,
                    qty = item.quantity.toString(),
                    discountRequest = item.discountRequest.toDto(),
                    note = item.note,
                )
            },
            paymentMethod = command.paymentMethod,
            amountPaid = command.amountPaid,
            payments = command.payments?.map { line ->
                CheckoutPaymentLineRequestDto(
                    method = line.method,
                    amount = line.amount,
                    reference = line.reference,
                )
            },
            notes = command.notes,
            dueDays = command.dueDays,
            transactionDiscount = command.transactionDiscount.toDto(),
            checkoutAttemptId = command.checkoutAttemptId,
            managerApprovalId = command.managerApprovalId,
        )

        return safeApiCall { checkoutApi.submitCheckout(request) }
            .toRepositoryResult { response ->
                val checkout = response.data
                if (!response.success || checkout == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CHECKOUT_FAILED",
                        message = response.message ?: response.error ?: "Checkout gagal."
                    )
                } else {
                    RepositoryResult.Success(
                        CheckoutReceipt(
                            receiptId = checkout.receiptId?.takeIf(String::isNotBlank)
                                ?: "TRX-${checkout.id.take(8).uppercase()}",
                            transactionId = checkout.id,
                            amountTendered = checkout.amountTendered,
                            changeAmount = checkout.changeAmount,
                            idempotentReplay = checkout.idempotentReplay
                        )
                    )
                }
            }
    }
}

private fun CheckoutDiscount?.toDto(): DiscountRequestDto? = this?.let {
    DiscountRequestDto(type = it.type.name, value = it.value.toPlainString())
}
