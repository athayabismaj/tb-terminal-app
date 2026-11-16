package com.tbterminal.app.ui.payables

import androidx.compose.runtime.Composable
import com.tbterminal.app.ui.components.TbPagination

@Composable
internal fun SupplierDebtFooter(
    uiState: SupplierDebtUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false,
) {
    val desktopText = if (uiState.total == 0L) {
        "0 hutang"
    } else {
        "${uiState.total} hutang"
    }
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = desktopText,
        isLoading = uiState.isLoading,
        compact = compact,
        testTag = "supplier-debt-pagination",
    )
}
