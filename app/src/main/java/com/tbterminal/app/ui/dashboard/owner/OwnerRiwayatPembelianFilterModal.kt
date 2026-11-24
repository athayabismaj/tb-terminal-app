package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.purchasehistory.PurchaseHistoryUiState
import com.tbterminal.app.ui.purchasehistory.PurchaseSortOrder
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.Placeholder
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

private val PURCHASE_FILTER_TABLET_BREAKPOINT = 840.dp
private val PurchaseFilterZoneWib = ZoneId.of("Asia/Jakarta")

@Immutable
internal data class PurchaseFilterDraftState(
    val preset: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val supplierId: String?,
    val sortOrder: PurchaseSortOrder
)

internal fun normalizePurchasePeriodType(raw: String): String = when {
    raw.equals("Semua", ignoreCase = true) -> "Semua"
    raw.equals("Hari ini", ignoreCase = true) || raw.equals("Hari", ignoreCase = true) -> "Hari Ini"
    raw.equals("Minggu ini", ignoreCase = true) || raw.equals("Minggu", ignoreCase = true) -> "Minggu Ini"
    raw.equals("Bulan ini", ignoreCase = true) || raw.equals("Bulan", ignoreCase = true) -> "Bulan Ini"
    raw.equals("Kustom", ignoreCase = true) -> "Kustom"
    else -> "Hari Ini"
}

internal fun PurchaseHistoryUiState.toPurchaseFilterDraftState(
    today: LocalDate = LocalDate.now(PurchaseFilterZoneWib)
): PurchaseFilterDraftState {
    val activePreset = normalizePurchasePeriodType(selectedPeriodType)
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
    return PurchaseFilterDraftState(
        preset = activePreset,
        startDate = start,
        endDate = end,
        supplierId = selectedSupplierId,
        sortOrder = sortOrder
    )
}

internal fun resetPurchaseFilterDraft(): PurchaseFilterDraftState = PurchaseFilterDraftState(
    preset = "Semua",
    startDate = null,
    endDate = null,
    supplierId = null,
    sortOrder = PurchaseSortOrder.NEWEST
)

