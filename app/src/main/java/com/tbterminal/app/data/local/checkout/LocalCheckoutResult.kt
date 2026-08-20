package com.tbterminal.app.data.local.checkout

sealed interface LocalCheckoutResult {
    data class Success(
        val transactionLocalId: Long,
        val transactionClientGeneratedId: String,
        val transactionCode: String,
        val paymentLocalId: Long?,
        val receivableLocalId: Long?,
        val syncQueueId: Long
    ) : LocalCheckoutResult

    data class Failed(
        val reason: String
    ) : LocalCheckoutResult
}
