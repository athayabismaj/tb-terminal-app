package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.LocalBackofficePageNavigation
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.users.StaffStatus
import com.tbterminal.app.ui.users.UserManagementUiState

private val OWNER_USER_TABLET_BREAKPOINT = 840.dp

internal object OwnerUserToastBridge {
    private var pendingMessage: String? = null

    fun setPendingSuccess(message: String) {
        pendingMessage = message
    }

    fun consumePendingSuccess(): String? {
        val value = pendingMessage
        pendingMessage = null
        return value
    }
}

@Immutable
internal data class UserEmptySummaryUiState(
    val totalUsers: Long = 0L,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0
)

@Immutable
internal data class UserEmptySpec(
    val icon: ImageVector,
    val iconContainerColor: Color,
    val iconTint: Color,
    val title: String,
    val description: String
)

internal fun UserManagementUiState.toUserEmptySummary(): UserEmptySummaryUiState {
    val active = staffMembers.count { it.status == StaffStatus.Active }
    val inactive = staffMembers.count { it.status == StaffStatus.Inactive }
    return UserEmptySummaryUiState(
        totalUsers = totalStaff,
        activeCount = active,
        inactiveCount = inactive
    )
}

internal fun userEmptyStateSpecFor(
    searchQuery: String,
    isFiltered: Boolean
): UserEmptySpec {
    val trimmedQuery = searchQuery.trim()
    return when {
        trimmedQuery.isNotEmpty() -> UserEmptySpec(
            icon = OwnerIcons.Search,
            iconContainerColor = ChipBg,
            iconTint = Muted,
            title = "\"$trimmedQuery\" tidak ditemukan",
            description = "Periksa ejaan atau cari dengan username maupun email."
        )
        isFiltered -> UserEmptySpec(
            icon = OwnerIcons.Search,
            iconContainerColor = ChipBg,
            iconTint = Muted,
            title = "Pengguna tidak ditemukan",
            description = "Ubah pilihan filter atau tambahkan pengguna baru."
        )
        else -> UserEmptySpec(
            icon = OwnerIcons.CustomersEmptyGroup,
            iconContainerColor = SurfaceWhite,
            iconTint = Brand,
            title = "Belum ada pengguna",
            description = "Tambahkan akun admin atau kasir untuk mengelola operasional dan hak akses."
        )
    }
}

@Composable
internal fun OwnerUserEmptyRoute(
    uiState: UserManagementUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddUserClick: () -> Unit,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit,
    loadingContent: @Composable (isTablet: Boolean) -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val pageBack = LocalBackofficePageNavigation.current.onBack
    val navigateBack: () -> Unit = onBack ?: pageBack ?: {
        destinationNavigator?.invoke(AdminDestination.MoreHub) ?: onDashboardClick()
    }

    BackHandler(enabled = isSearchExpanded) {
        onCloseSearch()
    }
    BackHandler(enabled = !isSearchExpanded) {
        navigateBack()
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                destinationNavigator?.invoke(AdminDestination.TransactionsHub)
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub)
            DashboardNavDestination.STOK ->
                destinationNavigator?.invoke(AdminDestination.StockHub)
            DashboardNavDestination.MENU -> navigateBack()
        }
    }

    val emptySpec = userEmptyStateSpecFor(
        searchQuery = searchQuery,
        isFiltered = isFiltered
    )
    val summary = uiState.toUserEmptySummary()

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.staffMembers.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            OwnerUserEmptyScreen(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = navigateBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onNavSelect = ::handleNavSelect,
                panelContent = { isTablet ->
                    when {
                        uiState.isLoading && uiState.staffMembers.isEmpty() -> loadingContent(isTablet)
                        uiState.errorMessage != null && uiState.staffMembers.isEmpty() -> UserEmptyCenteredError(
                            message = uiState.errorMessage,
                            onRetry = onRefresh
                        )
                        else -> EmptyState(
                            icon = emptySpec.icon,
                            iconContainerColor = emptySpec.iconContainerColor,
                            iconTint = emptySpec.iconTint,
                            title = emptySpec.title,
                            description = emptySpec.description,
                            showAddButton = true,
                            onAddCustomerClick = onAddUserClick,
                            buttonTestTag = "add-user",
                            buttonLabel = "Tambah pengguna",
                            modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                        )
                    }
                }
            )
        }

        TopFloatingCapsule(
            message = uiState.actionSuccessMessage,
            error = uiState.actionErrorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        )
    }
}

@Composable
internal fun OwnerUserEmptyScreen(
    summary: UserEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    panelContent: @Composable (isTablet: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_USER_TABLET_BREAKPOINT
        if (isTablet) {
            TabletUserEmptyLayout(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch,
                panelContent = { panelContent(true) }
            )
        } else {
            PhoneUserEmptyLayout(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                autoFocusSearch = autoFocusSearch,
                panelContent = { panelContent(false) }
            )
        }
    }
}

@Composable
internal fun UserPhoneGreenHeader(
    summary: UserEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    autoFocusSearch: Boolean
) {
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
            if (isSearchExpanded) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChanged = onSearchChanged,
                    onCloseSearch = onCloseSearch,
                    autoFocus = autoFocusSearch,
                    placeholder = "Cari nama, username, atau email...",
                    backTestTag = "user-search-back",
                    inputTestTag = "user-search-input",
                    clearTestTag = "user-search-clear"
                )
            } else {
                SecondaryHeader(
                    title = "Pengguna & akses",
                    isFiltered = isFiltered,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter,
                    backTestTag = "user-back-btn",
                    searchTestTag = "user-toggle-search",
                    filterTestTag = "user-open-filter",
                    searchContentDescription = "Cari pengguna",
                    filterContentDescription = "Filter pengguna"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            UserSummaryStats(summary = summary)
        }
    }
}

