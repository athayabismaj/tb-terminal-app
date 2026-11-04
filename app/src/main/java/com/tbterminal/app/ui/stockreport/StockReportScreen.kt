package com.tbterminal.app.ui.stockreport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.StockMovement
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.ui.components.TbPeriodFilterRow
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrandGreen = Color(0xFF256B57)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val TextMuted = Color(0xFF94A3B8)
private val BorderSlate200 = Color(0xFFE2E8F0)
private val BorderSlate300 = Color(0xFFCBD5E1)

private val StockReportBackground = TbBackground
private val StockReportDanger = TbError
private val StockReportWarning = TbAmber

@Composable
internal fun StockReportScreen(
    modifier: Modifier,
    uiState: StockReportUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onProductSelected: (String) -> Unit,
    onMovementPeriodSelected: (StockMovementPeriod) -> Unit,
    onMovementDateSelected: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    var detailProductId by remember { mutableStateOf<String?>(null) }
    var showSummary by remember { mutableStateOf(false) }
    val detailProduct = uiState.visibleStocks.firstOrNull { it.productId == detailProductId }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(StockReportBackground).testTag("stock-report-screen")) {
        val compact = maxWidth < 700.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 12.dp else 20.dp
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1280.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
            ) {
                StockReportToolbar(
                    uiState = uiState,
                    compact = compact,
                    onSearchChanged = onSearchChanged,
                    onCategoryFilterChanged = onCategoryFilterChanged,
                    onMovementPeriodSelected = onMovementPeriodSelected,
                    onMovementDateSelected = onMovementDateSelected,
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daftar stok",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, BorderSlate200),
                        ) {
                            Text(
                                text = "${uiState.totalProducts} produk",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                            )
                        }
                    }
                    TbMobileSummaryButton(
                        onClick = { showSummary = true },
                        testTag = "stock-report-open-summary",
                    )
                }

                StockTableCard(
                    uiState = uiState,
                    compact = compact,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    onProductSelected = { productId ->
                        detailProductId = productId
                        onProductSelected(productId)
                    },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (detailProduct != null) {
            StockMovementSheet(
                stock = detailProduct,
                uiState = uiState,
                onDismiss = { detailProductId = null },
            )
        }
        if (showSummary) {
            StockSummarySheet(
                uiState = uiState,
                compact = compact,
                onDismiss = { showSummary = false },
            )
        }
    }
}

@Composable
private fun StockSummarySheet(
    uiState: StockReportUiState,
    compact: Boolean,
    onDismiss: () -> Unit,
) {
    TbMobileControlSheet(
        title = "Ringkasan stok",
        subtitle = "Nilai dan kondisi stok pada halaman ini",
        onDismiss = onDismiss,
        testTag = "stock-report-summary-sheet",
    ) {
        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderSlate200),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Inventory2, null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                    }
                    Text("Kondisi stok", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryMetric("Total produk", uiState.totalProducts.toString(), BrandGreen, Modifier.weight(1f))
                            SummaryMetric("Nilai stok", formatCurrency(uiState.pageStockValue), TextPrimary, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryMetric("Stok menipis", uiState.pageLowStockCount.toString(), StockReportWarning, Modifier.weight(1f))
                            SummaryMetric("Stok habis", uiState.pageOutOfStockCount.toString(), StockReportDanger, Modifier.weight(1f))
                        }
                    }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryMetric("Total produk", uiState.totalProducts.toString(), BrandGreen, Modifier.weight(1f))
                    SummaryMetric("Nilai stok halaman", formatCurrency(uiState.pageStockValue), TextPrimary, Modifier.weight(1.35f))
                    SummaryMetric("Stok menipis", uiState.pageLowStockCount.toString(), StockReportWarning, Modifier.weight(1f))
                    SummaryMetric("Stok habis", uiState.pageOutOfStockCount.toString(), StockReportDanger, Modifier.weight(1f))
                }
            }
        }
        TbMobileSheetDoneButton(onClick = onDismiss, testTag = "stock-report-summary-done")
    }
}

