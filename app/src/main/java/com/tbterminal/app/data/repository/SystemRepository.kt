package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.AuditLogPage
import com.tbterminal.app.data.model.StoreSettings
import com.tbterminal.app.data.model.UpdateStoreSettingsCommand
import com.tbterminal.app.data.network.SystemApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemRepository(
    private val systemApi: SystemApi
) {
    suspend fun getAuditLogs(
        page: Int = 1,
        limit: Int = 20,
        action: String? = null,
        range: String? = null
    ): RepositoryResult<AuditLogPage> = withContext(Dispatchers.IO) {
        safeApiCall { systemApi.getAuditLogs(page, limit, action, range) }
    }

    suspend fun getStoreSettings(): RepositoryResult<StoreSettings> = withContext(Dispatchers.IO) {
        safeApiCall { systemApi.getStoreSettings() }
    }

    suspend fun updateStoreSettings(command: UpdateStoreSettingsCommand): RepositoryResult<StoreSettings> = withContext(Dispatchers.IO) {
        safeApiCall { systemApi.updateStoreSettings(command) }
    }
}
