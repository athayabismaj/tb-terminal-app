package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.products.components.ProductListToolbar
import com.tbterminal.app.ui.products.components.ProductTableCard

@Composable
internal fun ProductListContent(
    modifier: Modifier,
    uiState: ProductListUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    onAddProductClick: () -> Unit,
    onImportProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onProductDetailClick: (String) -> Unit,
    onCategoriesClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onToggleProductClick: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProductSurface)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Produk",
                color = ProductText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onImportProductClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ProductPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) { Text("Impor CSV", fontSize = 14.sp) }
                Button(
                    onClick = onAddProductClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ProductPrimaryDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("Tambah Produk", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        ProductListToolbar(
            searchQuery = uiState.searchQuery,
            categories = uiState.availableCategories,
            selectedCategory = uiState.selectedCategory,
            onSearchChanged = onSearchChanged,
            onCategorySelected = onCategorySelected
        )

        if (uiState.actionMessage != null) {
            ProductBanner(message = uiState.actionMessage, onDismiss = onDismissMessage)
        }

        ProductTableCard(
            uiState = uiState,
            products = uiState.visibleProducts,
            onRetry = onRetry,
            onEditProductClick = onEditProductClick,
            onProductDetailClick = onProductDetailClick,
            onToggleProductClick = onToggleProductClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

