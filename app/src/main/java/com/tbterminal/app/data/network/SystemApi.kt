package com.tbterminal.app.data.network

import com.tbterminal.app.data.model.AuditLogPage
import com.tbterminal.app.data.model.StoreSettings
import com.tbterminal.app.data.model.UpdateStoreSettingsCommand
import com.tbterminal.app.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface SystemApi {
    @GET("system/audit-logs")
    suspend fun getAuditLogs(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("action") action: String?,
        @Query("range") range: String?
    ): Response<ApiResponse<AuditLogPage>>

    @GET("system/store-settings")
    suspend fun getStoreSettings(): Response<ApiResponse<StoreSettings>>

    @PUT("system/store-settings")
    suspend fun updateStoreSettings(
        @Body command: UpdateStoreSettingsCommand
    ): Response<ApiResponse<StoreSettings>>
}
