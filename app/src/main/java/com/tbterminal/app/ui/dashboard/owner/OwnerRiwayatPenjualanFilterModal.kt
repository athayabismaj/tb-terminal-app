package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryUiState
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private val FILTER_MODAL_TABLET_BREAKPOINT = 840.dp
internal val FilterModalScrimColor = Color(0x7A14201B) // #14201B alpha 0.48
internal val FilterSheetHandleColor = Color(0xFFD5DAD7)
private val FilterZoneWib = ZoneId.of("Asia/Jakarta")

@Immutable
internal data class SalesFilterChipOption(
    val id: String,
    val label: String,
    val selected: Boolean,
    val span: Int = 1,
    val icon: ImageVector? = null,
    val testTag: String,
    val onClick: () -> Unit
)

@Immutable
internal data class SalesFilterDraftState(
    val preset: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val statusFilter: String,
    val paymentMethodFilter: String,
    val sortOrder: SalesSortOrder
)

internal fun AdminTransactionHistoryUiState.toSalesFilterDraftState(
    today: LocalDate = LocalDate.now(FilterZoneWib)
): SalesFilterDraftState {
    val activePreset = selectedPreset ?: "Hari ini"
    val start = if (activePreset == "Semua") {
        null
    } else {
        startDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    val end = if (activePreset == "Semua") {
        null
    } else {
        endDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    return SalesFilterDraftState(
        preset = activePreset,
        startDate = start,
        endDate = end,
        statusFilter = statusFilter,
        paymentMethodFilter = paymentMethodFilter,
        sortOrder = sortOrder
    )
}

internal fun resetSalesFilterDraft(): SalesFilterDraftState = SalesFilterDraftState(
    preset = "Semua",
    startDate = null,
    endDate = null,
    statusFilter = "Semua",
    paymentMethodFilter = "Semua",
    sortOrder = SalesSortOrder.Newest
)

internal fun formatSalesFilterCustomDateLabel(
    startDate: LocalDate?,
    endDate: LocalDate?
): String {
    if (startDate == null || endDate == null) return "Pilih tanggal"
    val startMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val endMillis = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    return formatCustomRangeChipLabel(startMillis to endMillis)
}

@Composable
internal fun OwnerSalesFilterModal(
    state: AdminTransactionHistoryUiState,
    onDismiss: () -> Unit,
    onApplyFilter: (String, String, String?, String?, String?, SalesSortOrder) -> Unit
) {
    val today = remember { LocalDate.now(FilterZoneWib) }
    var draft by remember(
        state.selectedPreset,
        state.startDate,
        state.endDate,
        state.statusFilter,
        state.paymentMethodFilter,
        state.sortOrder
    ) {
        mutableStateOf(state.toSalesFilterDraftState(today))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= FILTER_MODAL_TABLET_BREAKPOINT
        val availableHeight = maxHeight

        FilterSheetContainer(
            title = "Filter penjualan",
            isTablet = isTablet,
            availableHeight = availableHeight,
            onDismiss = onDismiss,
            footer = {
                FilterFooter(
                    isTablet = isTablet,
                    onReset = { draft = resetSalesFilterDraft() },
                    onApply = {
                        val startStr = if (draft.preset == "Semua") null else draft.startDate?.toString()
                        val endStr = if (draft.preset == "Semua") null else draft.endDate?.toString()
                        onApplyFilter(
                            draft.statusFilter,
                            draft.paymentMethodFilter,
                            startStr,
                            endStr,
                            draft.preset,
                            draft.sortOrder
                        )
                    }
                )
            }
        ) {
            OwnerSalesFilterFormBody(
                draft = draft,
                isTablet = isTablet,
                today = today,
                onDraftChange = { draft = it },
                onOpenDatePicker = { showDatePicker = true }
            )
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = (draft.startDate ?: today).toString(),
            initialMode = when (draft.preset) {
                "Bulan ini", "Bulan" -> "Bulan"
                "Minggu ini", "Minggu" -> "Minggu"
                else -> "Hari"
            },
            onDismiss = { showDatePicker = false },
            onConfirmRange = { newStartStr, newEndStr, _ ->
                draft = draft.copy(
                    preset = "Kustom",
                    startDate = LocalDate.parse(newStartStr),
                    endDate = LocalDate.parse(newEndStr)
                )
                showDatePicker = false
            },
            onConfirm = { singleDateStr ->
                val date = LocalDate.parse(singleDateStr)
                draft = draft.copy(
                    preset = "Kustom",
                    startDate = date,
                    endDate = date
                )
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheetContainer(
    title: String,
    isTablet: Boolean,
    availableHeight: Dp,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetTestTag: String = "sales-history-filter-sheet",
    closeTestTag: String = "sales-history-filter-close",
    content: @Composable ColumnScope.() -> Unit
) {
    if (isTablet) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FilterModalScrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                val maxDialogHeight = (availableHeight - 48.dp).coerceAtLeast(320.dp)
                Column(
                    modifier = modifier
                        .width(560.dp)
                        .heightIn(max = maxDialogHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceWhite)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .testTag(sheetTestTag)
                ) {
                    FilterSheetHeader(
                        title = title,
                        isTablet = true,
                        onDismiss = onDismiss,
                        closeTestTag = closeTestTag
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
                        content = content
                    )
                    footer()
                }
            }
        }
    } else {
        val density = LocalDensity.current
        val statusBarTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        val minTopClearance = maxOf(56.dp, statusBarTopDp + 8.dp)
        val sheetHeight = (availableHeight - minTopClearance).coerceAtLeast(320.dp)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = SurfaceWhite,
            tonalElevation = 0.dp,
            scrimColor = FilterModalScrimColor,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            modifier = modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .testTag(sheetTestTag)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FilterSheetDragHandle()
                FilterSheetHeader(
                    title = title,
                    isTablet = false,
                    onDismiss = onDismiss,
                    closeTestTag = closeTestTag
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp),
                    content = content
                )
                footer()
            }
        }
    }
}