@Composable
internal fun UserTabletGreenPanel(
    summary: UserEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    autoFocusSearch: Boolean
) {
    Box(
        modifier = Modifier
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
            if (isSearchExpanded) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChanged = onSearchChanged,
                    onCloseSearch = onCloseSearch,
                    autoFocus = autoFocusSearch,
                    placeholder = "Cari nama, username, atau email...",
                    backTestTag = "user-search-back",
                    inputTestTag = "user-search-input",
                    clearTestTag = "user-search-clear"
                )
            } else {
                SecondaryHeader(
                    title = "Pengguna & akses",
                    isFiltered = isFiltered,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter,
                    backTestTag = "user-back-btn",
                    searchTestTag = "user-toggle-search",
                    filterTestTag = "user-open-filter",
                    searchContentDescription = "Cari pengguna",
                    filterContentDescription = "Filter pengguna"
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                UserSummaryStats(summary = summary, isTablet = true)
            }
        }
    }
}

@Composable
internal fun UserSummaryStats(
    summary: UserEmptySummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val countFontSize = if (isTablet) 48.sp else 40.sp
    val countLineHeight = if (isTablet) 54.sp else 46.sp
    val countBoxHeight = if (isTablet) 54.dp else 46.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Total pengguna",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = summary.totalUsers.toString(),
                style = ownerTextStyle(
                    fontSize = countFontSize,
                    lineHeight = countLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.03).em,
                    tabularNumbers = true
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(countBoxHeight)
                    .alignByBaseline()
                    .testTag("user-total-count")
            )

            Text(
                text = "orang",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandSoftText
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(20.dp)
                    .alignByBaseline()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            PelangganStatColumn(
                label = "Aktif",
                value = "${summary.activeCount} orang",
                modifier = Modifier.weight(1f)
            )
            PelangganStatColumn(
                label = "Nonaktif",
                value = "${summary.inactiveCount} orang",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
            )
        }
    }
}

@Composable
private fun PhoneUserEmptyLayout(
    summary: UserEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    autoFocusSearch: Boolean,
    panelContent: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val viewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Layout(
                content = {
                    UserPhoneGreenHeader(
                        summary = summary,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        isFiltered = isFiltered,
                        onBack = onBack,
                        onOpenSearch = onOpenSearch,
                        onCloseSearch = onCloseSearch,
                        onSearchChanged = onSearchChanged,
                        onOpenFilter = onOpenFilter,
                        autoFocusSearch = autoFocusSearch
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        panelContent()
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val minPanelHeight = (viewportHeightPx - (headerPlaceable.height - overlapPx)).coerceAtLeast(0)
                val panelPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = minPanelHeight, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + panelPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)

                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    panelPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }
    }
}

@Composable
private fun TabletUserEmptyLayout(
    summary: UserEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean,
    panelContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            UserTabletGreenPanel(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                autoFocusSearch = autoFocusSearch
            )

            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                val panelViewportHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = panelViewportHeight)
                            .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        panelContent()
                    }
                }
            }
        }
    }
}

@Composable
internal fun OwnerUserLoadingSkeleton(compact: Boolean) {
    val count = if (compact) 4 else 5
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user-loading-skeleton")
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(120.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
            SkeletonBox(modifier = Modifier.width(96.dp).height(14.dp), shape = RoundedCornerShape(6.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        repeat(count) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(modifier = Modifier.width(140.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
                    SkeletonBox(modifier = Modifier.width(180.dp).height(12.dp), shape = RoundedCornerShape(6.dp))
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(modifier = Modifier.width(56.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
                    SkeletonBox(modifier = Modifier.width(72.dp).height(12.dp), shape = RoundedCornerShape(6.dp))
                }
            }
            if (index < count - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp)
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }
        }
    }
}

@Composable
internal fun UserEmptyCenteredError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("user-error-block"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Muted
            ),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = OnBrand),
            modifier = Modifier.testTag("user-retry-btn")
        ) {
            Text(
                text = "Muat ulang",
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBrand
                )
            )
        }
    }
}

@Preview(
    name = "OwnerUserEmptyPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerUserEmptyPhonePreview() {
    val spec = userEmptyStateSpecFor(searchQuery = "", isFiltered = false)
    TbterminalappTheme {
        OwnerUserEmptyScreen(
            summary = UserEmptySummaryUiState(),
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            autoFocusSearch = false,
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "add-user",
                    buttonLabel = "Tambah pengguna",
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerUserEmptyTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerUserEmptyTabletPreview() {
    val spec = userEmptyStateSpecFor(searchQuery = "", isFiltered = false)
    TbterminalappTheme {
        OwnerUserEmptyScreen(
            summary = UserEmptySummaryUiState(),
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            autoFocusSearch = false,
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "add-user",
                    buttonLabel = "Tambah pengguna",
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerUserSearchEmpty",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerUserSearchEmptyPreview() {
    val spec = userEmptyStateSpecFor(searchQuery = "agus", isFiltered = false)
    TbterminalappTheme {
        OwnerUserEmptyScreen(
            summary = UserEmptySummaryUiState(totalUsers = 4, activeCount = 3, inactiveCount = 1),
            isSearchExpanded = true,
            searchQuery = "agus",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            autoFocusSearch = false,
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "add-user",
                    buttonLabel = "Tambah pengguna",
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}
