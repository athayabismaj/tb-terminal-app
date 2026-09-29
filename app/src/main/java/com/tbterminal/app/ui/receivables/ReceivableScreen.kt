package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.Receivable

@Composable
internal fun ReceivableScreen(
    modifier: Modifier,
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit,
    canAdjust: Boolean,
    onAddOpeningBalance: () -> Unit,
    onAddAdjustment: () -> Unit,
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(ReceivableBackground)) {
        val compact = maxWidth < 720.dp
        val showMetrics = uiState.receivables.isNotEmpty() ||
            (!uiState.isLoading && uiState.errorMessage == null)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            if (!compact) ReceivableHeader(canAdjust, onAddOpeningBalance, onAddAdjustment, compact)
            ReceivableMessage(uiState, onDismissMessage)
            if (showMetrics && !compact) ReceivableMetrics(uiState, compact = false)
            ReceivableTableCard(
                onAddOpeningBalance = onAddOpeningBalance,
                onAddAdjustment = onAddAdjustment,
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onDueFilterChanged = onDueFilterChanged,
                onPayClick = onPayClick,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact
            )
        }
    }
}

