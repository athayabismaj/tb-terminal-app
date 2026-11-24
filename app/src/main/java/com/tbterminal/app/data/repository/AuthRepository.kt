package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.AuthenticatedUser
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.PinUnlockResponse
import com.tbterminal.app.data.remote.ResetPinRequestDto
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.remote.UserApi
import com.tbterminal.app.data.remote.UserFacingErrorMapper
import com.tbterminal.app.data.remote.UserUpdateRequest
import com.tbterminal.app.data.remote.safeApiCall
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface PinUnlockResult {
    data object Unlocked : PinUnlockResult
    data class Failed(
        val message: String,
        val attemptsLeft: Int,
        val lockedUntilEpochMillis: Long? = null,
        val isLocked: Boolean = false
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
    private val userApiProvider: (() -> UserApi)? = null
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
        userApiProvider: (() -> UserApi)? = null
    ) : this(authApiProvider, sessionManager, deviceIdProvider, ioDispatcher, userApiProvider)

    private val authApi: AuthApi by lazy(authApiProvider)
    private val userApi: UserApi? by lazy { userApiProvider?.invoke() }

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

    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> {
        if (!pin.matches(PIN_PATTERN)) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN harus terdiri dari 4 sampai 6 digit")
        }
        return try {
            val response = authApi.unlock(UnlockRequest(pin = pin, deviceId = deviceIdProvider()))
            if (response.isSuccessful) {
                val payload = response.body()
                val status = payload?.data
                if (payload?.success == true && (status == null || status.valid)) {
                    sessionManager.markPinUnlocked()
                    return RepositoryResult.Success(PinUnlockResult.Unlocked)
                }
                if (status != null) {
                    return mapUnlockStatus(payload.code, status)
                }
                return RepositoryResult.Error(
                    code = payload?.code ?: "UNLOCK_FAILED",
                    message = payload?.message ?: payload?.error ?: "PIN salah"
                )
            }

            val rawError = response.errorBody()?.string().orEmpty()
            val parsedStatusResponse = rawError.takeIf(String::isNotBlank)?.let {
                runCatching { unlockJson.decodeFromString<ApiResponse<PinUnlockResponse>>(it) }.getOrNull()
            }
            val parsedData = parsedStatusResponse?.data
            if (parsedData != null) {
                return mapUnlockStatus(parsedStatusResponse.code, parsedData)
            }

            val rootJson = rawError.takeIf(String::isNotBlank)?.let {
                runCatching { unlockJson.parseToJsonElement(it).jsonObject }.getOrNull()
            }
            val dataJson = runCatching { rootJson?.get("data")?.jsonObject }.getOrNull()
            val fallbackCode = parsedStatusResponse?.code
                ?: rootJson?.get("code")?.jsonPrimitive?.contentOrNull
                ?: "HTTP_${response.code()}"
            val fallbackMessage = parsedStatusResponse?.error
                ?: parsedStatusResponse?.message
                ?: rootJson?.get("error")?.jsonPrimitive?.contentOrNull
                ?: rootJson?.get("message")?.jsonPrimitive?.contentOrNull

            if (dataJson != null) {
                val forceLogout = dataJson["force_logout"]?.jsonPrimitive?.booleanOrNull == true ||
                    fallbackCode == "PIN_MAX_ATTEMPTS_EXCEEDED"
                val lockedUntil = dataJson["locked_until"]?.jsonPrimitive?.contentOrNull
                val attemptsLeft = dataJson["attempts_left"]?.jsonPrimitive?.intOrNull ?: 0
                val valid = dataJson["valid"]?.jsonPrimitive?.booleanOrNull == true
                if (forceLogout || lockedUntil != null || dataJson.containsKey("attempts_left") || dataJson.containsKey("valid")) {
                    return mapUnlockStatus(
                        code = fallbackCode,
                        data = PinUnlockResponse(
                            valid = valid,
                            attemptsLeft = attemptsLeft,
                            lockedUntil = lockedUntil,
                            forceLogout = forceLogout
                        )
                    )
                }
            }

            if ((response.code() == 400 && fallbackCode != "INVALID_REQUEST") ||
                response.code() == 422 ||
                fallbackCode in WRONG_PIN_ERROR_CODES
            ) {
                return RepositoryResult.Error(
                    code = fallbackCode,
                    message = "PIN salah. Silakan coba lagi."
                )
            }

            RepositoryResult.Error(
                code = fallbackCode,
                message = UserFacingErrorMapper.message(
                    httpStatus = response.code(),
                    code = fallbackCode,
                    backendMessage = fallbackMessage
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: SocketTimeoutException) {
            RepositoryResult.Error("NETWORK_TIMEOUT", UserFacingErrorMapper.message(code = "NETWORK_TIMEOUT"))
        } catch (_: UnknownHostException) {
            RepositoryResult.Error("NETWORK_UNAVAILABLE", UserFacingErrorMapper.message(code = "NETWORK_UNAVAILABLE"))
        } catch (_: ConnectException) {
            RepositoryResult.Error("CONNECTION_FAILED", UserFacingErrorMapper.message(code = "CONNECTION_FAILED"))
        } catch (_: IOException) {
            RepositoryResult.Error("CONNECTION_INTERRUPTED", UserFacingErrorMapper.message(code = "CONNECTION_INTERRUPTED"))
        } catch (exception: Exception) {
            RepositoryResult.Exception(exception)
        }
    }

    private suspend fun mapUnlockStatus(
        code: String?,
        data: PinUnlockResponse
    ): RepositoryResult<PinUnlockResult> {
        if (data.forceLogout || code == "PIN_MAX_ATTEMPTS_EXCEEDED") {
            withContext(ioDispatcher) {
                sessionManager.clearSession()
            }
            return RepositoryResult.Success(
                PinUnlockResult.ForceLogout("Batas percobaan PIN terlampaui. Silakan login kembali.")
            )
        }
        if (data.valid) {
            sessionManager.markPinUnlocked()
            return RepositoryResult.Success(PinUnlockResult.Unlocked)
        }
        val lockedMillis = data.lockedUntil?.let(::parseIsoEpochMillis)
        val isLocked = lockedMillis != null || code == "PIN_LOCKED"
        return RepositoryResult.Success(
            PinUnlockResult.Failed(
                message = "PIN salah",
                attemptsLeft = data.attemptsLeft.coerceAtLeast(0),
                lockedUntilEpochMillis = lockedMillis,
                isLocked = isLocked
            )
        )
    }

    override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> {
        if (currentPassword.isBlank()) {
            return RepositoryResult.Error("VALIDATION_ERROR", "Kata sandi wajib diisi")
        }
        if (!newPin.matches(PIN_PATTERN)) {
            return RepositoryResult.Error("VALIDATION_ERROR", "PIN baru harus terdiri dari 4 sampai 6 digit")
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
            sessionManager.markPinUnlocked()
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

    private fun parseIsoEpochMillis(rawTimestamp: String): Long? {
        return runCatching { OffsetDateTime.parse(rawTimestamp).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { Instant.parse(rawTimestamp).toEpochMilli() }.getOrNull()
    }

    private companion object {
        val PIN_PATTERN = Regex("\\d{4,6}")
        val USERNAME_PATTERN = Regex("^[a-zA-Z0-9._-]{3,50}$")
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        val WRONG_PIN_ERROR_CODES = setOf("INVALID_PIN", "WRONG_PIN", "PIN_MISMATCH", "UNLOCK_FAILED")
        val unlockJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
