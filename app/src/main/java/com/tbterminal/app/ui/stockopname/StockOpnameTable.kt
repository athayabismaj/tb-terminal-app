package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun StockOpnameTableCard(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StockTableToolbar(uiState, onSearchChanged, onCategoryFilterChanged, compact)
        Spacer(modifier = Modifier.height(if (compact) 12.dp else 28.dp))
        if (!compact) StockTableHeader()
        StockTableRows(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onSelectProduct = onSelectProduct,
            compact = compact
        )
        StockTableFooter(uiState, compact, onPreviousPage, onNextPage)
    }
}

@Composable
private fun StockTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OpnameSoft)
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
    val controls: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TablePageButton(uiState.tablePage > 1 && !uiState.isLoading, onPreviousPage, Icons.Default.ChevronLeft)
            Text("${uiState.tablePage} / ${uiState.totalTablePages}", color = OpnameText, fontWeight = FontWeight.Bold)
            TablePageButton(uiState.tablePage < uiState.totalTablePages && !uiState.isLoading, onNextPage, Icons.Default.ChevronRight)
        }
    }
    if (compact) Column(Modifier.fillMaxWidth().background(OpnameSoft).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${uiState.totalTableProducts} produk", color = OpnameMuted, fontSize = 12.sp)
        controls()
    } else Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OpnameSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Menampilkan ${uiState.tableStartIndex}-${uiState.tableEndIndex} dari ${uiState.totalTableProducts} produk",
                color = OpnameText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Maksimal $STOCK_OPNAME_TABLE_PAGE_SIZE produk per halaman",
                color = OpnameMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        controls()
    }
}

@Composable
private fun TablePageButton(
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpnameText)
    ) {
        Icon(icon, contentDescription = null)
    }
}
