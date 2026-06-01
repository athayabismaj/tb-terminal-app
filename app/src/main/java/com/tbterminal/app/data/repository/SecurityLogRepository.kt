package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.data.model.AuditLogPage
import com.tbterminal.app.data.remote.AuditLogResponseDto
import com.tbterminal.app.data.remote.SecurityApi
import com.tbterminal.app.data.remote.safeApiCall
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.remote.ApiResponse

interface SecurityLogRepository {
    suspend fun getAuditLogs(
        page: Int,
        limit: Int,
        action: String?,
        range: String?
    ): NetworkResult<ApiResponse<AuditLogPage>>
}

class RemoteSecurityLogRepository(
    private val securityApi: SecurityApi
) : SecurityLogRepository {
    override suspend fun getAuditLogs(
        page: Int,
        limit: Int,
        action: String?,
        range: String?
    ): NetworkResult<ApiResponse<AuditLogPage>> {
        val result = safeApiCall {
            securityApi.getAuditLogs(page = page, limit = limit, action = action, range = range)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                val pageData = response.data
                if (response.success && pageData != null) {
                    NetworkResult.Success(
                        ApiResponse(
                            success = true,
                            data = pageData.toAuditLogPage(),
                            message = response.message
                        )
                    )
                } else {
                    NetworkResult.Error(code = "AUDIT_LOGS_FAILED", message = response.message ?: "Log keamanan gagal dimuat.")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Exception -> result
        }
    }
}

private fun com.tbterminal.app.data.remote.PaginatedResponse<AuditLogResponseDto>.toAuditLogPage(): AuditLogPage {
    return AuditLogPage(
        data = data.map { log ->
            AuditLogItem(
                id = log.id,
                actorUserId = log.actorUserId,
                actorName = log.actorName,
                actorRole = log.actorRole,
                action = log.action,
                schemaName = log.schemaName,
                tableName = log.tableName,
                recordId = log.recordId,
                ipAddress = log.ipAddress,
                oldData = null,
                newData = null,
                activityLabel = log.activityLabel,
                createdAt = log.createdAt
            )
        },
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}
