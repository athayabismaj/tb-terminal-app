package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun StockOpnameFormScreen(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSelectProduct: (ProductStock) -> Unit,
    onActualQtyChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onOpeningDateChanged: (String) -> Unit,
    onAdjustmentTypeChanged: (StockAdjustmentType) -> Unit,
    onSubmit: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OpnameBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StockOpnameHeader(title = "Form Penyesuaian Stok")
        StockOpnameMessage(uiState, onDismissMessage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            StockOpnameFormCard(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxHeight(),
                uiState = uiState,
                onActualQtyChanged = onActualQtyChanged,
                onNotesChanged = onNotesChanged,
                onOpeningDateChanged = onOpeningDateChanged,
                onAdjustmentTypeChanged = onAdjustmentTypeChanged,
                onSubmit = onSubmit,
                onSelectProduct = onSelectProduct
            )
        }
    }
}
