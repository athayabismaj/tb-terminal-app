package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.remote.CheckoutApi
import com.tbterminal.app.data.remote.CheckoutLineItemRequest
import com.tbterminal.app.data.remote.CheckoutRequest
import com.tbterminal.app.data.remote.safeApiCall

interface CheckoutRepository {
    suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt>
}

class RemoteCheckoutRepository(
    private val checkoutApi: CheckoutApi
) : CheckoutRepository {
    override suspend fun submitCheckout(
        command: CheckoutSubmitCommand
    ): RepositoryResult<CheckoutReceipt> {
        val request = CheckoutRequest(
            idempotencyKey = command.idempotencyKey,
            customerId = command.customerId,
            items = command.items.map { item ->
                CheckoutLineItemRequest(
                    productId = item.productId,
                    qty = item.quantity.toString()
                )
            },
            paymentMethod = command.paymentMethod,
            amountPaid = command.amountPaid,
            notes = command.notes,
            dueDays = command.dueDays
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
