package com.tbterminal.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ManagerApprovalAction(val displayName: String) {
    VOID_TRANSACTION("Void transaksi"),
    REFUND_TRANSACTION("Refund transaksi"),
    DISCOUNT_OVERRIDE("Override diskon"),
}

@Serializable
enum class ManagerApprovalResourceType {
    TRANSACTION,
}

@Serializable
enum class ManagerApprovalStatus {
    APPROVED,
    USED,
    EXPIRED,
}

data class ManagerApprovalContext(
    val action: ManagerApprovalAction,
    val resourceId: String,
    val resourceType: ManagerApprovalResourceType = ManagerApprovalResourceType.TRANSACTION,
) {
    init {
        require(resourceId.isNotBlank()) { "Resource ID approval tidak boleh kosong." }
    }
}

@Serializable
data class CreateManagerApprovalCommand(
    val action: ManagerApprovalAction,
    val resourceType: ManagerApprovalResourceType,
    val resourceId: String,
    val approverUsername: String,
    val approverPin: String,
) {
    override fun toString(): String =
        "CreateManagerApprovalCommand(action=$action, resourceType=$resourceType, " +
            "resourceId=$resourceId, approverUsername=$approverUsername, approverPin=<redacted>)"
}

@Serializable
data class ManagerApprovalGrant(
    val approvalId: String,
    val action: ManagerApprovalAction,
    val resourceType: ManagerApprovalResourceType,
    val resourceId: String,
    val status: ManagerApprovalStatus,
    val createdAt: String,
    val expiresAt: String,
    val usedAt: String? = null,
)
