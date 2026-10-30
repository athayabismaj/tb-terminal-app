package com.tbterminal.app.ui.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.SecurityLogRepository
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TBTopAppBar
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

// ── COLOR PALETTE ACCORDING TO HTML & RIWAYAT TRANSAKSI ──
private val BrandSage = Color(0xFF256B57)
private val BrandSageDark = Color(0xFF1E5847)
private val BrandSageLight = Color(0xFFE8F2EE)
private val BrandSage50 = Color(0xFFF4F8F6)

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
private val BackgroundSurface = Color(0xFFF8F9FA)

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
        pageTitle = "Log Keamanan",
        onBack = onBackToPrevious,
        showPageHeader = true,
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
        Box(modifier = contentModifier.fillMaxSize()) {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.logs.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                SecurityLogContent(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onDateFilterChange = { viewModel.updateDateFilter(it, null) },
                    onCustomDateFilterChange = viewModel::updateDateFilter,
                    onDateRangeFilterChange = viewModel::setDateRange,
                    onApplyFilters = viewModel::applyFilters,
                    onActivityFilterChange = viewModel::updateActivityFilter,
                    onRefresh = viewModel::refresh,
                    onPreviousPage = viewModel::previousPage,
                    onNextPage = viewModel::nextPage,
                    onPageClick = viewModel::goToPage,
                    onViewLogDetail = viewModel::showLogDetail,
                    onDismissLogDetail = viewModel::dismissLogDetail,
                )
            }
            if (uiState.logs.isNotEmpty()) {
                TopFloatingCapsule(
                    message = null,
                    error = uiState.errorMessage,
                    onDismiss = viewModel::clearError,
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
                )
            }
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
    onCustomDateFilterChange: ((SecurityDateFilter, String?) -> Unit)? = null,
    onDateRangeFilterChange: ((startDate: String?, endDate: String?, preset: String?) -> Unit)? = null,
    onApplyFilters: ((startDate: String?, endDate: String?, preset: String?, activityFilter: SecurityActivityFilter) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showMobileFilters by remember { mutableStateOf(false) }
    var showMobileOverview by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(BackgroundSurface),
    ) {
        val compact = maxWidth < 700.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (compact) 16.dp else 32.dp,
                end = if (compact) 16.dp else 32.dp,
                top = if (compact) 8.dp else 16.dp,
                bottom = if (compact) 20.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            item {
                SecurityFilterToolbar(
                    searchQuery = uiState.searchQuery,
                    dateFilter = uiState.dateFilter,
                    selectedPreset = uiState.selectedPreset,
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    customDate = uiState.customDate,
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
                            onOpenOverview = { showMobileOverview = true },
                        )
                    }

                    // Group logs by day ("bedakan per hari seperti halaman riwayat transaksi untuk datanya")
                    val groupedLogs = uiState.visibleLogs.groupBy { it.dateLabel }
                    groupedLogs.forEach { (date, logsInGroup) ->
                        item(key = "header-$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = date.toDateGroupHeader(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                )
                                Text(
                                    text = "${logsInGroup.size} aktivitas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate400,
                                )
                            }
                        }
                        item(key = "card-$date") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("security-log-list"),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                shadowElevation = 0.5.dp,
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    logsInGroup.forEachIndexed { index, log ->
                                        SecurityLogCardItem(
                                            log = log,
                                            compact = compact,
                                            onViewDetail = { onViewLogDetail(log) },
                                        )
                                        if (index < logsInGroup.lastIndex) {
                                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SecurityPagination(
                            uiState = uiState,
                            onPrevious = onPreviousPage,
                            onNext = onNextPage,
                        )
                    }
                }
            }
        }
    }

    if (showMobileFilters) {
        SecurityFilterSheet(
            currentDateFilter = uiState.dateFilter,
            currentStartDate = uiState.startDate,
            currentEndDate = uiState.endDate,
            currentPreset = uiState.selectedPreset,
            currentCustomDate = uiState.customDate,
            currentActivityFilter = uiState.activityFilter,
            onApplyFilter = { date, act, start, end, preset ->
                if (onApplyFilters != null) {
                    onApplyFilters(start, end, preset, act)
                } else {
                if (onCustomDateFilterChange != null) {
                    if (onDateRangeFilterChange != null) {
                        onDateRangeFilterChange(start, end, preset)
                    } else {
                        onCustomDateFilterChange(date, start)
                    }
                } else {
                    onDateFilterChange(date)
                }
                onActivityFilterChange(act)
                }
                showMobileFilters = false
            },
            onDismiss = { showMobileFilters = false },
        )
    }

    if (showMobileOverview) {
        SecurityOverviewSheet(
            uiState = uiState,
            onDismiss = { showMobileOverview = false },
        )
    }

    uiState.selectedLog?.let { log ->
        SecurityLogDetailDialog(log = log, onDismiss = onDismissLogDetail)
    }
}