@Composable
internal fun FilterSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(FilterSheetHandleColor)
        )
    }
}

@Composable
internal fun FilterSheetHeader(
    title: String,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    closeTestTag: String = "sales-history-filter-close"
) {
    val rowHeight = if (isTablet) 64.dp else 48.dp
    val startPadding = if (isTablet) 24.dp else 20.dp
    val endPadding = if (isTablet) 12.dp else 8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(start = startPadding, end = endPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ownerTextStyle(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .cssLineBoxHeight(26.dp)
                .semantics { heading() }
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onDismiss
                )
                .semantics { contentDescription = "Tutup" }
                .testTag(closeTestTag),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.CloseX,
                tint = Muted,
                size = 24.dp
            )
        }
    }
}

@Composable
internal fun OwnerSalesFilterFormBody(
    draft: SalesFilterDraftState,
    isTablet: Boolean,
    today: LocalDate,
    onDraftChange: (SalesFilterDraftState) -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val sectionSpacing = if (isTablet) 28.dp else 32.dp
    val isCustomDate = draft.preset.equals("Kustom", ignoreCase = true)
    val customDateLabel = remember(isCustomDate, draft.startDate, draft.endDate) {
        if (isCustomDate) {
            formatSalesFilterCustomDateLabel(draft.startDate, draft.endDate)
        } else {
            "Pilih tanggal"
        }
    }

    FilterSection(
        label = "Periode",
        options = listOf(
            SalesFilterChipOption(
                id = "Semua",
                label = "Semua",
                selected = draft.preset.equals("Semua", ignoreCase = true),
                testTag = "sales-filter-period-Semua",
                onClick = {
                    onDraftChange(draft.copy(preset = "Semua", startDate = null, endDate = null))
                }
            ),
            SalesFilterChipOption(
                id = "Hari ini",
                label = "Hari ini",
                selected = draft.preset.equals("Hari ini", ignoreCase = true),
                testTag = "sales-filter-period-Hari ini",
                onClick = {
                    onDraftChange(draft.copy(preset = "Hari ini", startDate = today, endDate = today))
                }
            ),
            SalesFilterChipOption(
                id = "Minggu ini",
                label = "Minggu ini",
                selected = draft.preset.equals("Minggu ini", ignoreCase = true),
                testTag = "sales-filter-period-Minggu ini",
                onClick = {
                    onDraftChange(
                        draft.copy(
                            preset = "Minggu ini",
                            startDate = today.with(DayOfWeek.MONDAY),
                            endDate = today.with(DayOfWeek.SUNDAY)
                        )
                    )
                }
            ),
            SalesFilterChipOption(
                id = "Bulan ini",
                label = "Bulan ini",
                selected = draft.preset.equals("Bulan ini", ignoreCase = true),
                testTag = "sales-filter-period-Bulan ini",
                onClick = {
                    onDraftChange(
                        draft.copy(
                            preset = "Bulan ini",
                            startDate = today.withDayOfMonth(1),
                            endDate = today.withDayOfMonth(today.lengthOfMonth())
                        )
                    )
                }
            ),
            SalesFilterChipOption(
                id = "Kustom",
                label = customDateLabel,
                selected = isCustomDate,
                span = 2,
                icon = OwnerIcons.CalendarRange,
                testTag = "sales-history-custom-date-btn",
                onClick = onOpenDatePicker
            )
        )
    )

    Spacer(modifier = Modifier.height(sectionSpacing))

    val normalizedStatus = draft.statusFilter.trim().lowercase()
    FilterSection(
        label = "Status transaksi",
        options = listOf(
            SalesFilterChipOption(
                id = "Semua",
                label = "Semua",
                selected = normalizedStatus == "semua",
                testTag = "sales-filter-status-Semua",
                onClick = { onDraftChange(draft.copy(statusFilter = "Semua")) }
            ),
            SalesFilterChipOption(
                id = "Lunas",
                label = "Lunas",
                selected = normalizedStatus == "lunas",
                testTag = "sales-filter-status-Lunas",
                onClick = { onDraftChange(draft.copy(statusFilter = "Lunas")) }
            ),
            SalesFilterChipOption(
                id = "DP",
                label = "DP",
                selected = normalizedStatus == "dp",
                testTag = "sales-filter-status-DP",
                onClick = { onDraftChange(draft.copy(statusFilter = "DP")) }
            ),
            SalesFilterChipOption(
                id = "Hutang",
                label = "Belum lunas",
                selected = normalizedStatus == "hutang" || normalizedStatus == "belum lunas",
                testTag = "sales-filter-status-Hutang",
                onClick = { onDraftChange(draft.copy(statusFilter = "Hutang")) }
            ),
            SalesFilterChipOption(
                id = "Voided",
                label = "Dibatalkan",
                selected = normalizedStatus == "voided" || normalizedStatus == "dibatalkan",
                span = 2,
                testTag = "sales-filter-status-voided",
                onClick = { onDraftChange(draft.copy(statusFilter = "Voided")) }
            )
        )
    )

    Spacer(modifier = Modifier.height(sectionSpacing))

    val normalizedPayment = draft.paymentMethodFilter.trim().lowercase()
    FilterSection(
        label = "Metode pembayaran",
        options = listOf(
            SalesFilterChipOption(
                id = "Semua",
                label = "Semua",
                selected = normalizedPayment == "semua",
                testTag = "sales-filter-payment-Semua",
                onClick = { onDraftChange(draft.copy(paymentMethodFilter = "Semua")) }
            ),
            SalesFilterChipOption(
                id = "tunai",
                label = "Cash",
                selected = normalizedPayment == "tunai" || normalizedPayment == "cash",
                testTag = "sales-filter-payment-tunai",
                onClick = { onDraftChange(draft.copy(paymentMethodFilter = "tunai")) }
            ),
            SalesFilterChipOption(
                id = "qris",
                label = "QRIS",
                selected = normalizedPayment == "qris",
                testTag = "sales-filter-payment-qris",
                onClick = { onDraftChange(draft.copy(paymentMethodFilter = "qris")) }
            ),
            SalesFilterChipOption(
                id = "transfer",
                label = "Transfer",
                selected = normalizedPayment == "transfer",
                testTag = "sales-filter-payment-transfer",
                onClick = { onDraftChange(draft.copy(paymentMethodFilter = "transfer")) }
            ),
            SalesFilterChipOption(
                id = "hutang",
                label = "Hutang",
                selected = normalizedPayment == "hutang",
                span = 2,
                testTag = "sales-filter-payment-hutang",
                onClick = { onDraftChange(draft.copy(paymentMethodFilter = "hutang")) }
            )
        )
    )

    Spacer(modifier = Modifier.height(sectionSpacing))

    FilterSection(
        label = "Urutkan",
        options = listOf(
            SalesFilterChipOption(
                id = "newest",
                label = "Terbaru",
                selected = draft.sortOrder == SalesSortOrder.Newest,
                testTag = "sales-filter-sort-newest",
                onClick = { onDraftChange(draft.copy(sortOrder = SalesSortOrder.Newest)) }
            ),
            SalesFilterChipOption(
                id = "highest",
                label = "Nominal",
                selected = draft.sortOrder == SalesSortOrder.HighestAmount,
                testTag = "sales-filter-sort-highest",
                onClick = { onDraftChange(draft.copy(sortOrder = SalesSortOrder.HighestAmount)) }
            ),
            SalesFilterChipOption(
                id = "receipt",
                label = "No. nota",
                selected = draft.sortOrder == SalesSortOrder.ReceiptAsc,
                testTag = "sales-filter-sort-receipt",
                onClick = { onDraftChange(draft.copy(sortOrder = SalesSortOrder.ReceiptAsc)) }
            )
        )
    )
}

