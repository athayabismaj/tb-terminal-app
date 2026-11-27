package com.tbterminal.app.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCustomerEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.CategoryRequestDto
import com.tbterminal.app.data.remote.CategoryResponseDto
import com.tbterminal.app.data.remote.CreateStandaloneReceivableRequestDto
import com.tbterminal.app.data.remote.CustomerReceivableSummaryResponseDto
import com.tbterminal.app.data.remote.CustomerRequestDto
import com.tbterminal.app.data.remote.CustomerResponseDto
import com.tbterminal.app.data.remote.InventoryApi
import com.tbterminal.app.data.remote.OpeningStockRequestDto
import com.tbterminal.app.data.remote.OpeningStockResponseDto
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ProductCreateRequestDto
import com.tbterminal.app.data.remote.ProductCsvImportRequestDto
import com.tbterminal.app.data.remote.ProductCsvImportResponseDto
import com.tbterminal.app.data.remote.ProductCsvPreviewResponseDto
import com.tbterminal.app.data.remote.ProductPhotoResponseDto
import com.tbterminal.app.data.remote.ProductPriceTierDto
import com.tbterminal.app.data.remote.ProductResponseDto
import com.tbterminal.app.data.remote.ProductUpdateRequestDto
import com.tbterminal.app.data.remote.ReceivableApi
import com.tbterminal.app.data.remote.ReceivablePaymentHistoryResponseDto
import com.tbterminal.app.data.remote.ReceivablePaymentRequestDto
import com.tbterminal.app.data.remote.ReceivablePaymentResponseDto
import com.tbterminal.app.data.remote.ReceivableResponseDto
import com.tbterminal.app.data.remote.ReverseReceivablePaymentRequestDto
import com.tbterminal.app.data.remote.StockAdjustmentResponseDto
import com.tbterminal.app.data.remote.StockCardResponseDto
import com.tbterminal.app.data.remote.StockDetailResponseDto
import com.tbterminal.app.data.remote.StockOpnameRequestDto
import com.tbterminal.app.data.remote.UnitRequestDto
import com.tbterminal.app.data.remote.UnitResponseDto
import com.tbterminal.app.data.remote.UpdateProductPriceTiersRequestDto
import java.io.IOException
import java.math.BigDecimal
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RepositoryCacheFallbackTest {
    private lateinit var database: TbTerminalDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun inventoryStocks_fallbackOnTransportAndGateway502_503_504_butNotOn500OrAuthErrors() = runBlocking {
        database.productDao().upsert(
            LocalProductEntity(
                serverId = "prod-1",
                clientGeneratedId = "PROD-1",
                name = "Semen Tiga Roda",
                sku = "SMN-01",
                unitServerId = "unit-1",
                unitName = "Sak",
                priceSell = ScaledDecimal2.ofExact(BigDecimal("70000")),
                priceBuy = ScaledDecimal2.ofExact(BigDecimal("60000")),
                priceRetail = ScaledDecimal2.ofExact(BigDecimal("70000")),
                priceContractor = ScaledDecimal2.ofExact(BigDecimal("68000")),
                stock = ScaledDecimal2.ofExact(BigDecimal("25")),
                minimumStock = ScaledDecimal2.ofExact(BigDecimal("5")),
                syncStatus = SyncStatus.SYNCED,
            )
        )
        val localSource = ProductLocalDataSource(database.productDao())
        val api = StubFallbackInventoryApi()
        val repository = RemoteInventoryRepository(
            inventoryApi = api,
            productLocalDataSource = localSource,
        )

        val transportErrors: List<() -> Response<ApiResponse<PaginatedResponse<StockDetailResponseDto>>>> = listOf(
            { throw ConnectException("Connection refused") },
            { throw IOException("Stream reset") },
            { throw SocketTimeoutException("Read timed out") },
            { Response.error(502, """{"message":"bad gateway"}""".toResponseBody("application/json".toMediaType())) },
            { Response.error(503, """{"message":"service unavailable"}""".toResponseBody("application/json".toMediaType())) },
            { Response.error(504, """{"message":"gateway timeout"}""".toResponseBody("application/json".toMediaType())) },
        )
        transportErrors.forEach { responder ->
            api.stockResponder = responder
            val res = repository.getProductStocks(page = 1, limit = 20)
            assertTrue("Expected cache fallback Success", res is RepositoryResult.Success)
            val data = (res as RepositoryResult.Success).data
            assertEquals(1, data.data.size)
            assertEquals("prod-1", data.data.first().productId)
        }

        val nonFallbackHttp = listOf(500, 401, 403, 400)
        nonFallbackHttp.forEach { httpCode ->
            api.stockResponder = {
                Response.error(httpCode, """{"message":"server error $httpCode"}""".toResponseBody("application/json".toMediaType()))
            }
            val res = repository.getProductStocks(page = 1, limit = 20)
            assertTrue("Expected Error (no cache fallback) for HTTP $httpCode", res is RepositoryResult.Error)
        }
    }

    @Test
    fun customers_fallbackOnTransportAndGateway502_503_504_butNotOn500OrAuthErrors() = runBlocking {
        database.customerDao().upsert(
            LocalCustomerEntity(
                serverId = "cust-1",
                clientGeneratedId = "CUST-1",
                name = "Toko Bangunan Maju",
                phone = "08123456789",
                syncStatus = SyncStatus.SYNCED,
            )
        )
        val localSource = CustomerLocalDataSource(database.customerDao())
        val api = StubFallbackReceivableApi()
        val repository = RemoteCustomerRepository(
            receivableApi = api,
            customerLocalDataSource = localSource,
        )

        val unavailableResponders: List<() -> Response<ApiResponse<PaginatedResponse<CustomerResponseDto>>>> = listOf(
            { throw ConnectException("Connection refused") },
            { throw IOException("Connection reset") },
            { Response.error(502, """{"message":"bad gateway"}""".toResponseBody("application/json".toMediaType())) },
            { Response.error(503, """{"message":"unavailable"}""".toResponseBody("application/json".toMediaType())) },
            { Response.error(504, """{"message":"timeout"}""".toResponseBody("application/json".toMediaType())) },
        )
        unavailableResponders.forEach { responder ->
            api.customersResponder = responder
            val res = repository.getCustomers(page = 1, limit = 10)
            assertTrue("Expected customer cache fallback Success", res is RepositoryResult.Success)
            val data = (res as RepositoryResult.Success).data
            assertEquals("cust-1", data.data.first().id)
        }

        val nonFallbackHttp = listOf(500, 401, 403)
        nonFallbackHttp.forEach { httpCode ->
            api.customersResponder = {
                Response.error(httpCode, """{"message":"err $httpCode"}""".toResponseBody("application/json".toMediaType()))
            }
            val res = repository.getCustomers(page = 1, limit = 10)
            assertTrue("Expected Error (no cache fallback) for HTTP $httpCode", res is RepositoryResult.Error)
        }
    }
}

