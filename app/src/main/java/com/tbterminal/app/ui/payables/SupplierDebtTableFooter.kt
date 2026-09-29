package com.tbterminal.app.ui.payables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.components.TbPagination

@Composable
internal fun SupplierDebtFooter(
    uiState: SupplierDebtUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false,
) {
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (uiState.searchQuery.isNotBlank()) "${uiState.filteredPayables.size} hasil"
        else if (compact) "${uiState.total} data"
        else "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} hutang",
        isLoading = uiState.isLoading,
        testTag = "supplier-debt-pagination",
    )
}