@Composable
internal fun FilterSection(
    label: String,
    options: List<SalesFilterChipOption>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    headerEndContent: (@Composable () -> Unit)? = null
) {
    val rows = remember(options, columns) { partitionColumnRows(options, columns) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(20.dp)
                    .semantics { heading() }
            )
            headerEndContent?.invoke()
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { rowOptions ->
                ColumnChipRow(options = rowOptions, columns = columns)
            }
        }
    }
}

private fun partitionColumnRows(
    options: List<SalesFilterChipOption>,
    columns: Int
): List<List<SalesFilterChipOption>> {
    val safeColumns = columns.coerceAtLeast(1)
    val rows = mutableListOf<List<SalesFilterChipOption>>()
    var current = mutableListOf<SalesFilterChipOption>()
    var usedColumns = 0
    options.forEach { option ->
        val span = option.span.coerceIn(1, safeColumns)
        if (usedColumns + span > safeColumns && current.isNotEmpty()) {
            rows.add(current)
            current = mutableListOf()
            usedColumns = 0
        }
        current.add(option)
        usedColumns += span
        if (usedColumns == safeColumns) {
            rows.add(current)
            current = mutableListOf()
            usedColumns = 0
        }
    }
    if (current.isNotEmpty()) {
        rows.add(current)
    }
    return rows
}

