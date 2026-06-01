package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.products.components.ProductCategoryFilterChip
import com.tbterminal.app.ui.products.components.ProductTableCard

@Composable
internal fun ProductListContent(
    modifier: Modifier,
    uiState: ProductListUiState,
    onSearchChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onProductDetailClick: (String) -> Unit,
    onCategoriesClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onToggleProductClick: (ProductStock) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    val filterCategories = remember(uiState.products) {
        listOf("Semua") + uiState.products
            .map(ProductStock::categoryName)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProductBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProductHeader(
            title = "Daftar Produk",
            subtitle = "Kelola inventaris stok dan harga barang Anda.",
            actions = {}
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = filterCategories, key = { category -> category }) { category ->
                ProductCategoryFilterChip(
                    text = category,
                    isSelected = category == selectedCategory,
                    onClick = {
                        selectedCategory = category
                        onSearchChanged(if (category == "Semua") "" else category)
                    }
                )
            }
        }

        if (uiState.actionMessage != null) {
            ProductBanner(message = uiState.actionMessage, onDismiss = onDismissMessage)
        }

        ProductTableCard(
            modifier = Modifier.weight(1f),
            uiState = uiState,
            onSearchChanged = { query ->
                selectedCategory = "Semua"
                onSearchChanged(query)
            },
            onRetry = onRetry,
            onEditProductClick = onEditProductClick,
            onProductDetailClick = onProductDetailClick,
            onToggleProductClick = onToggleProductClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

