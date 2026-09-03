package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

internal val OpnameBackground = Color(0xFFF4FAFD)
internal val OpnameSurface = Color.White
internal val OpnameSoft = Color(0xFFF1F5F9)
internal val OpnameLine = Color(0xFFE2E8F0)
internal val OpnameText = Color(0xFF0F172A)
internal val OpnameMuted = Color(0xFF64748B)
internal val OpnamePrimary = Color(0xFF10B981)
internal val OpnamePrimaryDark = Color(0xFF059669)
internal val OpnameDanger = Color(0xFFEF4444)
internal val OpnameWarning = Color(0xFFF59E0B)

@Composable
internal fun OpnameTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OpnamePrimary,
    unfocusedBorderColor = OpnameLine,
    focusedContainerColor = OpnameSoft,
    unfocusedContainerColor = OpnameSoft
)

@Composable
internal fun HeaderText(
    text: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = OpnameMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun LoadingState() {
    com.tbterminal.app.ui.components.SkeletonList(modifier = Modifier.fillMaxSize(), itemCount = 6)
}

@Composable
internal fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = OpnameMuted)
    }
}

internal fun BigDecimal.qtyText(): String {
    return stripTrailingZeros().toPlainString()
}

internal fun BigDecimal.signedQtyText(): String {
    val value = qtyText()
    return if (signum() > 0) "+$value" else value
}

internal fun BigDecimal?.differenceColor(): Color {
    if (this == null || compareTo(BigDecimal.ZERO) == 0) return OpnameMuted
    return if (signum() > 0) OpnamePrimaryDark else OpnameDanger
}
