package com.tbterminal.app.ui.cashhistory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
            ) {
                if (uiState.isLoading && uiState.sessions.isEmpty() && uiState.errorMessage == null) {
                    CashHistorySkeleton(compact)
                } else {
                    if (!compact) {
                        SessionDateToolbar(
                            uiState = uiState,
                            compact = false,
                            onCalendarClick = { showDatePicker = true },
                            onDatePresetSelected = onDatePresetSelected,
                        )
                        SessionOverview(uiState, compact = false)
                    }
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
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-overview"),
        color = SessionSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SessionBorder),
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SessionDifferenceSummary(difference)
                HorizontalDivider(color = SessionBorder.copy(alpha = 0.75f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SessionOverviewValue("Total sesi", uiState.totalSessions.toString(), Modifier.weight(1f))
                    SessionOverviewValue("Masih terbuka", openCount.toString(), Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionDifferenceSummary(difference, Modifier.weight(1.5f))
                OverviewDivider()
                SessionOverviewValue("Total sesi", uiState.totalSessions.toString(), Modifier.weight(1f))
                OverviewDivider()
                SessionOverviewValue("Terbuka di halaman", openCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SessionDifferenceSummary(difference: BigDecimal, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            color = SessionPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(13.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = SessionPrimaryDark,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Selisih kas", style = MaterialTheme.typography.labelMedium, color = SessionMuted)
            Text(
                difference.asCompactCurrency(),
                style = MaterialTheme.typography.titleLarge,
                color = difference.differenceColor(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Akumulasi halaman ini", style = MaterialTheme.typography.labelSmall, color = SessionMuted)
        }
    }
}

@Composable
private fun SessionOverviewValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SessionMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = SessionText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OverviewDivider() {
    Box(Modifier.width(1.dp).height(46.dp).background(SessionBorder))
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
                color = SessionText,
                fontWeight = FontWeight.SemiBold,
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
        TextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari kasir atau sesi", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            modifier = Modifier.weight(1f).height(50.dp).testTag("cash-session-search"),
            shape = RoundedCornerShape(15.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SessionSoft,
                unfocusedContainerColor = SessionSoft,
                disabledContainerColor = SessionSoft,
                focusedTextColor = SessionText,
                unfocusedTextColor = SessionText,
                focusedLeadingIconColor = SessionPrimaryDark,
                unfocusedLeadingIconColor = SessionMuted,
                focusedPlaceholderColor = SessionMuted,
                unfocusedPlaceholderColor = SessionMuted,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(
                Triple("Semua", "Semua", "all"),
                Triple("OPEN", "Terbuka", "open"),
                Triple("CLOSED", "Ditutup", "closed"),
            ).forEach { (value, label, tag) ->
                SessionStatusOption(
                    label = label,
                    selected = selectedStatus == value,
                    onClick = { onSelected(value) },
                    modifier = Modifier.weight(1f).testTag("cash-session-status-$tag"),
                )
            }
        }
    }
}

@Composable
private fun SessionStatusOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else SessionMuted,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
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
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cash-session-status-filter"),
            color = SessionSurface,
            contentColor = SessionText,
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, SessionBorder),
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selectedStatus == "Semua") "Semua" else selectedStatus.statusLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription = "Pilih status", tint = SessionMuted, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(180.dp).background(SessionSurface),
        ) {
            listOf("Semua", "OPEN", "CLOSED").forEach { status ->
                DropdownMenuItem(
                    text = { Text(if (status == "Semua") "Semua status" else status.statusLabel()) },
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
        color = SessionSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SessionBorder),
    ) {
        Column {
            SessionHeader()
            sessions.forEachIndexed { index, session ->
                if (index > 0) HorizontalDivider(color = SessionBorder)
                SessionRow(session, onShowDetail)
            }
        }
    }
}

@Composable
private fun SessionHeader() {
    Row(
        Modifier.fillMaxWidth().background(SessionSoft).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionHeaderLabel("Kasir", Modifier.weight(1.35f))
        SessionHeaderLabel("Waktu sesi", Modifier.weight(1.55f))
        SessionHeaderLabel("Modal awal", Modifier.weight(1.1f), Alignment.End)
        SessionHeaderLabel("Kas sistem", Modifier.weight(1.1f), Alignment.End)
        SessionHeaderLabel("Selisih", Modifier.weight(1f), Alignment.End)
        SessionHeaderLabel("Status", Modifier.weight(0.85f), Alignment.CenterHorizontally)
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SessionRow(session: CashSession, onShowDetail: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("cash-session-${session.id}")
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1.35f), verticalAlignment = Alignment.CenterVertically) {
            SessionAvatar(session.userName)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    session.userName ?: "Kasir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SessionText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(session.id.take(8).uppercase(), style = MaterialTheme.typography.labelSmall, color = SessionMuted)
            }
        }
        Column(Modifier.weight(1.55f)) {
            Text(session.openedAt.asDateTime(), style = MaterialTheme.typography.bodySmall, color = SessionText)
            Text(
                session.closedAt?.let { "Tutup ${it.asDateTime()}" } ?: "Masih berjalan",
                style = MaterialTheme.typography.labelSmall,
                color = SessionMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SessionAmount(session.openingCash.asCompactCurrency(), Modifier.weight(1.1f))
        SessionAmount((session.systemCash ?: session.openingCash).asCompactCurrency(), Modifier.weight(1.1f), FontWeight.SemiBold)
        SessionAmount(
            (session.difference ?: BigDecimal.ZERO).asCompactCurrency(),
            Modifier.weight(1f),
            FontWeight.SemiBold,
            session.difference.differenceColor(),
        )
        Box(Modifier.weight(0.85f), contentAlignment = Alignment.Center) { SessionStatusBadge(session.status) }
        IconButton(
            onClick = { onShowDetail(session.id) },
            modifier = Modifier.size(48.dp).testTag("cash-session-detail-${session.id}"),
        ) {
            Icon(Icons.Outlined.Visibility, "Lihat rincian", tint = SessionPrimaryDark, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SessionAmount(
    text: String,
    modifier: Modifier,
    weight: FontWeight = FontWeight.Medium,
    color: Color = SessionText,
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
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-${session.id}"),
        color = SessionSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SessionBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SessionAvatar(session.userName)
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.userName ?: "Kasir",
                        style = MaterialTheme.typography.titleSmall,
                        color = SessionText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(session.openedAt.asDateTime(), style = MaterialTheme.typography.bodySmall, color = SessionMuted, maxLines = 1)
                }
                SessionStatusBadge(session.status)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Kas sistem", style = MaterialTheme.typography.labelMedium, color = SessionMuted)
                Text(
                    (session.systemCash ?: session.openingCash).asCompactCurrency(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = SessionText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionCardValue("Modal awal", session.openingCash.asCompactCurrency(), Modifier.weight(1f))
                SessionCardValue(
                    "Selisih",
                    (session.difference ?: BigDecimal.ZERO).asCompactCurrency(),
                    Modifier.weight(1f),
                    session.difference.differenceColor(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = SessionMuted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    session.closedAt?.let { "Ditutup ${it.asDateTime()}" } ?: "Sesi masih berjalan",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = SessionMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { onShowDetail(session.id) },
                    modifier = Modifier.height(48.dp).testTag("cash-session-detail-${session.id}"),
                    colors = ButtonDefaults.textButtonColors(contentColor = SessionPrimaryDark),
                ) { Text("Rincian", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SessionAvatar(name: String?) {
    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = SessionSoft) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.orEmpty().ifBlank { "K" }.take(1).uppercase(), color = SessionPrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SessionCardValue(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = SessionText,
) {
    Column(
        modifier = modifier.background(SessionSoft, RoundedCornerShape(13.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SessionMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionStatusBadge(status: String) {
    val isOpen = status.equals("OPEN", true)
    val tint = if (isOpen) MaterialTheme.colorScheme.primary else SessionPrimaryDark
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp)) {
        Text(
            status.statusLabel(),
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
        )
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
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 210.dp else 64.dp))
        }
    }
}

@Composable
private fun CashHistorySkeleton(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-session-skeleton"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 98.dp else 50.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 154.dp else 92.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(50.dp))
        repeat(if (compact) 3 else 5) {
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 210.dp else 64.dp))
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
        color = SessionSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SessionBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Payments, contentDescription = null, tint = SessionMuted, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = SessionText, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = SessionMuted, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction, modifier = Modifier.height(48.dp)) { Text(actionLabel) }
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
        Text(text, style = MaterialTheme.typography.labelMedium, color = SessionMuted, fontWeight = FontWeight.SemiBold)
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
    this == null || compareTo(BigDecimal.ZERO) == 0 -> SessionMuted
    compareTo(BigDecimal.ZERO) < 0 -> SessionDanger
    else -> SessionPrimaryDark
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
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