private class StubFallbackInventoryApi : InventoryApi {
    var stockResponder: () -> Response<ApiResponse<PaginatedResponse<StockDetailResponseDto>>> = { unsupported() }

    override suspend fun getStockDetails(page: Int, limit: Int, search: String?) = stockResponder()
    override suspend fun getCategories(): Response<ApiResponse<List<CategoryResponseDto>>> = unsupported()
    override suspend fun getCategoriesPage(page: Int, limit: Int, search: String?): Response<ApiResponse<PaginatedResponse<CategoryResponseDto>>> = unsupported()
    override suspend fun createCategory(request: CategoryRequestDto): Response<ApiResponse<CategoryResponseDto>> = unsupported()
    override suspend fun updateCategory(id: String, request: CategoryRequestDto): Response<ApiResponse<CategoryResponseDto>> = unsupported()
    override suspend fun deleteCategory(id: String): Response<ApiResponse<Unit>> = unsupported()
    override suspend fun getUnits(): Response<ApiResponse<List<UnitResponseDto>>> = unsupported()
    override suspend fun getUnitsPage(page: Int, limit: Int, search: String?): Response<ApiResponse<PaginatedResponse<UnitResponseDto>>> = unsupported()
    override suspend fun createUnit(request: UnitRequestDto): Response<ApiResponse<UnitResponseDto>> = unsupported()
    override suspend fun updateUnit(id: String, request: UnitRequestDto): Response<ApiResponse<UnitResponseDto>> = unsupported()
    override suspend fun deleteUnit(id: String): Response<ApiResponse<Unit>> = unsupported()
    override suspend fun getProducts(page: Int, limit: Int, search: String?): Response<ApiResponse<PaginatedResponse<ProductResponseDto>>> = unsupported()
    override suspend fun getProduct(id: String): Response<ApiResponse<ProductResponseDto>> = unsupported()
    override suspend fun createProduct(request: ProductCreateRequestDto): Response<ApiResponse<ProductResponseDto>> = unsupported()
    override suspend fun updateProduct(id: String, request: ProductUpdateRequestDto): Response<ApiResponse<ProductResponseDto>> = unsupported()
    override suspend fun deleteProduct(id: String): Response<ApiResponse<Unit>> = unsupported()
    override suspend fun activateProduct(id: String): Response<ApiResponse<ProductResponseDto>> = unsupported()
    override suspend fun getProductPriceTiers(id: String): Response<ApiResponse<List<ProductPriceTierDto>>> = unsupported()
    override suspend fun updateProductPriceTiers(id: String, request: UpdateProductPriceTiersRequestDto): Response<ApiResponse<List<ProductPriceTierDto>>> = unsupported()
    override suspend fun uploadProductPhoto(id: String, photo: MultipartBody.Part): Response<ApiResponse<ProductPhotoResponseDto>> = unsupported()
    override suspend fun deleteProductPhoto(id: String): Response<Unit> = unsupported()
    override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): Response<ApiResponse<PaginatedResponse<StockAdjustmentResponseDto>>> = unsupported()
    override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): Response<ApiResponse<StockCardResponseDto>> = unsupported()
    override suspend fun executeStockOpname(request: StockOpnameRequestDto): Response<ApiResponse<Unit>> = unsupported()
    override suspend fun createOpeningStock(request: OpeningStockRequestDto): Response<ApiResponse<OpeningStockResponseDto>> = unsupported()
    override suspend fun previewProductCsv(request: ProductCsvImportRequestDto): Response<ApiResponse<ProductCsvPreviewResponseDto>> = unsupported()
    override suspend fun commitProductCsv(request: ProductCsvImportRequestDto): Response<ApiResponse<ProductCsvImportResponseDto>> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("unused")
}

