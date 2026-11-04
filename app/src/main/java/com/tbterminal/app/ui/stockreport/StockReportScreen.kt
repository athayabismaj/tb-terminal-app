package com.tbterminal.app.ui.stockreport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
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
        val isFiltered = uiState.categoryFilter != null
        val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

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
                if (compact) {
                    // Expandable Search Bar (Riwayat Pembelian style)
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
                                            "Cari produk di kartu stok...",
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
                                            .testTag("stock-report-search"),
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

                    // Active Filter Chips
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
                                    Icon(Icons.Outlined.Category, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = uiState.categoryFilter.orEmpty(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { onCategoryFilterChanged(null) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
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
                                    .clickable { onCategoryFilterChanged(null) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    StockReportToolbar(
                        uiState = uiState,
                        compact = compact,
                        onSearchChanged = onSearchChanged,
                        onCategoryFilterChanged = onCategoryFilterChanged,
                        onMovementPeriodSelected = onMovementPeriodSelected,
                        onMovementDateSelected = onMovementDateSelected,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Daftar stok",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
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
        if (showFilterPopup) {
            StockCategoryFilterBottomSheet(
                uiState = uiState,
                onDismiss = onDismissFilter,
                onCategorySelected = onCategoryFilterChanged
            )
        }
        if (showSummary || showSummaryPopup) {
            StockSummarySheet(
                uiState = uiState,
                onDismiss = {
                    showSummary = false
                    onDismissSummary()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockCategoryFilterBottomSheet(
    uiState: StockReportUiState,
    onDismiss: () -> Unit,
    onCategorySelected: (String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCat by remember(uiState.categoryFilter) { mutableStateOf(uiState.categoryFilter) }
    var categoryQuery by remember { mutableStateOf("") }
    var isCategorySearchVisible by remember { mutableStateOf(false) }

    val rawCategories = uiState.categoryOptions
    val filteredCategories = if (categoryQuery.isBlank()) {
        rawCategories
    } else {
        rawCategories.filter { it.contains(categoryQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filter Kartu Stok",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pilih kategori untuk menyaring stok barang",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
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
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Kategori Header & Search Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KATEGORI PRODUK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF64748B)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${rawCategories.size} Kategori",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isCategorySearchVisible) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                            .clickable {
                                isCategorySearchVisible = !isCategorySearchVisible
                                if (!isCategorySearchVisible) categoryQuery = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Cari kategori",
                            tint = if (isCategorySearchVisible || categoryQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expandable Category Search Input
            AnimatedVisibility(
                visible = isCategorySearchVisible || categoryQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (categoryQuery.isEmpty()) {
                                Text(
                                    text = "Cari kategori...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            BasicTextField(
                                value = categoryQuery,
                                onValueChange = { categoryQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (categoryQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { categoryQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus teks",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Category options grid
            val allCategoryItems: List<String?> = listOf<String?>(null) + filteredCategories
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    allCategoryItems.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { cat ->
                                val isSelected = if (cat == null) selectedCat == null else selectedCat == cat
                                Surface(
                                    onClick = { selectedCat = cat },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = if (isSelected) 1.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat ?: "Semua Kategori",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF256B57),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedCat = null
                        onCategorySelected(null)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF256B57)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF256B57))
                ) {
                    Text("Reset", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onCategorySelected(selectedCat)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Terapkan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockSummarySheet(
    uiState: StockReportUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Stok",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TOTAL PRODUK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${uiState.totalProducts} Produk",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF256B57)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 1: Nilai Stok Halaman
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nilai Stok Halaman",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = formatCurrency(uiState.pageStockValue),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 2: Stok Menipis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok Menipis",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                    )
                    Text(
                        text = "${uiState.pageLowStockCount} Produk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 3: Stok Habis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok Habis",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                    Text(
                        text = "${uiState.pageOutOfStockCount} Produk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Row 4: Total Kategori
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "${uiState.categoryOptions.size} Kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Selesai Button
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("stock-report-product-${stock.productId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stock.productName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StockStatusBadge(status.first, status.second)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockValue("Stok", "${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(1f))
            StockValue("Minimum", "${formatQuantity(stock.minStock)} ${stock.unitName}", Modifier.weight(1f), Alignment.CenterHorizontally)
            StockValue("Nilai stok", formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(1.2f), Alignment.End)
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
private fun StockStatusBadge(label: String, color: Color) {
    val (bgColor, textColor) = when (label.lowercase()) {
        "habis" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        "menipis" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        "aman" -> Color(0xFFEAF3EF) to Color(0xFF256B57)
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
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
            StockStatusBadge(status.first, status.second)
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
