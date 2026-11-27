package com.tbterminal.app.data.local.database

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.entity.CheckoutDraftEntity
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.sync.OfflineCashExpenseSyncResult
import com.tbterminal.app.data.sync.OfflineCashExpenseSyncService
import com.tbterminal.app.data.sync.OfflineCashSessionSyncResult
import com.tbterminal.app.data.sync.OfflineCashSessionSyncService
import com.tbterminal.app.data.sync.OfflineCheckoutSyncResult
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
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
class TbTerminalDatabaseMigrationTest {
    private lateinit var context: Context
    private val createdDbNames = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        createdDbNames.forEach { name -> context.deleteDatabase(name) }
    }

    @Test
    fun migration4To5AddsLastErrorCodeColumnAndPreservesExistingQueueRows() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS sync_queue (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    entityType TEXT NOT NULL,
                                    entityLocalId INTEGER NOT NULL,
                                    operation TEXT NOT NULL,
                                    payloadJson TEXT NOT NULL,
                                    status TEXT NOT NULL,
                                    retryCount INTEGER NOT NULL,
                                    lastError TEXT,
                                    createdAt INTEGER NOT NULL,
                                    updatedAt INTEGER NOT NULL
                                )
                                """.trimIndent()
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        helper.use {
            val db = it.writableDatabase
            db.execSQL(
                """
                INSERT INTO sync_queue (id, entityType, entityLocalId, operation, payloadJson, status, retryCount, lastError, createdAt, updatedAt)
                VALUES (1, 'TRANSACTION', 42, 'CREATE', '{}', 'CONFLICT', 2, 'Konflik lama', 1000, 2000)
                """.trimIndent()
            )

            TbTerminalDatabase.MIGRATION_4_5.migrate(db)

            val columns = tableColumns(db, "sync_queue")
            assertTrue(columns.contains("lastErrorCode"))

            db.query("SELECT id, entityLocalId, status, lastError, lastErrorCode FROM sync_queue WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42L, cursor.getLong(1))
                assertEquals("CONFLICT", cursor.getString(2))
                assertEquals("Konflik lama", cursor.getString(3))
                assertNull(cursor.getString(4))
            }
        }
    }

    @Test
    fun migration5To6AddsUnitColumnsAndPreservesExistingRows() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(5) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS local_transaction_items (
                                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    transactionLocalId INTEGER NOT NULL,
                                    transactionServerId TEXT,
                                    productLocalId INTEGER,
                                    productServerId TEXT,
                                    productNameSnapshot TEXT NOT NULL,
                                    skuSnapshot TEXT,
                                    unitNameSnapshot TEXT,
                                    quantity REAL NOT NULL,
                                    priceAtTransaction REAL NOT NULL,
                                    cogsAtTransaction REAL NOT NULL,
                                    discount REAL NOT NULL,
                                    subtotal REAL NOT NULL,
                                    note TEXT,
                                    syncStatus TEXT NOT NULL,
                                    createdAt INTEGER NOT NULL,
                                    updatedAt INTEGER NOT NULL,
                                    syncedAt INTEGER,
                                    deletedAt INTEGER
                                )
                                """.trimIndent()
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS local_products (
                                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    serverId TEXT,
                                    clientGeneratedId TEXT NOT NULL,
                                    deviceId TEXT,
                                    name TEXT NOT NULL,
                                    sku TEXT,
                                    barcode TEXT,
                                    categoryLocalId INTEGER,
                                    categoryServerId TEXT,
                                    unitLocalId INTEGER,
                                    unitServerId TEXT,
                                    priceSell REAL NOT NULL,
                                    priceBuy REAL NOT NULL,
                                    priceRetail REAL NOT NULL DEFAULT 0.0,
                                    priceContractor REAL NOT NULL DEFAULT 0.0,
                                    discount REAL NOT NULL DEFAULT 0.0,
                                    stock REAL NOT NULL,
                                    minimumStock REAL NOT NULL,
                                    photoFilename TEXT,
                                    isActive INTEGER NOT NULL,
                                    syncStatus TEXT NOT NULL,
                                    remoteCreatedAt TEXT,
                                    remoteUpdatedAt TEXT,
                                    createdAt INTEGER NOT NULL,
                                    updatedAt INTEGER NOT NULL,
                                    deletedAt INTEGER
                                )
                                """.trimIndent()
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        helper.use {
            val db = it.writableDatabase
            db.execSQL(
                """
                INSERT INTO local_transaction_items (
                    localId, transactionLocalId, productServerId, productNameSnapshot,
                    unitNameSnapshot, quantity, priceAtTransaction, cogsAtTransaction,
                    discount, subtotal, syncStatus, createdAt, updatedAt
                )
                VALUES (
                    10, 100, 'prod-1', 'Semen 50kg', 'Sak', 2.0, 75000.0, 60000.0,
                    0.0, 150000.0, 'PENDING', 1000, 2000
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO local_products (
                    localId, serverId, clientGeneratedId, name, sku, unitServerId,
                    priceSell, priceBuy, priceRetail, priceContractor, discount,
                    stock, minimumStock, isActive, syncStatus, createdAt, updatedAt
                )
                VALUES (
                    20, 'prod-1', 'server-product-prod-1', 'Paku 5 cm', 'PK-05', 'unit-pcs',
                    10000.0, 8000.0, 10000.0, 9500.0, 0.0,
                    50.0, 5.0, 1, 'SYNCED', 1000, 2000
                )
                """.trimIndent()
            )

            TbTerminalDatabase.MIGRATION_5_6.migrate(db)

            val itemColumns = tableColumns(db, "local_transaction_items")
            assertTrue(itemColumns.contains("unitServerId"))
            assertTrue(itemColumns.contains("baseQuantity"))
            assertTrue(itemColumns.contains("conversionFactor"))

            val productColumns = tableColumns(db, "local_products")
            assertTrue(productColumns.contains("unitName"))
            assertTrue(productColumns.contains("secondaryUnitServerId"))
            assertTrue(productColumns.contains("secondaryUnitName"))
            assertTrue(productColumns.contains("secondaryUnitFactor"))
        }
    }

    @Test
    fun migration4To7_migratesRealMoneyAndQuantitiesWithHalfUpAndPreservesPendingSyncQueue() = runBlocking {
        val dbName = newDbName("mig_4_to_7.db")
        createLegacyDatabase(dbName, version = 4) { db ->
            seedLegacyBusinessData(db, version = 4)
        }

        val migratedDb = openRoomDatabaseV8(dbName)
        try {
            assertMigratedDataAndSchemaIntegrity(migratedDb, hasUnitColumns = false)
            assertPendingRowsSyncCleanly(migratedDb, expectSecondaryUnitPair = false)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migration5To7_migratesRealMoneyAndQuantitiesWithHalfUpAndPreservesPendingSyncQueue() = runBlocking {
        val dbName = newDbName("mig_5_to_7.db")
        createLegacyDatabase(dbName, version = 5) { db ->
            seedLegacyBusinessData(db, version = 5)
        }

        val migratedDb = openRoomDatabaseV8(dbName)
        try {
            assertMigratedDataAndSchemaIntegrity(migratedDb, hasUnitColumns = false)
            assertPendingRowsSyncCleanly(migratedDb, expectSecondaryUnitPair = false)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migration6To7_migratesRealMoneyAndQuantitiesWithHalfUpAndPreservesPendingSyncQueue() = runBlocking {
        val dbName = newDbName("mig_6_to_7.db")
        createLegacyDatabase(dbName, version = 6) { db ->
            seedLegacyBusinessData(db, version = 6)
        }

        val migratedDb = openRoomDatabaseV8(dbName)
        try {
            assertMigratedDataAndSchemaIntegrity(migratedDb, hasUnitColumns = true)
            assertPendingRowsSyncCleanly(migratedDb, expectSecondaryUnitPair = true)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migration6To8_migratesMoneyPreservesSyncQueueAndCreatesStateSurvivalTables() = runBlocking {
        val dbName = newDbName("mig_6_to_8.db")
        createLegacyDatabase(dbName, version = 6) { db ->
            seedLegacyBusinessData(db, version = 6)
        }

        val migratedDb = openRoomDatabaseV8(dbName)
        try {
            assertMigratedDataAndSchemaIntegrity(migratedDb, hasUnitColumns = true)
            assertV8TablesAndIndicesReady(migratedDb)
            assertPendingRowsSyncCleanly(migratedDb, expectSecondaryUnitPair = true)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migration7To8_preservesExistingV7DataAndCreatesPendingSubmissionsAndCheckoutDraftsTables() = runBlocking {
        val dbName = newDbName("mig_7_to_8.db")
        createLegacyV7Database(dbName)

        val migratedDb = openRoomDatabaseV8(dbName)
        try {
            assertMigratedDataAndSchemaIntegrity(migratedDb, hasUnitColumns = true)
            assertV8TablesAndIndicesReady(migratedDb)
            assertPendingRowsSyncCleanly(migratedDb, expectSecondaryUnitPair = true)
        } finally {
            migratedDb.close()
        }
    }

    private fun newDbName(name: String): String {
        context.deleteDatabase(name)
        createdDbNames += name
        return name
    }

    private fun openRoomDatabaseV8(dbName: String): TbTerminalDatabase {
        return Room.databaseBuilder(context, TbTerminalDatabase::class.java, dbName)
            .addMigrations(
                TbTerminalDatabase.MIGRATION_4_5,
                TbTerminalDatabase.MIGRATION_5_6,
                TbTerminalDatabase.MIGRATION_6_7,
                TbTerminalDatabase.MIGRATION_7_8
            )
            .allowMainThreadQueries()
            .build()
    }

    private suspend fun assertV8TablesAndIndicesReady(db: TbTerminalDatabase) {
        val sqlite = db.openHelper.writableDatabase
        assertIndicesExist(
            sqlite,
            "pending_submissions",
            listOf(
                "index_pending_submissions_createdAt",
                "index_pending_submissions_updatedAt"
            )
        )
        assertIndicesExist(
            sqlite,
            "checkout_drafts",
            listOf(
                "index_checkout_drafts_updatedAt"
            )
        )

        db.pendingSubmissionDao().upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-1",
                scope = "checkout",
                idempotencyKey = "key-mig-1",
                intentJson = """{"fp":"abc"}""",
                payloadJson = """{"cmd":1}""",
                status = "AMBIGUOUS",
                schemaVersion = 1,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
        val loadedPending = db.pendingSubmissionDao().get("kasir-1", "checkout")
        assertEquals("key-mig-1", loadedPending?.idempotencyKey)

        db.checkoutDraftDao().upsert(
            CheckoutDraftEntity(
                ownerUserId = "kasir-1",
                activeCartJson = """{"items":[]}""",
                heldCartsJson = """{"carts":[]}""",
                schemaVersion = 1,
                updatedAt = 2000L
            )
        )
        val loadedDraft = db.checkoutDraftDao().get("kasir-1")
        assertEquals("""{"items":[]}""", loadedDraft?.activeCartJson)
    }

    private fun createLegacyV7Database(dbName: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(7) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createAllLegacyTables(db, version = 6)
                            seedLegacyBusinessData(db, version = 6)
                            RoomMigration6To7.migrate(db)
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )
        helper.use { it.writableDatabase }
    }

    private suspend fun assertMigratedDataAndSchemaIntegrity(
        db: TbTerminalDatabase,
        hasUnitColumns: Boolean
    ) {
        val sqlite = db.openHelper.writableDatabase

        // Room validates schema on first query; verify foreign_key_check is empty.
        sqlite.query("PRAGMA foreign_key_check").use { fkCursor ->
            assertFalse("PRAGMA foreign_key_check must return no violations", fkCursor.moveToFirst())
        }

        // Verify all expected indices exist after table recreation.
        assertIndicesExist(
            sqlite,
            "local_transactions",
            listOf(
                "index_local_transactions_serverId",
                "index_local_transactions_clientGeneratedId",
                "index_local_transactions_transactionCode",
                "index_local_transactions_customerLocalId",
                "index_local_transactions_customerServerId",
                "index_local_transactions_cashSessionLocalId",
                "index_local_transactions_cashSessionServerId",
                "index_local_transactions_status",
                "index_local_transactions_occurredAt",
                "index_local_transactions_syncStatus"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_transaction_items",
            listOf(
                "index_local_transaction_items_transactionLocalId",
                "index_local_transaction_items_productLocalId",
                "index_local_transaction_items_productServerId",
                "index_local_transaction_items_syncStatus"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_payments",
            listOf(
                "index_local_payments_serverId",
                "index_local_payments_clientGeneratedId",
                "index_local_payments_transactionLocalId",
                "index_local_payments_transactionServerId",
                "index_local_payments_method",
                "index_local_payments_paidAt",
                "index_local_payments_syncStatus"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_receivables",
            listOf(
                "index_local_receivables_serverId",
                "index_local_receivables_clientGeneratedId",
                "index_local_receivables_transactionLocalId",
                "index_local_receivables_transactionServerId",
                "index_local_receivables_customerLocalId",
                "index_local_receivables_customerServerId",
                "index_local_receivables_status",
                "index_local_receivables_dueDate",
                "index_local_receivables_syncStatus"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_cash_sessions",
            listOf(
                "index_local_cash_sessions_serverId",
                "index_local_cash_sessions_clientGeneratedId",
                "index_local_cash_sessions_cashierUserId",
                "index_local_cash_sessions_status",
                "index_local_cash_sessions_openedAt",
                "index_local_cash_sessions_closedAt",
                "index_local_cash_sessions_syncStatus"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_cash_expenses",
            listOf(
                "index_local_cash_expenses_serverId",
                "index_local_cash_expenses_clientGeneratedId",
                "index_local_cash_expenses_cashSessionLocalId",
                "index_local_cash_expenses_cashSessionServerId",
                "index_local_cash_expenses_category",
                "index_local_cash_expenses_occurredAt",
                "index_local_cash_expenses_syncStatus"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_products",
            listOf(
                "index_local_products_serverId",
                "index_local_products_clientGeneratedId",
                "index_local_products_categoryLocalId",
                "index_local_products_unitLocalId"
            )
        )
        assertIndicesExist(
            sqlite,
            "local_customers",
            listOf(
                "index_local_customers_serverId",
                "index_local_customers_clientGeneratedId",
                "index_local_customers_phone"
            )
        )

        // Verify HALF_UP rounding on fractional / float-drift REAL values
        val tx = db.transactionDao().getByLocalId(100L)!!
        assertEquals(ScaledDecimal2.ofMinorUnits(2000000L), tx.subtotal)       // 19999.999999 -> 20000.00
        assertEquals(ScaledDecimal2.ofMinorUnits(101L), tx.discount)           // 1.005 -> 1.01
        assertEquals(ScaledDecimal2.ofMinorUnits(1100L), tx.taxRate)           // 11.0 -> 11.00
        assertEquals(ScaledDecimal2.ofMinorUnits(2000030L), tx.taxableBase)    // 20000.300000000004 -> 20000.30
        assertEquals(ScaledDecimal2.ofMinorUnits(220003L), tx.taxAmount)       // 2200.033 -> 2200.03
        assertEquals(ScaledDecimal2.ofMinorUnits(-50001L), tx.roundingAmount)  // -500.005 -> -500.01
        assertEquals(ScaledDecimal2.ofMinorUnits(2000000L), tx.total)          // 19999.999999 -> 20000.00
        assertEquals(ScaledDecimal2.ofMinorUnits(1500000L), tx.paidAmount)     // 15000.00
        assertEquals(ScaledDecimal2.ofMinorUnits(500000L), tx.remainingAmount) // 5000.00
        assertEquals(SyncStatus.PENDING, tx.syncStatus)

        val item = db.transactionItemDao().getByTransaction(100L).single()
        assertEquals(ScaledDecimal2.ofMinorUnits(250L), item.quantity)              // 2.5 -> 2.50
        assertEquals(ScaledDecimal2.ofMinorUnits(800000L), item.priceAtTransaction) // 8000.00
        assertEquals(ScaledDecimal2.ofMinorUnits(640000L), item.cogsAtTransaction)  // 6400.00
        assertEquals(ScaledDecimal2.ofMinorUnits(2000000L), item.subtotal)          // 19999.999999 -> 20000.00
        if (hasUnitColumns) {
            assertEquals("unit-dus", item.unitServerId)
            assertEquals(0, BigDecimal("7.5").compareTo(item.baseQuantity))
            assertEquals(0, BigDecimal("3").compareTo(item.conversionFactor))
        } else {
            assertNull(item.unitServerId)
            assertNull(item.baseQuantity)
            assertNull(item.conversionFactor)
        }

        val payment = db.paymentDao().getByTransaction(100L).single()
        assertEquals(ScaledDecimal2.ofMinorUnits(1500000L), payment.amount)

        val receivable = db.receivableDao().getByTransaction(100L).single()
        assertEquals(ScaledDecimal2.ofMinorUnits(2000000L), receivable.totalAmount)
        assertEquals(ScaledDecimal2.ofMinorUnits(1500000L), receivable.paidAmount)
        assertEquals(ScaledDecimal2.ofMinorUnits(500000L), receivable.remainingAmount)

        val openSession = db.cashSessionDao().getByLocalId(10L)!!
        assertEquals(ScaledDecimal2.ofMinorUnits(10000030L), openSession.startingCash) // 100000.30000000004
        assertNull(openSession.expectedCash)
        assertNull(openSession.actualCash)
        assertNull(openSession.difference)
        assertEquals(SyncStatus.PENDING, openSession.syncStatus)

        val expense = db.cashExpenseDao().getByLocalId(50L)!!
        assertEquals(ScaledDecimal2.ofMinorUnits(1250055L), expense.amount) // 12500.554 -> 12500.55
        assertEquals(SyncStatus.PENDING, expense.syncStatus)

        val product = db.productDao().getByLocalId(20L)!!
        assertEquals(ScaledDecimal2.ofMinorUnits(1000000L), product.priceSell)
        assertEquals(ScaledDecimal2.ofMinorUnits(800000L), product.priceBuy)
        assertEquals(ScaledDecimal2.ofMinorUnits(1250L), product.stock) // 12.50
        assertEquals(ScaledDecimal2.ofMinorUnits(500L), product.minimumStock)
        if (hasUnitColumns) {
            assertEquals(0, BigDecimal("2.5").compareTo(product.secondaryUnitFactor))
        }

        val customer = db.customerDao().getByLocalId(30L)!!
        assertEquals(ScaledDecimal2.ofMinorUnits(500000000L), customer.creditLimit)

        // Verify PENDING sync_queue entries preserved
        val pendingQueue = db.syncQueueDao().getByStatus(SyncStatus.PENDING, 20)
        assertEquals(3, pendingQueue.size)
    }

    private suspend fun assertPendingRowsSyncCleanly(
        db: TbTerminalDatabase,
        expectSecondaryUnitPair: Boolean
    ) {
        val server = MockWebServer()
        server.start()
        try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            val salesApi = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(SalesApi::class.java)

            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-sess-mig","openedAt":"2026-11-26T08:00:00+07:00","syncedAt":"2026-11-26T08:00:05+07:00"}}"""
                )
            )
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-mig","syncedAt":"2026-11-26T08:05:00+07:00"}}"""
                )
            )
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"success":true,"data":{"syncStatus":"CREATED","serverTransactionId":"srv-tx-mig","receiptId":"RCP-MIG","serverPaymentIds":["srv-pay-mig"],"serverReceivableId":"srv-rcv-mig","syncedAt":"2026-11-26T08:10:00+07:00"}}"""
                )
            )

            val sessionSync = OfflineCashSessionSyncService(db, salesApi, "android-mig-1")
            val expenseSync = OfflineCashExpenseSyncService(db, salesApi, "android-mig-1", sessionSync)
            val checkoutSync = OfflineCheckoutSyncService(db, salesApi, "android-mig-1", sessionSync, expenseSync)

            val sessRes = sessionSync.syncOpenSession(10L)
            val expRes = expenseSync.syncExpense(50L)
            val txRes = checkoutSync.syncOne(100L)

            assertTrue(sessRes is OfflineCashSessionSyncResult.Success)
            assertTrue(expRes is OfflineCashExpenseSyncResult.Success)
            assertTrue(txRes is OfflineCheckoutSyncResult.Success)

            val sessBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals("100000.30", sessBody["startingCash"]?.jsonPrimitive?.content)

            val expBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals("12500.55", expBody["amount"]?.jsonPrimitive?.content)

            val txBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals("20000.00", txBody["total"]?.jsonPrimitive?.content)
            assertEquals("15000.00", txBody["paidAmount"]?.jsonPrimitive?.content)
            assertEquals("5000.00", txBody["remainingAmount"]?.jsonPrimitive?.content)
            assertEquals("-500.01", txBody["roundingAmount"]?.jsonPrimitive?.content)
            val itemJson = txBody["items"]!!.jsonArray.single().jsonObject
            assertEquals("2.50", itemJson["quantity"]?.jsonPrimitive?.content)
            assertEquals("20000.00", itemJson["subtotal"]?.jsonPrimitive?.content)
            if (expectSecondaryUnitPair) {
                assertEquals("unit-dus", itemJson["unitId"]?.jsonPrimitive?.content)
                assertEquals("7.5", itemJson["baseQuantity"]?.jsonPrimitive?.content)
            } else {
                assertFalse(itemJson.containsKey("unitId"))
                assertFalse(itemJson.containsKey("baseQuantity"))
            }

            assertEquals(SyncStatus.SYNCED, db.cashSessionDao().getByLocalId(10L)!!.syncStatus)
            assertEquals(SyncStatus.SYNCED, db.cashExpenseDao().getByLocalId(50L)!!.syncStatus)
            assertEquals(SyncStatus.SYNCED, db.transactionDao().getByLocalId(100L)!!.syncStatus)
            assertTrue(db.syncQueueDao().getByStatus(SyncStatus.PENDING, 20).isEmpty())
        } finally {
            server.shutdown()
        }
    }

    private fun createLegacyDatabase(
        dbName: String,
        version: Int,
        seed: (SupportSQLiteDatabase) -> Unit
    ) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createAllLegacyTables(db, version)
                            seed(db)
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )
        helper.use { it.writableDatabase }
    }

    private fun createAllLegacyTables(db: SupportSQLiteDatabase, version: Int) {
        val productUnitCols = if (version >= 6) {
            """
            unitName TEXT,
            secondaryUnitServerId TEXT,
            secondaryUnitName TEXT,
            secondaryUnitFactor TEXT,
            """.trimIndent()
        } else ""

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_products (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                name TEXT NOT NULL,
                sku TEXT,
                barcode TEXT,
                categoryLocalId INTEGER,
                categoryServerId TEXT,
                unitLocalId INTEGER,
                unitServerId TEXT,
                $productUnitCols
                priceSell REAL NOT NULL,
                priceBuy REAL NOT NULL,
                priceRetail REAL NOT NULL DEFAULT 0.0,
                priceContractor REAL NOT NULL DEFAULT 0.0,
                discount REAL NOT NULL DEFAULT 0.0,
                stock REAL NOT NULL,
                minimumStock REAL NOT NULL,
                photoFilename TEXT,
                isActive INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                remoteCreatedAt TEXT,
                remoteUpdatedAt TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_categories (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                name TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                remoteCreatedAt TEXT,
                remoteUpdatedAt TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_local_categories_serverId ON local_categories (serverId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_categories_clientGeneratedId ON local_categories (clientGeneratedId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_units (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                name TEXT NOT NULL,
                symbol TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                remoteCreatedAt TEXT,
                remoteUpdatedAt TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_local_units_serverId ON local_units (serverId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_units_clientGeneratedId ON local_units (clientGeneratedId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_customers (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                name TEXT NOT NULL,
                phone TEXT,
                address TEXT,
                isContractor INTEGER NOT NULL DEFAULT 0,
                creditAllowed INTEGER NOT NULL,
                creditLimit REAL NOT NULL,
                paymentTermDays INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                remoteCreatedAt TEXT,
                remoteUpdatedAt TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_transactions (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                transactionCode TEXT NOT NULL,
                customerLocalId INTEGER,
                customerServerId TEXT,
                cashSessionLocalId INTEGER,
                cashSessionServerId TEXT,
                cashierUserId TEXT,
                status TEXT NOT NULL,
                subtotal REAL NOT NULL,
                discount REAL NOT NULL,
                taxEnabled INTEGER NOT NULL DEFAULT 0,
                taxMode TEXT,
                taxRate REAL NOT NULL DEFAULT 0.0,
                taxLabel TEXT,
                taxableBase REAL NOT NULL DEFAULT 0.0,
                taxAmount REAL NOT NULL DEFAULT 0.0,
                roundingAmount REAL NOT NULL DEFAULT 0.0,
                total REAL NOT NULL,
                paidAmount REAL NOT NULL,
                remainingAmount REAL NOT NULL,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                occurredAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        val itemUnitCols = if (version >= 6) {
            """
            unitServerId TEXT,
            baseQuantity TEXT,
            conversionFactor TEXT,
            """.trimIndent()
        } else ""

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_transaction_items (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionLocalId INTEGER NOT NULL,
                transactionServerId TEXT,
                productLocalId INTEGER,
                productServerId TEXT,
                productNameSnapshot TEXT NOT NULL,
                skuSnapshot TEXT,
                unitNameSnapshot TEXT,
                $itemUnitCols
                quantity REAL NOT NULL,
                priceAtTransaction REAL NOT NULL,
                cogsAtTransaction REAL NOT NULL,
                discount REAL NOT NULL,
                subtotal REAL NOT NULL,
                note TEXT,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_payments (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                transactionLocalId INTEGER NOT NULL,
                transactionServerId TEXT,
                method TEXT NOT NULL,
                amount REAL NOT NULL,
                reference TEXT,
                paidAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_receivables (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                transactionLocalId INTEGER NOT NULL,
                transactionServerId TEXT,
                customerLocalId INTEGER,
                customerServerId TEXT,
                totalAmount REAL NOT NULL,
                paidAmount REAL NOT NULL,
                remainingAmount REAL NOT NULL,
                status TEXT NOT NULL,
                dueDate INTEGER,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                occurredAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_cash_sessions (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                cashierUserId TEXT NOT NULL,
                status TEXT NOT NULL,
                openedAt INTEGER NOT NULL,
                closedAt INTEGER,
                startingCash REAL NOT NULL,
                expectedCash REAL,
                actualCash REAL,
                difference REAL,
                openingNote TEXT,
                closingNote TEXT,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_cash_expenses (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                cashSessionLocalId INTEGER,
                cashSessionServerId TEXT,
                category TEXT,
                description TEXT,
                amount REAL NOT NULL,
                occurredAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_audit_logs (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT,
                clientGeneratedId TEXT NOT NULL,
                deviceId TEXT,
                actorUserId TEXT,
                action TEXT NOT NULL,
                tableName TEXT NOT NULL,
                recordId TEXT,
                description TEXT,
                metadataJson TEXT,
                occurredAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncedAt INTEGER,
                deletedAt INTEGER
            )
            """.trimIndent()
        )
        listOf("serverId", "actorUserId", "action", "tableName", "recordId", "occurredAt", "syncStatus").forEach {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_local_audit_logs_$it ON local_audit_logs ($it)")
        }
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_audit_logs_clientGeneratedId ON local_audit_logs (clientGeneratedId)")

        val queueErrorCodeCol = if (version >= 5) "lastErrorCode TEXT," else ""
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityType TEXT NOT NULL,
                entityLocalId INTEGER NOT NULL,
                operation TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                status TEXT NOT NULL,
                retryCount INTEGER NOT NULL,
                lastError TEXT,
                $queueErrorCodeCol
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType ON sync_queue (entityType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue (createdAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun seedLegacyBusinessData(db: SupportSQLiteDatabase, version: Int) {
        val productExtraCols = if (version >= 6) ", unitName, secondaryUnitServerId, secondaryUnitName, secondaryUnitFactor" else ""
        val productExtraVals = if (version >= 6) ", 'Pcs', 'unit-dus', 'Dus', '2.5'" else ""
        db.execSQL(
            """
            INSERT INTO local_products (
                localId, serverId, clientGeneratedId, name, sku, unitServerId$productExtraCols,
                priceSell, priceBuy, priceRetail, priceContractor, discount,
                stock, minimumStock, isActive, syncStatus, createdAt, updatedAt
            )
            VALUES (
                20, 'prod-1', 'server-product-prod-1', 'Paku 5 cm', 'PK-05', 'unit-pcs'$productExtraVals,
                10000.0, 8000.0, 10000.0, 9500.0, 0.0,
                12.5, 5.0, 1, 'SYNCED', 1000, 2000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO local_customers (
                localId, serverId, clientGeneratedId, name, phone,
                isContractor, creditAllowed, creditLimit, paymentTermDays,
                isActive, syncStatus, createdAt, updatedAt
            )
            VALUES (
                30, 'cust-1', 'server-customer-cust-1', 'Pak Budi', '08123',
                0, 1, 5000000.0, 30,
                1, 'SYNCED', 1000, 2000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO local_cash_sessions (
                localId, serverId, clientGeneratedId, deviceId, cashierUserId, status,
                openedAt, startingCash, expectedCash, actualCash, difference,
                syncStatus, createdAt, updatedAt
            )
            VALUES (
                10, NULL, 'SESSION-MIG-1', 'android-mig-1', 'kasir-1', 'OPEN',
                1795740000000, 100000.30000000004, NULL, NULL, NULL,
                'PENDING', 1000, 2000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO local_cash_expenses (
                localId, clientGeneratedId, deviceId, cashSessionLocalId, cashSessionServerId,
                category, description, amount, occurredAt, syncStatus, createdAt, updatedAt
            )
            VALUES (
                50, 'EXP-MIG-1', 'android-mig-1', 10, NULL,
                'CASH_OUT: OPERASIONAL', 'Beli plastik', 12500.554, 1795740300000, 'PENDING', 1000, 2000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO local_transactions (
                localId, clientGeneratedId, deviceId, transactionCode, customerLocalId, customerServerId,
                cashSessionLocalId, cashSessionServerId, cashierUserId, status,
                subtotal, discount, taxEnabled, taxMode, taxRate, taxLabel,
                taxableBase, taxAmount, roundingAmount, total, paidAmount, remainingAmount,
                syncStatus, createdAt, updatedAt, occurredAt
            )
            VALUES (
                100, 'TRX-MIG-1', 'android-mig-1', 'TRX-20261126-MIG1', 30, 'cust-1',
                10, NULL, 'kasir-1', 'UNPAID',
                19999.999999, 1.005, 1, 'EXCLUDE', 11.0, 'PPN',
                20000.300000000004, 2200.033, -500.005, 19999.999999, 15000.0, 5000.0,
                'PENDING', 1000, 2000, 1795740600000
            )
            """.trimIndent()
        )

        val itemExtraCols = if (version >= 6) ", unitServerId, baseQuantity, conversionFactor" else ""
        val itemExtraVals = if (version >= 6) ", 'unit-dus', '7.5', '3'" else ""
        db.execSQL(
            """
            INSERT INTO local_transaction_items (
                localId, transactionLocalId, productLocalId, productServerId,
                productNameSnapshot, unitNameSnapshot$itemExtraCols,
                quantity, priceAtTransaction, cogsAtTransaction, discount, subtotal,
                syncStatus, createdAt, updatedAt
            )
            VALUES (
                101, 100, 20, 'prod-1',
                'Paku 5 cm', 'Dus'$itemExtraVals,
                2.5, 8000.0, 6400.0, 0.0, 19999.999999,
                'PENDING', 1000, 2000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO local_payments (
                localId, clientGeneratedId, deviceId, transactionLocalId, method, amount,
                paidAt, syncStatus, createdAt, updatedAt
            )
            VALUES (
                201, 'PAY-MIG-1', 'android-mig-1', 100, 'dp', 15000.0,
                1795740600000, 'PENDING', 1000, 2000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO local_receivables (
                localId, clientGeneratedId, deviceId, transactionLocalId, customerLocalId, customerServerId,
                totalAmount, paidAmount, remainingAmount, status,
                syncStatus, createdAt, updatedAt, occurredAt
            )
            VALUES (
                301, 'RCV-MIG-1', 'android-mig-1', 100, 30, 'cust-1',
                19999.999999, 15000.0, 5000.0, 'UNPAID',
                'PENDING', 1000, 2000, 1795740600000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO sync_queue (id, entityType, entityLocalId, operation, payloadJson, status, retryCount, createdAt, updatedAt)
            VALUES
                (1, 'CASH_SESSION', 10, 'CREATE', '{}', 'PENDING', 0, 1000, 1000),
                (2, 'CASH_EXPENSE', 50, 'CREATE', '{}', 'PENDING', 0, 1100, 1100),
                (3, 'TRANSACTION', 100, 'CREATE', '{}', 'PENDING', 0, 1200, 1200)
            """.trimIndent()
        )
    }

    private fun assertIndicesExist(
        db: SupportSQLiteDatabase,
        tableName: String,
        expectedIndexNames: List<String>
    ) {
        val actual = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = '$tableName'").use { cursor ->
            while (cursor.moveToNext()) {
                actual += cursor.getString(0)
            }
        }
        expectedIndexNames.forEach { expected ->
            assertTrue("Missing index $expected on $tableName (actual=$actual)", expected in actual)
        }
    }

    private fun tableColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        return columns
    }
}
