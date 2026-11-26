package com.tbterminal.app.data.local.checkout

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalCheckoutSyncPayloadTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var service: LocalCheckoutService

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java
        ).allowMainThreadQueries().build()
        service = LocalCheckoutService(database, deviceId = "android-test-1")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun toSyncPayloadJson_escapesQuotesBackslashesNewlinesTabsAndControlChars() {
        val specialCustomer = "PT \"Barokah\"\\Jaya\nCabang\t1\r\nOK\u0007"
        val specialNote = "Antar ke gudang \"B\"\\blok-2\nJam 10:00\tpagi"
        val specialProduct = "Cat \"Super\"\\5Kg\nPutih\tGloss"
        val specialSku = "SKU\\\"01\"\n\t"
        val specialUnit = "Kaleng\\\"2.5\""

        val command = LocalCheckoutCommand(
            cashierUserId = "kasir-1",
            cashSessionLocalId = 1L,
            cashSessionServerId = "srv-sess-1",
            customerName = specialCustomer,
            paymentMethod = "tunai",
            subtotal = BigDecimal("75000.00"),
            total = BigDecimal("75000.00"),
            paidAmount = BigDecimal("75000.00"),
            note = specialNote,
            items = listOf(
                LocalCheckoutItemCommand(
                    productLocalId = 10L,
                    productServerId = "prod-1",
                    productNameSnapshot = specialProduct,
                    skuSnapshot = specialSku,
                    unitNameSnapshot = specialUnit,
                    unitServerId = "unit-kaleng",
                    quantity = BigDecimal("3"),
                    baseQuantity = BigDecimal("7.5"),
                    conversionFactor = BigDecimal("2.5"),
                    priceAtTransaction = BigDecimal("25000.00"),
                    cogsAtTransaction = BigDecimal("20000.00"),
                    subtotal = BigDecimal("75000.00"),
                    baseQuantityExact = BigDecimal("7.5"),
                    conversionFactorExact = BigDecimal("2.5"),
                )
            )
        )

        val jsonText = with(service) {
            command.toSyncPayloadJson(
                transactionLocalId = 42L,
                transactionClientGeneratedId = "TRX-CLIENT-1",
                transactionCode = "TRX-20261125-ABCD",
                remainingAmount = BigDecimal.ZERO,
            )
        }

        val parsed = Json.parseToJsonElement(jsonText).jsonObject
        assertEquals(specialCustomer, parsed["customerName"]?.jsonPrimitive?.content)
        assertEquals(specialNote, parsed["note"]?.jsonPrimitive?.content)

        val decoded = LocalCheckoutService.SYNC_PAYLOAD_JSON.decodeFromString<LocalCheckoutSyncPayload>(jsonText)
        assertEquals(42L, decoded.transactionLocalId)
        assertEquals("TRX-CLIENT-1", decoded.clientGeneratedId)
        assertEquals(specialCustomer, decoded.customerName)
        assertEquals(specialNote, decoded.note)

        val decodedItem = decoded.items.single()
        assertEquals(specialProduct, decodedItem.productNameSnapshot)
        assertEquals(specialSku, decodedItem.skuSnapshot)
        assertEquals(specialUnit, decodedItem.unitNameSnapshot)
        assertEquals("unit-kaleng", decodedItem.unitServerId)
        assertEquals(0, BigDecimal("7.5").compareTo(decodedItem.baseQuantity))
        assertEquals(0, BigDecimal("2.5").compareTo(decodedItem.conversionFactor))
    }

    @Test
    fun saveCheckout_persistsValidEscapedSyncQueuePayloadAndAuditMetadataInRoom() = runBlocking {
        val productLocalId = database.productDao().upsert(
            LocalProductEntity(
                serverId = "prod-esc-1",
                clientGeneratedId = "server-product-prod-esc-1",
                name = "Besi \"Siku\"\\4x4\nGalvanis",
                sku = "BS-1",
                unitServerId = "unit-btg",
                unitName = "Btg",
                priceSell = ScaledDecimal2.ofWhole(50_000L),
                priceBuy = ScaledDecimal2.ofWhole(40_000L),
                priceRetail = ScaledDecimal2.ofWhole(50_000L),
                stock = ScaledDecimal2.ofWhole(20L),
                syncStatus = SyncStatus.SYNCED,
            )
        )
        val sessionLocalId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-1",
                clientGeneratedId = "SESSION-1",
                deviceId = "android-test-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_700_000_000_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )

        val specialName = "Besi \"Siku\"\\4x4\nGalvanis\tSuper"
        val specialCustomer = "CV \"Maju\"\\Mandiri\nUtama"
        val result = service.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = "srv-sess-1",
                customerName = specialCustomer,
                paymentMethod = "tunai",
                subtotal = BigDecimal("100000.00"),
                total = BigDecimal("100000.00"),
                paidAmount = BigDecimal("100000.00"),
                note = "Catatan:\n\"Barang dicek\\ulangi\"",
                items = listOf(
                    LocalCheckoutItemCommand(
                        productLocalId = productLocalId,
                        productServerId = "prod-esc-1",
                        productNameSnapshot = specialName,
                        skuSnapshot = "BS\\1\"",
                        unitNameSnapshot = "Btg\t\"",
                        unitServerId = "unit-btg",
                        quantity = BigDecimal("2"),
                        priceAtTransaction = BigDecimal("50000.00"),
                        cogsAtTransaction = BigDecimal("40000.00"),
                        subtotal = BigDecimal("100000.00"),
                    )
                )
            )
        )

        assertTrue(result is LocalCheckoutResult.Success)
        val success = result as LocalCheckoutResult.Success
        val queueItem = database.syncQueueDao().getById(success.syncQueueId)!!
        val decoded = LocalCheckoutService.SYNC_PAYLOAD_JSON.decodeFromString<LocalCheckoutSyncPayload>(queueItem.payloadJson)
        assertEquals(specialCustomer, decoded.customerName)
        assertEquals("Catatan:\n\"Barang dicek\\ulangi\"", decoded.note)
        assertEquals(specialName, decoded.items.single().productNameSnapshot)

        val root = Json.parseToJsonElement(queueItem.payloadJson).jsonObject
        assertEquals(1, root["items"]?.jsonArray?.size)

        val auditLog = database.localAuditLogDao().getBySyncStatus(SyncStatus.PENDING, 10).single()
        val meta = Json.parseToJsonElement(auditLog.metadataJson!!).jsonObject
        assertEquals(success.transactionCode, meta["transactionCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun legacyRoomCheckoutPayloadWithUnquotedNumbersDecodesCleanly() {
        val legacyJson = """
            {
              "transactionLocalId":1,
              "clientGeneratedId":"TRX-OLD-1",
              "transactionCode":"TRX-20261124-0001",
              "cashierUserId":"kasir-1",
              "cashSessionLocalId":2,
              "cashSessionServerId":null,
              "customerLocalId":null,
              "customerServerId":null,
              "customerName":null,
              "paymentMethod":"tunai",
              "subtotal":75000.0,
              "discount":0.0,
              "total":75000.0,
              "paidAmount":75000.0,
              "remainingAmount":0.0,
              "occurredAt":1700000000000,
              "note":null,
              "items":[
                {
                  "productLocalId":5,
                  "productServerId":"prod-1",
                  "productNameSnapshot":"Cat",
                  "skuSnapshot":"CAT-1",
                  "unitNameSnapshot":"Kaleng",
                  "unitServerId":"unit-kaleng",
                  "quantity":3.0,
                  "baseQuantity":7.5,
                  "conversionFactor":2.5,
                  "priceAtTransaction":25000.0,
                  "cogsAtTransaction":20000.0,
                  "discount":0.0,
                  "subtotal":75000.0
                }
              ]
            }
        """.trimIndent()

        val decoded = LocalCheckoutService.SYNC_PAYLOAD_JSON.decodeFromString<LocalCheckoutSyncPayload>(legacyJson)
        assertEquals("TRX-OLD-1", decoded.clientGeneratedId)
        assertEquals(0, BigDecimal("7.5").compareTo(decoded.items.single().baseQuantity))
        assertEquals(0, BigDecimal("2.5").compareTo(decoded.items.single().conversionFactor))
    }
}
