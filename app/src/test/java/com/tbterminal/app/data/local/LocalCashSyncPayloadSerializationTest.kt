package com.tbterminal.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseSyncPayload
import com.tbterminal.app.data.local.cashsession.LocalCashSessionCloseSyncPayload
import com.tbterminal.app.data.local.cashsession.LocalCashSessionOpenSyncPayload
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalCashSyncPayloadSerializationTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var expenseService: LocalCashExpenseService
    private lateinit var sessionService: LocalCashSessionService

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java,
        ).allowMainThreadQueries().build()
        expenseService = LocalCashExpenseService(database, deviceId = "device-\"pos\"\\1")
        sessionService = LocalCashSessionService(database, deviceId = "device-\"pos\"\\1")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cashExpense_addLocalCashMovement_escapesSpecialCharactersInSyncPayload() = runBlocking {
        val sessionLocalId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-\"1\"\\a",
                clientGeneratedId = "SESSION-ESC-1",
                deviceId = "device-\"pos\"\\1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_700_000_000_000L,
                startingCash = 200_000.0,
                syncStatus = SyncStatus.SYNCED,
            )
        )

        val specialCategory = "Beli \"Material\"\\Gudang\nLantai\t2\r\nOK\u0007"
        val specialDescription = "Bayar \"Tukang\"\\Shift-1\nJam 10:00\tSelesai\r\nDone\u0007"

        val result = expenseService.addLocalCashMovement(
            cashSessionLocalId = sessionLocalId,
            type = CashMovementType.CASH_OUT,
            amount = BigDecimal("45000.50"),
            description = specialDescription,
            category = specialCategory,
        )

        assertTrue(result is LocalCashExpenseResult.Success)
        val savedExpense = (result as LocalCashExpenseResult.Success).expense
        val queueItem = database.syncQueueDao().getLatestForEntity(
            entityLocalId = savedExpense.localId,
            entityType = SyncEntityType.CASH_EXPENSE,
        )
        assertNotNull(queueItem)

        val payloadJson = queueItem!!.payloadJson
        val root = Json.parseToJsonElement(payloadJson).jsonObject
        assertEquals("CASH_OUT: $specialCategory", root["category"]?.jsonPrimitive?.content)
        assertEquals(specialDescription, root["note"]?.jsonPrimitive?.content)
        assertEquals("srv-sess-\"1\"\\a", root["serverCashSessionId"]?.jsonPrimitive?.content)
        assertEquals(45000.50, root["amount"]!!.jsonPrimitive.double, 0.001)

        val decoded = LocalCashExpenseService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashExpenseSyncPayload>(payloadJson)
        assertEquals(savedExpense.clientGeneratedId, decoded.clientGeneratedId)
        assertEquals("device-\"pos\"\\1", decoded.deviceId)
        assertEquals("kasir-1", decoded.cashierUserId)
        assertEquals("srv-sess-\"1\"\\a", decoded.serverCashSessionId)
        assertEquals("CASH_OUT: $specialCategory", decoded.category)
        assertEquals(specialDescription, decoded.note)
        assertEquals(45000.50, decoded.amount, 0.001)
    }

    @Test
    fun legacyRoomCashExpensePayload_decodesCleanly() {
        val legacyJson = """
            {
              "clientGeneratedId":"EXPENSE-LEGACY-1",
              "deviceId":"dev-1",
              "cashierUserId":"kasir-1",
              "serverCashSessionId":"srv-1",
              "amount":125000.0,
              "category":"CASH_OUT: OPERASIONAL",
              "note":"Beli \"Paku\"\\box",
              "occurredAt":1700000001000
            }
        """.trimIndent()

        val decoded = LocalCashExpenseService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashExpenseSyncPayload>(legacyJson)
        assertEquals("EXPENSE-LEGACY-1", decoded.clientGeneratedId)
        assertEquals("CASH_OUT: OPERASIONAL", decoded.category)
        assertEquals("Beli \"Paku\"\\box", decoded.note)
        assertEquals(125000.0, decoded.amount, 0.001)
        assertEquals(1700000001000L, decoded.occurredAt)
    }

    @Test
    fun cashSession_openAndClose_escapesSpecialCharactersInSyncPayloads() = runBlocking {
        val specialOpeningNote = "Shift \"Pagi\"\\Kasir-1\nLaci\tAman\r\nCek\u0007"
        val openResult = sessionService.openLocalSession(
            cashierUserId = "kasir-\"1\"\\a",
            startingCash = BigDecimal("150000.00"),
            openingNote = specialOpeningNote,
        )
        assertTrue(openResult is LocalCashSessionResult.Success)
        val opened = (openResult as LocalCashSessionResult.Success).session

        val openQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = opened.localId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.CREATE,
        )
        assertNotNull(openQueue)

        val openRoot = Json.parseToJsonElement(openQueue!!.payloadJson).jsonObject
        assertEquals(specialOpeningNote, openRoot["openingNote"]?.jsonPrimitive?.content)
        val decodedOpen = LocalCashSessionService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashSessionOpenSyncPayload>(openQueue.payloadJson)
        assertEquals(opened.clientGeneratedId, decodedOpen.clientGeneratedId)
        assertEquals("device-\"pos\"\\1", decodedOpen.deviceId)
        assertEquals("kasir-\"1\"\\a", decodedOpen.cashierUserId)
        assertEquals(150000.0, decodedOpen.startingCash, 0.001)
        assertEquals(specialOpeningNote, decodedOpen.openingNote)

        val specialClosingNote = "Tutup \"Shift\"\\Selisih 0\nSetor\tBrankas\r\nOK\u0007"
        val closeResult = sessionService.closeLocalSession(
            localId = opened.localId,
            closingCash = BigDecimal("150000.00"),
            closingNote = specialClosingNote,
        )
        assertTrue(closeResult is LocalCashSessionResult.Success)

        val closeQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = opened.localId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.UPDATE,
        )
        assertNotNull(closeQueue)

        val closeRoot = Json.parseToJsonElement(closeQueue!!.payloadJson).jsonObject
        assertEquals(specialClosingNote, closeRoot["closingNote"]?.jsonPrimitive?.content)
        val decodedClose = LocalCashSessionService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashSessionCloseSyncPayload>(closeQueue.payloadJson)
        assertEquals(opened.clientGeneratedId, decodedClose.clientGeneratedId)
        assertEquals(150000.0, decodedClose.actualCash, 0.001)
        assertEquals(150000.0, decodedClose.expectedCash, 0.001)
        assertEquals(0.0, decodedClose.difference, 0.001)
        assertEquals(specialClosingNote, decodedClose.closingNote)
    }

    @Test
    fun cashSession_nullNotes_omitsOptionalKeysKeepsZeroAmountsAndDecodesLegacyJson() = runBlocking {
        val openResult = sessionService.openLocalSession(
            cashierUserId = "kasir-2",
            startingCash = BigDecimal.ZERO,
            openingNote = null,
        )
        assertTrue(openResult is LocalCashSessionResult.Success)
        val opened = (openResult as LocalCashSessionResult.Success).session

        val openQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = opened.localId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.CREATE,
        )!!
        val openRoot = Json.parseToJsonElement(openQueue.payloadJson).jsonObject
        assertFalse(openRoot.containsKey("openingNote"))
        assertTrue(openRoot.containsKey("startingCash"))
        assertEquals(0.0, openRoot["startingCash"]!!.jsonPrimitive.double, 0.001)

        val closeResult = sessionService.closeLocalSession(
            localId = opened.localId,
            closingCash = BigDecimal.ZERO,
            closingNote = null,
        )
        assertTrue(closeResult is LocalCashSessionResult.Success)

        val closeQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = opened.localId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.UPDATE,
        )!!
        val closeRoot = Json.parseToJsonElement(closeQueue.payloadJson).jsonObject
        assertFalse(closeRoot.containsKey("closingNote"))
        assertTrue(closeRoot.containsKey("actualCash"))
        assertTrue(closeRoot.containsKey("expectedCash"))
        assertTrue(closeRoot.containsKey("difference"))
        assertEquals(0.0, closeRoot["difference"]!!.jsonPrimitive.double, 0.001)

        val legacyOpenWithoutNote = """
            {"clientGeneratedId":"S-1","deviceId":"d-1","cashierUserId":"kasir-2","openedAt":1700000000000,"startingCash":50000.0}
        """.trimIndent()
        val decodedLegacyOpen = LocalCashSessionService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashSessionOpenSyncPayload>(legacyOpenWithoutNote)
        assertNull(decodedLegacyOpen.openingNote)
        assertEquals(50000.0, decodedLegacyOpen.startingCash, 0.001)

        val legacyCloseWithNullNote = """
            {"clientGeneratedId":"S-1","deviceId":"d-1","cashierUserId":"kasir-2","serverId":"srv-s-1","closedAt":1700003600000,"actualCash":50000.0,"expectedCash":50000.0,"difference":0.0,"closingNote":null}
        """.trimIndent()
        val decodedLegacyClose = LocalCashSessionService.SYNC_PAYLOAD_JSON
            .decodeFromString<LocalCashSessionCloseSyncPayload>(legacyCloseWithNullNote)
        assertNull(decodedLegacyClose.closingNote)
        assertEquals("srv-s-1", decodedLegacyClose.serverId)
    }
}
