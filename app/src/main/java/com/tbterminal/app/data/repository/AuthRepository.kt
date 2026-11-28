package com.tbterminal.app.data.repository

import com.tbterminal.app.data.auth.DefaultPinUnlockService
import com.tbterminal.app.data.auth.InMemoryOfflinePinVerifierStore
import com.tbterminal.app.data.auth.PinUnlockService
import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.AuthenticatedUser
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.ResetPinRequestDto
import com.tbterminal.app.data.remote.UserApi
import com.tbterminal.app.data.remote.UserUpdateRequest
import com.tbterminal.app.data.remote.safeApiCall
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.validation.CredentialValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PinUnlockResult {
    data object Unlocked : PinUnlockResult
    data class Failed(
        val message: String,
        val attemptsLeft: Int,
        val lockedUntilEpochMillis: Long? = null,
        val isLocked: Boolean = false,
        val remainingLockMillis: Long? = null,
    ) : PinUnlockResult
    data class ForceLogout(val message: String) : PinUnlockResult
}

interface AuthRepository {
    suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession>

    suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult>

    suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit>

    suspend fun getProfile(): RepositoryResult<UserProfile>

    suspend fun updateProfile(
        name: String,
        username: String,
        email: String?
    ): RepositoryResult<UserProfile> = getProfile()

    suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit>

    suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit>
}

