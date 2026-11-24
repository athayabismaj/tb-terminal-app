package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.LocalBackofficePageNavigation
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.users.StaffMember
import com.tbterminal.app.ui.users.StaffRole
import com.tbterminal.app.ui.users.StaffStatus
import com.tbterminal.app.ui.users.UserManagementUiState
import com.tbterminal.app.ui.users.UserRoleFilter
import com.tbterminal.app.ui.users.UserSortOrder
import com.tbterminal.app.ui.users.UserStatusFilter

private val OWNER_USER_DATA_TABLET_BREAKPOINT = 840.dp

internal fun filterAndSortOwnerUsers(
    staffMembers: List<StaffMember>,
    searchQuery: String,
    statusFilter: UserStatusFilter,
    roleFilter: UserRoleFilter,
    sortOrder: UserSortOrder
): List<StaffMember> {
    val trimmedQuery = searchQuery.trim()

    val filtered = staffMembers
        .asSequence()
        .filter { staff ->
            when (statusFilter) {
                UserStatusFilter.All -> true
                UserStatusFilter.Active -> staff.status == StaffStatus.Active
                UserStatusFilter.Inactive -> staff.status == StaffStatus.Inactive
            }
        }
        .filter { staff ->
            when (roleFilter) {
                UserRoleFilter.All -> true
                UserRoleFilter.Admin -> staff.role == StaffRole.Admin
                UserRoleFilter.Kasir -> staff.role == StaffRole.Kasir
            }
        }
        .filter { staff ->
            if (trimmedQuery.isEmpty()) {
                true
            } else {
                listOf(staff.name, staff.username, staff.rawEmail.orEmpty())
                    .any { it.contains(trimmedQuery, ignoreCase = true) }
            }
        }
        .toList()

    return when (sortOrder) {
        UserSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        UserSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        UserSortOrder.RECENT_LOGIN -> filtered.sortedWith(
            compareByDescending<StaffMember> { it.lastLoginEpochMillis ?: Long.MIN_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }
}

private fun StaffRole.displayLabel(): String {
    return when (this) {
        StaffRole.Owner -> "Owner"
        StaffRole.Admin -> "Admin"
        StaffRole.Kasir -> "Kasir"
        StaffRole.Other -> "Staf"
    }
}

@Composable
internal fun OwnerUserRoute(
    uiState: UserManagementUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    statusFilter: UserStatusFilter,
    roleFilter: UserRoleFilter,
    sortOrder: UserSortOrder,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddUserClick: () -> Unit,
    onUserDetailClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    onDashboardClick: () -> Unit,
    onDismissActionMessage: () -> Unit
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

    val isFiltered = statusFilter != UserStatusFilter.All || roleFilter != UserRoleFilter.All
    val displayedUsers = remember(
        uiState.staffMembers,
        searchQuery,
        statusFilter,
        roleFilter,
        sortOrder
    ) {
        filterAndSortOwnerUsers(
            staffMembers = uiState.staffMembers,
            searchQuery = searchQuery,
            statusFilter = statusFilter,
            roleFilter = roleFilter,
            sortOrder = sortOrder
        )
    }

    val summary = remember(uiState.staffMembers, uiState.totalStaff) {
        uiState.toUserEmptySummary()
    }

    val isInitialLoading = uiState.isLoading && uiState.staffMembers.isEmpty()
    val isInitialError = uiState.errorMessage != null && uiState.staffMembers.isEmpty() && !uiState.isLoading
    val showEmptyState = !isInitialLoading && !isInitialError && displayedUsers.isEmpty()
    val emptySpec = userEmptyStateSpecFor(
        searchQuery = searchQuery,
        isFiltered = isFiltered
    )

    var pendingToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        OwnerUserToastBridge.consumePendingSuccess()?.let {
            pendingToastMessage = it
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.staffMembers.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading || isInitialError || showEmptyState) {
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
                            isInitialLoading -> OwnerUserLoadingSkeleton(compact = !isTablet)
                            isInitialError -> UserEmptyCenteredError(
                                message = uiState.errorMessage.orEmpty(),
                                onRetry = onRefresh
                            )
                            else -> EmptyState(
                                icon = emptySpec.icon,
                                iconContainerColor = emptySpec.iconContainerColor,
                                iconTint = emptySpec.iconTint,
                                title = emptySpec.title,
                                description = emptySpec.description,
                                showAddButton = searchQuery.isBlank(),
                                onAddCustomerClick = onAddUserClick,
                                buttonTestTag = "add-user",
                                buttonLabel = "Tambah pengguna",
                                modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                            )
                        }
                    }
                )
            } else {
                OwnerUserDataScreen(
                    summary = summary,
                    staffMembers = displayedUsers,
                    sortLabel = sortOrder.label,
                    statusFilter = statusFilter,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = searchQuery,
                    isFiltered = isFiltered,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onOpenFilter = onOpenFilter,
                    onAddUserClick = onAddUserClick,
                    onStaffClick = { onUserDetailClick(it.id) },
                    onNavSelect = ::handleNavSelect
                )
            }
        }

        TopFloatingCapsule(
            message = uiState.actionSuccessMessage,
            error = uiState.actionErrorMessage,
            onDismiss = onDismissActionMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
        )

        AppAlert(
            type = AlertType.Success,
            title = pendingToastMessage.orEmpty(),
            message = "",
            visible = !pendingToastMessage.isNullOrBlank(),
            onDismiss = { pendingToastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .zIndex(99f)
        )
    }
}

