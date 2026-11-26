package com.tbterminal.app.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.id.DeviceIdProvider
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
    }

    @Test
    fun createBackup_usesRoomDatabaseVersionSixInMetadata() = runTest {
        assertEquals(6, TbTerminalDatabase.DATABASE_VERSION)
        val dbFile = context.getDatabasePath(TbTerminalDatabase.DATABASE_NAME).apply {
            parentFile?.mkdirs()
            writeText("sqlite-placeholder")
        }
        assertTrue(dbFile.exists())

        val outFile = File(context.cacheDir, "backup-v6.zip").apply { delete() }
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
        assertEquals(6, parsed.databaseVersion)
    }

    @Test
    fun inspectBackup_and_stageRestore_rejectNewerAndNonPositiveDatabaseVersions() = runTest {
        val currentFile = createSyntheticBackup(File(context.cacheDir, "backup-current.zip"), version = 6)
        val olderFile = createSyntheticBackup(File(context.cacheDir, "backup-old.zip"), version = 3)
        val newerFile = createSyntheticBackup(File(context.cacheDir, "backup-newer.zip"), version = 7)
        val zeroFile = createSyntheticBackup(File(context.cacheDir, "backup-zero.zip"), version = 0)

        assertTrue(repository.inspectBackup(Uri.fromFile(currentFile)) is LocalRestorePreviewResult.Valid)
        assertTrue(repository.inspectBackup(Uri.fromFile(olderFile)) is LocalRestorePreviewResult.Valid)

        val newerPreview = repository.inspectBackup(Uri.fromFile(newerFile))
        assertTrue(newerPreview is LocalRestorePreviewResult.Invalid)
        assertTrue((newerPreview as LocalRestorePreviewResult.Invalid).message.contains("lebih baru"))

        val zeroPreview = repository.inspectBackup(Uri.fromFile(zeroFile))
        assertTrue(zeroPreview is LocalRestorePreviewResult.Invalid)
        assertTrue((zeroPreview as LocalRestorePreviewResult.Invalid).message.contains("tidak valid"))

        assertTrue(repository.stageRestore(Uri.fromFile(newerFile)) is LocalBackupResult.Failed)
        assertTrue(repository.stageRestore(Uri.fromFile(zeroFile)) is LocalBackupResult.Failed)
        assertTrue(repository.stageRestore(Uri.fromFile(currentFile)) is LocalBackupResult.Success)
    }

    private fun createSyntheticBackup(file: File, version: Int): File {
        file.delete()
        val metadata = LocalBackupMetadata(
            appVersion = "1.0.7",
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