private class StubFallbackReceivableApi : ReceivableApi {
    var customersResponder: () -> Response<ApiResponse<PaginatedResponse<CustomerResponseDto>>> = { unsupported() }

    override suspend fun getCustomers(page: Int, limit: Int, search: String?, status: String?) = customersResponder()
    override suspend fun getCustomerById(id: String, includeInactive: Boolean?): Response<ApiResponse<CustomerResponseDto>> = unsupported()
    override suspend fun createCustomer(request: CustomerRequestDto): Response<ApiResponse<CustomerResponseDto>> = unsupported()
    override suspend fun updateCustomer(id: String, request: CustomerRequestDto): Response<ApiResponse<CustomerResponseDto>> = unsupported()
    override suspend fun deactivateCustomer(id: String): Response<ApiResponse<Unit>> = unsupported()
    override suspend fun activateCustomer(id: String): Response<ApiResponse<CustomerResponseDto>> = unsupported()
    override suspend fun getReceivables(page: Int, limit: Int, customerId: String?, status: String?, dueFilter: String?, dueFrom: String?, dueTo: String?): Response<ApiResponse<PaginatedResponse<ReceivableResponseDto>>> = unsupported()
    override suspend fun createOpeningBalance(request: CreateStandaloneReceivableRequestDto): Response<ApiResponse<ReceivableResponseDto>> = unsupported()
    override suspend fun createAdjustment(request: CreateStandaloneReceivableRequestDto): Response<ApiResponse<ReceivableResponseDto>> = unsupported()
    override suspend fun getCustomerReceivableSummaries(page: Int, limit: Int, dueFilter: String?): Response<ApiResponse<PaginatedResponse<CustomerReceivableSummaryResponseDto>>> = unsupported()
    override suspend fun getReceivableById(id: String): Response<ApiResponse<ReceivableResponseDto>> = unsupported()
    override suspend fun createReceivablePayment(request: ReceivablePaymentRequestDto): Response<ApiResponse<ReceivablePaymentResponseDto>> = unsupported()
    override suspend fun getReceivablePayments(page: Int, limit: Int, receivableId: String?, customerId: String?, method: String?, userId: String?, customerSearch: String?, receiverSearch: String?, status: String?, dateFrom: String?, dateTo: String?): Response<ApiResponse<PaginatedResponse<ReceivablePaymentHistoryResponseDto>>> = unsupported()
    override suspend fun getReceivablePaymentReceipt(id: String): Response<ApiResponse<ReceivablePaymentHistoryResponseDto>> = unsupported()
    override suspend fun reverseReceivablePayment(id: String, request: ReverseReceivablePaymentRequestDto): Response<ApiResponse<ReceivablePaymentResponseDto>> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("unused")
}
