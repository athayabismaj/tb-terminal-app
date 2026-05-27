package com.tbterminal.app.ui.users

enum class StaffRole {
    Owner,
    Admin,
    Kasir,
    Other;

    companion object {
        fun fromBackendName(name: String): StaffRole {
            return when (name.trim().uppercase()) {
                "OWNER" -> Owner
                "ADMIN" -> Admin
                "KASIR" -> Kasir
                else -> Other
            }
        }
    }
}

enum class StaffStatus {
    Active,
    Inactive
}

data class StaffMember(
    val id: String,
    val name: String,
    val email: String,
    val rawEmail: String?,
    val username: String,
    val roleId: String,
    val role: StaffRole,
    val status: StaffStatus,
    val lastLogin: String,
    val lastLoginEpochMillis: Long? = null,
    val initials: String
)

data class StaffEditInput(
    val name: String,
    val username: String,
    val email: String,
    val roleId: String,
    val isActive: Boolean,
    val newPassword: String,
    val newPin: String
)
