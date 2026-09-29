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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductUnit) -> Unit,
    onDelete: (ProductUnit) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        UnitToolbar(uiState.searchQuery, compact, onSearchChanged, onAdd)
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        when {
            uiState.isLoading && uiState.units.isEmpty() -> ProductLoadingState(Modifier.height(360.dp))
            uiState.units.isEmpty() -> ProductEmptyMasterState("Belum ada satuan yang cocok.")
            compact -> UnitMobileList(uiState.units, onEdit, onDelete)
            else -> UnitTable(uiState.units, onEdit, onDelete)
        }
        if (!uiState.isLoading && uiState.units.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MasterDataPagination(
                itemLabel = "satuan",
                total = uiState.totalUnits,
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
private fun UnitToolbar(query: String, compact: Boolean, onQuery: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag("unit-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).height(52.dp).testTag("unit-search"),
            placeholder = { Text("Cari satuan") },
            leadingIcon = { Icon(Icons.Outlined.Search, "Cari satuan", Modifier.size(21.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = UnitWhite,
                unfocusedContainerColor = UnitWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = UnitEmerald700,
            ),
        )
        if (compact) {
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(52.dp).testTag("unit-add"),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = UnitEmerald600,
                ),
            ) { Icon(Icons.Default.Add, "Tambah satuan", Modifier.size(22.dp)) }
        } else {
            Button(
                onClick = onAdd,
                modifier = Modifier.height(52.dp).testTag("unit-add"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UnitEmerald600),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("Tambah satuan", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UnitMobileList(items: List<ProductUnit>, onEdit: (ProductUnit) -> Unit, onDelete: (ProductUnit) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("unit-card-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { unit ->
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("unit-card-${unit.id}"),
                color = UnitWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, UnitSlate200.copy(alpha = 0.85f)),
                tonalElevation = 1.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MasterAvatar(unit.initial(), UnitEmerald600, UnitEmerald50)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(unit.name, color = UnitSlate900, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Surface(color = UnitSlate50, shape = RoundedCornerShape(8.dp)) {
                            Text(unit.symbol, Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = UnitSlate600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    UnitActions(unit, onEdit, onDelete)
                }
            }
        }
    }
}

@Composable
private fun UnitTable(items: List<ProductUnit>, onEdit: (ProductUnit) -> Unit, onDelete: (ProductUnit) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().testTag("unit-table"),
        color = UnitWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, UnitSlate200.copy(alpha = 0.8f)),
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().background(UnitSlate50.copy(alpha = 0.6f)).padding(horizontal = 24.dp, vertical = 13.dp)) {
                MasterHeader("Satuan", Modifier.weight(1f))
                MasterHeader("Simbol", Modifier.weight(0.35f))
                MasterHeader("Tanggal dibuat", Modifier.weight(0.45f))
                MasterHeader("Aksi", Modifier.weight(0.18f), Alignment.End)
            }
            items.forEachIndexed { index, unit ->
                Row(
                    Modifier.fillMaxWidth().testTag("unit-card-${unit.id}")
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        MasterAvatar(unit.initial(), UnitEmerald600, UnitEmerald50)
                        Spacer(Modifier.width(12.dp))
                        Text(unit.name, color = UnitSlate900, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(unit.symbol, Modifier.weight(0.35f), color = UnitSlate600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(unit.createdAt.substringBefore("T"), Modifier.weight(0.45f), color = UnitSlate500, fontSize = 13.sp)
                    Box(Modifier.weight(0.18f), contentAlignment = Alignment.CenterEnd) { UnitActions(unit, onEdit, onDelete) }
                }
                if (index < items.lastIndex) HorizontalDivider(Modifier.padding(start = 24.dp), color = UnitSlate200.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun UnitActions(item: ProductUnit, onEdit: (ProductUnit) -> Unit, onDelete: (ProductUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp).testTag("unit-actions-${item.id}")) {
            Icon(Icons.Default.MoreVert, "Aksi satuan", Modifier.size(20.dp), tint = UnitSlate500)
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(UnitWhite, RoundedCornerShape(14.dp))) {
            DropdownMenuItem(
                text = { Text("Edit satuan") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(19.dp)) },
                onClick = { expanded = false; onEdit(item) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            DropdownMenuItem(
                text = { Text("Hapus satuan", color = ProductDanger) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(19.dp), tint = ProductDanger) },
                onClick = { expanded = false; onDelete(item) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}