@Composable
internal fun OwnerUserDataScreen(
    summary: UserEmptySummaryUiState,
    staffMembers: List<StaffMember>,
    sortLabel: String,
    statusFilter: UserStatusFilter = UserStatusFilter.All,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddUserClick: () -> Unit,
    onStaffClick: (StaffMember) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true,
    listState: LazyListState = rememberLazyListState()
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_USER_DATA_TABLET_BREAKPOINT
        if (isTablet) {
            TabletUserDataLayout(
                summary = summary,
                staffMembers = staffMembers,
                sortLabel = sortLabel,
                statusFilter = statusFilter,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                listState = listState,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onAddUserClick = onAddUserClick,
                onStaffClick = onStaffClick,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch
            )
        } else {
            PhoneUserDataLayout(
                summary = summary,
                staffMembers = staffMembers,
                sortLabel = sortLabel,
                statusFilter = statusFilter,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                listState = listState,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onAddUserClick = onAddUserClick,
                onStaffClick = onStaffClick,
                autoFocusSearch = autoFocusSearch
            )
        }
    }
}

@Composable
private fun UserListHeaderSection(
    sortLabel: String,
    statusFilter: UserStatusFilter,
    onAddUserClick: () -> Unit
) {
    val sectionTitle = when (statusFilter) {
        UserStatusFilter.All -> "Semua pengguna"
        UserStatusFilter.Active -> "Pengguna aktif"
        UserStatusFilter.Inactive -> "Pengguna nonaktif"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AddPelangganPrimaryButton(
            label = "Tambah pengguna",
            onClick = onAddUserClick,
            testTag = "add-user",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier
                    .cssLineBoxHeight(20.dp)
                    .semantics { heading() }
            )

            Text(
                text = "Urut: $sortLabel",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .cssLineBoxHeight(18.dp)
                    .testTag("user-sort-label")
            )
        }
    }
}

@Composable
private fun PhoneUserDataLayout(
    summary: UserEmptySummaryUiState,
    staffMembers: List<StaffMember>,
    sortLabel: String,
    statusFilter: UserStatusFilter,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddUserClick: () -> Unit,
    onStaffClick: (StaffMember) -> Unit,
    autoFocusSearch: Boolean
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .testTag("owner-user-lazy-list")
    ) {
        item(key = "user-header-cap") {
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
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp)
                    ) {
                        UserListHeaderSection(
                            sortLabel = sortLabel,
                            statusFilter = statusFilter,
                            onAddUserClick = onAddUserClick
                        )
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val capPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + capPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)
                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    capPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }

        itemsIndexed(
            items = staffMembers,
            key = { _, staff -> staff.id }
        ) { index, staff ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 20.dp)
            ) {
                UserRow(
                    staff = staff,
                    onClick = { onStaffClick(staff) }
                )
                if (index < staffMembers.lastIndex) {
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

        item(key = "user-footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                ListFooterState(
                    status = PelangganFooterStatus.END_OF_LIST,
                    onRetry = {},
                    loadingText = "Memuat pengguna lainnya\u2026",
                    endText = "Semua pengguna sudah ditampilkan",
                    footerTestTag = "user-list-footer",
                    retryTestTag = "user-footer-retry"
                )
            }
        }
    }
}

