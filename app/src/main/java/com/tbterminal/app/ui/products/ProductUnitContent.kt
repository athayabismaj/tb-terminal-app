package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductUnit

@Composable
internal fun ProductUnitContent(
    modifier: Modifier,
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSave: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (ProductUnit) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(UnitSurfaceBg)) {
        val compact = maxWidth < 720.dp
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 1180.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
            ) {
                if (uiState.isFormVisible) {
                    ProductUnitFormCard(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).align(Alignment.CenterHorizontally),
                        uiState = uiState,
                        onNameChanged = onNameChanged,
                        onSymbolChanged = onSymbolChanged,
                        onSave = onSave,
                        onCancelEdit = onCancelEdit,
                    )
                } else {
                    uiState.message?.let { ProductUnitMessage(it) }
                    ProductUnitListCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        compact = compact,
                        onSearchChanged = onSearchChanged,
                        onAdd = onAdd,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onRetry = onRetry,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage,
                    )
                }
            }
        }
    }
}
