package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SecurityApi {
    @GET("/api/system/audit-logs")
    suspend fun getAuditLogs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("action") action: String? = null,
        @Query("range") range: String? = null
    ): Response<ApiResponse<PaginatedResponse<AuditLogResponseDto>>>
}

@Serializable
data class AuditLogResponseDto(
    val id: String,
    val actorUserId: String? = null,
    val actorName: String? = null,
    val actorRole: String? = null,
    val action: String,
    val schemaName: String,
    val tableName: String,
    val recordId: String? = null,
    val ipAddress: String? = null,
    val activityLabel: String,
    val createdAt: String
)
