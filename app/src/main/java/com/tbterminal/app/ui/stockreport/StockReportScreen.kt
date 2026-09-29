package com.tbterminal.app.ui.stockreport

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.StockMovement
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
import com.tbterminal.app.ui.components.TbPeriodFilterRow
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val StockReportBackground = TbBackground
private val StockReportSurface = Color.White
private val StockReportBorder = TbOutline
private val StockReportText = TbText
private val StockReportMuted = TbTextMuted
private val StockReportPrimary = TbGreenDark
private val StockReportSoft = TbSurfaceMuted
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)
        ) {
            StockReportToolbar(
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onCategoryFilterChanged = onCategoryFilterChanged,
                onMovementPeriodSelected = onMovementPeriodSelected,
                onMovementDateSelected = onMovementDateSelected,
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Daftar stok",
                    modifier = Modifier.weight(1f),
                    color = StockReportText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
            Spacer(Modifier.height(8.dp))
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
            color = StockReportSoft.copy(alpha = 0.62f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = StockReportSurface, shape = RoundedCornerShape(12.dp)) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Inventory2, null, tint = StockReportPrimary, modifier = Modifier.size(21.dp))
                        }
                    }
                    Text("Kondisi stok", color = StockReportText, fontWeight = FontWeight.SemiBold)
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryMetric("Total produk", uiState.totalProducts.toString(), StockReportPrimary, Modifier.weight(1f))
                            SummaryMetric("Nilai stok", formatCurrency(uiState.pageStockValue), StockReportText, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryMetric("Stok menipis", uiState.pageLowStockCount.toString(), StockReportWarning, Modifier.weight(1f))
                            SummaryMetric("Stok habis", uiState.pageOutOfStockCount.toString(), StockReportDanger, Modifier.weight(1f))
                        }
                    }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryMetric("Total produk", uiState.totalProducts.toString(), StockReportPrimary, Modifier.weight(1f))
                    SummaryMetric("Nilai stok halaman", formatCurrency(uiState.pageStockValue), StockReportText, Modifier.weight(1.35f))
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
        Text(title, color = StockReportMuted, fontSize = 11.sp, maxLines = 1)
        Text(value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StockReportToolbar(
    uiState: StockReportUiState,
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
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        Surface(
            modifier = fieldModifier.height(52.dp).testTag("stock-report-search"),
            color = StockReportSoft.copy(alpha = 0.58f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, StockReportBorder.copy(alpha = 0.42f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Icon(Icons.Outlined.Search, "Cari produk", tint = StockReportMuted, modifier = Modifier.size(20.dp))
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = StockReportText),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (uiState.searchQuery.isBlank()) {
                                Text("Cari nama atau SKU", color = StockReportMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        search(Modifier.weight(1f))
        Surface(
            onClick = {
                pendingCategory = uiState.categoryFilter
                pendingPeriod = uiState.movementPeriod
                pendingDate = uiState.customMovementDate
                showFilterSheet = true
            },
            modifier = Modifier.size(48.dp).testTag("stock-report-open-filter"),
            shape = RoundedCornerShape(14.dp),
            color = com.tbterminal.app.ui.theme.TbGreenLight,
            contentColor = com.tbterminal.app.ui.theme.TbGreenDark,
            border = BorderStroke(1.dp, StockReportBorder.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter kategori dan tanggal mutasi",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    if (showFilterSheet) {
        TbMobileControlSheet(
            title = "Filter stok",
            subtitle = "Kategori produk dan tanggal mutasi",
            onDismiss = {
                showFilterSheet = false
                showDatePicker = false
            },
            testTag = "stock-report-filter-sheet",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kategori", color = StockReportText, fontWeight = FontWeight.SemiBold)
                StockCategoryDropdown(
                    selectedCategory = pendingCategory,
                    categories = uiState.categoryOptions,
                    onCategoryFilterChanged = { pendingCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Periode mutasi", color = StockReportText, fontWeight = FontWeight.SemiBold)
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
                    Text("Reset filter", color = StockReportPrimary)
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
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("stock-report-category"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, StockReportBorder),
        ) {
            Text(
                text = selectedCategory ?: "Semua kategori",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = StockReportText,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Outlined.ExpandMore, "Pilih kategori", tint = StockReportMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .heightIn(max = 320.dp)
                .background(StockReportSurface)
        ) {
            DropdownMenuItem(text = { Text("Semua kategori") }, onClick = {
                expanded = false
                onCategoryFilterChanged(null)
            })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = {
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
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(StockReportSurface),
            border = BorderStroke(1.dp, StockReportBorder.copy(alpha = 0.72f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                if (!compact) StockTableHeader()
                when {
                    uiState.isLoading -> com.tbterminal.app.ui.components.SkeletonList(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        itemCount = 4,
                    )
                    uiState.errorMessage != null -> StockError(uiState.errorMessage)
                    uiState.visibleStocks.isEmpty() -> StockEmptyState(insideCard = true)
                    compact -> uiState.visibleStocks.forEachIndexed { index, stock ->
                        StockMobileRow(stock = stock, onProductSelected = onProductSelected)
                        if (index < uiState.visibleStocks.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = StockReportBorder.copy(alpha = 0.58f),
                            )
                        }
                    }
                    else -> uiState.visibleStocks.forEachIndexed { index, stock ->
                            StockRow(stock, index % 2 != 0, onProductSelected)
                    }
                }
            }
        }
        if (!uiState.isLoading && uiState.errorMessage == null && uiState.totalProducts > 0) {
            StockPagination(uiState, onPreviousPage, onNextPage)
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("stock-report-product-${stock.productId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stock.productName, color = StockReportText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${stock.sku} · ${stock.categoryName}", color = StockReportMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(modifier = Modifier.padding(top = 2.dp, start = 14.dp)) {
                StockStatusBadge(status.first, status.second, showChevron = true)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            StockValue("Stok", "${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(4f))
            StockValue("Minimum", formatQuantity(stock.minStock), Modifier.weight(3f), Alignment.CenterHorizontally)
            StockValue("Nilai stok", formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(5f), Alignment.End)
        }
    }
}

@Composable
private fun StockValue(label: String, value: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = StockReportMuted, fontSize = 11.sp, maxLines = 1)
        Text(value, color = StockReportText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StockEmptyState(insideCard: Boolean = false) {
    if (insideCard) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("Belum ada data stok.", color = StockReportMuted)
        }
    } else Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        color = StockReportSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StockReportBorder.copy(alpha = 0.72f)),
    ) {
        Box(contentAlignment = Alignment.Center) { Text("Belum ada data stok.", color = StockReportMuted) }
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
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
    Row(Modifier.fillMaxWidth().background(StockReportSoft.copy(alpha = 0.7f)).padding(horizontal = 20.dp, vertical = 12.dp)) {
        StockLabel("PRODUK", Modifier.weight(2.4f))
        StockLabel("KATEGORI", Modifier.weight(1.3f))
        StockLabel("STOK", Modifier.weight(0.9f))
        StockLabel("MINIMUM", Modifier.weight(0.9f))
        StockLabel("NILAI STOK", Modifier.weight(1.2f))
        StockLabel("STATUS", Modifier.weight(1.25f))
    }
}

@Composable
private fun StockRow(stock: ProductStock, useAlternateBackground: Boolean, onProductSelected: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onProductSelected(stock.productId) }
            .background(if (useAlternateBackground) StockReportSoft.copy(alpha = 0.76f) else StockReportSurface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(2.4f)) {
            Text(stock.productName, color = StockReportText, fontWeight = FontWeight.Bold)
            Text(stock.sku, color = StockReportMuted, fontSize = 12.sp)
        }
        Text(stock.categoryName, Modifier.weight(1.3f), color = StockReportMuted, fontSize = 13.sp)
        Text("${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(0.9f), color = StockReportText, fontSize = 13.sp)
        Text(formatQuantity(stock.minStock), Modifier.weight(0.9f), color = StockReportMuted, fontSize = 13.sp)
        Text(formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(1.2f), color = StockReportText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        val status = stockStatus(stock)
        Box(modifier = Modifier.weight(1.25f), contentAlignment = Alignment.CenterStart) {
            StockStatusBadge(status.first, status.second, showChevron = true)
        }
    }
    HorizontalDivider(color = StockReportBorder)
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
                    color = StockReportSoft.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stock.categoryName, modifier = Modifier.weight(1f), color = StockReportMuted, fontSize = 12.sp)
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
                        Text("Aktivitas stok", color = StockReportText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        val periodLabel = movementPeriodLabel(uiState.movementPeriod, uiState.customMovementDate)
                        if (periodLabel != null) {
                            Text(periodLabel, color = StockReportMuted, fontSize = 12.sp)
                        }
                    }
                    if (uiState.cardReconciled != null) {
                        LedgerStatusBadge(uiState.cardReconciled)
                    }
                }
            }
            when {
                uiState.isCardLoading -> item {
                    com.tbterminal.app.ui.components.SkeletonList(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        itemCount = 2,
                    )
                }
                uiState.cardErrorMessage != null -> item {
                    Text(uiState.cardErrorMessage, color = StockReportDanger)
                }
                uiState.stockMovements.isEmpty() -> item {
                    LedgerEmptyState("Belum ada mutasi untuk produk ini.")
                }
                else -> items(uiState.stockMovements, key = StockMovement::id) { movement ->
                    StockMovementMobileRow(movement)
                }
            }
            item {
                TbMobileSheetDoneButton(onClick = onDismiss, testTag = "stock-report-detail-done")
            }
        }
    }
}

@Composable
private fun LedgerStatusBadge(reconciled: Boolean?) {
    val success = reconciled == true
    Surface(
        color = (if (success) StockReportPrimary else StockReportDanger).copy(alpha = 0.10f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            if (success) "Saldo sesuai" else "Perlu diperiksa",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = if (success) StockReportPrimary else StockReportDanger,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LedgerEmptyState(message: String) {
    Surface(color = StockReportSoft.copy(alpha = 0.65f), shape = RoundedCornerShape(14.dp)) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            color = StockReportMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StockMovementMobileRow(row: StockMovement) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StockReportSoft.copy(alpha = 0.58f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(movementTypeLabel(row.type), color = StockReportText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(row.occurredAt.take(19).replace('T', ' '), color = StockReportMuted, fontSize = 11.sp)
                }
                Text(formatQuantity(row.balanceAfter), color = StockReportText, fontWeight = FontWeight.Bold)
            }
            Text(row.referenceNumber ?: movementReferenceLabel(row.referenceType), color = StockReportMuted, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MovementValue("Sebelum", formatQuantity(row.balanceBefore), StockReportMuted, Alignment.Start)
                val (deltaLabel, deltaValue, deltaColor) = when {
                    row.qtyIn > BigDecimal.ZERO -> Triple("Masuk", "+${formatQuantity(row.qtyIn)}", StockReportPrimary)
                    row.qtyOut > BigDecimal.ZERO -> Triple("Keluar", "-${formatQuantity(row.qtyOut)}", StockReportDanger)
                    else -> Triple("Perubahan", "0", StockReportMuted)
                }
                MovementValue(deltaLabel, deltaValue, deltaColor, Alignment.CenterHorizontally)
                MovementValue("Sesudah", formatQuantity(row.balanceAfter), StockReportText, Alignment.End)
            }
        }
    }
}

@Composable
private fun MovementValue(label: String, value: String, tint: Color, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = StockReportMuted, fontSize = 10.sp)
        Text(value, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StockError(message: String) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = StockReportDanger)
    }
}

@Composable
private fun StockPagination(uiState: StockReportUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    val safePage = uiState.page.coerceAtLeast(1)
    val safeTotalPages = uiState.totalPages.coerceAtLeast(1)

    TbPagination(
        currentPage = safePage,
        totalPages = safeTotalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        isLoading = uiState.isLoading,
        testTag = "stock-report-pagination",
    )
}

@Composable
private fun StockLabel(text: String, modifier: Modifier) {
    Text(text, modifier, color = StockReportMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

private fun stockStatus(stock: ProductStock): Pair<String, Color> {
    return when {
        stock.quantity <= BigDecimal.ZERO -> "Habis" to StockReportDanger
        stock.quantity <= stock.minStock -> "Menipis" to StockReportWarning
        else -> "Aman" to StockReportPrimary
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
