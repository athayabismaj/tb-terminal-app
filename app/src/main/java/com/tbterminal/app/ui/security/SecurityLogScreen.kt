package com.tbterminal.app.ui.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.SecurityLogRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardShell
import com.tbterminal.app.ui.dashboard.owner.OwnerDestination
import com.tbterminal.app.ui.components.RefreshableContent

val SecuritySurface = Color(0xFFF8FAFB)
val SecurityOnSurface = Color(0xFF0F172A)
val SecuritySlate50 = Color(0xFFF8FAFC)
val SecuritySlate100 = Color(0xFFF1F5F9)
val SecuritySlate200 = Color(0xFFE2E8F0)
val SecuritySlate400 = Color(0xFF94A3B8)
val SecuritySlate500 = Color(0xFF64748B)
val SecuritySlate600 = Color(0xFF475569)
val SecuritySlate900 = Color(0xFF0F172A)
val SecurityPrimary = Color(0xFF10B981)
val SecurityPrimaryDark = Color(0xFF059669)
val SecurityPrimaryLight = Color(0xFFECFDF5)
val SecurityError = Color(0xFFEF4444)
val SecurityErrorLight = Color(0xFFFFF1F2)
val SecurityInfo = Color(0xFF3B82F6)
val SecurityInfoLight = Color(0xFFEFF6FF)

@Composable
fun OwnerSecurityLogScreen(
    name: String,
    role: String,
    onDashboardClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onReportsClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    securityLogRepository: SecurityLogRepository,
    viewModel: SecurityLogViewModel = viewModel(
        factory = SecurityLogViewModel.factory(securityLogRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OwnerDashboardShell(
        userName = name,
        role = role,
        activeDestination = OwnerDestination.SecurityLog,
        onDashboardClick = onDashboardClick,
        onReportsClick = onReportsClick,
        onSyncCenterClick = onSyncCenterClick,
        onStockReportClick = onStockReportClick,
        onReceivablesClick = onReceivablesClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = {},
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.logs.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            SecurityLogContent(
            modifier = Modifier,
            uiState = uiState,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onDateFilterChange = viewModel::updateDateFilter,
            onActivityFilterChange = viewModel::updateActivityFilter,
            onRefresh = viewModel::refresh,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onPageClick = viewModel::goToPage,
            onViewLogDetail = viewModel::showLogDetail,
            onDismissLogDetail = viewModel::dismissLogDetail
            )
        }
    }
}

@Composable
private fun SecurityLogContent(
    uiState: SecurityLogUiState,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterChange: (SecurityDateFilter) -> Unit,
    onActivityFilterChange: (SecurityActivityFilter) -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageClick: (Int) -> Unit,
    onViewLogDetail: (SecurityLogItem) -> Unit,
    onDismissLogDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SecuritySurface)
            .padding(if (compact) 16.dp else 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 28.dp)
    ) {
        SecurityLogHeader(compact)
        SecurityMetricRow(uiState = uiState, compact = compact)
        SecurityLogToolbar(
            searchQuery = uiState.searchQuery,
            dateFilter = uiState.dateFilter,
            activityFilter = uiState.activityFilter,
            onSearchQueryChange = onSearchQueryChange,
            onDateFilterChange = onDateFilterChange,
            onActivityFilterChange = onActivityFilterChange,
            onRefresh = onRefresh,
            compact = compact
        )
        SecurityLogTable(
            logs = uiState.visibleLogs,
            totalLogs = uiState.totalLogs,
            page = uiState.page,
            limit = uiState.limit,
            totalPages = uiState.totalPages,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onRetry = onRefresh,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onPageClick = onPageClick,
            onViewLogDetail = onViewLogDetail,
            compact = compact
        )
    }

    uiState.selectedLog?.let { log ->
        SecurityLogDetailDialog(log = log, onDismiss = onDismissLogDetail)
    }
}

