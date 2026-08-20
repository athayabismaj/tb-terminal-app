package com.tbterminal.app.data.repository

import android.content.Context
import android.net.Uri
import com.tbterminal.app.BuildConfig
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.local.id.DeviceIdProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LocalBackupRepository(
    private val context: Context,
    private val database: TbTerminalDatabase,
    private val deviceIdProvider: DeviceIdProvider
) {
    suspend fun getSummary(): LocalBackupSummary = withContext(Dispatchers.IO) {
        LocalBackupSummary(
            pendingSyncCount = database.syncQueueDao().countByStatus(SyncStatus.PENDING),
            failedSyncCount = database.syncQueueDao().countByStatus(SyncStatus.FAILED),
            conflictCount = database.syncQueueDao().countByStatus(SyncStatus.CONFLICT),
            syncingCount = database.syncQueueDao().countByStatus(SyncStatus.SYNCING),
            openCashSessionCount = database.cashSessionDao().countByStatus(CASH_SESSION_OPEN),
            suggestedFileName = createBackupFileName()
        )
    }

    suspend fun createBackup(uri: Uri): LocalBackupResult = withContext(Dispatchers.IO) {
        val summary = getSummary()
        val dbFile = context.getDatabasePath(TbTerminalDatabase.DATABASE_NAME)
        if (!dbFile.exists()) {
            return@withContext LocalBackupResult.Failed("Database lokal belum tersedia.")
        }

        checkpointDatabase()
        val metadata = LocalBackupMetadata(
            appVersion = BuildConfig.VERSION_NAME,
            databaseVersion = CURRENT_DATABASE_VERSION,
            createdAt = Instant.now().toString(),
            deviceId = deviceIdProvider.deviceId,
            pendingSyncCount = summary.pendingSyncCount,
            failedSyncCount = summary.failedSyncCount,
            conflictCount = summary.conflictCount
        )

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(METADATA_ENTRY))
                    zip.write(metadata.toJson().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
                    FileInputStream(dbFile).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            } ?: return@withContext LocalBackupResult.Failed("Lokasi backup tidak bisa dibuka.")
        }.fold(
            onSuccess = {
                LocalBackupResult.Success("Backup lokal berhasil dibuat.")
            },
            onFailure = { error ->
                LocalBackupResult.Failed(error.message ?: "Backup lokal gagal dibuat.")
            }
        )
    }

    suspend fun inspectBackup(uri: Uri): LocalRestorePreviewResult = withContext(Dispatchers.IO) {
        runCatching {
            readBackup(uri)
        }.fold(
            onSuccess = { backup ->
                when {
                    backup.metadata.databaseVersion > CURRENT_DATABASE_VERSION -> {
                        LocalRestorePreviewResult.Invalid(
                            "Versi database backup (${backup.metadata.databaseVersion}) lebih baru dari aplikasi ini."
                        )
                    }
                    !backup.hasDatabase -> {
                        LocalRestorePreviewResult.Invalid("File backup tidak berisi database lokal.")
                    }
                    else -> LocalRestorePreviewResult.Valid(
                        LocalRestorePreview(
                            metadata = backup.metadata,
                            openCashSessionCount = database.cashSessionDao().countByStatus(CASH_SESSION_OPEN),
                            syncingCount = database.syncQueueDao().countByStatus(SyncStatus.SYNCING)
                        )
                    )
                }
            },
            onFailure = { error ->
                LocalRestorePreviewResult.Invalid(error.message ?: "File backup tidak valid.")
            }
        )
    }

    suspend fun stageRestore(uri: Uri): LocalBackupResult = withContext(Dispatchers.IO) {
        val syncingCount = database.syncQueueDao().countByStatus(SyncStatus.SYNCING)
        if (syncingCount > 0) {
            return@withContext LocalBackupResult.Failed("Restore tidak bisa dilakukan saat sinkronisasi berjalan.")
        }

        val backup = runCatching { readBackup(uri) }.getOrElse { error ->
            return@withContext LocalBackupResult.Failed(error.message ?: "File backup tidak valid.")
        }
        if (backup.metadata.databaseVersion > CURRENT_DATABASE_VERSION || !backup.hasDatabase) {
            return@withContext LocalBackupResult.Failed("File backup tidak kompatibel.")
        }

        runCatching {
            val pendingDir = pendingRestoreDir(context).apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val stagedDb = File(pendingDir, TbTerminalDatabase.DATABASE_NAME)
            extractDatabase(uri, stagedDb)
            File(pendingDir, METADATA_FILE_NAME).writeText(backup.metadata.toJson(), Charsets.UTF_8)
        }.fold(
            onSuccess = {
                LocalBackupResult.Success("Restore disiapkan. Restart aplikasi untuk menerapkan database backup.")
            },
            onFailure = { error ->
                LocalBackupResult.Failed(error.message ?: "Restore gagal disiapkan.")
            }
        )
    }

    private fun checkpointDatabase() {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        }
    }

    private fun readBackup(uri: Uri): ReadBackupResult {
        var metadata: LocalBackupMetadata? = null
        var hasDatabase = false
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        METADATA_ENTRY -> metadata = LocalBackupMetadata.fromJson(zip.readBytes().toString(Charsets.UTF_8))
                        DATABASE_ENTRY -> hasDatabase = true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("File backup tidak bisa dibuka.")

        return ReadBackupResult(
            metadata = metadata ?: error("Metadata backup tidak ditemukan."),
            hasDatabase = hasDatabase
        )
    }

    private fun extractDatabase(uri: Uri, targetFile: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == DATABASE_ENTRY) {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { output -> zip.copyTo(output) }
                        zip.closeEntry()
                        return
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("File backup tidak bisa dibuka.")
        error("Database backup tidak ditemukan.")
    }

    private fun createBackupFileName(): String {
        val timestamp = Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US))
        return "tb-terminal-backup-$timestamp.zip"
    }

    private data class ReadBackupResult(
        val metadata: LocalBackupMetadata,
        val hasDatabase: Boolean
    )

    companion object {
        private const val CURRENT_DATABASE_VERSION = 3
        private const val BACKUP_TYPE = "tb-terminal-local-backup"
        private const val FORMAT_VERSION = 1
        private const val METADATA_ENTRY = "metadata.json"
        private const val DATABASE_ENTRY = "database/${TbTerminalDatabase.DATABASE_NAME}"
        private const val METADATA_FILE_NAME = "metadata.json"
        private const val CASH_SESSION_OPEN = "OPEN"

        fun applyPendingRestoreIfAny(context: Context) {
            val pendingDir = pendingRestoreDir(context)
            val stagedDb = File(pendingDir, TbTerminalDatabase.DATABASE_NAME)
            if (!stagedDb.exists()) return

            val dbFile = context.getDatabasePath(TbTerminalDatabase.DATABASE_NAME)
            dbFile.parentFile?.mkdirs()
            listOf(
                dbFile,
                File(dbFile.path + "-wal"),
                File(dbFile.path + "-shm")
            ).forEach { file ->
                if (file.exists()) file.delete()
            }
            stagedDb.copyTo(dbFile, overwrite = true)
            pendingDir.deleteRecursively()
        }

        private fun pendingRestoreDir(context: Context): File {
            return File(context.filesDir, "pending_local_restore")
        }
    }
}

