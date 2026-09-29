package com.tbterminal.app.ui.audit

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.audit.components.AuditLogItemRow
import com.tbterminal.app.ui.audit.components.AuditPagination
import com.tbterminal.app.ui.audit.components.OperationalAuditActionDropdown
import com.tbterminal.app.ui.audit.components.OperationalAuditDateSelector
import com.tbterminal.app.ui.audit.components.OperationalAuditDatePresets
import com.tbterminal.app.ui.audit.components.OperationalAuditEmptyState
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
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
    onActionFilterChanged: (String?) -> Unit,
    onDateChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showMobileFilters by remember { mutableStateOf(false) }

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
                AuditFilterToolbar(
                    compact = compact,
                    uiState = uiState,
                    onActionFilterChanged = onActionFilterChanged,
                    onDatePresetSelected = onDatePresetSelected,
                    onCalendarClick = { showDatePicker = true },
                    onOpenMobileFilters = { showMobileFilters = true },
                )
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

    if (showMobileFilters) {
        TbMobileControlSheet(
            title = "Filter aktivitas",
            subtitle = "Atur periode dan kategori aktivitas",
            onDismiss = { showMobileFilters = false },
            testTag = "audit-filter-sheet",
        ) {
            OperationalAuditDateSelector(
                label = uiState.dateRangeLabel(),
                onClick = {
                    showMobileFilters = false
                    showDatePicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OperationalAuditDatePresets(
                selectedPreset = uiState.selectedPreset,
                onSelected = onDatePresetSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            OperationalAuditActionDropdown(
                selectedAction = uiState.selectedAction,
                onActionFilterChanged = onActionFilterChanged,
                modifier = Modifier.fillMaxWidth(),
            )
            TbMobileSheetDoneButton(
                onClick = { showMobileFilters = false },
                testTag = "audit-filter-done",
            )
        }
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
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TbSurface),
                border = BorderStroke(1.dp, TbOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(42.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.44f).height(16.dp))
                            Spacer(Modifier.height(8.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.30f).height(12.dp))
                        }
                        SkeletonBox(Modifier.width(72.dp).height(26.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.72f).height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun AuditErrorState(message: String?, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = TbError,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = message ?: "Riwayat aktivitas gagal dimuat.",
                modifier = Modifier.padding(top = 12.dp),
                color = TbText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 14.dp)) {
                Text("Coba lagi")
            }
        }
    }
}
