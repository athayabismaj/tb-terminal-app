package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.components.TopFloatingCapsule
import java.time.LocalDate

@Composable
internal fun SupplierDebtScreen(
    modifier: Modifier = Modifier,
    uiState: SupplierDebtUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onOpenSummary: () -> Unit = {},
    onDismissSummary: () -> Unit = {},
    onResetAllFilters: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onDueFilterChanged: (SupplierDebtDueFilter) -> Unit = {},
    onSortOrderChanged: (SupplierDebtSortOrder) -> Unit = {},
    onAddOpeningBalance: () -> Unit = {},
    onAddAdjustment: () -> Unit = {},
    onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit,
    onSupplierDetailClick: (String) -> Unit = {}
) {
    var showActionBottomSheet by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(DebtBackground)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            if (!compact && (uiState.payables.isNotEmpty() || (!uiState.isLoading && uiState.errorMessage == null))) {
                SupplierDebtMetrics(uiState, compact = false)
            }
            SupplierDebtTableCard(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                onResetAllFilters = onResetAllFilters,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onDueFilterChanged = onDueFilterChanged,
                onOpenActionMenu = { showActionBottomSheet = true },
                onRefresh = onRefresh,
                onPayClick = onPayClick,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact,
                onSupplierDetailClick = onSupplierDetailClick,
            )
        }

        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }

    if (showFilterPopup) {
        SupplierDebtFilterBottomSheet(
            uiState = uiState,
            onDismiss = onDismissFilter,
            onApplyStatusFilter = onStatusFilterChanged,
            onApplyDueFilter = onDueFilterChanged,
            onApplySortOrder = onSortOrderChanged
        )
    }

    if (showActionBottomSheet) {
        SupplierDebtActionBottomSheet(
            canAdjust = true,
            onDismiss = { showActionBottomSheet = false },
            onAddOpeningBalance = {
                showActionBottomSheet = false
                onAddOpeningBalance()
            },
            onAddAdjustment = {
                showActionBottomSheet = false
                onAddAdjustment()
            },
            onOpenSummary = {
                showActionBottomSheet = false
                onOpenSummary()
            }
        )
    }

    if (showSummaryPopup) {
        SupplierDebtSummaryBottomSheet(
            uiState = uiState,
            onDismiss = onDismissSummary
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SupplierDebtActionBottomSheet(
    canAdjust: Boolean = true,
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
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(6.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Kelola Hutang",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Pilih tindakan atau transaksi hutang supplier",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup menu",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
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
                // Action Item 1: Input Saldo Awal Hutang Supplier
                SupplierActionShortcutCard(
                    title = "Input Saldo Awal Hutang",
                    description = "Catat saldo tagihan lama distributor sebelum migrasi sistem toko",
                    icon = Icons.Outlined.AddCircleOutline,
                    iconBg = Color(0xFFF0F7F5),
                    iconBorder = Color(0xFFE1EFEA).copy(alpha = 0.8f),
                    iconTint = Color(0xFF256B57),
                    badge = null,
                    onClick = onAddOpeningBalance,
                    testTag = "supplier-action-opening-balance"
                )

                // Action Item 2: Penyesuaian Hutang (Adjustment)
                if (canAdjust) {
                    SupplierActionShortcutCard(
                        title = "Penyesuaian Hutang (Adjustment)",
                        description = "Koreksi selisih faktur, diskon khusus tempo, atau potongan retur supplier",
                        icon = Icons.Outlined.Tune,
                        iconBg = Color(0xFFF0F7F5),
                        iconBorder = Color(0xFFE1EFEA).copy(alpha = 0.8f),
                        iconTint = Color(0xFF256B57),
                        badge = "Owner / Admin",
                        onClick = onAddAdjustment,
                        testTag = "supplier-action-adjustment"
                    )
                }


            }
        }
    }
}

