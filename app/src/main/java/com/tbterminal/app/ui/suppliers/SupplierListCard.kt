package com.tbterminal.app.ui.suppliers

import com.tbterminal.app.ui.components.TbPagination

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
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun SupplierListCard(
    modifier: Modifier,
    uiState: SupplierUiState,
    compact: Boolean,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierStatusFilter) -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenSummary: () -> Unit = {},
) {
    val isFiltered = uiState.statusFilter != SupplierStatusFilter.ALL
    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        if (compact) {
            // Expandable Search Bar on Compact Screen
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("supplier-search-expandable"),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.5f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF256B57),
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChanged,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("supplier-search-input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari supplier...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Filter Chips on Compact Screen
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
                                text = when (uiState.statusFilter) {
                                    SupplierStatusFilter.ACTIVE -> "Aktif"
                                    SupplierStatusFilter.INACTIVE -> "Nonaktif"
                                    else -> ""
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF256B57)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onStatusFilterChanged(SupplierStatusFilter.ALL) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
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
                            .clickable { onStatusFilterChanged(SupplierStatusFilter.ALL) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            SupplierToolbar(
                uiState = uiState,
                compact = compact,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
            )
        }

        SupplierSectionHeader(
            onAdd = onAdd,
        )

        when {
            uiState.isLoading && uiState.suppliers.isEmpty() -> SupplierLoadingState()
            uiState.visibleSuppliers.isEmpty() -> SupplierEmptyState(query = uiState.searchQuery)
            compact -> SupplierMobileList(uiState.visibleSuppliers, onEdit, onDelete)
            else -> SupplierTable(uiState.visibleSuppliers, onEdit, onDelete)
        }

        if (!uiState.isLoading && uiState.visibleSuppliers.isNotEmpty()) {
            SupplierPagination(
                uiState = uiState,
                compact = compact,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
            )
        }
    }
}

@Composable
private fun SupplierToolbar(
    uiState: SupplierUiState,
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierStatusFilter) -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().testTag("supplier-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Search bar
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(if (compact) 42.dp else 48.dp)
                .testTag("supplier-search"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SupplierSlate300),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = SupplierSlate400,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = SupplierSlate900,
                        fontWeight = FontWeight.Normal,
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari supplier",
                                    fontSize = 13.sp,
                                    color = SupplierSlate400,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChanged("") },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus pencarian",
                            tint = SupplierSlate400,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Filter button
        val hasActiveFilter = uiState.statusFilter != SupplierStatusFilter.ALL
        Surface(
            modifier = Modifier
                .size(if (compact) 42.dp else 48.dp)
                .testTag("supplier-open-filter")
                .clickable { showFilters = true },
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (hasActiveFilter) Color(0xFF0F172A) else SupplierSlate300),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter supplier",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp),
                )
                if (hasActiveFilter) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(7.dp)
                            .background(Color(0xFFF59E0B), CircleShape),
                    )
                }
            }
        }
    }

    if (showFilters) {
        SupplierFilterSheet(
            statusFilter = uiState.statusFilter,
            onApplyFilter = onStatusFilterChanged,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SupplierSectionHeader(
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Daftar supplier",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SupplierSlate900,
        )

        Surface(
            onClick = onAdd,
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF256B57),
            contentColor = Color.White,
            shadowElevation = 0.5.dp,
            modifier = Modifier.testTag("supplier-add"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah supplier",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Tambah",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SupplierPagination(
    uiState: SupplierUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val totalCount = uiState.visibleSuppliers.size.toLong()
    val start = if (totalCount == 0L) 0 else (uiState.page - 1L) * uiState.pageSize + 1
    val end = minOf(uiState.page.toLong() * uiState.pageSize, if (uiState.statusFilter == SupplierStatusFilter.ALL) uiState.totalSuppliers else totalCount)
    val totalDisplay = if (uiState.statusFilter == SupplierStatusFilter.ALL) uiState.totalSuppliers else totalCount
    val supportingText = "$start–$end dari $totalDisplay supplier"

    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = supportingText,
        isLoading = uiState.isLoading,
        compact = compact,
        testTag = "supplier-pagination",
    )
}
