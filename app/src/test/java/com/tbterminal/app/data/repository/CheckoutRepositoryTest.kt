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
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.NetworkConnectivity
import java.io.IOException
import java.math.BigDecimal
import java.net.ConnectException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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

    @Test
    fun previewAndSubmitSerializeExactDecimalAndPlainWholeQuantitiesWithoutScientificNotation() = runTest {
        val api = FakeCheckoutApi(validPreview())
        val repository = RemoteCheckoutRepository(api)

        val items = listOf(
            CheckoutSubmitItem(productId = "product-frac", quantity = BigDecimal("7.50")),
            CheckoutSubmitItem(productId = "product-whole", quantity = BigDecimal("10.00")),
        )

        val previewResult = repository.previewCheckout(CheckoutPreviewCommand(items = items))
        assertTrue(previewResult is RepositoryResult.Success)
        assertEquals("7.5", api.previewRequest?.items?.get(0)?.qty)
        assertEquals("10", api.previewRequest?.items?.get(1)?.qty)

        val submitResult = repository.submitCheckout(
            CheckoutSubmitCommand(
                idempotencyKey = "idem-decimal-1",
                items = items,
                paymentMethod = "tunai",
                amountPaid = "100000",
            ),
        )
        assertTrue(submitResult is RepositoryResult.Success)
        assertEquals("7.5", api.checkoutRequest?.items?.get(0)?.qty)
        assertEquals("10", api.checkoutRequest?.items?.get(1)?.qty)
    }

    @Test
    fun previewUsesPreviewApiSubmitUsesCheckoutApiAndHealthMonitorOnlyMarksUnreachableOnTransportOrGateway502_503_504() = runTest {
        val healthApi = object : HealthApi {
            override suspend fun getReadiness(): Response<HealthResponseDto> =
                Response.success(HealthResponseDto(status = "ready", database = "up"))
        }
        val monitor = BackendHealthMonitor(
            healthApi = healthApi,
            networkMonitor = object : NetworkConnectivity {
                private val flow = MutableStateFlow(true)
                override fun isOnline(): Boolean = true
                override fun observeOnline(): Flow<Boolean> = flow
            },
            throttleMillis = 30_000L,
            timeoutMillis = 1_000L,
        )
        monitor.markConnected()

        var previewCalls = 0
        var submitCalls = 0
        var previewResponder: () -> Response<ApiResponse<CheckoutPreviewResponse>> = {
            Response.error(500, """{"code":"HTTP_500","message":"Internal server error"}""".toResponseBody("application/json".toMediaType()))
        }
        var submitResponder: () -> Response<ApiResponse<CheckoutResponse>> = {
            Response.success(ApiResponse(success = true, data = CheckoutResponse(id = "tx-1", receiptId = "R-1", amountTendered = "10000")))
        }
        val previewApi = object : CheckoutApi {
            override suspend fun previewCheckout(request: CheckoutPreviewRequest): Response<ApiResponse<CheckoutPreviewResponse>> {
                previewCalls += 1
                return previewResponder()
            }
            override suspend fun submitCheckout(request: CheckoutRequest): Response<ApiResponse<CheckoutResponse>> =
                error("previewApi must not handle submitCheckout")
        }
        val submitApi = object : CheckoutApi {
            override suspend fun previewCheckout(request: CheckoutPreviewRequest): Response<ApiResponse<CheckoutPreviewResponse>> =
                error("submitApi must not handle previewCheckout")
            override suspend fun submitCheckout(request: CheckoutRequest): Response<ApiResponse<CheckoutResponse>> {
                submitCalls += 1
                return submitResponder()
            }
        }
        val repository = RemoteCheckoutRepository(
            checkoutApi = submitApi,
            previewApi = previewApi,
            backendHealthMonitor = monitor,
        )

        // HTTP 500 keeps CONNECTED
        repository.previewCheckout(CheckoutPreviewCommand(listOf(CheckoutSubmitItem("p-1", 1))))
        assertEquals(1, previewCalls)
        assertEquals(BackendStatus.CONNECTED, monitor.status.value)

        // HTTP 400 keeps CONNECTED
        previewResponder = {
            Response.error(400, """{"code":"BAD_REQUEST","message":"bad"}""".toResponseBody("application/json".toMediaType()))
        }
        repository.previewCheckout(CheckoutPreviewCommand(listOf(CheckoutSubmitItem("p-1", 1))))
        assertEquals(BackendStatus.CONNECTED, monitor.status.value)

        // ConnectException marks UNREACHABLE
        previewResponder = { throw ConnectException("refused") }
        repository.previewCheckout(CheckoutPreviewCommand(listOf(CheckoutSubmitItem("p-1", 1))))
        assertEquals(BackendStatus.UNREACHABLE, monitor.status.value)

        // Successful submit marks CONNECTED again
        repository.submitCheckout(
            CheckoutSubmitCommand(
                idempotencyKey = "k-1",
                items = listOf(CheckoutSubmitItem("p-1", 1)),
                paymentMethod = "tunai",
                amountPaid = "10000",
            )
        )
        assertEquals(1, submitCalls)
        assertEquals(BackendStatus.CONNECTED, monitor.status.value)

        // HTTP 503 on submit marks UNREACHABLE; IOException also marks UNREACHABLE
        submitResponder = {
            Response.error(503, """{"message":"down"}""".toResponseBody("application/json".toMediaType()))
        }
        repository.submitCheckout(
            CheckoutSubmitCommand(
                idempotencyKey = "k-2",
                items = listOf(CheckoutSubmitItem("p-1", 1)),
                paymentMethod = "tunai",
                amountPaid = "10000",
            )
        )
        assertEquals(BackendStatus.UNREACHABLE, monitor.status.value)

        monitor.markConnected()
        previewResponder = { throw IOException("reset") }
        repository.previewCheckout(CheckoutPreviewCommand(listOf(CheckoutSubmitItem("p-1", 1))))
        assertEquals(BackendStatus.UNREACHABLE, monitor.status.value)
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
