package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(ProductBackground)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(if (compact) 16.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
        ) {
            ProductListToolbar(
                searchQuery = uiState.searchQuery,
                categories = uiState.availableCategories,
                selectedCategory = uiState.selectedCategory,
                onSearchChanged = onSearchChanged,
                onCategorySelected = onCategorySelected,
                onAddProductClick = onAddProductClick,
                onImportProductClick = onImportProductClick,
                compact = compact,
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
                onNextPage = onNextPage,
                compact = compact,
            )
        }
    }
}