@Composable
internal fun OwnerPurchaseFilterModal(
    state: PurchaseHistoryUiState,
    onDismiss: () -> Unit,
    onApplyFilter: (String?, String?, String?, String, PurchaseSortOrder) -> Unit
) {
    val today = remember { LocalDate.now(PurchaseFilterZoneWib) }
    var draft by remember(
        state.selectedPeriodType,
        state.startDate,
        state.endDate,
        state.selectedSupplierId,
        state.sortOrder
    ) {
        mutableStateOf(state.toPurchaseFilterDraftState(today))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= PURCHASE_FILTER_TABLET_BREAKPOINT
        val availableHeight = maxHeight

        FilterSheetContainer(
            title = "Filter pembelian",
            isTablet = isTablet,
            availableHeight = availableHeight,
            onDismiss = onDismiss,
            sheetTestTag = "purchase-history-filter-sheet",
            closeTestTag = "purchase-history-filter-close",
            footer = {
                FilterFooter(
                    isTablet = isTablet,
                    onReset = { draft = resetPurchaseFilterDraft() },
                    onApply = {
                        val (startIso, endIso) = if (draft.preset == "Semua") {
                            null to null
                        } else {
                            val s = draft.startDate ?: today
                            val e = draft.endDate ?: s
                            "${s}T00:00:00Z" to "${e}T23:59:59Z"
                        }
                        onApplyFilter(
                            draft.supplierId,
                            startIso,
                            endIso,
                            draft.preset,
                            draft.sortOrder
                        )
                    },
                    resetTestTag = "purchase-history-filter-reset",
                    applyTestTag = "purchase-history-filter-done"
                )
            }
        ) {
            OwnerPurchaseFilterFormBody(
                draft = draft,
                suppliers = state.suppliers,
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
                "Bulan Ini", "Bulan" -> "Bulan"
                "Minggu Ini", "Minggu" -> "Minggu"
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

@Composable
internal fun OwnerPurchaseFilterFormBody(
    draft: PurchaseFilterDraftState,
    suppliers: List<Supplier>,
    isTablet: Boolean,
    today: LocalDate,
    onDraftChange: (PurchaseFilterDraftState) -> Unit,
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
                testTag = "purchase-filter-period-Semua",
                onClick = {
                    onDraftChange(draft.copy(preset = "Semua", startDate = null, endDate = null))
                }
            ),
            SalesFilterChipOption(
                id = "Hari Ini",
                label = "Hari ini",
                selected = draft.preset.equals("Hari Ini", ignoreCase = true),
                testTag = "purchase-filter-period-Hari Ini",
                onClick = {
                    onDraftChange(draft.copy(preset = "Hari Ini", startDate = today, endDate = today))
                }
            ),
            SalesFilterChipOption(
                id = "Minggu Ini",
                label = "Minggu ini",
                selected = draft.preset.equals("Minggu Ini", ignoreCase = true),
                testTag = "purchase-filter-period-Minggu Ini",
                onClick = {
                    onDraftChange(
                        draft.copy(
                            preset = "Minggu Ini",
                            startDate = today.with(DayOfWeek.MONDAY),
                            endDate = today.with(DayOfWeek.SUNDAY)
                        )
                    )
                }
            ),
            SalesFilterChipOption(
                id = "Bulan Ini",
                label = "Bulan ini",
                selected = draft.preset.equals("Bulan Ini", ignoreCase = true),
                testTag = "purchase-filter-period-Bulan Ini",
                onClick = {
                    onDraftChange(
                        draft.copy(
                            preset = "Bulan Ini",
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
                testTag = "purchase-history-custom-date-btn",
                onClick = onOpenDatePicker
            )
        )
    )

    Spacer(modifier = Modifier.height(sectionSpacing))

    SupplierFilterChipSection(
        suppliers = suppliers,
        selectedSupplierId = draft.supplierId,
        onSelectSupplier = { supplierId ->
            onDraftChange(draft.copy(supplierId = supplierId))
        }
    )

    Spacer(modifier = Modifier.height(sectionSpacing))

    FilterSection(
        label = "Urutkan",
        options = listOf(
            SalesFilterChipOption(
                id = "newest",
                label = "Terbaru",
                selected = draft.sortOrder == PurchaseSortOrder.NEWEST,
                testTag = "purchase-filter-sort-newest",
                onClick = { onDraftChange(draft.copy(sortOrder = PurchaseSortOrder.NEWEST)) }
            ),
            SalesFilterChipOption(
                id = "highest",
                label = "Nominal",
                selected = draft.sortOrder == PurchaseSortOrder.HIGHEST_AMOUNT,
                testTag = "purchase-filter-sort-highest",
                onClick = { onDraftChange(draft.copy(sortOrder = PurchaseSortOrder.HIGHEST_AMOUNT)) }
            ),
            SalesFilterChipOption(
                id = "name",
                label = "Nama (A–Z)",
                selected = draft.sortOrder == PurchaseSortOrder.NAME_ASC,
                testTag = "purchase-filter-sort-name",
                onClick = { onDraftChange(draft.copy(sortOrder = PurchaseSortOrder.NAME_ASC)) }
            ),
            SalesFilterChipOption(
                id = "due",
                label = "Tempo terdekat",
                selected = draft.sortOrder == PurchaseSortOrder.DUE_NEAREST,
                span = 2,
                testTag = "purchase-filter-sort-due",
                onClick = { onDraftChange(draft.copy(sortOrder = PurchaseSortOrder.DUE_NEAREST)) }
            )
        )
    )
}

@Composable
private fun SupplierFilterChipSection(
    suppliers: List<Supplier>,
    selectedSupplierId: String?,
    onSelectSupplier: (String?) -> Unit
) {
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val latestOnSelectSupplier by rememberUpdatedState(onSelectSupplier)

    val filteredSuppliers = remember(suppliers, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) suppliers
        else suppliers.filter { it.name.contains(trimmed, ignoreCase = true) }
    }

    val options = remember(filteredSuppliers, selectedSupplierId, searchQuery) {
        buildList {
            if (searchQuery.isBlank() || "semua supplier".contains(searchQuery.trim(), ignoreCase = true)) {
                add(
                    SalesFilterChipOption(
                        id = "all",
                        label = "Semua",
                        selected = selectedSupplierId == null,
                        testTag = "purchase-filter-supplier-all",
                        onClick = { latestOnSelectSupplier(null) }
                    )
                )
            }
            filteredSuppliers.forEach { supplier ->
                add(
                    SalesFilterChipOption(
                        id = supplier.id,
                        label = supplier.name,
                        selected = selectedSupplierId == supplier.id,
                        testTag = "purchase-filter-supplier-${supplier.id}",
                        onClick = { latestOnSelectSupplier(supplier.id) }
                    )
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Supplier",
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${suppliers.size} terdaftar",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Muted
                    ),
                    maxLines = 1
                )
                if (suppliers.size > 4 || isSearchVisible) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSearchVisible || searchQuery.isNotEmpty()) ChipBg else SurfaceWhite)
                            .border(1.dp, OwnerBorder, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    isSearchVisible = !isSearchVisible
                                    if (!isSearchVisible) searchQuery = ""
                                }
                            )
                            .semantics { contentDescription = "Cari supplier" }
                            .testTag("purchase-filter-toggle-supplier-search"),
                        contentAlignment = Alignment.Center
                    ) {
                        OwnerVectorIcon(
                            imageVector = OwnerIcons.Search,
                            tint = if (isSearchVisible || searchQuery.isNotEmpty()) Brand else Muted,
                            size = 16.dp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isSearchVisible || searchQuery.isNotEmpty()) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, OwnerBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.Search,
                        tint = Muted,
                        size = 16.dp
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari supplier...",
                                style = ownerTextStyle(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Placeholder
                                )
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = ownerTextStyle(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Ink
                            ),
                            cursorBrush = SolidColor(Brand),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("purchase-filter-supplier-search-input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            OwnerVectorIcon(
                                imageVector = OwnerIcons.CloseX,
                                tint = Muted,
                                size = 14.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (options.isEmpty()) {
            Text(
                text = "Supplier tidak ditemukan",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val rows = remember(options) {
                options.chunked(2)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowOptions ->
                    ColumnChipRow(options = rowOptions, columns = 2)
                }
            }
        }
    }
}

@Composable
internal fun PurchaseFilterReferenceFrame(
    isTablet: Boolean,
    draft: PurchaseFilterDraftState = PurchaseFilterDraftState(
        preset = "Hari Ini",
        startDate = LocalDate.of(2026, 11, 23),
        endDate = LocalDate.of(2026, 11, 23),
        supplierId = null,
        sortOrder = PurchaseSortOrder.NEWEST
    ),
    suppliers: List<Supplier> = sampleReferenceSuppliers(),
    today: LocalDate = LocalDate.of(2026, 11, 23),
    modifier: Modifier = Modifier
) {
    val emptySpec = ownerPurchaseEmptySpec(hasActiveFilterOrSearch = false)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableHeight = maxHeight

        OwnerRiwayatPembelianScreen(
            summary = OwnerPurchaseSummaryUiState(),
            sortLabel = "Urut: Terbaru",
            isSearchExpanded = false,
            searchQuery = "",
            isLoading = false,
            errorMessage = null,
            purchases = emptyList(),
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
                    FilterSheetHeader(
                        title = "Filter pembelian",
                        isTablet = true,
                        onDismiss = {},
                        closeTestTag = "purchase-history-filter-close"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp)
                    ) {
                        OwnerPurchaseFilterFormBody(
                            draft = draft,
                            suppliers = suppliers,
                            isTablet = true,
                            today = today,
                            onDraftChange = {},
                            onOpenDatePicker = {}
                        )
                    }
                    FilterFooter(
                        isTablet = true,
                        onReset = {},
                        onApply = {},
                        resetTestTag = "purchase-history-filter-reset",
                        applyTestTag = "purchase-history-filter-done"
                    )
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
                    FilterSheetHeader(
                        title = "Filter pembelian",
                        isTablet = false,
                        onDismiss = {},
                        closeTestTag = "purchase-history-filter-close"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        OwnerPurchaseFilterFormBody(
                            draft = draft,
                            suppliers = suppliers,
                            isTablet = false,
                            today = today,
                            onDraftChange = {},
                            onOpenDatePicker = {}
                        )
                    }
                    FilterFooter(
                        isTablet = false,
                        onReset = {},
                        onApply = {},
                        resetTestTag = "purchase-history-filter-reset",
                        applyTestTag = "purchase-history-filter-done"
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Barokah Jaya Filter Pembelian - Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
internal fun OwnerPurchaseFilterPhonePreview() {
    TbterminalappTheme {
        PurchaseFilterReferenceFrame(isTablet = false)
    }
}

@Preview(
    name = "Barokah Jaya Filter Pembelian - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerPurchaseFilterTabletPreview() {
    TbterminalappTheme {
        PurchaseFilterReferenceFrame(isTablet = true)
    }
}
