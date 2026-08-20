package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ReceivableHeader(
    canAdjust: Boolean,
    onAddOpeningBalance: () -> Unit,
    onAddAdjustment: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Piutang Pelanggan",
            modifier = Modifier.weight(1f),
            color = ReceivableText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
        if (canAdjust) {
            Button(onClick = onAddAdjustment) { Text("Adjustment") }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = onAddOpeningBalance) { Text("Tambah Saldo Awal") }
        }
    }
}

@Composable
internal fun ReceivableMessage(
    uiState: ReceivableUiState,
    onDismiss: () -> Unit
) {
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
