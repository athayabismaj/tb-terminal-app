package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun StockOpnameTableCard(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onRefresh: () -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = OpnameSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpnameLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StockTableToolbar(uiState, onSearchChanged, onCategoryFilterChanged, onRefresh)
            HorizontalDivider(color = OpnameLine)
            StockTableHeader()
            HorizontalDivider(color = OpnameLine)
            StockTableRows(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                onSelectProduct = onSelectProduct
            )
            HorizontalDivider(color = OpnameLine)
            StockTableFooter(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun StockTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OpnameSurface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OpnameSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Menampilkan ${uiState.tableStartIndex}-${uiState.tableEndIndex} dari ${uiState.totalTableProducts} produk",
            color = OpnameMuted,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TablePageButton(
                enabled = uiState.tablePage > 1 && !uiState.isLoading,
                onClick = onPreviousPage,
                icon = Icons.Default.ChevronLeft
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OpnamePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("${uiState.tablePage}", color = OpnameSurface, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalTablePages}", color = OpnameMuted, fontWeight = FontWeight.SemiBold)
            TablePageButton(
                enabled = uiState.tablePage < uiState.totalTablePages && !uiState.isLoading,
                onClick = onNextPage,
                icon = Icons.Default.ChevronRight
            )
        }
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
        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpnameText)
    ) {
        Icon(icon, contentDescription = null)
    }
}
