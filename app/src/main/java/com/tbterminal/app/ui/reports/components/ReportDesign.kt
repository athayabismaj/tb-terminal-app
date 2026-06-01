package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object ReportColors {
    val Primary = Color(0xFF1D9E75)
    val PrimaryDark = Color(0xFF008560)
    val PrimarySoft = Color(0xFFECFDF5)
    val Background = Color(0xFFF8FAFB)
    val Surface = Color.White
    val SurfaceSoft = Color(0xFFF8FAFC)
    val OnSurface = Color(0xFF161D1F)
    val OnSurfaceVariant = Color(0xFF3D4943)
    val Outline = Color(0xFF6D7A73)
    val OutlineSoft = Color(0xFFE5E7EB)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Error = Color(0xFFBA1A1A)
    val ErrorSoft = Color(0xFFFFF1F1)
    val Secondary = Color(0xFF0060A8)
    val BlueSoft = Color(0xFFEAF4FF)
    val Orange = Color(0xFFEA580C)
    val OrangeSoft = Color(0xFFFFF4E6)
    val Purple = Color(0xFF7C3AED)
    val PurpleSoft = Color(0xFFF3E8FF)
}

@Composable
fun ReportSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ReportColors.Surface,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, ReportColors.OutlineSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun ReportSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = ReportColors.OnSurface
                )
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ReportColors.Outline,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ReportDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = ReportColors.OutlineSoft)
}

fun BigDecimal.toReportCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
}

fun Double.toReportCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
}