data class LocalBackupSummary(
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val conflictCount: Int = 0,
    val syncingCount: Int = 0,
    val openCashSessionCount: Int = 0,
    val suggestedFileName: String = "tb-terminal-backup.zip"
)

data class LocalBackupMetadata(
    val appVersion: String,
    val databaseVersion: Int,
    val createdAt: String,
    val deviceId: String,
    val pendingSyncCount: Int,
    val failedSyncCount: Int,
    val conflictCount: Int
) {
    fun toJson(): String {
        return JSONObject()
            .put("type", "tb-terminal-local-backup")
            .put("formatVersion", 1)
            .put("appVersion", appVersion)
            .put("databaseVersion", databaseVersion)
            .put("createdAt", createdAt)
            .put("deviceId", deviceId)
            .put("pendingSyncCount", pendingSyncCount)
            .put("failedSyncCount", failedSyncCount)
            .put("conflictCount", conflictCount)
            .toString(2)
    }

    companion object {
        fun fromJson(json: String): LocalBackupMetadata {
            val obj = JSONObject(json)
            require(obj.optString("type") == "tb-terminal-local-backup") {
                "Tipe backup tidak dikenali."
            }
            require(obj.optInt("formatVersion") == 1) {
                "Format backup tidak didukung."
            }
            return LocalBackupMetadata(
                appVersion = obj.optString("appVersion"),
                databaseVersion = obj.optInt("databaseVersion"),
                createdAt = obj.optString("createdAt"),
                deviceId = obj.optString("deviceId"),
                pendingSyncCount = obj.optInt("pendingSyncCount"),
                failedSyncCount = obj.optInt("failedSyncCount"),
                conflictCount = obj.optInt("conflictCount")
            )
        }
    }
}

data class LocalRestorePreview(
    val metadata: LocalBackupMetadata,
    val openCashSessionCount: Int,
    val syncingCount: Int
)

sealed interface LocalRestorePreviewResult {
    data class Valid(val preview: LocalRestorePreview) : LocalRestorePreviewResult
    data class Invalid(val message: String) : LocalRestorePreviewResult
}

sealed interface LocalBackupResult {
    data class Success(val message: String) : LocalBackupResult
    data class Failed(val message: String) : LocalBackupResult
}
