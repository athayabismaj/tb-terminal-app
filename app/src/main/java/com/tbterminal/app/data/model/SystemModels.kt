package com.tbterminal.app.data.model

import com.tbterminal.app.data.remote.BigDecimalStringSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AuditLogPage(
    val data: List<AuditLogItem>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

@Serializable
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

@Serializable
data class StoreSettings(
    val id: String = "",
    val storeName: String,
    val address: String? = null,
    val phone: String? = null,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null,
    val printerSize: String = "58mm",
    @Serializable(with = BigDecimalStringSerializer::class)
    val cashierDiscountLimitPercent: BigDecimal = BigDecimal("10.00"),
    val taxEnabled: Boolean = false,
    @Serializable(with = BigDecimalStringSerializer::class)
    val taxRate: BigDecimal = BigDecimal("11.00"),
    val taxMode: String = "EXCLUDE",
    val taxLabel: String = "PPN",
    val storeNpwp: String? = null,
    val cashRoundingUnit: Int = 0,
    val cashRoundingRule: String = "NEAREST",
    val updatedAt: String = ""
)

@Serializable
data class UpdateStoreSettingsCommand(
    val storeName: String,
    val address: String?,
    val phone: String?,
    val receiptHeader: String?,
    val receiptFooter: String?,
    val printerSize: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    val cashierDiscountLimitPercent: BigDecimal? = null,
    val taxEnabled: Boolean? = null,
    @Serializable(with = BigDecimalStringSerializer::class)
    val taxRate: BigDecimal? = null,
    val taxMode: String? = null,
    val taxLabel: String? = null,
    val storeNpwp: String? = null,
    val cashRoundingUnit: Int? = null,
    val cashRoundingRule: String? = null
)
