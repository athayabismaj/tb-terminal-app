package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.CurrentUserDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.LoginResponse
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.PinUnlockResponse
import com.tbterminal.app.data.remote.ResetPinRequestDto
import com.tbterminal.app.data.remote.RoleResponseDto
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.remote.UserApi
import com.tbterminal.app.data.remote.UserCreateRequest
import com.tbterminal.app.data.remote.UserResponseDto
import com.tbterminal.app.data.remote.UserUpdateRequest
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthProfileRepositoryTest {
    @Test
    fun profileUsesAuthoritativeServerDataAndRefreshesSession() = runBlocking {
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
        assertEquals("Siti Aminah", store.readSessionUser()?.name)
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

    @Test
    fun updateProfilePersistsNameUsernameEmailToServerAndSession() = runBlocking {
        val authApi = FakeProfileAuthApi()
        val userApi = FakeProfileUserApi()
        val store = ProfileTokenStore().apply {
            saveTokens("access", "refresh")
            saveSessionUser(
                SessionUser(
                    name = "Pemilik Toko",
                    role = "OWNER",
                    isActive = true,
                    joinedAt = "2026-01-02T10:00:00+07:00",
                    lastLoginAt = null,
                    userId = "usr-owner-1",
                    username = "owner",
                    email = "owner@gmail.com"
                )
            )
        }
        val repository = RemoteAuthRepository(authApi, SessionManager(store), userApiProvider = { userApi })

        val res = repository.updateProfile(
            name = "Haji Ahmad",
            username = "ahmad.owner",
            email = "ahmad@barokah.id"
        ) as RepositoryResult.Success

        assertEquals("usr-owner-1", userApi.updatedUserId)
        assertEquals("Haji Ahmad", userApi.lastUpdateRequest?.name)
        assertEquals("ahmad.owner", userApi.lastUpdateRequest?.username)
        assertEquals("ahmad@barokah.id", userApi.lastUpdateRequest?.email)
        assertEquals("role-owner", userApi.lastUpdateRequest?.roleId)

        assertEquals("Haji Ahmad", res.data.name)
        assertEquals("ahmad.owner", res.data.username)
        assertEquals("ahmad@barokah.id", res.data.email)
        assertEquals("Haji Ahmad", store.readSessionUser()?.name)
        assertEquals("ahmad.owner", store.readSessionUser()?.username)
        assertEquals("ahmad@barokah.id", store.readSessionUser()?.email)
    }
}

private class FakeProfileUserApi : UserApi {
    var updatedUserId: String? = null
    var lastUpdateRequest: UserUpdateRequest? = null

    override suspend fun getUsers(page: Int, limit: Int): Response<ApiResponse<PaginatedResponse<UserResponseDto>>> =
        Response.success(
            ApiResponse(
                success = true,
                data = PaginatedResponse(
                    data = listOf(
                        UserResponseDto(
                            id = "usr-owner-1",
                            roleId = "role-owner",
                            roleName = "OWNER",
                            name = "Pemilik Toko",
                            username = "owner",
                            email = "owner@gmail.com",
                            isActive = true,
                            lastLogin = null,
                            createdAt = "2026-01-02T10:00:00+07:00"
                        )
                    ),
                    total = 1,
                    page = 1,
                    limit = 200,
                    totalPages = 1
                )
            )
        )

    override suspend fun getRoles(): Response<ApiResponse<List<RoleResponseDto>>> = error("not used")
    override suspend fun createUser(request: UserCreateRequest): Response<ApiResponse<UserResponseDto>> = error("not used")
    override suspend fun updateUser(id: String, request: UserUpdateRequest): Response<ApiResponse<UserResponseDto>> {
        updatedUserId = id
        lastUpdateRequest = request
        return Response.success(
            ApiResponse(
                success = true,
                data = UserResponseDto(
                    id = id,
                    roleId = request.roleId,
                    roleName = "OWNER",
                    name = request.name,
                    username = request.username,
                    email = request.email,
                    isActive = request.isActive,
                    lastLogin = null,
                    createdAt = "2026-01-02T10:00:00+07:00"
                )
            )
        )
    }
    override suspend fun deactivateUser(id: String): Response<ApiResponse<Unit>> = error("not used")
}

private class FakeProfileAuthApi : AuthApi {
    var passwordRequest: ChangePasswordRequestDto? = null
    var pinRequest: ChangePinRequestDto? = null

    override suspend fun login(request: LoginRequest): Response<ApiResponse<LoginResponse>> = error("not used")
    override suspend fun unlock(request: UnlockRequest): Response<ApiResponse<PinUnlockResponse>> = error("not used")
    override suspend fun resetPinWithPassword(request: ResetPinRequestDto): Response<ApiResponse<Unit>> = error("not used")
    override suspend fun getMe(): Response<ApiResponse<CurrentUserDto>> = Response.success(
        ApiResponse(success = true, data = profileResponse())
    )
    override suspend fun changeMyPassword(request: ChangePasswordRequestDto): Response<ApiResponse<Unit>> {
        passwordRequest = request
        return Response.success(ApiResponse(success = true, data = Unit))
    }
    override suspend fun changeMyPin(request: ChangePinRequestDto): Response<ApiResponse<Unit>> {
        pinRequest = request
        return Response.success(ApiResponse(success = true, data = Unit))
    }

    private fun profileResponse() = CurrentUserDto(
        username = "siti",
        role = "KASIR",
    )
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