/**
 * Menata satu baris grid dengan jarak tetap 8dp. Chip dengan span > 1 melebar proporsional
 * sehingga tepi sejajar dengan kolom di baris lain, sekaligus memberi tinggi sentuh 48dp
 * tanpa mengubah jarak visual 40dp + 8dp.
 */
@Composable
internal fun ColumnChipRow(
    options: List<SalesFilterChipOption>,
    columns: Int = 3,
    modifier: Modifier = Modifier
) {
    val safeColumns = columns.coerceAtLeast(1)
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            options.forEach { option ->
                SalesFilterChip(option = option)
            }
        }
    ) { measurables, constraints ->
        val gapPx = 8.dp.roundToPx()
        val chipVisualHeightPx = 40.dp.roundToPx()
        val touchExtraVerticalPx = 4.dp.roundToPx()
        val touchHeightPx = chipVisualHeightPx + (touchExtraVerticalPx * 2)

        val totalWidth = constraints.maxWidth
        val colWidthPx = (totalWidth - ((safeColumns - 1) * gapPx)).coerceAtLeast(0) / safeColumns.toFloat()

        var xCursor = 0
        val placeables = measurables.mapIndexed { index, measurable ->
            val span = options[index].span.coerceIn(1, safeColumns)
            val isLastInFullRow = index == measurables.lastIndex &&
                options.sumOf { it.span.coerceIn(1, safeColumns) } == safeColumns
            val itemWidth = if (isLastInFullRow) {
                (totalWidth - xCursor).coerceAtLeast(0)
            } else {
                (colWidthPx * span + gapPx * (span - 1)).toInt().coerceAtLeast(0)
            }
            val placeable = measurable.measure(
                Constraints.fixed(width = itemWidth, height = touchHeightPx)
            )
            xCursor += itemWidth + gapPx
            placeable
        }

        layout(width = totalWidth, height = chipVisualHeightPx) {
            var placeX = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x = placeX, y = -touchExtraVerticalPx)
                placeX += placeable.width + gapPx
            }
        }
    }
}

