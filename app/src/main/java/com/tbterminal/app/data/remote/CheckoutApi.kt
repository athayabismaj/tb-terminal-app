package com.tbterminal.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CheckoutApi {
    @POST("/api/sales/checkout/preview")
    suspend fun previewCheckout(
        @Body request: CheckoutPreviewRequest,
    ): Response<ApiResponse<CheckoutPreviewResponse>>

    @POST("/api/sales/checkout")
    suspend fun submitCheckout(
        @Body request: CheckoutRequest
    ): Response<ApiResponse<CheckoutResponse>>
}
