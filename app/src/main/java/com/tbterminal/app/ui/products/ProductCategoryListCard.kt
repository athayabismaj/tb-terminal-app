package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.products.components.MasterDataPagination

@Composable
internal fun ProductCategoryListCard(
    modifier: Modifier,
    uiState: ProductCategoryUiState,
    onSearchChanged: (String) -> Unit,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CategoryWhite),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CategorySlate200)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CategoryListHeader(total = uiState.totalCategories)
            HorizontalDivider(color = CategorySlate100)
            CategoryToolbar(uiState.searchQuery, onSearchChanged, onRetry)
            CategoryTableHeader()
            CategoryTableBody(uiState.isLoading, uiState.categories, onEdit, onDelete)
            CategoryFooter(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun CategoryListHeader(total: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductCategoryCardTitle(
            icon = Icons.AutoMirrored.Outlined.ListAlt,
            title = "Daftar Kategori",
            bottomPadding = 0.dp
        )
        Box(
            modifier = Modifier
                .background(CategoryEmerald50, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("$total total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CategoryEmerald600)
        }
    }
}

@Composable
private fun CategoryToolbar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CategoryWhite)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari kategori...", color = CategorySlate400, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = CategorySlate400, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            singleLine = true,
            colors = productCategoryTextFieldColors(),
            shape = RoundedCornerShape(8.dp)
        )
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.height(54.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CategorySlate200),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CategorySlate600)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Muat Ulang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CategorySlate50)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        CategoryHeaderText("IKON", Modifier.weight(1f))
        CategoryHeaderText("NAMA KATEGORI", Modifier.weight(2.2f))
        CategoryHeaderText("DIBUAT", Modifier.weight(1.6f))
        CategoryHeaderText("AKSI", Modifier.weight(1f), Alignment.CenterHorizontally)
    }
}

@Composable
private fun CategoryTableBody(
    isLoading: Boolean,
    categories: List<ProductCategory>,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit
) {
    when {
        isLoading -> ProductLoadingState(modifier = Modifier.height(CategoryPageListHeight))
        categories.isEmpty() -> CategoryEmptyState()
        else -> Column(modifier = Modifier.fillMaxWidth()) {
            categories.forEach { category ->
                CategoryRow(category = category, onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: ProductCategory,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CategorySlate50))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CategoryEmerald50),
                contentAlignment = Alignment.Center
            ) {
                Text(category.initial(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CategoryEmerald700)
            }
        }
        Text(
            category.name,
            modifier = Modifier.weight(2.2f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = CategorySlate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            category.createdAt.substringBefore("T"),
            modifier = Modifier.weight(1.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CategorySlate500
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { onEdit(category) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CategorySlate400)
            }
            IconButton(onClick = { onDelete(category) }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = CategorySlate400)
            }
        }
    }
}

@Composable
private fun CategoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CategoryPageListHeight),
        contentAlignment = Alignment.Center
    ) {
        Text("Belum ada kategori yang cocok.", color = CategorySlate500, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CategoryFooter(
    uiState: ProductCategoryUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    MasterDataPagination(
        itemLabel = "kategori",
        total = uiState.totalCategories,
        page = uiState.page,
        totalPages = uiState.totalPages,
        pageSize = uiState.pageSize,
        isLoading = uiState.isLoading,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage
    )
}

@Composable
private fun CategoryHeaderText(
    text: String,
    modifier: Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CategorySlate400)
    }
}

private val CategoryPageListHeight = 240.dp
