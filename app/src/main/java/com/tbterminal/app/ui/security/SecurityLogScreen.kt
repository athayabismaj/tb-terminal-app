package com.tbterminal.app.ui.security

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.SecurityLogRepository
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

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
    onBackToPrevious: (() -> Unit)? = null,
    onLogout: () -> Unit,
    securityLogRepository: SecurityLogRepository,
    viewModel: SecurityLogViewModel = viewModel(
        factory = SecurityLogViewModel.factory(securityLogRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        onProductsClick = {},

        userName = name,
        role = role,
        activeDestination = AdminDestination.SecurityLog,
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
        onLogout = onLogout,
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.logs.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            SecurityLogContent(
                uiState = uiState,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onDateFilterChange = viewModel::updateDateFilter,
                onActivityFilterChange = viewModel::updateActivityFilter,
                onRefresh = viewModel::refresh,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onPageClick = viewModel::goToPage,
                onViewLogDetail = viewModel::showLogDetail,
                onDismissLogDetail = viewModel::dismissLogDetail,
                onBack = onBackToPrevious,
            )
        }
    }
}

@Composable
internal fun SecurityLogContent(
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
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showMobileFilters by remember { mutableStateOf(false) }
    var showMobileOverview by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        val compact = maxWidth < 700.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (compact) 16.dp else 32.dp,
                end = if (compact) 16.dp else 32.dp,
                top = if (compact) 4.dp else 12.dp,
                bottom = if (compact) 20.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            item { SecurityPageHeader(onBack = onBack) }
            if (!compact) {
                item {
                    SecuritySummaryStrip(
                        total = uiState.totalLogs,
                        changes = uiState.updateCount,
                        latest = uiState.latestCompactRelativeTime,
                    )
                }
            }
            item {
                SecurityFilterToolbar(
                    searchQuery = uiState.searchQuery,
                    dateFilter = uiState.dateFilter,
                    activityFilter = uiState.activityFilter,
                    onSearchQueryChange = onSearchQueryChange,
                    onDateFilterChange = onDateFilterChange,
                    onActivityFilterChange = onActivityFilterChange,
                    compact = compact,
                    onOpenMobileFilters = { showMobileFilters = true },
                )
            }

            when {
                uiState.isLoading && uiState.logs.isEmpty() -> item { SecurityLogSkeleton() }
                uiState.errorMessage != null && uiState.logs.isEmpty() -> {
                    item { SecurityFeedback(uiState.errorMessage, "Coba lagi", onRefresh) }
                }
                uiState.visibleLogs.isEmpty() -> item { SecurityFeedback("Log keamanan tidak ditemukan.") }
                else -> {
                    item {
                        SecuritySectionHeader(
                            resultText = if (uiState.totalLogs > 0) "${uiState.totalLogs} log" else "Halaman ${uiState.page}",
                            compact = compact,
                            onOpenOverview = { showMobileOverview = true },
                        )
                    }
                    items(uiState.visibleLogs, key = SecurityLogItem::id) { log ->
                        SecurityLogCard(
                            log = log,
                            compact = compact,
                            onViewDetail = { onViewLogDetail(log) },
                        )
                    }
                    item {
                        SecurityPagination(
                            page = uiState.page,
                            totalPages = uiState.totalPages,
                            onPrevious = onPreviousPage,
                            onNext = onNextPage,
                            onPageClick = onPageClick,
                            compact = compact,
                        )
                    }
                }
            }
        }
    }

    if (showMobileFilters) {
        TbMobileControlSheet(
            title = "Filter log keamanan",
            subtitle = "Atur periode dan jenis aktivitas",
            onDismiss = { showMobileFilters = false },
            testTag = "security-filter-sheet",
        ) {
            SecurityDateFilterButton(
                selected = uiState.dateFilter,
                onSelected = onDateFilterChange,
                modifier = Modifier.fillMaxWidth(),
            )
            SecurityActivityFilterButton(
                selected = uiState.activityFilter,
                onSelected = onActivityFilterChange,
                modifier = Modifier.fillMaxWidth(),
            )
            TbMobileSheetDoneButton(
                onClick = { showMobileFilters = false },
                testTag = "security-filter-done",
            )
        }
    }

    if (showMobileOverview) {
        TbMobileControlSheet(
            title = "Ringkasan keamanan",
            subtitle = "Ringkasan aktivitas yang tersimpan",
            onDismiss = { showMobileOverview = false },
            testTag = "security-overview-sheet",
        ) {
            SecuritySummaryStrip(
                total = uiState.totalLogs,
                changes = uiState.updateCount,
                latest = uiState.latestCompactRelativeTime,
            )
            TbMobileSheetDoneButton(
                onClick = { showMobileOverview = false },
                label = "Tutup",
                testTag = "security-overview-done",
            )
        }
    }

    uiState.selectedLog?.let { log ->
        SecurityLogDetailDialog(log = log, onDismiss = onDismissLogDetail)
    }
}

