package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton

@Composable
internal fun SupplierDebtToolbar(
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    compact: Boolean = false,
) {
    var showMobileFilters by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().testTag("supplier-debt-toolbar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SupplierDebtSearch(uiState.searchQuery, onSearchChanged, compact, Modifier.weight(1f))
        Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
        if (compact) {
            TbMobileFilterButton(
                onClick = { showMobileFilters = true },
                active = uiState.statusFilter != SupplierDebtStatusFilter.All,
                testTag = "supplier-debt-open-filters",
            )
        } else {
            StatusFilterButton(
                selected = uiState.statusFilter,
                onSelect = onStatusFilterChanged,
                compact = false,
                modifier = Modifier.width(210.dp),
            )
        }
    }

    if (showMobileFilters) {
        TbMobileControlSheet(
            title = "Filter hutang",
            subtitle = "Pilih status hutang supplier",
            onDismiss = { showMobileFilters = false },
            testTag = "supplier-debt-filter-sheet",
        ) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.labelLarge, color = DebtText, fontWeight = FontWeight.SemiBold)
                StatusFilterButton(
                    selected = uiState.statusFilter,
                    onSelect = onStatusFilterChanged,
                    compact = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TbMobileSheetDoneButton(
                onClick = { showMobileFilters = false },
                testTag = "supplier-debt-filter-done",
            )
        }
    }
}

@Composable
private fun SupplierDebtSearch(value: String, onValueChange: (String) -> Unit, compact: Boolean, modifier: Modifier) {
    Surface(
        modifier = modifier.height(50.dp).testTag("supplier-debt-search"),
        color = DebtSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DebtLine),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = DebtMuted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        if (compact) "Cari supplier" else "Cari supplier atau nomor pembelian",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DebtMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = DebtText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Hapus pencarian", tint = DebtMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusFilterButton(
    selected: SupplierDebtStatusFilter,
    onSelect: (SupplierDebtStatusFilter) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = selected != SupplierDebtStatusFilter.All
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("supplier-debt-status-filter"),
            color = if (active) DebtPrimary.copy(alpha = 0.1f) else DebtSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (active) DebtPrimary.copy(alpha = 0.3f) else DebtLine),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selected == SupplierDebtStatusFilter.All) {
                        if (compact) "Semua" else "Semua status"
                    } else selected.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DebtText,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription = "Filter status", tint = DebtMuted, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 190.dp, max = 260.dp).background(DebtSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            SupplierDebtStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = { expanded = false; onSelect(filter) },
                )
            }
        }
    }
}
