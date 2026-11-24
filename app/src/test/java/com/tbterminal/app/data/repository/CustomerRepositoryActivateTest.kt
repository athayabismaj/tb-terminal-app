package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.CreateStandaloneReceivableRequestDto
import com.tbterminal.app.data.remote.CustomerReceivableSummaryResponseDto
import com.tbterminal.app.data.remote.CustomerRequestDto
import com.tbterminal.app.data.remote.CustomerResponseDto
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ReceivableApi
import com.tbterminal.app.data.remote.ReceivablePaymentHistoryResponseDto
import com.tbterminal.app.data.remote.ReceivablePaymentRequestDto
import com.tbterminal.app.data.remote.ReceivablePaymentResponseDto
import com.tbterminal.app.data.remote.ReceivableResponseDto
import com.tbterminal.app.data.remote.ReverseReceivablePaymentRequestDto
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CustomerRepositoryActivateTest {

    @Test
    fun activateCustomerSuccessCallsPatchActivateAndReturnsActiveCustomer() = runBlocking {
        val api = FakeCustomerReceivableApi().apply {
            activateResponse = { id ->
                Response.success(
                    ApiResponse(
                        success = true,
                        data = sampleCustomerDto(id = id, isActive = true)
                    )
                )
            }
        }
        val repository = RemoteCustomerRepository(api)

        val result = repository.activateCustomer("cust-01")

        assertTrue(result is RepositoryResult.Success)
        val customer = (result as RepositoryResult.Success).data
        assertEquals("cust-01", customer.id)
        assertTrue(customer.isActive)
        assertEquals(listOf("cust-01"), api.activateCalls)
    }

    @Test
    fun activateCustomerConflict409RefreshesCustomerAndReturnsSuccess() = runBlocking {
        val api = FakeCustomerReceivableApi().apply {
            activateResponse = {
                Response.error(
                    409,
                    """{"success":false,"code":"ALREADY_ACTIVE","message":"Pelanggan sudah aktif."}"""
                        .toResponseBody("application/json".toMediaType())
                )
            }
            detailResponse = { id ->
                Response.success(
                    ApiResponse(
                        success = true,
                        data = sampleCustomerDto(id = id, isActive = true)
                    )
                )
            }
        }
        val repository = RemoteCustomerRepository(api)

        val result = repository.activateCustomer("cust-01")

        assertTrue(result is RepositoryResult.Success)
        assertTrue((result as RepositoryResult.Success).data.isActive)
        assertEquals(listOf("cust-01"), api.activateCalls)
        assertEquals(listOf("cust-01"), api.detailCalls)
    }

    @Test
    fun activateCustomer404ReturnsNotFoundErrorMessage() = runBlocking {
        val api = FakeCustomerReceivableApi().apply {
            activateResponse = {
                Response.error(
                    404,
                    """{"success":false,"code":"NOT_FOUND","message":"Customer not found"}"""
                        .toResponseBody("application/json".toMediaType())
                )
            }
        }
        val repository = RemoteCustomerRepository(api)

        val result = repository.activateCustomer("cust-deleted")

        assertTrue(result is RepositoryResult.Error)
        val err = result as RepositoryResult.Error
        assertEquals("HTTP_404", err.code)
        assertEquals("Pelanggan sudah dihapus atau tidak ditemukan.", err.message)
    }

    @Test
    fun activateCustomerNetworkFailureReturnsConnectionInterruptedError() = runBlocking {
        val api = FakeCustomerReceivableApi().apply {
            activateResponse = { throw IOException("offline") }
        }
        val repository = RemoteCustomerRepository(api)

        val result = repository.activateCustomer("cust-01")

        assertTrue(result is RepositoryResult.Error)
        assertEquals("CONNECTION_INTERRUPTED", (result as RepositoryResult.Error).code)
    }

    @Test
    fun deactivateCustomerStillUsesDeleteEndpoint() = runBlocking {
        val api = FakeCustomerReceivableApi()
        val repository = RemoteCustomerRepository(api)

        val result = repository.deactivateCustomer("cust-01")

        assertTrue(result is RepositoryResult.Success)
        assertEquals(listOf("cust-01"), api.deactivateCalls)
    }

    private fun sampleCustomerDto(id: String, isActive: Boolean) = CustomerResponseDto(
        id = id,
        name = "Budi Santoso",
        phone = "081234567890",
        address = "Jl. Sudirman No. 45",
        isContractor = true,
        creditLimit = BigDecimal("5000000"),
        paymentTermDays = 14,
        isActive = isActive,
        createdAt = "2026-01-10T08:00:00Z",
        updatedAt = "2026-02-15T10:30:00Z"
    )
}