class RemoteAuthRepository private constructor(
    authApiProvider: () -> AuthApi,
    private val sessionManager: SessionManager,
    private val deviceIdProvider: () -> String = { "android-unknown" },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val userApiProvider: (() -> UserApi)? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    pinUnlockService: PinUnlockService? = null,
) : AuthRepository {
    constructor(
        authApi: AuthApi,
        sessionManager: SessionManager,
        userApiProvider: (() -> UserApi)? = null
    ) : this({ authApi }, sessionManager, userApiProvider = userApiProvider)

    constructor(
        sessionManager: SessionManager,
        authApiProvider: () -> AuthApi,
        deviceIdProvider: () -> String = { "android-unknown" },
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        userApiProvider: (() -> UserApi)? = null,
        clockMillis: () -> Long = { System.currentTimeMillis() },
        pinUnlockService: PinUnlockService? = null,
    ) : this(
        authApiProvider,
        sessionManager,
        deviceIdProvider,
        ioDispatcher,
        userApiProvider,
        clockMillis,
        pinUnlockService,
    )

    private val authApi: AuthApi by lazy(authApiProvider)
    private val userApi: UserApi? by lazy { userApiProvider?.invoke() }
    private val resolvedPinUnlockService: PinUnlockService = pinUnlockService ?: DefaultPinUnlockService(
        authApiProvider = authApiProvider,
        sessionManager = sessionManager,
        deviceIdProvider = deviceIdProvider,
        verifierStore = InMemoryOfflinePinVerifierStore(),
        ioDispatcher = ioDispatcher,
        clockMillis = clockMillis,
    )

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
                    withContext(ioDispatcher) {
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
                    }
                    RepositoryResult.Success(
                        AuthenticatedSession(
                            token = login.token,
                            user = user
                        )
                    )
                }
            }
    }

    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> =
        resolvedPinUnlockService.unlock(pin)

    override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> {
        if (currentPassword.isBlank()) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Kata sandi wajib diisi")
        }
        if (!CredentialValidator.isValidPin(newPin)) {
            return RepositoryResult.Error(
                "VALIDATION_ERROR",
                "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka"
            )
        }
        val result = safeApiCall {
            authApi.resetPinWithPassword(
                ResetPinRequestDto(
                    currentPassword = currentPassword,
                    newPin = newPin,
                    deviceId = deviceIdProvider()
                )
            )
        }.toRepositoryResult(::successfulUnitResult)
        if (result is RepositoryResult.Success) {
            resolvedPinUnlockService.recordOnlineVerifiedPin(newPin)
            sessionManager.markPinUnlocked()
        } else {
            handleCredentialMaxAttemptsIfNeeded(result)
        }
        return result
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

    override suspend fun updateProfile(
        name: String,
        username: String,
        email: String?
    ): RepositoryResult<UserProfile> {
        val trimmedName = name.trim()
        val trimmedUsername = username.trim()
        val normalizedEmail = email?.trim()?.takeIf { it.isNotBlank() }

        if (trimmedName.length !in 2..100) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Nama harus terdiri dari 2 sampai 100 karakter.")
        }
        if (!trimmedUsername.matches(USERNAME_PATTERN)) {
            return RepositoryResult.Error(
                "VALIDATION_ERROR",
                "Username minimal 3 karakter (huruf, angka, titik, garis bawah, atau hubung)."
            )
        }
        if (normalizedEmail != null && (normalizedEmail.length > 150 || !normalizedEmail.matches(EMAIL_PATTERN))) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Format email tidak valid.")
        }

        val sessionUser = sessionManager.readSessionUser()
            ?: return RepositoryResult.Error("AUTH_REQUIRED", "Sesi login tidak tersedia")

        val api = userApi
            ?: return saveProfileLocally(
                sessionUser = sessionUser,
                id = sessionUser.userId,
                name = trimmedName,
                username = trimmedUsername,
                role = sessionUser.role,
                email = normalizedEmail,
                isActive = sessionUser.isActive,
                joinedAt = sessionUser.joinedAt,
                lastLoginAt = sessionUser.lastLoginAt
            )

        val usersRes = safeApiCall { api.getUsers(page = 1, limit = 200) }
            .toRepositoryResult { response ->
                val list = response.data?.data
                if (!response.success || list == null) {
                    RepositoryResult.Error(
                        response.code ?: "USERS_FAILED",
                        response.message ?: response.error ?: "Profil pengguna gagal dimuat dari server."
                    )
                } else {
                    RepositoryResult.Success(list)
                }
            }

        when (usersRes) {
            is RepositoryResult.Error -> return usersRes
            is RepositoryResult.Exception -> return usersRes
            is RepositoryResult.Success -> {
                val targetUser = usersRes.data.firstOrNull { dto ->
                    (!sessionUser.userId.isNullOrBlank() && dto.id == sessionUser.userId) ||
                        (!sessionUser.username.isNullOrBlank() &&
                            dto.username.equals(sessionUser.username, ignoreCase = true))
                } ?: usersRes.data.firstOrNull { dto ->
                    dto.name.equals(sessionUser.name, ignoreCase = true) &&
                        dto.roleName.equals(sessionUser.role, ignoreCase = true)
                } ?: return RepositoryResult.Error(
                    "PROFILE_NOT_FOUND",
                    "Akun pengguna tidak ditemukan di daftar pengguna."
                )

                val updateRes = safeApiCall {
                    api.updateUser(
                        id = targetUser.id,
                        request = UserUpdateRequest(
                            name = trimmedName,
                            username = trimmedUsername,
                            isActive = targetUser.isActive,
                            roleId = targetUser.roleId,
                            email = normalizedEmail
                        )
                    )
                }.toRepositoryResult { response ->
                    val updated = response.data
                    if (!response.success || updated == null) {
                        RepositoryResult.Error(
                            response.code ?: "UPDATE_PROFILE_FAILED",
                            response.message ?: response.error ?: "Profil gagal diperbarui."
                        )
                    } else {
                        RepositoryResult.Success(updated)
                    }
                }

                return when (updateRes) {
                    is RepositoryResult.Success -> {
                        val updated = updateRes.data
                        saveProfileLocally(
                            sessionUser = sessionUser,
                            id = updated.id,
                            name = updated.name,
                            username = updated.username,
                            role = updated.roleName.ifBlank { sessionUser.role },
                            email = updated.email,
                            isActive = updated.isActive,
                            joinedAt = updated.createdAt.ifBlank { sessionUser.joinedAt },
                            lastLoginAt = updated.lastLogin ?: sessionUser.lastLoginAt
                        )
                    }
                    is RepositoryResult.Error -> updateRes
                    is RepositoryResult.Exception -> updateRes
                }
            }
        }
    }

    private suspend fun saveProfileLocally(
        sessionUser: SessionUser,
        id: String?,
        name: String,
        username: String,
        role: String,
        email: String?,
        isActive: Boolean,
        joinedAt: String,
        lastLoginAt: String?
    ): RepositoryResult<UserProfile> {
        val updatedSession = sessionUser.copy(
            userId = id ?: sessionUser.userId,
            name = name,
            username = username,
            role = role,
            email = email,
            isActive = isActive,
            joinedAt = joinedAt,
            lastLoginAt = lastLoginAt
        )
        withContext(ioDispatcher) {
            sessionManager.updateSessionUser(updatedSession)
        }
        return RepositoryResult.Success(
            UserProfile(
                id = updatedSession.userId,
                username = username,
                name = name,
                role = role,
                email = email,
                isActive = isActive,
                joinedAt = joinedAt,
                lastLoginAt = lastLoginAt
            )
        )
    }

    override suspend fun changeMyPassword(
        oldPassword: String,
        newPassword: String
    ): RepositoryResult<Unit> {
        if (oldPassword.isBlank()) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Password lama wajib diisi")
        }
        CredentialValidator.validatePassword(
            password = newPassword,
            requiredMessage = "Password baru wajib diisi",
            minMessage = "Password baru minimal ${CredentialValidator.PASSWORD_MIN_LENGTH} karakter",
            maxMessage = "Password baru maksimal ${CredentialValidator.PASSWORD_MAX_LENGTH} karakter"
        )?.let { message ->
            return RepositoryResult.Error("VALIDATION_ERROR", message)
        }
        if (oldPassword == newPassword) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Password baru harus berbeda dari password lama")
        }
        val result = safeApiCall {
            authApi.changeMyPassword(ChangePasswordRequestDto(oldPassword, newPassword))
        }.toRepositoryResult(::successfulUnitResult)
        if (result is RepositoryResult.Success) {
            withContext(ioDispatcher) {
                sessionManager.handlePasswordChanged()
            }
        } else {
            handleCredentialMaxAttemptsIfNeeded(result)
        }
        return result
    }

    override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> {
        if (!CredentialValidator.isValidPin(oldPin)) {
            return RepositoryResult.Error(
                "VALIDATION_ERROR",
                "PIN lama harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka"
            )
        }
        if (!CredentialValidator.isValidPin(newPin)) {
            return RepositoryResult.Error(
                "VALIDATION_ERROR",
                "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka"
            )
        }
        if (oldPin == newPin) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN baru harus berbeda dari PIN lama")
        }
        val result = safeApiCall {
            authApi.changeMyPin(ChangePinRequestDto(oldPin, newPin))
        }.toRepositoryResult(::successfulUnitResult)
        if (result is RepositoryResult.Success) {
            resolvedPinUnlockService.recordOnlineVerifiedPin(newPin)
        } else {
            handleCredentialMaxAttemptsIfNeeded(result)
        }
        return result
    }

    private suspend fun handleCredentialMaxAttemptsIfNeeded(result: RepositoryResult<Unit>) {
        if (result !is RepositoryResult.Error) return
        val normalizedCode = result.code.trim().uppercase()
        val lowerMsg = result.message.lowercase()
        val isMaxAttemptsRevocation = normalizedCode == "PIN_MAX_ATTEMPTS_EXCEEDED" ||
            ((normalizedCode == "HTTP_401" || normalizedCode == "UNAUTHORIZED") &&
                (lowerMsg.contains("batas percobaan") ||
                    lowerMsg.contains("terlampaui") ||
                    lowerMsg.contains("login kembali")))
        if (isMaxAttemptsRevocation) {
            val reason = result.message.ifBlank {
                "Batas percobaan terlampaui. Silakan login kembali."
            }
            withContext(ioDispatcher) {
                resolvedPinUnlockService.clearOfflineState()
                sessionManager.handleUnauthorized(reason)
            }
        }
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
        val USERNAME_PATTERN = Regex("^[a-zA-Z0-9._-]{3,50}$")
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
