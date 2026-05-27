package com.tbterminal.app.data.model

data class AuditLogPage(
    val data: List<AuditLog>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class AuditLog(
    val id: String,
    val actorUserId: String?,
    val actorName: String?,
    val actorRole: String?,
    val action: String,
    val schemaName: String,
    val tableName: String,
    val recordId: String?,
    val ipAddress: String?,
    val activityLabel: String,
    val createdAt: String
)
