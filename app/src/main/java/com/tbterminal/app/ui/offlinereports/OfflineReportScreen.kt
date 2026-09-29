package com.tbterminal.app.ui.offlinereports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.local.dao.OfflineCashReportRow
import com.tbterminal.app.data.local.dao.OfflineExpenseReportRow
import com.tbterminal.app.data.local.dao.OfflineReceivableReportRow
import com.tbterminal.app.data.local.dao.OfflineSalesReportRow
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.ui.components.TbPeriodFilterRow
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OfflineReportScreen(
    uiState: OfflineReportUiState,
    onPresetSelected: (OfflineReportPreset) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    onApplyCustomRange: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSnapshot = uiState.lastRefresh != null ||
        uiState.transactions.isNotEmpty() ||
        uiState.cashSessions.isNotEmpty() ||
        uiState.receivables.isNotEmpty() ||
        uiState.expenses.isNotEmpty()

    RefreshableContent(
        isRefreshing = uiState.isLoading && hasSnapshot,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val compact = maxWidth < 700.dp
            var selectedSection by rememberSaveable { mutableStateOf(OfflineReportSection.SALES) }
            var showMobileFilters by rememberSaveable { mutableStateOf(false) }
            var showMobileSummary by rememberSaveable { mutableStateOf(false) }
            var currentPage by rememberSaveable { mutableIntStateOf(1) }
            val totalItems = selectedSection.itemCount(uiState)
            val totalPages = ((totalItems + OFFLINE_REPORT_PAGE_SIZE - 1) / OFFLINE_REPORT_PAGE_SIZE).coerceAtLeast(1)
            val safePage = currentPage.coerceIn(1, totalPages)

            LaunchedEffect(selectedSection, uiState.startDate, uiState.endDate) {
                currentPage = 1
            }
            LaunchedEffect(totalPages) {
                if (currentPage > totalPages) currentPage = totalPages
            }

            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .widthIn(max = 1240.dp)
                    .padding(
                        horizontal = if (compact) 16.dp else 28.dp,
                        vertical = if (compact) 14.dp else 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
            ) {
                item {
                    if (compact) {
                        MobilePeriodToolbar(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            selectedSection = selectedSection,
                            onOpenFilters = { showMobileFilters = true },
                        )
                    } else {
                        OfflineReportFilter(
                            uiState = uiState,
                            onPresetSelected = onPresetSelected,
                            onCustomStartChanged = onCustomStartChanged,
                            onCustomEndChanged = onCustomEndChanged,
                            onApplyCustomRange = onApplyCustomRange,
                            selectedSection = selectedSection,
                            onSectionSelected = { selectedSection = it },
                        )
                    }
                }

                uiState.errorMessage?.let { message -> item { MessageCard(message) } }

                if (uiState.isLoading && !hasSnapshot) {
                    item { OfflineReportSkeleton(compact = compact) }
                } else {
                    if (!compact) {
                        item { OfflineKpiGrid(uiState = uiState, compact = false) }
                    }

                    item {
                        ReportContentHeader(
                            selectedSection = selectedSection,
                            compact = compact,
                            onOpenSummary = { showMobileSummary = true },
                        )
                    }

                    item {
                        Box(modifier = Modifier.testTag("offline-report-content")) {
                            when (selectedSection) {
                                OfflineReportSection.SALES -> OfflineSalesReportSection(
                                    transactions = uiState.transactions.page(safePage),
                                    compact = compact,
                                )
                                OfflineReportSection.CASH -> OfflineCashReportSection(uiState.cashSessions.page(safePage), compact)
                                OfflineReportSection.RECEIVABLE -> OfflineReceivableReportSection(uiState.receivables.page(safePage), compact)
                                OfflineReportSection.EXPENSE -> OfflineExpenseReportSection(uiState.expenses.page(safePage), compact)
                            }
                        }
                    }

                    item {
                        OfflineReportPagination(
                            currentPage = safePage,
                            totalPages = totalPages,
                            totalItems = totalItems,
                            compact = compact,
                            onPreviousPage = { currentPage = (safePage - 1).coerceAtLeast(1) },
                            onNextPage = { currentPage = (safePage + 1).coerceAtMost(totalPages) },
                        )
                    }
                }
            }

            if (compact && showMobileFilters) {
                TbMobileControlSheet(
                    title = "Filter laporan",
                    subtitle = "Pilih jenis laporan dan periode data",
                    onDismiss = { showMobileFilters = false },
                    testTag = "offline-report-filter-sheet",
                ) {
                    OfflineReportFilter(
                        uiState = uiState,
                        onPresetSelected = onPresetSelected,
                        onCustomStartChanged = onCustomStartChanged,
                        onCustomEndChanged = onCustomEndChanged,
                        onApplyCustomRange = onApplyCustomRange,
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it },
                        embedded = true,
                    )
                    TbMobileSheetDoneButton(
                        onClick = { showMobileFilters = false },
                        testTag = "offline-report-filter-done",
                    )
                }
            }

            if (compact && showMobileSummary) {
                TbMobileControlSheet(
                    title = "Ringkasan perangkat",
                    subtitle = compactRangeLabel(uiState.startDate, uiState.endDate),
                    onDismiss = { showMobileSummary = false },
                    testTag = "offline-report-summary-sheet",
                ) {
                    OfflineKpiGrid(uiState = uiState, compact = true)
                    TbMobileSheetDoneButton(
                        onClick = { showMobileSummary = false },
                        testTag = "offline-report-summary-done",
                    )
                }
            }
        }
    }
}

