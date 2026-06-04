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
import com.tbterminal.app.data.model.ProductCategory

@Composable
internal fun ProductCategoryContent(
    modifier: Modifier,
    uiState: ProductCategoryUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (ProductCategory) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onRetry: () -> Unit,
    onSearchChanged: (String) -> Unit,
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
            ProductCategoryHeader()
            if (isDesktop) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                    ProductCategoryFormCard(
                        modifier = Modifier.weight(5f),
                        uiState = uiState,
                        onNameChanged = onNameChanged,
                        onSave = onSave,
                        onCancelEdit = onCancelEdit
                    )
                    ProductCategoryListCard(
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
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                    ProductCategoryFormCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onNameChanged = onNameChanged,
                        onSave = onSave,
                        onCancelEdit = onCancelEdit
                    )
                    ProductCategoryListCard(
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
        }
    }
}

@Composable
private fun ProductCategoryHeader() {
    Text("Kategori Produk", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = CategorySlate900)
}
