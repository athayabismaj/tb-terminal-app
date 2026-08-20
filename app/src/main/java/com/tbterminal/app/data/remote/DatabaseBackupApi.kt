package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface DatabaseBackupApi {
    @GET("/api/system/database-backups")
    suspend fun list(@Query("limit") limit: Int = 30): Response<ApiResponse<List<DatabaseBackupJobDto>>>

    @GET("/api/system/database-backups/{id}")
    suspend fun get(@Path("id") id: String): Response<ApiResponse<DatabaseBackupJobDto>>

    @POST("/api/system/database-backups")
    suspend fun create(): Response<ApiResponse<DatabaseBackupJobDto>>

    @Streaming
    @GET("/api/system/database-backups/{id}/download")
    suspend fun download(@Path("id") id: String): Response<ResponseBody>

    @Multipart
    @POST("/api/system/database-backups/restore/validate")
    suspend fun validateRestore(@Part file: MultipartBody.Part): Response<ApiResponse<RestoreValidationDto>>

    @POST("/api/system/database-backups/restore/{id}/confirm")
    suspend fun confirmRestore(
        @Path("id") id: String,
        @Body request: RestoreConfirmDto
    ): Response<ApiResponse<DatabaseBackupJobDto>>
}

@Serializable
data class DatabaseBackupJobDto(
    val id: String,
    val operation: String,
    val status: String,
    val fileName: String,
    val fileSize: Long? = null,
    val sha256: String? = null,
    val requestedBy: String? = null,
    val sourceBackupId: String? = null,
    val errorMessage: String? = null,
    val createdAt: String,
    val completedAt: String? = null,
    val removedAt: String? = null
)

@Serializable
data class RestoreValidationDto(
    val job: DatabaseBackupJobDto,
    val confirmationToken: String,
    val confirmationPhrase: String,
    val expiresAt: String
)

@Serializable
data class RestoreConfirmDto(
    val confirmationToken: String,
    val confirmationPhrase: String,
    val acknowledgeDowntimeAndOverwrite: Boolean
)
