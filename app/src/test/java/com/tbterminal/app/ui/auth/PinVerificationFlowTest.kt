package com.tbterminal.app.ui.auth

import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.CurrentUserDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.LoginResponse
import com.tbterminal.app.data.remote.PinUnlockResponse
import com.tbterminal.app.data.remote.ResetPinRequestDto
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.remote.UserDto
import com.tbterminal.app.data.repository.RemoteAuthRepository
import com.tbterminal.app.data.session.SessionEvent
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PinVerificationFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun wrongPinStaysOnPinScreenKeepsTokenAndShowsRemainingAttempts() = runTest {
        val nowMillis = 1_795_000_000_000L
        val api = FakeUnlockAuthApi {
            errorResponse(
                httpCode = 422,
                code = "INVALID_PIN",
                error = "PIN salah",
                data = PinUnlockResponse(valid = false, attemptsLeft = 4, lockedUntil = null, forceLogout = false)
            )
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo, clockMillis = { nowMillis }, tickIntervalMillis = 100L)

        viewModel.unlock("999999")
        advanceUntilIdle()

        val state = viewModel.unlockState.value as UnlockState.WrongPin
        assertEquals("PIN salah", state.message)
        assertEquals(4, state.attemptsLeft)
        assertTrue(sessionManager.hasAccessToken())
        assertTrue(sessionManager.requiresPinUnlock())
        assertEquals("android-dev-1", api.lastUnlockRequest?.deviceId)
    }

    @Test
    fun http400UnlockWithoutPayloadDataReturnsFriendlyWrongPinMessage() = runTest {
        val api = FakeUnlockAuthApi {
            Response.error(
                400,
                """{"success":false,"error":"Format error"}""".toResponseBody("application/json".toMediaType())
            )
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo)

        viewModel.unlock("999999")
        advanceUntilIdle()

        val state = viewModel.unlockState.value as UnlockState.Error
        assertEquals("PIN salah. Silakan coba lagi.", state.message)
    }

    @Test
    fun http400InvalidRequestSurfacesBackendErrorInsteadOfWrongPin() = runTest {
        val api = FakeUnlockAuthApi {
            Response.error(
                400,
                """{"code":"INVALID_REQUEST","message":"Format request tidak valid","details":null}"""
                    .toResponseBody("application/json".toMediaType())
            )
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo)

        viewModel.unlock("123456")
        advanceUntilIdle()

        val state = viewModel.unlockState.value as UnlockState.Error
        assertEquals("Format request tidak valid", state.message)
    }

    @Test
    fun reachingLimitLocksOutWithCountdownAndEscalatingBackoff() = runTest {
        var nowMillis = 1_795_000_000_000L
        val backoffSchedule = listOf(30L, 60L, 300L, 900L)
        var callIndex = 0
        val api = FakeUnlockAuthApi {
            val seconds = backoffSchedule[callIndex.coerceAtMost(backoffSchedule.lastIndex)]
            callIndex++
            val lockedUntil = Instant.ofEpochMilli(nowMillis + seconds * 1000L).toString()
            errorResponse(
                httpCode = 423,
                code = "PIN_LOCKED",
                error = "PIN salah",
                data = PinUnlockResponse(valid = false, attemptsLeft = 0, lockedUntil = lockedUntil, forceLogout = false)
            )
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo, clockMillis = { nowMillis }, tickIntervalMillis = 100L)

        for (expectedSeconds in backoffSchedule) {
            viewModel.unlock("999999")
            testDispatcher.scheduler.runCurrent()

            val locked = viewModel.unlockState.value as UnlockState.LockedOut
            assertEquals(expectedSeconds, locked.remainingSeconds)
            assertTrue(sessionManager.hasAccessToken())

            // Simulate server lock window expiring
            nowMillis += expectedSeconds * 1000L + 10L
            advanceTimeBy(150L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(UnlockState.Idle, viewModel.unlockState.value)
        }
    }

    @Test
    fun tenthFailureForcesLogoutAndClearsLocalSession() = runTest {
        val api = FakeUnlockAuthApi {
            errorResponse(
                httpCode = 403,
                code = "PIN_MAX_ATTEMPTS_EXCEEDED",
                error = "Batas percobaan PIN terlampaui. Silakan login kembali.",
                data = PinUnlockResponse(valid = false, attemptsLeft = 0, lockedUntil = null, forceLogout = true)
            )
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo)

        viewModel.unlock("999999")
        advanceUntilIdle()

        assertTrue(viewModel.unlockState.value is UnlockState.ForceLogout)
        assertFalse(sessionManager.hasAccessToken())
        assertTrue(viewModel.authState.value is AuthState.Error)
    }

    @Test
    fun validPinUnlocksSessionAndClearsLockState() = runTest {
        var failFirst = true
        val api = FakeUnlockAuthApi {
            if (failFirst) {
                failFirst = false
                errorResponse(
                    httpCode = 422,
                    code = "INVALID_PIN",
                    error = "PIN salah",
                    data = PinUnlockResponse(valid = false, attemptsLeft = 4, lockedUntil = null, forceLogout = false)
                )
            } else {
                Response.success(
                    ApiResponse(
                        success = true,
                        data = PinUnlockResponse(valid = true, attemptsLeft = 5, lockedUntil = null, forceLogout = false),
                        message = "Unlock berhasil"
                    )
                )
            }
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo)

        viewModel.unlock("999999")
        advanceUntilIdle()
        assertTrue(viewModel.unlockState.value is UnlockState.WrongPin)

        viewModel.unlock("654321")
        advanceUntilIdle()
        assertEquals(UnlockState.Success, viewModel.unlockState.value)
        assertTrue(sessionManager.hasAccessToken())
        assertFalse(sessionManager.requiresPinUnlock())
    }

    @Test
    fun forgotPinFlowReauthenticatesWithPasswordAndSetsNewPin() = runTest {
        val api = FakeUnlockAuthApi {
            Response.success(ApiResponse(success = true, data = PinUnlockResponse(valid = true, attemptsLeft = 5)))
        }
        val sessionManager = seededSessionManager()
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo)

        viewModel.startForgotPinFlow()
        assertTrue(viewModel.pendingForgotPinReset.value)

        viewModel.login("kasir1", "secret123")
        advanceUntilIdle()
        assertTrue(viewModel.authState.value is AuthState.Success)
        assertTrue(viewModel.hasVerifiedPasswordForPinReset)

        viewModel.resetPinAfterLogin("258014", "258014")
        advanceUntilIdle()
        assertEquals(ResetPinState.Success, viewModel.resetPinState.value)
        assertFalse(viewModel.pendingForgotPinReset.value)
        assertFalse(viewModel.hasVerifiedPasswordForPinReset)
        assertEquals(ResetPinRequestDto("secret123", "258014", "android-dev-1"), api.lastResetPinRequest)
    }

    @Test
    fun http423PinLockedDisplaysLockedUntilTimestampAndFallsBackToRetryAfter() = runTest(testDispatcher) {
        val nowMillis = 1_795_000_000_000L
        val lockedUntilIso = Instant.ofEpochMilli(nowMillis + 90_000L).toString()
        val apiWithTimestamp = FakeUnlockAuthApi {
            errorResponse(
                httpCode = 423,
                code = "PIN_LOCKED",
                error = "PIN terkunci",
                data = PinUnlockResponse(valid = false, attemptsLeft = 0, lockedUntil = lockedUntilIso, forceLogout = false)
            )
        }
        val sessionManager1 = seededSessionManager()
        val repo1 = RemoteAuthRepository(sessionManager1, { apiWithTimestamp }, { "android-dev-1" }, testDispatcher, clockMillis = { nowMillis })
        val vm1 = AuthViewModel(repo1, clockMillis = { nowMillis }, tickIntervalMillis = 100L)

        vm1.unlock("999999")
        testDispatcher.scheduler.runCurrent()

        val locked1 = vm1.unlockState.value as UnlockState.LockedOut
        assertEquals(nowMillis + 90_000L, locked1.lockedUntilEpochMillis)
        assertEquals(90L, locked1.remainingSeconds)
        val expectedTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            .format(Instant.ofEpochMilli(locked1.lockedUntilEpochMillis).atZone(ZoneOffset.UTC).toLocalTime())
        assertEquals(
            "PIN terkunci hingga $expectedTime · 01:30",
            formatPinLockoutMessage(locked1.lockedUntilEpochMillis, locked1.remainingSeconds, ZoneOffset.UTC)
        )
        vm1.forceClearUnlockState()

        val apiWithRetryAfterOnly = FakeUnlockAuthApi {
            val raw = Response.error<ApiResponse<PinUnlockResponse>>(
                423,
                """{"success":false,"code":"PIN_LOCKED","message":"Locked"}"""
                    .toResponseBody("application/json".toMediaType())
            ).raw().newBuilder()
                .header("Retry-After", "45")
                .build()
            Response.error(
                """{"success":false,"code":"PIN_LOCKED","message":"Locked"}"""
                    .toResponseBody("application/json".toMediaType()),
                raw
            )
        }
        val sessionManager2 = seededSessionManager()
        val repo2 = RemoteAuthRepository(sessionManager2, { apiWithRetryAfterOnly }, { "android-dev-1" }, testDispatcher, clockMillis = { nowMillis })
        val vm2 = AuthViewModel(repo2, clockMillis = { nowMillis }, tickIntervalMillis = 100L)

        vm2.unlock("999999")
        testDispatcher.scheduler.runCurrent()

        val locked2 = vm2.unlockState.value as UnlockState.LockedOut
        assertEquals(nowMillis + 45_000L, locked2.lockedUntilEpochMillis)
        assertEquals(45L, locked2.remainingSeconds)
        vm2.forceClearUnlockState()
    }

    @Test
    fun http401MaxAttemptsOnUnlockRevokesSessionAndEmitsUnauthorized() = runTest(testDispatcher) {
        val api = FakeUnlockAuthApi {
            Response.error(
                401,
                """{"success":false,"code":"UNAUTHORIZED","message":"Batas percobaan PIN terlampaui. Silakan login kembali."}"""
                    .toResponseBody("application/json".toMediaType())
            )
        }
        val sessionManager = seededSessionManager()
        val events = mutableListOf<SessionEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            events.add(sessionManager.events.first())
        }
        val repo = RemoteAuthRepository(sessionManager, { api }, { "android-dev-1" }, testDispatcher)
        val viewModel = AuthViewModel(repo)

        viewModel.unlock("999999")
        advanceUntilIdle()
        job.join()

        assertTrue(viewModel.unlockState.value is UnlockState.ForceLogout)
        assertFalse(sessionManager.hasAccessToken())
        assertEquals(
            listOf(SessionEvent.Unauthorized("Batas percobaan PIN terlampaui. Silakan login kembali.")),
            events
        )
    }

    private fun seededSessionManager(): SessionManager {
        val store = InMemoryPinTokenStore().apply {
            saveTokens("access-token", "refresh-token")
            saveSessionUser(SessionUser("Kasir Satu", "kasir", true, "2026-09-01", null, userId = "u1"))
        }
        return SessionManager(store).apply { lockForResume() }
    }

    private fun errorResponse(
        httpCode: Int,
        code: String,
        error: String,
        data: PinUnlockResponse
    ): Response<ApiResponse<PinUnlockResponse>> {
        val payload = testJson.encodeToString(
            ApiResponse.serializer(PinUnlockResponse.serializer()),
            ApiResponse(success = false, data = data, error = error, code = code)
        )
        return Response.error(httpCode, payload.toResponseBody("application/json".toMediaType()))
    }

    private companion object {
        val testJson = Json { encodeDefaults = true }
    }
}