@Composable
private fun SummaryMetric(title: String, value: String, tint: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        Text(value, color = tint, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StockReportToolbar(
    uiState: StockReportUiState,
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onMovementPeriodSelected: (StockMovementPeriod) -> Unit,
    onMovementDateSelected: (String) -> Unit,
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingCategory by remember { mutableStateOf<String?>(null) }
    var pendingPeriod by remember { mutableStateOf(StockMovementPeriod.ALL) }
    var pendingDate by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()

    val isFiltered = uiState.categoryFilter != null || uiState.movementPeriod != StockMovementPeriod.ALL

    if (uiState.isLoading && uiState.visibleStocks.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().testTag("stock-report-toolbar"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(
                modifier = Modifier.weight(1f).height(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
            SkeletonBox(
                modifier = Modifier.size(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().testTag("stock-report-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StockSearchField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.weight(1f),
            compact = compact,
        )
        Surface(
            onClick = {
                pendingCategory = uiState.categoryFilter
                pendingPeriod = uiState.movementPeriod
                pendingDate = uiState.customMovementDate
                showFilterSheet = true
            },
            modifier = Modifier
                .size(if (compact) 42.dp else 48.dp)
                .testTag("stock-report-open-filter"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (isFiltered) Color(0xFF0F172A) else BorderSlate300),
            contentColor = Color(0xFF0F172A),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter kategori dan tanggal mutasi",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF0F172A),
                )
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        TbMobileControlSheet(
            title = "Filter stok",
            subtitle = "Kategori produk dan periode mutasi",
            onDismiss = {
                showFilterSheet = false
                showDatePicker = false
            },
            testTag = "stock-report-filter-sheet",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kategori", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                StockCategoryDropdown(
                    selectedCategory = pendingCategory,
                    categories = uiState.categoryOptions,
                    onCategoryFilterChanged = { pendingCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Periode mutasi", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                TbPeriodFilterRow(
                    selectedValue = pendingPeriod.takeIf {
                        it == StockMovementPeriod.DAY || it == StockMovementPeriod.WEEK || it == StockMovementPeriod.MONTH
                    },
                    presets = listOf(
                        StockMovementPeriod.DAY to "Hari",
                        StockMovementPeriod.WEEK to "Minggu",
                        StockMovementPeriod.MONTH to "Bulan",
                    ),
                    dateLabel = (pendingDate ?: today).let { raw ->
                        runCatching {
                            LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("id-ID")))
                        }.getOrDefault(raw)
                    },
                    dateSelected = pendingPeriod == StockMovementPeriod.DATE,
                    onPresetSelected = { period ->
                        pendingPeriod = period
                        pendingDate = null
                    },
                    onDateClick = { showDatePicker = true },
                    testTag = "stock-movement-period",
                    presetTestTags = listOf("stock-movement-day", "stock-movement-week", "stock-movement-month"),
                    dateTestTag = "stock-movement-date",
                )
            }
            if (pendingCategory != null || pendingPeriod != StockMovementPeriod.ALL) {
                TextButton(onClick = {
                    pendingCategory = null
                    pendingPeriod = StockMovementPeriod.ALL
                    pendingDate = null
                }) {
                    Text("Reset filter", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                }
            }
            TbMobileSheetDoneButton(
                onClick = {
                    onCategoryFilterChanged(pendingCategory)
                    val selectedDate = pendingDate
                    if (pendingPeriod == StockMovementPeriod.DATE && selectedDate != null) {
                        onMovementDateSelected(selectedDate)
                    } else {
                        onMovementPeriodSelected(pendingPeriod)
                    }
                    showFilterSheet = false
                    showDatePicker = false
                },
                testTag = "stock-report-filter-done",
            )
        }
    }
    if (showDatePicker && showFilterSheet) {
        HistoryDatePickerDialog(
            currentDate = pendingDate ?: today,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                pendingDate = date
                pendingPeriod = StockMovementPeriod.DATE
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun StockSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(if (compact) 42.dp else 48.dp)
            .testTag("stock-report-search"),
        color = Color.White,
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        border = BorderStroke(1.dp, BorderSlate300),
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
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = "Cari nama produk atau SKU...",
                        fontSize = 14.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Hapus pencarian",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StockCategoryDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("stock-report-category"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderSlate300),
        ) {
            Text(
                text = selectedCategory ?: "Semua kategori",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Icon(Icons.Outlined.ExpandMore, "Pilih kategori", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(240.dp)
                .heightIn(max = 320.dp)
                .background(Color.White, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderSlate200),
        ) {
            DropdownMenuItem(text = { Text("Semua kategori", fontSize = 13.sp) }, onClick = {
                expanded = false
                onCategoryFilterChanged(null)
            })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category, fontSize = 13.sp) }, onClick = {
                    expanded = false
                    onCategoryFilterChanged(category)
                })
            }
        }
    }
}

@Composable
private fun StockTableCard(
    uiState: StockReportUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onProductSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            uiState.isLoading && uiState.visibleStocks.isEmpty() -> StockLoadingCard(compact = compact)
            uiState.errorMessage != null -> StockError(uiState.errorMessage)
            uiState.visibleStocks.isEmpty() -> StockEmptyState()
            compact -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("stock-report-list"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderSlate200),
                ) {
                    Column {
                        uiState.visibleStocks.forEachIndexed { index, stock ->
                            StockMobileRow(stock = stock, onProductSelected = onProductSelected)
                            if (index < uiState.visibleStocks.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
            else -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("stock-report-table"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderSlate200),
                ) {
                    Column {
                        StockTableHeader()
                        HorizontalDivider(color = BorderSlate200)
                        uiState.visibleStocks.forEachIndexed { index, stock ->
                            StockRow(stock, onProductSelected)
                            if (index < uiState.visibleStocks.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.errorMessage == null && uiState.totalProducts > 0L) {
            StockPagination(uiState, compact, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun StockMobileRow(stock: ProductStock, onProductSelected: (String) -> Unit) {
    val status = stockStatus(stock)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductSelected(stock.productId) }
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag("stock-report-product-${stock.productId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stock.productName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stock.sku} · ${stock.categoryName}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(modifier = Modifier.padding(top = 2.dp, start = 12.dp)) {
                StockStatusBadge(status.first, status.second, showChevron = true)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            StockValue("Stok", "${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(4f))
            StockValue("Minimum", formatQuantity(stock.minStock), Modifier.weight(3f), Alignment.CenterHorizontally)
            StockValue("Nilai stok", formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(5f), Alignment.End)
        }
    }
}

@Composable
private fun StockValue(label: String, value: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StockEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Belum ada data stok",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = "Tidak ada produk yang cocok dengan pencarian atau filter saat ini.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StockLoadingCard(compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("stock-report-skeleton"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column {
            if (!compact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(Modifier.width(100.dp).height(12.dp))
                    Spacer(Modifier.weight(2.4f))
                    SkeletonBox(Modifier.width(70.dp).height(12.dp))
                    Spacer(Modifier.weight(1.3f))
                    SkeletonBox(Modifier.width(40.dp).height(12.dp))
                    Spacer(Modifier.weight(0.9f))
                    SkeletonBox(Modifier.width(80.dp).height(12.dp))
                }
                HorizontalDivider(color = BorderSlate200)
            }

            repeat(if (compact) 4 else 5) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(if (compact) 1f else 2.4f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(if (compact) 0.7f else 0.5f).height(14.dp))
                        SkeletonBox(Modifier.width(80.dp).height(11.dp))
                    }
                    if (!compact) {
                        SkeletonBox(Modifier.weight(1.3f).height(13.dp))
                        SkeletonBox(Modifier.weight(0.9f).height(13.dp))
                        SkeletonBox(Modifier.weight(0.9f).height(13.dp))
                        SkeletonBox(Modifier.weight(1.2f).height(13.dp))
                        SkeletonBox(Modifier.weight(1.25f).height(24.dp), shape = RoundedCornerShape(999.dp))
                    } else {
                        SkeletonBox(Modifier.width(60.dp).height(24.dp), shape = RoundedCornerShape(999.dp))
                    }
                }
                if (index < (if (compact) 3 else 4)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun StockStatusBadge(label: String, color: Color, showChevron: Boolean = false) {
    Surface(
        color = color.copy(alpha = 0.11f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = if (showChevron) 8.dp else 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StockTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StockLabel("PRODUK", Modifier.weight(2.4f))
        StockLabel("KATEGORI", Modifier.weight(1.3f))
        StockLabel("STOK", Modifier.weight(0.9f))
        StockLabel("MINIMUM", Modifier.weight(0.9f))
        StockLabel("NILAI STOK", Modifier.weight(1.2f))
        StockLabel("STATUS", Modifier.weight(1.25f))
    }
}

@Composable
private fun StockRow(stock: ProductStock, onProductSelected: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onProductSelected(stock.productId) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(2.4f)) {
            Text(stock.productName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(stock.sku, color = TextSecondary, fontSize = 12.sp)
        }
        Text(stock.categoryName, Modifier.weight(1.3f), color = Color(0xFF475569), fontSize = 13.sp)
        Text("${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(0.9f), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(formatQuantity(stock.minStock), Modifier.weight(0.9f), color = TextSecondary, fontSize = 13.sp)
        Text(formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(1.2f), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        val status = stockStatus(stock)
        Box(modifier = Modifier.weight(1.25f), contentAlignment = Alignment.CenterStart) {
            StockStatusBadge(status.first, status.second, showChevron = true)
        }
    }
}

@Composable
private fun StockMovementSheet(
    stock: ProductStock,
    uiState: StockReportUiState,
    onDismiss: () -> Unit,
) {
    val status = stockStatus(stock)
    TbMobileControlSheet(
        title = "Riwayat mutasi",
        subtitle = "${stock.productName} · ${stock.sku}",
        onDismiss = onDismiss,
        testTag = "stock-report-movement-sheet",
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSlate200),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stock.categoryName, modifier = Modifier.weight(1f), color = TextSecondary, fontSize = 12.sp)
                            StockStatusBadge(status.first, status.second)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StockValue("Stok", "${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(1f))
                            StockValue("Minimum", formatQuantity(stock.minStock), Modifier.weight(1f), Alignment.CenterHorizontally)
                            StockValue("Nilai stok", formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(1.35f), Alignment.End)
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktivitas stok", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        val periodLabel = movementPeriodLabel(uiState.movementPeriod, uiState.customMovementDate)
                        if (periodLabel != null) {
                            Text(periodLabel, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    if (uiState.cardReconciled != null) {
                        LedgerStatusBadge(uiState.cardReconciled)
                    }
                }
            }
            if (uiState.isCardLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        com.tbterminal.app.ui.components.SkeletonList(Modifier.fillMaxWidth(), itemCount = 3)
                    }
                }
            } else if (uiState.cardErrorMessage != null) {
                item {
                    Text(uiState.cardErrorMessage, color = StockReportDanger, modifier = Modifier.padding(16.dp))
                }
            } else if (uiState.stockMovements.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada mutasi stok.", color = TextSecondary)
                    }
                }
            } else {
                items(uiState.stockMovements) { movement ->
                    StockMovementMobileRow(movement)
                }
            }
        }
        TbMobileSheetDoneButton(onClick = onDismiss, testTag = "stock-report-movement-done")
    }
}

@Composable
private fun StockMovementMobileRow(row: StockMovement) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(movementTypeLabel(row.type), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(row.occurredAt.take(19).replace('T', ' '), color = TextSecondary, fontSize = 11.sp)
                }
                Text(formatQuantity(row.balanceAfter), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Text(row.referenceNumber ?: movementReferenceLabel(row.referenceType), color = TextSecondary, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MovementValue("Sebelum", formatQuantity(row.balanceBefore), TextSecondary, Alignment.Start)
                val (deltaLabel, deltaValue, deltaColor) = when {
                    row.qtyIn > BigDecimal.ZERO -> Triple("Masuk", "+${formatQuantity(row.qtyIn)}", BrandGreen)
                    row.qtyOut > BigDecimal.ZERO -> Triple("Keluar", "-${formatQuantity(row.qtyOut)}", StockReportDanger)
                    else -> Triple("Perubahan", "0", TextSecondary)
                }
                MovementValue(deltaLabel, deltaValue, deltaColor, Alignment.CenterHorizontally)
                MovementValue("Sesudah", formatQuantity(row.balanceAfter), TextPrimary, Alignment.End)
            }
        }
    }
}

@Composable
private fun MovementValue(label: String, value: String, tint: Color, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(value, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LedgerStatusBadge(reconciled: Boolean) {
    val color = if (reconciled) BrandGreen else Color(0xFFEF4444)
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = if (reconciled) "Rekonsiliasi Sesuai" else "Selisih Saldo",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StockError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF1F2),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text(message, color = Color(0xFFE11D48), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StockPagination(
    uiState: StockReportUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val safePage = uiState.page.coerceAtLeast(1)
    val safeTotalPages = uiState.totalPages.coerceAtLeast(1)
    val first = if (uiState.totalProducts == 0L) 0L else (safePage - 1L) * uiState.pageSize + 1L
    val last = minOf(safePage.toLong() * uiState.pageSize, uiState.totalProducts)
    val text = if (compact) {
        "${uiState.totalProducts} produk"
    } else {
        "Menampilkan $first–$last dari ${uiState.totalProducts} produk"
    }

    TbPagination(
        currentPage = safePage,
        totalPages = safeTotalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = text,
        isLoading = uiState.isLoading,
        compact = compact,
        testTag = "stock-report-pagination",
    )
}

@Composable
private fun StockLabel(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
    )
}

private fun stockStatus(stock: ProductStock): Pair<String, Color> {
    return when {
        stock.quantity <= BigDecimal.ZERO -> "Habis" to Color(0xFFE11D48)
        stock.quantity <= stock.minStock -> "Menipis" to Color(0xFFD97706)
        else -> "Aman" to BrandGreen
    }
}

private fun movementTypeLabel(type: String): String = when (type.uppercase()) {
    "OPENING_BALANCE" -> "Saldo awal"
    "PURCHASE", "INCOMING_GOODS" -> "Barang masuk"
    "SALE" -> "Penjualan"
    "VOID", "SALE_VOID" -> "Pembatalan penjualan"
    "STOCK_OPNAME" -> "Stok opname"
    "CORRECTION", "ADJUSTMENT" -> "Koreksi stok"
    "DAMAGED", "DAMAGE" -> "Barang rusak"
    "RETURN", "SALES_RETURN" -> "Retur"
    else -> type.replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase)
}

private fun movementReferenceLabel(referenceType: String): String =
    referenceType.replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase)

private fun movementPeriodLabel(period: StockMovementPeriod, date: String?): String? = when (period) {
    StockMovementPeriod.ALL -> null
    StockMovementPeriod.DAY -> "Hari ini"
    StockMovementPeriod.WEEK -> "Minggu ini"
    StockMovementPeriod.MONTH -> "Bulan ini"
    StockMovementPeriod.DATE -> date?.let { raw ->
        runCatching {
            LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
        }.getOrDefault(raw)
    }
}

private fun formatQuantity(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

private fun formatCurrency(value: BigDecimal): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(value)
}