@Composable
private fun TabletUserDataLayout(
    summary: UserEmptySummaryUiState,
    staffMembers: List<StaffMember>,
    sortLabel: String,
    statusFilter: UserStatusFilter,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddUserClick: () -> Unit,
    onStaffClick: (StaffMember) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean
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

            Box(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("owner-user-lazy-list"),
                    contentPadding = PaddingValues(start = 64.dp, top = 32.dp, end = 32.dp, bottom = 32.dp)
                ) {
                    item(key = "user-top-controls") {
                        UserListHeaderSection(
                            sortLabel = sortLabel,
                            statusFilter = statusFilter,
                            onAddUserClick = onAddUserClick
                        )
                    }

                    itemsIndexed(
                        items = staffMembers,
                        key = { _, staff -> staff.id }
                    ) { index, staff ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            UserRow(
                                staff = staff,
                                onClick = { onStaffClick(staff) }
                            )
                            if (index < staffMembers.lastIndex) {
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

                    item(key = "user-footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            ListFooterState(
                                status = PelangganFooterStatus.END_OF_LIST,
                                onRetry = {},
                                loadingText = "Memuat pengguna lainnya\u2026",
                                endText = "Semua pengguna sudah ditampilkan",
                                footerTestTag = "user-list-footer",
                                retryTestTag = "user-footer-retry"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    staff: StaffMember,
    onClick: () -> Unit
) {
    val isActive = staff.status == StaffStatus.Active
    val subtitle = buildString {
        append("@${staff.username}")
        staff.rawEmail?.takeIf(String::isNotBlank)?.let { email ->
            append(" \u00B7 ")
            append(email)
        }
    }
    val roleLabel = staff.role.displayLabel()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 16.dp)
            .testTag("user-row-${staff.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusAvatar(
            name = staff.name,
            isActive = isActive
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = staff.name,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) Ink else Muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(18.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = roleLabel,
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Ink else Muted
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isActive) staff.lastLogin else "Nonaktif",
                style = ownerTextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isActive) Muted else FieldErrorColor
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(16.dp)
            )
        }

        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronRight,
                tint = Muted,
                size = 20.dp
            )
        }
    }
}

internal fun sampleReferenceUsers(): List<StaffMember> = listOf(
    StaffMember(
        id = "u1",
        name = "Andi Pratama",
        email = "andi@tbterminal.id",
        rawEmail = "andi@tbterminal.id",
        username = "andi.admin",
        roleId = "r-admin",
        role = StaffRole.Admin,
        status = StaffStatus.Active,
        lastLogin = "15 menit lalu",
        lastLoginEpochMillis = 1_760_000_000_000L,
        initials = "AP"
    ),
    StaffMember(
        id = "u2",
        name = "Budi Kasir",
        email = "Email belum diisi",
        rawEmail = null,
        username = "budi.kasir",
        roleId = "r-kasir",
        role = StaffRole.Kasir,
        status = StaffStatus.Active,
        lastLogin = "Baru saja",
        lastLoginEpochMillis = 1_760_001_000_000L,
        initials = "BK"
    ),
    StaffMember(
        id = "u3",
        name = "Sari Wijaya",
        email = "sari@tbterminal.id",
        rawEmail = "sari@tbterminal.id",
        username = "sari.kasir",
        roleId = "r-kasir",
        role = StaffRole.Kasir,
        status = StaffStatus.Inactive,
        lastLogin = "3 hari lalu",
        lastLoginEpochMillis = 1_759_700_000_000L,
        initials = "SW"
    )
)

@Preview(
    name = "OwnerUserDataPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerUserDataPhonePreview() {
    val sample = sampleReferenceUsers()
    TbterminalappTheme {
        OwnerUserDataScreen(
            summary = UserEmptySummaryUiState(totalUsers = 3L, activeCount = 2, inactiveCount = 1),
            staffMembers = sample,
            sortLabel = UserSortOrder.NAME_ASC.label,
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onAddUserClick = {},
            onStaffClick = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}

@Preview(
    name = "OwnerUserDataTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerUserDataTabletPreview() {
    val sample = sampleReferenceUsers()
    TbterminalappTheme {
        OwnerUserDataScreen(
            summary = UserEmptySummaryUiState(totalUsers = 3L, activeCount = 2, inactiveCount = 1),
            staffMembers = sample,
            sortLabel = UserSortOrder.NAME_ASC.label,
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onAddUserClick = {},
            onStaffClick = {},
            onNavSelect = {},
            autoFocusSearch = false
        )
    }
}
