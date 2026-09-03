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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.ui.components.HistoryDateFilter
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SessionBackground = Color(0xFFF4FAFD)
private val SessionBorder = Color(0xFFE2E8F0)
private val SessionText = Color(0xFF0F172A)
private val SessionMuted = Color(0xFF64748B)
private val SessionPrimary = Color(0xFF059669)

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
    onNextPage: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(SessionBackground)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(if (compact) 16.dp else 40.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 28.dp)
        ) {
            CashSessionHistoryHeader(
                uiState = uiState,
                compact = compact,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = { showDatePicker = true },
                onClearDate = { onDateChanged(null) },
                onDatePresetSelected = onDatePresetSelected
            )
            SessionMetrics(uiState, compact)
            SessionTable(
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onRefresh = onRefresh,
                onShowDetail = onShowDetail,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact
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
            }
        )
    }
}

@Composable
private fun CashSessionHistoryHeader(
    uiState: CashSessionHistoryUiState,
    compact: Boolean,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onCalendarClick: () -> Unit,
    onClearDate: () -> Unit,
    onDatePresetSelected: (String) -> Unit
) {
    if (compact) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HistoryDateFilter(
                selectedDate = uiState.endDate ?: uiState.selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = onClearDate,
                displayTextOverride = uiState.dateRangeLabel(),
                modifier = Modifier.fillMaxWidth()
            )
            SessionDatePresets(
                selectedPreset = uiState.selectedPreset,
                onSelected = onDatePresetSelected,
                modifier = Modifier.fillMaxWidth(),
                compact = true
            )
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryDateFilter(
                selectedDate = uiState.endDate ?: uiState.selectedDate,
                onPreviousDate = onPreviousDate,
                onNextDate = onNextDate,
                onCalendarClick = onCalendarClick,
                onClearDate = onClearDate,
                displayTextOverride = uiState.dateRangeLabel(),
                modifier = Modifier.width(320.dp)
            )
            SessionDatePresets(
                selectedPreset = uiState.selectedPreset,
                onSelected = onDatePresetSelected,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun SessionDatePresets(
    selectedPreset: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SessionBorder),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Hari ini", "Minggu ini", "Bulan ini").forEach { preset ->
                SessionDatePresetChip(
                    text = preset,
                    selected = selectedPreset == preset,
                    onClick = { onSelected(preset) },
                    modifier = if (compact) Modifier.weight(1f) else Modifier
                )
            }
        }
    }
}

@Composable
private fun SessionDatePresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF86F8C9) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = if (selected) Color(0xFF00513A) else SessionMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SessionMetrics(uiState: CashSessionHistoryUiState, compact: Boolean) {
    val openCount = uiState.sessions.count { it.status.equals("OPEN", true) }
    val difference = uiState.sessions.fold(BigDecimal.ZERO) { total, item -> total + (item.difference ?: BigDecimal.ZERO) }
    if (compact) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(Color.White),
            border = BorderStroke(1.dp, SessionBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).background(SessionPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = SessionPrimary)
                    }
                    Column {
                        Text("Selisih kas", color = SessionMuted, fontSize = 12.sp)
                        Text(
                            difference.asCompactCurrency(),
                            color = difference.differenceColor(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SessionSummaryValue("Total sesi", uiState.totalSessions.toString(), Modifier.weight(1f))
                    SessionSummaryValue("Masih terbuka", openCount.toString(), Modifier.weight(1f))
                }
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SessionMetric("TOTAL SESI", uiState.totalSessions.toString(), "Sesuai histori tersimpan", Icons.AutoMirrored.Outlined.ReceiptLong, SessionPrimary, Modifier.weight(1f))
        SessionMetric("SESI TERBUKA", openCount.toString(), "Pada halaman yang tampil", Icons.Outlined.Schedule, Color(0xFF2563EB), Modifier.weight(1f))
        SessionMetric("SELISIH HALAMAN INI", difference.asCurrency(), "Akumulasi selisih kas", Icons.Outlined.Payments, Color(0xFFF59E0B), Modifier.weight(1f))
    }
}

