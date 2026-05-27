package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.AuditLog
import com.tbterminal.app.data.model.AuditLogPage
import com.tbterminal.app.data.remote.AuditLogResponseDto
import com.tbterminal.app.data.remote.SecurityApi
import com.tbterminal.app.data.remote.safeApiCall

interface SecurityLogRepository {
    suspend fun getAuditLogs(
        page: Int,
        limit: Int,
        action: String?,
        range: String?
    ): RepositoryResult<AuditLogPage>
}

class RemoteSecurityLogRepository(
    private val securityApi: SecurityApi
) : SecurityLogRepository {
    override suspend fun getAuditLogs(
        page: Int,
        limit: Int,
        action: String?,
        range: String?
    ): RepositoryResult<AuditLogPage> {
        return safeApiCall {
            securityApi.getAuditLogs(page = page, limit = limit, action = action, range = range)
        }.toRepositoryResult { response ->
            val pageData = response.data
            if (!response.success || pageData == null) {
                RepositoryResult.Error(
                    code = response.code ?: "AUDIT_LOGS_FAILED",
                    message = response.message ?: response.error ?: "Log keamanan gagal dimuat."
                )
            } else {
                RepositoryResult.Success(pageData.toAuditLogPage())
            }
        }
    }
}

private fun com.tbterminal.app.data.remote.PaginatedResponse<AuditLogResponseDto>.toAuditLogPage(): AuditLogPage {
    return AuditLogPage(
        data = data.map { log ->
            AuditLog(
                id = log.id,
                actorUserId = log.actorUserId,
                actorName = log.actorName,
                actorRole = log.actorRole,
                action = log.action,
                schemaName = log.schemaName,
                tableName = log.tableName,
                recordId = log.recordId,
                ipAddress = log.ipAddress,
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
