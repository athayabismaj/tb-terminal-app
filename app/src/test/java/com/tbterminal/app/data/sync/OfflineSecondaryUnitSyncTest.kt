package com.tbterminal.app.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.checkout.LocalCheckoutCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutItemCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutResult
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.checkout.OfflineItemUnitResolution
import com.tbterminal.app.data.local.checkout.resolveOfflineCheckoutItemUnit
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.SalesApi
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class OfflineSecondaryUnitSyncTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var salesApi: SalesApi
    private lateinit var localCheckoutService: LocalCheckoutService
    private lateinit var syncService: OfflineCheckoutSyncService
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java
        ).allowMainThreadQueries().build()

        server = MockWebServer()
        server.start()
        salesApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SalesApi::class.java)

        localCheckoutService = LocalCheckoutService(database, deviceId = "android-dev-1")
        syncService = OfflineCheckoutSyncService(database, salesApi, deviceId = "android-dev-1")
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun twoDusFactor12_persistsUnitMetadataAndSyncsExactBaseQuantityWithUnitId() = runBlocking {
        val productLocalId = database.productDao().upsert(
            LocalProductEntity(
                serverId = "prod-paku",
                clientGeneratedId = "server-product-prod-paku",
                name = "Paku 5 cm",
                sku = "PK-05",
                unitServerId = "unit-pcs",
                unitName = "Pcs",
                secondaryUnitServerId = "unit-dus",
                secondaryUnitName = "Dus",
                secondaryUnitFactor = BigDecimal("12"),
                priceSell = 10_000.0,
                priceBuy = 8_000.0,
                priceRetail = 10_000.0,
                stock = 100.0,
                syncStatus = SyncStatus.SYNCED
            )
        )
        val sessionLocalId = insertSyncedOpenSession("srv-sess-1")

        val saveResult = localCheckoutService.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = "srv-sess-1",
                paymentMethod = "tunai",
                subtotal = 240_000.0,
                total = 240_000.0,
                paidAmount = 240_000.0,
                items = listOf(
                    LocalCheckoutItemCommand(
                        productLocalId = productLocalId,
                        productServerId = "prod-paku",
                        productNameSnapshot = "Paku 5 cm",
                        skuSnapshot = "PK-05",
                        unitNameSnapshot = "Dus",
                        quantity = 2.0,
                        baseQuantity = 24.0,
                        conversionFactor = 12.0,
                        priceAtTransaction = 120_000.0,
                        cogsAtTransaction = 96_000.0,
                        subtotal = 240_000.0,
                        unitServerId = "unit-dus",
                        baseQuantityExact = BigDecimal("24"),
                        conversionFactorExact = BigDecimal("12")
                    )
                )
            )
        )
        assertTrue(saveResult is LocalCheckoutResult.Success)
        val txLocalId = (saveResult as LocalCheckoutResult.Success).transactionLocalId

        val storedItems = database.transactionItemDao().getByTransaction(txLocalId)
        assertEquals(1, storedItems.size)
        val stored = storedItems.single()
        assertEquals("unit-dus", stored.unitServerId)
        assertEquals(2.0, stored.quantity, 0.001)
        assertEquals(0, BigDecimal("24").compareTo(stored.baseQuantity))
        assertEquals(0, BigDecimal("12").compareTo(stored.conversionFactor))
        assertEquals(120_000.0, stored.priceAtTransaction, 0.001)

        val updatedProduct = database.productDao().getByLocalId(productLocalId)!!
        assertEquals(76.0, updatedProduct.stock, 0.001)

        enqueueSuccessSyncResponse("srv-trx-1")
        val syncResult = syncService.syncOne(txLocalId)
        assertTrue(syncResult is OfflineCheckoutSyncResult.Success)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        val items = body["items"]!!.jsonArray
        assertEquals(1, items.size)
        val itemJson = items[0].jsonObject
        assertEquals("2.00", itemJson["quantity"]?.jsonPrimitive?.content)
        assertEquals("120000.00", itemJson["priceAtTransaction"]?.jsonPrimitive?.content)
        assertEquals("240000.00", itemJson["subtotal"]?.jsonPrimitive?.content)
        assertEquals("unit-dus", itemJson["unitId"]?.jsonPrimitive?.content)
        assertEquals("24", itemJson["baseQuantity"]?.jsonPrimitive?.content)
    }

    @Test
    fun mixedDusAndPcsTransaction_syncsBothItemsWithPairedUnitIdAndBaseQuantity() = runBlocking {
        val productLocalId = database.productDao().upsert(
            LocalProductEntity(
                serverId = "prod-paku",
                clientGeneratedId = "server-product-prod-paku",
                name = "Paku 5 cm",
                sku = "PK-05",
                unitServerId = "unit-pcs",
                unitName = "Pcs",
                secondaryUnitServerId = "unit-dus",
                secondaryUnitName = "Dus",
                secondaryUnitFactor = BigDecimal("12"),
                priceSell = 10_000.0,
                priceBuy = 8_000.0,
                priceRetail = 10_000.0,
                stock = 100.0,
                syncStatus = SyncStatus.SYNCED
            )
        )
        val sessionLocalId = insertSyncedOpenSession("srv-sess-2")

        val saveResult = localCheckoutService.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = "srv-sess-2",
                paymentMethod = "tunai",
                subtotal = 270_000.0,
                total = 270_000.0,
                paidAmount = 270_000.0,
                items = listOf(
                    LocalCheckoutItemCommand(
                        productLocalId = productLocalId,
                        productServerId = "prod-paku",
                        productNameSnapshot = "Paku 5 cm",
                        unitNameSnapshot = "Dus",
                        quantity = 2.0,
                        baseQuantity = 24.0,
                        conversionFactor = 12.0,
                        priceAtTransaction = 120_000.0,
                        cogsAtTransaction = 96_000.0,
                        subtotal = 240_000.0,
                        unitServerId = "unit-dus",
                        baseQuantityExact = BigDecimal("24"),
                        conversionFactorExact = BigDecimal("12")
                    ),
                    LocalCheckoutItemCommand(
                        productLocalId = productLocalId,
                        productServerId = "prod-paku",
                        productNameSnapshot = "Paku 5 cm",
                        unitNameSnapshot = "Pcs",
                        quantity = 3.0,
                        baseQuantity = 3.0,
                        conversionFactor = 1.0,
                        priceAtTransaction = 10_000.0,
                        cogsAtTransaction = 8_000.0,
                        subtotal = 30_000.0,
                        unitServerId = "unit-pcs",
                        baseQuantityExact = BigDecimal("3"),
                        conversionFactorExact = BigDecimal.ONE
                    )
                )
            )
        )
        assertTrue(saveResult is LocalCheckoutResult.Success)
        val txLocalId = (saveResult as LocalCheckoutResult.Success).transactionLocalId

        val updatedProduct = database.productDao().getByLocalId(productLocalId)!!
        assertEquals(73.0, updatedProduct.stock, 0.001)

        enqueueSuccessSyncResponse("srv-trx-2")
        val syncResult = syncService.syncOne(txLocalId)
        assertTrue(syncResult is OfflineCheckoutSyncResult.Success)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        val items = body["items"]!!.jsonArray
        assertEquals(2, items.size)

        val dusItem = items[0].jsonObject
        assertEquals("2.00", dusItem["quantity"]?.jsonPrimitive?.content)
        assertEquals("120000.00", dusItem["priceAtTransaction"]?.jsonPrimitive?.content)
        assertEquals("240000.00", dusItem["subtotal"]?.jsonPrimitive?.content)
        assertEquals("unit-dus", dusItem["unitId"]?.jsonPrimitive?.content)
        assertEquals("24", dusItem["baseQuantity"]?.jsonPrimitive?.content)

        val pcsItem = items[1].jsonObject
        assertEquals("3.00", pcsItem["quantity"]?.jsonPrimitive?.content)
        assertEquals("10000.00", pcsItem["priceAtTransaction"]?.jsonPrimitive?.content)
        assertEquals("30000.00", pcsItem["subtotal"]?.jsonPrimitive?.content)
        assertEquals("unit-pcs", pcsItem["unitId"]?.jsonPrimitive?.content)
        assertEquals("3", pcsItem["baseQuantity"]?.jsonPrimitive?.content)
    }

    @Test
    fun legacyItemWithoutUnitData_omitsBothUnitIdAndBaseQuantityFromSyncRequest() = runBlocking {
        // Current product has secondary unit configured; legacy transaction must NOT recompute from it.
        database.productDao().upsert(
            LocalProductEntity(
                serverId = "prod-paku",
                clientGeneratedId = "server-product-prod-paku",
                name = "Paku 5 cm",
                unitServerId = "unit-pcs",
                secondaryUnitServerId = "unit-dus",
                secondaryUnitFactor = BigDecimal("12"),
                stock = 50.0,
                syncStatus = SyncStatus.SYNCED
            )
        )
        val txLocalId = insertTransactionWithRawItem(
            clientGeneratedId = "tx-legacy-1",
            unitServerId = null,
            baseQuantity = null,
            conversionFactor = null
        )

        enqueueSuccessSyncResponse("srv-trx-legacy")
        val syncResult = syncService.syncOne(txLocalId)
        assertTrue(syncResult is OfflineCheckoutSyncResult.Success)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        val itemJson = body["items"]!!.jsonArray.single().jsonObject
        assertEquals("2.00", itemJson["quantity"]?.jsonPrimitive?.content)
        assertEquals("120000.00", itemJson["priceAtTransaction"]?.jsonPrimitive?.content)
        assertFalse(itemJson.containsKey("unitId"))
        assertFalse(itemJson.containsKey("baseQuantity"))
    }

    @Test
    fun partialUnitFieldsOnLocalItem_neverSendsUnpairedUnitIdOrBaseQuantity() = runBlocking {
        val txOnlyUnitId = insertTransactionWithRawItem(
            clientGeneratedId = "tx-unpaired-unit",
            unitServerId = "unit-dus",
            baseQuantity = null,
            conversionFactor = BigDecimal("12")
        )
        val txOnlyBaseQty = insertTransactionWithRawItem(
            clientGeneratedId = "tx-unpaired-base",
            unitServerId = null,
            baseQuantity = BigDecimal("24"),
            conversionFactor = BigDecimal("12")
        )

        enqueueSuccessSyncResponse("srv-trx-u1")
        enqueueSuccessSyncResponse("srv-trx-u2")

        assertTrue(syncService.syncOne(txOnlyUnitId) is OfflineCheckoutSyncResult.Success)
        val firstItem = Json.parseToJsonElement(server.takeRequest().body.readUtf8())
            .jsonObject["items"]!!.jsonArray.single().jsonObject
        assertFalse(firstItem.containsKey("unitId"))
        assertFalse(firstItem.containsKey("baseQuantity"))

        assertTrue(syncService.syncOne(txOnlyBaseQty) is OfflineCheckoutSyncResult.Success)
        val secondItem = Json.parseToJsonElement(server.takeRequest().body.readUtf8())
            .jsonObject["items"]!!.jsonArray.single().jsonObject
        assertFalse(secondItem.containsKey("unitId"))
        assertFalse(secondItem.containsKey("baseQuantity"))
    }

    @Test
    fun secondaryUnitBaseQuantityScaleValidation_rejectsOver2DecimalsAndSendsExactTwoDecimals() = runBlocking {
        val rejected = resolveOfflineCheckoutItemUnit(
            productName = "Kawat Bendrat",
            quantity = 1,
            isSecondaryUnit = true,
            cartBaseUnitId = "unit-kg",
            cartSecondaryUnitId = "unit-ons",
            cartSecondaryUnitFactor = BigDecimal("0.125"),
            cachedBaseUnitId = "unit-kg",
            cachedSecondaryUnitId = "unit-ons",
            cachedSecondaryUnitFactor = BigDecimal("0.125")
        )
        assertTrue(rejected is OfflineItemUnitResolution.Error)

        val sessionLocalId = insertSyncedOpenSession("srv-sess-scale")
        val invalidSave = localCheckoutService.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = "srv-sess-scale",
                paymentMethod = "tunai",
                subtotal = 5_000.0,
                total = 5_000.0,
                paidAmount = 5_000.0,
                items = listOf(
                    LocalCheckoutItemCommand(
                        productServerId = "prod-kawat",
                        productNameSnapshot = "Kawat Bendrat",
                        unitNameSnapshot = "Ons",
                        quantity = 1.0,
                        baseQuantity = 0.125,
                        conversionFactor = 0.125,
                        priceAtTransaction = 5_000.0,
                        subtotal = 5_000.0,
                        unitServerId = "unit-ons",
                        baseQuantityExact = BigDecimal("0.125"),
                        conversionFactorExact = BigDecimal("0.125")
                    )
                )
            )
        )
        assertTrue(invalidSave is LocalCheckoutResult.Failed)

        val accepted = resolveOfflineCheckoutItemUnit(
            productName = "Kawat Bendrat",
            quantity = 2,
            isSecondaryUnit = true,
            cartBaseUnitId = "unit-kg",
            cartSecondaryUnitId = "unit-ons",
            cartSecondaryUnitFactor = BigDecimal("0.125"),
            cachedBaseUnitId = "unit-kg",
            cachedSecondaryUnitId = "unit-ons",
            cachedSecondaryUnitFactor = BigDecimal("0.125")
        )
        assertTrue(accepted is OfflineItemUnitResolution.Success)
        accepted as OfflineItemUnitResolution.Success
        assertEquals("unit-ons", accepted.unitServerId)
        assertEquals("0.25", accepted.baseQuantity.toPlainString())

        val validSave = localCheckoutService.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = "srv-sess-scale",
                paymentMethod = "tunai",
                subtotal = 10_000.0,
                total = 10_000.0,
                paidAmount = 10_000.0,
                items = listOf(
                    LocalCheckoutItemCommand(
                        productServerId = "prod-kawat",
                        productNameSnapshot = "Kawat Bendrat",
                        unitNameSnapshot = "Ons",
                        quantity = 2.0,
                        baseQuantity = 0.25,
                        conversionFactor = 0.125,
                        priceAtTransaction = 5_000.0,
                        subtotal = 10_000.0,
                        unitServerId = accepted.unitServerId,
                        baseQuantityExact = accepted.baseQuantity,
                        conversionFactorExact = accepted.conversionFactor
                    )
                )
            )
        )
        assertTrue(validSave is LocalCheckoutResult.Success)
        val validTxId = (validSave as LocalCheckoutResult.Success).transactionLocalId

        enqueueSuccessSyncResponse("srv-trx-scale")
        val syncResult = syncService.syncOne(validTxId)
        assertTrue(syncResult is OfflineCheckoutSyncResult.Success)
        val itemJson = Json.parseToJsonElement(server.takeRequest().body.readUtf8())
            .jsonObject["items"]!!.jsonArray.single().jsonObject
        assertEquals("unit-ons", itemJson["unitId"]?.jsonPrimitive?.content)
        assertEquals("0.25", itemJson["baseQuantity"]?.jsonPrimitive?.content)
        assertNotNull(database.transactionItemDao().getByTransaction(validTxId).single().unitServerId)
        assertNull(database.transactionItemDao().getByTransaction(validTxId).single().deletedAt)
    }

    private suspend fun insertSyncedOpenSession(serverId: String): Long {
        return database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = serverId,
                clientGeneratedId = "sess-$serverId",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = 100_000.0,
                syncStatus = SyncStatus.SYNCED
            )
        )
    }

    private suspend fun insertTransactionWithRawItem(
        clientGeneratedId: String,
        unitServerId: String?,
        baseQuantity: BigDecimal?,
        conversionFactor: BigDecimal?
    ): Long {
        val txId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = clientGeneratedId,
                deviceId = "android-dev-1",
                transactionCode = "TRX-$clientGeneratedId",
                cashSessionLocalId = 1L,
                cashSessionServerId = "srv-sess-legacy",
                cashierUserId = "kasir-1",
                status = "PAID",
                subtotal = 240_000.0,
                total = 240_000.0,
                paidAmount = 240_000.0,
                remainingAmount = 0.0
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = txId,
                productServerId = "prod-paku",
                productNameSnapshot = "Paku 5 cm",
                unitNameSnapshot = "Dus",
                unitServerId = unitServerId,
                quantity = 2.0,
                baseQuantity = baseQuantity,
                conversionFactor = conversionFactor,
                priceAtTransaction = 120_000.0,
                cogsAtTransaction = 96_000.0,
                subtotal = 240_000.0
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "pay-$clientGeneratedId",
                transactionLocalId = txId,
                method = "tunai",
                amount = 240_000.0
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = txId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}"
            )
        )
        return txId
    }

    private fun enqueueSuccessSyncResponse(serverTransactionId: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "syncStatus": "CREATED",
                    "serverTransactionId": "$serverTransactionId",
                    "receiptId": "RCP-$serverTransactionId",
                    "serverPaymentIds": ["pay-1"],
                    "syncedAt": "2026-11-24T10:20:00+07:00"
                  }
                }
                """.trimIndent()
            )
        )
    }
}
