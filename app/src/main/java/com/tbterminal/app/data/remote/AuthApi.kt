package com.tbterminal.app.data.remote

import retrofit2.Response
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("/api/auth/unlock")
    suspend fun unlock(@Body request: UnlockRequest): Response<ApiResponse<Unit>>

    @GET("/api/auth/me")
    suspend fun getMe(): Response<ApiResponse<CurrentUserDto>>

    @PUT("/api/system/users/me/password")
    suspend fun changeMyPassword(
        @Body request: ChangePasswordRequestDto
    ): Response<ApiResponse<Unit>>

    @PUT("/api/system/users/me/pin")
    suspend fun changeMyPin(
        @Body request: ChangePinRequestDto
    ): Response<ApiResponse<Unit>>
}

interface TokenRefreshApi {
    @POST("/api/auth/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<ApiResponse<RefreshTokenResponse>>
}
