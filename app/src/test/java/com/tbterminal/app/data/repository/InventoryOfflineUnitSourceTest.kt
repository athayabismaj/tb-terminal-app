package com.tbterminal.app.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.CategoryLocalDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.database.UnitLocalDataSource
import com.tbterminal.app.data.remote.InventoryApi
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class InventoryOfflineUnitSourceTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteInventoryRepository
    private lateinit var productLocalDataSource: ProductLocalDataSource

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java
        ).allowMainThreadQueries().build()
        productLocalDataSource = ProductLocalDataSource(database.productDao())

        server = MockWebServer()
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(InventoryApi::class.java)

        repository = RemoteInventoryRepository(
            inventoryApi = api,
            productLocalDataSource = productLocalDataSource,
            categoryLocalDataSource = CategoryLocalDataSource(database.categoryDao()),
            unitLocalDataSource = UnitLocalDataSource(database.unitDao())
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun getProductStocks_hydratesAndCachesSecondaryUnitFromProductsAndUnitsWhenStockOmitsIt() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/inventory/stock") -> MockResponse().setResponseCode(200).setBody(
                        """
                        {
                          "success": true,
                          "data": {
                            "data": [
                              {
                                "productId": "prod-1",
                                "sku": "PK-05",
                                "productName": "Paku 5 cm",
                                "categoryName": "Paku",
                                "unitName": "Pcs",
                                "quantity": "100",
                                "minStock": "10",
                                "priceBuy": "8000.00",
                                "priceRetail": "10000.00",
                                "priceContractor": "9500.00",
                                "discount": "0.00",
                                "isActive": true
                              }
                            ],
                            "total": 1,
                            "page": 1,
                            "limit": 24,
                            "totalPages": 1
                          }
                        }
                        """.trimIndent()
                    )
                    path.startsWith("/api/inventory/products") -> MockResponse().setResponseCode(200).setBody(
                        """
                        {
                          "success": true,
                          "data": {
                            "data": [
                              {
                                "id": "prod-1",
                                "categoryId": "cat-1",
                                "baseUnitId": "unit-pcs",
                                "sku": "PK-05",
                                "name": "Paku 5 cm",
                                "priceBuy": "8000.00",
                                "priceRetail": "10000.00",
                                "priceContractor": "9500.00",
                                "discount": "0.00",
                                "minStock": "10",
                                "isActive": true,
                                "secondaryUnitId": "unit-dus",
                                "secondaryUnitFactor": "12"
                              }
                            ],
                            "total": 1,
                            "page": 1,
                            "limit": 24,
                            "totalPages": 1
                          }
                        }
                        """.trimIndent()
                    )
                    path.startsWith("/api/inventory/units") -> MockResponse().setResponseCode(200).setBody(
                        """
                        {
                          "success": true,
                          "data": [
                            { "id": "unit-pcs", "name": "Pcs", "symbol": "pcs", "createdAt": "2026-01-01T00:00:00Z" },
                            { "id": "unit-dus", "name": "Dus", "symbol": "dus", "createdAt": "2026-01-01T00:00:00Z" }
                          ]
                        }
                        """.trimIndent()
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val onlineResult = repository.getProductStocks(page = 1, limit = 24)
        assertTrue(onlineResult is RepositoryResult.Success)
        val onlineItem = (onlineResult as RepositoryResult.Success).data.data.single()
        assertEquals("unit-pcs", onlineItem.baseUnitId)
        assertEquals("Pcs", onlineItem.unitName)
        assertEquals("unit-dus", onlineItem.secondaryUnitId)
        assertEquals("Dus", onlineItem.secondaryUnitName)
        assertEquals(0, BigDecimal("12").compareTo(onlineItem.secondaryUnitFactor))
        assertEquals(0, BigDecimal("100").compareTo(onlineItem.quantity))

        val cachedEntity = database.productDao().getByServerId("prod-1")
        assertNotNull(cachedEntity)
        cachedEntity!!
        assertEquals(100.0, cachedEntity.stock, 0.001)
        assertEquals("unit-pcs", cachedEntity.unitServerId)
        assertEquals("Pcs", cachedEntity.unitName)
        assertEquals("unit-dus", cachedEntity.secondaryUnitServerId)
        assertEquals("Dus", cachedEntity.secondaryUnitName)
        assertEquals(0, BigDecimal("12").compareTo(cachedEntity.secondaryUnitFactor))

        // Simulate offline mode: server fails with 503 so repository falls back to Room cache.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(503)
        }

        val offlineStocksResult = repository.getProductStocks(page = 1, limit = 24)
        assertTrue(offlineStocksResult is RepositoryResult.Success)
        val offlineItem = (offlineStocksResult as RepositoryResult.Success).data.data.single()
        assertEquals("unit-pcs", offlineItem.baseUnitId)
        assertEquals("Pcs", offlineItem.unitName)
        assertEquals("unit-dus", offlineItem.secondaryUnitId)
        assertEquals("Dus", offlineItem.secondaryUnitName)
        assertEquals(0, BigDecimal("12").compareTo(offlineItem.secondaryUnitFactor))
        assertEquals(0, BigDecimal("100").compareTo(offlineItem.quantity))

        val offlineProductResult = repository.getProduct("prod-1")
        assertTrue(offlineProductResult is RepositoryResult.Success)
        val offlineProduct = (offlineProductResult as RepositoryResult.Success).data
        assertEquals("unit-pcs", offlineProduct.baseUnitId)
        assertEquals("unit-dus", offlineProduct.secondaryUnitId)
        assertEquals(0, BigDecimal("12").compareTo(offlineProduct.secondaryUnitFactor))
    }

    @Test
    fun getProductStocks_succeedsWithoutInventingFactorWhenProductsEndpointReturns403() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/inventory/stock") -> MockResponse().setResponseCode(200).setBody(
                        """
                        {
                          "success": true,
                          "data": {
                            "data": [
                              {
                                "productId": "prod-2",
                                "sku": "SM-50",
                                "productName": "Semen 50kg",
                                "categoryName": "Semen",
                                "unitName": "Sak",
                                "quantity": "40",
                                "minStock": "5",
                                "priceBuy": "60000.00",
                                "priceRetail": "70000.00",
                                "priceContractor": "68000.00",
                                "discount": "0.00",
                                "isActive": true
                              }
                            ],
                            "total": 1,
                            "page": 1,
                            "limit": 24,
                            "totalPages": 1
                          }
                        }
                        """.trimIndent()
                    )
                    path.startsWith("/api/inventory/products") -> MockResponse().setResponseCode(403).setBody(
                        """{"success":false,"code":"FORBIDDEN","message":"Akses ditolak"}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result = repository.getProductStocks(page = 1, limit = 24)
        assertTrue(result is RepositoryResult.Success)
        val item = (result as RepositoryResult.Success).data.data.single()
        assertEquals("prod-2", item.productId)
        assertEquals("Sak", item.unitName)
        assertNull(item.baseUnitId)
        assertNull(item.secondaryUnitId)
        assertNull(item.secondaryUnitName)
        assertNull(item.secondaryUnitFactor)
    }
}
