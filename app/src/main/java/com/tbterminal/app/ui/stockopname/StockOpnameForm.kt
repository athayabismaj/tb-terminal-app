package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun StockOpnameFormCard(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onActualQtyChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onOpeningDateChanged: (String) -> Unit,
    onAdjustmentTypeChanged: (StockAdjustmentType) -> Unit,
    compact: Boolean,
    onSelectProduct: ((com.tbterminal.app.data.model.ProductStock) -> Unit)? = null
) {
    val productSection: @Composable () -> Unit = {
        StockOpnameFormSection("Produk", Icons.Outlined.Inventory2, "stock-adjustment-product") {
            if (onSelectProduct != null) {
                ProductSelectionDropdown(
                    products = uiState.products,
                    selectedProduct = uiState.selectedProduct,
                    onSelectProduct = onSelectProduct,
                )
            }
            SelectedProductSummary(uiState.selectedProduct)
            StockDifferenceSummary(uiState)
        }
    }
    val adjustmentSection: @Composable () -> Unit = {
        StockOpnameFormSection("Penyesuaian", Icons.Outlined.Tune, "stock-adjustment-input") {
            AdjustmentTypeCards(uiState.adjustmentType, onAdjustmentTypeChanged)
            QuantityInput(uiState.actualQtyInput, onActualQtyChanged)
            if (uiState.adjustmentType == StockAdjustmentType.OPENING_BALANCE) {
                OpeningDateInput(uiState.openingDateInput, onOpeningDateChanged)
            }
            NotesInput(uiState.notesInput, onNotesChanged)
        }
    }

    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            productSection()
            adjustmentSection()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) { productSection() }
            Column(Modifier.weight(1.05f)) { adjustmentSection() }
        }
    }
}

@Composable
private fun StockOpnameFormSection(
    title: String,
    icon: ImageVector,
    tag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(containerColor = OpnameSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OpnameLine.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = OpnamePrimary.copy(alpha = 0.10f),
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = OpnamePrimaryDark)
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 10.dp))
                Text(title, color = OpnameText, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}
