package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
    BoxWithConstraints(modifier.fillMaxSize().background(OpnameBackground)) {
        val compact = maxWidth < 720.dp
        Scaffold(
            containerColor = OpnameBackground,
            bottomBar = {
                Surface(color = OpnameSurface, shadowElevation = 3.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = 12.dp),
                        contentAlignment = if (compact) Alignment.Center else Alignment.CenterEnd,
                    ) {
                        SubmitButton(
                            uiState = uiState,
                            onSubmit = onSubmit,
                            modifier = if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(max = 280.dp),
                        )
                    }
                }
            },
        ) { contentPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding)
                    .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 14.dp else 24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 1180.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StockOpnameMessage(uiState, onDismissMessage)
                    StockOpnameFormCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onActualQtyChanged = onActualQtyChanged,
                        onNotesChanged = onNotesChanged,
                        onOpeningDateChanged = onOpeningDateChanged,
                        onAdjustmentTypeChanged = onAdjustmentTypeChanged,
                        compact = compact,
                        onSelectProduct = onSelectProduct,
                    )
                }
            }
        }
    }
}
