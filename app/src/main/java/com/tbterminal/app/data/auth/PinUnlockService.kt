package com.tbterminal.app.data.auth

import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.PinUnlockResponse
import com.tbterminal.app.data.remote.ServerUnavailabilityClassifier
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.remote.UserFacingErrorMapper
import com.tbterminal.app.data.remote.parseRetryAfterSeconds
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.sync.isSameBootSession
import com.tbterminal.app.data.validation.CredentialValidator
import java.io.IOException
import java.net.ProtocolException
import java.time.Instant
import java.time.OffsetDateTime
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

interface PinUnlockService {
    suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult>
    suspend fun recordOnlineVerifiedPin(pin: String)
    fun clearOfflineState()
}

class DefaultPinUnlockService(
    private val authApiProvider: () -> AuthApi,
    private val sessionManager: SessionManager,
    private val deviceIdProvider: () -> String = { "android-unknown" },
    private val verifierStore: OfflinePinVerifierStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val elapsedRealtimeMillis: (() -> Long)? = null,
    private val bootCountProvider: () -> Long? = { 1L },
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : PinUnlockService {
    private val resolvedElapsedRealtimeMillis: () -> Long = elapsedRealtimeMillis ?: clockMillis

    private val authApi: AuthApi by lazy(authApiProvider)
    private val offlineMutex = Mutex()

    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> {
        if (!CredentialValidator.isValidPin(pin)) {
            return RepositoryResult.Error(
                "VALIDATION_ERROR",
                "PIN harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka",
            )
        }

        val response = try {
            authApi.unlock(UnlockRequest(pin = pin, deviceId = deviceIdProvider()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            if (isOfflinePreResponseFailure(io)) {
                return unlockOffline(pin)
            }
            return RepositoryResult.Error(
                "CONNECTION_INTERRUPTED",
                UserFacingErrorMapper.message(code = "CONNECTION_INTERRUPTED"),
            )
        } catch (exception: Exception) {
            return RepositoryResult.Exception(exception)
        }

        return handleOnlineResponse(response, pin)
    }

    override suspend fun recordOnlineVerifiedPin(pin: String) {
        if (!CredentialValidator.isValidPin(pin)) return
        val userKey = resolvePinVerifierUserId(sessionManager.readSessionUser()) ?: return
        offlineMutex.withLock {
            withContext(ioDispatcher) {
                verifierStore.saveVerifier(
                    userId = userKey,
                    pin = pin,
                    verifiedAtMillis = clockMillis(),
                    verifiedAtElapsedMillis = resolvedElapsedRealtimeMillis(),
                    verifiedAtBootCount = bootCountProvider(),
                )
                verifierStore.clearLockout(userKey)
            }
        }
    }

    override fun clearOfflineState() {
        verifierStore.clearAll()
    }

    private suspend fun handleOnlineResponse(
        response: Response<ApiResponse<PinUnlockResponse>>,
        submittedPin: String,
    ): RepositoryResult<PinUnlockResult> {
        if (response.isSuccessful) {
            val payload = response.body()
            val status = payload?.data
            if (payload?.success == true && (status == null || status.valid)) {
                recordOnlineVerifiedPin(submittedPin)
                sessionManager.markPinUnlocked()
                return RepositoryResult.Success(PinUnlockResult.Unlocked)
            }
            if (status != null) {
                return mapUnlockStatus(payload.code, status, validPin = null)
            }
            return RepositoryResult.Error(
                code = payload?.code ?: "UNLOCK_FAILED",
                message = payload?.message ?: payload?.error ?: "PIN salah",
            )
        }

        if (ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(response.code())) {
            return unlockOffline(submittedPin)
        }

        val rawError = try {
            response.errorBody()?.string().orEmpty()
        } catch (_: IOException) {
            ""
        }
        val retryAfterSeconds = parseRetryAfterSeconds(response.headers()["Retry-After"], clockMillis)
        val parsedStatusResponse = rawError.takeIf(String::isNotBlank)?.let {
            runCatching { unlockJson.decodeFromString<ApiResponse<PinUnlockResponse>>(it) }.getOrNull()
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

        val parsedData = parsedStatusResponse?.data
        if (parsedData != null) {
            val revoked401 = response.code() == 401 &&
                fallbackCode !in WRONG_PIN_ERROR_CODES &&
                fallbackCode != "PIN_LOCKED"
            return mapUnlockStatus(
                code = fallbackCode,
                data = if (revoked401) parsedData.copy(forceLogout = true) else parsedData,
                retryAfterSeconds = retryAfterSeconds,
                validPin = submittedPin,
            )
        }

        val lockedUntil = dataJson?.get("locked_until")?.jsonPrimitive?.contentOrNull
            ?: dataJson?.get("lockedUntil")?.jsonPrimitive?.contentOrNull
            ?: rootJson?.get("locked_until")?.jsonPrimitive?.contentOrNull
            ?: rootJson?.get("lockedUntil")?.jsonPrimitive?.contentOrNull
        val attemptsLeft = dataJson?.get("attempts_left")?.jsonPrimitive?.intOrNull
            ?: dataJson?.get("attemptsLeft")?.jsonPrimitive?.intOrNull
            ?: 0
        val valid = dataJson?.get("valid")?.jsonPrimitive?.booleanOrNull == true
        val forceLogout = dataJson?.get("force_logout")?.jsonPrimitive?.booleanOrNull == true ||
            dataJson?.get("forceLogout")?.jsonPrimitive?.booleanOrNull == true ||
            fallbackCode == "PIN_MAX_ATTEMPTS_EXCEEDED" ||
            (response.code() == 401 && fallbackCode !in WRONG_PIN_ERROR_CODES && fallbackCode != "PIN_LOCKED")

        if (forceLogout ||
            lockedUntil != null ||
            response.code() == 423 ||
            fallbackCode == "PIN_LOCKED" ||
            (dataJson != null && (dataJson.containsKey("attempts_left") || dataJson.containsKey("valid")))
        ) {
            return mapUnlockStatus(
                code = if (response.code() == 423 && fallbackCode.startsWith("HTTP_")) "PIN_LOCKED" else fallbackCode,
                data = PinUnlockResponse(
                    valid = valid,
                    attemptsLeft = attemptsLeft,
                    lockedUntil = lockedUntil,
                    forceLogout = forceLogout,
                ),
                retryAfterSeconds = retryAfterSeconds,
                validPin = submittedPin,
            )
        }

        if ((response.code() == 400 && fallbackCode != "INVALID_REQUEST") ||
            response.code() == 422 ||
            fallbackCode in WRONG_PIN_ERROR_CODES
        ) {
            return RepositoryResult.Error(
                code = fallbackCode,
                message = "PIN salah. Silakan coba lagi.",
                retryAfterSeconds = retryAfterSeconds,
            )
        }

        return RepositoryResult.Error(
            code = fallbackCode,
            message = UserFacingErrorMapper.message(
                httpStatus = response.code(),
                code = fallbackCode,
                backendMessage = fallbackMessage,
                retryAfterSeconds = retryAfterSeconds,
            ),
            retryAfterSeconds = retryAfterSeconds,
        )
    }

    private suspend fun mapUnlockStatus(
        code: String?,
        data: PinUnlockResponse,
        retryAfterSeconds: Long? = null,
        validPin: String? = null,
    ): RepositoryResult<PinUnlockResult> {
        if (data.forceLogout || code == "PIN_MAX_ATTEMPTS_EXCEEDED") {
            val message = "Batas percobaan PIN terlampaui. Silakan login kembali."
            withContext(ioDispatcher) {
                verifierStore.clearAll()
                sessionManager.handleUnauthorized(message)
            }
            return RepositoryResult.Success(PinUnlockResult.ForceLogout(message))
        }
        if (data.valid) {
            if (validPin != null) {
                recordOnlineVerifiedPin(validPin)
            }
            sessionManager.markPinUnlocked()
            return RepositoryResult.Success(PinUnlockResult.Unlocked)
        }
        val lockedMillis = data.lockedUntil?.let(::parseIsoEpochMillis)
            ?: retryAfterSeconds?.let { clockMillis() + it * 1000L }
        val isLocked = lockedMillis != null || code == "PIN_LOCKED"
        val message = if (isLocked) {
            UserFacingErrorMapper.message(
                httpStatus = 423,
                code = "PIN_LOCKED",
                retryAfterSeconds = retryAfterSeconds,
            )
        } else {
            "PIN salah"
        }
        return RepositoryResult.Success(
            PinUnlockResult.Failed(
                message = message,
                attemptsLeft = data.attemptsLeft.coerceAtLeast(0),
                lockedUntilEpochMillis = lockedMillis,
                isLocked = isLocked,
            )
        )
    }

    private suspend fun unlockOffline(pin: String): RepositoryResult<PinUnlockResult> {
        return offlineMutex.withLock {
            withContext(ioDispatcher) {
                val sessionUser = sessionManager.readSessionUser()
                val userKey = resolvePinVerifierUserId(sessionUser)
                    ?: return@withContext RepositoryResult.Error(
                        code = OFFLINE_VERIFIER_UNAVAILABLE_CODE,
                        message = OFFLINE_VERIFIER_REQUIRED_MESSAGE,
                    )

                val verifier = verifierStore.readVerifier(userKey)
                    ?: return@withContext RepositoryResult.Error(
                        code = OFFLINE_VERIFIER_UNAVAILABLE_CODE,
                        message = OFFLINE_VERIFIER_REQUIRED_MESSAGE,
                    )

                val verifiedAtElapsed = verifier.verifiedAtElapsedMillis
                val lastHighWaterWall = verifier.highWaterWallClockMillis
                if (verifiedAtElapsed == null || lastHighWaterWall == null) {
                    return@withContext RepositoryResult.Error(
                        code = OFFLINE_VERIFIER_EXPIRED_CODE,
                        message = OFFLINE_VERIFIER_REQUIRED_MESSAGE,
                    )
                }

                val wallNow = clockMillis()
                val elapsedNow = resolvedElapsedRealtimeMillis()
                val bootNow = bootCountProvider()
                val sameBootAsVerified = isSameBootSession(verifier.verifiedAtBootCount, bootNow) &&
                    elapsedNow >= verifiedAtElapsed
                val highWaterElapsed = verifier.highWaterElapsedMillis
                val sameBootAsHighWater = highWaterElapsed != null &&
                    isSameBootSession(verifier.highWaterBootCount, bootNow) &&
                    elapsedNow >= highWaterElapsed

                val wallAge = wallNow - verifier.lastOnlineVerifiedAtMillis
                val monotonicAgeFromVerified = if (sameBootAsVerified) elapsedNow - verifiedAtElapsed else null
                val highWaterWallDelta = wallNow - lastHighWaterWall
                val highWaterMonotonicDelta = if (sameBootAsHighWater) elapsedNow - highWaterElapsed!! else null

                val backwardTampered =
                    wallNow < verifier.lastOnlineVerifiedAtMillis - CLOCK_BACKWARD_TOLERANCE_MILLIS ||
                    wallNow < lastHighWaterWall - CLOCK_BACKWARD_TOLERANCE_MILLIS ||
                    (monotonicAgeFromVerified != null &&
                        wallAge + CLOCK_BACKWARD_TOLERANCE_MILLIS < monotonicAgeFromVerified) ||
                    (highWaterMonotonicDelta != null &&
                        highWaterWallDelta + CLOCK_BACKWARD_TOLERANCE_MILLIS < highWaterMonotonicDelta)

                if (backwardTampered) {
                    verifierStore.clearAll()
                    return@withContext RepositoryResult.Error(
                        code = OFFLINE_VERIFIER_EXPIRED_CODE,
                        message = OFFLINE_VERIFIER_REQUIRED_MESSAGE,
                    )
                }

                val segmentElapsed = if (sameBootAsHighWater) {
                    highWaterMonotonicDelta!!
                } else {
                    elapsedNow.coerceAtLeast(0L)
                }
                val effectiveElapsedAge = verifier.accumulatedOfflineElapsedMillis + segmentElapsed
                val expired = wallAge > OfflinePinVerifierStore.VERIFIER_TTL_MILLIS ||
                    (monotonicAgeFromVerified != null && monotonicAgeFromVerified > OfflinePinVerifierStore.VERIFIER_TTL_MILLIS) ||
                    effectiveElapsedAge > OfflinePinVerifierStore.VERIFIER_TTL_MILLIS
                if (expired) {
                    return@withContext RepositoryResult.Error(
                        code = OFFLINE_VERIFIER_EXPIRED_CODE,
                        message = OFFLINE_VERIFIER_REQUIRED_MESSAGE,
                    )
                }

                verifierStore.updateVerifierRecord(
                    verifier.copy(
                        highWaterWallClockMillis = maxOf(lastHighWaterWall, wallNow),
                        highWaterElapsedMillis = elapsedNow,
                        highWaterBootCount = bootNow,
                        accumulatedOfflineElapsedMillis = effectiveElapsedAge,
                    )
                )

                val policy = offlineLockoutPolicyForRole(sessionUser?.role)
                val existingLockout = verifierStore.readLockout(userKey)
                val hasPersistedLock = existingLockout != null &&
                    (existingLockout.lockedUntilElapsedMillis != null || existingLockout.lockedUntilEpochMillis != null)
                if (hasPersistedLock) {
                    val lockedUntilElapsed = existingLockout!!.lockedUntilElapsedMillis
                    val lockDuration = existingLockout.lockDurationMillis?.takeIf { it > 0L }
                    val fallbackDurationMillis = lockDuration
                        ?: policyDurationMillisForFailures(policy, existingLockout.failedAttempts)
                    val sameBootLock = lockedUntilElapsed != null &&
                        lockDuration != null &&
                        isSameBootSession(existingLockout.lockBootCount, bootNow) &&
                        elapsedNow >= (lockedUntilElapsed - lockDuration)
                    if (sameBootLock) {
                        val remainingMillis = lockedUntilElapsed!! - elapsedNow
                        if (remainingMillis > 0L) {
                            val remainingSeconds = (remainingMillis + 999L) / 1000L
                            val displayLockedUntilWall = wallNow + remainingMillis
                            return@withContext RepositoryResult.Success(
                                PinUnlockResult.Failed(
                                    message = UserFacingErrorMapper.message(
                                        httpStatus = 423,
                                        code = "PIN_LOCKED",
                                        retryAfterSeconds = remainingSeconds,
                                    ),
                                    attemptsLeft = 0,
                                    lockedUntilEpochMillis = displayLockedUntilWall,
                                    isLocked = true,
                                )
                            )
                        }
                    } else {
                        // Opsi B: reboot during active lockout or legacy lockout without monotonic anchor -> re-arm full duration on current boot.
                        val rearmedUntilElapsed = elapsedNow + fallbackDurationMillis
                        val rearmedUntilWall = wallNow + fallbackDurationMillis
                        val rearmedSeconds = (fallbackDurationMillis + 999L) / 1000L
                        verifierStore.saveLockout(
                            existingLockout.copy(
                                lockedUntilEpochMillis = rearmedUntilWall,
                                lockedUntilElapsedMillis = rearmedUntilElapsed,
                                lockDurationMillis = fallbackDurationMillis,
                                lockBootCount = bootNow,
                            )
                        )
                        return@withContext RepositoryResult.Success(
                            PinUnlockResult.Failed(
                                message = UserFacingErrorMapper.message(
                                    httpStatus = 423,
                                    code = "PIN_LOCKED",
                                    retryAfterSeconds = rearmedSeconds,
                                ),
                                attemptsLeft = 0,
                                lockedUntilEpochMillis = rearmedUntilWall,
                                isLocked = true,
                            )
                        )
                    }
                }

                if (verifierStore.verifyPin(verifier, pin)) {
                    verifierStore.clearLockout(userKey)
                    sessionManager.markPinUnlocked()
                    return@withContext RepositoryResult.Success(PinUnlockResult.Unlocked)
                }

                val nextFailures = (existingLockout?.failedAttempts ?: 0) + 1
                if (nextFailures >= OFFLINE_MAX_FAILED_ATTEMPTS) {
                    verifierStore.clearAll()
                    return@withContext RepositoryResult.Error(
                        code = OFFLINE_MAX_ATTEMPTS_CODE,
                        message = OFFLINE_VERIFIER_REQUIRED_MESSAGE,
                    )
                }

                if (nextFailures >= policy.threshold) {
                    val durationMillis = policyDurationMillisForFailures(policy, nextFailures)
                    val durationSeconds = durationMillis / 1000L
                    val lockedUntilWall = wallNow + durationMillis
                    val lockedUntilElapsed = elapsedNow + durationMillis
                    verifierStore.saveLockout(
                        OfflinePinLockoutRecord(
                            userId = userKey,
                            failedAttempts = nextFailures,
                            lockedUntilEpochMillis = lockedUntilWall,
                            lockedUntilElapsedMillis = lockedUntilElapsed,
                            lockDurationMillis = durationMillis,
                            lockBootCount = bootNow,
                        )
                    )
                    return@withContext RepositoryResult.Success(
                        PinUnlockResult.Failed(
                            message = UserFacingErrorMapper.message(
                                httpStatus = 423,
                                code = "PIN_LOCKED",
                                retryAfterSeconds = durationSeconds,
                            ),
                            attemptsLeft = 0,
                            lockedUntilEpochMillis = lockedUntilWall,
                            isLocked = true,
                        )
                    )
                }

                verifierStore.saveLockout(
                    OfflinePinLockoutRecord(
                        userId = userKey,
                        failedAttempts = nextFailures,
                        lockedUntilEpochMillis = null,
                        lockedUntilElapsedMillis = null,
                        lockDurationMillis = null,
                        lockBootCount = null,
                    )
                )
                return@withContext RepositoryResult.Success(
                    PinUnlockResult.Failed(
                        message = "PIN salah",
                        attemptsLeft = (policy.threshold - nextFailures).coerceAtLeast(0),
                        lockedUntilEpochMillis = null,
                        isLocked = false,
                    )
                )
            }
        }
    }

    private fun isOfflinePreResponseFailure(error: IOException): Boolean {
        return error !is SSLException && error !is ProtocolException
    }

    private fun offlineLockoutPolicyForRole(role: String?): OfflineRoleLockoutPolicy {
        return when (role?.trim()?.uppercase()) {
            "OWNER", "ADMIN" -> OWNER_ADMIN_LOCKOUT_POLICY
            else -> CASHIER_LOCKOUT_POLICY
        }
    }

    private fun policyDurationMillisForFailures(
        policy: OfflineRoleLockoutPolicy,
        failedAttempts: Int,
    ): Long {
        val lockIndex = (failedAttempts - policy.threshold)
            .coerceAtLeast(0)
            .coerceAtMost(policy.lockoutSeconds.lastIndex)
        return policy.lockoutSeconds[lockIndex] * 1000L
    }

    private fun parseIsoEpochMillis(rawTimestamp: String): Long? {
        return runCatching { OffsetDateTime.parse(rawTimestamp).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { Instant.parse(rawTimestamp).toEpochMilli() }.getOrNull()
    }

    private data class OfflineRoleLockoutPolicy(
        val threshold: Int,
        val lockoutSeconds: LongArray,
    )

    companion object {
        const val OFFLINE_VERIFIER_REQUIRED_MESSAGE = "Butuh koneksi untuk membuka kunci"
        const val OFFLINE_MAX_FAILED_ATTEMPTS = 10
        const val CLOCK_BACKWARD_TOLERANCE_MILLIS = 5L * 60L * 1000L
        private const val OFFLINE_VERIFIER_UNAVAILABLE_CODE = "OFFLINE_VERIFIER_UNAVAILABLE"
        private const val OFFLINE_VERIFIER_EXPIRED_CODE = "OFFLINE_VERIFIER_EXPIRED"
        private const val OFFLINE_MAX_ATTEMPTS_CODE = "OFFLINE_MAX_ATTEMPTS"

        private val CASHIER_LOCKOUT_POLICY = OfflineRoleLockoutPolicy(
            threshold = 5,
            lockoutSeconds = longArrayOf(30L, 60L, 300L, 900L),
        )
        private val OWNER_ADMIN_LOCKOUT_POLICY = OfflineRoleLockoutPolicy(
            threshold = 3,
            lockoutSeconds = longArrayOf(60L, 300L, 900L, 1800L),
        )
        private val WRONG_PIN_ERROR_CODES = setOf("INVALID_PIN", "WRONG_PIN", "PIN_MISMATCH", "UNLOCK_FAILED")
        private val unlockJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

internal fun resolvePinVerifierUserId(user: SessionUser?): String? {
    if (user == null) return null
    return user.userId?.takeIf(String::isNotBlank)
        ?: user.username?.takeIf(String::isNotBlank)
}
