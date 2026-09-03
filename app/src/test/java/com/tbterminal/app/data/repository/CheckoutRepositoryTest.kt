package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutSubmitItem
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.DiscountType
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.CheckoutApi
import com.tbterminal.app.data.remote.CheckoutPreviewItemResponse
import com.tbterminal.app.data.remote.CheckoutPreviewRequest
import com.tbterminal.app.data.remote.CheckoutPreviewResponse
import com.tbterminal.app.data.remote.CheckoutRequest
import com.tbterminal.app.data.remote.CheckoutResponse
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CheckoutRepositoryTest {
    @Test
    fun previewMapsAuthoritativeTotalsAndSendsOnlyDiscountIntent() = runTest {
        val api = FakeCheckoutApi(validPreview())
        val repository = RemoteCheckoutRepository(api)

        val result = repository.previewCheckout(
            CheckoutPreviewCommand(
                items = listOf(
                    CheckoutSubmitItem(
                        productId = "product-1",
                        quantity = 2,
                        discountRequest = CheckoutDiscount(DiscountType.PERCENTAGE, BigDecimal("10")),
                    ),
                ),
                transactionDiscount = CheckoutDiscount(DiscountType.FIXED_AMOUNT, BigDecimal("5000")),
            ),
        )

        assertTrue(result is RepositoryResult.Success)
        val preview = (result as RepositoryResult.Success).data
        assertEquals(0, BigDecimal("85000").compareTo(preview.netTotal))
        assertTrue(preview.approvalRequired)
        assertEquals("PERCENTAGE", api.previewRequest?.items?.single()?.discountRequest?.type)
        assertEquals("5000", api.previewRequest?.transactionDiscount?.value)
    }

    @Test
    fun previewRejectsInvalidMoneyInSuccessfulHttpResponse() = runTest {
        val repository = RemoteCheckoutRepository(
            FakeCheckoutApi(validPreview().copy(netTotal = "not-a-number")),
        )

        val result = repository.previewCheckout(
            CheckoutPreviewCommand(listOf(CheckoutSubmitItem("product-1", 1))),
        )

        assertTrue(result is RepositoryResult.Error)
        assertEquals("INVALID_RESPONSE", (result as RepositoryResult.Error).code)
    }

    @Test
    fun normalCheckoutDoesNotSendApprovalFields() = runTest {
        val api = FakeCheckoutApi(validPreview())
        val repository = RemoteCheckoutRepository(api)

        val result = repository.submitCheckout(
            CheckoutSubmitCommand(
                idempotencyKey = "idem-1",
                items = listOf(
                    CheckoutSubmitItem(
                        productId = "product-1",
                        quantity = 1,
                        discountRequest = CheckoutDiscount(DiscountType.PERCENTAGE, BigDecimal("5")),
                    ),
                ),
                paymentMethod = "tunai",
                amountPaid = "95000",
            ),
        )

        assertTrue(result is RepositoryResult.Success)
        assertEquals(null, api.checkoutRequest?.checkoutAttemptId)
        assertEquals(null, api.checkoutRequest?.managerApprovalId)
        assertEquals("PERCENTAGE", api.checkoutRequest?.items?.single()?.discountRequest?.type)
    }

    private fun validPreview() = CheckoutPreviewResponse(
        checkoutAttemptId = "00000000-0000-0000-0000-000000000001",
        discountFingerprint = "hash",
        items = listOf(
            CheckoutPreviewItemResponse(
                productId = "product-1",
                quantity = "2",
                unitPrice = "50000",
                grossLineTotal = "100000",
                discountType = "PERCENTAGE",
                discountValue = "10",
                discountAmount = "10000",
                netLineTotal = "90000",
            ),
        ),
        grossSubtotal = "100000",
        itemDiscountTotal = "10000",
        transactionDiscountAmount = "5000",
        totalDiscountAmount = "15000",
        effectiveDiscountPercent = "15",
        netTotal = "85000",
        cashierDiscountLimitPercent = "10",
        approvalRequired = true,
        expiresAt = "2099-01-01T00:00:00Z",
    )
}

private class FakeCheckoutApi(
    private val preview: CheckoutPreviewResponse,
) : CheckoutApi {
    var previewRequest: CheckoutPreviewRequest? = null
    var checkoutRequest: CheckoutRequest? = null

    override suspend fun previewCheckout(
        request: CheckoutPreviewRequest,
    ): Response<ApiResponse<CheckoutPreviewResponse>> {
        previewRequest = request
        return Response.success(ApiResponse(success = true, data = preview))
    }

    override suspend fun submitCheckout(
        request: CheckoutRequest,
    ): Response<ApiResponse<CheckoutResponse>> {
        checkoutRequest = request
        return Response.success(
            ApiResponse(
                success = true,
                data = CheckoutResponse(
                    id = "transaction-1",
                    receiptId = "receipt-1",
                    amountTendered = request.amountPaid,
                ),
            ),
        )
    }
}
