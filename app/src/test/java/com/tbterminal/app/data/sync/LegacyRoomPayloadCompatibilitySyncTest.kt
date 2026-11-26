package com.tbterminal.app.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseSyncPayload
import com.tbterminal.app.data.local.cashsession.LocalCashSessionCloseSyncPayload
import com.tbterminal.app.data.local.cashsession.LocalCashSessionOpenSyncPayload
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.checkout.LocalCheckoutSyncPayload
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
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
class LegacyRoomPayloadCompatibilitySyncTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var salesApi: SalesApi
    private lateinit var cashSessionSyncService: OfflineCashSessionSyncService
    private lateinit var cashExpenseSyncService: OfflineCashExpenseSyncService
    private lateinit var checkoutSyncService: OfflineCheckoutSyncService

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java,
        ).allowMainThreadQueries().build()

        server = MockWebServer()
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        salesApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SalesApi::class.java)

        cashSessionSyncService = OfflineCashSessionSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = "android-dev-1",
        )
        cashExpenseSyncService = OfflineCashExpenseSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = "android-dev-1",
            cashSessionSyncService = cashSessionSyncService,
        )
        checkoutSyncService = OfflineCheckoutSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = "android-dev-1",
            cashSessionSyncService = cashSessionSyncService,
            cashExpenseSyncService = cashExpenseSyncService,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun legacyCheckoutRoomRows_preB1bAndB1bFormats_decodeAndSyncEndToEnd() = runBlocking {
        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-checkout-1",
                clientGeneratedId = "SESSION-CHK-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_700_000_000_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )

        // Kasus A: format pre-B1b (tanpa unitServerId, angka baseQuantity/conversionFactor tanpa kutip)
        val preB1bTxId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "TRX-PREB1B-1",
                deviceId = "android-dev-1",
                transactionCode = "TRX-20261120-0001",
                cashSessionLocalId = sessionId,
                cashSessionServerId = "srv-sess-checkout-1",
                cashierUserId = "kasir-1",
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(75_000L),
                total = ScaledDecimal2.ofWhole(75_000L),
                paidAmount = ScaledDecimal2.ofWhole(75_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                occurredAt = 1_700_000_100_000L,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = preB1bTxId,
                productServerId = "prod-semen",
                productNameSnapshot = "Semen Tiga Roda",
                skuSnapshot = "SMN-1",
                unitNameSnapshot = "Sak",
                unitServerId = null,
                quantity = ScaledDecimal2.ofWhole(3L),
                baseQuantity = null,
                conversionFactor = null,
                priceAtTransaction = ScaledDecimal2.ofWhole(25_000L),
                cogsAtTransaction = ScaledDecimal2.ofWhole(21_000L),
                subtotal = ScaledDecimal2.ofWhole(75_000L),
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "PAY-PREB1B-1",
                transactionLocalId = preB1bTxId,
                method = "tunai",
                amount = ScaledDecimal2.ofWhole(75_000L),
            )
        )
        val preB1bPayloadJson = """
            {
              "transactionLocalId":$preB1bTxId,
              "clientGeneratedId":"TRX-PREB1B-1",
              "transactionCode":"TRX-20261120-0001",
              "cashierUserId":"kasir-1",
              "cashSessionLocalId":$sessionId,
              "cashSessionServerId":"srv-sess-checkout-1",
              "customerLocalId":null,
              "customerServerId":null,
              "customerName":null,
              "paymentMethod":"tunai",
              "subtotal":75000.0,
              "discount":0.0,
              "total":75000.0,
              "paidAmount":75000.0,
              "remainingAmount":0.0,
              "occurredAt":1700000100000,
              "note":null,
              "items":[
                {
                  "productLocalId":null,
                  "productServerId":"prod-semen",
                  "productNameSnapshot":"Semen Tiga Roda",
                  "skuSnapshot":"SMN-1",
                  "unitNameSnapshot":"Sak",
                  "quantity":3.0,
                  "baseQuantity":3.0,
                  "conversionFactor":1.0,
                  "priceAtTransaction":25000.0,
                  "cogsAtTransaction":21000.0,
                  "discount":0.0,
                  "subtotal":75000.0
                }
              ]
            }
        """.trimIndent()
        val preB1bQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = preB1bTxId,
                operation = SyncOperation.CREATE,
                payloadJson = preB1bPayloadJson,
            )
        )

        val storedPreB1bQueue = database.syncQueueDao().getById(preB1bQueueId)!!
        val decodedPreB1b = LocalCheckoutService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCheckoutSyncPayload>(storedPreB1bQueue.payloadJson)
        assertEquals("TRX-PREB1B-1", decodedPreB1b.clientGeneratedId)
        assertNull(decodedPreB1b.items.single().unitServerId)
        assertEquals(0, BigDecimal("3.0").compareTo(decodedPreB1b.items.single().baseQuantity))
        assertEquals(0, BigDecimal("1.0").compareTo(decodedPreB1b.items.single().conversionFactor))

        enqueueCheckoutSuccess("srv-trx-preb1b")
        val preB1bSyncResult = checkoutSyncService.syncOne(preB1bTxId)
        assertTrue(preB1bSyncResult is OfflineCheckoutSyncResult.Success)

        val preB1bRequestBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        val preB1bRequestItem = preB1bRequestBody["items"]!!.jsonArray.single().jsonObject
        assertEquals("prod-semen", preB1bRequestItem["productId"]?.jsonPrimitive?.content)
        assertEquals("3.00", preB1bRequestItem["quantity"]?.jsonPrimitive?.content)
        assertFalse(preB1bRequestItem.containsKey("unitId"))
        assertFalse(preB1bRequestItem.containsKey("baseQuantity"))
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(preB1bTxId)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(preB1bQueueId)!!.status)

        // Kasus B: format B1b (memuat unitServerId dan baseQuantity/conversionFactor desimal)
        val b1bTxId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "TRX-B1B-1",
                deviceId = "android-dev-1",
                transactionCode = "TRX-20261124-0002",
                cashSessionLocalId = sessionId,
                cashSessionServerId = "srv-sess-checkout-1",
                cashierUserId = "kasir-1",
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(150_000L),
                total = ScaledDecimal2.ofWhole(150_000L),
                paidAmount = ScaledDecimal2.ofWhole(150_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                occurredAt = 1_700_000_200_000L,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = b1bTxId,
                productServerId = "prod-cat",
                productNameSnapshot = "Cat Tembok",
                skuSnapshot = "CAT-1",
                unitNameSnapshot = "Kaleng",
                unitServerId = "unit-kaleng",
                quantity = ScaledDecimal2.ofWhole(3L),
                baseQuantity = BigDecimal("7.5"),
                conversionFactor = BigDecimal("2.5"),
                priceAtTransaction = ScaledDecimal2.ofWhole(50_000L),
                cogsAtTransaction = ScaledDecimal2.ofWhole(40_000L),
                subtotal = ScaledDecimal2.ofWhole(150_000L),
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "PAY-B1B-1",
                transactionLocalId = b1bTxId,
                method = "tunai",
                amount = ScaledDecimal2.ofWhole(150_000L),
            )
        )
        val b1bPayloadJson = """
            {
              "transactionLocalId":$b1bTxId,
              "clientGeneratedId":"TRX-B1B-1",
              "transactionCode":"TRX-20261124-0002",
              "cashierUserId":"kasir-1",
              "cashSessionLocalId":$sessionId,
              "cashSessionServerId":"srv-sess-checkout-1",
              "customerLocalId":null,
              "customerServerId":null,
              "customerName":null,
              "paymentMethod":"tunai",
              "subtotal":150000.0,
              "discount":0.0,
              "total":150000.0,
              "paidAmount":150000.0,
              "remainingAmount":0.0,
              "occurredAt":1700000200000,
              "note":null,
              "items":[
                {
                  "productLocalId":null,
                  "productServerId":"prod-cat",
                  "productNameSnapshot":"Cat Tembok",
                  "skuSnapshot":"CAT-1",
                  "unitNameSnapshot":"Kaleng",
                  "unitServerId":"unit-kaleng",
                  "quantity":3.0,
                  "baseQuantity":7.5,
                  "conversionFactor":2.5,
                  "priceAtTransaction":50000.0,
                  "cogsAtTransaction":40000.0,
                  "discount":0.0,
                  "subtotal":150000.0
                }
              ]
            }
        """.trimIndent()
        val b1bQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = b1bTxId,
                operation = SyncOperation.CREATE,
                payloadJson = b1bPayloadJson,
            )
        )

        val storedB1bQueue = database.syncQueueDao().getById(b1bQueueId)!!
        val decodedB1b = LocalCheckoutService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCheckoutSyncPayload>(storedB1bQueue.payloadJson)
        assertEquals("unit-kaleng", decodedB1b.items.single().unitServerId)
        assertEquals(0, BigDecimal("7.5").compareTo(decodedB1b.items.single().baseQuantity))
        assertEquals(0, BigDecimal("2.5").compareTo(decodedB1b.items.single().conversionFactor))

        enqueueCheckoutSuccess("srv-trx-b1b")
        val b1bSyncResult = checkoutSyncService.syncOne(b1bTxId)
        assertTrue(b1bSyncResult is OfflineCheckoutSyncResult.Success)

        val b1bRequestBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        val b1bRequestItem = b1bRequestBody["items"]!!.jsonArray.single().jsonObject
        assertEquals("unit-kaleng", b1bRequestItem["unitId"]?.jsonPrimitive?.content)
        assertEquals("7.5", b1bRequestItem["baseQuantity"]?.jsonPrimitive?.content)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(b1bTxId)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(b1bQueueId)!!.status)
    }

    @Test
    fun legacyCashSessionRoomRows_openAndCloseFormats_decodeAndSyncEndToEnd() = runBlocking {
        val sessionLocalId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = null,
                clientGeneratedId = "SESSION-LEGACY-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "CLOSED",
                openedAt = 1_700_000_000_000L,
                closedAt = 1_700_003_600_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                expectedCash = ScaledDecimal2.ofWhole(150_000L),
                actualCash = ScaledDecimal2.ofWhole(150_000L),
                difference = ScaledDecimal2.ZERO,
                openingNote = null,
                closingNote = "Tutup \"Shift\"\\1",
                syncStatus = SyncStatus.PENDING,
            )
        )

        val legacyOpenQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessionLocalId,
                operation = SyncOperation.CREATE,
                payloadJson = """{"clientGeneratedId":"SESSION-LEGACY-1","deviceId":"android-dev-1","cashierUserId":"kasir-1","openedAt":1700000000000,"startingCash":100000.0}""",
            )
        )
        val legacyCloseQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessionLocalId,
                operation = SyncOperation.UPDATE,
                payloadJson = """{"clientGeneratedId":"SESSION-LEGACY-1","deviceId":"android-dev-1","cashierUserId":"kasir-1","serverId":"","closedAt":1700003600000,"actualCash":150000.0,"expectedCash":150000.0,"difference":0.0,"closingNote":"Tutup \"Shift\"\\1"}""",
            )
        )

        val storedOpen = database.syncQueueDao().getById(legacyOpenQueueId)!!
        val storedClose = database.syncQueueDao().getById(legacyCloseQueueId)!!
        val decodedOpen = LocalCashSessionService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashSessionOpenSyncPayload>(storedOpen.payloadJson)
        val decodedClose = LocalCashSessionService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashSessionCloseSyncPayload>(storedClose.payloadJson)
        assertEquals("SESSION-LEGACY-1", decodedOpen.clientGeneratedId)
        assertEquals(0, BigDecimal("100000.0").compareTo(decodedOpen.startingCash))
        assertNull(decodedOpen.openingNote)
        assertEquals("Tutup \"Shift\"\\1", decodedClose.closingNote)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "syncStatus": "CREATED",
                    "serverCashSessionId": "srv-sess-legacy-1",
                    "openedAt": "2026-11-24T08:00:00+07:00",
                    "syncedAt": "2026-11-24T08:00:05+07:00"
                  }
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "syncStatus": "UPDATED",
                    "serverCashSessionId": "srv-sess-legacy-1",
                    "closedAt": "2026-11-24T17:00:00+07:00",
                    "syncedAt": "2026-11-24T17:00:05+07:00"
                  }
                }
                """.trimIndent()
            )
        )

        val closeSyncResult = cashSessionSyncService.syncCloseSessionIfReady(sessionLocalId)
        assertTrue(closeSyncResult is OfflineCashSessionSyncResult.Success)
        assertEquals("srv-sess-legacy-1", (closeSyncResult as OfflineCashSessionSyncResult.Success).serverCashSessionId)

        val openRecorded = server.takeRequest()
        assertEquals("/api/sales/sessions/sync/open", openRecorded.path)
        val openRequestJson = Json.parseToJsonElement(openRecorded.body.readUtf8()).jsonObject
        assertEquals("SESSION-LEGACY-1", openRequestJson["clientGeneratedId"]?.jsonPrimitive?.content)
        assertEquals("100000.00", openRequestJson["startingCash"]?.jsonPrimitive?.content)

        val closeRecorded = server.takeRequest()
        assertEquals("/api/sales/sessions/sync/close", closeRecorded.path)
        val closeRequestJson = Json.parseToJsonElement(closeRecorded.body.readUtf8()).jsonObject
        assertEquals("srv-sess-legacy-1", closeRequestJson["serverCashSessionId"]?.jsonPrimitive?.content)
        assertEquals("Tutup \"Shift\"\\1", closeRequestJson["closingNote"]?.jsonPrimitive?.content)
        assertEquals("150000.00", closeRequestJson["actualCash"]?.jsonPrimitive?.content)

        val syncedSession = database.cashSessionDao().getByLocalId(sessionLocalId)!!
        assertEquals("srv-sess-legacy-1", syncedSession.serverId)
        assertEquals(SyncStatus.SYNCED, syncedSession.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(legacyOpenQueueId)!!.status)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(legacyCloseQueueId)!!.status)
    }

    @Test
    fun legacyCashExpenseRoomRow_oldFormat_decodesAndSyncsEndToEnd() = runBlocking {
        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-exp-1",
                clientGeneratedId = "SESSION-EXP-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_700_000_000_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )
        val expenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXPENSE-LEGACY-SYNC-1",
                deviceId = "android-dev-1",
                cashSessionLocalId = sessionId,
                cashSessionServerId = "srv-sess-exp-1",
                category = "CASH_OUT: OPERASIONAL",
                description = "Beli \"Paku\"\\box",
                amount = ScaledDecimal2.ofWhole(45_000L),
                occurredAt = 1_700_001_200_000L,
                syncStatus = SyncStatus.PENDING,
            )
        )
        val legacyQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expenseId,
                operation = SyncOperation.CREATE,
                payloadJson = """{"clientGeneratedId":"EXPENSE-LEGACY-SYNC-1","deviceId":"android-dev-1","cashierUserId":"kasir-1","serverCashSessionId":"srv-sess-exp-1","amount":45000.0,"category":"CASH_OUT: OPERASIONAL","note":"Beli \"Paku\"\\box","occurredAt":1700001200000}""",
            )
        )

        val storedQueue = database.syncQueueDao().getById(legacyQueueId)!!
        val decoded = LocalCashExpenseService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashExpenseSyncPayload>(storedQueue.payloadJson)
        assertEquals("EXPENSE-LEGACY-SYNC-1", decoded.clientGeneratedId)
        assertEquals("srv-sess-exp-1", decoded.serverCashSessionId)
        assertEquals("CASH_OUT: OPERASIONAL", decoded.category)
        assertEquals("Beli \"Paku\"\\box", decoded.note)
        assertEquals(0, BigDecimal("45000.0").compareTo(decoded.amount))

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "syncStatus": "CREATED",
                    "serverExpenseId": "srv-exp-legacy-1",
                    "syncedAt": "2026-11-24T10:15:00+07:00"
                  }
                }
                """.trimIndent()
            )
        )

        val syncResult = cashExpenseSyncService.syncExpense(expenseId)
        assertTrue(syncResult is OfflineCashExpenseSyncResult.Success)
        assertEquals("srv-exp-legacy-1", (syncResult as OfflineCashExpenseSyncResult.Success).serverExpenseId)

        val recorded = server.takeRequest()
        assertEquals("/api/sales/cash-expenses/sync", recorded.path)
        val requestJson = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
        assertEquals("srv-sess-exp-1", requestJson["serverCashSessionId"]?.jsonPrimitive?.content)
        assertEquals("45000.00", requestJson["amount"]?.jsonPrimitive?.content)
        assertEquals("CASH_OUT: OPERASIONAL", requestJson["category"]?.jsonPrimitive?.content)
        assertEquals("Beli \"Paku\"\\box", requestJson["note"]?.jsonPrimitive?.content)

        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(expenseId)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(legacyQueueId)!!.status)
    }

    @Test
    fun legacyRoomRowsWithMalformedUnescapedNewlinePayloadJson_stillSyncCleanlyEndToEnd() = runBlocking {
        // Simulasikan bug format lama sebelum B1c yang meninggalkan newline mentah di sync_queue.payloadJson
        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = null,
                clientGeneratedId = "SESSION-RAW-NL-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_700_000_000_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                openingNote = "Shift pagi\nLaci 1",
                syncStatus = SyncStatus.PENDING,
            )
        )
        val sessionQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessionId,
                operation = SyncOperation.CREATE,
                payloadJson = "{\"clientGeneratedId\":\"SESSION-RAW-NL-1\",\"deviceId\":\"android-dev-1\",\"cashierUserId\":\"kasir-1\",\"openedAt\":1700000000000,\"startingCash\":100000.0,\"openingNote\":\"Shift pagi\nLaci 1\"}",
            )
        )

        val expenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXP-RAW-NL-1",
                deviceId = "android-dev-1",
                cashSessionLocalId = sessionId,
                cashSessionServerId = null,
                category = "CASH_OUT: OPERASIONAL",
                description = "Ongkir\nTrucking",
                amount = ScaledDecimal2.ofWhole(30_000L),
                occurredAt = 1_700_000_500_000L,
                syncStatus = SyncStatus.PENDING,
            )
        )
        val expenseQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expenseId,
                operation = SyncOperation.CREATE,
                payloadJson = "{\"clientGeneratedId\":\"EXP-RAW-NL-1\",\"deviceId\":\"android-dev-1\",\"cashierUserId\":\"kasir-1\",\"serverCashSessionId\":\"\",\"amount\":30000.0,\"category\":\"CASH_OUT: OPERASIONAL\",\"note\":\"Ongkir\nTrucking\",\"occurredAt\":1700000500000}",
            )
        )

        val txId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "TRX-RAW-NL-1",
                deviceId = "android-dev-1",
                transactionCode = "TRX-20261124-NL01",
                cashSessionLocalId = sessionId,
                cashSessionServerId = null,
                cashierUserId = "kasir-1",
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(50_000L),
                total = ScaledDecimal2.ofWhole(50_000L),
                paidAmount = ScaledDecimal2.ofWhole(50_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                occurredAt = 1_700_000_600_000L,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = txId,
                productServerId = "prod-kawat",
                productNameSnapshot = "Kawat\nSuper",
                quantity = ScaledDecimal2.ofWhole(2L),
                priceAtTransaction = ScaledDecimal2.ofWhole(25_000L),
                cogsAtTransaction = ScaledDecimal2.ofWhole(20_000L),
                subtotal = ScaledDecimal2.ofWhole(50_000L),
                note = "Baris 1\nBaris 2",
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "PAY-RAW-NL-1",
                transactionLocalId = txId,
                method = "tunai",
                amount = ScaledDecimal2.ofWhole(50_000L),
            )
        )
        val txQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = txId,
                operation = SyncOperation.CREATE,
                payloadJson = "{\"transactionLocalId\":$txId,\"clientGeneratedId\":\"TRX-RAW-NL-1\",\"note\":\"Baris 1\nBaris 2\",\"items\":[{\"productNameSnapshot\":\"Kawat\nSuper\"}]}",
            )
        )

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "syncStatus": "CREATED",
                    "serverCashSessionId": "srv-sess-nl-1",
                    "openedAt": "2026-11-24T08:00:00+07:00",
                    "syncedAt": "2026-11-24T08:00:05+07:00"
                  }
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "data": {
                    "syncStatus": "CREATED",
                    "serverExpenseId": "srv-exp-nl-1",
                    "syncedAt": "2026-11-24T08:10:00+07:00"
                  }
                }
                """.trimIndent()
            )
        )
        enqueueCheckoutSuccess("srv-trx-nl-1")

        val openRes = cashSessionSyncService.syncOpenSession(sessionId)
        val expRes = cashExpenseSyncService.syncExpense(expenseId)
        val txRes = checkoutSyncService.syncOne(txId)

        assertTrue(openRes is OfflineCashSessionSyncResult.Success)
        assertTrue(expRes is OfflineCashExpenseSyncResult.Success)
        assertTrue(txRes is OfflineCheckoutSyncResult.Success)

        val openReq = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("Shift pagi\nLaci 1", openReq["openingNote"]?.jsonPrimitive?.content)

        val expReq = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("srv-sess-nl-1", expReq["serverCashSessionId"]?.jsonPrimitive?.content)
        assertEquals("Ongkir\nTrucking", expReq["note"]?.jsonPrimitive?.content)

        val txReq = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("srv-sess-nl-1", txReq["cashSessionId"]?.jsonPrimitive?.content)
        val txReqItem = txReq["items"]!!.jsonArray.single().jsonObject
        assertEquals("Kawat\nSuper", txReqItem["productNameSnapshot"]?.jsonPrimitive?.content)
        assertEquals("Baris 1\nBaris 2", txReqItem["note"]?.jsonPrimitive?.content)

        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(sessionQueueId)!!.status)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(expenseQueueId)!!.status)
        assertEquals(SyncStatus.SYNCED, database.syncQueueDao().getById(txQueueId)!!.status)
    }

    private fun enqueueCheckoutSuccess(serverTransactionId: String) {
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
