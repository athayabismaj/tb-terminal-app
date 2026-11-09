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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.ui.components.TopFloatingCapsule

@Composable
internal fun ReceivableScreen(
    modifier: Modifier = Modifier,
    uiState: ReceivableUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onOpenSummary: () -> Unit = {},
    onDismissSummary: () -> Unit = {},
    onResetAllFilters: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit,
    onSortOrderChanged: (ReceivableSortOrder) -> Unit = {},
    canAdjust: Boolean,
    onAddOpeningBalance: () -> Unit,
    onAddAdjustment: () -> Unit,
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit,
    onCustomerDetailClick: (String) -> Unit = {}
) {
    var showActionBottomSheet by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(ReceivableBackground)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            ReceivableTableCard(
                canAdjust = canAdjust,
                onOpenActionMenu = { showActionBottomSheet = true },
                onAddOpeningBalance = onAddOpeningBalance,
                onAddAdjustment = onAddAdjustment,
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                onResetAllFilters = onResetAllFilters,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onDueFilterChanged = onDueFilterChanged,
                onSortOrderChanged = onSortOrderChanged,
                onPayClick = onPayClick,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact,
                onCustomerDetailClick = onCustomerDetailClick
            )
        }

        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }

    if (showActionBottomSheet) {
        ReceivableActionBottomSheet(
            canAdjust = canAdjust,
            onDismiss = { showActionBottomSheet = false },
            onAddOpeningBalance = {
                showActionBottomSheet = false
                onAddOpeningBalance()
            },
            onAddAdjustment = {
                showActionBottomSheet = false
                onAddAdjustment()
            },
            onOpenSummary = {
                showActionBottomSheet = false
                onOpenSummary()
            }
        )
    }

    if (showFilterPopup) {
        ReceivableFilterBottomSheet(
            uiState = uiState,
            onDismiss = onDismissFilter,
            onApplyStatusFilter = onStatusFilterChanged,
            onApplyDueFilter = onDueFilterChanged,
            onApplySortOrder = onSortOrderChanged
        )
    }

    if (showSummaryPopup) {
        ReceivableSummaryBottomSheet(
            uiState = uiState,
            onDismiss = onDismissSummary
        )
    }
}