private class FakeCustomerReceivableApi : ReceivableApi {
    val activateCalls = mutableListOf<String>()
    val deactivateCalls = mutableListOf<String>()
    val detailCalls = mutableListOf<String>()

    var activateResponse: suspend (String) -> Response<ApiResponse<CustomerResponseDto>> = {
        Response.error(500, "unconfigured".toResponseBody())
    }
    var detailResponse: suspend (String) -> Response<ApiResponse<CustomerResponseDto>> = {
        Response.error(500, "unconfigured".toResponseBody())
    }

    override suspend fun getCustomers(
        page: Int,
        limit: Int,
        search: String?,
        status: String?
    ): Response<ApiResponse<PaginatedResponse<CustomerResponseDto>>> = unsupported()

    override suspend fun getCustomerById(
        id: String,
        includeInactive: Boolean?
    ): Response<ApiResponse<CustomerResponseDto>> {
        detailCalls += id
        return detailResponse(id)
    }

    override suspend fun createCustomer(request: CustomerRequestDto): Response<ApiResponse<CustomerResponseDto>> =
        unsupported()

    override suspend fun updateCustomer(
        id: String,
        request: CustomerRequestDto
    ): Response<ApiResponse<CustomerResponseDto>> = unsupported()

    override suspend fun deactivateCustomer(id: String): Response<ApiResponse<Unit>> {
        deactivateCalls += id
        return Response.success(ApiResponse(success = true, data = Unit))
    }

    override suspend fun activateCustomer(id: String): Response<ApiResponse<CustomerResponseDto>> {
        activateCalls += id
        return activateResponse(id)
    }

    override suspend fun getReceivables(
        page: Int,
        limit: Int,
        customerId: String?,
        status: String?,
        dueFilter: String?,
        dueFrom: String?,
        dueTo: String?
    ): Response<ApiResponse<PaginatedResponse<ReceivableResponseDto>>> = unsupported()

    override suspend fun createOpeningBalance(
        request: CreateStandaloneReceivableRequestDto
    ): Response<ApiResponse<ReceivableResponseDto>> = unsupported()

    override suspend fun createAdjustment(
        request: CreateStandaloneReceivableRequestDto
    ): Response<ApiResponse<ReceivableResponseDto>> = unsupported()

    override suspend fun getCustomerReceivableSummaries(
        page: Int,
        limit: Int,
        dueFilter: String?
    ): Response<ApiResponse<PaginatedResponse<CustomerReceivableSummaryResponseDto>>> = unsupported()

    override suspend fun getReceivableById(id: String): Response<ApiResponse<ReceivableResponseDto>> =
        unsupported()

    override suspend fun createReceivablePayment(
        request: ReceivablePaymentRequestDto
    ): Response<ApiResponse<ReceivablePaymentResponseDto>> = unsupported()

    override suspend fun getReceivablePayments(
        page: Int,
        limit: Int,
        receivableId: String?,
        customerId: String?,
        method: String?,
        userId: String?,
        customerSearch: String?,
        receiverSearch: String?,
        status: String?,
        dateFrom: String?,
        dateTo: String?
    ): Response<ApiResponse<PaginatedResponse<ReceivablePaymentHistoryResponseDto>>> = unsupported()

    override suspend fun getReceivablePaymentReceipt(
        id: String
    ): Response<ApiResponse<ReceivablePaymentHistoryResponseDto>> = unsupported()

    override suspend fun reverseReceivablePayment(
        id: String,
        request: ReverseReceivablePaymentRequestDto
    ): Response<ApiResponse<ReceivablePaymentResponseDto>> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Not used in test")
}
