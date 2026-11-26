package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.SalesApi
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TransactionItemUnitMappingTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteCashReconciliationRepository

    @Before
    fun setUp() {
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
            .create(SalesApi::class.java)
        repository = RemoteCashReconciliationRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getTransactionById_mapsServerUnitNameAndSupportsLegacyNullFallback() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "id": "trx-1",
                    "receiptId": "TRX-001",
                    "sessionId": "sess-1",
                    "customerId": null,
                    "type": "sale",
                    "status": "settled",
                    "total": "240000.00",
                    "paidAmount": "240000.00",
                    "createdAt": "2026-11-25T10:00:00+07:00",
                    "items": [
                      {
                        "productId": "prod-1",
                        "productName": "Paku 5 cm",
                        "unitId": "unit-pcs",
                        "unitName": "Pcs",
                        "quantity": "24.00",
                        "priceAtTransaction": "10000.00",
                        "subtotal": "240000.00"
                      },
                      {
                        "productId": "prod-2",
                        "productName": "Semen 50kg",
                        "unitId": "unit-sak",
                        "quantity": "2.00",
                        "priceAtTransaction": "65000.00",
                        "subtotal": "130000.00"
                      }
                    ]
                  }
                }
                """.trimIndent()
            )
        )

        val result = repository.getTransactionById("trx-1")
        assertTrue(result is RepositoryResult.Success)
        val items = (result as RepositoryResult.Success).data.items
        assertEquals(2, items.size)
        assertEquals("Pcs", items[0].unitName)
        assertNull(items[1].unitName)
    }

    @Test
    fun payTransactionDebt_mapsServerUnitNameFromDetailResponse() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "id": "trx-2",
                    "receiptId": "TRX-002",
                    "sessionId": "sess-1",
                    "customerId": "cust-1",
                    "type": "sale",
                    "status": "settled",
                    "total": "50000.00",
                    "paidAmount": "50000.00",
                    "createdAt": "2026-11-25T10:00:00+07:00",
                    "items": [
                      {
                        "productId": "prod-3",
                        "productName": "Pipa PVC",
                        "unitId": "unit-meter",
                        "unitName": "Meter",
                        "quantity": "5.00",
                        "priceAtTransaction": "10000.00",
                        "subtotal": "50000.00"
                      }
                    ]
                  }
                }
                """.trimIndent()
            )
        )

        val result = repository.payTransactionDebt(
            transactionId = "trx-2",
            amount = BigDecimal("50000"),
            method = "tunai",
            idempotencyKey = "pay-key-12345678",
        )
        assertTrue(result is RepositoryResult.Success)
        val item = (result as RepositoryResult.Success).data.items.single()
        assertEquals("Meter", item.unitName)
    }
}
