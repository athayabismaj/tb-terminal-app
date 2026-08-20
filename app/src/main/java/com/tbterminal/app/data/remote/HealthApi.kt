package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET

interface HealthApi {
    @GET("/api/readiness")
    suspend fun getReadiness(): Response<HealthResponseDto>
}

@Serializable
data class HealthResponseDto(
    val status: String? = null,
    val service: String? = null,
    val database: String? = null,
    val time: String? = null
)
