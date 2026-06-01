package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.SupplierPayable

@Composable
internal fun SupplierDebtTableCard(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DebtSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DebtLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged, onRefresh)
            DebtTableHeader()
            SupplierDebtRows(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                onPayClick = onPayClick
            )
            SupplierDebtFooter(uiState, onPreviousPage, onNextPage)
        }
    }
}
