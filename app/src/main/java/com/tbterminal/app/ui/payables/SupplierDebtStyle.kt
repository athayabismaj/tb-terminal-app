package com.tbterminal.app.ui.payables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

internal val DebtBackground = Color(0xFFF4FAFD)
internal val DebtSurface = Color.White
internal val DebtSoft = Color(0xFFF1F5F9)
internal val DebtLine = Color(0xFFE2E8F0)
internal val DebtText = Color(0xFF0F172A)
internal val DebtMuted = Color(0xFF64748B)
internal val DebtPrimary = Color(0xFF10B981)
internal val DebtPrimaryDark = Color(0xFF059669)
internal val DebtAccentText = Color(0xFF42588F)
internal val DebtDanger = Color(0xFFEF4444)
internal val DebtWarning = Color(0xFFF59E0B)
internal val DebtInfo = Color(0xFF3B82F6)

@Composable
internal fun DebtTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DebtPrimaryDark,
    unfocusedBorderColor = DebtLine,
    focusedContainerColor = DebtSoft,
    unfocusedContainerColor = DebtSoft
)

@Composable
internal fun DebtHeaderText(
    text: String,
    modifier: Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, color = DebtMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun DebtAmountText(amount: BigDecimal, modifier: Modifier, strong: Boolean = false) {
    Text(
        amount.currencyText(),
        modifier = modifier,
        color = if (strong) DebtText else DebtMuted,
        fontSize = 13.sp,
        fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun StatusBadge(status: String) {
    val color = when (status) {
        "lunas" -> DebtPrimaryDark
        "sebagian" -> DebtWarning
        else -> DebtDanger
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(status.statusLabel(), color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun PageIconButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color.White else DebtSoft)
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) DebtMuted else DebtMuted.copy(alpha = 0.35f))
    }
}

internal fun String.initial(): String = firstOrNull()?.uppercase() ?: "S"

internal fun String.shortId(): String = take(8).uppercase()

internal fun String.statusLabel(): String {
    return when (this) {
        "belum_lunas" -> "BELUM LUNAS"
        "sebagian" -> "SEBAGIAN"
        "lunas" -> "LUNAS"
        else -> uppercase()
    }
}

internal fun String.simpleDate(): String {
    return take(10).ifBlank { "-" }
}

internal fun String.dueRelativeText(): String {
    val dueDate = runCatching { LocalDate.parse(this.take(10)) }.getOrNull() ?: return "-"
    val days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
    return when {
        days < 0 -> "Lewat ${kotlin.math.abs(days)} hari"
        days == 0L -> "Jatuh tempo hari ini"
        days <= 7 -> "$days hari lagi"
        else -> "Aman"
    }
}

internal fun String.dueColor(): Color {
    val dueDate = runCatching { LocalDate.parse(this.take(10)) }.getOrNull() ?: return DebtMuted
    val days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
    return when {
        days < 0 -> DebtDanger
        days <= 7 -> DebtWarning
        else -> DebtMuted
    }
}

internal fun BigDecimal.currencyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}