@Composable
private fun SecurityLogHeader(compact: Boolean) {
    Column {
        Text(
            text = "Log Keamanan",
            color = SecuritySlate900,
            fontSize = if (compact) 24.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (!compact) Spacer(modifier = Modifier.height(8.dp))
        if (!compact) Text(
            text = "Pantau aktivitas akun dan riwayat akses sistem secara real-time.",
            color = SecuritySlate500,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SecurityMetricRow(
    uiState: SecurityLogUiState,
    compact: Boolean
) {
    val cards: @Composable (Modifier, Modifier, Modifier) -> Unit = { firstModifier, secondModifier, thirdModifier ->
        SecurityMetricCard(
            modifier = firstModifier,
            title = "TOTAL AKTIVITAS",
            value = uiState.totalLogs.toString(),
            badgeText = "Database",
            icon = Icons.Outlined.Analytics,
            iconTint = SecurityPrimary,
            iconBackground = SecurityPrimaryLight,
            trendText = "Diambil dari system.audit_logs",
            trendIcon = Icons.AutoMirrored.Outlined.TrendingUp,
            trendColor = SecurityPrimary
        )
        SecurityMetricCard(
            modifier = secondModifier,
            title = "PERUBAHAN DATA",
            value = uiState.updateCount.toString(),
            badgeText = "Update",
            icon = Icons.Outlined.GppMaybe,
            iconTint = SecurityError,
            iconBackground = SecurityErrorLight,
            trendText = "Aktivitas update pada halaman ini",
            trendIcon = Icons.Outlined.Warning,
            trendColor = SecurityError
        )
        SecurityMetricCard(
            modifier = thirdModifier,
            title = "TERAKHIR DIPERBARUI",
            value = uiState.latestCompactRelativeTime,
            badgeText = "Realtime",
            icon = Icons.Outlined.Update,
            iconTint = SecurityInfo,
            iconBackground = SecurityInfoLight,
            trendText = "Sinkronisasi otomatis aktif",
            trendColor = SecuritySlate400
        )
    }
    if (compact) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards(Modifier.fillMaxWidth(), Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
    } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        cards(Modifier.weight(1f), Modifier.weight(1f), Modifier.weight(1f))
    }
}

@Composable
private fun SecurityMetricCard(
    title: String,
    value: String,
    badgeText: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    trendText: String,
    trendIcon: ImageVector? = null,
    trendColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SecuritySlate100)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBackground)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = SecuritySlate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = value,
                        color = SecuritySlate900,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBackground)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = iconTint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trendIcon != null) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = trendText,
                    color = trendColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SecurityLogToolbar(
    searchQuery: String,
    dateFilter: SecurityDateFilter,
    activityFilter: SecurityActivityFilter,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterChange: (SecurityDateFilter) -> Unit,
    onActivityFilterChange: (SecurityActivityFilter) -> Unit,
    onRefresh: () -> Unit,
    compact: Boolean
) {
    val searchField: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(text = "Cari user, IP, atau aktivitas...", color = SecuritySlate400)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = SecuritySlate400
                )
            },
            modifier = fieldModifier.height(56.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = SecuritySlate200,
                focusedBorderColor = SecurityPrimary
            )
        )
    }
    if (compact) Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        searchField(Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecurityDateFilterButton(dateFilter, onDateFilterChange, Modifier.weight(1f))
            SecurityActivityFilterButton(activityFilter, onActivityFilterChange, Modifier.weight(1f))
        }
    } else Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        searchField(Modifier.weight(1f))
        SecurityDateFilterButton(dateFilter, onDateFilterChange)
        SecurityActivityFilterButton(activityFilter, onActivityFilterChange)
    }
}