@Composable
private fun SecuritySectionHeader(
    onOpenOverview: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Aktivitas keamanan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { onOpenOverview() }
                .testTag("security-open-overview"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                contentDescription = "Rincian",
                tint = Color(0xFF334155),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SecurityFilterToolbar(
    searchQuery: String,
    dateFilter: SecurityDateFilter,
    selectedPreset: String? = "Hari",
    startDate: String? = null,
    endDate: String? = null,
    customDate: String? = null,
    activityFilter: SecurityActivityFilter,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterChange: (SecurityDateFilter) -> Unit,
    onActivityFilterChange: (SecurityActivityFilter) -> Unit,
    compact: Boolean,
    onOpenMobileFilters: () -> Unit,
) {
    val todayStr = LocalDate.now(WIB_ZONE).toString()
    val hasActiveFilter = (dateFilter != SecurityDateFilter.Today && selectedPreset != "Hari") ||
        activityFilter != SecurityActivityFilter.All ||
        (startDate != null && startDate != todayStr) ||
        (endDate != null && endDate != todayStr)

    Row(
        modifier = Modifier.fillMaxWidth().testTag("security-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Search bar (42.dp height, RoundedCornerShape(12.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(if (compact) 42.dp else 48.dp)
                .testTag("security-search"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate300),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = Slate900,
                        fontWeight = FontWeight.Normal,
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari aktivitas atau pengguna",
                                    fontSize = 13.sp,
                                    color = Slate400,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus pencarian",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Filter button (42.dp square in compact)
        if (compact) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .testTag("security-open-filters")
                    .clickable { onOpenMobileFilters() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (hasActiveFilter) Color(0xFF0F172A) else Slate300),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Buka Filter",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp),
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
        } else {
            SecurityDateFilterButton(
                selected = dateFilter,
                onSelected = onDateFilterChange,
                modifier = Modifier.width(180.dp),
            )
            SecurityActivityFilterButton(
                selected = activityFilter,
                onSelected = onActivityFilterChange,
                modifier = Modifier.width(180.dp),
            )
        }
    }
}

@Composable
private fun SecurityLogCardItem(
    log: SecurityLogItem,
    compact: Boolean,
    onViewDetail: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetail)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Icon Box: 40.dp rounded-xl, bg-sage-100, text-sage-600
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandSageLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = BrandSage,
                modifier = Modifier.size(20.dp),
            )
        }

        // Content area
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Top Row: Activity Title, Type Badge, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = log.activityLabel,
                    color = Slate800,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    color = BrandSageLight,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = log.type.shortLabel(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = BrandSageDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${log.dateLabel}, ${log.time}",
                    color = Slate400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }

            // Subtitle: User · Role · Relative Time
            Text(
                text = "${log.userName} · ${log.userRole.toRoleLabel()} · ${log.relativeTime}",
                color = Slate500,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Divider & Device / Detail Row
            HorizontalDivider(color = Slate100, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                ) {
                    Icon(
                        imageVector = log.deviceIcon,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${log.ipAddress} · ${log.deviceName}",
                        color = Slate400,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Detail",
                        color = BrandSage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = BrandSage,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityPagination(
    uiState: SecurityLogUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val start = if (uiState.totalLogs == 0L) 0 else (uiState.page - 1L) * uiState.limit + 1
    val end = minOf(uiState.page.toLong() * uiState.limit, uiState.totalLogs)
    val safePage = uiState.page.coerceAtLeast(1)
    val safeTotalPages = uiState.totalPages.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("security-log-pagination"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Range Pill
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
        ) {
            Text(
                text = "$start–$end dari ${uiState.totalLogs} data",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
            )
        }

        // Navigation Pill
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val canPrev = safePage > 1 && !uiState.isLoading
                val canNext = safePage < safeTotalPages && !uiState.isLoading

                IconButton(
                    onClick = onPrevious,
                    enabled = canPrev,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Halaman sebelumnya",
                        modifier = Modifier.size(16.dp),
                        tint = if (canPrev) Slate700 else Slate300,
                    )
                }

                Text(
                    text = "$safePage / $safeTotalPages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )

                IconButton(
                    onClick = onNext,
                    enabled = canNext,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Halaman berikutnya",
                        modifier = Modifier.size(16.dp),
                        tint = if (canNext) Slate700 else Slate300,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityOverviewSheet(
    uiState: SecurityLogUiState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .testTag("security-overview-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sheet Header with Title, Description, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Ringkasan Log Keamanan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 24.sp,
                    )
                    Text(
                        text = "Rincian data dan aktivitas keamanan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B),
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Ringkasan",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Metrics List: divide-y divide-slate-100 without cards (like CustomerSummarySheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security-summary-grid"),
            ) {
                SecuritySummaryRow(
                    label = "Total Log",
                    value = uiState.totalLogs.toString(),
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SecuritySummaryRow(
                    label = "Perubahan Data",
                    value = uiState.updateCount.toString(),
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SecuritySummaryRow(
                    label = "Aktivitas Terakhir",
                    value = uiState.latestRelativeTime,
                )
            }

            // Primary Action: Button Selesai
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("security-overview-done"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandSageDark),
            ) {
                Text(
                    text = "Selesai",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SecuritySummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityFilterSheet(
    currentDateFilter: SecurityDateFilter,
    currentStartDate: String? = null,
    currentEndDate: String? = null,
    currentPreset: String? = null,
    currentCustomDate: String?,
    currentActivityFilter: SecurityActivityFilter,
    onApplyFilter: (SecurityDateFilter, SecurityActivityFilter, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now(WIB_ZONE) }
    var tempStartDate by remember(currentStartDate) { mutableStateOf<String?>(currentStartDate ?: today.toString()) }
    var tempEndDate by remember(currentEndDate) { mutableStateOf<String?>(currentEndDate ?: today.toString()) }
    var tempPreset by remember(currentPreset) { mutableStateOf<String?>(currentPreset ?: "Hari") }
    var tempDateFilter by remember(currentDateFilter) { mutableStateOf(currentDateFilter) }
    var tempCustomDate by remember(currentCustomDate) { mutableStateOf(currentCustomDate) }
    var tempActivityFilter by remember(currentActivityFilter) { mutableStateOf(currentActivityFilter) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("security-filter-sheet"),
        ) {
            // Header matching Riwayat Transaksi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Filter Log Keamanan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur periode dan jenis aktivitas",
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
                        Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Section: PERIODE (Semua, Hari ini, 7 Hari, 30 Hari + Kalender di bagian kanan)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PERIODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Segmented control presets
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val dateOptions = listOf(
                                SecurityDateFilter.All to "Semua",
                                SecurityDateFilter.Today to "Hari",
                                SecurityDateFilter.Last7Days to "Minggu",
                                SecurityDateFilter.Last30Days to "Bulan",
                            )
                            dateOptions.forEach { (option, label) ->
                                val isActive = when (option) {
                                    SecurityDateFilter.All -> tempPreset == "Semua"
                                    SecurityDateFilter.Today -> tempPreset == "Hari"
                                    SecurityDateFilter.Last7Days -> tempPreset == "Minggu"
                                    SecurityDateFilter.Last30Days -> tempPreset == "Bulan"
                                    SecurityDateFilter.Custom -> tempPreset == null
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (isActive) {
                                                Modifier
                                                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            tempDateFilter = option
                                            tempCustomDate = null
                                            when (option) {
                                                SecurityDateFilter.All -> {
                                                    tempPreset = "Semua"
                                                    tempStartDate = null
                                                    tempEndDate = null
                                                }
                                                SecurityDateFilter.Today -> {
                                                    tempPreset = "Hari"
                                                    tempStartDate = today.toString()
                                                    tempEndDate = today.toString()
                                                }
                                                SecurityDateFilter.Last7Days -> {
                                                    tempPreset = "Minggu"
                                                    tempStartDate = today.with(DayOfWeek.MONDAY).toString()
                                                    tempEndDate = today.toString()
                                                }
                                                SecurityDateFilter.Last30Days -> {
                                                    tempPreset = "Bulan"
                                                    tempStartDate = today.withDayOfMonth(1).toString()
                                                    tempEndDate = today.toString()
                                                }
                                                SecurityDateFilter.Custom -> {
                                                    tempPreset = null
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isActive) Color(0xFF0F172A) else Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        // Calendar Date Button on the right (like Riwayat Transaksi)
                        val isCustomDateActive = tempPreset == null || tempDateFilter == SecurityDateFilter.Custom
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCustomDateActive) Color(0xFFE8F2EE) else Color(0xFFF1F5F9))
                                .border(
                                    BorderStroke(1.dp, if (isCustomDateActive) Color(0xFFB8D8CD).copy(alpha = 0.6f) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { showDatePickerDialog = true }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Pilih tanggal",
                                    tint = if (isCustomDateActive) Color(0xFF256B57) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp),
                                )
                                val dateLabel = formatHistoryShortDate(tempStartDate, tempEndDate)
                                if (dateLabel.isNotBlank()) {
                                    Text(
                                        text = dateLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCustomDateActive) Color(0xFF256B57) else Color(0xFF64748B),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: JENIS AKTIVITAS (Menu Dropdown agar rapi dan tidak mengecil)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "JENIS AKTIVITAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )

                    SecurityActivityDropdownField(
                        selectedFilter = tempActivityFilter,
                        onSelected = { tempActivityFilter = it },
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Action Buttons
                val isFiltered = tempPreset != "Hari" ||
                    tempActivityFilter != SecurityActivityFilter.All ||
                    tempStartDate != today.toString() ||
                    tempEndDate != today.toString()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isFiltered) {
                        Button(
                            onClick = {
                                tempDateFilter = SecurityDateFilter.Today
                                tempActivityFilter = SecurityActivityFilter.All
                                tempCustomDate = null
                                tempStartDate = today.toString()
                                tempEndDate = today.toString()
                                tempPreset = "Hari"
                                onApplyFilter(
                                    SecurityDateFilter.Today,
                                    SecurityActivityFilter.All,
                                    today.toString(),
                                    today.toString(),
                                    "Hari"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF475569),
                            ),
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onApplyFilter(tempDateFilter, tempActivityFilter, tempStartDate, tempEndDate, tempPreset)
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .testTag("security-filter-done"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandSageDark),
                    ) {
                        Text(
                            text = "Terapkan Filter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        HistoryDatePickerDialog(
            currentDate = tempStartDate ?: tempEndDate ?: today.toString(),
            initialMode = when (tempPreset) {
                "Bulan" -> "Bulan"
                "Minggu" -> "Minggu"
                else -> "Hari"
            },
            onDismiss = { showDatePickerDialog = false },
            onConfirmRange = { newStartStr, newEndStr, mode ->
                tempStartDate = newStartStr
                tempEndDate = newEndStr
                tempPreset = when (mode) {
                    "Bulan" -> "Bulan"
                    "Minggu" -> "Minggu"
                    else -> null
                }
                tempDateFilter = when (mode) {
                    "Bulan" -> SecurityDateFilter.Last30Days
                    "Minggu" -> SecurityDateFilter.Last7Days
                    else -> SecurityDateFilter.Custom
                }
                tempCustomDate = if (tempPreset == null) newStartStr else null
                showDatePickerDialog = false
            },
            onConfirm = { selectedDate ->
                tempStartDate = selectedDate
                tempEndDate = selectedDate
                val isToday = selectedDate == today.toString()
                tempPreset = if (isToday) "Hari" else null
                tempCustomDate = if (tempPreset == null) selectedDate else null
                tempDateFilter = if (isToday) SecurityDateFilter.Today else SecurityDateFilter.Custom
                showDatePickerDialog = false
            },
        )
    }
}

@Composable
private fun SecurityActivityDropdownField(
    selectedFilter: SecurityActivityFilter,
    onSelected: (SecurityActivityFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val activityOptions = listOf(
        SecurityActivityFilter.All to "Semua aktivitas",
        SecurityActivityFilter.Insert to "Tambah data (INSERT)",
        SecurityActivityFilter.Update to "Ubah data (UPDATE)",
        SecurityActivityFilter.Delete to "Nonaktifkan data (DELETE)",
    )

    val displayLabel = activityOptions.firstOrNull { it.first == selectedFilter }?.second
        ?: selectedFilter.label

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAF9))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = "Pilih jenis aktivitas",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(12.dp)),
        ) {
            activityOptions.forEach { (option, label) ->
                val isSelected = selectedFilter == option
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF1E293B),
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF256B57),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else null,
                )
            }
        }
    }
}

private fun String.toDateGroupHeader(): String {
    if (this == "-" || this.isBlank()) return "Lainnya"
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        val today = LocalDate.now().format(formatter)
        val yesterday = LocalDate.now().minusDays(1).format(formatter)
        when (this) {
            today -> "Hari Ini • $this"
            yesterday -> "Kemarin • $this"
            else -> this
        }
    } catch (_: Exception) {
        this
    }
}

@Composable
private fun SecurityLogSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("security-log-skeleton"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBox(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SkeletonBox(Modifier.size(width = 120.dp, height = 15.dp))
                            SkeletonBox(Modifier.size(width = 90.dp, height = 12.dp))
                        }
                        SkeletonBox(Modifier.size(width = 160.dp, height = 12.dp))
                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SkeletonBox(Modifier.size(width = 100.dp, height = 12.dp))
                            SkeletonBox(Modifier.size(width = 40.dp, height = 12.dp))
                        }
                    }
                }
                if (index < 3) {
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun SecurityFeedback(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(BrandSageLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = BrandSage,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "Log keamanan",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate900,
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = Slate500,
            )
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSage),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(actionLabel)
                }
            }
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
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Slate300),
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
                    tint = BrandSage,
                )
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    color = Slate800,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = Slate400,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White,
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
    SecurityDateFilter.All -> "Semua"
    SecurityDateFilter.Today -> "Hari"
    SecurityDateFilter.Last7Days -> "Minggu"
    SecurityDateFilter.Last30Days -> "Bulan"
    SecurityDateFilter.Custom -> "Kustom"
}

