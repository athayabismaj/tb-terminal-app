package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton

@Composable
internal fun SupplierDebtTableCard(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false,
    onOpenMobileOverview: () -> Unit = {},
    onSupplierDetailClick: (String) -> Unit = {},
) {
    if (compact) {
        var showMobileFilters by remember { mutableStateOf(false) }
        Column(
            modifier = modifier.fillMaxWidth().testTag("supplier-debt-list-content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF1E293B)),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        "Cari supplier atau nomor nota...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
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
                    modifier = Modifier.size(40.dp).testTag("supplier-debt-open-filters")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(16.dp)
                        )
                        if (uiState.statusFilter != SupplierDebtStatusFilter.All) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(8.dp)
                                    .background(Color(0xFF256B57), CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daftar hutang",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .clickable { onOpenMobileOverview() }
                        .background(Color.White)
                        .testTag("supplier-debt-open-overview"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FactCheck,
                        contentDescription = "Ringkasan",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Cards Area
            when {
                uiState.isLoading && uiState.payables.isEmpty() -> SupplierDebtLoading(Modifier, compact = true)
                uiState.errorMessage != null && uiState.payables.isEmpty() -> SupplierDebtError(Modifier)
                uiState.filteredPayables.isEmpty() -> SupplierDebtEmpty(Modifier)
                else -> {
                    val groupedBySupplier = uiState.filteredPayables.groupBy { it.supplierId.ifBlank { it.supplierName } }
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        groupedBySupplier.forEach { (supplierId, payables) ->
                            SupplierDebtGroupCard(supplierId, payables, onPayClick, onSupplierDetailClick)
                        }
                    }

                    // Pagination (Mobile Style)
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
                            Text(
                                "${if (uiState.total == 0L) 0 else uiState.payables.size} dari ${uiState.total} nota",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = onPreviousPage,
                                    modifier = Modifier.size(28.dp).background(Color.Transparent, CircleShape),
                                    enabled = uiState.page > 1
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Sebelumnya",
                                        tint = if (uiState.page > 1) Color(0xFF334155) else Color(0xFF94A3B8).copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "${uiState.page} / ${uiState.totalPages}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                IconButton(
                                    onClick = onNextPage,
                                    modifier = Modifier.size(28.dp).background(Color.Transparent, CircleShape),
                                    enabled = uiState.page < uiState.totalPages
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Selanjutnya",
                                        tint = if (uiState.page < uiState.totalPages) Color(0xFF334155) else Color(0xFF94A3B8).copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showMobileFilters) {
            TbMobileControlSheet(
                title = "Filter hutang",
                subtitle = "Pilih status hutang supplier",
                onDismiss = { showMobileFilters = false },
                testTag = "supplier-debt-filter-sheet",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Status",
                        style = MaterialTheme.typography.labelLarge,
                        color = DebtText,
                        fontWeight = FontWeight.SemiBold
                    )
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
    } else {
        // Desktop / Tablet mode
        Column(
            modifier = modifier.fillMaxWidth().testTag("supplier-debt-list-content"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged, compact = false)

            when {
                uiState.isLoading && uiState.payables.isEmpty() -> SupplierDebtLoading(Modifier, compact = false)
                uiState.errorMessage != null && uiState.payables.isEmpty() -> SupplierDebtError(Modifier)
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Daftar hutang",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = DebtText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (uiState.filteredPayables.isEmpty()) {
                        SupplierDebtEmpty(Modifier)
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DebtSurface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, DebtLine),
                        ) {
                            Column {
                                DebtTableHeader()
                                SupplierDebtRows(Modifier.fillMaxWidth(), uiState, onPayClick, compact = false, onSupplierDetailClick = onSupplierDetailClick)
                            }
                        }
                        SupplierDebtFooter(uiState, onPreviousPage, onNextPage, compact = false)
                    }
                }
            }
        }
    }
}