@Composable
private fun SecurityPageHeader(onBack: (() -> Unit)?) {
    com.tbterminal.app.ui.components.TBTopAppBar(
        title = "Log keamanan",
        onBackClick = onBack,
    )
}

@Composable
private fun SecuritySummaryStrip(total: Long, changes: Int, latest: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecuritySummaryItem("Total log", total.toString(), Modifier.weight(1f))
            VerticalDivider(Modifier.height(38.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SecuritySummaryItem("Perubahan", changes.toString(), Modifier.weight(1f))
            VerticalDivider(Modifier.height(38.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SecuritySummaryItem("Diperbarui", latest, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SecuritySummaryItem(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun SecurityFilterToolbar(
    searchQuery: String,
    dateFilter: SecurityDateFilter,
    activityFilter: SecurityActivityFilter,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterChange: (SecurityDateFilter) -> Unit,
    onActivityFilterChange: (SecurityActivityFilter) -> Unit,
    compact: Boolean,
    onOpenMobileFilters: () -> Unit,
) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecuritySearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
            )
            TbMobileFilterButton(
                onClick = onOpenMobileFilters,
                active = dateFilter != SecurityDateFilter.All || activityFilter != SecurityActivityFilter.All,
                testTag = "security-open-filters",
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecuritySearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
            )
            SecurityDateFilterButton(
                selected = dateFilter,
                onSelected = onDateFilterChange,
                modifier = Modifier.width(200.dp),
            )
            SecurityActivityFilterButton(
                selected = activityFilter,
                onSelected = onActivityFilterChange,
                modifier = Modifier.width(210.dp),
            )
        }
    }
}

@Composable
private fun SecuritySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(52.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "Cari aktivitas atau pengguna",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun SecurityDateFilterButton(
    selected: SecurityDateFilter,
    onSelected: (SecurityDateFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecurityFilterMenu(
        selectedLabel = selected.compactLabel(),
        accessibilityLabel = "Pilih tanggal: ${selected.label}",
        leadingIcon = Icons.Outlined.CalendarToday,
        options = SecurityDateFilter.entries,
        optionLabel = SecurityDateFilter::label,
        onSelected = onSelected,
        modifier = modifier,
    )
}

@Composable
private fun SecurityActivityFilterButton(
    selected: SecurityActivityFilter,
    onSelected: (SecurityActivityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecurityFilterMenu(
        selectedLabel = selected.compactLabel(),
        accessibilityLabel = "Pilih aktivitas: ${selected.label}",
        leadingIcon = Icons.Outlined.FilterList,
        options = SecurityActivityFilter.entries,
        optionLabel = SecurityActivityFilter::label,
        onSelected = onSelected,
        modifier = modifier,
    )
}

@Composable
private fun <T> SecurityFilterMenu(
    selectedLabel: String,
    accessibilityLabel: String,
    leadingIcon: ImageVector,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = accessibilityLabel,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

private fun SecurityDateFilter.compactLabel(): String = when (this) {
    SecurityDateFilter.All -> "Tanggal"
    SecurityDateFilter.Today -> "Hari ini"
    SecurityDateFilter.Last7Days -> "7 hari"
    SecurityDateFilter.Last30Days -> "30 hari"
}

private fun SecurityActivityFilter.compactLabel(): String = when (this) {
    SecurityActivityFilter.All -> "Semua"
    SecurityActivityFilter.Insert -> "Tambah"
    SecurityActivityFilter.Update -> "Ubah"
    SecurityActivityFilter.Delete -> "Nonaktif"
}

@Composable
private fun SecuritySectionHeader(
    resultText: String,
    compact: Boolean,
    onOpenOverview: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Aktivitas keamanan",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (compact) {
            TbMobileSummaryButton(
                onClick = onOpenOverview,
                testTag = "security-open-overview",
            )
        } else {
            Text(
                text = resultText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SecurityLogCard(log: SecurityLogItem, compact: Boolean, onViewDetail: () -> Unit) {
    val accent = log.type.accentColor()
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewDetail),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(42.dp).background(accent.copy(alpha = 0.10f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = log.activityLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SecurityTypeBadge(log.type, accent)
                        Text(
                            text = "${log.dateLabel}, ${log.time}",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${log.userName} · ${log.userRole.toRoleLabel()} · ${log.relativeTime}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(log.deviceIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(
                    text = "${log.ipAddress} · ${log.deviceName}",
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = onViewDetail, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Detail")
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SecurityTypeBadge(type: SecurityLogType, accent: Color) {
    Surface(color = accent.copy(alpha = 0.10f), shape = RoundedCornerShape(999.dp)) {
        Text(
            text = type.shortLabel(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SecurityLogType.accentColor(): Color = when (this) {
    SecurityLogType.Insert -> MaterialTheme.colorScheme.primary
    SecurityLogType.Update -> MaterialTheme.colorScheme.tertiary
    SecurityLogType.Delete -> MaterialTheme.colorScheme.error
}

private fun SecurityLogType.shortLabel(): String = when (this) {
    SecurityLogType.Insert -> "Tambah"
    SecurityLogType.Update -> "Ubah"
    SecurityLogType.Delete -> "Nonaktif"
}

private fun String.toRoleLabel(): String = lowercase().replaceFirstChar(Char::titlecase)

@Composable
private fun SecurityLogSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("security-log-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBox(Modifier.fillMaxWidth(0.34f).height(18.dp))
        repeat(5) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        SkeletonBox(Modifier.size(42.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.64f).height(15.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SkeletonBox(Modifier.width(58.dp).height(24.dp))
                                SkeletonBox(Modifier.width(112.dp).height(12.dp))
                            }
                            SkeletonBox(Modifier.fillMaxWidth(0.46f).height(12.dp))
                        }
                    }
                    SkeletonBox(Modifier.fillMaxWidth(0.58f).height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SecurityFeedback(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SecurityPagination(
    page: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onPageClick: (Int) -> Unit,
    compact: Boolean,
) {
    TbPagination(
        currentPage = page,
        totalPages = totalPages,
        onPreviousPage = onPrevious,
        onNextPage = onNext,
        supportingText = if (compact) null else "Halaman log keamanan",
        testTag = "security-log-pagination",
    )
}

@Composable
private fun SecurityLogDetailDialog(log: SecurityLogItem, onDismiss: () -> Unit) {
    val accent = log.type.accentColor()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = accent,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "Detail aktivitas",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Log keamanan",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = log.activityLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SecurityTypeBadge(log.type, accent)
                        Text(
                            text = "${log.dateLabel}, ${log.time}",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SecurityDetailRow("Pengguna", log.userName)
                        SecurityDetailRow("Peran", log.userRole.toRoleLabel())
                        SecurityDetailRow("Perangkat", log.deviceName)
                        SecurityDetailRow("Alamat IP", log.ipAddress)
                        SecurityDetailRow("Terjadi", log.relativeTime)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(88.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