@Composable
private fun SecurityDateFilterButton(
    selectedFilter: SecurityDateFilter,
    onFilterSelected: (SecurityDateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        SecurityFilterButtonSurface(
            icon = Icons.Outlined.CalendarToday,
            text = selectedFilter.label,
            showDropdown = true,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SecurityDateFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(text = filter.label) },
                    onClick = {
                        expanded = false
                        onFilterSelected(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun SecurityActivityFilterButton(
    selectedFilter: SecurityActivityFilter,
    onFilterSelected: (SecurityActivityFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        SecurityFilterButtonSurface(
            icon = Icons.Outlined.FilterList,
            text = selectedFilter.label,
            showDropdown = true,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SecurityActivityFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(text = filter.label) },
                    onClick = {
                        expanded = false
                        onFilterSelected(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun SecurityFilterButtonSurface(
    icon: ImageVector,
    text: String,
    showDropdown: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SecuritySlate200),
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SecuritySlate400)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, fontSize = 14.sp, color = SecuritySlate900)
            if (showDropdown) {
                Spacer(modifier = Modifier.width(18.dp))
                Icon(imageVector = Icons.Outlined.ExpandMore, contentDescription = null, tint = SecuritySlate400)
            }
        }
    }
}

@Composable
private fun SecurityLogTable(
    logs: List<SecurityLogItem>,
    totalLogs: Long,
    page: Int,
    limit: Int,
    totalPages: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageClick: (Int) -> Unit,
    onViewLogDetail: (SecurityLogItem) -> Unit,
    compact: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SecuritySlate100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (!compact) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecuritySlate50.copy(alpha = 0.45f))
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecurityTableHeaderText(text = "USER", modifier = Modifier.weight(2.4f))
                SecurityTableHeaderText(text = "AKTIVITAS", modifier = Modifier.weight(1.85f))
                SecurityTableHeaderText(text = "ALAMAT IP", modifier = Modifier.weight(1.35f))
                SecurityTableHeaderText(text = "PERANGKAT", modifier = Modifier.weight(1.85f))
                SecurityTableHeaderText(text = "WAKTU", modifier = Modifier.weight(1.35f))
                SecurityTableHeaderText(text = "AKSI", modifier = Modifier.weight(0.6f), alignment = Alignment.End)
            }

            HorizontalDivider(color = SecuritySlate100)

            when {
                isLoading -> com.tbterminal.app.ui.components.SkeletonList(itemCount = 6)
                errorMessage != null -> SecurityTableFeedback(
                    message = errorMessage,
                    actionLabel = "Muat ulang",
                    onAction = onRetry
                )
                logs.isEmpty() -> SecurityTableFeedback(message = "Log tidak ditemukan.")
                else -> {
                    logs.forEachIndexed { index, log ->
                        if (compact) SecurityLogMobileRow(
                            log = log,
                            onViewLogDetail = onViewLogDetail
                        ) else SecurityLogTableRow(
                            log = log,
                            onViewLogDetail = onViewLogDetail
                        )
                        if (index < logs.lastIndex) {
                            HorizontalDivider(color = SecuritySlate50)
                        }
                    }
                }
            }

            SecurityPaginationFooter(
                visibleCount = logs.size,
                totalCount = totalLogs,
                page = page,
                limit = limit,
                totalPages = totalPages,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                onPageClick = onPageClick,
                compact = compact
            )
        }
    }
}

