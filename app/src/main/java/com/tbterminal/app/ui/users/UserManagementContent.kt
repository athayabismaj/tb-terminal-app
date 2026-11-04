package com.tbterminal.app.ui.users

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.components.AppConfirmDialog
import com.tbterminal.app.ui.components.AppConfirmationSpec
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.theme.TbBackground

// ── COLOR PALETTE ACCORDING TO HTML & RIWAYAT PENJUALAN DESIGN ──
private val BrandSage = Color(0xFF256B57)
private val BrandSageDark = Color(0xFF1D5344)
private val BrandSageLight = Color(0xFFEAF3EF)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val RoleOwnerBg = Color(0xFFFEF3C7)
private val RoleOwnerText = Color(0xFFD97706)
private val RoleAdminBg = Color(0xFFE7F3EF)
private val RoleAdminText = Color(0xFF256B57)
private val RoleKasirBg = Color(0xFFF1F5F9)
private val RoleKasirText = Color(0xFF334155)

enum class UserStatusFilter(val label: String) {
    All("Semua status"),
    Active("Aktif"),
    Inactive("Nonaktif"),
}

enum class UserRoleFilter(val label: String) {
    All("Semua"),
    Owner("Owner"),
    Admin("Admin"),
    Kasir("Kasir"),
}

