package com.tbterminal.app.data.model

data class AuditLogPage(
    val data: List<AuditLogItem>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class AuditLogItem(
    val id: String,
    val actorUserId: String?,
    val actorName: String?,
    val actorRole: String?,
    val action: String,
    val schemaName: String,
    val tableName: String,
    val recordId: String?,
    val ipAddress: String?,
    val oldData: String?,
    val newData: String?,
    val activityLabel: String,
    val createdAt: String
)

data class StoreSettings(
    val id: String,
    val storeName: String,
    val address: String?,
    val phone: String?,
    val receiptHeader: String?,
    val receiptFooter: String?,
    val printerSize: String,
    val updatedAt: String
)

data class UpdateStoreSettingsCommand(
    val storeName: String,
    val address: String?,
    val phone: String?,
    val receiptHeader: String?,
    val receiptFooter: String?,
    val printerSize: String
)
