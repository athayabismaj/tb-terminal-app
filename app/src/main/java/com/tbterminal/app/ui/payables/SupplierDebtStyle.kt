package com.tbterminal.app.ui.payables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

internal val DebtBackground = TbBackground
internal val DebtSurface = TbSurface
internal val DebtSoft = TbSurfaceMuted
internal val DebtLine = TbOutline
internal val DebtText = TbText
internal val DebtMuted = TbTextMuted
internal val DebtPrimary = TbGreen
internal val DebtPrimaryDark = TbGreenDark
internal val DebtAccentText = TbGreenDark
internal val DebtDanger = TbError
internal val DebtWarning = TbAmber
internal val DebtInfo = TbGreen

@Composable
internal fun debtTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
    Column(modifier = modifier.padding(horizontal = 6.dp), horizontalAlignment = align) {
        Text(
            text,
            color = DebtMuted,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
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
