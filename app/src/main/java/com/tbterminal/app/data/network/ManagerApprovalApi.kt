package com.tbterminal.app.data.network

import com.tbterminal.app.data.model.CreateManagerApprovalCommand
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.remote.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ManagerApprovalApi {
    @POST("/api/system/manager-approvals")
    suspend fun createApproval(
        @Body command: CreateManagerApprovalCommand,
    ): Response<ApiResponse<ManagerApprovalGrant>>
}
