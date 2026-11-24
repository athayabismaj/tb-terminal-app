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