@Composable
private fun MobilePeriodToolbar(
    startDate: LocalDate,
    endDate: LocalDate,
    selectedSection: OfflineReportSection,
    onOpenFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(50.dp),
            color = TbSurface,
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, TbOutline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp), TbGreenDark)
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedSection.label, style = MaterialTheme.typography.labelMedium, color = TbText, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = compactRangeLabel(startDate, endDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = TbTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TbMobileFilterButton(
            onClick = onOpenFilters,
            testTag = "offline-report-open-filters",
            contentDescription = "Atur filter laporan",
        )
    }
}

@Composable
private fun OfflineReportFilter(
    uiState: OfflineReportUiState,
    onPresetSelected: (OfflineReportPreset) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    onApplyCustomRange: () -> Unit,
    selectedSection: OfflineReportSection,
    onSectionSelected: (OfflineReportSection) -> Unit,
    embedded: Boolean = false,
) {
    var showCustomDates by rememberSaveable {
        mutableStateOf(uiState.selectedPreset == OfflineReportPreset.Custom)
    }
    val content: @Composable ColumnScope.() -> Unit = {
        if (!embedded) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Periode laporan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(compactRangeLabel(uiState.startDate, uiState.endDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        FilterGroupTitle("Jenis laporan")
        ReportSectionSelector(selectedSection, onSectionSelected, twoRows = embedded)

        FilterGroupTitle("Periode")
        CompactPeriodSelector(
            selectedPreset = uiState.selectedPreset,
            dateLabel = uiState.endDate.formatCompactDate(),
            onSelected = {
                showCustomDates = false
                onPresetSelected(it)
            },
            onDateClick = { showCustomDates = !showCustomDates },
        )

        if (showCustomDates || uiState.selectedPreset == OfflineReportPreset.Custom) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterGroupTitle("Tanggal khusus")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateInput(uiState.customStartInput, onCustomStartChanged, "Mulai", Modifier.weight(1f))
                    DateInput(uiState.customEndInput, onCustomEndChanged, "Selesai", Modifier.weight(1f))
                    FilledTonalIconButton(
                        onClick = onApplyCustomRange,
                        modifier = Modifier.size(56.dp).testTag("offline-report-apply-date"),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = "Terapkan tanggal", tint = TbGreenDark)
                    }
                }
            }
        }
    }

    if (embedded) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("offline-report-period-filter"),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