@Composable
private fun SessionSummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color(0xFFF3F7F5), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = SessionMuted, fontSize = 11.sp)
        Text(value, color = SessionText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SessionMetric(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, SessionBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = SessionText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = SessionMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SessionTable(
    uiState: CashSessionHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean
) {
    val visibleSessions = uiState.filteredSessions()
    Column(modifier = Modifier.fillMaxWidth()) {
        SessionToolbar(
            searchQuery = uiState.searchQuery,
            selectedStatus = uiState.statusFilter,
            onSearchChanged = onSearchChanged,
            onStatusFilterChanged = onStatusFilterChanged,
            compact = compact
        )
        Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
        if (!compact) SessionHeader()
        when {
            uiState.isLoading -> LoadingBox()
            uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onRefresh)
            uiState.sessions.isEmpty() -> EmptyBox()
            visibleSessions.isEmpty() -> EmptyBox("Tidak ada sesi yang cocok.")
            else -> visibleSessions.forEach {
                if (compact) SessionCompactCard(it, onShowDetail) else SessionRow(it, onShowDetail)
            }
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
    compact: Boolean
) {
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari kasir atau nomor sesi", color = SessionMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SessionMuted) },
            singleLine = true,
            modifier = fieldModifier.height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SessionText,
                unfocusedTextColor = SessionText,
                cursorColor = SessionPrimary,
                focusedBorderColor = SessionPrimary,
                unfocusedBorderColor = SessionBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
    if (compact) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            search(Modifier.fillMaxWidth())
            SessionStatusDropdown(selectedStatus, onStatusFilterChanged, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            search(Modifier.weight(1f))
            SessionStatusDropdown(selectedStatus, onStatusFilterChanged, Modifier.width(220.dp))
        }
    }
}

@Composable
private fun SessionStatusDropdown(
    selectedStatus: String,
    onStatusFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SessionBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = SessionText
            )
        ) {
            Text(
                text = if (selectedStatus == "Semua") "Semua status" else selectedStatus.statusLabel(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "Pilih status",
                tint = SessionMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .widthIn(min = 220.dp)
                .background(Color.White)
        ) {
            listOf("Semua", "OPEN", "CLOSED").forEach { status ->
                DropdownMenuItem(
                    text = { Text(if (status == "Semua") "Semua status" else status.statusLabel()) },
                    onClick = {
                        onStatusFilterChanged(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Label("KASIR", Modifier.weight(1.3f))
        Label("DIBUKA", Modifier.weight(1.5f))
        Label("DITUTUP", Modifier.weight(1.5f))
        Label("MODAL AWAL", Modifier.weight(1.1f))
        Label("KAS SISTEM", Modifier.weight(1.1f))
        Label("SELISIH", Modifier.weight(1f))
        Label("STATUS", Modifier.weight(0.9f))
        Label("AKSI", Modifier.weight(0.45f))
    }
}

@Composable
private fun SessionRow(session: CashSession, onShowDetail: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.3f)) {
            Text(session.userName ?: "Kasir", color = SessionText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(session.userId.take(8), color = SessionMuted, fontSize = 10.sp)
        }
        Text(session.openedAt.asDateTime(), Modifier.weight(1.5f), color = SessionMuted, fontSize = 12.sp)
        Text(session.closedAt?.asDateTime() ?: "-", Modifier.weight(1.5f), color = SessionMuted, fontSize = 12.sp)
        Text(session.openingCash.asCurrency(), Modifier.weight(1.1f), color = SessionText, fontSize = 12.sp)
        Text((session.systemCash ?: session.openingCash).asCurrency(), Modifier.weight(1.1f), color = SessionText, fontSize = 12.sp)
        Text((session.difference ?: BigDecimal.ZERO).asCurrency(), Modifier.weight(1f), color = session.difference.differenceColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.9f)) { SessionStatusBadge(session.status) }
        Box(Modifier.weight(0.45f)) {
            IconButton(onClick = { onShowDetail(session.id) }) {
                Icon(Icons.Outlined.Visibility, "Lihat detail", tint = SessionPrimary)
            }
        }
    }
    HorizontalDivider(color = SessionBorder)
}

@Composable
private fun SessionCompactCard(session: CashSession, onShowDetail: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, SessionBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(42.dp).background(SessionPrimary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.userName.orEmpty().ifBlank { "K" }.take(1).uppercase(),
                        color = SessionPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.userName ?: "Kasir",
                        color = SessionText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Dibuka ${session.openedAt.asDateTime()}", color = SessionMuted, fontSize = 11.sp)
                }
                SessionStatusBadge(session.status)
            }
            HorizontalDivider(color = SessionBorder.copy(alpha = 0.7f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SessionCompactValue("Modal awal", session.openingCash.asCompactCurrency(), Modifier.weight(1f))
                SessionCompactValue(
                    "Kas sistem",
                    (session.systemCash ?: session.openingCash).asCompactCurrency(),
                    Modifier.weight(1f)
                )
                SessionCompactValue(
                    "Selisih",
                    (session.difference ?: BigDecimal.ZERO).asCompactCurrency(),
                    Modifier.weight(1f),
                    session.difference.differenceColor()
                )
            }
            OutlinedButton(
                onClick = { onShowDetail(session.id) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionPrimary)
            ) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lihat rincian", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SessionCompactValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SessionText
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = SessionMuted, fontSize = 10.sp, maxLines = 1)
        Text(
            value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionStatusBadge(status: String) {
    val isOpen = status.equals("OPEN", true)
    val tint = if (isOpen) Color(0xFF2563EB) else SessionPrimary
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Text(status.statusLabel().uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SessionPagination(
    uiState: CashSessionHistoryUiState,
    visibleCount: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean
) {
    val start = if (visibleCount == 0) 0 else ((uiState.page - 1) * uiState.pageSize) + 1
    val end = if (visibleCount == 0) 0 else start + visibleCount - 1
    val totalText = if (uiState.searchQuery.isBlank()) {
        "${uiState.totalSessions} sesi"
    } else {
        "$visibleCount hasil pada halaman ini"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (compact) {
            Text("$start-$end dari $totalText", color = SessionMuted, fontSize = 12.sp)
        } else Column {
                Text(
                    "Menampilkan $start-$end dari $totalText",
                    color = SessionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Maksimal ${uiState.pageSize} sesi per halaman",
                    color = SessionMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SessionPageButton(onClick = onPreviousPage, enabled = uiState.page > 1, text = "<")
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(SessionPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${uiState.page}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = SessionMuted, fontWeight = FontWeight.SemiBold)
            SessionPageButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages, text = ">")
        }
    }
}

@Composable
private fun SessionPageButton(onClick: () -> Unit, enabled: Boolean, text: String) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionText)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingBox() = com.tbterminal.app.ui.components.SkeletonList(
    modifier = Modifier.fillMaxWidth().height(180.dp),
    itemCount = 4,
)

@Composable
private fun EmptyBox(message: String = "Belum ada sesi kas.") = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text(message, color = SessionMuted) }

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color(0xFFDC2626))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) = Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

private fun String.statusLabel(): String = when (this) {
    "OPEN" -> "Terbuka"
    "CLOSED" -> "Ditutup"
    else -> this
}

private fun BigDecimal?.differenceColor(): Color = when {
    this == null || compareTo(BigDecimal.ZERO) == 0 -> SessionMuted
    compareTo(BigDecimal.ZERO) < 0 -> Color(0xFFDC2626)
    else -> SessionPrimary
}

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

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

private fun CashSessionHistoryUiState.dateRangeLabel(): String? {
    val start = startDate ?: selectedDate
    val end = endDate ?: selectedDate
    if (start == null || end == null) return null
    val startText = start.asShortDate()
    val endText = end.asShortDate()
    return if (start == end) startText else "$startText - $endText"
}

private fun String.asShortDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

private fun String.asDateTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