private class FakeUnlockAuthApi(
    private val unlockResponder: () -> Response<ApiResponse<PinUnlockResponse>>
) : AuthApi {
    var lastUnlockRequest: UnlockRequest? = null
    var lastResetPinRequest: ResetPinRequestDto? = null

    override suspend fun login(request: LoginRequest): Response<ApiResponse<LoginResponse>> {
        return Response.success(
            ApiResponse(
                success = true,
                data = LoginResponse(
                    token = "new-access",
                    refreshToken = "new-refresh",
                    user = UserDto(
                        id = "u1",
                        username = request.username,
                        name = "Kasir Satu",
                        role = "kasir",
                        isActive = true,
                        email = null,
                        joinedAt = "2026-01-01"
                    )
                )
            )
        )
    }

    override suspend fun unlock(request: UnlockRequest): Response<ApiResponse<PinUnlockResponse>> {
        lastUnlockRequest = request
        return unlockResponder()
    }

    override suspend fun resetPinWithPassword(request: ResetPinRequestDto): Response<ApiResponse<Unit>> {
        lastResetPinRequest = request
        return Response.success(ApiResponse(success = true, data = Unit))
    }

    override suspend fun getMe(): Response<ApiResponse<CurrentUserDto>> = error("not used")
    override suspend fun changeMyPassword(request: ChangePasswordRequestDto): Response<ApiResponse<Unit>> = error("not used")
    override suspend fun changeMyPin(request: ChangePinRequestDto): Response<ApiResponse<Unit>> = error("not used")
}

private class InMemoryPinTokenStore : TokenStore {
    private var access: String? = null
    private var refresh: String? = null
    private var user: SessionUser? = null

    override fun readAccessToken(): String? = access
    override fun readRefreshToken(): String? = refresh
    override fun saveAccessToken(token: String) { access = token }
    override fun saveTokens(accessToken: String, refreshToken: String) {
        access = accessToken
        refresh = refreshToken
    }
    override fun readSessionUser(): SessionUser? = user
    override fun saveSessionUser(user: SessionUser) { this.user = user }
    override fun clearAccessToken() {
        access = null
        refresh = null
        user = null
    }
}
