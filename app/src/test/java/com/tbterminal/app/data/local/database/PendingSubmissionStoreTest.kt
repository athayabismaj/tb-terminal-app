package com.tbterminal.app.data.local.database

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteFullException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.dao.PendingSubmissionDao
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import kotlinx.coroutines.test.runTest
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
class PendingSubmissionStoreTest {
    private lateinit var database: TbTerminalDatabase
    private var currentOwner = "kasir-1"
    private var nowMillis = 1_000_000L
    private lateinit var store: RoomPendingSubmissionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomPendingSubmissionStore(
            dao = database.pendingSubmissionDao(),
            ownerUserIdProvider = { currentOwner },
            clockMillis = { nowMillis }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveLoadClear_roundTripsAndIsolatesByOwnerUserId() = runTest {
        val record = PendingSubmissionRecord(
            scope = PendingSubmissionScopes.CHECKOUT,
            idempotencyKey = "idem-key-1",
            intentJson = """{"fp":"v1"}""",
            payloadJson = """{"cmd":"x"}""",
            status = PendingSubmissionStatus.AMBIGUOUS,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )
        store.save(record)

        val loadedForKasir1 = store.load(PendingSubmissionScopes.CHECKOUT)
        assertNotNull(loadedForKasir1)
        assertEquals("idem-key-1", loadedForKasir1!!.idempotencyKey)
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, loadedForKasir1.status)

        currentOwner = "kasir-2"
        assertNull(store.load(PendingSubmissionScopes.CHECKOUT))

        currentOwner = "kasir-1"
        store.clear(PendingSubmissionScopes.CHECKOUT)
        assertNull(store.load(PendingSubmissionScopes.CHECKOUT))
    }

    @Test
    fun load_expiresAfter72HoursAndDeletesRow() = runTest {
        val created = 10_000L
        nowMillis = created
        store.save(
            PendingSubmissionRecord(
                scope = PendingSubmissionScopes.PURCHASE,
                idempotencyKey = "pur-1",
                intentJson = """{"s":"1"}""",
                status = PendingSubmissionStatus.AMBIGUOUS,
                createdAt = created,
                updatedAt = created
            )
        )

        // Just before 72h -> still valid
        nowMillis = created + RoomPendingSubmissionStore.DEFAULT_TTL_MILLIS
        assertNotNull(store.load(PendingSubmissionScopes.PURCHASE))

        // Past 72h -> expired, returns null and deletes row
        nowMillis = created + RoomPendingSubmissionStore.DEFAULT_TTL_MILLIS + 1L
        assertNull(store.load(PendingSubmissionScopes.PURCHASE))
        assertNull(database.pendingSubmissionDao().get("kasir-1", PendingSubmissionScopes.PURCHASE))
    }

    @Test
    fun load_deletesCorruptSchemaVersionOrBlankFieldsWithoutCrashing() = runTest {
        val dao = database.pendingSubmissionDao()
        dao.upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-1",
                scope = PendingSubmissionScopes.CHECKOUT,
                idempotencyKey = "key-bad-schema",
                intentJson = "{}",
                payloadJson = null,
                status = "AMBIGUOUS",
                schemaVersion = 99,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
        assertNull(store.load(PendingSubmissionScopes.CHECKOUT))
        assertNull(dao.get("kasir-1", PendingSubmissionScopes.CHECKOUT))

        dao.upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-1",
                scope = PendingSubmissionScopes.CASHIER_PAY_DEBT,
                idempotencyKey = "",
                intentJson = "{}",
                payloadJson = null,
                status = "UNKNOWN_STATUS",
                schemaVersion = 1,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
        assertNull(store.load(PendingSubmissionScopes.CASHIER_PAY_DEBT))
        assertNull(dao.get("kasir-1", PendingSubmissionScopes.CASHIER_PAY_DEBT))
    }

    @Test
    fun loadByScopePrefix_returnsOnlyMatchingOwnerValidRecords() = runTest {
        store.save(
            PendingSubmissionRecord(
                scope = PendingSubmissionScopes.supplierPayment("pay-1"),
                idempotencyKey = "k-1",
                intentJson = """{"p":"1"}""",
                status = PendingSubmissionStatus.AMBIGUOUS,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
        store.save(
            PendingSubmissionRecord(
                scope = PendingSubmissionScopes.supplierPayment("pay-2"),
                idempotencyKey = "k-2",
                intentJson = """{"p":"2"}""",
                status = PendingSubmissionStatus.IN_FLIGHT,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
        store.save(
            PendingSubmissionRecord(
                scope = PendingSubmissionScopes.PURCHASE,
                idempotencyKey = "k-pur",
                intentJson = """{"p":"3"}""",
                status = PendingSubmissionStatus.AMBIGUOUS,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )

        val records = store.loadByScopePrefix(PendingSubmissionScopes.SUPPLIER_PAYMENT_PREFIX)
        assertEquals(2, records.size)
        assertEquals(setOf("k-1", "k-2"), records.map { it.idempotencyKey }.toSet())
    }

    @Test
    fun save_returnsFalseWhenDaoThrowsSqliteFullException() = runTest {
        val delegate = database.pendingSubmissionDao()
        val fullDiskDao = object : PendingSubmissionDao by delegate {
            override suspend fun upsert(entity: PendingSubmissionEntity) {
                throw SQLiteFullException("database or disk is full (code 13)")
            }
        }
        val failingStore = RoomPendingSubmissionStore(
            dao = fullDiskDao,
            ownerUserIdProvider = { currentOwner },
            clockMillis = { nowMillis },
        )

        val saved = failingStore.save(
            PendingSubmissionRecord(
                scope = PendingSubmissionScopes.CHECKOUT,
                idempotencyKey = "k-full",
                intentJson = """{"fp":"v1"}""",
                status = PendingSubmissionStatus.IN_FLIGHT,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            )
        )
        assertFalse(saved)
        assertTrue(
            store.save(
                PendingSubmissionRecord(
                    scope = PendingSubmissionScopes.CHECKOUT,
                    idempotencyKey = "k-ok",
                    intentJson = """{"fp":"v1"}""",
                    status = PendingSubmissionStatus.IN_FLIGHT,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                )
            )
        )
    }
}
