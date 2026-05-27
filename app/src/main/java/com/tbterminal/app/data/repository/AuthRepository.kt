package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.AuthenticatedUser
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.remote.safeApiCall
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser

interface AuthRepository {
    suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession>

    suspend fun unlock(pin: String): RepositoryResult<Unit>
}

class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override suspend fun login(
        username: String,
        password: String
    ): RepositoryResult<AuthenticatedSession> {
        return safeApiCall { authApi.login(LoginRequest(username, password)) }
            .toRepositoryResult { response ->
                val login = response.data
                if (!response.success || login == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "LOGIN_FAILED",
                        message = response.message ?: response.error ?: "Login gagal"
                    )
                } else {
                    val user = AuthenticatedUser(
                        name = login.user.name,
                        role = login.user.role
                    )
                    sessionManager.saveAuthenticatedSession(
                        token = login.token,
                        user = SessionUser(
                            name = login.user.name,
                            role = login.user.role,
                            isActive = login.user.isActive,
                            joinedAt = login.user.joinedAt,
                            lastLoginAt = login.user.lastLoginAt
                        )
                    )
                    RepositoryResult.Success(
                        AuthenticatedSession(
                            token = login.token,
                            user = user
                        )
                    )
                }
            }
    }

    override suspend fun unlock(pin: String): RepositoryResult<Unit> {
        return safeApiCall { authApi.unlock(UnlockRequest(pin)) }
            .toRepositoryResult { response ->
                if (response.success) {
                    sessionManager.markPinUnlocked()
                    RepositoryResult.Success(Unit)
                } else {
                    RepositoryResult.Error(
                        code = response.code ?: "UNLOCK_FAILED",
                        message = response.message ?: response.error ?: "PIN tidak valid"
                    )
                }
            }
    }
}
