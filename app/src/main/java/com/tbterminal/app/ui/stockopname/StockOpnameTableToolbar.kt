package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StockTableToolbar(
    uiState: StockOpnameUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari produk atau SKU...", color = OpnameMuted) },
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Cari produk", tint = OpnameMuted) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OpnameText,
                unfocusedTextColor = OpnameText,
                cursorColor = OpnamePrimaryDark,
                focusedBorderColor = OpnamePrimaryDark,
                unfocusedBorderColor = OpnameLine,
                focusedContainerColor = OpnameSurface,
                unfocusedContainerColor = OpnameSurface
            )
        )
        CategoryFilterDropdown(uiState.categoryFilter, uiState.categoryOptions, onCategoryFilterChanged)
    }
}

@Composable
private fun CategoryFilterDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .width(220.dp)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, OpnameLine),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = OpnameSurface,
                contentColor = OpnameText
            )
        ) {
            Text(
                text = selectedCategory ?: "Semua kategori",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "Pilih kategori",
                tint = OpnameMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .heightIn(max = 320.dp)
                .background(OpnameSurface)
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
