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
import com.tbterminal.app.ui.products.components.MasterDataPagination

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
        shape = RoundedCornerShape(8.dp),
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
        else -> Column(modifier = Modifier.fillMaxWidth()) {
            units.forEach { unit ->
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
    MasterDataPagination(
        itemLabel = "satuan",
        total = uiState.totalUnits,
        page = uiState.page,
        totalPages = uiState.totalPages,
        pageSize = uiState.pageSize,
        isLoading = uiState.isLoading,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage
    )
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

private val UnitPageListHeight = 240.dp