private fun formatHistoryShortDate(startDate: String?, endDate: String?): String {
    val start = startDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val end = endDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    if (start != null && end != null) {
        return if (start == end) {
            start.format(DateTimeFormatter.ofPattern("dd/MM"))
        } else {
            "${start.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${end.format(DateTimeFormatter.ofPattern("dd/MM"))}"
        }
    }
    val single = end ?: start
    if (single != null) {
        return single.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
    return ""
}

private fun String.toShortDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

private fun SecurityActivityFilter.compactLabel(): String = when (this) {
    SecurityActivityFilter.All -> "Semua"
    SecurityActivityFilter.Insert -> "Tambah"
    SecurityActivityFilter.Update -> "Ubah"
    SecurityActivityFilter.Delete -> "Nonaktif"
}

private fun SecurityLogType.shortLabel(): String = when (this) {
    SecurityLogType.Insert -> "Tambah"
    SecurityLogType.Update -> "Ubah"
    SecurityLogType.Delete -> "Nonaktif"
}

private fun String.toRoleLabel(): String = lowercase().replaceFirstChar(Char::titlecase)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityLogDetailDialog(log: SecurityLogItem, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Slate300, RoundedCornerShape(999.dp)),
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(BrandSageLight, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = BrandSage,
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Detail aktivitas",
                            color = Slate900,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp,
                        )
                        Text(
                            text = "Log keamanan",
                            color = Slate400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup modal",
                        modifier = Modifier.size(20.dp),
                        tint = Slate400,
                    )
                }
            }

            // Aktivitas Title & Badge / Tanggal
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = log.activityLabel,
                    color = Slate900,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = BrandSageLight,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = log.type.shortLabel(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            color = BrandSageDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "${log.dateLabel}, ${log.time}",
                        color = Slate500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Garis Pemisah Halus
            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Key-Value Metadata List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecurityDetailRow(
                    label = "Pengguna",
                    value = log.userName,
                    valueColor = Slate900,
                    valueFontWeight = FontWeight.SemiBold,
                )
                SecurityDetailRow(
                    label = "Peran",
                    value = log.userRole.toRoleLabel(),
                    valueColor = Slate800,
                    valueFontWeight = FontWeight.Medium,
                )
                SecurityDetailRow(
                    label = "Perangkat",
                    value = log.deviceName,
                    valueColor = Slate800,
                    valueFontWeight = FontWeight.Medium,
                )
                SecurityDetailRow(
                    label = "Alamat IP",
                    value = log.ipAddress,
                    valueColor = if (log.ipAddress == "-") Slate500 else Slate800,
                    valueFontWeight = FontWeight.Medium,
                )
                SecurityDetailRow(
                    label = "Terjadi",
                    value = log.relativeTime,
                    valueColor = Slate900,
                    valueFontWeight = FontWeight.SemiBold,
                )
            }

            // iOS Home Indicator Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 128.dp, height = 4.dp)
                        .background(Slate300, RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@Composable
private fun SecurityDetailRow(
    label: String,
    value: String,
    valueColor: Color = Slate800,
    valueFontWeight: FontWeight = FontWeight.Medium,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Slate500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = valueFontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
