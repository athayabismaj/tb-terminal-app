package com.tbterminal.app.ui.receivables

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
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

internal val ReceivableBackground = TbBackground
internal val ReceivableSurface = TbSurface
internal val ReceivableSoft = TbSurfaceMuted
internal val ReceivableLine = TbOutline
internal val ReceivableText = TbText
internal val ReceivableMuted = TbTextMuted
internal val ReceivablePrimary = TbGreen
internal val ReceivablePrimaryDark = TbGreenDark
internal val ReceivableAccentText = TbGreenDark
internal val ReceivableDanger = TbError
internal val ReceivableWarning = TbAmber
internal val ReceivableInfo = TbGreen

@Composable
internal fun ReceivableTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ReceivablePrimaryDark,
    unfocusedBorderColor = ReceivableLine,
    focusedContainerColor = ReceivableSoft,
    unfocusedContainerColor = ReceivableSoft
)

@Composable
internal fun ReceivableToolbarTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ReceivableText,
    unfocusedTextColor = ReceivableText,
    cursorColor = ReceivablePrimaryDark,
    focusedBorderColor = ReceivablePrimaryDark,
    unfocusedBorderColor = ReceivableLine,
    focusedContainerColor = ReceivableSurface,
    unfocusedContainerColor = ReceivableSurface
)

@Composable
internal fun ReceivableHeaderText(
    text: String,
    modifier: Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, color = ReceivableMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun ReceivableAmountText(amount: BigDecimal, modifier: Modifier, strong: Boolean = false) {
    Text(
        amount.currencyText(),
        modifier = modifier,
        color = if (strong) ReceivableText else ReceivableMuted,
        fontSize = 13.sp,
        fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun ReceivableStatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "paid", "lunas" -> ReceivablePrimaryDark
        "partial", "sebagian" -> ReceivableWarning
        else -> ReceivableDanger
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
internal fun ReceivablePageIconButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) ReceivableSurface else ReceivableSoft)
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) ReceivableMuted else ReceivableMuted.copy(alpha = 0.35f))
    }
}

internal fun String.initial(): String = firstOrNull()?.uppercase() ?: "P"

internal fun String.shortTransactionId(): String = take(10).uppercase()

internal fun String.statusLabel(): String {
    return when (lowercase()) {
        "unpaid", "belum_lunas" -> "BELUM LUNAS"
        "partial", "sebagian" -> "SEBAGIAN"
        "paid", "lunas" -> "LUNAS"
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
    val dueDate = runCatching { LocalDate.parse(this.take(10)) }.getOrNull() ?: return ReceivableMuted
    val days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
    return when {
        days < 0 -> ReceivableDanger
        days <= 7 -> ReceivableWarning
        else -> ReceivableMuted
    }
}

internal fun BigDecimal.currencyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}
