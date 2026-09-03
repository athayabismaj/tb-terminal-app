package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateManagerApprovalCommand
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.network.ManagerApprovalApi
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.remote.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ManagerApprovalRepository {
    suspend fun createApproval(
        command: CreateManagerApprovalCommand,
    ): NetworkResult<ApiResponse<ManagerApprovalGrant>>
}

class RemoteManagerApprovalRepository(
    private val api: ManagerApprovalApi,
) : ManagerApprovalRepository {
    override suspend fun createApproval(
        command: CreateManagerApprovalCommand,
    ): NetworkResult<ApiResponse<ManagerApprovalGrant>> = withContext(Dispatchers.IO) {
        safeApiCall { api.createApproval(command) }
    }
}
