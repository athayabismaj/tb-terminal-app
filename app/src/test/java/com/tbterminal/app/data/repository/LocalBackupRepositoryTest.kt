package com.tbterminal.app.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.id.DeviceIdProvider
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
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
class LocalBackupRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: TbTerminalDatabase
    private lateinit var repository: LocalBackupRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalBackupRepository(
            context = context,
            database = database,
            deviceIdProvider = DeviceIdProvider(context)
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.getDatabasePath(TbTerminalDatabase.DATABASE_NAME).delete()
        context.deleteDatabase("backup_source_v6.db")
    }

    @Test
    fun createBackup_usesRoomDatabaseVersionEightInMetadata() = runTest {
        assertEquals(9, TbTerminalDatabase.DATABASE_VERSION)
        val dbFile = context.getDatabasePath(TbTerminalDatabase.DATABASE_NAME).apply {
            parentFile?.mkdirs()
            writeText("sqlite-placeholder")
        }
        assertTrue(dbFile.exists())

        val outFile = File(context.cacheDir, "backup-v9.zip").apply { delete() }
        val result = repository.createBackup(Uri.fromFile(outFile))
        assertTrue(result is LocalBackupResult.Success)

        val preview = repository.inspectBackup(Uri.fromFile(outFile))
        assertTrue(preview is LocalRestorePreviewResult.Valid)
        val valid = preview as LocalRestorePreviewResult.Valid
        assertEquals(TbTerminalDatabase.DATABASE_VERSION, valid.preview.metadata.databaseVersion)

        var metadataJson: String? = null
        ZipInputStream(outFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "metadata.json") {
                    metadataJson = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val parsed = LocalBackupMetadata.fromJson(requireNotNull(metadataJson))
        assertEquals(9, parsed.databaseVersion)
    }

    @Test
    fun inspectBackup_and_stageRestore_rejectNewerAndNonPositiveDatabaseVersions() = runTest {
        val currentFile = createSyntheticBackup(File(context.cacheDir, "backup-current.zip"), version = 9)
        val olderV6File = createSyntheticBackup(File(context.cacheDir, "backup-old-v6.zip"), version = 6)
        val newerFile = createSyntheticBackup(File(context.cacheDir, "backup-newer.zip"), version = 10)
        val zeroFile = createSyntheticBackup(File(context.cacheDir, "backup-zero.zip"), version = 0)

        assertTrue(repository.inspectBackup(Uri.fromFile(currentFile)) is LocalRestorePreviewResult.Valid)
        assertTrue(repository.inspectBackup(Uri.fromFile(olderV6File)) is LocalRestorePreviewResult.Valid)

        val newerPreview = repository.inspectBackup(Uri.fromFile(newerFile))
        assertTrue(newerPreview is LocalRestorePreviewResult.Invalid)
        assertTrue((newerPreview as LocalRestorePreviewResult.Invalid).message.contains("lebih baru"))

        val zeroPreview = repository.inspectBackup(Uri.fromFile(zeroFile))
        assertTrue(zeroPreview is LocalRestorePreviewResult.Invalid)
        assertTrue((zeroPreview as LocalRestorePreviewResult.Invalid).message.contains("tidak valid"))

        assertTrue(repository.stageRestore(Uri.fromFile(newerFile)) is LocalBackupResult.Failed)
        assertTrue(repository.stageRestore(Uri.fromFile(zeroFile)) is LocalBackupResult.Failed)
        assertTrue(repository.stageRestore(Uri.fromFile(olderV6File)) is LocalBackupResult.Success)
        assertTrue(repository.stageRestore(Uri.fromFile(currentFile)) is LocalBackupResult.Success)
    }

    @Test
    fun restoreV6Backup_migratesCleanlyToV7ViaRoomMigration() = runTest {
        val v6SourceFile = createRealV6SqliteFile("backup_source_v6.db")
        val backupZip = File(context.cacheDir, "real-v6-backup.zip").apply { delete() }
        val metadata = LocalBackupMetadata(
            appVersion = "1.0.8",
            databaseVersion = 6,
            createdAt = "2026-11-26T10:00:00Z",
            deviceId = "device-v6",
            pendingSyncCount = 1,
            failedSyncCount = 0,
            conflictCount = 0
        )
        ZipOutputStream(FileOutputStream(backupZip).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toJson().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("database/${TbTerminalDatabase.DATABASE_NAME}"))
            zip.write(v6SourceFile.readBytes())
            zip.closeEntry()
        }

        val preview = repository.inspectBackup(Uri.fromFile(backupZip))
        assertTrue(preview is LocalRestorePreviewResult.Valid)
        assertEquals(6, (preview as LocalRestorePreviewResult.Valid).preview.metadata.databaseVersion)

        val staged = repository.stageRestore(Uri.fromFile(backupZip))
        assertTrue(staged is LocalBackupResult.Success)

        LocalBackupRepository.applyPendingRestoreIfAny(context)

        val restoredDb = Room.databaseBuilder(context, TbTerminalDatabase::class.java, TbTerminalDatabase.DATABASE_NAME)
            .addMigrations(
                TbTerminalDatabase.MIGRATION_4_5,
                TbTerminalDatabase.MIGRATION_5_6,
                TbTerminalDatabase.MIGRATION_6_7,
                TbTerminalDatabase.MIGRATION_7_8,
                TbTerminalDatabase.MIGRATION_8_9
            )
            .allowMainThreadQueries()
            .build()
        try {
            val restoredTx = restoredDb.transactionDao().getByLocalId(77L)!!
            assertEquals(ScaledDecimal2.ofMinorUnits(2000000L), restoredTx.total)
            assertEquals(SyncStatus.PENDING, restoredTx.syncStatus)
            assertEquals(1, restoredDb.syncQueueDao().countByStatus(SyncStatus.PENDING))
        } finally {
            restoredDb.close()
        }
    }

    private fun createRealV6SqliteFile(name: String): File {
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(6) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_products (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, name TEXT NOT NULL, sku TEXT, barcode TEXT, categoryLocalId INTEGER, categoryServerId TEXT, unitLocalId INTEGER, unitServerId TEXT, unitName TEXT, secondaryUnitServerId TEXT, secondaryUnitName TEXT, secondaryUnitFactor TEXT, priceSell REAL NOT NULL, priceBuy REAL NOT NULL, priceRetail REAL NOT NULL DEFAULT 0.0, priceContractor REAL NOT NULL DEFAULT 0.0, discount REAL NOT NULL DEFAULT 0.0, stock REAL NOT NULL, minimumStock REAL NOT NULL, photoFilename TEXT, isActive INTEGER NOT NULL, syncStatus TEXT NOT NULL, remoteCreatedAt TEXT, remoteUpdatedAt TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_categories (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, name TEXT NOT NULL, isActive INTEGER NOT NULL, syncStatus TEXT NOT NULL, remoteCreatedAt TEXT, remoteUpdatedAt TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deletedAt INTEGER)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_local_categories_serverId ON local_categories (serverId)")
                            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_categories_clientGeneratedId ON local_categories (clientGeneratedId)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_units (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, name TEXT NOT NULL, symbol TEXT NOT NULL, isActive INTEGER NOT NULL, syncStatus TEXT NOT NULL, remoteCreatedAt TEXT, remoteUpdatedAt TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deletedAt INTEGER)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_local_units_serverId ON local_units (serverId)")
                            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_units_clientGeneratedId ON local_units (clientGeneratedId)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_customers (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, name TEXT NOT NULL, phone TEXT, address TEXT, isContractor INTEGER NOT NULL DEFAULT 0, creditAllowed INTEGER NOT NULL, creditLimit REAL NOT NULL, paymentTermDays INTEGER NOT NULL DEFAULT 0, isActive INTEGER NOT NULL, syncStatus TEXT NOT NULL, remoteCreatedAt TEXT, remoteUpdatedAt TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_transactions (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, transactionCode TEXT NOT NULL, customerLocalId INTEGER, customerServerId TEXT, cashSessionLocalId INTEGER, cashSessionServerId TEXT, cashierUserId TEXT, status TEXT NOT NULL, subtotal REAL NOT NULL, discount REAL NOT NULL, taxEnabled INTEGER NOT NULL DEFAULT 0, taxMode TEXT, taxRate REAL NOT NULL DEFAULT 0.0, taxLabel TEXT, taxableBase REAL NOT NULL DEFAULT 0.0, taxAmount REAL NOT NULL DEFAULT 0.0, roundingAmount REAL NOT NULL DEFAULT 0.0, total REAL NOT NULL, paidAmount REAL NOT NULL, remainingAmount REAL NOT NULL, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, occurredAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_transaction_items (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transactionLocalId INTEGER NOT NULL, transactionServerId TEXT, productLocalId INTEGER, productServerId TEXT, productNameSnapshot TEXT NOT NULL, skuSnapshot TEXT, unitNameSnapshot TEXT, unitServerId TEXT, baseQuantity TEXT, conversionFactor TEXT, quantity REAL NOT NULL, priceAtTransaction REAL NOT NULL, cogsAtTransaction REAL NOT NULL, discount REAL NOT NULL, subtotal REAL NOT NULL, note TEXT, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_payments (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, transactionLocalId INTEGER NOT NULL, transactionServerId TEXT, method TEXT NOT NULL, amount REAL NOT NULL, reference TEXT, paidAt INTEGER NOT NULL, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_receivables (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, transactionLocalId INTEGER NOT NULL, transactionServerId TEXT, customerLocalId INTEGER, customerServerId TEXT, totalAmount REAL NOT NULL, paidAmount REAL NOT NULL, remainingAmount REAL NOT NULL, status TEXT NOT NULL, dueDate INTEGER, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, occurredAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_cash_sessions (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, cashierUserId TEXT NOT NULL, status TEXT NOT NULL, openedAt INTEGER NOT NULL, closedAt INTEGER, startingCash REAL NOT NULL, expectedCash REAL, actualCash REAL, difference REAL, openingNote TEXT, closingNote TEXT, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_cash_expenses (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, cashSessionLocalId INTEGER, cashSessionServerId TEXT, category TEXT, description TEXT, amount REAL NOT NULL, occurredAt INTEGER NOT NULL, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS local_audit_logs (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, serverId TEXT, clientGeneratedId TEXT NOT NULL, deviceId TEXT, actorUserId TEXT, action TEXT NOT NULL, tableName TEXT NOT NULL, recordId TEXT, description TEXT, metadataJson TEXT, occurredAt INTEGER NOT NULL, syncStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER, deletedAt INTEGER)")
                            listOf("serverId", "actorUserId", "action", "tableName", "recordId", "occurredAt", "syncStatus").forEach {
                                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_audit_logs_$it ON local_audit_logs ($it)")
                            }
                            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_audit_logs_clientGeneratedId ON local_audit_logs (clientGeneratedId)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, entityType TEXT NOT NULL, entityLocalId INTEGER NOT NULL, operation TEXT NOT NULL, payloadJson TEXT NOT NULL, status TEXT NOT NULL, retryCount INTEGER NOT NULL, lastError TEXT, lastErrorCode TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue (status)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType ON sync_queue (entityType)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue (createdAt)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL, updatedAt INTEGER NOT NULL)")

                            db.execSQL("INSERT INTO local_transactions (localId, clientGeneratedId, transactionCode, status, subtotal, discount, total, paidAmount, remainingAmount, syncStatus, createdAt, updatedAt, occurredAt) VALUES (77, 'TRX-BK-77', 'TRX-BK-077', 'PAID', 19999.999999, 0.0, 19999.999999, 19999.999999, 0.0, 'PENDING', 1000, 1000, 1000)")
                            db.execSQL("INSERT INTO sync_queue (id, entityType, entityLocalId, operation, payloadJson, status, retryCount, createdAt, updatedAt) VALUES (1, 'TRANSACTION', 77, 'CREATE', '{}', 'PENDING', 0, 1000, 1000)")
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )
        helper.use {
            val db = it.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
        }
        return context.getDatabasePath(name)
    }

    private fun createSyntheticBackup(file: File, version: Int): File {
        file.delete()
        val metadata = LocalBackupMetadata(
            appVersion = "1.0.8",
            databaseVersion = version,
            createdAt = "2026-11-26T10:00:00Z",
            deviceId = "device-test",
            pendingSyncCount = 0,
            failedSyncCount = 0,
            conflictCount = 0
        )
        ZipOutputStream(FileOutputStream(file).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toJson().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("database/${TbTerminalDatabase.DATABASE_NAME}"))
            zip.write("sqlite-data".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return file
    }
}
