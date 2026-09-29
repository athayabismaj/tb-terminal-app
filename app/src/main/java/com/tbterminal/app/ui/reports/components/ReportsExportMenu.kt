package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.repository.ReportCsvType

@Composable
fun ReportsExportMenu(
    isExporting: Boolean,
    onExport: (ReportCsvType) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        FilledTonalButton(
            onClick = { expanded = true },
            enabled = !isExporting,
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = 48.dp)
                .testTag("report-export"),
            shape = RoundedCornerShape(14.dp),
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(
                text = if (isExporting) "Memproses" else "Ekspor",
                fontWeight = FontWeight.SemiBold,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ReportColors.Surface, RoundedCornerShape(14.dp)),
        ) {
            ReportCsvType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        expanded = false
                        onExport(type)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}
