package com.tbterminal.app.ui.receivables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong

import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ModalBottomSheet
import java.time.LocalDate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.text.withStyle

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.Icon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FactCheck
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun ReceivableTableCard(
    canAdjust: Boolean,
    onOpenActionMenu: () -> Unit = {},
    onAddOpeningBalance: () -> Unit = {},
    onAddAdjustment: () -> Unit = {},
    modifier: Modifier,
    uiState: ReceivableUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onResetAllFilters: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit,
    onSortOrderChanged: (ReceivableSortOrder) -> Unit = {},
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false,
    onCustomerDetailClick: (String) -> Unit = {},
    onQuickBillCustomer: (String) -> Unit = {},
) {
    if (compact) {
        var userPage by rememberSaveable { mutableIntStateOf(1) }

        LaunchedEffect(uiState.searchQuery, uiState.statusFilter, uiState.dueFilter) {
            userPage = 1
        }
        Column(
            modifier = modifier.fillMaxWidth().testTag("receivable-list-content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReceivableHeaderToolbar(
                uiState = uiState,
                compact = compact,
                onOpenActionMenu = onOpenActionMenu,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                onSearchChanged = onSearchChanged,
                onClearStatusFilter = { onStatusFilterChanged(ReceivableStatusFilter.All) },
                onClearDueFilter = { onDueFilterChanged(ReceivableDueFilter.All) },
                onClearSortFilter = { onSortOrderChanged(ReceivableSortOrder.NearestDueDate) },
                onResetAllFilters = onResetAllFilters
            )

            if (uiState.isLoading && uiState.receivables.isEmpty()) {
                ReceivableLoading(compact = true)
            } else {
                when {
                    uiState.errorMessage != null && uiState.receivables.isEmpty() -> ReceivableError()
                    uiState.filteredReceivables.isEmpty() -> ReceivableEmpty(uiState.searchQuery)
                    else -> {
                        val groupedByCustomer = remember(uiState.filteredReceivables) {
                            uiState.filteredReceivables.groupBy { it.customerId }.toList()
                        }
                        val totalUsers = groupedByCustomer.size
                        val totalUserPages = ((totalUsers + RECEIVABLE_USERS_PER_PAGE - 1) / RECEIVABLE_USERS_PER_PAGE).coerceAtLeast(1)
                        val safeUserPage = userPage.coerceIn(1, totalUserPages)
                        val pageCustomers = remember(groupedByCustomer, safeUserPage) {
                            groupedByCustomer.drop((safeUserPage - 1) * RECEIVABLE_USERS_PER_PAGE).take(RECEIVABLE_USERS_PER_PAGE)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            pageCustomers.forEach { (customerId, customerReceivables) ->
                                CustomerReceivableGroupCard(
                                    customerId = customerId,
                                    receivables = customerReceivables,
                                    onPayClick = onPayClick,
                                    onOpenLedger = { id, _ -> onCustomerDetailClick(id) },
                                    onQuickBill = { onQuickBillCustomer(customerId) },
                                    defaultExpanded = false
                                )
                            }
                        }
                        
                        // Mobile Pagination
                        val startUser = if (totalUsers == 0) 0 else ((safeUserPage - 1) * RECEIVABLE_USERS_PER_PAGE) + 1
                        val endUser = minOf(safeUserPage * RECEIVABLE_USERS_PER_PAGE, totalUsers)
                        val userRangeText = if (totalUsers == 0) {
                            "0 pelanggan"
                        } else if (totalUsers <= RECEIVABLE_USERS_PER_PAGE) {
                            "$totalUsers pelanggan"
                        } else {
                            "$startUser-$endUser dari $totalUsers pelanggan"
                        }
                        TbPagination(
                            currentPage = safeUserPage,
                            totalPages = totalUserPages,
                            onPreviousPage = { userPage = (safeUserPage - 1).coerceAtLeast(1) },
                            onNextPage = { userPage = (safeUserPage + 1).coerceAtMost(totalUserPages) },
                            supportingText = userRangeText,
                            testTag = "receivable-user-pagination"
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().testTag("receivable-list-content"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReceivableHeaderToolbar(
                uiState = uiState,
                compact = compact,
                onOpenActionMenu = onOpenActionMenu,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                onSearchChanged = onSearchChanged,
                onClearStatusFilter = { onStatusFilterChanged(ReceivableStatusFilter.All) },
                onClearDueFilter = { onDueFilterChanged(ReceivableDueFilter.All) },
                onClearSortFilter = { onSortOrderChanged(ReceivableSortOrder.NearestDueDate) },
                onResetAllFilters = onResetAllFilters
            )

            if (uiState.isLoading && uiState.receivables.isEmpty()) {
                ReceivableLoading(compact = false)
            } else {
                when {
                    uiState.errorMessage != null && uiState.receivables.isEmpty() -> ReceivableError()
                    uiState.filteredReceivables.isEmpty() -> ReceivableEmpty(uiState.searchQuery)
                    else -> {
                        ReceivableTable(uiState.filteredReceivables, onPayClick)

                        // Desktop Pagination
                        val desktopText = if (uiState.total == 0L) {
                            "Menampilkan 0 piutang"
                        } else {
                            "Menampilkan ${uiState.currentStart}–${uiState.currentEnd} dari ${uiState.total} piutang"
                        }
                        TbPagination(
                            currentPage = uiState.page,
                            totalPages = uiState.totalPages,
                            onPreviousPage = onPreviousPage,
                            onNextPage = onNextPage,
                            supportingText = desktopText,
                            isLoading = uiState.isLoading,
                            testTag = "receivable-pagination"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableHeaderToolbar(
    uiState: ReceivableUiState,
    compact: Boolean,
    onOpenActionMenu: () -> Unit,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onClearStatusFilter: () -> Unit,
    onClearDueFilter: () -> Unit,
    onClearSortFilter: () -> Unit = {},
    onResetAllFilters: () -> Unit
) {
    val hasStatusFilter = uiState.statusFilter != ReceivableStatusFilter.All
    val hasDueFilter = uiState.dueFilter != ReceivableDueFilter.All
    val hasSortFilter = uiState.sortOrder != ReceivableSortOrder.NearestDueDate
    val isFiltered = hasStatusFilter || hasDueFilter || hasSortFilter
    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Expandable Search Bar (When Search is open or query is present)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.5f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF256B57),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                "Cari pelanggan atau nomor nota...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChanged,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receivable-search"),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                onSearchChanged("")
                            } else {
                                onToggleSearch()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Tutup pencarian",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // 2. Active Filters Row (jika filter aktif)
        if (isFiltered) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Filter:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                if (hasStatusFilter) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Receipt, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Status: ${uiState.statusFilter.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearStatusFilter),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                if (hasDueFilter) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Jatuh Tempo: ${uiState.dueFilter.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearDueFilter),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                if (hasSortFilter) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.FilterList, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Urutkan: ${uiState.sortOrder.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearSortFilter),
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
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onResetAllFilters)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        // 3. Title & Action Row (selalu tampil)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Piutang",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            // Action Button "Kelola"
            Surface(
                onClick = onOpenActionMenu,
                modifier = Modifier
                    .height(32.dp)
                    .testTag("receivable-manage-btn"),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                contentColor = Color(0xFF475569),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Kelola",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
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
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape).border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(receivable.customerName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = receivable.customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = receivable.mobileReference(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                // Status Badge
                val isPaid = receivable.isPaid()
                Surface(
                    color = if (isPaid) Color(0xFFE1EFEA) else Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isPaid) "LUNAS" else "BELUM LUNAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) Color(0xFF256B57) else Color(0xFFC2410C),
                        maxLines = 1,
                        softWrap = false,
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
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("receivable-skeleton"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(3) {
                CustomerReceivableGroupCardSkeleton()
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth().testTag("receivable-skeleton")) {
            ReceivableTableDesktopSkeleton()
        }
    }
}

@Composable
private fun CustomerReceivableGroupCardSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonBox(Modifier.size(44.dp), shape = CircleShape)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(0.55f).height(15.dp), shape = RoundedCornerShape(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(Modifier.width(90.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                            SkeletonBox(Modifier.width(60.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(Modifier.width(46.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.size(20.dp), shape = CircleShape)
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(Modifier.width(100.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.width(50.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(70.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(100.dp).height(18.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(45.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(65.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(80.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(90.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(80.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(50.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableTableDesktopSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            repeat(5) { index ->
                if (index > 0) HorizontalDivider(color = ReceivableLine)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(34.dp), shape = CircleShape)
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonBox(Modifier.width(110.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                            SkeletonBox(Modifier.width(75.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                        }
                    }
                    Box(Modifier.weight(1.35f)) {
                        SkeletonBox(Modifier.width(85.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(Modifier.weight(1.45f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SkeletonBox(Modifier.width(90.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                        SkeletonBox(Modifier.width(65.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                    }
                    Box(Modifier.weight(1.25f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(80.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1.5f)) {
                        SkeletonBox(Modifier.width(85.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                        SkeletonBox(Modifier.width(62.dp).height(22.dp), shape = RoundedCornerShape(6.dp))
                    }
                    Box(Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
                        SkeletonBox(Modifier.width(64.dp).height(28.dp), shape = RoundedCornerShape(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableEmpty(searchQuery: String = "") {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (searchQuery.isNotBlank()) Icons.Outlined.Search else Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = if (searchQuery.isNotBlank()) "Piutang tidak ditemukan" else "Belum ada data piutang",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = if (searchQuery.isNotBlank()) "Tidak ada data piutang yang cocok dengan \"$searchQuery\"" else "Data piutang pelanggan akan tercatat di sini saat transaksi tempo atau saldo awal dibuat.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
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







@Composable
private fun CustomerReceivableGroupCard(
    customerId: String,
    receivables: List<Receivable>,
    onPayClick: (Receivable) -> Unit,
    onOpenLedger: (String, String) -> Unit,
    onQuickBill: () -> Unit = {},
    defaultExpanded: Boolean = false,
) {
    val customerName = receivables.firstOrNull()?.customerName ?: "Unknown"
    val totalRemaining = receivables.sumOf { it.remainingAmount }
    val allPaid = totalRemaining <= java.math.BigDecimal.ZERO
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    
    val slate100 = Color(0xFFF1F5F9)
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate700 = Color(0xFF334155)
    val slate900 = Color(0xFF0F172A)
    
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "customerChevronRotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer-group-$customerId"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, slate200),
        shadowElevation = 0.5.dp
    ) {
        Column {
            // Header Accordion
            Surface(
                onClick = { isExpanded = !isExpanded },
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Customer Avatar Pill (40.dp x 40.dp, bg #F1F5F9, border #E2E8F0, text #334155)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(slate100, CircleShape)
                                .border(1.dp, slate200, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customerName.take(1).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = slate700
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = customerName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = slate100,
                                ) {
                                    Text(
                                        text = "Umum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = slate500,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = totalRemaining.currencyText(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = slate900,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (allPaid) "(Semua lunas)" else "(Belum lunas)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = slate400,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    // Header Badge & Chevron Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = slate400,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${receivables.size} Nota",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = slate500,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Tutup nota" else "Buka nota",
                            tint = slate400,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationState)
                        )
                    }
                }
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = slate100)
                    
                    val chronological = remember(receivables) {
                        receivables.sortedWith(
                            compareBy<Receivable> { it.debtDate }
                                .thenBy { it.createdAt }
                                .thenBy { it.id }
                        )
                    }
                    val sortedReceivables = remember(receivables) {
                        receivables.sortedWith(
                            compareByDescending<Receivable> { it.debtDate }
                                .thenByDescending { it.createdAt }
                                .thenByDescending { it.id }
                        )
                    }

                    val limit = 3
                    val itemsToDisplay = sortedReceivables.take(limit)
                    val remainingCount = sortedReceivables.size - limit
                    
                    itemsToDisplay.forEachIndexed { index, receivable ->
                        if (index > 0) {
                            HorizontalDivider(color = slate100)
                        }
                        val cardinalNumber = chronological.indexOfFirst { it.id == receivable.id } + 1
                        MobileReceivableAccordionItem(
                            receivable = receivable,
                            cardinalNumber = cardinalNumber,
                            onPayClick = onPayClick,
                            onOpenLedger = onOpenLedger
                        )
                    }
                    
                    // Show "Detail Riwayat Piutang" and "Tagih WhatsApp" Footer
                    HorizontalDivider(color = slate100)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!allPaid) {
                            Surface(
                                onClick = onQuickBill,
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF15803D),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Tagih WhatsApp",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { onOpenLedger(customerId, customerName) },
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, slate200),
                            modifier = Modifier
                                .weight(if (!allPaid) 1f else 2f)
                                .height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val footerText = if (remainingCount > 0) "Riwayat ($remainingCount+)" else "Riwayat Piutang"
                                Text(
                                    text = footerText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = slate700
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = slate700,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileReceivableAccordionItem(
    receivable: Receivable,
    cardinalNumber: Int,
    onPayClick: (Receivable) -> Unit,
    onOpenLedger: (String, String) -> Unit
) {
    val isPaid = receivable.isPaid()
    val isPartial = !isPaid && (receivable.paidAmount > java.math.BigDecimal.ZERO || receivable.status.equals("sebagian", true) || receivable.status.equals("partial", true))

    val isOverdue = remember(receivable.dueDate) {
        val date = runCatching { java.time.LocalDate.parse(receivable.dueDate.take(10)) }.getOrNull()
        if (date != null) {
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), date) < 0
        } else false
    }

    val slate50 = Color(0xFFF8FAFC)
    val slate100 = Color(0xFFF1F5F9)
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate600 = Color(0xFF475569)
    val slate700 = Color(0xFF334155)
    val slate800 = Color(0xFF1E293B)
    val slate900 = Color(0xFF0F172A)
    val rose500 = Color(0xFFF43F5E)
    val rose600 = Color(0xFFE11D48)
    val amber500 = Color(0xFFF59E0B)
    val amber700 = Color(0xFFB45309)
    val emerald500 = Color(0xFF10B981)
    val emerald700 = Color(0xFF047857)

    val itemBg = if (isPaid) slate50.copy(alpha = 0.7f) else Color.White

    Surface(
        color = itemBg,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receivable-item-${receivable.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Line 1: Invoice ID & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = receivable.mobileReference(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) slate700 else slate900,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "Nota $cardinalNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = slate400
                    )
                }

                val (badgeDotColor, badgeTextColor, badgeLabel) = when {
                    isPaid -> Triple(emerald500, emerald700, "Terbayar Penuh")
                    isPartial -> Triple(amber500, amber700, "Sebagian")
                    else -> Triple(rose500, rose600, "Belum Bayar")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(badgeDotColor, CircleShape)
                    )
                    Text(
                        text = badgeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = badgeTextColor
                    )
                }
            }

            // Line 2: Grid 2 Cols (Sisa Piutang & Total Tagihan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Col 1: Sisa Piutang
                Column {
                    Text(
                        text = "Sisa Piutang",
                        fontSize = 11.sp,
                        color = slate500
                    )
                    Text(
                        text = receivable.remainingAmount.currencyText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) slate700 else slate900,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Col 2: Total Tagihan
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Tagihan",
                        fontSize = 11.sp,
                        color = slate500
                    )
                    if (isPaid) {
                        Text(
                            text = receivable.amount.currencyText(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = slate600,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = receivable.amount.currencyText(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = slate800
                            )
                            Text(
                                text = " / ${receivable.paidAmount.currencyText()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = slate400
                            )
                        }
                    }
                }
            }

            // Line 3: Due date / Lunas & Action button
            HorizontalDivider(color = if (isPaid) slate200.copy(alpha = 0.6f) else slate100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Due Date or Lunas Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isPaid) emerald500 else if (isOverdue) rose500 else amber500, CircleShape)
                    )
                    if (isPaid) {
                        Text(
                            text = "Lunas: ${receivable.dueDate.simpleDate()}",
                            fontSize = 11.sp,
                            color = slate500
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Jatuh tempo: ",
                                fontSize = 11.sp,
                                color = slate500
                            )
                            Text(
                                text = receivable.dueDate.simpleDate(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = slate700
                            )
                        }
                    }
                }

                // Right: Action
                if (!isPaid) {
                    Surface(
                        onClick = { onPayClick(receivable) },
                        shape = RoundedCornerShape(12.dp),
                        color = slate900,
                        contentColor = Color.White,
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Bayar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Lihat Bukti →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = slate400,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onOpenLedger(receivable.customerId, receivable.customerName) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivableFilterBottomSheet(
    uiState: ReceivableUiState,
    onDismiss: () -> Unit,
    onApplyStatusFilter: (ReceivableStatusFilter) -> Unit,
    onApplyDueFilter: (ReceivableDueFilter) -> Unit = {},
    onApplySortOrder: (ReceivableSortOrder) -> Unit = {}
) {
    var selectedStatus by remember { mutableStateOf(uiState.statusFilter) }
    var selectedDue by remember { mutableStateOf(uiState.dueFilter) }
    var selectedSortOrder by remember { mutableStateOf(uiState.sortOrder) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Filter Piutang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur status pelunasan dan filter piutang pelanggan",
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
                        Icons.Outlined.Close,
                        contentDescription = "Tutup Filter",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Scrollable Content Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 1. Section: STATUS PELUNASAN (Grid 2x2 with white bg & green border on active)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "STATUS PELUNASAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    val statusPairs = listOf(
                        listOf(ReceivableStatusFilter.All to "Semua", ReceivableStatusFilter.Unpaid to "Belum Lunas"),
                        listOf(ReceivableStatusFilter.Partial to "Sebagian", ReceivableStatusFilter.Paid to "Lunas")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        statusPairs.forEach { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPair.forEach { (status, label) ->
                                    val isSelected = selectedStatus == status
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clickable { selectedStatus = status },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)),
                                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                                                maxLines = 1
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF256B57),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Section: JATUH TEMPO (Aging Category: Grid 2x2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "JATUH TEMPO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    val duePairs = listOf(
                        listOf(ReceivableDueFilter.All to "Semua", ReceivableDueFilter.Overdue to "Lewat Jatuh Tempo"),
                        listOf(ReceivableDueFilter.DueToday to "Hari Ini", ReceivableDueFilter.Upcoming to "Akan Datang")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        duePairs.forEach { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPair.forEach { (filter, label) ->
                                    val isSelected = selectedDue == filter
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clickable { selectedDue = filter },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)),
                                        shadowElevation = if (isSelected) 0.5.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                                                maxLines = 1
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF256B57),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Section: URUTKAN BERDASARKAN (2 on row 1, 1 full-width on row 2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "URUTKAN BERDASARKAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    // Row 1: Tempo Terdekat & Nominal Terbesar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tempo Terdekat
                        val isDueDateSelected = selectedSortOrder == ReceivableSortOrder.NearestDueDate
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { selectedSortOrder = ReceivableSortOrder.NearestDueDate },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (isDueDateSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)),
                            shadowElevation = if (isDueDateSelected) 0.5.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = if (isDueDateSelected) Color(0xFF256B57) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Tempo Terdekat",
                                    fontSize = 12.sp,
                                    fontWeight = if (isDueDateSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isDueDateSelected) Color(0xFF256B57) else Color(0xFF475569)
                                )
                            }
                        }

                        // Nominal Terbesar
                        val isAmountSelected = selectedSortOrder == ReceivableSortOrder.HighestAmount
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { selectedSortOrder = ReceivableSortOrder.HighestAmount },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (isAmountSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)),
                            shadowElevation = if (isAmountSelected) 0.5.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isAmountSelected) Color(0xFF256B57) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Nominal Terbesar",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAmountSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isAmountSelected) Color(0xFF256B57) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Row 2 (col-span-2): Nama Pelanggan (A–Z)
                    val isNameSelected = selectedSortOrder == ReceivableSortOrder.CustomerNameAsc
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { selectedSortOrder = ReceivableSortOrder.CustomerNameAsc },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, if (isNameSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)),
                        shadowElevation = if (isNameSelected) 0.5.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                tint = if (isNameSelected) Color(0xFF256B57) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Nama Pelanggan (A–Z)",
                                fontSize = 12.sp,
                                fontWeight = if (isNameSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isNameSelected) Color(0xFF256B57) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Footer Actions: Reset (Green Outlined) & Terapkan (Green Filled)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button: border border-[#256B57] text-[#256B57] font-semibold
                Surface(
                    onClick = {
                        selectedStatus = ReceivableStatusFilter.All
                        selectedDue = ReceivableDueFilter.All
                        selectedSortOrder = ReceivableSortOrder.NearestDueDate
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF256B57))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF256B57)
                        )
                    }
                }

                // Terapkan Button: bg-brand text-white font-semibold
                Button(
                    onClick = {
                        onApplyStatusFilter(selectedStatus)
                        onApplyDueFilter(selectedDue)
                        onApplySortOrder(selectedSortOrder)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = "Terapkan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivableSummaryBottomSheet(
    uiState: ReceivableUiState,
    onDismiss: () -> Unit
) {
    val totalRemaining = uiState.pageRemainingTotal
    val totalDebtors = remember(uiState.filteredReceivables) {
        uiState.filteredReceivables.map { it.customerId }.distinct().size
    }
    val unpaidInvoices = remember(uiState.filteredReceivables) {
        uiState.filteredReceivables.count { it.status != ReceivableStatusFilter.Paid.apiValue }
    }
    val overdueInvoices = remember(uiState.filteredReceivables) {
        val today = LocalDate.now()
        uiState.filteredReceivables.count { r ->
            val due = runCatching { LocalDate.parse(r.dueDate.take(10)) }.getOrNull()
            due != null && due.isBefore(today) && r.status != ReceivableStatusFilter.Paid.apiValue
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFE2E8F0), CircleShape))
            }
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ringkasan Piutang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Informasi ringkas total piutang dan debitur",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan modal",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Content Metrics List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                // Row 1: Total Sisa Piutang (Featured Highlight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Total Sisa Piutang",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Sesuai filter halaman aktif",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        text = totalRemaining.currencyText(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF256B57),
                        letterSpacing = (-0.5).sp
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Row 2: Pelanggan Berpiutang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Pelanggan Berpiutang",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Jumlah debitur aktif",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = totalDebtors.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Orang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Row 3: Tagihan Belum Lunas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Tagihan Belum Lunas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Nota yang belum selesai",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = unpaidInvoices.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Nota",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Row 4: Lewat Jatuh Tempo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Lewat Jatuh Tempo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Perlu segera ditagih",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = overdueInvoices.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overdueInvoices > 0) Color(0xFFDC2626) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Nota",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (overdueInvoices > 0) Color(0xFFDC2626) else Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }

            // Action Button Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Selesai",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivableActionBottomSheet(
    canAdjust: Boolean,
    onDismiss: () -> Unit,
    onAddOpeningBalance: () -> Unit,
    onAddAdjustment: () -> Unit,
    onOpenSummary: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Kelola Piutang",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Pilih tindakan atau transaksi piutang pelanggan",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup menu",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = Color(0xFFF1F5F9),
                thickness = 1.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action Item 1: Input Saldo Awal Piutang
                ActionShortcutCard(
                    title = "Input Saldo Awal Piutang",
                    description = "Catat saldo utang lama pelanggan sebelum migrasi sistem toko",
                    icon = Icons.Outlined.AddCircleOutline,
                    iconBg = Color(0xFFF0F7F5),
                    iconBorder = Color(0xFFE1EFEA).copy(alpha = 0.8f),
                    iconTint = Color(0xFF256B57),
                    badge = null,
                    onClick = onAddOpeningBalance,
                    testTag = "action-opening-balance"
                )

                // Action Item 2: Penyesuaian Piutang (Adjustment)
                if (canAdjust) {
                    ActionShortcutCard(
                        title = "Penyesuaian Piutang (Adjustment)",
                        description = "Koreksi selisih saldo, diskon khusus tempo, atau hapus buku piutang macet",
                        icon = Icons.Outlined.Tune,
                        iconBg = Color(0xFFF0F7F5),
                        iconBorder = Color(0xFFE1EFEA).copy(alpha = 0.8f),
                        iconTint = Color(0xFF256B57),
                        badge = "Owner / Admin",
                        onClick = onAddAdjustment,
                        testTag = "action-adjustment"
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionShortcutCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconBorder: Color = Color.Transparent,
    iconTint: Color,
    badge: String?,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg)
                        .then(
                            if (iconBorder != Color.Transparent) {
                                Modifier.border(1.dp, iconBorder, RoundedCornerShape(12.dp))
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        lineHeight = 19.sp
                    )

                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = badge,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
