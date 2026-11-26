package com.tbterminal.app.ui.settings

import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelErrorTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun changePasswordPreserves429RetryAfterAnd401MaxAttemptsMessagesWithoutWrongPasswordRewrite() = runTest(dispatcher) {
        val repo429 = FakePasswordErrorAuthRepository(
            RepositoryResult.Error(
                code = "TOO_MANY_ATTEMPTS",
                message = "Terlalu banyak percobaan gagal. Coba lagi dalam 2 menit.",
                retryAfterSeconds = 120L,
            )
        )
        val vm429 = ProfileViewModel(repo429)
        vm429.changePassword("oldpass88", "newpass88", "newpass88")
        advanceUntilIdle()

        assertEquals("Terlalu banyak percobaan gagal. Coba lagi dalam 2 menit.", vm429.uiState.value.error)
        assertFalse(vm429.uiState.value.isNetworkError)

        val repo401 = FakePasswordErrorAuthRepository(
            RepositoryResult.Error(
                code = "HTTP_401",
                message = "Batas percobaan terlampaui. Silakan login kembali.",
            )
        )
        val vm401 = ProfileViewModel(repo401)
        vm401.changePassword("oldpass88", "newpass88", "newpass88")
        advanceUntilIdle()

        assertEquals("Batas percobaan terlampaui. Silakan login kembali.", vm401.uiState.value.error)
    }

    @Test
    fun verifyCurrentPinEnforces4To6DigitsAndMaps423And429AndForceLogout() = runTest(dispatcher) {
        val repo = ConfigurablePinAuthRepository()
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        val outcomes = mutableListOf<PinVerifyOutcome>()
        listOf("", "123", "1234567", "12ab").forEach { invalidPin ->
            vm.verifyCurrentPin(invalidPin) { outcomes += it }
        }
        advanceUntilIdle()
        assertEquals(0, repo.unlockCalls.size)
        assertEquals(4, outcomes.size)
        assertTrue(outcomes.all { it is PinVerifyOutcome.Error && !it.isNetworkError })

        repo.unlockResult = RepositoryResult.Success(PinUnlockResult.Unlocked)
        var outcome4: PinVerifyOutcome? = null
        vm.verifyCurrentPin("1357") { outcome4 = it }
        advanceUntilIdle()
        assertEquals(PinVerifyOutcome.Verified, outcome4)

        repo.unlockResult = RepositoryResult.Success(
            PinUnlockResult.Failed("PIN terkunci sementara. Coba lagi dalam 1 menit.", attemptsLeft = 0, isLocked = true)
        )
        var outcome423: PinVerifyOutcome? = null
        vm.verifyCurrentPin("135790") { outcome423 = it }
        advanceUntilIdle()
        assertTrue(outcome423 is PinVerifyOutcome.Rejected && (outcome423 as PinVerifyOutcome.Rejected).isLocked)

        repo.unlockResult = RepositoryResult.Error(
            code = "TOO_MANY_ATTEMPTS",
            message = "Terlalu banyak percobaan gagal",
            retryAfterSeconds = 45L,
        )
        var outcome429: PinVerifyOutcome? = null
        vm.verifyCurrentPin("135790") { outcome429 = it }
        advanceUntilIdle()
        assertTrue(outcome429 is PinVerifyOutcome.Error)
        assertEquals(
            "Terlalu banyak percobaan gagal. Coba lagi dalam 45 detik.",
            (outcome429 as PinVerifyOutcome.Error).message,
        )
        assertFalse((outcome429 as PinVerifyOutcome.Error).isNetworkError)

        repo.unlockResult = RepositoryResult.Success(PinUnlockResult.ForceLogout("Sesi dicabut"))
        var outcomeLogout: PinVerifyOutcome? = null
        vm.verifyCurrentPin("135790") { outcomeLogout = it }
        advanceUntilIdle()
        assertEquals(PinVerifyOutcome.ForceLogout("Sesi dicabut"), outcomeLogout)
    }

    @Test
    fun changePinAndResetPinEnforce4To6DigitsAndPreserve429And423Messages() = runTest(dispatcher) {
        val repo = ConfigurablePinAuthRepository()
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.changePin("123", "5678", "5678")
        assertTrue(vm.uiState.value.error?.contains("4 sampai 6") == true)
        vm.changePin("1234", "1234567", "1234567")
        assertTrue(vm.uiState.value.error?.contains("4 sampai 6") == true)
        assertEquals(0, repo.changePinCalls.size)

        repo.changePinResult = RepositoryResult.Error(
            code = "PIN_LOCKED",
            message = "",
            retryAfterSeconds = 60L,
        )
        vm.changePin("1357", "24680", "24680")
        advanceUntilIdle()
        assertEquals(1, repo.changePinCalls.size)
        assertEquals("PIN terkunci sementara. Coba lagi dalam 1 menit.", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isNetworkError)

        var resetSuccess = false
        vm.resetPinWithPassword("pass1234", "123", "123") { resetSuccess = true }
        assertTrue(vm.uiState.value.error?.contains("4 sampai 6") == true)
        assertFalse(resetSuccess)

        repo.resetPinResult = RepositoryResult.Error(
            code = "TOO_MANY_ATTEMPTS",
            message = "Terlalu banyak percobaan gagal",
            retryAfterSeconds = 120L,
        )
        vm.resetPinWithPassword("pass1234", "2468", "2468") { resetSuccess = true }
        advanceUntilIdle()
        assertFalse(resetSuccess)
        assertEquals("Terlalu banyak percobaan gagal. Coba lagi dalam 2 menit.", vm.uiState.value.error)

        repo.resetPinResult = RepositoryResult.Success(Unit)
        vm.resetPinWithPassword("pass1234", "2468", "2468") { resetSuccess = true }
        advanceUntilIdle()
        assertTrue(resetSuccess)
        assertEquals("PIN diperbarui", vm.uiState.value.message)
    }
}

private class ConfigurablePinAuthRepository : AuthRepository {
    val unlockCalls = mutableListOf<String>()
    val changePinCalls = mutableListOf<Pair<String, String>>()
    var unlockResult: RepositoryResult<PinUnlockResult> = RepositoryResult.Success(PinUnlockResult.Unlocked)
    var changePinResult: RepositoryResult<Unit> = RepositoryResult.Success(Unit)
    var resetPinResult: RepositoryResult<Unit> = RepositoryResult.Success(Unit)

    override suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession> = error("unused")
    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> {
        unlockCalls += pin
        return unlockResult
    }
    override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> = resetPinResult
    override suspend fun getProfile(): RepositoryResult<UserProfile> = RepositoryResult.Success(
        UserProfile("u1", "kasir", "Kasir", "KASIR", null, true, "2026-01-01", null)
    )
    override suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit> = error("unused")
    override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> {
        changePinCalls += oldPin to newPin
        return changePinResult
    }
}

private class FakePasswordErrorAuthRepository(
    private val passwordResult: RepositoryResult<Unit>,
) : AuthRepository {
    override suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession> = error("unused")
    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> = error("unused")
    override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> = error("unused")
    override suspend fun getProfile(): RepositoryResult<UserProfile> = RepositoryResult.Success(
        UserProfile(
            id = "u1",
            username = "kasir",
            name = "Kasir",
            role = "KASIR",
            email = null,
            isActive = true,
            joinedAt = "2026-01-01",
            lastLoginAt = null,
        )
    )
    override suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit> = passwordResult
    override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> = error("unused")
}
