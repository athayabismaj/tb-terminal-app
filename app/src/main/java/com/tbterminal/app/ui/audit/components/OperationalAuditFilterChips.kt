package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OperationalAuditActionDropdown(
    selectedAction: String?,
    onActionFilterChanged: (String?) -> Unit
) {
    val options = listOf(
        null to "Semua kategori",
        "INSERT" to "Tambah data",
        "UPDATE" to "Ubah data",
        "DELETE" to "Nonaktif / batal"
    )
    val selectedLabel = options.firstOrNull { it.first == selectedAction }?.second ?: "Semua kategori"
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .width(240.dp)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                    Text(
                        selectedLabel,
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(240.dp)
                .background(Color.White)
        ) {
            options.forEach { (action, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (action == selectedAction) Color(0xFF059669) else Color(0xFF0F172A),
                            fontWeight = if (action == selectedAction) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    onClick = {
                        expanded = false
                        onActionFilterChanged(action)
                    }
                )
            }
        }
    }
}

@Composable
fun OperationalAuditDatePresets(
    selectedPreset: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Hari ini", "7 hari", "30 hari").forEach { preset ->
                AuditDatePresetChip(
                    text = preset,
                    selected = selectedPreset == preset,
                    onClick = { onSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun AuditDatePresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF86F8C9) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color(0xFF00513A) else Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
