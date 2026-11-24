package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.SyncMonitoringSnapshot
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val OWNER_MENU_TABLET_BREAKPOINT = 840.dp

@Suppress("UNUSED_PARAMETER")
@Composable
fun OwnerMenuScreen(
    name: String,
    role: String,
    authRepository: AuthRepository,
    onDashboardClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onFallbackTransactions: () -> Unit = {},
    onFallbackFinance: () -> Unit = {},
    onFallbackStock: () -> Unit = {},
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? TbTerminalApplication)?.appContainer
    val destinationNavigator = LocalAdminDestinationNavigator.current

    val offlineStatusFlow = remember(appContainer) { appContainer?.offlineStatusRepository?.status }
    val offlineStatus by (
        offlineStatusFlow?.collectAsState(initial = OfflineStatus())
            ?: remember { mutableStateOf(OfflineStatus()) }
        )

    val syncSnapshotFlow = remember(appContainer) { appContainer?.syncMonitoringRepository?.observeSnapshot() }
    val syncSnapshot by (
        syncSnapshotFlow?.collectAsState(initial = SyncMonitoringSnapshot())
            ?: remember { mutableStateOf(SyncMonitoringSnapshot()) }
        )

    var resolvedName by remember(name) { mutableStateOf(name.ifBlank { "Pemilik Toko" }) }
    var resolvedRole by remember(role) { mutableStateOf(role) }
    var storeName by remember { mutableStateOf("Terminal Barokah Jaya") }

    LaunchedEffect(authRepository, appContainer, name, role) {
        appContainer?.localAppSettingsDataSource?.let { settings ->
            val cachedName = runCatching { settings.getCachedStoreConfig().storeName }.getOrNull()
            resolveDisplayStoreName(cachedName)?.let { storeName = it }
        }

        val profileRes = runCatching { authRepository.getProfile() }.getOrNull()
        if (profileRes is RepositoryResult.Success) {
            val profile = profileRes.data
            if (profile.name.isNotBlank()) {
                resolvedName = profile.name
            }
            if (profile.role.isNotBlank()) {
                resolvedRole = profile.role
            }
        }

        val storeRes = runCatching { appContainer?.systemRepository?.getStoreSettings() }.getOrNull()
        if (storeRes is NetworkResult.Success) {
            resolveDisplayStoreName(storeRes.data.data?.storeName)?.let { storeName = it }
        }
    }

    val isSynced = remember(offlineStatus, syncSnapshot) {
        val networkOk = offlineStatus.isInternetOnline && offlineStatus.backendStatus != BackendStatus.UNREACHABLE
        networkOk && offlineStatus.failedSyncCount == 0 && syncSnapshot.totalFailed == 0 && syncSnapshot.totalConflict == 0
    }

    val syncSubtitle = remember(offlineStatus, syncSnapshot, isSynced) {
        formatMenuSyncSubtitle(
            offlineStatus = offlineStatus,
            syncSnapshot = syncSnapshot,
            synced = isSynced
        )
    }

    val uiState = remember(resolvedName, resolvedRole, storeName, isSynced, syncSubtitle) {
        OwnerMenuUiState(
            userName = resolvedName,
            role = resolvedRole,
            storeName = storeName,
            photoUrl = null,
            synced = isSynced,
            syncSubtitle = syncSubtitle
        )
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                destinationNavigator?.invoke(AdminDestination.TransactionsHub) ?: onFallbackTransactions()
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub) ?: onFallbackFinance()
            DashboardNavDestination.STOK ->
                destinationNavigator?.invoke(AdminDestination.StockHub) ?: onFallbackStock()
            DashboardNavDestination.MENU -> Unit
        }
    }

    MenuScreen(
        state = uiState,
        onProfileClick = onProfileClick,
        onDestinationClick = { destination -> destinationNavigator?.invoke(destination) },
        onNavSelect = ::handleNavSelect
    )
}

internal fun formatMenuSyncSubtitle(
    offlineStatus: OfflineStatus,
    syncSnapshot: SyncMonitoringSnapshot,
    synced: Boolean,
    nowMillis: Long = System.currentTimeMillis()
): String {
    if (syncSnapshot.totalSyncing > 0) {
        return "Sedang menyinkronkan..."
    }
    val failed = maxOf(syncSnapshot.totalFailed, offlineStatus.failedSyncCount)
    if (failed > 0) {
        return "$failed item gagal sinkron"
    }
    val pending = maxOf(syncSnapshot.totalPending, offlineStatus.pendingSyncCount)
    if (pending > 0) {
        return "$pending antrean menunggu"
    }
    val latestSyncedAt = syncSnapshot.queueItems
        .asSequence()
        .filter { it.syncStatus == SyncStatus.SYNCED }
        .maxOfOrNull { it.updatedAt }
    if (latestSyncedAt != null && latestSyncedAt > 0L) {
        val elapsedMinutes = ((nowMillis - latestSyncedAt) / 60_000L).coerceAtLeast(0L)
        return when {
            elapsedMinutes < 1L -> "Terakhir baru saja"
            elapsedMinutes < 60L -> "Terakhir $elapsedMinutes menit lalu"
            elapsedMinutes < 24L * 60L -> "Terakhir ${elapsedMinutes / 60L} jam lalu"
            else -> "Terakhir ${elapsedMinutes / (24L * 60L)} hari lalu"
        }
    }
    return when {
        synced -> "Semua data tersinkron"
        !offlineStatus.isInternetOnline -> "Menunggu koneksi internet"
        else -> "Status sinkronisasi aktif"
    }
}

