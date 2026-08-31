package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
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
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(ProductSurface)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(if (compact) 16.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp)
        ) {
            val actionButton: @Composable (String, Modifier, androidx.compose.ui.graphics.Color, () -> Unit) -> Unit =
                { label, buttonModifier, color, onClick ->
                Button(
                    onClick = onClick,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }

            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Daftar Produk",
                        color = ProductText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        actionButton("Impor CSV", Modifier.weight(1f), ProductPrimary, onImportProductClick)
                        actionButton("Tambah", Modifier.weight(1f), ProductPrimaryDark, onAddProductClick)
                    }
                }
            } else Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            Text(
                text = "Daftar Produk",
                color = ProductText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                actionButton("Impor CSV", Modifier, ProductPrimary, onImportProductClick)
                actionButton("Tambah", Modifier, ProductPrimaryDark, onAddProductClick)
            }
        }

        ProductListToolbar(
            searchQuery = uiState.searchQuery,
            categories = uiState.availableCategories,
            selectedCategory = uiState.selectedCategory,
            onSearchChanged = onSearchChanged,
            onCategorySelected = onCategorySelected,
            compact = compact
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
            compact = compact
        )
        }
    }
}

