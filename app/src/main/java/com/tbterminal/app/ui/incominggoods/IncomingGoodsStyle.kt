package com.tbterminal.app.ui.incominggoods

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
import java.text.NumberFormat
import java.util.Locale

internal val IncomingBackground = Color(0xFFF4FAFD)
internal val IncomingSurface = Color.White
internal val IncomingSoft = Color(0xFFF1F5F9)
internal val IncomingLine = Color(0xFFE2E8F0)
internal val IncomingText = Color(0xFF0F172A)
internal val IncomingMuted = Color(0xFF64748B)
internal val IncomingPrimary = Color(0xFF10B981)
internal val IncomingPrimaryDark = Color(0xFF059669)
internal val IncomingDanger = Color(0xFFEF4444)
internal val IncomingWarning = Color(0xFFF59E0B)

@Composable
internal fun IncomingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IncomingPrimary,
    unfocusedBorderColor = IncomingLine,
    focusedContainerColor = IncomingSoft,
    unfocusedContainerColor = IncomingSoft
)

@Composable
internal fun HeaderText(
    text: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = IncomingMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun LoadingState() {
    com.tbterminal.app.ui.components.SkeletonList(modifier = Modifier.fillMaxSize(), itemCount = 6)
}

@Composable
internal fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = IncomingMuted)
    }
}

internal fun BigDecimal.qtyText(): String {
    return stripTrailingZeros().toPlainString()
}

internal fun BigDecimal?.currencyText(): String {
    if (this == null) return "-"
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
        .format(this)
        .replace(",00", "")
}
