package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductUnit

@Composable
internal fun ProductUnitContent(
    modifier: Modifier,
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (ProductUnit) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ProductSurface)
            .padding(40.dp)
    ) {
        val isDesktop = maxWidth > 900.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ProductUnitHeader()
            if (isDesktop) {
                ProductUnitDesktopContent(
                    uiState = uiState,
                    onNameChanged = onNameChanged,
                    onSymbolChanged = onSymbolChanged,
                    onSearchChanged = onSearchChanged,
                    onSave = onSave,
                    onEdit = onEdit,
                    onCancelEdit = onCancelEdit,
                    onDelete = onDelete,
                    onRetry = onRetry,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            } else {
                ProductUnitCompactContent(
                    uiState = uiState,
                    onNameChanged = onNameChanged,
                    onSymbolChanged = onSymbolChanged,
                    onSearchChanged = onSearchChanged,
                    onSave = onSave,
                    onEdit = onEdit,
                    onCancelEdit = onCancelEdit,
                    onDelete = onDelete,
                    onRetry = onRetry,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            }
        }
    }
}

@Composable
private fun ProductUnitHeader() {
    Text("Satuan Produk", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = UnitSlate900)
}

@Composable
private fun ProductUnitDesktopContent(
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (ProductUnit) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProductUnitFormCard(
            modifier = Modifier.weight(5f),
            uiState = uiState,
            onNameChanged = onNameChanged,
            onSymbolChanged = onSymbolChanged,
            onSave = onSave,
            onCancelEdit = onCancelEdit
        )
        ProductUnitListCard(
            modifier = Modifier.weight(7f),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onEdit = onEdit,
            onDelete = onDelete,
            onRetry = onRetry,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
private fun ProductUnitCompactContent(
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (ProductUnit) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProductUnitFormCard(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onNameChanged = onNameChanged,
            onSymbolChanged = onSymbolChanged,
            onSave = onSave,
            onCancelEdit = onCancelEdit
        )
        ProductUnitListCard(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onEdit = onEdit,
            onDelete = onDelete,
            onRetry = onRetry,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}
