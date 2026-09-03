package com.tbterminal.app.ui.audit

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.audit.components.AuditLogItemRow
import com.tbterminal.app.ui.audit.components.AuditPagination
import com.tbterminal.app.ui.audit.components.OperationalAuditActionDropdown
import com.tbterminal.app.ui.audit.components.OperationalAuditDatePresets
import com.tbterminal.app.ui.audit.components.OperationalAuditEmptyState
import com.tbterminal.app.ui.audit.components.OperationalAuditHeader
import com.tbterminal.app.ui.components.HistoryDateFilter
import com.tbterminal.app.ui.components.HistoryDatePickerDialog

@Composable
fun AdminOperationalAuditScreen(
    modifier: Modifier = Modifier,
    uiState: AdminOperationalAuditUiState,
    onActionFilterChanged: (String?) -> Unit,
    onDateChanged: (String?) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize().background(Color(0xFFF4FAFD))) {
        val compact = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp)) {
        if (compact) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryDateFilter(selectedDate = uiState.endDate, onPreviousDate = onPreviousDate, onNextDate = onNextDate, onCalendarClick = { showDatePicker = true }, onClearDate = { onDateChanged(null) }, displayTextOverride = uiState.dateRangeLabel(), modifier = Modifier.fillMaxWidth())
                OperationalAuditDatePresets(selectedPreset = uiState.selectedPreset, onSelected = onDatePresetSelected, modifier = Modifier.fillMaxWidth())
            }
        } else OperationalAuditHeader {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistoryDateFilter(
                    selectedDate = uiState.endDate,
                    onPreviousDate = onPreviousDate,
                    onNextDate = onNextDate,
                    onCalendarClick = { showDatePicker = true },
                    onClearDate = { onDateChanged(null) },
                    displayTextOverride = uiState.dateRangeLabel(),
                    modifier = Modifier.width(288.dp)
                )
                OperationalAuditDatePresets(
                    selectedPreset = uiState.selectedPreset,
                    onSelected = onDatePresetSelected,
                    modifier = Modifier.width(232.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OperationalAuditActionDropdown(
                selectedAction = uiState.selectedAction,
                onActionFilterChanged = onActionFilterChanged
            )
        }

        if (uiState.isLoading && uiState.logs.isEmpty()) {
            com.tbterminal.app.ui.components.SkeletonList(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                itemCount = 6,
            )
        } else if (uiState.error != null && uiState.logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error ?: "Error", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.logs.isEmpty()) {
                        OperationalAuditEmptyState()
                        if (uiState.totalPages > 1) {
                            AuditPagination(
                                currentPage = uiState.currentPage,
                                totalPages = uiState.totalPages,
                                onPrevious = onPreviousPage,
                                onNext = onNextPage
                            )
                        }
                    } else {
                        uiState.logs.forEach { log ->
                            AuditLogItemRow(log)
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 12.dp))
                        }

                        if (uiState.totalPages > 1) {
                            AuditPagination(
                                currentPage = uiState.currentPage,
                                totalPages = uiState.totalPages,
                                onPrevious = onPreviousPage,
                                onNext = onNextPage
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = uiState.endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChanged(it)
                showDatePicker = false
            }
        )
    }
}
