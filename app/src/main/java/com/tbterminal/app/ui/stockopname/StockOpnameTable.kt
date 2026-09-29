package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onOpenForm: () -> Unit,
    compact: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StockTableToolbar(uiState, onSearchChanged, onCategoryFilterChanged, onOpenForm, compact)
        Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Daftar produk",
                    color = OpnameText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = OpnameSoft,
                ) {
                    Text(
                        uiState.totalTableProducts.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OpnameMuted
                    )
                }
            }
            Text(
                "Total penyesuaian",
                style = MaterialTheme.typography.labelSmall,
                color = OpnameMuted,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stock-adjustment-list-card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = OpnameSurface),
            border = BorderStroke(1.dp, OpnameLine.copy(alpha = 0.72f)),
        ) {
            Column {
                if (!compact) StockTableHeader()
                StockTableRows(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onSelectProduct = onSelectProduct,
                    compact = compact,
                )
            }
        }
        if (!uiState.isLoading && uiState.totalTableProducts > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            StockTableFooter(uiState, onPreviousPage, onNextPage)
        }
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
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    TbPagination(
        currentPage = uiState.tablePage,
        totalPages = uiState.totalTablePages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = null,
        isLoading = uiState.isLoading,
        testTag = "stock-adjustment-pagination",
    )
}
