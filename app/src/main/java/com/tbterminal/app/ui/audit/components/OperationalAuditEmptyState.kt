package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

@Composable
fun OperationalAuditEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(TbGreenLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = TbGreenDark)
            }
            Text(
                text = "Belum ada aktivitas",
                modifier = Modifier.padding(top = 14.dp),
                color = TbText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Aktivitas operasional pada periode ini akan tampil di sini.",
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 420.dp),
                color = TbTextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
