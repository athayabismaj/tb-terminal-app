package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
            .background(UnitSurfaceBg)
            .padding(32.dp)
    ) {
        val isDesktop = maxWidth > 900.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Dashboard", fontSize = 14.sp, color = UnitSlate400)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = UnitSlate400,
                modifier = Modifier.size(16.dp)
            )
            Text("Master Data", fontSize = 14.sp, color = UnitSlate400)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = UnitSlate400,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Satuan Produk",
                fontSize = 14.sp,
                color = UnitSlate900,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Satuan Produk",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = UnitSlate900
        )
        Text(
            text = "Kelola satuan penjualan dan stok seperti pcs, sak, batang, meter, atau dus.",
            fontSize = 14.sp,
            color = UnitSlate500,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
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