@Composable
fun UserManagementContent(
    modifier: Modifier = Modifier,
    uiState: UserManagementUiState,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    statusFilter: UserStatusFilter = UserStatusFilter.All,
    onStatusFilterChanged: (UserStatusFilter) -> Unit = {},
    roleFilter: UserRoleFilter = UserRoleFilter.All,
    onRoleFilterChanged: (UserRoleFilter) -> Unit = {},
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onAddUserClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditUserClick: (StaffMember) -> Unit = {},
    onChangePasswordClick: (StaffMember) -> Unit = {},
    onChangePinClick: (StaffMember) -> Unit = {},
    onDeactivateUserClick: (StaffMember) -> Unit = {},
    onActivateUserClick: (StaffMember) -> Unit = {},
    onDismissActionMessage: () -> Unit = {},
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(1) }

    val hasActiveFilter = statusFilter != UserStatusFilter.All || roleFilter != UserRoleFilter.All
    val isSearchActive = isSearchExpanded || searchQuery.isNotEmpty()

    val filteredStaff = remember(uiState.staffMembers, searchQuery, statusFilter, roleFilter) {
        uiState.staffMembers.filter { staff ->
            val matchesQuery = searchQuery.isBlank() || listOf(staff.name, staff.username, staff.email)
                .any { it.contains(searchQuery.trim(), ignoreCase = true) }
            val matchesStatus = when (statusFilter) {
                UserStatusFilter.All -> true
                UserStatusFilter.Active -> staff.status == StaffStatus.Active
                UserStatusFilter.Inactive -> staff.status == StaffStatus.Inactive
            }
            val matchesRole = when (roleFilter) {
                UserRoleFilter.All -> true
                UserRoleFilter.Owner -> staff.role == StaffRole.Owner
                UserRoleFilter.Admin -> staff.role == StaffRole.Admin
                UserRoleFilter.Kasir -> staff.role == StaffRole.Kasir
            }
            matchesQuery && matchesStatus && matchesRole
        }
    }
    val totalPages = ((filteredStaff.size + USER_PAGE_SIZE - 1) / USER_PAGE_SIZE).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(1, totalPages)
    val pageStart = (safePage - 1) * USER_PAGE_SIZE
    val visibleStaff = filteredStaff.drop(pageStart).take(USER_PAGE_SIZE)

    LaunchedEffect(searchQuery, statusFilter, roleFilter) {
        currentPage = 1
    }
    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) currentPage = totalPages
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(TbBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1200.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 12.dp else 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            if (compact) {
                // Expandable Search Bar on Compact Screen
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("user-search-expandable"),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandSage.copy(alpha = 0.5f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = BrandSage,
                                modifier = Modifier.size(18.dp)
                            )
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChanged,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("user-search-input"),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color(0xFF1E293B),
                                    fontSize = 13.sp
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Cari nama atau username...",
                                            color = Slate400,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChanged("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Hapus pencarian",
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Filter Chips on Compact Screen
                if (hasActiveFilter) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Filter:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )

                        if (roleFilter != UserRoleFilter.All) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandSageLight,
                                border = BorderStroke(1.dp, BrandSage.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = roleFilter.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandSage
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { onRoleFilterChanged(UserRoleFilter.All) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                        }

                        if (statusFilter != UserStatusFilter.All) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandSageLight,
                                border = BorderStroke(1.dp, BrandSage.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = statusFilter.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandSage
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { onStatusFilterChanged(UserStatusFilter.All) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onRoleFilterChanged(UserRoleFilter.All)
                                    onStatusFilterChanged(UserStatusFilter.All)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                // ── 1. SEARCH & ACTIONS TOOLBAR ──
                UserToolbar(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    hasActiveFilter = hasActiveFilter,
                    compact = compact,
                    onOpenFilters = { showFilters = true },
                )
            }

            // ── 2. SECTION HEADER (DAFTAR PENGGUNA & TOMBOL TAMBAH) ──
            UserSectionHeader(
                onAddUserClick = onAddUserClick,
            )

            // ── 3. USER LIST / UNIFIED CARD ──
            when {
                uiState.isLoading -> {
                    UserListSkeleton(compact = compact)
                }
                uiState.errorMessage != null -> {
                    UserFeedback(
                        message = uiState.errorMessage,
                        actionLabel = "Muat ulang",
                        onAction = onRetry,
                    )
                }
                visibleStaff.isEmpty() -> {
                    UserFeedback(
                        message = if (searchQuery.isNotBlank() || hasActiveFilter) {
                            "Tidak ada pengguna yang cocok."
                        } else {
                            "Belum ada pengguna."
                        },
                    )
                }
                compact -> {
                    // Unified Card Container matching HTML mockup
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user-unified-card"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200),
                        shadowElevation = 0.5.dp,
                    ) {
                        Column {
                            visibleStaff.forEachIndexed { index, staff ->
                                StaffItem(
                                    staff = staff,
                                    actionsEnabled = !uiState.isMutating,
                                    onEditUserClick = onEditUserClick,
                                    onChangePasswordClick = onChangePasswordClick,
                                    onChangePinClick = onChangePinClick,
                                    onDeactivateUserClick = onDeactivateUserClick,
                                    onActivateUserClick = onActivateUserClick,
                                )
                                if (index < visibleStaff.lastIndex) {
                                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Desktop / Tablet: 2-column grid of StaffCard
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        visibleStaff.chunked(2).forEach { rowUsers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowUsers.forEach { staff ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("user-${staff.id}"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Slate200),
                                        shadowElevation = 0.5.dp,
                                    ) {
                                        StaffItem(
                                            staff = staff,
                                            actionsEnabled = !uiState.isMutating,
                                            onEditUserClick = onEditUserClick,
                                            onChangePasswordClick = onChangePasswordClick,
                                            onChangePinClick = onChangePinClick,
                                            onDeactivateUserClick = onDeactivateUserClick,
                                            onActivateUserClick = onActivateUserClick,
                                        )
                                    }
                                }
                                if (rowUsers.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. PAGINATION CONTROLS ──
            if (!uiState.isLoading && uiState.errorMessage == null && filteredStaff.isNotEmpty()) {
                val firstItem = ((safePage - 1) * USER_PAGE_SIZE) + 1
                val lastItem = minOf(safePage * USER_PAGE_SIZE, filteredStaff.size)
                UserPagination(
                    firstItem = firstItem,
                    lastItem = lastItem,
                    totalCount = filteredStaff.size,
                    currentPage = safePage,
                    totalPages = totalPages,
                    onPreviousPage = { currentPage = (safePage - 1).coerceAtLeast(1) },
                    onNextPage = { currentPage = (safePage + 1).coerceAtMost(totalPages) },
                )
            }

            Spacer(Modifier.height(4.dp))
        }

        TopFloatingCapsule(
            message = uiState.actionSuccessMessage,
            error = uiState.actionErrorMessage,
            onDismiss = onDismissActionMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )

        // ── FILTER BOTTOM SHEET ──
        if (showFilters || showFilterPopup) {
            UserFilterSheet(
                roleFilter = roleFilter,
                statusFilter = statusFilter,
                onApplyFilter = { r, s ->
                    onRoleFilterChanged(r)
                    onStatusFilterChanged(s)
                },
                onDismiss = {
                    showFilters = false
                    onDismissFilter()
                },
            )
        }

        // ── SUMMARY BOTTOM SHEET ──
        if (showSummary || showSummaryPopup) {
            UserSummarySheet(
                uiState = uiState,
                onDismiss = {
                    showSummary = false
                    onDismissSummary()
                },
            )
        }
    }
}

@Composable
fun UserHeaderAddButton(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .testTag("add-user"),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Tambah pengguna",
            tint = Color(0xFF1E293B),
            modifier = Modifier.size(22.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TOOLBAR (SEARCH BAR & FILTER BUTTON)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserToolbar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    hasActiveFilter: Boolean,
    compact: Boolean,
    onOpenFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("user-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Search field (height 42.dp on compact, 48.dp on desktop)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(if (compact) 42.dp else 48.dp)
                .testTag("user-search"),
            color = Color.White,
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            border = BorderStroke(1.dp, Slate300),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Cari pengguna",
                    tint = Slate400,
                    modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Cari nama atau username",
                            fontSize = if (compact) 13.sp else 14.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Slate900,
                            fontSize = if (compact) 13.sp else 14.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChanged("") },
                        modifier = Modifier.size(if (compact) 24.dp else 36.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Hapus pencarian",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Filter button (size 42.dp on compact, 48.dp on desktop) with orange dot indicator
        Surface(
            onClick = onOpenFilters,
            modifier = Modifier
                .size(if (compact) 42.dp else 48.dp)
                .testTag("user-open-filter"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (hasActiveFilter) Color(0xFF0F172A) else Slate300),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter pengguna",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF0F172A),
                )
                if (hasActiveFilter) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(7.dp)
                            .background(Color(0xFFF59E0B), CircleShape),
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION HEADER (DAFTAR PENGGUNA & TOMBOL TAMBAH)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserSectionHeader(
    onAddUserClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Daftar pengguna",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )

        Surface(
            onClick = onAddUserClick,
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFF256B57)),
            contentColor = Color(0xFF256B57),
            modifier = Modifier
                .size(32.dp)
                .testTag("add-user"),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah pengguna",
                    tint = Color(0xFF256B57),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// USER ITEM CARD (MATCHES HTML ARTICLE LAYOUT 1:1)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun StaffItem(
    staff: StaffMember,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEditUserClick: (StaffMember) -> Unit,
    onChangePasswordClick: (StaffMember) -> Unit,
    onChangePinClick: (StaffMember) -> Unit,
    onDeactivateUserClick: (StaffMember) -> Unit,
    onActivateUserClick: (StaffMember) -> Unit,
) {
    val inactive = staff.status == StaffStatus.Inactive

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user-${staff.id}")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Top Row: User Avatar, Name Stack, Role Badge, and Options Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Avatar Circle (44.dp)
                val avatarBg = when {
                    inactive -> Slate100
                    staff.role == StaffRole.Admin || staff.role == StaffRole.Owner -> BrandSageLight
                    else -> Slate100
                }
                val avatarText = when {
                    inactive -> Slate400
                    staff.role == StaffRole.Admin || staff.role == StaffRole.Owner -> BrandSage
                    else -> Slate700
                }
                val avatarBorder = when {
                    inactive -> Slate200
                    staff.role == StaffRole.Admin || staff.role == StaffRole.Owner -> BrandSage.copy(alpha = 0.15f)
                    else -> Slate200.copy(alpha = 0.6f)
                }

                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = avatarBg,
                    border = BorderStroke(1.dp, avatarBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = staff.initials.take(1).ifBlank { staff.name.take(1) }.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = avatarText,
                        )
                    }
                }

                // Username Stack
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = staff.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inactive) Slate500 else Slate900,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@${staff.username}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (inactive) {
                        Text(
                            text = "Akun nonaktif",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE11D48),
                            lineHeight = 14.sp,
                        )
                    }
                }
            }

            // Right: Role Pill & Context Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StaffRoleBadge(role = staff.role, inactive = inactive)

                UserActionsMenu(
                    staff = staff,
                    enabled = actionsEnabled,
                    onEditUserClick = onEditUserClick,
                    onChangePasswordClick = onChangePasswordClick,
                    onChangePinClick = onChangePinClick,
                    onDeactivateUserClick = onDeactivateUserClick,
                    onActivateUserClick = onActivateUserClick,
                )
            }
        }

        // Bottom Row: Metadata Info (Email & Last Login)
        HorizontalDivider(color = Slate100, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Email Block
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                    tint = Slate400,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Email",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        lineHeight = 14.sp,
                    )
                    Text(
                        text = staff.email.ifBlank { "-" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Last Login Block
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                    tint = Slate400,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Login terakhir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        lineHeight = 14.sp,
                    )
                    Text(
                        text = staff.lastLogin,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// ROLE BADGE (EXACT HTML PILL DESIGN)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun StaffRoleBadge(role: StaffRole, inactive: Boolean) {
    val (label, bg, text) = when {
        inactive -> Triple("Nonaktif", Slate100, Slate400)
        role == StaffRole.Admin -> Triple("Admin", RoleAdminBg, RoleAdminText)
        role == StaffRole.Owner -> Triple("Owner", RoleOwnerBg, RoleOwnerText)
        role == StaffRole.Kasir -> Triple("Kasir", RoleKasirBg, RoleKasirText)
        else -> Triple("Lainnya", Slate100, Slate600)
    }

    Surface(
        shape = CircleShape,
        color = bg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = text,
            lineHeight = 14.sp,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PAGINATION CONTROLS (MATCHES RIWAYAT PENJUALAN & HTML MOCKUP)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserPagination(
    firstItem: Int,
    lastItem: Int,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .testTag("user-pagination"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Range Information Pill
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 0.5.dp,
        ) {
            Text(
                text = "$firstItem-$lastItem dari $totalCount data",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
            )
        }

        // Pager Buttons Pill
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 0.5.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 1,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Halaman sebelumnya",
                        modifier = Modifier.size(18.dp),
                        tint = if (currentPage > 1) Slate700 else Slate300,
                    )
                }

                Text(
                    text = "$currentPage / $totalPages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Halaman berikutnya",
                        modifier = Modifier.size(18.dp),
                        tint = if (currentPage < totalPages) Slate700 else Slate300,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SKELETON LOADING
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserListSkeleton(compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp,
    ) {
        Column {
            val count = if (compact) 3 else 4
            repeat(count) { index ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SkeletonBox(Modifier.size(44.dp).clip(CircleShape))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SkeletonBox(Modifier.width(110.dp).height(14.dp))
                                SkeletonBox(Modifier.width(70.dp).height(12.dp))
                            }
                        }
                        SkeletonBox(Modifier.width(60.dp).height(24.dp).clip(CircleShape))
                    }
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SkeletonBox(Modifier.weight(1f).height(18.dp))
                        SkeletonBox(Modifier.weight(1f).height(18.dp))
                    }
                }
                if (index < count - 1) {
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// ACTIONS CONTEXT MENU
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserActionsMenu(
    staff: StaffMember,
    enabled: Boolean,
    onEditUserClick: (StaffMember) -> Unit,
    onChangePasswordClick: (StaffMember) -> Unit,
    onChangePinClick: (StaffMember) -> Unit,
    onDeactivateUserClick: (StaffMember) -> Unit,
    onActivateUserClick: (StaffMember) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val inactive = staff.status == StaffStatus.Inactive
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "Menu opsi pengguna",
                tint = Slate400,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 210.dp).background(Color.White),
            shape = RoundedCornerShape(16.dp),
        ) {
            UserMenuItem("Edit pengguna", Icons.Outlined.Edit) {
                expanded = false
                onEditUserClick(staff)
            }
            if (inactive) {
                UserMenuItem("Aktifkan kembali", Icons.Outlined.CheckCircle, BrandSage) {
                    expanded = false
                    onActivateUserClick(staff)
                }
            } else {
                UserMenuItem("Ubah password", Icons.Outlined.Password) {
                    expanded = false
                    onChangePasswordClick(staff)
                }
                UserMenuItem("Ubah PIN", Icons.Outlined.Dialpad) {
                    expanded = false
                    onChangePinClick(staff)
                }
                UserMenuItem("Nonaktifkan", Icons.Outlined.Block, Color(0xFFE11D48)) {
                    expanded = false
                    onDeactivateUserClick(staff)
                }
            }
        }
    }
}

@Composable
private fun UserMenuItem(label: String, icon: ImageVector, color: Color = Slate900, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = color, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) },
        contentPadding = PaddingValues(horizontal = 14.dp),
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// FILTER MODAL BOTTOM SHEET (MATCHES CUSTOMER FILTER SHEET DESIGN)
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFilterSheet(
    roleFilter: UserRoleFilter,
    statusFilter: UserStatusFilter,
    onApplyFilter: (UserRoleFilter, UserStatusFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempRoleFilter by remember(roleFilter) { mutableStateOf(roleFilter) }
    var tempStatusFilter by remember(statusFilter) { mutableStateOf(statusFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp)),
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .testTag("user-filter-sheet"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Filter Pengguna",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )
                    Text(
                        text = "Atur peran dan status akun pengguna",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Group 1: PERAN PENGGUNA
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PERAN PENGGUNA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF64748B),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                        .testTag("user-filter"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UserRoleFilter.entries.forEach { filter ->
                        val isSelected = filter == tempRoleFilter
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tempRoleFilter = filter }
                                .testTag("user-role-filter-${filter.name}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color.White else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                        ) {
                            Text(
                                text = filter.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Group 2: STATUS AKUN
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STATUS AKUN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF64748B),
                )
                UserStatusFilter.entries.forEach { statusOption ->
                    UserFilterOptionCard(
                        title = statusOption.label,
                        selected = tempStatusFilter == statusOption,
                        onClick = { tempStatusFilter = statusOption },
                        testTag = "user-status-option-${statusOption.name}"
                    )
                }
            }

            // Sticky footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        tempRoleFilter = UserRoleFilter.All
                        tempStatusFilter = UserStatusFilter.All
                        onApplyFilter(UserRoleFilter.All, UserStatusFilter.All)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Text("Reset", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onApplyFilter(tempRoleFilter, tempStatusFilter)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("user-filter-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSage),
                ) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UserFilterOptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String = "",
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) BrandSageLight else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (selected) BrandSage else Color(0xFFE2E8F0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) BrandSage else Color(0xFF334155),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = BrandSage,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSummarySheet(
    uiState: UserManagementUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val inactive = (uiState.staffMembers.size - uiState.activeStaff).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp)),
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .testTag("user-summary-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ringkasan Pengguna",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TOTAL PENGGUNA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF64748B),
                    )
                    Text(
                        text = if (uiState.isLoading) "—" else "${uiState.totalStaff} Pengguna",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandSage,
                    )
                }
            }

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Pengguna Aktif", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text(if (uiState.isLoading) "—" else "${uiState.activeStaff} Pengguna", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Dinonaktifkan", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text(if (uiState.isLoading) "—" else "$inactive Pengguna", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("mobile-sheet-done"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandSage,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UserSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// USER FEEDBACK (ERROR & EMPTY STATE)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserFeedback(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Group, contentDescription = null, tint = Slate400, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Slate500)
            if (actionLabel != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = BrandSage) }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// ACTION BANNER
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserActionBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val background = if (isError) MaterialTheme.colorScheme.errorContainer else BrandSageLight
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else BrandSageDark
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Tutup") }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// DEACTIVATE CONFIRMATION DIALOG
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun StaffDeactivateDialog(
    staff: StaffMember,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppConfirmDialog(
        spec = AppConfirmationSpec(
            title = "Nonaktifkan pengguna",
            target = staff.name,
            consequence = "Akun tidak dapat digunakan untuk login sampai diaktifkan kembali.",
            confirmLabel = "Nonaktifkan",
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        isLoading = isSaving,
    )
}

private const val USER_PAGE_SIZE = 10
private val BrandSageBorder = Color(0xFFC7E0D6)
