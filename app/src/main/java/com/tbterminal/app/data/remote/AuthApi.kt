package com.tbterminal.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("/api/auth/unlock")
    suspend fun unlock(@Body request: UnlockRequest): Response<ApiResponse<Unit>>
}
