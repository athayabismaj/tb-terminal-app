package com.tbterminal.app.ui.cashhistory

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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
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
                    // Toolbar periode selalu ditampilkan agar pengguna mobile dapat langsung mengganti periode
                    SessionDateToolbar(
                        uiState = uiState,
                        compact = compact,
                        onCalendarClick = { showDatePicker = true },
                        onDatePresetSelected = onDatePresetSelected,
                    )

                    // Ringkasan metrik kas sesi
                    SessionOverview(uiState = uiState, compact = compact)

                    SessionListSection(
                        uiState = uiState,
                        compact = compact,
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

    if (showMobileFilters) {
        TbMobileControlSheet(
            title = "Filter sesi",
            subtitle = "Atur periode dan status sesi kas",
            onDismiss = { showMobileFilters = false },
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
                onClick = { showMobileFilters = false },
                testTag = "cash-session-filter-done",
            )
        }
    }

    if (showMobileOverview) {
        TbMobileControlSheet(
            title = "Ringkasan kas",
            subtitle = uiState.fullDateRangeLabel(),
            onDismiss = { showMobileOverview = false },
            testTag = "cash-session-overview-sheet",
        ) {
            SessionOverview(uiState = uiState, compact = true)
            TbMobileSheetDoneButton(
                onClick = { showMobileOverview = false },
                label = "Tutup",
                testTag = "cash-session-overview-done",
            )
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = uiState.endDate ?: uiState.selectedDate,
            onDismiss = { showDatePicker = false },
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
    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Daftar sesi",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = Slate900,
                fontWeight = FontWeight.Bold,
            )
            if (compact) {
                TbMobileSummaryButton(
                    onClick = onOpenMobileOverview,
                    testTag = "cash-session-open-overview",
                )
            }
        }
        SessionToolbar(
            searchQuery = uiState.searchQuery,
            selectedStatus = uiState.statusFilter,
            onSearchChanged = onSearchChanged,
            onStatusFilterChanged = onStatusFilterChanged,
            compact = compact,
            onOpenMobileFilters = onOpenMobileFilters,
        )
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
            compact -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleSessions.forEach { SessionCompactCard(it, onShowDetail) }
            }
            else -> SessionDesktopTable(visibleSessions, onShowDetail)
        }
        SessionPagination(uiState, visibleSessions.size, onPreviousPage, onNextPage, compact)
    }
}

@Composable
private fun SessionToolbar(
    searchQuery: String,
    selectedStatus: String,
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
                .height(48.dp)
                .testTag("cash-session-search"),
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Slate200),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (compact) "Cari kasir atau sesi..." else "Cari kasir atau nomor sesi...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = Slate900,
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
                            tint = Slate400,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }

        if (compact) {
            TbMobileFilterButton(
                onClick = onOpenMobileFilters,
                active = selectedStatus != "Semua",
                testTag = "cash-session-open-filters",
            )
        } else {
            SessionStatusDropdown(
                selectedStatus = selectedStatus,
                onStatusFilterChanged = onStatusFilterChanged,
                modifier = Modifier.width(180.dp),
            )
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
    val isOpen = session.status.equals("OPEN", true)
    val diff = session.difference ?: BigDecimal.ZERO
    val hasDiff = diff.compareTo(BigDecimal.ZERO) != 0
    val diffColor = when {
        diff.compareTo(BigDecimal.ZERO) < 0 -> ErrorRed
        diff.compareTo(BigDecimal.ZERO) > 0 -> Brand600
        else -> Slate500
    }

    Surface(
        onClick = { onShowDetail(session.id) },
        modifier = Modifier.fillMaxWidth().testTag("cash-session-${session.id}"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row: Avatar, Cashier Name & Session ID, Status Badge, Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isOpen) Brand50 else Slate100, CircleShape)
                        .border(1.dp, if (isOpen) Brand100 else Slate200, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = session.userName.orEmpty().ifBlank { "K" }.take(1).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOpen) Brand700 else Slate700,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.userName ?: "Kasir",
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate900,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "#${session.id.take(8).uppercase()} • Buka ${session.openedAt.asDateTimeShort()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

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
                            text = if (isOpen) "Terbuka" else "Ditutup",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOpen) Brand700 else Slate600,
                            maxLines = 1,
                        )
                    }
                }

                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Financial Summary Block
            Surface(
                color = Slate50,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "KAS SISTEM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                (session.systemCash ?: session.openingCash).asCompactCurrency(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "SELISIH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                if (hasDiff) diff.asCompactCurrency() else "Rp 0 (Pas)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = diffColor,
                            )
                        }
                    }

                    HorizontalDivider(color = Slate200.copy(alpha = 0.7f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modal Awal", fontSize = 10.sp, color = Slate400)
                            Text(
                                session.openingCash.asCompactCurrency(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pengeluaran", fontSize = 10.sp, color = Slate400)
                            val hasExpense = session.totalExpenses.compareTo(BigDecimal.ZERO) > 0
                            Text(
                                session.totalExpenses.asCompactCurrency(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasExpense) ErrorRed else Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Kas Fisik", fontSize = 10.sp, color = Slate400)
                            Text(
                                session.closingCash?.asCompactCurrency() ?: "—",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Footer Row: Status lifecycle note & Detail button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (isOpen) Brand600 else Slate400,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (isOpen) {
                            "Sesi masih berlangsung"
                        } else {
                            "Ditutup ${session.closedAt?.asDateTimeShort() ?: "-"}"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isOpen) Brand700 else Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    onClick = { onShowDetail(session.id) },
                    modifier = Modifier.testTag("cash-session-detail-${session.id}"),
                    color = Color.Transparent,
                ) {
                    Text(
                        "Lihat Rincian →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brand600,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                }
            }
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
    val start = if (visibleCount == 0) 0 else ((uiState.page - 1) * uiState.pageSize) + 1
    val end = if (visibleCount == 0) 0 else start + visibleCount - 1
    val totalText = if (uiState.searchQuery.isBlank()) "${uiState.totalSessions} sesi" else "$visibleCount hasil"
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (compact) {
            if (uiState.searchQuery.isBlank()) "${uiState.totalSessions} data" else "$visibleCount hasil"
        } else {
            "Menampilkan $start-$end dari $totalText"
        },
        isLoading = uiState.isLoading,
        testTag = "cash-session-pagination",
    )
}

@Composable
private fun SessionRowsSkeleton(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-rows-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(if (compact) 3 else 5) {
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 180.dp else 64.dp))
        }
    }
}

@Composable
private fun CashHistorySkeleton(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-skeleton"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBox(Modifier.fillMaxWidth().height(50.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 110.dp else 88.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(48.dp))
        repeat(if (compact) 3 else 5) {
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 180.dp else 64.dp))
        }
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
