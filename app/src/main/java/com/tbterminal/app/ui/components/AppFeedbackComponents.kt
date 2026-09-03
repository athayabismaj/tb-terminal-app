package com.tbterminal.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AppConfirmationSpec(
    val title: String,
    val target: String,
    val consequence: String,
    val confirmLabel: String,
) {
    init {
        require(title.isNotBlank() && target.isNotBlank() && consequence.isNotBlank() && confirmLabel.isNotBlank())
    }
}

enum class StatusTone { SUCCESS, WARNING, DANGER, INFO, NEUTRAL }

data class StatusPresentation(val label: String, val tone: StatusTone)

fun statusPresentation(status: String): StatusPresentation {
    val normalized = status.trim().uppercase().ifBlank { "-" }
    val tone = when (normalized) {
        "LUNAS", "PAID", "APPROVED", "ACTIVE", "AKTIF" -> StatusTone.SUCCESS
        "DP", "PARTIAL", "HUTANG", "UNPAID", "PENDING" -> StatusTone.WARNING
        "VOIDED", "REFUNDED", "REVERSED", "FAILED", "BATAL" -> StatusTone.DANGER
        "USED", "PROCESSING", "RUNNING" -> StatusTone.INFO
        else -> StatusTone.NEUTRAL
    }
    return StatusPresentation(normalized, tone)
}

@Composable
fun AppSnackbar(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
            onDismiss()
        }
    }
    SnackbarHost(hostState = hostState, modifier = modifier)
}

@Composable
fun AppConfirmDialog(
    spec: AppConfirmationSpec,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        title = { Text(spec.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(spec.target, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(spec.consequence, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "Memproses" else spec.confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Batal") } },
    )
}

@Composable
fun AppErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Warning, contentDescription = "Terjadi masalah", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) OutlinedButton(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
fun AppEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun AppStatusChip(status: String, modifier: Modifier = Modifier) {
    val presentation = statusPresentation(status)
    val color = when (presentation.tone) {
        StatusTone.SUCCESS -> Color(0xFF047857)
        StatusTone.WARNING -> Color(0xFFB26A00)
        StatusTone.DANGER -> MaterialTheme.colorScheme.error
        StatusTone.INFO -> Color(0xFF0369A1)
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f), contentColor = color) {
        Text(
            presentation.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
