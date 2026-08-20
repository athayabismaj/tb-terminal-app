package com.tbterminal.app.data.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.tbterminal.app.data.remote.DatabaseBackupApi
import com.tbterminal.app.data.remote.DatabaseBackupJobDto
import com.tbterminal.app.data.remote.RestoreConfirmDto
import com.tbterminal.app.data.remote.safeApiCall
import java.security.MessageDigest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink

data class ServerRestoreValidation(
    val job: DatabaseBackupJobDto,
    val confirmationToken: String,
    val confirmationPhrase: String,
    val expiresAt: String
)

class ServerBackupRepository(
    private val contentResolver: ContentResolver,
    private val api: DatabaseBackupApi
) {
    suspend fun list(): RepositoryResult<List<DatabaseBackupJobDto>> =
        safeApiCall { api.list() }.toRepositoryResult(::unwrap)

    suspend fun get(id: String): RepositoryResult<DatabaseBackupJobDto> =
        safeApiCall { api.get(id) }.toRepositoryResult(::unwrap)

    suspend fun create(): RepositoryResult<DatabaseBackupJobDto> =
        safeApiCall { api.create() }.toRepositoryResult(::unwrap)

    suspend fun validateRestore(uri: Uri): RepositoryResult<ServerRestoreValidation> {
        val body = ContentUriRequestBody(contentResolver, uri)
        val name = queryDisplayName(contentResolver, uri) ?: "restore.dump"
        val part = MultipartBody.Part.createFormData("file", name, body)
        return safeApiCall { api.validateRestore(part) }.toRepositoryResult { response ->
            val value = response.data
            if (!response.success || value == null) {
                RepositoryResult.Error(response.code ?: "RESTORE_VALIDATION_FAILED", response.message ?: response.error ?: "Validasi file restore gagal.")
            } else RepositoryResult.Success(
                ServerRestoreValidation(value.job, value.confirmationToken, value.confirmationPhrase, value.expiresAt)
            )
        }
    }

    suspend fun confirmRestore(
        id: String,
        token: String,
        phrase: String,
        acknowledge: Boolean
    ): RepositoryResult<DatabaseBackupJobDto> = safeApiCall {
        api.confirmRestore(id, RestoreConfirmDto(token, phrase, acknowledge))
    }.toRepositoryResult(::unwrap)

    suspend fun download(job: DatabaseBackupJobDto, destination: Uri): RepositoryResult<Unit> = try {
        val response = api.download(job.id)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            RepositoryResult.Error("BACKUP_DOWNLOAD_FAILED", "Download backup gagal (HTTP ${response.code()}).")
        } else {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openOutputStream(destination, "w")?.use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                    }
                }
            } ?: error("Lokasi tujuan tidak dapat dibuka")
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            if (job.sha256 != null && !checksum.equals(job.sha256, ignoreCase = true)) {
                runCatching { contentResolver.delete(destination, null, null) }
                RepositoryResult.Error("BACKUP_CHECKSUM_MISMATCH", "Checksum file hasil download tidak sesuai.")
            } else RepositoryResult.Success(Unit)
        }
    } catch (cause: Throwable) {
        runCatching { contentResolver.delete(destination, null, null) }
        RepositoryResult.Exception(cause)
    }

    private fun <T> unwrap(response: com.tbterminal.app.data.remote.ApiResponse<T>): RepositoryResult<T> {
        val value = response.data
        return if (!response.success || value == null) {
            RepositoryResult.Error(response.code ?: "DATABASE_BACKUP_FAILED", response.message ?: response.error ?: "Operasi backup database gagal.")
        } else RepositoryResult.Success(value)
    }
}

private class ContentUriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri
) : RequestBody() {
    override fun contentType() = (resolver.getType(uri) ?: "application/octet-stream").toMediaType()
    override fun contentLength(): Long = querySize(resolver, uri) ?: -1L
    override fun writeTo(sink: BufferedSink) {
        resolver.openInputStream(uri)?.use { input -> input.copyTo(sink.outputStream()) }
            ?: error("File restore tidak dapat dibuka")
    }
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        cursor.stringValue(OpenableColumns.DISPLAY_NAME)
    }

private fun querySize(resolver: ContentResolver, uri: Uri): Long? =
    resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
    }

private fun Cursor.stringValue(column: String): String? {
    val index = getColumnIndex(column)
    return if (index >= 0 && moveToFirst()) getString(index) else null
}
