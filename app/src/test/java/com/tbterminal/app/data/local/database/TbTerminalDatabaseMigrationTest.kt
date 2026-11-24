package com.tbterminal.app.data.local.database

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TbTerminalDatabaseMigrationTest {
    @Test
    fun migration4To5AddsLastErrorCodeColumnAndPreservesExistingQueueRows() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
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

            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(sync_queue)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    columns += cursor.getString(nameIndex)
                }
            }
            assertTrue(columns.contains("lastErrorCode"))

            db.query("SELECT id, entityLocalId, status, lastError, lastErrorCode FROM sync_queue WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42L, cursor.getLong(1))
                assertEquals("CONFLICT", cursor.getString(2))
                assertEquals("Konflik lama", cursor.getString(3))
                assertNull(cursor.getString(4))
            }

            db.execSQL("UPDATE sync_queue SET lastErrorCode = 'OFFLINE_PRICE_CONFLICT' WHERE id = 1")
            db.query("SELECT lastErrorCode FROM sync_queue WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("OFFLINE_PRICE_CONFLICT", cursor.getString(0))
            }
        }
    }

    @Test
    fun migration5To6AddsUnitColumnsAndPreservesExistingRows() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
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

            db.query(
                """
                SELECT productNameSnapshot, quantity, priceAtTransaction, subtotal,
                       unitServerId, baseQuantity, conversionFactor
                FROM local_transaction_items WHERE localId = 10
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Semen 50kg", cursor.getString(0))
                assertEquals(2.0, cursor.getDouble(1), 0.001)
                assertEquals(75000.0, cursor.getDouble(2), 0.001)
                assertEquals(150000.0, cursor.getDouble(3), 0.001)
                assertNull(cursor.getString(4))
                assertNull(cursor.getString(5))
                assertNull(cursor.getString(6))
            }

            db.query(
                """
                SELECT name, unitServerId, stock, unitName,
                       secondaryUnitServerId, secondaryUnitName, secondaryUnitFactor
                FROM local_products WHERE localId = 20
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Paku 5 cm", cursor.getString(0))
                assertEquals("unit-pcs", cursor.getString(1))
                assertEquals(50.0, cursor.getDouble(2), 0.001)
                assertNull(cursor.getString(3))
                assertNull(cursor.getString(4))
                assertNull(cursor.getString(5))
                assertNull(cursor.getString(6))
            }

            db.execSQL(
                """
                UPDATE local_transaction_items
                SET unitServerId = 'unit-dus', baseQuantity = '24', conversionFactor = '12'
                WHERE localId = 10
                """.trimIndent()
            )
            db.query("SELECT unitServerId, baseQuantity, conversionFactor FROM local_transaction_items WHERE localId = 10").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("unit-dus", cursor.getString(0))
                assertEquals("24", cursor.getString(1))
                assertEquals("12", cursor.getString(2))
            }

            db.execSQL(
                """
                UPDATE local_products
                SET unitName = 'Pcs', secondaryUnitServerId = 'unit-dus', secondaryUnitName = 'Dus', secondaryUnitFactor = '12'
                WHERE localId = 20
                """.trimIndent()
            )
            db.query("SELECT unitName, secondaryUnitServerId, secondaryUnitName, secondaryUnitFactor FROM local_products WHERE localId = 20").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Pcs", cursor.getString(0))
                assertEquals("unit-dus", cursor.getString(1))
                assertEquals("Dus", cursor.getString(2))
                assertEquals("12", cursor.getString(3))
            }
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