@Composable
private fun SupplierActionShortcutCard(
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
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        color = Color(0xFF0F172A),
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
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SupplierDebtFilterBottomSheet(
    uiState: SupplierDebtUiState,
    onDismiss: () -> Unit,
    onApplyStatusFilter: (SupplierDebtStatusFilter) -> Unit,
    onApplyDueFilter: (SupplierDebtDueFilter) -> Unit = {},
    onApplySortOrder: (SupplierDebtSortOrder) -> Unit = {}
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
            SupplierDebtFilterHeader(onDismiss = onDismiss)

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SupplierDebtStatusFilterSection(
                    selectedStatus = selectedStatus,
                    onSelect = { selectedStatus = it }
                )

                SupplierDebtDueFilterSection(
                    selectedDue = selectedDue,
                    onSelect = { selectedDue = it }
                )

                SupplierDebtSortOrderSection(
                    selectedSortOrder = selectedSortOrder,
                    onSelect = { selectedSortOrder = it }
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            SupplierDebtFilterFooterActions(
                onReset = {
                    selectedStatus = SupplierDebtStatusFilter.All
                    selectedDue = SupplierDebtDueFilter.All
                    selectedSortOrder = SupplierDebtSortOrder.NearestDueDate
                },
                onApply = {
                    onApplyStatusFilter(selectedStatus)
                    onApplyDueFilter(selectedDue)
                    onApplySortOrder(selectedSortOrder)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SupplierDebtFilterHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = "Filter Hutang",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Atur status pelunasan dan filter hutang supplier",
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
}

@Composable
private fun SupplierDebtStatusFilterSection(
    selectedStatus: SupplierDebtStatusFilter,
    onSelect: (SupplierDebtStatusFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "STATUS PELUNASAN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.8.sp,
        )

        val statusPairs = listOf(
            listOf(SupplierDebtStatusFilter.All to "Semua", SupplierDebtStatusFilter.Unpaid to "Belum Lunas"),
            listOf(SupplierDebtStatusFilter.Partial to "Sebagian", SupplierDebtStatusFilter.Paid to "Lunas")
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
                                .clickable { onSelect(status) },
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
}

@Composable
private fun SupplierDebtDueFilterSection(
    selectedDue: SupplierDebtDueFilter,
    onSelect: (SupplierDebtDueFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "JATUH TEMPO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.8.sp,
        )

        val duePairs = listOf(
            listOf(SupplierDebtDueFilter.All to "Semua", SupplierDebtDueFilter.Overdue to "Lewat Jatuh Tempo"),
            listOf(SupplierDebtDueFilter.DueToday to "Hari Ini", SupplierDebtDueFilter.Upcoming to "Akan Datang")
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
                                .clickable { onSelect(filter) },
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
}

@Composable
private fun SupplierDebtSortOrderSection(
    selectedSortOrder: SupplierDebtSortOrder,
    onSelect: (SupplierDebtSortOrder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "URUTKAN BERDASARKAN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.8.sp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isDueDateSelected = selectedSortOrder == SupplierDebtSortOrder.NearestDueDate
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSelect(SupplierDebtSortOrder.NearestDueDate) },
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

            val isAmountSelected = selectedSortOrder == SupplierDebtSortOrder.HighestAmount
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSelect(SupplierDebtSortOrder.HighestAmount) },
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

        val isNameSelected = selectedSortOrder == SupplierDebtSortOrder.SupplierNameAsc
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { onSelect(SupplierDebtSortOrder.SupplierNameAsc) },
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
                    text = "Nama Supplier (A–Z)",
                    fontSize = 12.sp,
                    fontWeight = if (isNameSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isNameSelected) Color(0xFF256B57) else Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
private fun SupplierDebtFilterFooterActions(
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onReset,
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

        Button(
            onClick = onApply,
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SupplierDebtSummaryBottomSheet(
    uiState: SupplierDebtUiState,
    onDismiss: () -> Unit
) {
    val totalRemaining = uiState.pageRemainingTotal
    val totalSuppliers = remember(uiState.filteredPayables) {
        uiState.filteredPayables.map { it.supplierId.ifBlank { it.supplierName } }.distinct().size
    }
    val unpaidInvoices = uiState.unpaidCount
    val overdueInvoices = remember(uiState.filteredPayables) {
        val today = java.time.LocalDate.now()
        uiState.filteredPayables.count { p ->
            val due = runCatching { java.time.LocalDate.parse(p.dueDate.take(10)) }.getOrNull()
            due != null && due.isBefore(today) && !p.status.equals(SupplierDebtStatusFilter.Paid.apiValue, ignoreCase = true)
        }
    }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            SupplierDebtSummaryHeader(onDismiss = onDismiss)

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                SupplierDebtSummaryMetricRow(
                    title = "Total Sisa Hutang",
                    subtitle = "Sesuai filter halaman aktif",
                    value = totalRemaining.currencyText(),
                    valueColor = Color(0xFF256B57)
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                SupplierDebtSummaryMetricRow(
                    title = "Supplier Berhutang",
                    subtitle = "Jumlah supplier dengan tagihan",
                    value = totalSuppliers.toString(),
                    unit = "Supplier"
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                SupplierDebtSummaryMetricRow(
                    title = "Tagihan Belum Lunas",
                    subtitle = "Nota hutang belum selesai",
                    value = unpaidInvoices.toString(),
                    unit = "Nota"
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                SupplierDebtSummaryMetricRow(
                    title = "Lewat Jatuh Tempo",
                    subtitle = "Perlu segera dilunasi",
                    value = overdueInvoices.toString(),
                    unit = "Nota",
                    valueColor = if (overdueInvoices > 0) Color(0xFFE11D48) else Color(0xFF0F172A),
                    unitColor = if (overdueInvoices > 0) Color(0xFFF43F5E) else Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
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
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = "Tutup",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtSummaryHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Ringkasan Hutang",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Informasi ringkas total kewajiban dan supplier",
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
                contentDescription = "Tutup Ringkasan",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SupplierDebtSummaryMetricRow(
    title: String,
    subtitle: String,
    value: String,
    unit: String? = null,
    valueColor: Color = Color(0xFF0F172A),
    unitColor: Color = Color(0xFF64748B)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
        if (unit != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = unitColor
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

@Composable
private fun SupplierDebtMetrics(uiState: SupplierDebtUiState, compact: Boolean) {
    Row(
        modifier = (if (compact) Modifier.fillMaxWidth() else Modifier.width(548.dp))
            .testTag("supplier-debt-overview"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DebtMetricItem(
            title = "Sisa di halaman",
            value = uiState.pageRemainingTotal.currencyText(),
            icon = Icons.Outlined.AccountBalanceWallet,
            emphasized = true,
            modifier = if (compact) Modifier.weight(1.55f) else Modifier.width(330.dp),
        )
        DebtMetricItem(
            title = "Belum lunas",
            value = "${uiState.unpaidCount} tagihan",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            emphasized = false,
            modifier = if (compact) Modifier.weight(1f) else Modifier.width(208.dp),
        )
    }
}

@Composable
private fun DebtMetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    emphasized: Boolean,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(92.dp),
        color = if (emphasized) DebtPrimary.copy(alpha = 0.12f) else DebtSurface,
        shape = RoundedCornerShape(18.dp),
        border = if (emphasized) null else BorderStroke(1.dp, DebtLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (emphasized) DebtPrimary.copy(alpha = 0.16f) else DebtSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = DebtPrimaryDark, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = DebtMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = if (emphasized) DebtPrimaryDark else DebtText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
