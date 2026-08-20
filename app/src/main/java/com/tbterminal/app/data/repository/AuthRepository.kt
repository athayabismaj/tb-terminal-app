package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.AuthenticatedUser
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.remote.safeApiCall
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser

interface AuthRepository {
    suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession>

    suspend fun unlock(pin: String): RepositoryResult<Unit>

    suspend fun getProfile(): RepositoryResult<UserProfile>

    suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit>

    suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit>
}

class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override suspend fun login(
        username: String,
        password: String
    ): RepositoryResult<AuthenticatedSession> {
        if (username.isBlank() || password.isBlank()) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Username dan password wajib diisi")
        }
        return safeApiCall { authApi.login(LoginRequest(username, password)) }
            .toRepositoryResult { response ->
                val login = response.data
                if (!response.success || login == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "LOGIN_FAILED",
                        message = response.message ?: response.error ?: "Login gagal"
                    )
                } else {
                    val userId = login.user.id.takeIf(String::isNotBlank)
                    val user = AuthenticatedUser(
                        name = login.user.name,
                        role = login.user.role,
                        userId = userId
                    )
                    sessionManager.saveAuthenticatedSession(
                        token = login.token,
                        refreshToken = login.refreshToken,
                        user = SessionUser(
                            name = login.user.name,
                            role = login.user.role,
                            isActive = login.user.isActive,
                            joinedAt = login.user.joinedAt,
                            lastLoginAt = login.user.lastLoginAt,
                            userId = userId,
                            username = login.user.username,
                            email = login.user.email
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
        if (!pin.matches(Regex("\\d{4,6}"))) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN harus terdiri dari 4 sampai 6 digit")
        }
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

    override suspend fun getProfile(): RepositoryResult<UserProfile> {
        val sessionUser = sessionManager.readSessionUser()
            ?: return RepositoryResult.Error("AUTH_REQUIRED", "Sesi login tidak tersedia")
        return safeApiCall { authApi.getMe() }
            .toRepositoryResult { response ->
                val remote = response.data
                if (!response.success || remote == null || remote.username.isBlank() || remote.role.isBlank()) {
                    RepositoryResult.Error(
                        response.code ?: "INVALID_PROFILE",
                        response.message ?: response.error ?: "Profil pengguna tidak valid"
                    )
                } else {
                    RepositoryResult.Success(
                        UserProfile(
                            id = sessionUser.userId,
                            username = remote.username,
                            name = sessionUser.name,
                            role = remote.role,
                            email = sessionUser.email,
                            isActive = sessionUser.isActive,
                            joinedAt = sessionUser.joinedAt,
                            lastLoginAt = sessionUser.lastLoginAt
                        )
                    )
                }
            }
    }

    override suspend fun changeMyPassword(
        oldPassword: String,
        newPassword: String
    ): RepositoryResult<Unit> {
        if (oldPassword.isBlank()) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Password lama wajib diisi")
        }
        if (newPassword.length < 6) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Password baru minimal 6 karakter")
        }
        if (oldPassword == newPassword) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Password baru harus berbeda dari password lama")
        }
        return safeApiCall {
            authApi.changeMyPassword(ChangePasswordRequestDto(oldPassword, newPassword))
        }.toRepositoryResult(::successfulUnitResult)
    }

    override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> {
        if (!oldPin.matches(PIN_PATTERN)) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN lama harus terdiri dari 4 sampai 6 digit")
        }
        if (!newPin.matches(PIN_PATTERN)) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN baru harus terdiri dari 4 sampai 6 digit")
        }
        if (oldPin == newPin) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN baru harus berbeda dari PIN lama")
        }
        return safeApiCall {
            authApi.changeMyPin(ChangePinRequestDto(oldPin, newPin))
        }.toRepositoryResult(::successfulUnitResult)
    }

    private fun successfulUnitResult(response: com.tbterminal.app.data.remote.ApiResponse<Unit>): RepositoryResult<Unit> {
        return if (response.success) {
            RepositoryResult.Success(Unit)
        } else {
            RepositoryResult.Error(
                response.code ?: "UPDATE_FAILED",
                response.message ?: response.error ?: "Perubahan credential gagal"
            )
        }
    }

    private companion object {
        val PIN_PATTERN = Regex("\\d{4,6}")
    }
}
