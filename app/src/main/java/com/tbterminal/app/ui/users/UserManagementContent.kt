package com.tbterminal.app.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.components.SkeletonList
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbAmberLight
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

private enum class UserStatusFilter(val label: String) {
    All("Semua"),
    Active("Aktif"),
    Inactive("Nonaktif"),
}

@Composable
fun UserManagementContent(
    modifier: Modifier = Modifier,
    uiState: UserManagementUiState,
    onAddUserClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditUserClick: (StaffMember) -> Unit = {},
    onChangePasswordClick: (StaffMember) -> Unit = {},
    onChangePinClick: (StaffMember) -> Unit = {},
    onDeactivateUserClick: (StaffMember) -> Unit = {},
    onActivateUserClick: (StaffMember) -> Unit = {},
    onDismissActionMessage: () -> Unit = {},
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(UserStatusFilter.All) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    val filteredStaff = remember(uiState.staffMembers, searchQuery, statusFilter) {
        uiState.staffMembers.filter { staff ->
            val matchesQuery = searchQuery.isBlank() || listOf(staff.name, staff.username, staff.email)
                .any { it.contains(searchQuery.trim(), ignoreCase = true) }
            val matchesStatus = when (statusFilter) {
                UserStatusFilter.All -> true
                UserStatusFilter.Active -> staff.status == StaffStatus.Active
                UserStatusFilter.Inactive -> staff.status == StaffStatus.Inactive
            }
            matchesQuery && matchesStatus
        }
    }
    val totalPages = ((filteredStaff.size + USER_PAGE_SIZE - 1) / USER_PAGE_SIZE).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(1, totalPages)
    val pageStart = (safePage - 1) * USER_PAGE_SIZE
    val visibleStaff = filteredStaff.drop(pageStart).take(USER_PAGE_SIZE)

    LaunchedEffect(searchQuery, statusFilter) {
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
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 18.dp else 26.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 20.dp),
        ) {
            uiState.actionErrorMessage?.let {
                UserActionBanner(it, isError = true, onDismiss = onDismissActionMessage)
            } ?: uiState.actionSuccessMessage?.let {
                UserActionBanner(it, isError = false, onDismiss = onDismissActionMessage)
            }

            UserToolbar(
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                selectedStatus = statusFilter,
                onStatusSelected = { statusFilter = it },
                compact = compact,
                onOpenFilters = { showFilters = true },
                onAddUserClick = onAddUserClick,
            )

            UserListSection(
                staffMembers = visibleStaff,
                filteredResultCount = filteredStaff.size,
                hasActiveFilter = searchQuery.isNotBlank() || statusFilter != UserStatusFilter.All,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                isMutating = uiState.isMutating,
                compact = compact,
                currentPage = safePage,
                totalPages = totalPages,
                onPreviousPage = { currentPage = (safePage - 1).coerceAtLeast(1) },
                onNextPage = { currentPage = (safePage + 1).coerceAtMost(totalPages) },
                onOpenSummary = { showSummary = true },
                onRetry = onRetry,
                onEditUserClick = onEditUserClick,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onDeactivateUserClick = onDeactivateUserClick,
                onActivateUserClick = onActivateUserClick,
            )

            Spacer(Modifier.height(8.dp))
        }

        if (compact && showFilters) {
            TbMobileControlSheet(
                title = "Filter pengguna",
                subtitle = "Pilih status akun yang ditampilkan",
                onDismiss = { showFilters = false },
                testTag = "user-filter-sheet",
            ) {
                Text("Status akun", style = MaterialTheme.typography.labelLarge, color = TbText, fontWeight = FontWeight.SemiBold)
                UserStatusOptions(
                    selectedStatus = statusFilter,
                    onStatusSelected = { statusFilter = it },
                    fillWidth = true,
                )
                TbMobileSheetDoneButton(
                    onClick = { showFilters = false },
                    testTag = "user-filter-done",
                )
            }
        }

        if (showSummary) {
            TbMobileControlSheet(
                title = "Rincian pengguna",
                subtitle = "Ringkasan akun yang dikelola",
                onDismiss = { showSummary = false },
                testTag = "user-summary-sheet",
            ) {
                UserSummary(uiState = uiState)
                TbMobileSheetDoneButton(
                    onClick = { showSummary = false },
                    label = "Tutup",
                    testTag = "user-summary-done",
                )
            }
        }
    }
}

