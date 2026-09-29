package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        CategoryToolbar(uiState.searchQuery, compact, onSearchChanged, onAdd)
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        when {
            uiState.isLoading && uiState.categories.isEmpty() -> ProductLoadingState(Modifier.height(360.dp))
            uiState.categories.isEmpty() -> ProductEmptyMasterState("Belum ada kategori yang cocok.")
            compact -> CategoryMobileList(uiState.categories, onEdit, onDelete)
            else -> CategoryTable(uiState.categories, onEdit, onDelete)
        }
        if (!uiState.isLoading && uiState.categories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MasterDataPagination(
                itemLabel = "kategori",
                total = uiState.totalCategories,
                page = uiState.page,
                totalPages = uiState.totalPages,
                pageSize = uiState.pageSize,
                isLoading = uiState.isLoading,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
            )
        }
    }
}

@Composable
private fun CategoryToolbar(query: String, compact: Boolean, onQuery: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag("category-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).height(52.dp).testTag("category-search"),
            placeholder = { Text("Cari kategori") },
            leadingIcon = { Icon(Icons.Outlined.Search, "Cari kategori", Modifier.size(21.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CategoryWhite,
                unfocusedContainerColor = CategoryWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = CategoryEmerald700,
            ),
        )
        MasterAddButton(compact, "Tambah kategori", onAdd)
    }
}

@Composable
private fun MasterAddButton(compact: Boolean, label: String, onClick: () -> Unit) {
    if (compact) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp).testTag("category-add"),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White,
                contentColor = CategoryEmerald600,
            ),
        ) { Icon(Icons.Default.Add, label, Modifier.size(22.dp)) }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.height(52.dp).testTag("category-add"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CategoryEmerald600),
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryMobileList(items: List<ProductCategory>, onEdit: (ProductCategory) -> Unit, onDelete: (ProductCategory) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("category-card-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { category ->
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("category-card-${category.id}"),
                color = CategoryWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CategorySlate200.copy(alpha = 0.85f)),
                tonalElevation = 1.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MasterAvatar(category.initial(), CategoryEmerald600, CategoryEmerald50)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(category.name, color = CategorySlate900, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Dibuat ${category.createdAt.substringBefore("T")}", color = CategorySlate500, fontSize = 12.sp)
                    }
                    CategoryActions(category, onEdit, onDelete)
                }
            }
        }
    }
}

@Composable
private fun CategoryTable(items: List<ProductCategory>, onEdit: (ProductCategory) -> Unit, onDelete: (ProductCategory) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().testTag("category-table"),
        color = CategoryWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CategorySlate200.copy(alpha = 0.8f)),
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().background(CategorySlate50.copy(alpha = 0.6f)).padding(horizontal = 24.dp, vertical = 13.dp)) {
                MasterHeader("Kategori", Modifier.weight(1f))
                MasterHeader("Tanggal dibuat", Modifier.weight(0.45f))
                MasterHeader("Aksi", Modifier.weight(0.18f), Alignment.End)
            }
            items.forEachIndexed { index, category ->
                Row(
                    Modifier.fillMaxWidth().testTag("category-card-${category.id}")
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        MasterAvatar(category.initial(), CategoryEmerald600, CategoryEmerald50)
                        Spacer(Modifier.width(12.dp))
                        Text(category.name, color = CategorySlate900, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(category.createdAt.substringBefore("T"), Modifier.weight(0.45f), color = CategorySlate500, fontSize = 13.sp)
                    Box(Modifier.weight(0.18f), contentAlignment = Alignment.CenterEnd) { CategoryActions(category, onEdit, onDelete) }
                }
                if (index < items.lastIndex) HorizontalDivider(Modifier.padding(start = 24.dp), color = CategorySlate200.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun CategoryActions(item: ProductCategory, onEdit: (ProductCategory) -> Unit, onDelete: (ProductCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp).testTag("category-actions-${item.id}")) {
            Icon(Icons.Default.MoreVert, "Aksi kategori", Modifier.size(20.dp), tint = CategorySlate500)
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(CategoryWhite, RoundedCornerShape(14.dp))) {
            DropdownMenuItem(
                text = { Text("Edit kategori") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(19.dp)) },
                onClick = { expanded = false; onEdit(item) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            DropdownMenuItem(
                text = { Text("Hapus kategori", color = ProductDanger) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(19.dp), tint = ProductDanger) },
                onClick = { expanded = false; onDelete(item) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
internal fun MasterAvatar(text: String, tint: Color, background: Color) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(text, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun MasterHeader(text: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier, horizontalAlignment = alignment) { Text(text, color = CategorySlate500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
internal fun ProductEmptyMasterState(message: String) {
    Surface(Modifier.fillMaxWidth(), color = CategoryWhite, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CategorySlate200)) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text(message, color = CategorySlate500) }
    }
}
