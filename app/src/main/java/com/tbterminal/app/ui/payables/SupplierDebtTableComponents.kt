package com.tbterminal.app.ui.payables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    Column(modifier = modifier.fillMaxWidth()) {
        SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged)
        Spacer(modifier = Modifier.height(28.dp))
        DebtTableHeader()
        SupplierDebtRows(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onPayClick = onPayClick
        )
        SupplierDebtFooter(uiState, onPreviousPage, onNextPage)
    }
}
