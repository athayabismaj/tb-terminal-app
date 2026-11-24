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
}
