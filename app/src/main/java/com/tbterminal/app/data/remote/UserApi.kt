package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserApi {
    @GET("/api/system/users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200
    ): Response<ApiResponse<PaginatedResponse<UserResponseDto>>>

    @GET("/api/system/roles")
    suspend fun getRoles(): Response<ApiResponse<List<RoleResponseDto>>>

    @POST("/api/system/users")
    suspend fun createUser(
        @Body request: UserCreateRequest
    ): Response<ApiResponse<UserResponseDto>>

    @PUT("/api/system/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UserUpdateRequest
    ): Response<ApiResponse<UserResponseDto>>

    @DELETE("/api/system/users/{id}")
    suspend fun deactivateUser(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

@Serializable
data class RoleResponseDto(
    val id: String,
    val name: String
)

@Serializable
data class UserResponseDto(
    val id: String,
    val roleId: String,
    val roleName: String,
    val name: String,
    val username: String,
    val email: String? = null,
    val isActive: Boolean,
    val lastLogin: String? = null,
    val createdAt: String
)

@Serializable
data class UserCreateRequest(
    val name: String,
    val username: String,
    val password: String,
    val pin: String,
    val email: String? = null,
    val roleId: String
)

@Serializable
data class UserUpdateRequest(
    val name: String,
    val username: String,
    val isActive: Boolean,
    val roleId: String,
    val email: String? = null,
    val newPassword: String? = null,
    val newPin: String? = null
)
