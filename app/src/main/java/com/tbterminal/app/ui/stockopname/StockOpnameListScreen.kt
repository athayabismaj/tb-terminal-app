package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun StockOpnameListScreen(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onOpenForm: () -> Unit,
    onSelectProduct: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OpnameSurface)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        StockOpnameListHeader(onOpenForm = onOpenForm)
        StockOpnameMessage(uiState, onDismissMessage)
        StockOpnameTableCard(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onCategoryFilterChanged = onCategoryFilterChanged,
            onSelectProduct = onSelectProduct,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}
