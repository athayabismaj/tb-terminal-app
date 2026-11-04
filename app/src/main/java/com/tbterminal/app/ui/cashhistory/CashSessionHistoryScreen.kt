package com.tbterminal.app.ui.cashhistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.SummaryBarChartIcon
import com.tbterminal.app.ui.components.SummaryIconButton
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.ui.components.TbPeriodFilterRow
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Brand50 = Color(0xFFF0F6F3)
private val Brand100 = Color(0xFFDCEEE7)
private val Brand600 = Color(0xFF256B57)
private val Brand700 = Color(0xFF1D5545)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val ErrorRed = Color(0xFFDC2626)
private val ErrorBg = Color(0xFFFEF2F2)

private val SessionBackground = TbBackground
private val SessionSurface = TbSurface
private val SessionSoft = TbSurfaceMuted
private val SessionBorder = TbOutline
private val SessionText = TbText
private val SessionMuted = TbTextMuted
private val SessionPrimary = TbGreen
private val SessionPrimaryDark = TbGreenDark
private val SessionDanger = TbError

@Composable
internal fun CashSessionHistoryScreen(
    modifier: Modifier,
    uiState: CashSessionHistoryUiState,
    isSearchExpanded: Boolean = false,
    showFilterPopup: Boolean = false,
    showSummaryPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    onDismissSummary: () -> Unit = {},
    onOpenFilter: () -> Unit = {},
    onOpenSummary: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onDateChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDateRangeChanged: (String, String, String?) -> Unit = { _, _, _ -> },
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showMobileFilters by remember { mutableStateOf(false) }
    var showMobileOverview by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(SessionBackground)) {
        val compact = maxWidth < 720.dp
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1180.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (compact) 16.dp else 28.dp,
                        vertical = if (compact) 14.dp else 20.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                if (uiState.isLoading && uiState.sessions.isEmpty() && uiState.errorMessage == null) {
                    CashHistorySkeleton(compact)
                } else {
                    SessionListSection(
                        uiState = uiState,
                        compact = compact,
                        isSearchExpanded = isSearchExpanded,
                        onSearchChanged = onSearchChanged,
                        onStatusFilterChanged = onStatusFilterChanged,
                        onRefresh = onRefresh,
                        onShowDetail = onShowDetail,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage,
                        onOpenMobileFilters = { showMobileFilters = true },
                        onOpenMobileOverview = { showMobileOverview = true },
                    )
                }
            }
        }
    }

    if (showMobileFilters || showFilterPopup) {
        TbMobileControlSheet(
            title = "Filter sesi",
            subtitle = "Atur periode dan status sesi kas",
            onDismiss = {
                showMobileFilters = false
                onDismissFilter()
            },
            testTag = "cash-session-filter-sheet",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Periode", style = MaterialTheme.typography.labelLarge, color = SessionText, fontWeight = FontWeight.SemiBold)
                SessionPeriodSelector(
                    selectedPreset = uiState.selectedPreset,
                    dateLabel = uiState.compactDateRangeLabel(),
                    onSelected = onDatePresetSelected,
                    onCalendarClick = {
                        showMobileFilters = false
                        onDismissFilter()
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.labelLarge, color = SessionText, fontWeight = FontWeight.SemiBold)
                SessionStatusSelector(
                    selectedStatus = uiState.statusFilter,
                    onSelected = onStatusFilterChanged,
                )
            }
            TbMobileSheetDoneButton(
                onClick = {
                    showMobileFilters = false
                    onDismissFilter()
                },
                testTag = "cash-session-filter-done",
            )
        }
    }

    if (showMobileOverview || showSummaryPopup) {
        TbMobileControlSheet(
            title = "Ringkasan kas",
            subtitle = uiState.fullDateRangeLabel(),
            onDismiss = {
                showMobileOverview = false
                onDismissSummary()
            },
            testTag = "cash-session-overview-sheet",
        ) {
            SessionOverview(uiState = uiState, compact = true)
            TbMobileSheetDoneButton(
                onClick = {
                    showMobileOverview = false
                    onDismissSummary()
                },
                label = "Tutup",
                testTag = "cash-session-overview-done",
            )
        }
    }

    if (showDatePicker) {
        val activeMode = when (uiState.selectedPreset) {
            "Bulan ini", "Bulan" -> "Bulan"
            "Minggu ini", "Minggu" -> "Minggu"
            else -> "Hari"
        }
        HistoryDatePickerDialog(
            currentDate = uiState.startDate ?: uiState.selectedDate,
            initialMode = activeMode,
            onDismiss = { showDatePicker = false },
            onConfirmRange = { newStartStr, newEndStr, mode ->
                val preset = when (mode) {
                    "Bulan" -> "Bulan ini"
                    "Minggu" -> "Minggu ini"
                    else -> "Hari ini"
                }
                onDateRangeChanged(newStartStr, newEndStr, preset)
                showDatePicker = false
            },
            onConfirm = {
                onDateChanged(it)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun SessionDateToolbar(
    uiState: CashSessionHistoryUiState,
    compact: Boolean,
    onCalendarClick: () -> Unit,
    onDatePresetSelected: (String) -> Unit,
) {
    SessionPeriodSelector(
        selectedPreset = uiState.selectedPreset,
        dateLabel = uiState.compactDateRangeLabel(),
        onSelected = onDatePresetSelected,
        onCalendarClick = onCalendarClick,
        modifier = Modifier
            .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(500.dp)),
    )
}

@Composable
private fun SessionPeriodSelector(
    selectedPreset: String?,
    dateLabel: String,
    onSelected: (String) -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TbPeriodFilterRow(
        selectedValue = selectedPreset?.let { selected ->
            listOf("Hari ini", "Minggu ini", "Bulan ini").firstOrNull { selected.matchesPreset(it) }
        },
        presets = listOf(
            "Hari ini" to "Hari",
            "Minggu ini" to "Minggu",
            "Bulan ini" to "Bulan",
        ),
        dateLabel = dateLabel,
        dateSelected = selectedPreset == null,
        onPresetSelected = onSelected,
        onDateClick = onCalendarClick,
        modifier = modifier,
        testTag = "cash-session-period-selector",
        presetTestTags = listOf(
            "cash-session-period-day",
            "cash-session-period-week",
            "cash-session-period-month",
        ),
        dateTestTag = "cash-session-period-date",
    )
}

@Composable
private fun SessionOverview(uiState: CashSessionHistoryUiState, compact: Boolean) {
    val openCount = uiState.sessions.count { it.status.equals("OPEN", true) }
    val difference = uiState.sessions.fold(BigDecimal.ZERO) { total, item ->
        total + (item.difference ?: BigDecimal.ZERO)
    }
    val totalSystemCash = uiState.sessions.fold(BigDecimal.ZERO) { total, item ->
        total + (item.systemCash ?: item.openingCash)
    }
    val totalExpenses = uiState.sessions.fold(BigDecimal.ZERO) { total, item ->
        total + item.totalExpenses
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-overview"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SessionMetricCard(
                        title = "TOTAL KAS SISTEM",
                        value = totalSystemCash.asCompactCurrency(),
                        icon = Icons.Outlined.AccountBalanceWallet,
                        iconTint = Brand600,
                        iconBg = Brand50,
                        modifier = Modifier.weight(1f),
                    )
                    SessionMetricCard(
                        title = "SELISIH KAS",
                        value = difference.asCompactCurrency(),
                        icon = Icons.Outlined.Payments,
                        iconTint = if (difference.compareTo(BigDecimal.ZERO) < 0) ErrorRed else Brand600,
                        iconBg = if (difference.compareTo(BigDecimal.ZERO) < 0) ErrorBg else Brand50,
                        valueColor = difference.differenceColor(),
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(color = Slate200.copy(alpha = 0.7f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Pengeluaran:", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text(
                            totalExpenses.asCompactCurrency(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) ErrorRed else Slate700,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Sesi:", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text(
                            if (openCount > 0) "$openCount aktif / ${uiState.totalSessions}" else "${uiState.totalSessions} sesi",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (openCount > 0) Brand700 else Slate600,
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionMetricCard(
                    title = "TOTAL KAS SISTEM",
                    value = totalSystemCash.asCompactCurrency(),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = Brand600,
                    iconBg = Brand50,
                    modifier = Modifier.weight(1.3f),
                )
                OverviewDivider()
                SessionMetricCard(
                    title = "SELISIH KAS",
                    value = difference.asCompactCurrency(),
                    icon = Icons.Outlined.Payments,
                    iconTint = if (difference.compareTo(BigDecimal.ZERO) < 0) ErrorRed else Brand600,
                    iconBg = if (difference.compareTo(BigDecimal.ZERO) < 0) ErrorBg else Brand50,
                    valueColor = difference.differenceColor(),
                    modifier = Modifier.weight(1.3f),
                )
                OverviewDivider()
                SessionOverviewValue("Pengeluaran", totalExpenses.asCompactCurrency(), Modifier.weight(1f))
                OverviewDivider()
                SessionOverviewValue(
                    "Sesi aktif",
                    if (openCount > 0) "$openCount terbuka / ${uiState.totalSessions}" else "${uiState.totalSessions} sesi",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SessionMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    valueColor: Color = Slate900,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = iconBg,
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionOverviewValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Slate400)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Slate900,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverviewDivider() {
    Box(Modifier.width(1.dp).height(44.dp).background(Slate200))
}

@Composable
private fun SessionListSection(
    uiState: CashSessionHistoryUiState,
    compact: Boolean,
    isSearchExpanded: Boolean = false,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenMobileFilters: () -> Unit,
    onOpenMobileOverview: () -> Unit,
) {
    val visibleSessions = uiState.filteredSessions()
    val isFiltered = uiState.statusFilter != "Semua" ||
        (uiState.selectedPreset != null && uiState.selectedPreset != "Hari ini") ||
        (uiState.selectedPreset == null && uiState.selectedDate != null)

    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (compact) {
            // Expandable Search Bar for mobile
            AnimatedVisibility(
                visible = isSearchExpanded || uiState.searchQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari kasir atau nomor sesi...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchChanged,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("cash-session-search-input"),
                            )
                        }
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChanged("") },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            SessionToolbar(
                searchQuery = uiState.searchQuery,
                selectedStatus = uiState.statusFilter,
                isFiltered = isFiltered,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                compact = false,
                onOpenMobileFilters = onOpenMobileFilters,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Daftar sesi",
                style = MaterialTheme.typography.titleMedium,
                color = Slate900,
                fontWeight = FontWeight.Bold,
            )
        }
        when {
            uiState.isLoading -> SessionRowsSkeleton(compact)
            uiState.errorMessage != null -> SessionFeedback(
                title = "Sesi kas gagal dimuat",
                message = uiState.errorMessage,
                actionLabel = "Coba lagi",
                onAction = onRefresh,
            )
            uiState.sessions.isEmpty() -> SessionFeedback(
                title = "Belum ada sesi kas",
                message = "Sesi kas yang sudah dibuka akan muncul di halaman ini.",
            )
            visibleSessions.isEmpty() -> SessionFeedback(
                title = "Sesi tidak ditemukan",
                message = "Coba ubah kata pencarian atau filter status.",
            )
            compact -> Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cash-session-grouped-list"),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Column {
                    visibleSessions.forEachIndexed { index, session ->
                        SessionMobileItem(session, onShowDetail)
                        if (index < visibleSessions.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
            else -> SessionDesktopTable(visibleSessions, onShowDetail)
        }
        if (visibleSessions.isNotEmpty()) {
            SessionPagination(uiState, visibleSessions.size, onPreviousPage, onNextPage, compact)
        }
    }
}

@Composable
private fun SessionToolbar(
    searchQuery: String,
    selectedStatus: String,
    isFiltered: Boolean,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    compact: Boolean,
    onOpenMobileFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(if (compact) 42.dp else 48.dp)
                .testTag("cash-session-search"),
            color = Color.White,
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (compact) "Cari kasir atau sesi..." else "Cari kasir atau nomor sesi...",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChanged("") },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Hapus pencarian",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Surface(
            onClick = onOpenMobileFilters,
            modifier = Modifier
                .size(if (compact) 42.dp else 48.dp)
                .testTag("cash-session-open-filters"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (isFiltered) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
            contentColor = Color(0xFF0F172A),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Filter sesi kas",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF0F172A),
                )
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionStatusSelector(
    selectedStatus: String,
    onSelected: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = Slate100,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                Triple("Semua", "Semua", "all"),
                Triple("OPEN", "Terbuka", "open"),
                Triple("CLOSED", "Ditutup", "closed"),
            ).forEach { (value, label, tag) ->
                val isSelected = selectedStatus == value
                Surface(
                    onClick = { onSelected(value) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("cash-session-status-$tag"),
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelected) BorderStroke(1.dp, Slate200) else null,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Brand700 else Slate500,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStatusDropdown(
    selectedStatus: String,
    onStatusFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cash-session-status-filter"),
            color = Color.White,
            contentColor = Slate900,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Slate200),
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selectedStatus == "Semua") "Semua Status" else selectedStatus.statusLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = Slate900,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription = "Pilih status", tint = Slate400, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(180.dp).background(Color.White),
        ) {
            listOf("Semua", "OPEN", "CLOSED").forEach { status ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (status == "Semua") "Semua status" else status.statusLabel(),
                            fontWeight = if (selectedStatus == status) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedStatus == status) Brand700 else Slate900,
                        )
                    },
                    onClick = {
                        onStatusFilterChanged(status)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SessionDesktopTable(sessions: List<CashSession>, onShowDetail: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
    ) {
        Column {
            SessionHeader()
            sessions.forEachIndexed { index, session ->
                if (index > 0) HorizontalDivider(color = Slate200.copy(alpha = 0.7f))
                SessionRow(session, onShowDetail)
            }
        }
    }
}

@Composable
private fun SessionHeader() {
    Row(
        Modifier.fillMaxWidth().background(Slate50).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionHeaderLabel("KASIR", Modifier.weight(1.35f))
        SessionHeaderLabel("WAKTU SESI", Modifier.weight(1.4f))
        SessionHeaderLabel("MODAL AWAL", Modifier.weight(1.1f), Alignment.End)
        SessionHeaderLabel("PENGELUARAN", Modifier.weight(1.1f), Alignment.End)
        SessionHeaderLabel("KAS SISTEM", Modifier.weight(1.1f), Alignment.End)
        SessionHeaderLabel("SELISIH", Modifier.weight(1f), Alignment.End)
        SessionHeaderLabel("STATUS", Modifier.weight(0.9f), Alignment.CenterHorizontally)
        Spacer(Modifier.width(44.dp))
    }
}

@Composable
private fun SessionRow(session: CashSession, onShowDetail: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onShowDetail(session.id) }
            .testTag("cash-session-${session.id}")
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1.35f), verticalAlignment = Alignment.CenterVertically) {
            SessionAvatar(session.userName, session.status.equals("OPEN", true))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    session.userName ?: "Kasir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate900,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${session.id.take(8).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                )
            }
        }
        Column(Modifier.weight(1.4f)) {
            Text(session.openedAt.asDateTime(), style = MaterialTheme.typography.bodySmall, color = Slate900)
            Text(
                session.closedAt?.let { "Tutup ${it.asDateTime()}" } ?: "Masih berjalan",
                style = MaterialTheme.typography.labelSmall,
                color = if (session.status.equals("OPEN", true)) Brand600 else Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SessionAmount(session.openingCash.asCompactCurrency(), Modifier.weight(1.1f))
        val hasExpense = session.totalExpenses.compareTo(BigDecimal.ZERO) > 0
        SessionAmount(
            session.totalExpenses.asCompactCurrency(),
            Modifier.weight(1.1f),
            FontWeight.Medium,
            if (hasExpense) ErrorRed else Slate700,
        )
        SessionAmount((session.systemCash ?: session.openingCash).asCompactCurrency(), Modifier.weight(1.1f), FontWeight.Bold)
        SessionAmount(
            (session.difference ?: BigDecimal.ZERO).asCompactCurrency(),
            Modifier.weight(1f),
            FontWeight.Bold,
            session.difference.differenceColor(),
        )
        Box(Modifier.weight(0.9f), contentAlignment = Alignment.Center) { SessionStatusBadge(session.status) }
        IconButton(
            onClick = { onShowDetail(session.id) },
            modifier = Modifier.size(44.dp).testTag("cash-session-detail-${session.id}"),
        ) {
            Icon(Icons.Outlined.Visibility, "Lihat rincian", tint = Brand600, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SessionAmount(
    text: String,
    modifier: Modifier,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Slate900,
) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = weight,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SessionCompactCard(session: CashSession, onShowDetail: (String) -> Unit) {
    SessionMobileItem(session, onShowDetail)
}

@Composable
private fun SessionMobileItem(
    session: CashSession,
    onShowDetail: (String) -> Unit,
) {
    val isOpen = session.status.equals("OPEN", true)
    val diff = session.difference ?: BigDecimal.ZERO
    val hasDiff = diff.compareTo(BigDecimal.ZERO) != 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowDetail(session.id) }
            .testTag("cash-session-${session.id}")
            .padding(16.dp),
    ) {
        // Header: Icon, Details & Chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = session.userName?.takeIf { it.isNotBlank() } ?: "Kasir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Sesi #${session.id.take(8).uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFCBD5E1),
                        )
                        Text(
                            text = "Modal: ${session.openingCash.asCompactCurrency()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(Modifier.height(10.dp))

        // Details Grid: Baris 1 Kas Sistem & Selisih
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "KAS SISTEM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = (session.systemCash ?: session.openingCash).asCompactCurrency(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SELISIH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.End,
                )
                Spacer(Modifier.height(2.dp))
                val diffColor = when {
                    diff.compareTo(BigDecimal.ZERO) < 0 -> Color(0xFFDC2626)
                    diff.compareTo(BigDecimal.ZERO) > 0 -> Color(0xFF15803D)
                    else -> Color(0xFF15803D)
                }
                Text(
                    text = if (hasDiff) diff.asCompactCurrency() else "Rp 0 (Pas)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = diffColor,
                    textAlign = TextAlign.End,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Color(0xFFF8FAFC))
        Spacer(Modifier.height(6.dp))

        // Details Grid: Baris 2 Tanggal & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.openedAt.asDateTime(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
            )
            val statusColor = if (isOpen) Color(0xFF15803D) else Color(0xFF64748B)
            Text(
                text = if (isOpen) "Terbuka" else "Ditutup",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun SessionAvatar(name: String?, isOpen: Boolean = false) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = if (isOpen) Brand50 else Slate100,
        border = BorderStroke(1.dp, if (isOpen) Brand100 else Slate200),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.orEmpty().ifBlank { "K" }.take(1).uppercase(),
                color = if (isOpen) Brand700 else Slate700,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SessionCardValue(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = Slate900,
) {
    Column(
        modifier = modifier.background(Slate50, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionStatusBadge(status: String) {
    val isOpen = status.equals("OPEN", true)
    Surface(
        color = if (isOpen) Brand50 else Slate100,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (isOpen) Brand100 else Slate200),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (isOpen) Brand600 else Slate400, CircleShape),
            )
            Text(
                status.statusLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOpen) Brand700 else Slate600,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SessionPagination(
    uiState: CashSessionHistoryUiState,
    visibleCount: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean,
) {
    val currentStart = if (visibleCount == 0) 0 else ((uiState.page - 1) * uiState.pageSize) + 1
    val currentEnd = if (visibleCount == 0) 0 else currentStart + visibleCount - 1
    val effectiveTotal = if (uiState.searchQuery.isBlank() && uiState.statusFilter == "Semua") {
        maxOf(uiState.totalSessions, visibleCount.toLong())
    } else {
        visibleCount.toLong()
    }

    val supportingText = if (compact) {
        val totalSessions = if (uiState.totalSessions > 0) uiState.totalSessions else uiState.sessions.size
        "${uiState.sessions.size} dari $totalSessions sesi"
    } else {
        "Menampilkan $currentStart–$currentEnd dari $effectiveTotal"
    }

    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = supportingText,
        isLoading = uiState.isLoading,
        compact = compact,
        testTag = "cash-session-pagination",
    )
}

@Composable
private fun SessionRowsSkeleton(compact: Boolean) {
    if (compact) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cash-session-rows-skeleton"),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Column {
                repeat(3) { index ->
                    if (index > 0) HorizontalDivider(color = Color(0xFFF1F5F9))
                    SessionMobileItemSkeleton()
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth().testTag("cash-session-rows-skeleton")) {
            SessionDesktopTableSkeleton()
        }
    }
}

@Composable
private fun SessionMobileItemSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SkeletonBox(Modifier.width(110.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.width(140.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            SkeletonBox(Modifier.size(16.dp), shape = CircleShape)
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SkeletonBox(Modifier.width(65.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                SkeletonBox(Modifier.width(95.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SkeletonBox(Modifier.width(50.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                SkeletonBox(Modifier.width(80.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(Modifier.width(62.dp).height(20.dp), shape = RoundedCornerShape(6.dp))
            SkeletonBox(Modifier.width(120.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
        }
    }
}

@Composable
private fun SessionDesktopTableSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
    ) {
        Column {
            SessionHeader()
            repeat(5) { index ->
                if (index > 0) HorizontalDivider(color = Slate200.copy(alpha = 0.7f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1.35f), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(36.dp), shape = CircleShape)
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonBox(Modifier.width(90.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                            SkeletonBox(Modifier.width(60.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                        }
                    }
                    Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SkeletonBox(Modifier.width(100.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                        SkeletonBox(Modifier.width(80.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                    }
                    Box(Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(75.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(75.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(75.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(70.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(0.9f), contentAlignment = Alignment.Center) {
                        SkeletonBox(Modifier.width(60.dp).height(22.dp), shape = RoundedCornerShape(6.dp))
                    }
                    Spacer(Modifier.width(44.dp))
                }
            }
        }
    }
}

@Composable
private fun CashHistorySkeleton(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-skeleton"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!compact) {
            SkeletonBox(Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(Modifier.width(110.dp).height(22.dp), shape = RoundedCornerShape(4.dp))
        }
        SessionRowsSkeleton(compact)
    }
}

@Composable
private fun SessionFeedback(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Slate100,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = Slate900, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = Slate500, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(actionLabel, color = Brand600, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SessionHeaderLabel(
    text: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

private fun String.statusLabel(): String = when (uppercase()) {
    "OPEN" -> "Terbuka"
    "CLOSED" -> "Ditutup"
    else -> this
}

private fun String?.matchesPreset(preset: String): Boolean = when (preset) {
    "Hari ini" -> this == "Hari ini"
    "Minggu ini" -> this == "Minggu ini" || this == "7 hari"
    "Bulan ini" -> this == "Bulan ini" || this == "30 hari"
    else -> false
}

private fun BigDecimal?.differenceColor(): Color = when {
    this == null || compareTo(BigDecimal.ZERO) == 0 -> Slate500
    compareTo(BigDecimal.ZERO) < 0 -> ErrorRed
    else -> Brand600
}

private fun BigDecimal.asCompactCurrency(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }.format(this)

private fun CashSessionHistoryUiState.filteredSessions(): List<CashSession> {
    val query = searchQuery.trim()
    if (query.isBlank()) return sessions
    return sessions.filter { session ->
        session.id.contains(query, ignoreCase = true) ||
            session.userId.contains(query, ignoreCase = true) ||
            session.userName.orEmpty().contains(query, ignoreCase = true) ||
            session.status.statusLabel().contains(query, ignoreCase = true)
    }
}

private fun CashSessionHistoryUiState.compactDateRangeLabel(): String {
    val start = startDate ?: selectedDate
    val end = endDate ?: selectedDate
    if (start == null || end == null) return "Tanggal"
    val startDateValue = runCatching { LocalDate.parse(start) }.getOrNull() ?: return "Tanggal"
    val endDateValue = runCatching { LocalDate.parse(end) }.getOrNull() ?: return "Tanggal"
    val locale = Locale.forLanguageTag("id-ID")
    return when {
        startDateValue == endDateValue -> endDateValue.format(DateTimeFormatter.ofPattern("dd MMM", locale))
        startDateValue.month == endDateValue.month ->
            "${startDateValue.dayOfMonth.toString().padStart(2, '0')}–${endDateValue.format(DateTimeFormatter.ofPattern("dd MMM", locale))}"
        else -> "${startDateValue.format(DateTimeFormatter.ofPattern("dd MMM", locale))}–${endDateValue.format(DateTimeFormatter.ofPattern("dd MMM", locale))}"
    }
}

private fun CashSessionHistoryUiState.fullDateRangeLabel(): String {
    val start = startDate ?: selectedDate
    val end = endDate ?: selectedDate
    if (start == null || end == null) return "Semua periode"
    val locale = Locale.forLanguageTag("id-ID")
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", locale)
    val startValue = runCatching { LocalDate.parse(start) }.getOrNull() ?: return compactDateRangeLabel()
    val endValue = runCatching { LocalDate.parse(end) }.getOrNull() ?: return compactDateRangeLabel()
    return if (startValue == endValue) endValue.format(formatter) else "${startValue.format(formatter)} – ${endValue.format(formatter)}"
}

private fun String.asDateTime(): String = runCatching {
    val zonedDateTime = try {
        OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
    } catch (_: Exception) {
        java.time.LocalDateTime.parse(this).atZone(ZoneId.of("Asia/Jakarta"))
    }
    zonedDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

private fun String.asDateTimeShort(): String = runCatching {
    val zonedDateTime = try {
        OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
    } catch (_: Exception) {
        java.time.LocalDateTime.parse(this).atZone(ZoneId.of("Asia/Jakarta"))
    }
    zonedDateTime.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