@Composable
private fun SecurityTableFeedback(
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = SecuritySlate500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = SecurityPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SecurityTableHeaderText(
    text: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = text,
            color = SecuritySlate400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecurityLogMobileRow(
    log: SecurityLogItem,
    onViewLogDetail: (SecurityLogItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onViewLogDetail(log) }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SecuritySlate100).border(1.dp, SecuritySlate200, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Person, contentDescription = null, tint = SecuritySlate400) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.userName, color = SecuritySlate900, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${log.userRole} · ${log.relativeTime}", color = SecuritySlate500, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Lihat detail", tint = SecuritySlate400)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(log.type.background).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(log.type.color))
                Spacer(Modifier.width(7.dp))
                Text(log.activityLabel, color = log.type.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(log.time, color = SecuritySlate500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("${log.ipAddress} · ${log.deviceName}", color = SecuritySlate500, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecurityLogTableRow(
    log: SecurityLogItem,
    onViewLogDetail: (SecurityLogItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(2.4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SecuritySlate100)
                    .border(1.dp, SecuritySlate200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = SecuritySlate400)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = log.userName,
                    color = SecuritySlate900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.userRole,
                    color = SecuritySlate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1.85f)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(log.type.background)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(log.type.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.activityLabel,
                    color = log.type.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = log.ipAddress,
            modifier = Modifier.weight(1.35f),
            color = SecuritySlate500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.weight(1.85f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = log.deviceIcon,
                contentDescription = null,
                tint = SecuritySlate500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = log.deviceName,
                color = SecuritySlate500,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(modifier = Modifier.weight(1.35f)) {
            Text(
                text = log.time,
                color = SecuritySlate900,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = log.relativeTime,
                color = if (log.type == SecurityLogType.Insert) SecurityPrimary else SecuritySlate400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.End
        ) {
            var expanded by remember(log.id) { mutableStateOf(false) }

            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Aksi log",
                    tint = SecuritySlate400
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Lihat Detail") },
                    onClick = {
                        expanded = false
                        onViewLogDetail(log)
                    }
                )
            }
        }
    }
}

@Composable
private fun SecurityPaginationFooter(
    visibleCount: Int,
    totalCount: Long,
    page: Int,
    limit: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageClick: (Int) -> Unit,
    compact: Boolean
) {
    val from = if (visibleCount == 0 || totalCount == 0L) {
        0
    } else {
        ((page - 1).toLong() * limit) + 1
    }
    val to = if (visibleCount == 0 || totalCount == 0L) {
        0
    } else {
        (((page - 1).toLong() * limit) + visibleCount).coerceAtMost(totalCount)
    }
    val pages = visiblePageNumbers(page, totalPages)

    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth().background(SecuritySlate50.copy(alpha = 0.5f)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("$from-$to dari $totalCount aktivitas", color = SecuritySlate500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SecurityPaginationButton(icon = Icons.Outlined.ChevronLeft, enabled = page > 1 && totalPages > 0, onClick = onPreviousPage)
                Text("Halaman $page / ${totalPages.coerceAtLeast(1)}", color = SecuritySlate900, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                SecurityPaginationButton(icon = Icons.Outlined.ChevronRight, enabled = page < totalPages, onClick = onNextPage)
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SecuritySlate50.copy(alpha = 0.5f))
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MENAMPILKAN $from-$to DARI $totalCount LOG",
            color = SecuritySlate400,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecurityPaginationButton(
                icon = Icons.Outlined.ChevronLeft,
                enabled = page > 1 && totalPages > 0,
                onClick = onPreviousPage
            )
            pages.forEach { pageNumber ->
                SecurityPaginationButton(
                    text = pageNumber.toString(),
                    selected = pageNumber == page,
                    onClick = { onPageClick(pageNumber) }
                )
            }
            SecurityPaginationButton(
                icon = Icons.Outlined.ChevronRight,
                enabled = page < totalPages,
                onClick = onNextPage
            )
        }
    }
}

@Composable
private fun SecurityPaginationButton(
    text: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val background = when {
        selected -> SecurityPrimary
        !enabled -> SecuritySlate50
        else -> Color.White
    }
    val contentColor = when {
        selected -> Color.White
        !enabled -> SecuritySlate400.copy(alpha = 0.5f)
        else -> SecuritySlate600
    }
    val borderColor = if (selected) Color.Transparent else SecuritySlate200

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(text = text, fontWeight = FontWeight.Bold, color = contentColor)
        }
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        }
    }
}

@Composable
private fun SecurityLogDetailDialog(
    log: SecurityLogItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Detail Log Keamanan",
                color = SecuritySlate900,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecurityDetailRow(label = "User", value = "${log.userName} (${log.userRole})")
                SecurityDetailRow(label = "Aktivitas", value = log.activityLabel)
                SecurityDetailRow(label = "Alamat IP", value = log.ipAddress)
                SecurityDetailRow(label = "Perangkat", value = log.deviceName)
                SecurityDetailRow(label = "Waktu", value = "${log.time} - ${log.relativeTime}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Tutup", color = SecurityPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun SecurityDetailRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label.uppercase(),
            color = SecuritySlate400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = SecuritySlate900,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun visiblePageNumbers(
    currentPage: Int,
    totalPages: Int
): List<Int> {
    if (totalPages <= 0) return emptyList()
    val end = (currentPage + 1).coerceAtMost(totalPages)
    val start = (end - 2).coerceAtLeast(1)
    return (start..end).toList()
}
