package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ReceivableHeader(
    canAdjust: Boolean,
    onAddOpeningBalance: () -> Unit,
    onAddAdjustment: () -> Unit,
    compact: Boolean = false
) {
    if (!canAdjust) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReceivableHeaderAction(
            label = "Isi saldo awal",
            icon = Icons.Outlined.AddCircleOutline,
            onClick = onAddOpeningBalance,
            primary = true,
            modifier = if (compact) Modifier.weight(1f) else Modifier.width(176.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        ReceivableHeaderAction(
            label = "Sesuaikan",
            icon = Icons.Outlined.Tune,
            onClick = onAddAdjustment,
            primary = false,
            modifier = if (compact) Modifier.weight(1f) else Modifier.width(156.dp),
        )
    }
}

@Composable
private fun ReceivableHeaderAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        color = if (primary) ReceivablePrimary.copy(alpha = 0.13f) else ReceivableSurface,
        contentColor = ReceivablePrimaryDark,
        shape = RoundedCornerShape(17.dp),
        border = if (primary) null else BorderStroke(1.dp, ReceivableLine),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
internal fun ReceivableMessage(
    uiState: ReceivableUiState,
    onDismiss: () -> Unit
) {
    if (uiState.errorMessage != null && uiState.receivables.isEmpty()) return
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val tint = if (isError) ReceivableDanger else ReceivablePrimaryDark
    val background = if (isError) ReceivableDanger.copy(alpha = 0.1f) else ReceivablePrimary.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Outlined.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = tint, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup", color = tint) }
    }
}
