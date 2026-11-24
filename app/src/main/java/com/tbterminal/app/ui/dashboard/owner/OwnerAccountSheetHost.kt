package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.repository.SyncMonitoringSnapshot
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.dashboard.BackofficeAccountBottomSheet
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Immutable
internal data class OwnerAccountContextState(
    val offlineStatus: OfflineStatus = OfflineStatus(),
    val devicePreferences: DevicePreferences = DevicePreferences(),
    val isNetworkHealthy: Boolean = true,
    val failedSyncCount: Int = 0
) {
    val isSynced: Boolean
        get() = isNetworkHealthy && failedSyncCount == 0
}

/**
 * Membaca status konektivitas offline, preferensi perangkat, dan antrean sinkronisasi
 * agar avatar & sheet akun di hub Owner selalu konsisten dengan Beranda.
 */
@Composable
internal fun rememberOwnerAccountContextState(): OwnerAccountContextState {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? TbTerminalApplication)?.appContainer
    val offlineStatusFlow = remember(appContainer) { appContainer?.offlineStatusRepository?.status }
    val syncSnapshotFlow = remember(appContainer) { appContainer?.syncMonitoringRepository?.observeSnapshot() }

    val offlineStatus by (
        offlineStatusFlow?.collectAsState(initial = OfflineStatus())
            ?: remember { mutableStateOf(OfflineStatus()) }
        )
    val syncSnapshot by (
        syncSnapshotFlow?.collectAsState(initial = null)
            ?: remember { mutableStateOf<SyncMonitoringSnapshot?>(null) }
        )
    var devicePreferences by remember { mutableStateOf(DevicePreferences()) }
    LaunchedEffect(appContainer) {
        devicePreferences = appContainer?.localAppSettingsDataSource?.getDevicePreferences() ?: DevicePreferences()
    }

    val isNetworkHealthy = remember(offlineStatus) {
        offlineStatus.isInternetOnline && offlineStatus.backendStatus != BackendStatus.UNREACHABLE
    }

    val failedSyncCount = syncSnapshot?.totalFailed ?: 0

    return remember(offlineStatus, devicePreferences, isNetworkHealthy, failedSyncCount) {
        OwnerAccountContextState(
            offlineStatus = offlineStatus,
            devicePreferences = devicePreferences,
            isNetworkHealthy = isNetworkHealthy,
            failedSyncCount = failedSyncCount
        )
    }
}

/**
 * Host bersama untuk menampilkan [BackofficeAccountBottomSheet] dari header hub Owner.
 */
@Composable
internal fun OwnerAccountSheetHost(
    visible: Boolean,
    userName: String,
    role: String,
    offlineStatus: OfflineStatus,
    devicePreferences: DevicePreferences,
    onDismiss: () -> Unit,
    onNavigate: (AdminDestination, () -> Unit) -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onReportsClick: () -> Unit = {}
) {
    if (!visible) return

    BackofficeAccountBottomSheet(
        userName = userName,
        role = role,
        offlineStatus = offlineStatus,
        devicePreferences = devicePreferences,
        onDismiss = onDismiss,
        onProfileClick = { onNavigate(AdminDestination.Profile, onSettingsClick) },
        onSettingsClick = { onNavigate(AdminDestination.Settings, onSettingsClick) },
        onSyncCenterClick = { onNavigate(AdminDestination.SyncCenter, onSyncCenterClick) },
        onReportsClick = { onNavigate(AdminDestination.Reports, onReportsClick) },
        onStoreSettingsClick = { onNavigate(AdminDestination.StoreSettings, onSettingsClick) },
        onLogout = onLogout
    )
}
