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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Straighten
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
import com.tbterminal.app.data.model.ProductUnit
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
internal fun ProductUnitListCard(
    modifier: Modifier,
    uiState: ProductUnitUiState,
    compact: Boolean,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit,
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
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF256B57),
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChanged,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unit-search-input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari satuan atau simbol...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            UnitToolbar(
                query = uiState.searchQuery,
                compact = compact,
                isLoading = uiState.isLoading && uiState.units.isEmpty(),
                onQuery = onSearchChanged,
                onAdd = onAdd,
            )
        }

        when {
            uiState.isLoading && uiState.units.isEmpty() -> UnitLoading(compact = compact)
            uiState.units.isEmpty() -> UnitEmpty(query = uiState.searchQuery)
            compact -> UnitMobileList(uiState.units, onEdit, onDelete)
            else -> UnitTable(uiState.units, onEdit, onDelete)
        }

        if (!uiState.isLoading && uiState.units.isNotEmpty()) {
            MasterDataPagination(
                itemLabel = "satuan",
                total = uiState.totalUnits,
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
private fun UnitToolbar(
    query: String,
    compact: Boolean,
    isLoading: Boolean,
    onQuery: (String) -> Unit,
    onAdd: () -> Unit,
) {
    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxWidth().testTag("unit-toolbar"),
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
        modifier = Modifier.fillMaxWidth().testTag("unit-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnitSearchField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            compact = compact,
        )
        Button(
            onClick = onAdd,
            modifier = Modifier
                .height(if (compact) 42.dp else 48.dp)
                .testTag("unit-add"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            contentPadding = PaddingValues(horizontal = if (compact) 14.dp else 18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
            Text(
                text = if (compact) "Tambah" else "Tambah satuan",
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun UnitSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(if (compact) 42.dp else 48.dp)
            .testTag("unit-search"),
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
                        text = "Cari satuan...",
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
private fun UnitMobileList(
    items: List<ProductUnit>,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("unit-card-list"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSlate200),
    ) {
        Column {
            items.forEachIndexed { index, unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit-card-${unit.id}")
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
                            text = unit.initial(),
                            color = BrandGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = unit.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            ) {
                                Text(
                                    text = unit.symbol,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    color = Color(0xFF334155),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "· Dibuat ${unit.createdAt.formatMasterDate()}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    UnitActions(unit, onEdit, onDelete)
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun UnitTable(
    items: List<ProductUnit>,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("unit-table"),
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
                    text = "SATUAN",
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "SIMBOL",
                    modifier = Modifier.weight(0.35f),
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
            items.forEachIndexed { index, unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit-card-${unit.id}")
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
                                text = unit.initial(),
                                color = BrandGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = unit.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(modifier = Modifier.weight(0.35f)) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        ) {
                            Text(
                                text = unit.symbol,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color(0xFF334155),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        text = unit.createdAt.formatMasterDate(),
                        modifier = Modifier.weight(0.45f),
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                    )
                    Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterEnd) {
                        UnitActions(unit, onEdit, onDelete)
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
private fun UnitActions(
    item: ProductUnit,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp).testTag("unit-actions-${item.id}"),
        ) {
            Icon(Icons.Default.MoreVert, "Aksi satuan", Modifier.size(18.dp), tint = TextMuted)
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
                text = { Text("Edit satuan", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = TextSecondary) },
                onClick = { expanded = false; onEdit(item) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
            DropdownMenuItem(
                text = { Text("Hapus satuan", color = ProductDanger, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp), tint = ProductDanger) },
                onClick = { expanded = false; onDelete(item) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
        }
    }
}

@Composable
private fun UnitLoading(compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("unit-skeleton"),
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
                    SkeletonBox(Modifier.width(60.dp).height(12.dp))
                    Spacer(Modifier.weight(0.35f))
                    SkeletonBox(Modifier.width(100.dp).height(12.dp))
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
                            SkeletonBox(Modifier.width(110.dp).height(11.dp))
                        }
                    }
                    if (!compact) {
                        SkeletonBox(Modifier.weight(0.35f).height(18.dp))
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
private fun UnitEmpty(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("unit-empty"),
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
                    imageVector = Icons.Outlined.Straighten,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Belum ada satuan",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = if (query.isNotBlank()) {
                    "Tidak ada satuan yang cocok dengan \"$query\"."
                } else {
                    "Mulai tambahkan satuan produk untuk menentukan takaran atau ukuran barang."
                },
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
