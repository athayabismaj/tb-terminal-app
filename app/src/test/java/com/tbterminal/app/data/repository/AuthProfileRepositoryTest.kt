package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.CurrentUserDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.LoginResponse
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthProfileRepositoryTest {
    @Test
    fun profileUsesServerUsernameAndRoleWithoutFabricatedEmail() = runBlocking {
        val api = FakeProfileAuthApi()
        val store = ProfileTokenStore().apply {
            saveTokens("access", "refresh")
            saveSessionUser(
                SessionUser(
                    name = "Siti Aminah",
                    role = "kasir",
                    isActive = true,
                    joinedAt = "2026-01-02T10:00:00+07:00",
                    lastLoginAt = null,
                    userId = "user-1"
                )
            )
        }
        val repository = RemoteAuthRepository(api, SessionManager(store))

        val result = repository.getProfile() as RepositoryResult.Success

        assertEquals("siti", result.data.username)
        assertEquals("KASIR", result.data.role)
        assertEquals("Siti Aminah", result.data.name)
        assertNull(result.data.email)
    }

    @Test
    fun selfCredentialChangesUseDedicatedEndpoints() = runBlocking {
        val api = FakeProfileAuthApi()
        val repository = RemoteAuthRepository(api, SessionManager(ProfileTokenStore()))

        assertTrue(repository.changeMyPassword("old-pass", "new-pass") is RepositoryResult.Success)
        assertEquals(ChangePasswordRequestDto("old-pass", "new-pass"), api.passwordRequest)
        assertTrue(repository.changeMyPin("1234", "5678") is RepositoryResult.Success)
        assertEquals(ChangePinRequestDto("1234", "5678"), api.pinRequest)
    }
}

private class FakeProfileAuthApi : AuthApi {
    var passwordRequest: ChangePasswordRequestDto? = null
    var pinRequest: ChangePinRequestDto? = null

    override suspend fun login(request: LoginRequest): Response<ApiResponse<LoginResponse>> = error("not used")
    override suspend fun unlock(request: UnlockRequest): Response<ApiResponse<Unit>> = error("not used")
    override suspend fun getMe(): Response<ApiResponse<CurrentUserDto>> = Response.success(
        ApiResponse(success = true, data = CurrentUserDto(username = "siti", role = "KASIR"))
    )
    override suspend fun changeMyPassword(request: ChangePasswordRequestDto): Response<ApiResponse<Unit>> {
        passwordRequest = request
        return Response.success(ApiResponse(success = true, data = Unit))
    }
    override suspend fun changeMyPin(request: ChangePinRequestDto): Response<ApiResponse<Unit>> {
        pinRequest = request
        return Response.success(ApiResponse(success = true, data = Unit))
    }
}

private class ProfileTokenStore : TokenStore {
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
