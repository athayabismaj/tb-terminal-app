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
    onNextPage: () -> Unit,
    compact: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged, compact)
        Spacer(modifier = Modifier.height(if (compact) 16.dp else 28.dp))
        if (!compact) DebtTableHeader()
        SupplierDebtRows(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onPayClick = onPayClick,
            compact = compact
        )
        SupplierDebtFooter(uiState, onPreviousPage, onNextPage, compact)
    }
}
