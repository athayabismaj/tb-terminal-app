package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

@Composable
fun OperationalAuditDateSelector(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = TbSurface,
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = TbGreenDark,
                modifier = Modifier.width(20.dp),
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = TbText,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TbTextMuted,
                modifier = Modifier.width(20.dp),
            )
        }
    }
}

@Composable
fun OperationalAuditActionDropdown(
    selectedAction: String?,
    onActionFilterChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        null to "Semua kategori",
        "INSERT" to "Tambah data",
        "UPDATE" to "Ubah data",
        "DELETE" to "Nonaktif / batal"
    )
    val selectedLabel = options.firstOrNull { it.first == selectedAction }?.second ?: "Semua kategori"
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = TbSurface,
            border = BorderStroke(1.dp, TbOutline),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.FilterList,
                    contentDescription = null,
                    tint = TbTextMuted,
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    selectedLabel,
                    modifier = Modifier.weight(1f),
                    color = TbText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = TbTextMuted,
                    modifier = Modifier.width(20.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp),
        ) {
            options.forEach { (action, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (action == selectedAction) TbGreenDark else TbText,
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
        color = TbSurfaceMuted,
        modifier = modifier.height(44.dp)
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
                    onClick = { onSelected(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AuditDatePresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = if (selected) TbGreenLight else TbSurfaceMuted,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) TbGreenDark else TbTextMuted,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