@Composable
internal fun MenuScreen(
    state: OwnerMenuUiState,
    onProfileClick: () -> Unit,
    onDestinationClick: (AdminDestination) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_MENU_TABLET_BREAKPOINT
        if (isTablet) {
            TabletMenuLayout(
                state = state,
                onProfileClick = onProfileClick,
                onDestinationClick = onDestinationClick,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneMenuLayout(
                state = state,
                onProfileClick = onProfileClick,
                onDestinationClick = onDestinationClick,
                onNavSelect = onNavSelect
            )
        }
    }
}

@Composable
private fun PhoneMenuLayout(
    state: OwnerMenuUiState,
    onProfileClick: () -> Unit,
    onDestinationClick: (AdminDestination) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    val groups = remember(state.role, state.syncSubtitle, onDestinationClick) {
        buildOwnerMenuGroups(
            role = state.role,
            syncSubtitle = state.syncSubtitle,
            onDestinationClick = onDestinationClick
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header hijau (padding 56dp vertikal, 20dp horizontal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brand)
                    .clipToBounds()
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    RingsPattern(
                        radii = listOf(50.dp, 95.dp, 140.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                            .offset(x = 90.dp, y = (-90).dp)
                            .size(300.dp)
                    )

                    DotsPattern(
                        dotRadius = 1.5.dp,
                        step = 20.dp,
                        maskAtBottomRight = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 190.dp, height = 110.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 56.dp)
                ) {
                    OwnerMenuTopBar(
                        userName = state.userName,
                        initial = state.initial,
                        synced = state.synced,
                        onProfileClick = onProfileClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OwnerMenuProfileHeaderBlock(state = state)
                }
            }

            // 2. Panel putih menumpuk 28dp ke atas header hijau (sudut atas radius 32dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val overlapPx = 28.dp.roundToPx()
                        layout(placeable.width, (placeable.height - overlapPx).coerceAtLeast(0)) {
                            placeable.placeRelative(0, -overlapPx)
                        }
                    }
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SurfaceWhite)
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 96.dp)
            ) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    OwnerPlainMenuSection(group = group)
                }
            }
        }

        // 3. Menu bawah menempel di dasar layar (tinggi 80dp)
        OwnerPhoneBottomNav(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TabletMenuLayout(
    state: OwnerMenuUiState,
    onProfileClick: () -> Unit,
    onDestinationClick: (AdminDestination) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    val groups = remember(state.role, state.syncSubtitle, onDestinationClick) {
        buildOwnerMenuGroups(
            role = state.role,
            syncSubtitle = state.syncSubtitle,
            onDestinationClick = onDestinationClick
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Rel navigasi samping tetap (88dp)
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 432dp + Panel putih menumpuk 32dp ke kiri
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 2. Panel hijau tetap 432dp (isi 352dp: kiri 24dp, kanan 56dp), hanya pola lingkaran kanan atas
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(432.dp)
                    .fillMaxHeight()
                    .background(Brand)
                    .clipToBounds()
            ) {
                RingsPattern(
                    radii = listOf(50.dp, 95.dp, 140.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .offset(x = 90.dp, y = (-90).dp)
                        .size(300.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, top = 40.dp, end = 56.dp, bottom = 40.dp)
                ) {
                    OwnerMenuTopBar(
                        userName = state.userName,
                        initial = state.initial,
                        synced = state.synced,
                        onProfileClick = onProfileClick
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        OwnerMenuProfileHeaderBlock(state = state)
                    }
                }
            }

            // 3. Panel kanan putih scroll-able menumpuk 32dp ke kiri di atas panel hijau
            Column(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
            ) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    OwnerPlainMenuSection(group = group)
                }
            }
        }
    }
}

@Preview(
    name = "OwnerMenuPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 988
)
@Composable
internal fun OwnerMenuPhonePreview() {
    TbterminalappTheme {
        MenuScreen(
            state = sampleReferenceMenuState(),
            onProfileClick = {},
            onDestinationClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerMenuTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerMenuTabletPreview() {
    TbterminalappTheme {
        MenuScreen(
            state = sampleReferenceMenuState(),
            onProfileClick = {},
            onDestinationClick = {},
            onNavSelect = {}
        )
    }
}
