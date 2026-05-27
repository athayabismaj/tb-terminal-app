package com.tbterminal.app.data.model

data class UserPage(
    val data: List<ManagedUser>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class ManagedUser(
    val id: String,
    val roleId: String,
    val roleName: String,
    val name: String,
    val username: String,
    val email: String?,
    val isActive: Boolean,
    val lastLogin: String?,
    val createdAt: String
)

data class UserRole(
    val id: String,
    val name: String
)

data class CreateUserCommand(
    val name: String,
    val username: String,
    val password: String,
    val pin: String,
    val email: String?,
    val roleId: String
)

data class UpdateUserCommand(
    val name: String,
    val username: String,
    val isActive: Boolean,
    val roleId: String,
    val email: String?,
    val newPassword: String? = null,
    val newPin: String? = null
)
