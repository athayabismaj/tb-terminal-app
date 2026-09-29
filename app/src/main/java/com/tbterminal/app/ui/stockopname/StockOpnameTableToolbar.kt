package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StockTableToolbar(
    uiState: StockOpnameUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onOpenForm: () -> Unit,
    compact: Boolean = false,
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var pendingCategory by remember { mutableStateOf<String?>(null) }
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        Surface(
            modifier = fieldModifier.height(52.dp).testTag("stock-adjustment-search"),
            color = OpnameSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpnameLine.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, "Cari produk", modifier = Modifier.size(20.dp), tint = OpnameMuted)
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = OpnameText),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (uiState.searchQuery.isBlank()) {
                                Text("Cari nama atau SKU", color = OpnameMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        search(Modifier.weight(1f))
        TbMobileFilterButton(
            onClick = {
                pendingCategory = uiState.categoryFilter
                showFilterSheet = true
            },
            active = uiState.categoryFilter != null,
            contentDescription = "Filter kategori produk",
            testTag = "stock-adjustment-open-filter",
        )
        if (compact) {
            FilledIconButton(
                onClick = onOpenForm,
                modifier = Modifier.size(52.dp).testTag("stock-adjustment-add"),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OpnameSurface,
                    contentColor = OpnamePrimaryDark,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat penyesuaian stok", modifier = Modifier.size(22.dp))
            }
        } else {
            Button(
                onClick = onOpenForm,
                modifier = Modifier.height(52.dp).testTag("stock-adjustment-add"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OpnamePrimaryDark),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat penyesuaian stok", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("Buat penyesuaian", maxLines = 1)
            }
        }
    }
    if (showFilterSheet) {
        TbMobileControlSheet(
            title = "Filter kategori",
            subtitle = "Pilih kategori produk",
            onDismiss = { showFilterSheet = false },
            testTag = "stock-adjustment-filter-sheet",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kategori", color = OpnameText, fontWeight = FontWeight.SemiBold)
                CategoryFilterDropdown(
                    selectedCategory = pendingCategory,
                    categories = uiState.categoryOptions,
                    onCategoryFilterChanged = { pendingCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TbMobileSheetDoneButton(
                onClick = {
                    onCategoryFilterChanged(pendingCategory)
                    showFilterSheet = false
                },
                testTag = "stock-adjustment-filter-done",
            )
        }
    }
}

@Composable
private fun CategoryFilterDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("stock-adjustment-category"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OpnameLine.copy(alpha = 0.62f)),
            color = OpnameSurface,
            contentColor = OpnameText,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedCategory ?: "Semua kategori",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                Icon(Icons.Outlined.ExpandMore, "Pilih kategori", tint = OpnameMuted, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp).heightIn(max = 320.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = OpnameSurface,
        ) {
            DropdownMenuItem(text = { Text("Semua kategori") }, onClick = {
                onCategoryFilterChanged(null)
                expanded = false
            })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = {
                    onCategoryFilterChanged(category)
                    expanded = false
                })
            }
        }
    }
}
