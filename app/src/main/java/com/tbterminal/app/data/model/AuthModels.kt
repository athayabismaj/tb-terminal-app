package com.tbterminal.app.data.model

data class AuthenticatedSession(
    val token: String,
    val user: AuthenticatedUser
)

data class AuthenticatedUser(
    val name: String,
    val role: String
)
