package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationalAuditFilterChips(
    selectedAction: String?,
    onActionFilterChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = selectedAction == null,
            onClick = { onActionFilterChanged(null) },
            label = { Text("Semua") }
        )
        FilterChip(
            selected = selectedAction == "INSERT",
            onClick = { onActionFilterChanged("INSERT") },
            label = { Text("Tambah") }
        )
        FilterChip(
            selected = selectedAction == "UPDATE",
            onClick = { onActionFilterChanged("UPDATE") },
            label = { Text("Ubah") }
        )
        FilterChip(
            selected = selectedAction == "DELETE",
            onClick = { onActionFilterChanged("DELETE") },
            label = { Text("Nonaktif/Batal") }
        )
    }
}
