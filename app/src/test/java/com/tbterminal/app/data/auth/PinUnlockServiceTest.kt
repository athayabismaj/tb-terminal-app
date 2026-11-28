package com.tbterminal.app.data.auth

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
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RemoteAuthRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PinUnlockServiceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun onlineValidPin_savesEncryptedVerifierWithPbkdf2And16ByteSalt_neverStoresRawPin() = runTest(testDispatcher) {
        var nowMillis = 1_800_000_000_000L
        val kvStore = InMemoryKvStore()
        val crypto = AesGcmJvmTestCrypto()
        val verifierStore = EncryptedOfflinePinVerifierStore(
            keyValueStore = kvStore,
            crypto = crypto,
            pbkdf2Iterations = OfflinePinVerifierStore.DEFAULT_PBKDF2_ITERATIONS,
        )
        val sessionManager = seededSessionManager(userId = "usr-1", role = "KASIR")
        val api = StubUnlockAuthApi {
            Response.success(ApiResponse(success = true, data = PinUnlockResponse(valid = true, attemptsLeft = 5)))
        }
        val service = DefaultPinUnlockService(
            authApiProvider = { api },
            sessionManager = sessionManager,
            deviceIdProvider = { "android-abc" },
            verifierStore = verifierStore,
            ioDispatcher = testDispatcher,
            clockMillis = { nowMillis },
        )

        val result = service.unlock("258014")
        assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), result)
        assertFalse(sessionManager.requiresPinUnlock())
        assertEquals("android-abc", api.lastUnlockRequest?.deviceId)

        val rawStored = kvStore.dumpAllValues().joinToString("|")
        assertFalse("Raw PIN must never appear in stored payload", rawStored.contains("258014"))
        assertTrue(rawStored.startsWith("v1:"))

        val record = verifierStore.readVerifier("usr-1")!!
        assertEquals(OfflinePinVerifierStore.DEFAULT_PBKDF2_ITERATIONS, record.iterations)
        assertEquals(16, Base64.getDecoder().decode(record.saltBase64).size)
        assertEquals(nowMillis, record.lastOnlineVerifiedAtMillis)
        assertTrue(verifierStore.verifyPin(record, "258014"))
        assertFalse(verifierStore.verifyPin(record, "258015"))
    }

    @Test
    fun serverErrorResponses_neverFallbackToValidOfflineVerifier() = runTest(testDispatcher) {
        var nowMillis = 1_800_000_000_000L
        val kvStore = InMemoryKvStore()
        val verifierStore = EncryptedOfflinePinVerifierStore(kvStore, AesGcmJvmTestCrypto(), pbkdf2Iterations = 1_000)
        verifierStore.saveVerifier("usr-1", "123456", nowMillis, verifiedAtElapsedMillis = nowMillis, verifiedAtBootCount = 1L)

        // 1. Server returns INVALID_PIN (422) even if entered PIN matches stale offline verifier
        val session1 = seededSessionManager(userId = "usr-1", role = "KASIR")
        val invalidPinApi = StubUnlockAuthApi {
            Response.error(
                422,
                """{"success":false,"code":"INVALID_PIN","error":"PIN salah","data":{"valid":false,"attempts_left":4}}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val service1 = DefaultPinUnlockService({ invalidPinApi }, session1, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val invalidRes = service1.unlock("123456") as RepositoryResult.Success
        val failed = invalidRes.data as PinUnlockResult.Failed
        assertFalse(failed.isLocked)
        assertEquals(4, failed.attemptsLeft)
        assertTrue(session1.requiresPinUnlock())

        // 2. Server returns HTTP 500 -> does NOT fall back to offline
        val session2 = seededSessionManager(userId = "usr-1", role = "KASIR")
        val http500Api = StubUnlockAuthApi {
            Response.error(500, """{"code":"INTERNAL_ERROR"}""".toResponseBody("application/json".toMediaType()))
        }
        val service2 = DefaultPinUnlockService({ http500Api }, session2, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val errRes = service2.unlock("123456")
        assertTrue(errRes is RepositoryResult.Error)
        assertTrue(session2.requiresPinUnlock())

        // 3. Server returns HTTP 423 PIN_LOCKED -> returns lockedUntil and does NOT fall back
        val session3 = seededSessionManager(userId = "usr-1", role = "KASIR")
        val lockedUntilIso = Instant.ofEpochMilli(nowMillis + 60_000L).toString()
        val lockedApi = StubUnlockAuthApi {
            Response.error(
                423,
                """{"success":false,"code":"PIN_LOCKED","error":"Terkunci","data":{"valid":false,"attempts_left":0,"locked_until":"$lockedUntilIso"}}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val service3 = DefaultPinUnlockService({ lockedApi }, session3, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val lockedRes = (service3.unlock("123456") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(lockedRes.isLocked)
        assertEquals(nowMillis + 60_000L, lockedRes.lockedUntilEpochMillis)
        assertEquals(60_000L, lockedRes.remainingLockMillis)
        assertTrue(session3.requiresPinUnlock())

        // 4. Server returns 401 max attempts -> revokes session AND deletes verifier
        val session4 = seededSessionManager(userId = "usr-1", role = "KASIR")
        val revokedApi = StubUnlockAuthApi {
            Response.error(
                401,
                """{"success":false,"code":"UNAUTHORIZED","message":"Batas percobaan PIN terlampaui. Silakan login kembali."}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val service4 = DefaultPinUnlockService({ revokedApi }, session4, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val forceLogoutRes = (service4.unlock("123456") as RepositoryResult.Success).data
        assertTrue(forceLogoutRes is PinUnlockResult.ForceLogout)
        assertFalse(session4.hasAccessToken())
        assertNull(verifierStore.readVerifier("usr-1"))
    }

    @Test
    fun offlineValidVerifier_unlocksOnPreResponseNetworkErrors_andRejectsMissingOrExpired72hVerifier() = runTest(testDispatcher) {
        var nowMillis = 1_800_000_000_000L
        val kvStore = InMemoryKvStore()
        val crypto = AesGcmJvmTestCrypto()
        val verifierStore = EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)

        val networkExceptions = listOf(
            UnknownHostException("offline"),
            ConnectException("unreachable"),
            SocketTimeoutException("timed out"),
        )

        // Without verifier -> requires connection
        networkExceptions.forEach { ex ->
            val session = seededSessionManager(userId = "usr-1", role = "KASIR")
            val api = StubUnlockAuthApi { throw ex }
            val service = DefaultPinUnlockService({ api }, session, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
            val res = service.unlock("123456") as RepositoryResult.Error
            assertEquals("Butuh koneksi untuk membuka kunci", res.message)
            assertTrue(session.requiresPinUnlock())
        }

        // Seed verifier at t=0
        verifierStore.saveVerifier("usr-1", "123456", nowMillis, verifiedAtElapsedMillis = nowMillis, verifiedAtBootCount = 1L)

        // Wrong user cannot use usr-1's verifier
        val otherSession = seededSessionManager(userId = "usr-other", role = "KASIR")
        val offlineApi = StubUnlockAuthApi { throw UnknownHostException("offline") }
        val otherService = DefaultPinUnlockService({ offlineApi }, otherSession, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val otherRes = otherService.unlock("123456") as RepositoryResult.Error
        assertEquals("Butuh koneksi untuk membuka kunci", otherRes.message)

        // Exactly at 72h boundary -> still valid
        nowMillis += OfflinePinVerifierStore.VERIFIER_TTL_MILLIS
        val validSession = seededSessionManager(userId = "usr-1", role = "KASIR")
        val validService = DefaultPinUnlockService({ offlineApi }, validSession, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), validService.unlock("123456"))
        assertFalse(validSession.requiresPinUnlock())

        // Past 72h (+1ms) -> rejected
        nowMillis += 1L
        val expiredSession = seededSessionManager(userId = "usr-1", role = "KASIR")
        val expiredService = DefaultPinUnlockService({ offlineApi }, expiredSession, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val expiredRes = expiredService.unlock("123456") as RepositoryResult.Error
        assertEquals("Butuh koneksi untuk membuka kunci", expiredRes.message)
        assertTrue(expiredSession.requiresPinUnlock())
    }

    @Test
    fun offlineKasirWrongPin_follows5Then30_60_300_900_persistsAcrossRestarts_andDeletesVerifierAt10th() = runTest(testDispatcher) {
        var nowMillis = 1_800_000_000_000L
        val kvStore = InMemoryKvStore()
        val crypto = AesGcmJvmTestCrypto()
        val verifiedAt = nowMillis
        EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)
            .saveVerifier("kasir-1", "654321", verifiedAt, verifiedAtElapsedMillis = verifiedAt, verifiedAtBootCount = 1L)

        fun newServiceAfterRestart(session: SessionManager): DefaultPinUnlockService {
            val freshStore = EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)
            return DefaultPinUnlockService(
                authApiProvider = { StubUnlockAuthApi { throw ConnectException("offline") } },
                sessionManager = session,
                deviceIdProvider = { "android-dev" },
                verifierStore = freshStore,
                ioDispatcher = testDispatcher,
                clockMillis = { nowMillis },
            )
        }

        // Attempts 1..4 -> wrong PIN with decreasing attemptsLeft (4,3,2,1)
        for (attempt in 1..4) {
            val session = seededSessionManager(userId = "kasir-1", role = "KASIR")
            val res = (newServiceAfterRestart(session).unlock("000000") as RepositoryResult.Success).data as PinUnlockResult.Failed
            assertFalse(res.isLocked)
            assertEquals(5 - attempt, res.attemptsLeft)
        }

        // Escalating locks at failures 5, 6, 7, 8, 9 -> 30s, 60s, 300s, 900s, 900s
        val expectedLockSeconds = listOf(30L, 60L, 300L, 900L, 900L)
        for (durationSeconds in expectedLockSeconds) {
            val session = seededSessionManager(userId = "kasir-1", role = "KASIR")
            val res = (newServiceAfterRestart(session).unlock("000000") as RepositoryResult.Success).data as PinUnlockResult.Failed
            assertTrue(res.isLocked)
            assertEquals(nowMillis + durationSeconds * 1000L, res.lockedUntilEpochMillis)

            // Attempting correct PIN while locked still stays locked across restart
            val duringLock = (newServiceAfterRestart(session).unlock("654321") as RepositoryResult.Success).data as PinUnlockResult.Failed
            assertTrue(duringLock.isLocked)
            assertEquals(nowMillis + durationSeconds * 1000L, duringLock.lockedUntilEpochMillis)

            // Advance just past lock window
            nowMillis += durationSeconds * 1000L + 1L
        }

        // 10th failure deletes verifier and requires online connection
        val finalSession = seededSessionManager(userId = "kasir-1", role = "KASIR")
        val tenthRes = newServiceAfterRestart(finalSession).unlock("000000") as RepositoryResult.Error
        assertEquals("Butuh koneksi untuk membuka kunci", tenthRes.message)
        assertNull(EncryptedOfflinePinVerifierStore(kvStore, crypto, 1_000).readVerifier("kasir-1"))

        // Even correct PIN offline is now rejected until verified online again
        val afterWipeRes = newServiceAfterRestart(finalSession).unlock("654321") as RepositoryResult.Error
        assertEquals("Butuh koneksi untuk membuka kunci", afterWipeRes.message)
    }

    @Test
    fun offlineOwnerAndAdminWrongPin_follows3Then60_300_900_1800_andDeletesAt10th() = runTest(testDispatcher) {
        listOf("OWNER", "ADMIN").forEach { role ->
            var nowMillis = 1_800_000_000_000L
            val kvStore = InMemoryKvStore()
            val crypto = AesGcmJvmTestCrypto()
            val userId = "user-${role.lowercase()}"
            EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)
                .saveVerifier(userId, "112233", nowMillis, verifiedAtElapsedMillis = nowMillis, verifiedAtBootCount = 1L)

            fun newService(): DefaultPinUnlockService {
                val store = EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)
                val session = seededSessionManager(userId = userId, role = role)
                return DefaultPinUnlockService(
                    authApiProvider = { StubUnlockAuthApi { throw UnknownHostException("offline") } },
                    sessionManager = session,
                    verifierStore = store,
                    ioDispatcher = testDispatcher,
                    clockMillis = { nowMillis },
                )
            }

            // Failures 1 & 2 -> attemptsLeft 2, 1
            val f1 = (newService().unlock("999999") as RepositoryResult.Success).data as PinUnlockResult.Failed
            assertFalse(f1.isLocked)
            assertEquals(2, f1.attemptsLeft)

            val f2 = (newService().unlock("999999") as RepositoryResult.Success).data as PinUnlockResult.Failed
            assertFalse(f2.isLocked)
            assertEquals(1, f2.attemptsLeft)

            // Failures 3..9 -> 60s, 300s, 900s, 1800s, 1800s, 1800s, 1800s
            val expectedLocks = listOf(60L, 300L, 900L, 1800L, 1800L, 1800L, 1800L)
            for (seconds in expectedLocks) {
                val locked = (newService().unlock("999999") as RepositoryResult.Success).data as PinUnlockResult.Failed
                assertTrue("Expected lock for $role at ${seconds}s", locked.isLocked)
                assertEquals(nowMillis + seconds * 1000L, locked.lockedUntilEpochMillis)
                nowMillis += seconds * 1000L + 1L
            }

            // Failure 10 -> deletes verifier
            val tenth = newService().unlock("999999") as RepositoryResult.Error
            assertEquals("Butuh koneksi untuk membuka kunci", tenth.message)
            assertNull(EncryptedOfflinePinVerifierStore(kvStore, crypto, 1_000).readVerifier(userId))
        }
    }

    @Test
    fun onlineChangePinAndResetPin_updateOfflineVerifier() = runTest(testDispatcher) {
        val nowMillis = 1_800_000_000_000L
        val kvStore = InMemoryKvStore()
        val verifierStore = EncryptedOfflinePinVerifierStore(kvStore, AesGcmJvmTestCrypto(), pbkdf2Iterations = 1_000)
        val sessionManager = seededSessionManager(userId = "usr-1", role = "KASIR")
        var failNetworkOnUnlock = false
        val api = StubUnlockAuthApi {
            if (failNetworkOnUnlock) {
                throw UnknownHostException("offline")
            }
            Response.success(ApiResponse(success = true, data = PinUnlockResponse(valid = true, attemptsLeft = 5)))
        }
        val unlockService = DefaultPinUnlockService({ api }, sessionManager, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
        val repo = RemoteAuthRepository(
            sessionManager = sessionManager,
            authApiProvider = { api },
            deviceIdProvider = { "android-dev" },
            ioDispatcher = testDispatcher,
            pinUnlockService = unlockService,
        )

        // Change PIN -> updates verifier to 222222
        assertTrue(repo.changeMyPin("111111", "222222") is RepositoryResult.Success)
        failNetworkOnUnlock = true
        sessionManager.lockForResume()
        assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), unlockService.unlock("222222"))

        // Old PIN rejected offline
        sessionManager.lockForResume()
        val oldFailed = (unlockService.unlock("111111") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertFalse(oldFailed.isLocked)

        // Reset PIN -> updates verifier to 333333 & clears local failure counter
        failNetworkOnUnlock = false
        assertTrue(repo.resetPinWithPassword("secret123", "333333") is RepositoryResult.Success)
        assertNull(verifierStore.readLockout("usr-1"))

        failNetworkOnUnlock = true
        sessionManager.lockForResume()
        assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), unlockService.unlock("333333"))
    }

    @Test
    fun verifierIsClearedOnLogoutPasswordChangedUnauthorizedAndUserSwitch() = runTest(testDispatcher) {
        val kvStore = InMemoryKvStore()
        val crypto = AesGcmJvmTestCrypto()
        val verifierStore = EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)

        fun buildSessionWithListener(userId: String): SessionManager {
            val tokenStore = InMemoryTestTokenStore().apply {
                saveTokens("access-$userId", "refresh-$userId")
                saveSessionUser(SessionUser("User $userId", "kasir", true, "2026-01-01", null, userId = userId))
            }
            return SessionManager(tokenStore).apply {
                addSessionClearedListener { verifierStore.clearAll() }
            }
        }

        // 1. logout
        val s1 = buildSessionWithListener("u1")
        verifierStore.saveVerifier("u1", "123456", 1_800_000_000_000L)
        s1.logout()
        assertNull(verifierStore.readVerifier("u1"))

        // 2. handlePasswordChanged
        val s2 = buildSessionWithListener("u1")
        verifierStore.saveVerifier("u1", "123456", 1_800_000_000_000L)
        s2.handlePasswordChanged()
        assertNull(verifierStore.readVerifier("u1"))

        // 3. handleUnauthorized (401)
        val s3 = buildSessionWithListener("u1")
        verifierStore.saveVerifier("u1", "123456", 1_800_000_000_000L)
        s3.handleUnauthorized("401 revoked")
        assertNull(verifierStore.readVerifier("u1"))

        // 4. User switch via saveAuthenticatedSession
        val s4 = buildSessionWithListener("u1")
        verifierStore.saveVerifier("u1", "123456", 1_800_000_000_000L)
        s4.saveAuthenticatedSession(
            token = "access-u2",
            refreshToken = "refresh-u2",
            user = SessionUser("User Two", "owner", true, "2026-01-01", null, userId = "u2"),
        )
        assertNull(verifierStore.readVerifier("u1"))
    }

    @Test
    fun gateway502_503_504FallbackToOfflineVerifier_enforceTtlAndLockoutAcrossRestarts() = runTest(testDispatcher) {
        var nowMillis = 1_800_000_000_000L
        val kvStore = InMemoryKvStore()
        val crypto = AesGcmJvmTestCrypto()
        val verifierStore = EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)

        // Missing verifier on 503 -> requires connection
        listOf(502, 503, 504).forEach { httpCode ->
            val session = seededSessionManager(userId = "usr-gw", role = "KASIR")
            val api = StubUnlockAuthApi {
                Response.error(httpCode, """{"message":"gateway unavailable"}""".toResponseBody("application/json".toMediaType()))
            }
            val service = DefaultPinUnlockService({ api }, session, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
            val res = service.unlock("123456") as RepositoryResult.Error
            assertEquals("Butuh koneksi untuk membuka kunci", res.message)
            assertTrue(session.requiresPinUnlock())
        }

        // Valid verifier unlocks on 502/503/504
        verifierStore.saveVerifier("usr-gw", "123456", nowMillis, verifiedAtElapsedMillis = nowMillis, verifiedAtBootCount = 1L)
        listOf(502, 503, 504).forEach { httpCode ->
            val session = seededSessionManager(userId = "usr-gw", role = "KASIR")
            val api = StubUnlockAuthApi {
                Response.error(httpCode, """{"message":"gateway unavailable"}""".toResponseBody("application/json".toMediaType()))
            }
            val service = DefaultPinUnlockService({ api }, session, { "android-dev" }, verifierStore, testDispatcher) { nowMillis }
            assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), service.unlock("123456"))
            assertFalse(session.requiresPinUnlock())
        }

        // Wrong PIN on 503 increments offline lockout identically (KASIR 5 -> 30s lock)
        val gw503Api = StubUnlockAuthApi {
            Response.error(503, """{"message":"service unavailable"}""".toResponseBody("application/json".toMediaType()))
        }
        for (attempt in 1..4) {
            val session = seededSessionManager(userId = "usr-gw", role = "KASIR")
            val service = DefaultPinUnlockService(
                { gw503Api },
                session,
                { "android-dev" },
                EncryptedOfflinePinVerifierStore(kvStore, crypto, 1_000),
                testDispatcher,
            ) { nowMillis }
            val res = (service.unlock("000000") as RepositoryResult.Success).data as PinUnlockResult.Failed
            assertFalse(res.isLocked)
            assertEquals(5 - attempt, res.attemptsLeft)
        }
        val lockSession = seededSessionManager(userId = "usr-gw", role = "KASIR")
        val lockService = DefaultPinUnlockService(
            { gw503Api },
            lockSession,
            { "android-dev" },
            EncryptedOfflinePinVerifierStore(kvStore, crypto, 1_000),
            testDispatcher,
        ) { nowMillis }
        val locked = (lockService.unlock("000000") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(locked.isLocked)
        assertEquals(nowMillis + 30_000L, locked.lockedUntilEpochMillis)

        // Past 72h verifier expiry on 504 -> rejected even after lock window passes
        nowMillis += OfflinePinVerifierStore.VERIFIER_TTL_MILLIS + 1L
        val gw504Api = StubUnlockAuthApi {
            Response.error(504, """{"message":"gateway timeout"}""".toResponseBody("application/json".toMediaType()))
        }
        val expiredSession = seededSessionManager(userId = "usr-gw", role = "KASIR")
        val expiredService = DefaultPinUnlockService(
            { gw504Api },
            expiredSession,
            { "android-dev" },
            EncryptedOfflinePinVerifierStore(kvStore, crypto, 1_000),
            testDispatcher,
        ) { nowMillis }
        val expiredRes = expiredService.unlock("123456") as RepositoryResult.Error
        assertEquals("Butuh koneksi untuk membuka kunci", expiredRes.message)
        assertTrue(expiredSession.requiresPinUnlock())
    }

    @Test
    fun tamperedCiphertextFailsClosedAndWipesCorruptRecord() {
        val kvStore = InMemoryKvStore()
        val store = EncryptedOfflinePinVerifierStore(kvStore, AesGcmJvmTestCrypto(), pbkdf2Iterations = 1_000)
        store.saveVerifier("u1", "123456", 1_800_000_000_000L)
        assertNotNull(store.readVerifier("u1"))

        kvStore.putString("offline_pin_verifier", "v1:tampered:ciphertext")
        assertNull(store.readVerifier("u1"))
        assertNull(kvStore.getString("offline_pin_verifier"))
    }

    @Test
    fun offlineLockout_ignoresForwardWallClockJump_andRearmsFullDurationOnRebootOrLegacyLockout() = runTest(testDispatcher) {
        var wallMillis = 1_800_000_000_000L
        var elapsedMillis = 100_000L
        var bootCount = 10L
        val kvStore = InMemoryKvStore()
        val crypto = AesGcmJvmTestCrypto()
        val store = EncryptedOfflinePinVerifierStore(kvStore, crypto, pbkdf2Iterations = 1_000)
        val offlineApi = StubUnlockAuthApi { throw ConnectException("offline") }
        val service = DefaultPinUnlockService(
            authApiProvider = { offlineApi },
            sessionManager = seededSessionManager("kasir-clk", "KASIR"),
            deviceIdProvider = { "android-dev" },
            verifierStore = store,
            ioDispatcher = testDispatcher,
            clockMillis = { wallMillis },
            elapsedRealtimeMillis = { elapsedMillis },
            bootCountProvider = { bootCount },
        )
        store.saveVerifier("kasir-clk", "123456", wallMillis, verifiedAtElapsedMillis = elapsedMillis, verifiedAtBootCount = bootCount)

        // Trigger 30s offline lockout on 5th wrong attempt
        repeat(4) {
            service.unlock("000000")
        }
        val triggeredLock = (service.unlock("000000") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(triggeredLock.isLocked)
        assertEquals(30_000L, triggeredLock.remainingLockMillis)

        val lockedBeforeJump = (service.unlock("123456") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(lockedBeforeJump.isLocked)
        assertEquals(30_000L, lockedBeforeJump.remainingLockMillis)

        // User advances wall-clock by +5 hours without monotonic time passing -> MUST remain locked with 30s remaining!
        wallMillis += 5L * 60L * 60L * 1000L
        val stillLockedAfterForwardJump = (service.unlock("123456") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(stillLockedAfterForwardJump.isLocked)
        assertEquals(30_000L, stillLockedAfterForwardJump.remainingLockMillis)

        // Device reboots during active lockout (bootCount increments, elapsed resets to 5_000L) -> Opsi B re-arms FULL 30s, verifier preserved
        bootCount = 11L
        elapsedMillis = 5_000L
        val lockedAfterReboot = (service.unlock("123456") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(lockedAfterReboot.isLocked)
        assertEquals(30_000L, lockedAfterReboot.remainingLockMillis)
        assertNotNull(store.readVerifier("kasir-clk"))

        // Still locked at +29s on new boot, unlocked at +30s + 1ms
        elapsedMillis += 29_000L
        val stillLockedAt29s = (service.unlock("123456") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(stillLockedAt29s.isLocked)

        elapsedMillis += 1_001L
        assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), service.unlock("123456"))

        // Legacy lockout record without monotonic anchor also re-arms full duration instead of bypassing
        store.saveLockout(
            OfflinePinLockoutRecord(
                userId = "kasir-clk",
                failedAttempts = 5,
                lockedUntilEpochMillis = wallMillis - 10_000L,
            )
        )
        val rearmedLegacy = (service.unlock("123456") as RepositoryResult.Success).data as PinUnlockResult.Failed
        assertTrue(rearmedLegacy.isLocked)
        assertNotNull(store.readVerifier("kasir-clk"))
    }

    @Test
    fun offlineVerifier_rejectsBackwardClockOver5Minutes_toleratesWithin5Minutes_andEnforces72hMonotonic() = runTest(testDispatcher) {
        val offlineApi = StubUnlockAuthApi { throw UnknownHostException("offline") }
        val crypto = AesGcmJvmTestCrypto()

        // 1. Legacy verifier without monotonic anchor fails closed
        run {
            val store = EncryptedOfflinePinVerifierStore(InMemoryKvStore(), crypto, 1_000)
            val wall = 1_800_000_000_000L
            store.saveVerifier("kasir-leg", "123456", wall)
            val svc = DefaultPinUnlockService(
                { offlineApi },
                seededSessionManager("kasir-leg", "KASIR"),
                { "dev" },
                store,
                testDispatcher,
                clockMillis = { wall },
                elapsedRealtimeMillis = { 10_000L },
                bootCountProvider = { 1L },
            )
            val res = svc.unlock("123456") as RepositoryResult.Error
            assertEquals(DefaultPinUnlockService.OFFLINE_VERIFIER_REQUIRED_MESSAGE, res.message)
        }

        // 2. Backward jump <= 5 min is tolerated; > 5 min wipes verifier and cannot be bypassed by moving clock forward again
        run {
            var wall = 1_800_000_000_000L
            var elapsed = 10_000L
            val store = EncryptedOfflinePinVerifierStore(InMemoryKvStore(), crypto, 1_000)
            store.saveVerifier("kasir-bw", "123456", wall, verifiedAtElapsedMillis = elapsed, verifiedAtBootCount = 1L)
            val svc = DefaultPinUnlockService(
                { offlineApi },
                seededSessionManager("kasir-bw", "KASIR"),
                { "dev" },
                store,
                testDispatcher,
                clockMillis = { wall },
                elapsedRealtimeMillis = { elapsed },
                bootCountProvider = { 1L },
            )

            // Minor 4-minute NTP backward adjustment -> still unlocks
            wall -= 4L * 60L * 1000L
            assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), svc.unlock("123456"))

            // Roll clock backward by 6 minutes (> 5-min tolerance) -> wipes verifier & requires online
            wall -= 6L * 60L * 1000L
            val tampered = svc.unlock("123456") as RepositoryResult.Error
            assertEquals(DefaultPinUnlockService.OFFLINE_VERIFIER_REQUIRED_MESSAGE, tampered.message)
            assertNull(store.readVerifier("kasir-bw"))

            // Restoring wall-clock forward does NOT revive wiped verifier
            wall += 10L * 60L * 1000L
            val afterRestore = svc.unlock("123456") as RepositoryResult.Error
            assertEquals(DefaultPinUnlockService.OFFLINE_VERIFIER_REQUIRED_MESSAGE, afterRestore.message)
        }

        // 3. Backward clock before first offline attempt relative to monotonic elapsed time (> 5 min skew) -> wipes verifier
        run {
            var wall = 1_800_000_000_000L
            var elapsed = 10_000L
            val store = EncryptedOfflinePinVerifierStore(InMemoryKvStore(), crypto, 1_000)
            store.saveVerifier("kasir-pre", "123456", wall, verifiedAtElapsedMillis = elapsed, verifiedAtBootCount = 1L)
            val svc = DefaultPinUnlockService(
                { offlineApi },
                seededSessionManager("kasir-pre", "KASIR"),
                { "dev" },
                store,
                testDispatcher,
                clockMillis = { wall },
                elapsedRealtimeMillis = { elapsed },
                bootCountProvider = { 1L },
            )

            // Real time passes 1 hour, user rolls wall clock back so wallAge is only 10s -> delta > 5 min -> fail closed
            elapsed += 60L * 60L * 1000L
            wall += 10_000L
            val res = svc.unlock("123456") as RepositoryResult.Error
            assertEquals(DefaultPinUnlockService.OFFLINE_VERIFIER_REQUIRED_MESSAGE, res.message)
            assertNull(store.readVerifier("kasir-pre"))
        }

        // 4. Monotonic TTL > 72h expires even if wall clock stays within 72h (within 5m skew or across reboot)
        run {
            var wall = 1_800_000_000_000L
            var elapsed = 10_000L
            var boot = 1L
            val store = EncryptedOfflinePinVerifierStore(InMemoryKvStore(), crypto, 1_000)
            store.saveVerifier("kasir-ttl", "123456", wall, verifiedAtElapsedMillis = elapsed, verifiedAtBootCount = boot)
            val svc = DefaultPinUnlockService(
                { offlineApi },
                seededSessionManager("kasir-ttl", "KASIR"),
                { "dev" },
                store,
                testDispatcher,
                clockMillis = { wall },
                elapsedRealtimeMillis = { elapsed },
                bootCountProvider = { boot },
            )
            // Advance 50h on boot 1
            wall += 50L * 60L * 60L * 1000L
            elapsed += 50L * 60L * 60L * 1000L
            assertEquals(RepositoryResult.Success(PinUnlockResult.Unlocked), svc.unlock("123456"))

            // Reboot to boot 2, run 22h + 1ms offline (total accumulated = 72h + 1ms) while wall clock only moved 20h (< 72h total wall)
            boot = 2L
            elapsed = 22L * 60L * 60L * 1000L + 1L
            wall += 20L * 60L * 60L * 1000L
            val expired = svc.unlock("123456") as RepositoryResult.Error
            assertEquals(DefaultPinUnlockService.OFFLINE_VERIFIER_REQUIRED_MESSAGE, expired.message)
        }
    }

    private fun seededSessionManager(userId: String, role: String): SessionManager {
        val store = InMemoryTestTokenStore().apply {
            saveTokens("access-token", "refresh-token")
            saveSessionUser(SessionUser("User", role, true, "2026-09-01", null, userId = userId))
        }
        return SessionManager(store).apply { lockForResume() }
    }
}

private class InMemoryKvStore : PinVerifierKeyValueStore {
    private val map = linkedMapOf<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String): Boolean {
        map[key] = value
        return true
    }
    override fun remove(key: String): Boolean {
        map.remove(key)
        return true
    }
    override fun clear(): Boolean {
        map.clear()
        return true
    }
    fun dumpAllValues(): Collection<String> = map.values
}

private class AesGcmJvmTestCrypto : PinVerifierCrypto {
    private val secretKey: SecretKey = KeyGenerator.getInstance("AES").apply {
        init(256, SecureRandom())
    }.generateKey()

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val enc = Base64.getEncoder()
        return "v1:${enc.encodeToString(cipher.iv)}:${enc.encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)))}"
    }

    override fun decrypt(ciphertext: String): String? {
        if (!ciphertext.startsWith("v1:")) return null
        val parts = ciphertext.split(':', limit = 3)
        if (parts.size != 3) return null
        return runCatching {
            val dec = Base64.getDecoder()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, dec.decode(parts[1])))
            cipher.doFinal(dec.decode(parts[2])).toString(Charsets.UTF_8)
        }.getOrNull()
    }
}

private class StubUnlockAuthApi(
    private val unlockResponder: () -> Response<ApiResponse<PinUnlockResponse>>,
) : AuthApi {
    var lastUnlockRequest: UnlockRequest? = null

    override suspend fun login(request: LoginRequest): Response<ApiResponse<LoginResponse>> = error("unused")

    override suspend fun unlock(request: UnlockRequest): Response<ApiResponse<PinUnlockResponse>> {
        lastUnlockRequest = request
        return unlockResponder()
    }

    override suspend fun resetPinWithPassword(request: ResetPinRequestDto): Response<ApiResponse<Unit>> =
        Response.success(ApiResponse(success = true, data = Unit))

    override suspend fun getMe(): Response<ApiResponse<CurrentUserDto>> = error("unused")

    override suspend fun changeMyPassword(request: ChangePasswordRequestDto): Response<ApiResponse<Unit>> =
        error("unused")

    override suspend fun changeMyPin(request: ChangePinRequestDto): Response<ApiResponse<Unit>> =
        Response.success(ApiResponse(success = true, data = Unit))
}

private class InMemoryTestTokenStore : TokenStore {
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
