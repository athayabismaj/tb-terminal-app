package com.tbterminal.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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

@Composable
fun TopFloatingCapsule(
    message: String?,
    error: String? = null,
    isError: Boolean = error != null,
    onDismiss: () -> Unit = {},
    durationMillis: Long = 3500L,
    modifier: Modifier = Modifier,
) {
    val activeText = error ?: message
    val effectiveIsError = isError || (error != null)

    var isVisible by remember(activeText) { mutableStateOf(activeText != null) }

    LaunchedEffect(activeText) {
        if (activeText != null) {
            isVisible = true
            delay(durationMillis)
            isVisible = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible && activeText != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(200),
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            border = BorderStroke(
                1.dp,
                if (effectiveIsError) Color(0xFFFECACA) else Color(0xFFC2DFD4),
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .clickable {
                    isVisible = false
                    onDismiss()
                }
                .wrapContentSize(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (effectiveIsError) Color(0xFFFEE2E2) else Color(0xFFE1EFEA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (effectiveIsError) Icons.Outlined.ErrorOutline else Icons.Outlined.Check,
                        contentDescription = if (effectiveIsError) "Gagal" else "Berhasil",
                        tint = if (effectiveIsError) Color(0xFFDC2626) else Color(0xFF256B57),
                        modifier = Modifier.size(14.dp),
                    )
                }

                Text(
                    text = activeText.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (effectiveIsError) Color(0xFF991B1B) else Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            isVisible = false
                            onDismiss()
                        },
                )
            }
        }
    }
}
