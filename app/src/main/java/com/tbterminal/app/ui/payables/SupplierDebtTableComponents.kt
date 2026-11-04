package com.tbterminal.app.ui.payables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.outlined.Close
import com.tbterminal.app.ui.components.TbPagination

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
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun SupplierDebtTableCard(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onResetAllFilters: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onDueFilterChanged: (SupplierDebtDueFilter) -> Unit = {},
    onOpenActionMenu: () -> Unit = {},
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
        var userPage by rememberSaveable { mutableIntStateOf(1) }

        LaunchedEffect(uiState.searchQuery, uiState.statusFilter) {
            userPage = 1
        }
        Column(
            modifier = modifier.fillMaxWidth().testTag("supplier-debt-list-content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isLoading && uiState.payables.isEmpty()) {
                // Section Header Skeleton (Text di atas card)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(Modifier.width(110.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
                }

                // Cards Skeleton
                SupplierDebtLoading(Modifier, compact = true)
            } else {
                // Expandable Search Bar (When Search is open in top bar or query is present)
                AnimatedVisibility(
                    visible = isSearchExpanded || uiState.searchQuery.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
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
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearchChanged,
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth().testTag("supplier-debt-search-input")
                                )
                            }
                            if (uiState.searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable { onSearchChanged("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Hapus",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Daftar hutang",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                // Cards Area
                when {
                    uiState.errorMessage != null && uiState.payables.isEmpty() -> SupplierDebtError(Modifier)
                    uiState.filteredPayables.isEmpty() -> SupplierDebtEmpty(Modifier)
                else -> {
                    val groupedBySupplier = remember(uiState.filteredPayables) {
                        uiState.filteredPayables.groupBy { it.supplierId.ifBlank { it.supplierName } }.toList()
                    }
                    val totalUsers = groupedBySupplier.size
                    val totalUserPages = ((totalUsers + SUPPLIER_DEBT_USERS_PER_PAGE - 1) / SUPPLIER_DEBT_USERS_PER_PAGE).coerceAtLeast(1)
                    val safeUserPage = userPage.coerceIn(1, totalUserPages)
                    val pageSuppliers = remember(groupedBySupplier, safeUserPage) {
                        groupedBySupplier.drop((safeUserPage - 1) * SUPPLIER_DEBT_USERS_PER_PAGE).take(SUPPLIER_DEBT_USERS_PER_PAGE)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        pageSuppliers.forEach { (supplierId, payables) ->
                            SupplierDebtGroupCard(supplierId, payables, onPayClick, onSupplierDetailClick)
                        }
                    }

                    // Pagination (Mobile Style: 5 users per page using TbPagination)
                    val startUser = if (totalUsers == 0) 0 else ((safeUserPage - 1) * SUPPLIER_DEBT_USERS_PER_PAGE) + 1
                    val endUser = minOf(safeUserPage * SUPPLIER_DEBT_USERS_PER_PAGE, totalUsers)
                    val userRangeText = if (totalUsers == 0) {
                        "0 supplier"
                    } else if (totalUsers <= SUPPLIER_DEBT_USERS_PER_PAGE) {
                        "$totalUsers supplier"
                    } else {
                        "$startUser-$endUser dari $totalUsers supplier"
                    }
                    TbPagination(
                        currentPage = safeUserPage,
                        totalPages = totalUserPages,
                        onPreviousPage = { userPage = (safeUserPage - 1).coerceAtLeast(1) },
                        onNextPage = { userPage = (safeUserPage + 1).coerceAtMost(totalUserPages) },
                        supportingText = userRangeText,
                        compact = true,
                        testTag = "supplier-debt-mobile-pagination",
                    )
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
            if (uiState.isLoading && uiState.payables.isEmpty()) {
                // Skeleton Toolbar (Searchbar + Filter dropdown)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp))
                    SkeletonBox(Modifier.width(210.dp).height(44.dp), shape = RoundedCornerShape(12.dp))
                }

                // Skeleton Section Header (Text di atas tabel)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(Modifier.width(120.dp).height(22.dp), shape = RoundedCornerShape(4.dp))
                }

                // Skeleton Table
                SupplierDebtLoading(Modifier, compact = false)
            } else {
                SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged, compact = false)

                when {
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
}