@Composable
internal fun SalesFilterChip(
    option: SalesFilterChipOption,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var chipWidthPx by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .height(48.dp)
            .selectable(
                selected = option.selected,
                onClick = option.onClick,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .testTag(option.testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .background(SurfaceWhite)
                .border(
                    width = if (option.selected) 2.dp else 1.dp,
                    color = if (option.selected) Brand else OwnerBorder,
                    shape = shape
                )
                .onSizeChanged { chipWidthPx = it.width }
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconWidthPx = if (option.icon != null) {
                with(density) { (18.dp + 8.dp).toPx().toInt() }
            } else {
                0
            }
            val horizontalPaddingPx = with(density) { 16.dp.toPx().toInt() }
            val availableTextWidthPx = (chipWidthPx - horizontalPaddingPx - iconWidthPx).coerceAtLeast(1)
            val fontWeight = if (option.selected) FontWeight.SemiBold else FontWeight.Medium
            val textSize = remember(option.label, availableTextWidthPx, fontWeight, density.fontScale) {
                if (chipWidthPx <= 0) {
                    14.sp
                } else {
                    val candidates = listOf(14f, 13f, 12f, 11f)
                    candidates.firstOrNull { sizeSp ->
                        textMeasurer.measure(
                            text = option.label,
                            style = ownerTextStyle(
                                fontSize = sizeSp.sp,
                                lineHeight = 20.sp,
                                fontWeight = fontWeight,
                                color = Ink
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width <= availableTextWidthPx
                    }?.sp ?: 11.sp
                }
            }

            if (option.icon != null) {
                OwnerVectorIcon(
                    imageVector = option.icon,
                    tint = if (option.selected) Brand else Ink,
                    size = 18.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = option.label,
                style = ownerTextStyle(
                    fontSize = textSize,
                    lineHeight = if (textSize == 14.sp) 20.sp else (textSize.value * 1.4f).sp,
                    fontWeight = fontWeight,
                    color = if (option.selected) Brand else Ink
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun FilterFooter(
    isTablet: Boolean,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    resetTestTag: String = "sales-history-filter-reset",
    applyTestTag: String = "sales-history-filter-done"
) {
    val startEndPadding = if (isTablet) 24.dp else 20.dp
    val buttonShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isTablet) Modifier else Modifier.navigationBarsPadding())
                .padding(start = startEndPadding, top = 12.dp, end = startEndPadding, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(buttonShape)
                    .background(SurfaceWhite)
                    .border(width = 1.dp, color = OwnerBorder, shape = buttonShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onReset
                    )
                    .testTag(resetTestTag),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Reset",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    ),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .clip(buttonShape)
                    .background(Brand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onApply
                    )
                    .testTag(applyTestTag),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Terapkan",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun SalesFilterReferenceFrame(
    isTablet: Boolean,
    draft: SalesFilterDraftState = SalesFilterDraftState(
        preset = "Hari ini",
        startDate = LocalDate.of(2026, 11, 23),
        endDate = LocalDate.of(2026, 11, 23),
        statusFilter = "Semua",
        paymentMethodFilter = "Semua",
        sortOrder = SalesSortOrder.Newest
    ),
    today: LocalDate = LocalDate.of(2026, 11, 23),
    modifier: Modifier = Modifier
) {
    val emptySpec = ownerSalesEmptySpec(hasActiveFilterOrSearch = true)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableHeight = maxHeight

        OwnerRiwayatPenjualanScreen(
            summary = OwnerSalesSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            transactions = emptyList(),
            emptyTitle = emptySpec.title,
            emptyDescription = emptySpec.description,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onReportClick = {},
            onNavSelect = {},
            autoFocusSearch = false
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FilterModalScrimColor)
        ) {
            if (isTablet) {
                val maxDialogHeight = (availableHeight - 48.dp).coerceAtLeast(320.dp)
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(560.dp)
                        .heightIn(max = maxDialogHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceWhite)
                ) {
                    FilterSheetHeader(title = "Filter penjualan", isTablet = true, onDismiss = {})
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp)
                    ) {
                        OwnerSalesFilterFormBody(
                            draft = draft,
                            isTablet = true,
                            today = today,
                            onDraftChange = {},
                            onOpenDatePicker = {}
                        )
                    }
                    FilterFooter(isTablet = true, onReset = {}, onApply = {})
                }
            } else {
                val sheetHeight = (availableHeight - 56.dp).coerceAtLeast(320.dp)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(SurfaceWhite)
                ) {
                    FilterSheetDragHandle()
                    FilterSheetHeader(title = "Filter penjualan", isTablet = false, onDismiss = {})
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        OwnerSalesFilterFormBody(
                            draft = draft,
                            isTablet = false,
                            today = today,
                            onDraftChange = {},
                            onOpenDatePicker = {}
                        )
                    }
                    FilterFooter(isTablet = false, onReset = {}, onApply = {})
                }
            }
        }
    }
}

@Preview(
    name = "Barokah Jaya Filter Penjualan - Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
internal fun OwnerSalesFilterPhonePreview() {
    TbterminalappTheme {
        SalesFilterReferenceFrame(isTablet = false)
    }
}

@Preview(
    name = "Barokah Jaya Filter Penjualan - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerSalesFilterTabletPreview() {
    TbterminalappTheme {
        SalesFilterReferenceFrame(isTablet = true)
    }
}
