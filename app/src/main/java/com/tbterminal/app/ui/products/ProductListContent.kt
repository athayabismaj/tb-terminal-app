package com.tbterminal.app.ui.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.products.components.ProductFilterBottomSheet
import com.tbterminal.app.ui.products.components.ProductListToolbar
import com.tbterminal.app.ui.products.components.ProductSummaryBottomSheet
import com.tbterminal.app.ui.products.components.ProductTableCard

@Composable
internal fun ProductListContent(
    modifier: Modifier,
    uiState: ProductListUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
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
        val isFiltered = uiState.selectedCategory != ALL_PRODUCT_CATEGORIES
        val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

        TopFloatingCapsule(
            message = uiState.actionMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (compact) 16.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
        ) {
            if (compact) {
                // Expandable Search Bar (Riwayat Pembelian style)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.5f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Color(0xFF256B57),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        "Cari nama atau SKU produk...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearchChanged,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product-search"),
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        onSearchChanged("")
                                    } else {
                                        onToggleSearch()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Tutup pencarian",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                // Active Filter Chips
                if (isFiltered) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Filter:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Category, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                Text(
                                    text = uiState.selectedCategory,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { onCategorySelected(ALL_PRODUCT_CATEGORIES) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCategorySelected(ALL_PRODUCT_CATEGORIES) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
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

        if (showFilterPopup) {
            ProductFilterBottomSheet(
                uiState = uiState,
                onDismiss = onDismissFilter,
                onCategorySelected = onCategorySelected
            )
        }

        if (showSummaryPopup) {
            ProductSummaryBottomSheet(
                uiState = uiState,
                onDismiss = onDismissSummary
            )
        }
    }
}

