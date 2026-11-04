package com.tbterminal.app.ui.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.products.components.MasterDataPagination
import com.tbterminal.app.ui.products.components.formatMasterDate

private val BrandGreen = Color(0xFF256B57)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val TextMuted = Color(0xFF94A3B8)
private val BorderSlate200 = Color(0xFFE2E8F0)
private val BorderSlate300 = Color(0xFFCBD5E1)

@Composable
internal fun ProductCategoryListCard(
    modifier: Modifier,
    uiState: ProductCategoryUiState,
    compact: Boolean,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
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
                                    "Cari nama kategori...",
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
                                    .testTag("category-search"),
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
        } else {
            CategoryToolbar(
                query = uiState.searchQuery,
                compact = compact,
                isLoading = uiState.isLoading && uiState.categories.isEmpty(),
                onQuery = onSearchChanged,
                onAdd = onAdd,
            )
        }

        ProductCategorySectionHeader(
            onAdd = onAdd,
        )

        when {
            uiState.isLoading && uiState.categories.isEmpty() -> CategoryLoading(compact = compact)
            uiState.categories.isEmpty() -> CategoryEmpty(query = uiState.searchQuery)
            compact -> CategoryMobileList(uiState.categories, onEdit, onDelete)
            else -> CategoryTable(uiState.categories, onEdit, onDelete)
        }

        if (!uiState.isLoading && uiState.categories.isNotEmpty()) {
            MasterDataPagination(
                itemLabel = "kategori",
                total = uiState.totalCategories,
                page = uiState.page,
                totalPages = uiState.totalPages,
                pageSize = uiState.pageSize,
                isLoading = uiState.isLoading,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact,
            )
        }
    }
}

@Composable
private fun ProductCategorySectionHeader(
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Daftar kategori",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
        )
        Surface(
            onClick = onAdd,
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFF256B57)),
            contentColor = Color(0xFF256B57),
            modifier = Modifier
                .size(32.dp)
                .testTag("category-add"),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah kategori",
                    tint = Color(0xFF256B57),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryToolbar(
    query: String,
    compact: Boolean,
    isLoading: Boolean,
    onQuery: (String) -> Unit,
    onAdd: () -> Unit,
) {
    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxWidth().testTag("category-toolbar"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(
                modifier = Modifier.weight(1f).height(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
            SkeletonBox(
                modifier = Modifier.width(if (compact) 96.dp else 164.dp).height(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().testTag("category-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategorySearchField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            compact = compact,
        )
        Button(
            onClick = onAdd,
            modifier = Modifier
                .height(if (compact) 42.dp else 48.dp)
                .testTag("category-add"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            contentPadding = PaddingValues(horizontal = if (compact) 14.dp else 18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
            Text(
                text = if (compact) "Tambah" else "Tambah kategori",
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun CategorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(if (compact) 42.dp else 48.dp)
            .testTag("category-search"),
        color = Color.White,
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        border = BorderStroke(1.dp, BorderSlate300),
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
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = "Cari kategori...",
                        fontSize = 14.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Hapus pencarian",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryMobileList(
    items: List<ProductCategory>,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("category-card-list"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column {
            items.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category-card-${category.id}")
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = category.initial(),
                            color = BrandGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = category.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Dibuat ${category.createdAt.formatMasterDate()}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    CategoryActions(category, onEdit, onDelete)
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun CategoryTable(
    items: List<ProductCategory>,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("category-table"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column {
            // Table Header (Light Gray Background #F8FAFC)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "KATEGORI",
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "TANGGAL DIBUAT",
                    modifier = Modifier.weight(0.45f),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "AKSI",
                    modifier = Modifier.width(60.dp),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.End,
                )
            }
            HorizontalDivider(color = BorderSlate200)

            // Table Rows
            items.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category-card-${category.id}")
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = category.initial(),
                                color = BrandGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = category.createdAt.formatMasterDate(),
                        modifier = Modifier.weight(0.45f),
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                    )
                    Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterEnd) {
                        CategoryActions(category, onEdit, onDelete)
                    }
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun CategoryActions(
    item: ProductCategory,
    onEdit: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp).testTag("category-actions-${item.id}"),
        ) {
            Icon(Icons.Default.MoreVert, "Aksi kategori", Modifier.size(18.dp), tint = TextMuted)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderSlate200),
        ) {
            DropdownMenuItem(
                text = { Text("Edit kategori", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = TextSecondary) },
                onClick = { expanded = false; onEdit(item) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
            DropdownMenuItem(
                text = { Text("Hapus kategori", color = ProductDanger, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp), tint = ProductDanger) },
                onClick = { expanded = false; onDelete(item) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
        }
    }
}

@Composable
private fun CategoryLoading(compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("category-skeleton"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column {
            if (!compact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(Modifier.width(90.dp).height(12.dp))
                    Spacer(Modifier.weight(1f))
                    SkeletonBox(Modifier.width(120.dp).height(12.dp))
                    Spacer(Modifier.weight(0.45f))
                    SkeletonBox(Modifier.width(40.dp).height(12.dp))
                }
                HorizontalDivider(color = BorderSlate200)
            }

            repeat(if (compact) 4 else 5) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(Modifier.size(36.dp), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(if (compact) 0.65f else 0.4f).height(14.dp))
                        if (compact) {
                            SkeletonBox(Modifier.width(100.dp).height(11.dp))
                        }
                    }
                    if (!compact) {
                        SkeletonBox(Modifier.weight(0.45f).height(13.dp))
                        SkeletonBox(Modifier.size(24.dp))
                    } else {
                        SkeletonBox(Modifier.size(24.dp))
                    }
                }
                if (index < (if (compact) 3 else 4)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun CategoryEmpty(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("category-empty"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Belum ada kategori",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = if (query.isNotBlank()) {
                    "Tidak ada kategori yang cocok dengan \"$query\"."
                } else {
                    "Mulai tambahkan kategori produk untuk mengelompokkan barang dagangan."
                },
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
