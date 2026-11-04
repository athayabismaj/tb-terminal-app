package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.components.TbPagination

private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val BorderSlate200 = Color(0xFFE2E8F0)

@Composable
internal fun StockOpnameTableCard(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenForm: () -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        StockTableToolbar(uiState, onSearchChanged, onCategoryFilterChanged, onOpenForm, compact)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Daftar produk",
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
                        text = "${uiState.totalTableProducts} produk",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().testTag("stock-adjustment-list-card"),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BorderSlate200),
        ) {
            Column {
                if (!compact && !uiState.isLoading && uiState.tableProducts.isNotEmpty()) {
                    StockTableHeader()
                    HorizontalDivider(color = BorderSlate200)
                }
                StockTableRows(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onSelectProduct = onSelectProduct,
                    compact = compact,
                )
            }
        }

        if (!uiState.isLoading && uiState.totalTableProducts > 0) {
            StockTableFooter(uiState, compact, onPreviousPage, onNextPage)
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
        HeaderText("PRODUK", Modifier.weight(2.45f).padding(end = 16.dp))
        HeaderText("STOK SISTEM", Modifier.weight(1.15f).padding(horizontal = 8.dp), Alignment.End)
        HeaderText("STOK FISIK", Modifier.weight(1.15f).padding(horizontal = 8.dp), Alignment.End)
        HeaderText("SELISIH", Modifier.weight(1.05f).padding(horizontal = 8.dp), Alignment.End)
        HeaderText("STATUS", Modifier.weight(1.35f).padding(horizontal = 10.dp), Alignment.CenterHorizontally)
        HeaderText("ALASAN", Modifier.weight(1.15f).padding(start = 10.dp))
    }
}

@Composable
private fun StockTableFooter(
    uiState: StockOpnameUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val safePage = uiState.tablePage.coerceAtLeast(1)
    val safeTotalPages = uiState.totalTablePages.coerceAtLeast(1)
    val first = if (uiState.totalTableProducts == 0) 0 else (safePage - 1) * STOCK_OPNAME_TABLE_PAGE_SIZE + 1
    val last = minOf(safePage * STOCK_OPNAME_TABLE_PAGE_SIZE, uiState.totalTableProducts)
    val text = if (compact) {
        "${uiState.totalTableProducts} produk"
    } else {
        "Menampilkan $first–$last dari ${uiState.totalTableProducts} produk"
    }

    TbPagination(
        currentPage = safePage,
        totalPages = safeTotalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = text,
        isLoading = uiState.isLoading,
        compact = compact,
        testTag = "stock-adjustment-pagination",
    )
}
