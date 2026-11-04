package com.tbterminal.app.ui.audit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.audit.components.AuditLogItemRow
import com.tbterminal.app.ui.audit.components.AuditPagination
import com.tbterminal.app.ui.audit.components.OperationalAuditActionDropdown
import com.tbterminal.app.ui.audit.components.OperationalAuditDateSelector
import com.tbterminal.app.ui.audit.components.OperationalAuditDatePresets
import com.tbterminal.app.ui.audit.components.OperationalAuditEmptyState
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

@Composable
fun AdminOperationalAuditScreen(
    modifier: Modifier = Modifier,
    uiState: AdminOperationalAuditUiState,
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onActionFilterChanged: (String?) -> Unit,
    onDateChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showMobileFilters by remember { mutableStateOf(false) }
    val isFiltered = uiState.selectedAction != null || uiState.selectedPreset != "today"

    BoxWithConstraints(modifier.fillMaxSize().background(TbBackground)) {
        val compact = maxWidth < 700.dp
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 16.dp else 32.dp,
                    vertical = if (compact) 14.dp else 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            item {
                if (compact) {
                    // Active filter chip row on mobile
                    if (isFiltered) {
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

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEAF3EF),
                                border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = uiState.dateRangeLabel(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF256B57)
                                    )
                                }
                            }

                            if (uiState.selectedAction != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEAF3EF),
                                    border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = uiState.selectedAction,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF256B57)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable { onActionFilterChanged(null) },
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
                                        onDatePresetSelected("today")
                                        onActionFilterChanged(null)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    AuditFilterToolbar(
                        compact = compact,
                        uiState = uiState,
                        onActionFilterChanged = onActionFilterChanged,
                        onDatePresetSelected = onDatePresetSelected,
                        onCalendarClick = { showDatePicker = true },
                        onOpenMobileFilters = { showMobileFilters = true },
                    )
                }
            }

            when {
                uiState.isLoading && uiState.logs.isEmpty() -> {
                    item { AuditLoadingState() }
                }
                uiState.error != null && uiState.logs.isEmpty() -> {
                    item { AuditErrorState(message = uiState.error, onRetry = onRetry) }
                }
                uiState.logs.isEmpty() -> {
                    item { OperationalAuditEmptyState() }
                }
                else -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Aktivitas",
                                modifier = Modifier.weight(1f),
                                color = TbText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Halaman ${uiState.currentPage}",
                                color = TbTextMuted,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    items(uiState.logs, key = { it.id }) { log ->
                        AuditLogItemRow(log = log, compact = compact)
                    }
                }
            }

            if (!uiState.isLoading && uiState.logs.isNotEmpty()) {
                item {
                    AuditPagination(
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        onPrevious = onPreviousPage,
                        onNext = onNextPage,
                    )
                }
            }
        }
    }

    if (showMobileFilters || showFilterPopup) {
        OperationalAuditFilterSheet(
            uiState = uiState,
            onActionFilterChanged = onActionFilterChanged,
            onDatePresetSelected = onDatePresetSelected,
            onOpenCalendar = { showDatePicker = true },
            onDismiss = {
                showMobileFilters = false
                onDismissFilter()
            },
        )
    }

    if (showSummaryPopup) {
        OperationalAuditSummarySheet(
            uiState = uiState,
            onDismiss = onDismissSummary,
        )
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = uiState.endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChanged(it)
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationalAuditSummarySheet(
    uiState: AdminOperationalAuditUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .testTag("audit-summary-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ringkasan Aktivitas",
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
                        text = "TOTAL LOG DITAMPILKAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${uiState.logs.size} Aktivitas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF256B57)
                    )
                }
            }

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Periode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text(uiState.dateRangeLabel(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kategori Aksi", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text(uiState.selectedAction ?: "Semua Aksi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Halaman", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text("${uiState.currentPage} dari ${uiState.totalPages}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("audit-summary-done"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF256B57),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationalAuditFilterSheet(
    uiState: AdminOperationalAuditUiState,
    onActionFilterChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempAction by remember(uiState.selectedAction) { mutableStateOf(uiState.selectedAction) }
    var tempPreset by remember(uiState.selectedPreset) { mutableStateOf(uiState.selectedPreset) }

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
                .testTag("audit-filter-sheet"),
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
                        text = "Filter Aktivitas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )
                    Text(
                        text = "Atur periode dan kategori aktivitas",
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

            OperationalAuditDateSelector(
                label = uiState.dateRangeLabel(),
                onClick = {
                    onDismiss()
                    onOpenCalendar()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OperationalAuditDatePresets(
                selectedPreset = tempPreset,
                onSelected = { tempPreset = it },
                modifier = Modifier.fillMaxWidth(),
            )
            OperationalAuditActionDropdown(
                selectedAction = tempAction,
                onActionFilterChanged = { tempAction = it },
                modifier = Modifier.fillMaxWidth(),
            )

            // Sticky footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onDatePresetSelected("today")
                        onActionFilterChanged(null)
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
                        onDatePresetSelected(tempPreset ?: "today")
                        onActionFilterChanged(tempAction)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("audit-filter-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AuditFilterToolbar(
    compact: Boolean,
    uiState: AdminOperationalAuditUiState,
    onActionFilterChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onOpenMobileFilters: () -> Unit,
) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.weight(1f).height(50.dp),
                color = TbSurface,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, TbOutline),
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        uiState.dateRangeLabel(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TbText,
                        maxLines = 1,
                    )
                }
            }
            TbMobileFilterButton(
                onClick = onOpenMobileFilters,
                active = uiState.selectedAction != null,
                testTag = "audit-open-filters",
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OperationalAuditDateSelector(
                label = uiState.dateRangeLabel(),
                onClick = onCalendarClick,
                modifier = Modifier.width(220.dp),
            )
            OperationalAuditDatePresets(
                selectedPreset = uiState.selectedPreset,
                onSelected = onDatePresetSelected,
                modifier = Modifier.width(236.dp),
            )
            Spacer(Modifier.weight(1f))
            OperationalAuditActionDropdown(
                selectedAction = uiState.selectedAction,
                onActionFilterChanged = onActionFilterChanged,
                modifier = Modifier.width(220.dp),
            )
        }
    }
}

@Composable
private fun AuditLoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("audit-initial-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(5) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun AuditErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("audit-error-card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = TbError,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TbText,
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF256B57)),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Coba Lagi")
            }
        }
    }
}
