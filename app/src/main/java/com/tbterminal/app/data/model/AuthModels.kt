package com.tbterminal.app.data.model

data class AuthenticatedSession(
    val token: String,
    val user: AuthenticatedUser
)

data class AuthenticatedUser(
    val name: String,
    val role: String,
    val userId: String? = null
)

data class UserProfile(
    val id: String?,
    val username: String,
    val name: String,
    val role: String,
    val email: String?,
    val isActive: Boolean,
    val joinedAt: String,
    val lastLoginAt: String?
)
