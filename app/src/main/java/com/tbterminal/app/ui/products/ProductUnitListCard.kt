package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.tbterminal.app.data.model.ProductUnit

@Composable
internal fun ProductUnitListCard(
    modifier: Modifier,
    uiState: ProductUnitUiState,
    onSearchChanged: (String) -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = UnitWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, UnitSlate200)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProductUnitListHeader(total = uiState.totalUnits)
            HorizontalDivider(color = UnitSlate100)
            ProductUnitToolbar(
                searchQuery = uiState.searchQuery,
                onSearchChanged = onSearchChanged,
                onRetry = onRetry
            )
            ProductUnitTableHeader()
            ProductUnitTableBody(
                isLoading = uiState.isLoading,
                units = uiState.units,
                onEdit = onEdit,
                onDelete = onDelete
            )
            ProductUnitFooter(
                uiState = uiState,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage
            )
        }
    }
}

@Composable
private fun ProductUnitListHeader(total: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductUnitCardTitle(
            icon = Icons.AutoMirrored.Outlined.ListAlt,
            title = "Daftar Satuan",
            bottomPadding = 0.dp
        )
        Box(
            modifier = Modifier
                .background(UnitEmerald50, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("$total total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UnitEmerald600)
        }
    }
}

@Composable
private fun ProductUnitToolbar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UnitWhite)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari satuan...", color = UnitSlate400, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = UnitSlate400,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            singleLine = true,
            colors = productUnitTextFieldColors(),
            shape = RoundedCornerShape(8.dp)
        )
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.height(54.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, UnitSlate200),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = UnitSlate600)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Muat Ulang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProductUnitTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UnitSlate50)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        UnitTableHeader("IKON", Modifier.weight(1f))
        UnitTableHeader("NAMA SATUAN", Modifier.weight(2f))
        UnitTableHeader("SIMBOL", Modifier.weight(2f))
        UnitTableHeader("AKSI", Modifier.weight(1f), Alignment.CenterHorizontally)
    }
}

@Composable
private fun ProductUnitTableBody(
    isLoading: Boolean,
    units: List<ProductUnit>,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit
) {
    when {
        isLoading -> ProductLoadingState(modifier = Modifier.height(UnitPageListHeight))
        units.isEmpty() -> ProductUnitEmptyState()
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(UnitPageListHeight)
        ) {
            items(items = units, key = ProductUnit::id) { unit ->
                ProductUnitRow(unit = unit, onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun ProductUnitRow(
    unit: ProductUnit,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, UnitSlate50))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(UnitEmerald50),
                contentAlignment = Alignment.Center
            ) {
                Text(unit.initial(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UnitEmerald700)
            }
        }
        Text(
            unit.name,
            modifier = Modifier.weight(2f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = UnitSlate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(modifier = Modifier.weight(2f)) {
            Box(
                modifier = Modifier
                    .background(UnitSlate100, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(unit.symbol, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = UnitSlate600)
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { onEdit(unit) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = UnitSlate400)
            }
            IconButton(onClick = { onDelete(unit) }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = UnitSlate400)
            }
        }
    }
}

@Composable
private fun ProductUnitEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UnitPageListHeight),
        contentAlignment = Alignment.Center
    ) {
        Text("Belum ada satuan yang cocok.", color = UnitSlate500, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProductUnitFooter(
    uiState: ProductUnitUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val firstItem = if (uiState.totalUnits == 0L) 0L else ((uiState.page - 1) * uiState.pageSize + 1).toLong()
    val lastItem = (uiState.page * uiState.pageSize).toLong().coerceAtMost(uiState.totalUnits)
    val rangeText = if (uiState.totalUnits == 0L) {
        "Belum ada satuan"
    } else {
        "Menampilkan $firstItem-$lastItem dari ${uiState.totalUnits}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UnitSlate50.copy(alpha = 0.5f))
            .border(BorderStroke(1.dp, UnitSlate100))
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rangeText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = UnitSlate400
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            UnitPageButton(
                text = "Sebelumnya",
                enabled = uiState.page > 1 && !uiState.isLoading,
                onClick = onPreviousPage
            )
            UnitPageIndicator(page = uiState.page, totalPages = uiState.totalPages)
            UnitPageButton(
                text = "Berikutnya",
                enabled = uiState.page < uiState.totalPages && !uiState.isLoading,
                onClick = onNextPage
            )
        }
    }
}

@Composable
private fun UnitPageButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, UnitSlate200),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UnitSlate600),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UnitPageIndicator(page: Int, totalPages: Int) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(UnitEmerald600, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$page/$totalPages", color = UnitWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UnitTableHeader(
    text: String,
    modifier: Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = UnitSlate400)
    }
}

private val UnitPageListHeight = 720.dp