@Composable
private fun UserSummary(uiState: UserManagementUiState) {
    val inactive = (uiState.staffMembers.size - uiState.activeStaff).coerceAtLeast(0)
    val totalValue = if (uiState.isLoading) "—" else uiState.totalStaff.toString()
    val activeValue = if (uiState.isLoading) "—" else uiState.activeStaff.toString()
    val inactiveValue = if (uiState.isLoading) "—" else inactive.toString()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = TbGreenLight,
                    contentColor = TbGreenDark,
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(23.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Total pengguna",
                        style = MaterialTheme.typography.titleSmall,
                        color = TbText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Akun yang dapat dikelola",
                        style = MaterialTheme.typography.bodySmall,
                        color = TbTextMuted,
                    )
                }
                Text(
                    text = totalValue,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TbText,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.fillMaxWidth().height(1.dp).background(TbOutline.copy(alpha = 0.7f)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryStatus(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Pengguna aktif",
                    value = activeValue,
                    tint = TbGreenDark,
                    modifier = Modifier.weight(1f),
                )
                SummaryStatus(
                    icon = Icons.Outlined.Block,
                    label = "Dinonaktifkan",
                    value = inactiveValue,
                    tint = TbTextMuted,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryStatus(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TbTextMuted, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UserToolbar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedStatus: UserStatusFilter,
    onStatusSelected: (UserStatusFilter) -> Unit,
    compact: Boolean,
    onOpenFilters: () -> Unit,
    onAddUserClick: () -> Unit,
) {
    val search: @Composable (Modifier) -> Unit = { searchModifier ->
        androidx.compose.material3.Surface(
            modifier = searchModifier.height(48.dp).testTag("user-search"),
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TbOutline.copy(alpha = 0.8f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Search, "Cari pengguna", tint = TbTextMuted, modifier = Modifier.size(20.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TbText),
                    decorationBox = @Composable { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isBlank()) {
                                Text("Cari nama atau username", color = TbTextMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    },
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChanged("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp), tint = TbTextMuted)
                    }
                }
            }
        }
    }
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            search(Modifier.weight(1f))
            TbMobileFilterButton(
                onClick = onOpenFilters,
                active = selectedStatus != UserStatusFilter.All,
                contentDescription = "Filter pengguna",
                testTag = "user-open-filter",
            )
            UserAddIconButton(onClick = onAddUserClick)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            search(Modifier.weight(1f).widthIn(max = 560.dp))
            Spacer(Modifier.weight(1f))
            UserStatusOptions(selectedStatus = selectedStatus, onStatusSelected = onStatusSelected)
            OutlinedButton(
                onClick = onAddUserClick,
                modifier = Modifier.height(48.dp).testTag("add-user"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TbGreenDark),
                border = BorderStroke(1.dp, TbOutline),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("Tambah", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UserAddIconButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp).testTag("add-user"),
        color = Color.White,
        contentColor = TbGreenDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TbOutline.copy(alpha = 0.8f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PersonAdd, contentDescription = "Tambah pengguna", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun UserStatusOptions(
    selectedStatus: UserStatusFilter,
    onStatusSelected: (UserStatusFilter) -> Unit,
    fillWidth: Boolean = false,
) {
    Row(
        modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserStatusFilter.entries.forEach { option ->
            FilterChip(
                selected = selectedStatus == option,
                onClick = { onStatusSelected(option) },
                label = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(option.label, maxLines = 1)
                    }
                },
                modifier = Modifier
                    .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    selectedContainerColor = TbGreenLight,
                    labelColor = TbTextMuted,
                    selectedLabelColor = TbGreenDark,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedStatus == option,
                    borderColor = TbOutline,
                    selectedBorderColor = TbGreenDark.copy(alpha = 0.25f),
                ),
            )
        }
    }
}

@Composable
private fun UserListSection(
    staffMembers: List<StaffMember>,
    filteredResultCount: Int,
    hasActiveFilter: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    isMutating: Boolean,
    compact: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenSummary: () -> Unit,
    onRetry: () -> Unit,
    onEditUserClick: (StaffMember) -> Unit,
    onChangePasswordClick: (StaffMember) -> Unit,
    onChangePinClick: (StaffMember) -> Unit,
    onDeactivateUserClick: (StaffMember) -> Unit,
    onActivateUserClick: (StaffMember) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Daftar pengguna",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = TbText,
                fontWeight = FontWeight.SemiBold,
            )
            TbMobileSummaryButton(
                onClick = onOpenSummary,
                label = "Rincian",
                testTag = "user-open-summary",
            )
        }

        when {
            isLoading -> Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, TbOutline),
            ) { SkeletonList(itemCount = if (compact) 4 else 6) }

            errorMessage != null -> UserFeedback(message = errorMessage, actionLabel = "Muat ulang", onAction = onRetry)
            staffMembers.isEmpty() -> UserFeedback(
                message = if (hasActiveFilter) "Tidak ada pengguna yang cocok." else "Belum ada pengguna.",
            )
            compact -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                staffMembers.forEach { staff ->
                    StaffCard(
                        staff = staff,
                        actionsEnabled = !isMutating,
                        onEditUserClick = onEditUserClick,
                        onChangePasswordClick = onChangePasswordClick,
                        onChangePinClick = onChangePinClick,
                        onDeactivateUserClick = onDeactivateUserClick,
                        onActivateUserClick = onActivateUserClick,
                    )
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                staffMembers.chunked(2).forEach { rowUsers ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowUsers.forEach { staff ->
                            StaffCard(
                                staff = staff,
                                actionsEnabled = !isMutating,
                                modifier = Modifier.weight(1f),
                                onEditUserClick = onEditUserClick,
                                onChangePasswordClick = onChangePasswordClick,
                                onChangePinClick = onChangePinClick,
                                onDeactivateUserClick = onDeactivateUserClick,
                                onActivateUserClick = onActivateUserClick,
                            )
                        }
                        if (rowUsers.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (!isLoading && errorMessage == null && filteredResultCount > 0) {
            val firstItem = ((currentPage - 1) * USER_PAGE_SIZE) + 1
            val lastItem = minOf(currentPage * USER_PAGE_SIZE, filteredResultCount)
            TbPagination(
                currentPage = currentPage,
                totalPages = totalPages,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                supportingText = "$firstItem-$lastItem dari $filteredResultCount data",
                isLoading = isLoading,
                testTag = "user-pagination",
            )
        }
    }
}

@Composable
private fun StaffCard(
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
    Surface(
        modifier = modifier.fillMaxWidth().testTag("user-${staff.id}"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TbOutline.copy(alpha = 0.8f)),
        shadowElevation = if (inactive) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(staff = staff, inactive = inactive)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = staff.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (inactive) TbTextMuted else TbText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@${staff.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TbTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (inactive) {
                        Text(
                            text = "Akun nonaktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = TbError,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                StaffRoleBadge(role = staff.role, inactive = inactive)
                Spacer(Modifier.width(6.dp))
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

            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = TbOutline.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                UserDetailRow(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    value = staff.email,
                    modifier = Modifier.weight(1f),
                )
                UserDetailRow(
                    icon = Icons.Outlined.Update,
                    label = "Login terakhir",
                    value = staff.lastLogin,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UserDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 1.dp).size(16.dp), tint = TbTextMuted.copy(alpha = 0.8f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TbTextMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = TbText,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UserAvatar(staff: StaffMember, inactive: Boolean) {
    Surface(
        modifier = Modifier.size(44.dp),
        color = if (inactive) TbSurfaceMuted else TbGreenLight,
        contentColor = if (inactive) TbTextMuted else TbGreenDark,
        shape = androidx.compose.foundation.shape.CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(staff.initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

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
        IconButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Aksi pengguna", tint = TbTextMuted, modifier = Modifier.size(20.dp))
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
                UserMenuItem("Aktifkan kembali", Icons.Outlined.CheckCircle, TbGreenDark) {
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
                UserMenuItem("Nonaktifkan", Icons.Outlined.Block, TbError) {
                    expanded = false
                    onDeactivateUserClick(staff)
                }
            }
        }
    }
}

@Composable
private fun UserMenuItem(label: String, icon: ImageVector, color: Color = TbText, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = color, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) },
        contentPadding = PaddingValues(horizontal = 14.dp),
    )
}

@Composable
private fun StaffRoleBadge(role: StaffRole, inactive: Boolean) {
    val (label, colors) = when (role) {
        StaffRole.Owner -> "Owner" to (TbAmberLight to TbAmber)
        StaffRole.Admin -> "Admin" to (TbGreenLight to TbGreenDark)
        StaffRole.Kasir -> "Kasir" to (MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer)
        StaffRole.Other -> "Lainnya" to (TbSurfaceMuted to TbTextMuted)
    }
    Badge(label, if (inactive) TbSurfaceMuted else colors.first, if (inactive) TbTextMuted else colors.second)
}

@Composable
private fun Badge(label: String, background: Color, contentColor: Color) {
    Surface(color = background, contentColor = contentColor, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun UserFeedback(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Group, contentDescription = null, tint = TbTextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TbTextMuted)
            if (actionLabel != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = TbGreenDark) }
            }
        }
    }
}

@Composable
private fun UserActionBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val background = if (isError) MaterialTheme.colorScheme.errorContainer else TbGreenLight
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else TbGreenDark
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
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

@Composable
fun StaffDeactivateDialog(
    staff: StaffMember,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    com.tbterminal.app.ui.components.AppConfirmDialog(
        spec = com.tbterminal.app.ui.components.AppConfirmationSpec(
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
