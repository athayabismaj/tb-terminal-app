package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.dao.CashExpenseDao
import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.dao.SyncQueueDao
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

enum class OfflineConnectionStatus {
    ONLINE,
    OFFLINE,
    SYNCING,
    SYNC_FAILED
}

data class OfflineStatus(
    val connectionStatus: OfflineConnectionStatus = OfflineConnectionStatus.ONLINE,
    val isOnline: Boolean = true,
    val isInternetOnline: Boolean = true,
    val isServerReachable: Boolean = false,
    val internetStatus: InternetStatus = InternetStatus.ONLINE,
    val backendStatus: BackendStatus = BackendStatus.UNKNOWN,
    val dataSourceStatus: DataSourceStatus = DataSourceStatus.SERVER,
    val syncStatusView: SyncStatusView = SyncStatusView.IDLE,
    val appDataMode: AppDataMode = AppDataMode.SYNC_SERVER,
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val otherUsersPendingSyncCount: Int = 0,
)

class OfflineStatusRepository(
    private val networkMonitor: NetworkConnectivity,
    private val backendHealthMonitor: BackendHealthMonitor,
    localAppSettingsDataSource: LocalAppSettingsDataSource,
    syncQueueDao: SyncQueueDao,
    private val cashSessionDao: CashSessionDao? = null,
    private val transactionDao: TransactionDao? = null,
    private val cashExpenseDao: CashExpenseDao? = null,
    activeUserIdFlow: Flow<String?>? = null,
) {
    private val connectivityStatus: Flow<ConnectivityStatus> = combine(
        networkMonitor.observeOnline(),
        backendHealthMonitor.status,
        localAppSettingsDataSource.observeDataMode()
    ) { isOnline, backendStatus, dataMode ->
        ConnectivityStatus(isOnline, backendStatus, dataMode)
    }

    private val syncQueueStatus: Flow<SyncQueueStatus> = if (activeUserIdFlow == null) {
        combine(
            syncQueueDao.observePendingCount(SyncStatus.SYNCED),
            syncQueueDao.observeCountByStatus(SyncStatus.FAILED),
            syncQueueDao.observeCountByStatus(SyncStatus.SYNCING)
        ) { pendingCount, failedCount, syncingCount ->
            SyncQueueStatus(pendingCount, failedCount, syncingCount, otherUsersPendingSyncCount = 0)
        }
    } else {
        combine(
            syncQueueDao.observeQueue(),
            cashSessionDao?.observeSessions() ?: flowOf(emptyList()),
            activeUserIdFlow,
        ) { queue, sessions, activeUserId ->
            val partitioned = partitionSyncQueueByUser(
                queue = queue,
                activeUserId = activeUserId,
                sessions = sessions,
                cashSessionDao = cashSessionDao,
                transactionDao = transactionDao,
                cashExpenseDao = cashExpenseDao,
                applyIsolationFilter = true,
            )
            SyncQueueStatus(
                pendingCount = partitioned.ownItems.count { it.status != SyncStatus.SYNCED },
                failedCount = partitioned.ownItems.count { it.status == SyncStatus.FAILED },
                syncingCount = partitioned.ownItems.count { it.status == SyncStatus.SYNCING },
                otherUsersPendingSyncCount = partitioned.otherUsersPendingSyncCount,
            )
        }
    }

    val status: Flow<OfflineStatus> = combine(
        connectivityStatus,
        syncQueueStatus
    ) { connectivity, syncQueue ->
        val isOnline = connectivity.isOnline
        val backendStatus = connectivity.backendStatus
        val dataMode = connectivity.dataMode
        val pendingCount = syncQueue.pendingCount
        val failedCount = syncQueue.failedCount
        val syncingCount = syncQueue.syncingCount
        val otherUsersPendingCount = syncQueue.otherUsersPendingSyncCount
        val connectionStatus = when {
            syncingCount > 0 -> OfflineConnectionStatus.SYNCING
            failedCount > 0 -> OfflineConnectionStatus.SYNC_FAILED
            isOnline -> OfflineConnectionStatus.ONLINE
            else -> OfflineConnectionStatus.OFFLINE
        }
        val internetStatus = if (isOnline) InternetStatus.ONLINE else InternetStatus.OFFLINE
        val syncStatusView = when {
            syncingCount > 0 -> SyncStatusView.SYNCING
            failedCount > 0 -> SyncStatusView.FAILED
            else -> SyncStatusView.IDLE
        }
        val dataSourceStatus = when {
            dataMode == AppDataMode.OFFLINE_ONLY -> DataSourceStatus.LOCAL_ONLY
            backendStatus == BackendStatus.CONNECTED -> DataSourceStatus.SERVER
            else -> DataSourceStatus.CACHE
        }

        OfflineStatus(
            connectionStatus = connectionStatus,
            isOnline = isOnline,
            isInternetOnline = isOnline,
            isServerReachable = backendStatus == BackendStatus.CONNECTED,
            internetStatus = internetStatus,
            backendStatus = backendStatus,
            dataSourceStatus = dataSourceStatus,
            syncStatusView = syncStatusView,
            appDataMode = dataMode,
            pendingSyncCount = pendingCount,
            failedSyncCount = failedCount,
            otherUsersPendingSyncCount = otherUsersPendingCount,
        )
    }.distinctUntilChanged()

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun currentBackendStatus(): BackendStatus = backendHealthMonitor.status.value

    suspend fun refreshBackendHealth(force: Boolean = false): BackendStatus {
        return backendHealthMonitor.refresh(force = force)
    }

    fun refreshBackendHealthAsync(force: Boolean = false) {
        backendHealthMonitor.refreshAsync(force = force)
    }

    fun markBackendConnected() {
        backendHealthMonitor.markConnected()
    }

    fun markBackendUnreachable() {
        backendHealthMonitor.markUnreachable()
    }

    fun isBackendHealthStale(): Boolean {
        return backendHealthMonitor.isStale()
    }
}

private data class ConnectivityStatus(
    val isOnline: Boolean,
    val backendStatus: BackendStatus,
    val dataMode: AppDataMode
)

private data class SyncQueueStatus(
    val pendingCount: Int,
    val failedCount: Int,
    val syncingCount: Int,
    val otherUsersPendingSyncCount: Int = 0,
)
