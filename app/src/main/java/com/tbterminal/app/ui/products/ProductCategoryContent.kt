package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
            .background(CategorySurfaceBg)
            .padding(32.dp)
    ) {
        val isDesktop = maxWidth > 900.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Dashboard", fontSize = 14.sp, color = CategorySlate400)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CategorySlate400, modifier = Modifier.size(16.dp))
            Text("Master Data", fontSize = 14.sp, color = CategorySlate400)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CategorySlate400, modifier = Modifier.size(16.dp))
            Text("Kategori Produk", fontSize = 14.sp, color = CategorySlate900, fontWeight = FontWeight.Bold)
        }
        Text("Kategori Produk", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CategorySlate900)
        Text(
            text = "Kelompokkan produk agar pencarian POS dan laporan stok lebih rapi.",
            fontSize = 14.sp,
            color = CategorySlate500,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
