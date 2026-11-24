package com.tbterminal.app.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class OfflineSyncConflictHandlingTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var salesApi: SalesApi

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java
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
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun checkout409OfflinePriceConflictStoresLastErrorCodeAndTailoredGuidanceWithoutMessageKeywords() = runBlocking {
        val txId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "tx-client-1",
                deviceId = "android-dev-1",
                transactionCode = "OFF-001",
                cashSessionLocalId = 10L,
                cashSessionServerId = "srv-session-1",
                cashierUserId = "kasir-1",
                status = "PAID",
                subtotal = 50_000.0,
                total = 50_000.0,
                paidAmount = 50_000.0,
                remainingAmount = 0.0,
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = txId,
                productServerId = "prod-1",
                productNameSnapshot = "Semen 50kg",
                quantity = 1.0,
                priceAtTransaction = 50_000.0,
                cogsAtTransaction = 40_000.0,
                subtotal = 50_000.0,
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "pay-client-1",
                transactionLocalId = txId,
                method = "tunai",
                amount = 50_000.0,
            )
        )
        val queueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = txId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("""{"success":false,"code":"OFFLINE_PRICE_CONFLICT","message":"Harga Semen berbeda dari katalog server"}""")
        )

        val checkoutSync = OfflineCheckoutSyncService(database, salesApi, "android-dev-1")
        val cashSessionSync = OfflineCashSessionSyncService(database, salesApi, "android-dev-1")
        val cashExpenseSync = OfflineCashExpenseSyncService(database, salesApi, "android-dev-1", cashSessionSync)
        val monitoringRepo = SyncMonitoringRepository(
            syncQueueDao = database.syncQueueDao(),
            offlineCashSessionSyncService = cashSessionSync,
            offlineCheckoutSyncService = checkoutSync,
            offlineCashExpenseSyncService = cashExpenseSync,
        )

        val result = checkoutSync.syncOne(txId)
        assertTrue(result is OfflineCheckoutSyncResult.Failed)
        result as OfflineCheckoutSyncResult.Failed
        assertEquals(OfflineCheckoutSyncFailureKind.CONFLICT, result.kind)

        val queueEntry = database.syncQueueDao().getById(queueId)!!
        assertEquals(SyncStatus.CONFLICT, queueEntry.status)
        assertEquals("OFFLINE_PRICE_CONFLICT", queueEntry.lastErrorCode)
        assertEquals("Harga Semen berbeda dari katalog server", queueEntry.lastError)
        assertFalse(queueEntry.lastError!!.trimStart().startsWith("{"))

        val snapshot = monitoringRepo.observeSnapshot().first()
        val uiItem = snapshot.queueItems.first { it.localId == queueId }
        assertEquals(SyncErrorCategory.CONFLICT, uiItem.errorCategory)
        assertEquals("Konflik harga produk", uiItem.conflictTitle)
        assertTrue(uiItem.recommendation.contains("harga", ignoreCase = true))
    }

    @Test
    fun cashSessionOpenCloseAndExpense409OfflineTimeConflictsAreClassifiedAsConflict() = runBlocking {
        val openSessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-open-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = 100_000.0,
            )
        )
        val openQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = openSessionId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )

        val closedSessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-2",
                clientGeneratedId = "sess-close-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "CLOSED",
                openedAt = 1_795_000_000_000L,
                closedAt = 1_794_999_000_000L,
                startingCash = 100_000.0,
                actualCash = 150_000.0,
            )
        )
        val closeQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = closedSessionId,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
            )
        )

        val expenseSessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-3",
                clientGeneratedId = "sess-for-expense",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = 100_000.0,
            )
        )
        val expenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "exp-1",
                deviceId = "android-dev-1",
                cashSessionLocalId = expenseSessionId,
                cashSessionServerId = "srv-sess-3",
                description = "Beli plastik",
                amount = 15_000.0,
            )
        )
        val expenseQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expenseId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )

        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"OFFLINE_OPENED_AT_INVALID","message":"Umur sesi lewat 72 jam"}"""))
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"OFFLINE_CLOSED_AT_INVALID","message":"Waktu tutup lebih awal"}"""))
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"OFFLINE_OCCURRED_AT_INVALID","message":"Waktu pengeluaran invalid"}"""))

        val sessionSync = OfflineCashSessionSyncService(database, salesApi, "android-dev-1")
        val expenseSync = OfflineCashExpenseSyncService(database, salesApi, "android-dev-1", sessionSync)

        val openRes = sessionSync.syncOpenSession(openSessionId) as OfflineCashSessionSyncResult.Failed
        val closeRes = sessionSync.syncCloseSessionIfReady(closedSessionId) as OfflineCashSessionSyncResult.Failed
        val expRes = expenseSync.syncExpense(expenseId) as OfflineCashExpenseSyncResult.Failed

        assertEquals(OfflineCheckoutSyncFailureKind.CONFLICT, openRes.kind)
        assertEquals(OfflineCheckoutSyncFailureKind.CONFLICT, closeRes.kind)
        assertEquals(OfflineCheckoutSyncFailureKind.CONFLICT, expRes.kind)

        assertEquals("OFFLINE_OPENED_AT_INVALID", database.syncQueueDao().getById(openQueueId)!!.lastErrorCode)
        assertEquals("OFFLINE_CLOSED_AT_INVALID", database.syncQueueDao().getById(closeQueueId)!!.lastErrorCode)
        assertEquals("OFFLINE_OCCURRED_AT_INVALID", database.syncQueueDao().getById(expenseQueueId)!!.lastErrorCode)
    }
}
