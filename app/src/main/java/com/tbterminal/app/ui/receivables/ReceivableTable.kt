package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbPagination

@Composable
internal fun ReceivableTableCard(
    onAddOpeningBalance: () -> Unit,
    onAddAdjustment: () -> Unit,
    modifier: Modifier,
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit,
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false,
) {
    if (compact) {
        var showMobileFilters by remember { mutableStateOf(false) }
        Column(
            modifier = modifier.fillMaxWidth().testTag("receivable-list-content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Top App Bar with Sesuaikan
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val navigator = com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator.current
                    IconButton(
                        onClick = { navigator?.invoke(com.tbterminal.app.ui.dashboard.admin.AdminDestination.FinanceHub) },
                        modifier = Modifier.size(36.dp).background(Color.Transparent, CircleShape)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF334155))
                    }
                    Text("Piutang Pelanggan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                
                Surface(
                    onClick = onAddAdjustment,
                    color = Color(0xFFE1EFEA),
                    shape = RoundedCornerShape(50),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Outlined.Tune, contentDescription = null, tint = Color(0xFF256B57), modifier = Modifier.size(14.dp))
                        Text("Sesuaikan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF256B57))
                    }
                }
            }
            
            // 2. Search and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f).height(40.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF1E293B)),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text("Cari pelanggan atau nomor nota...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        }
                    }
                )
                
                Surface(
                    onClick = { showMobileFilters = true },
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Outlined.Tune, contentDescription = "Filter", tint = Color(0xFF334155), modifier = Modifier.size(16.dp))
                        if (uiState.statusFilter != ReceivableStatusFilter.All || uiState.dueFilter != ReceivableDueFilter.All) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(Color(0xFF256B57), CircleShape).border(2.dp, Color.White, CircleShape))
                        }
                    }
                }
                
                Surface(
                    onClick = onAddOpeningBalance,
                    color = Color(0xFF256B57),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Outlined.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Isi Saldo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            
            // 3. Section Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar piutang", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.AssignmentTurnedIn, contentDescription = null, tint = Color(0xFF256B57), modifier = Modifier.size(16.dp))
                    Text("Ringkasan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF256B57))
                    Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF256B57), modifier = Modifier.size(14.dp))
                }
            }
            
            // 4. Cards
            when {
                uiState.isLoading && uiState.receivables.isEmpty() -> ReceivableLoading(compact = true)
                uiState.errorMessage != null && uiState.receivables.isEmpty() -> ReceivableError()
                uiState.filteredReceivables.isEmpty() -> ReceivableEmpty()
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        uiState.filteredReceivables.forEach { receivable ->
                            MobileReceivableCard(receivable, onPayClick)
                        }
                    }
                    
                    // 5. Pagination (Mobile Style)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f))
                        ) {
                            Text(" data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                        
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(onClick = onPreviousPage, modifier = Modifier.size(28.dp).background(Color.Transparent, CircleShape)) {
                                    Icon(androidx.compose.material.icons.Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                                Text("-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                IconButton(onClick = onNextPage, modifier = Modifier.size(28.dp).background(Color.Transparent, CircleShape)) {
                                    Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showMobileFilters) {
            TbMobileControlSheet(
                title = "Filter piutang",
                subtitle = "Atur status pembayaran dan jatuh tempo",
                onDismiss = { showMobileFilters = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReceivableToolbar(uiState, onSearchChanged, onStatusFilterChanged, onDueFilterChanged, compact = false)
                    TbMobileSheetDoneButton(onClick = { showMobileFilters = false })
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().testTag("receivable-list-content"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReceivableToolbar(uiState, onSearchChanged, onStatusFilterChanged, onDueFilterChanged, compact = false)

            when {
                uiState.isLoading && uiState.receivables.isEmpty() -> ReceivableLoading(compact = false)
                uiState.errorMessage != null && uiState.receivables.isEmpty() -> ReceivableError()
                else -> {
                    ReceivableSectionHeader(uiState, compact = false)
                    if (uiState.filteredReceivables.isEmpty()) {
                        ReceivableEmpty()
                    } else {
                        ReceivableTable(uiState.filteredReceivables, onPayClick)
                        ReceivableFooter(uiState, onPreviousPage, onNextPage, compact = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableToolbar(
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit,
    compact: Boolean,
) {
    var showMobileFilters by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("receivable-toolbar"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ReceivableSearchField(uiState.searchQuery, onSearchChanged, Modifier.weight(1f))
                TbMobileFilterButton(
                    onClick = { showMobileFilters = true },
                    active = uiState.statusFilter != ReceivableStatusFilter.All || uiState.dueFilter != ReceivableDueFilter.All,
                    testTag = "receivable-open-filters",
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReceivableSearchField(uiState.searchQuery, onSearchChanged, Modifier.weight(1f))
                ReceivableFilterDropdown(
                    uiState.statusFilter.label,
                    uiState.statusFilter != ReceivableStatusFilter.All,
                    ReceivableStatusFilter.entries,
                    { it.label },
                    onStatusFilterChanged,
                    "Filter status",
                    Modifier.width(210.dp),
                )
                ReceivableFilterDropdown(
                    uiState.dueFilter.compactLabel(),
                    uiState.dueFilter != ReceivableDueFilter.All,
                    ReceivableDueFilter.entries,
                    { it.label },
                    onDueFilterChanged,
                    "Filter jatuh tempo",
                    Modifier.width(230.dp),
                )
            }
        }
    }

    if (showMobileFilters) {
        TbMobileControlSheet(
            title = "Filter piutang",
            subtitle = "Atur status pembayaran dan jatuh tempo",
            onDismiss = { showMobileFilters = false },
            testTag = "receivable-filter-sheet",
        ) {
            ReceivableFilterDropdown(
                label = uiState.statusFilter.label,
                active = uiState.statusFilter != ReceivableStatusFilter.All,
                entries = ReceivableStatusFilter.entries,
                entryLabel = { it.label },
                onSelect = onStatusFilterChanged,
                contentDescription = "Filter status",
                modifier = Modifier.fillMaxWidth(),
            )
            ReceivableFilterDropdown(
                label = uiState.dueFilter.compactLabel(),
                active = uiState.dueFilter != ReceivableDueFilter.All,
                entries = ReceivableDueFilter.entries,
                entryLabel = { it.label },
                onSelect = onDueFilterChanged,
                contentDescription = "Filter jatuh tempo",
                modifier = Modifier.fillMaxWidth(),
            )
            TbMobileSheetDoneButton(
                onClick = { showMobileFilters = false },
                testTag = "receivable-filter-done",
            )
        }
    }
}

@Composable
private fun ReceivableSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(50.dp).testTag("receivable-search"),
        color = ReceivableSoft,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = ReceivableMuted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        "Cari pelanggan atau nomor nota",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReceivableMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = ReceivableText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Close, "Hapus pencarian", tint = ReceivableMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun <T> ReceivableFilterDropdown(
    label: String,
    active: Boolean,
    entries: List<T>,
    entryLabel: (T) -> String,
    onSelect: (T) -> Unit,
    contentDescription: String,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            color = if (active) ReceivablePrimary.copy(alpha = 0.12f) else ReceivableSoft,
            shape = RoundedCornerShape(22.dp),
            border = if (active) BorderStroke(1.dp, ReceivablePrimary.copy(alpha = 0.28f)) else null,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReceivableText,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription, tint = ReceivableMuted, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 190.dp, max = 280.dp).heightIn(max = 320.dp).background(ReceivableSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entryLabel(entry), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { expanded = false; onSelect(entry) },
                )
            }
        }
    }
}

@Composable
private fun ReceivableSectionHeader(uiState: ReceivableUiState, compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Daftar piutang",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = ReceivableText,
            fontWeight = FontWeight.SemiBold,
        )
        ReceivableCustomerSummaries(uiState.customerSummaries, compact = compact)
    }
}

@Composable
private fun MobileReceivableCard(receivable: Receivable, onPayClick: (Receivable) -> Unit) {
    Surface(
        onClick = { onPayClick(receivable) },
        modifier = Modifier.fillMaxWidth().testTag("receivable-"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.75f)),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Customer Top Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape).border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(receivable.customerName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                    Column {
                        Text(receivable.customerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(receivable.mobileReference(), fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                
                // Status Badge
                val isPaid = receivable.isPaid()
                Surface(
                    color = if (isPaid) Color(0xFFE1EFEA) else Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        if (isPaid) "LUNAS" else "BELUM LUNAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) Color(0xFF256B57) else Color(0xFFC2410C),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Remaining Debt Metric
            Column(modifier = Modifier.padding(top = 2.dp)) {
                Text("Sisa piutang", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Text(receivable.remainingAmount.currencyText(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.padding(top = 4.dp))
            }
            
            // Tagihan & Pembayaran Overview
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total tagihan", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(receivable.amount.currencyText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 2.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sudah dibayar", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(receivable.paidAmount.currencyText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 2.dp))
                }
            }
            
            // Due Date Status Badge
            Row(
                modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, Color(0xFFF1F5F9)), shape = RoundedCornerShape(0.dp)).padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jatuh tempo", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(receivable.dueDate.simpleDate(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                
                // Overdue status badge
                val dueColor = receivable.dueDate.dueColor()
                Surface(
                    color = dueColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, dueColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(dueColor, CircleShape))
                        Text(receivable.dueDate.dueRelativeText(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = dueColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableTable(receivables: List<Receivable>, onPayClick: (Receivable) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("receivable-table"),
        color = ReceivableSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ReceivableLine),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().background(ReceivableSoft).padding(horizontal = 18.dp, vertical = 13.dp)) {
                ReceivableTableLabel("Pelanggan", Modifier.weight(2f))
                ReceivableTableLabel("Referensi", Modifier.weight(1.35f))
                ReceivableTableLabel("Total / dibayar", Modifier.weight(1.45f), Alignment.End)
                ReceivableTableLabel("Sisa", Modifier.weight(1.25f), Alignment.End)
                ReceivableTableLabel("Jatuh tempo", Modifier.weight(1.5f))
                ReceivableTableLabel("Status", Modifier.weight(1.1f), Alignment.CenterHorizontally)
                ReceivableTableLabel("Aksi", Modifier.weight(0.8f), Alignment.End)
            }
            receivables.forEachIndexed { index, receivable ->
                if (index > 0) HorizontalDivider(color = ReceivableLine)
                ReceivableTableRow(receivable, onPayClick)
            }
        }
    }
}

@Composable
private fun ReceivableTableRow(receivable: Receivable, onPayClick: (Receivable) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receivable-${receivable.id}")
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            CustomerInitial(receivable.customerName)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(receivable.customerName, style = MaterialTheme.typography.bodyMedium, color = ReceivableText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(receivable.debtDate.simpleDate(), style = MaterialTheme.typography.labelSmall, color = ReceivableMuted)
            }
        }
        Text(receivable.mobileReference(), Modifier.weight(1.35f), style = MaterialTheme.typography.bodySmall, color = ReceivableMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Column(
            modifier = Modifier.weight(1.45f).padding(end = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(receivable.amount.currencyText(), style = MaterialTheme.typography.bodySmall, color = ReceivableText, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(receivable.paidAmount.currencyText(), style = MaterialTheme.typography.labelSmall, color = ReceivableMuted, maxLines = 1)
        }
        Text(
            receivable.remainingAmount.currencyText(),
            Modifier.weight(1.25f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = ReceivableText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Column(modifier = Modifier.weight(1.5f).padding(start = 8.dp)) {
            Text(receivable.dueDate.simpleDate(), style = MaterialTheme.typography.bodySmall, color = ReceivableText, fontWeight = FontWeight.Medium)
            Text(receivable.dueDate.dueRelativeText(), style = MaterialTheme.typography.labelSmall, color = receivable.dueDate.dueColor(), maxLines = 1)
        }
        Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.Center) { ReceivableStatusBadge(receivable.status) }
        Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
            if (receivable.isPaid()) {
                Text("Lunas", style = MaterialTheme.typography.labelMedium, color = ReceivableMuted)
            } else {
                Button(
                    onClick = { onPayClick(receivable) },
                    modifier = Modifier.height(48.dp).testTag("pay-receivable-${receivable.id}"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReceivablePrimaryDark),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Bayar", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun ReceivableTableLabel(text: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier.padding(horizontal = 6.dp), horizontalAlignment = alignment) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = ReceivableMuted, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun CustomerInitial(name: String) {
    Surface(color = ReceivableSoft, shape = CircleShape, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.initial(), color = ReceivablePrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceivableFooter(
    uiState: ReceivableUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean,
) {
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (uiState.searchQuery.isNotBlank()) "${uiState.filteredReceivables.size} hasil"
        else if (compact) "${uiState.total} data"
        else "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} piutang",
        isLoading = uiState.isLoading,
        testTag = "receivable-pagination",
    )
}

@Composable
private fun ReceivableLoading(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("receivable-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(if (compact) 4 else 5) {
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 174.dp else 62.dp))
        }
    }
}

@Composable
private fun ReceivableEmpty() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ReceivableSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ReceivableLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Piutang tidak ditemukan", style = MaterialTheme.typography.titleSmall, color = ReceivableText, fontWeight = FontWeight.SemiBold)
            Text("Coba ubah pencarian atau filter yang digunakan.", style = MaterialTheme.typography.bodySmall, color = ReceivableMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReceivableError() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ReceivableSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ReceivableLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Piutang pelanggan gagal dimuat", style = MaterialTheme.typography.titleSmall, color = ReceivableDanger, fontWeight = FontWeight.SemiBold)
            Text("Tarik layar ke bawah untuk mencoba lagi.", style = MaterialTheme.typography.bodySmall, color = ReceivableMuted)
        }
    }
}

private fun Receivable.mobileReference(): String =
    legacyInvoiceNumber?.takeIf(String::isNotBlank)
        ?: transactionId?.shortTransactionId()
        ?: source

private fun Receivable.isPaid(): Boolean = status.equals(ReceivableStatusFilter.Paid.apiValue, ignoreCase = true)

private fun ReceivableDueFilter.compactLabel(): String = when (this) {
    ReceivableDueFilter.All -> "Jatuh tempo"
    ReceivableDueFilter.Overdue -> "Terlambat"
    ReceivableDueFilter.DueToday -> "Hari ini"
    ReceivableDueFilter.Upcoming -> "Mendatang"
}




