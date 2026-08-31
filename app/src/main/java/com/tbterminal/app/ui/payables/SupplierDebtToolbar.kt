package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun SupplierDebtToolbar(
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    compact: Boolean = false
) {
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari supplier atau nomor pembelian", color = DebtMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = DebtMuted) },
            modifier = fieldModifier.height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = DebtTextFieldColors()
        )
    }
    if (compact) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            search(Modifier.fillMaxWidth())
            StatusFilterButton(uiState.statusFilter, onStatusFilterChanged, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            search(Modifier.weight(1f))
            StatusFilterButton(uiState.statusFilter, onStatusFilterChanged, Modifier.width(220.dp))
        }
    }
}

@Composable
private fun StatusFilterButton(
    selected: SupplierDebtStatusFilter,
    onSelect: (SupplierDebtStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DebtLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Text(
                if (selected == SupplierDebtStatusFilter.All) "Semua status" else selected.label,
                color = DebtText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
