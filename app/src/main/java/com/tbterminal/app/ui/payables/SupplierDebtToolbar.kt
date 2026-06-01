package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SupplierDebtToolbar(
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari supplier atau ID pembelian...", color = DebtMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = DebtMuted) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = DebtTextFieldColors()
        )
        StatusFilterButton(uiState.statusFilter, onStatusFilterChanged)
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DebtLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = DebtPrimaryDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat Ulang", color = DebtAccentText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusFilterButton(
    selected: SupplierDebtStatusFilter,
    onSelect: (SupplierDebtStatusFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DebtLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Text(selected.label, color = DebtText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(18.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = DebtMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SupplierDebtStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        expanded = false
                        onSelect(filter)
                    }
                )
            }
        }
    }
}