private fun DateInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        placeholder = { Text("yyyy-mm-dd") },
        textStyle = MaterialTheme.typography.bodySmall,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TbGreenDark,
            unfocusedBorderColor = TbOutline,
            focusedContainerColor = TbSurface,
            unfocusedContainerColor = TbSurface,
        ),
    )
}

@Composable
private fun FilterGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TbText,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun CompactPeriodSelector(
    selectedPreset: OfflineReportPreset,
    dateLabel: String,
    onSelected: (OfflineReportPreset) -> Unit,
    onDateClick: () -> Unit,
) {
    TbPeriodFilterRow(
        selectedValue = selectedPreset,
        presets = listOf(
            OfflineReportPreset.Today to "Hari",
            OfflineReportPreset.Last7Days to "Minggu",
            OfflineReportPreset.ThisMonth to "Bulan",
        ),
        dateLabel = dateLabel,
        dateSelected = selectedPreset == OfflineReportPreset.Custom,
        onPresetSelected = onSelected,
        onDateClick = onDateClick,
        testTag = "offline-report-period-selector",
        dateTestTag = "offline-report-custom-date-toggle",
    )
}

@Composable
private fun ReportSectionSelector(
    selectedSection: OfflineReportSection,
    onSectionSelected: (OfflineReportSection) -> Unit,
    twoRows: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("offline-report-section-selector"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OfflineReportSection.entries.chunked(if (twoRows) 2 else 4).forEach { sections ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sections.forEach { section ->
                    FilterOption(
                        label = section.label,
                        selected = selectedSection == section,
                        onClick = { onSectionSelected(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        color = if (selected) TbGreenLight else TbSurfaceMuted.copy(alpha = 0.58f),
        contentColor = if (selected) TbGreenDark else TbTextMuted,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) TbGreenDark.copy(alpha = 0.30f) else TbOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReportContentHeader(
    selectedSection: OfflineReportSection,
    compact: Boolean,
    onOpenSummary: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selectedSection.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (compact) {
            TbMobileSummaryButton(onClick = onOpenSummary, testTag = "offline-report-open-summary")
        }
    }
}

@Composable
private fun OfflineKpiGrid(uiState: OfflineReportUiState, compact: Boolean) {
    val items = listOf(
        ReportKpi("Omzet perangkat", uiState.salesSummary.localRevenue.formatCurrency(), Icons.Outlined.Assessment, MaterialTheme.colorScheme.primary),
        ReportKpi("Uang diterima", uiState.salesSummary.collectedAmount.formatCurrency(), Icons.Outlined.Payments, MaterialTheme.colorScheme.secondary),
        ReportKpi("Sisa piutang", uiState.salesSummary.outstandingAmount.formatCurrency(), Icons.Outlined.CreditCard, MaterialTheme.colorScheme.error),
        ReportKpi("Transaksi", uiState.salesSummary.totalTransactions.toString(), Icons.AutoMirrored.Outlined.ReceiptLong, MaterialTheme.colorScheme.tertiary),
    )
    Column(
        modifier = Modifier.fillMaxWidth().testTag("offline-report-kpis"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.chunked(if (compact) 2 else 4).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item -> KpiCard(item, compact, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KpiCard(item: ReportKpi, compact: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(if (compact) 98.dp else 112.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(if (compact) 14.dp else 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(color = item.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                    Icon(item.icon, null, Modifier.padding(7.dp).size(18.dp), item.accent)
                }
            }
            Text(
                item.value,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun OfflineSalesReportSection(
    transactions: List<OfflineSalesReportRow>,
    compact: Boolean = false,
) {
    ReportCard {
        if (transactions.isEmpty()) EmptyRows("Belum ada transaksi.")
        else if (compact) transactions.forEach { row ->
            OfflineMobileRow(
                row.transactionCode,
                "${row.customerName} · ${row.occurredAt.formatDateTime()}",
                listOf("Total" to row.total.formatCurrency(), "Dibayar" to row.paidAmount.formatCurrency(), "Sisa" to row.remainingAmount.formatCurrency()),
                row.syncStatus,
            )
        } else {
            HeaderRow(listOf("Transaksi", "Pelanggan", "Total", "Dibayar", "Sisa", "Status"))
            transactions.forEach { row ->
                DataRow(
                    listOf("${row.transactionCode}\n${row.occurredAt.formatDateTime()}", row.customerName, row.total.formatCurrency(), row.paidAmount.formatCurrency(), row.remainingAmount.formatCurrency()),
                    row.syncStatus,
                )
            }
        }
    }
}

@Composable
fun OfflineCashReportSection(cashSessions: List<OfflineCashReportRow>, compact: Boolean = false) {
    ReportCard {
        if (cashSessions.isEmpty()) EmptyRows("Belum ada sesi kas.")
        else if (compact) cashSessions.forEach { row ->
            OfflineMobileRow(
                "Sesi ${row.status.lowercase().replaceFirstChar { it.uppercase() }}",
                "Kasir ${row.cashierUserId} · ${row.openedAt.formatDateTime()}",
                listOf("Modal" to row.startingCash.formatCurrency(), "Penjualan" to row.totalCashSales.formatCurrency(), "Kas akhir" to row.endingCash.formatCurrency()),
                row.syncStatus,
            )
        } else {
            HeaderRow(listOf("Kasir", "Modal", "Tunai", "Pengeluaran", "Kas akhir", "Status"))
            cashSessions.forEach { row ->
                DataRow(
                    listOf("${row.cashierUserId}\n${row.status} · ${row.openedAt.formatDateTime()}", row.startingCash.formatCurrency(), row.totalCashSales.formatCurrency(), row.totalExpense.formatCurrency(), row.endingCash.formatCurrency()),
                    row.syncStatus,
                )
            }
        }
    }
}

@Composable
fun OfflineReceivableReportSection(receivables: List<OfflineReceivableReportRow>, compact: Boolean = false) {
    ReportCard {
        if (receivables.isEmpty()) EmptyRows("Belum ada piutang.")
        else if (compact) receivables.forEach { row ->
            OfflineMobileRow(
                row.customerName,
                "${row.transactionCode} · ${row.status}",
                listOf("Total" to row.totalAmount.formatCurrency(), "Dibayar" to row.paidAmount.formatCurrency(), "Sisa" to row.remainingAmount.formatCurrency()),
                row.syncStatus,
            )
        } else {
            HeaderRow(listOf("Pelanggan", "Transaksi", "Total", "Dibayar", "Sisa", "Status"))
            receivables.forEach { row ->
                DataRow(
                    listOf("${row.customerName}\n${row.status}", row.transactionCode, row.totalAmount.formatCurrency(), row.paidAmount.formatCurrency(), row.remainingAmount.formatCurrency()),
                    row.syncStatus,
                )
            }
        }
    }
}

@Composable
fun OfflineExpenseReportSection(expenses: List<OfflineExpenseReportRow>, compact: Boolean = false) {
    ReportCard {
        if (expenses.isEmpty()) EmptyRows("Belum ada pengeluaran.")
        else if (compact) expenses.forEach { row ->
            OfflineMobileRow(
                row.category,
                row.description.orEmpty().takeIf { it.isNotBlank() }
                    ?.let { "${row.occurredAt.formatDateTime()} · $it" }
                    ?: row.occurredAt.formatDateTime(),
                listOf("Nominal" to row.amount.formatCurrency()),
                row.syncStatus,
            )
        } else {
            HeaderRow(listOf("Tanggal", "Kategori", "Nominal", "Catatan", "Status"))
            expenses.forEach { row ->
                DataRow(listOf(row.occurredAt.formatDateTime(), row.category, row.amount.formatCurrency(), row.description.orEmpty().ifBlank { "-" }), row.syncStatus)
            }
        }
    }
}

@Composable
private fun OfflineMobileRow(
    title: String,
    subtitle: String,
    values: List<Pair<String, String>>,
    status: SyncStatus,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            SyncBadge(status)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            values.forEach { (label, value) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ReportCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

@Composable
private fun OfflineReportPagination(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val firstItem = if (totalItems == 0) 0 else ((currentPage - 1) * OFFLINE_REPORT_PAGE_SIZE) + 1
    val lastItem = minOf(currentPage * OFFLINE_REPORT_PAGE_SIZE, totalItems)
    TbPagination(
        currentPage = currentPage,
        totalPages = totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (totalItems == 0) "0 data" else if (compact) "$totalItems data" else "Menampilkan $firstItem–$lastItem dari $totalItems data",
        testTag = "offline-report-pagination",
    )
}

@Composable
private fun HeaderRow(titles: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        titles.forEach { title ->
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DataRow(cells: List<String>, status: SyncStatus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { cell ->
            Text(cell, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(modifier = Modifier.weight(1f)) { SyncBadge(status) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SyncBadge(status: SyncStatus) {
    val colors = when (status) {
        SyncStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SyncStatus.SYNCING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SyncStatus.CONFLICT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        SyncStatus.SYNCED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val label = when (status) {
        SyncStatus.PENDING -> "Tertunda"
        SyncStatus.SYNCING -> "Sinkronisasi"
        SyncStatus.FAILED -> "Gagal"
        SyncStatus.CONFLICT -> "Konflik"
        SyncStatus.SYNCED -> "Tersinkron"
    }
    Surface(shape = RoundedCornerShape(999.dp), color = colors.first) {
        Text(label, color = colors.second, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun EmptyRows(text: String) {
    Box(modifier = Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageCard(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = RoundedCornerShape(14.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp))
    }
}

@Composable
private fun OfflineReportSkeleton(compact: Boolean) {
    Column(Modifier.fillMaxWidth().testTag("offline-report-skeleton"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(if (compact) 2 else 4) { SkeletonBox(Modifier.weight(1f).height(if (compact) 96.dp else 112.dp)) }
        }
        SkeletonBox(Modifier.fillMaxWidth(0.46f).height(18.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 240.dp else 300.dp))
    }
}

private data class ReportKpi(val title: String, val value: String, val icon: ImageVector, val accent: Color)

private enum class OfflineReportSection(val label: String, val title: String) {
    SALES("Penjualan", "Daftar penjualan"),
    CASH("Kas", "Daftar sesi kas"),
    RECEIVABLE("Piutang", "Daftar piutang"),
    EXPENSE("Pengeluaran", "Daftar pengeluaran"),
}

private fun OfflineReportSection.itemCount(uiState: OfflineReportUiState): Int = when (this) {
    OfflineReportSection.SALES -> uiState.transactions.size
    OfflineReportSection.CASH -> uiState.cashSessions.size
    OfflineReportSection.RECEIVABLE -> uiState.receivables.size
    OfflineReportSection.EXPENSE -> uiState.expenses.size
}

private fun <T> List<T>.page(page: Int): List<T> {
    val fromIndex = ((page - 1) * OFFLINE_REPORT_PAGE_SIZE).coerceAtLeast(0)
    if (fromIndex >= size) return emptyList()
    return subList(fromIndex, minOf(fromIndex + OFFLINE_REPORT_PAGE_SIZE, size))
}

private const val OFFLINE_REPORT_PAGE_SIZE = 10

private fun Double.formatCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun Long.formatDateTime(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")))

private fun LocalDate.formatShortDate(): String = format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))

private fun LocalDate.formatCompactDate(): String = format(DateTimeFormatter.ofPattern("dd/MM"))

private fun compactRangeLabel(startDate: LocalDate, endDate: LocalDate): String =
    if (startDate == endDate) startDate.formatShortDate() else "${startDate.formatShortDate()} – ${endDate.formatShortDate()}"
