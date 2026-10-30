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
    val Primary = Color(0xFF256B57)
    val PrimaryDark = Color(0xFF1E5646)
    val PrimarySoft = Color(0xFFE8F2EE)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val SurfaceSoft = Color(0xFFF8FAFC)
    val OnSurface = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF334155)
    val Outline = Color(0xFF64748B)
    val OutlineSoft = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Error = Color(0xFFDC2626)
    val ErrorSoft = Color(0xFFFEE2E2)
    val Secondary = Color(0xFF2563EB)
    val BlueSoft = Color(0xFFEFF6FF)
    val Orange = Color(0xFFD97706)
    val OrangeSoft = Color(0xFFFEF3C7)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, ReportColors.OutlineSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ReportColors.OnSurface
                )
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ReportColors.Outline,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ReportDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = ReportColors.Slate100)
}

fun BigDecimal.toReportCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
}

fun Double.toReportCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
}
