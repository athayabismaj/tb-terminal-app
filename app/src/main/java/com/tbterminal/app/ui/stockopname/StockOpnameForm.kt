package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun StockOpnameFormCard(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onActualQtyChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAdjustmentTypeChanged: (StockAdjustmentType) -> Unit,
    onSubmit: () -> Unit,
    onSelectProduct: ((com.tbterminal.app.data.model.ProductStock) -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = OpnameSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpnameLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FormTitle()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (onSelectProduct != null) {
                        ProductSelectionDropdown(
                            products = uiState.products,
                            selectedProduct = uiState.selectedProduct,
                            onSelectProduct = onSelectProduct
                        )
                    }
                    SelectedProductSummary(uiState.selectedProduct)
                    StockDifferenceSummary(uiState)
                }
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    AdjustmentTypeCards(uiState.adjustmentType, onAdjustmentTypeChanged)
                    QuantityInput(uiState.actualQtyInput, onActualQtyChanged)
                    NotesInput(uiState.notesInput, onNotesChanged)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            SubmitButton(uiState, onSubmit)
        }
    }
}
