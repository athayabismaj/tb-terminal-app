package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.tbterminal.app.data.remote.DashboardMetricsDto

@Composable
fun ReportsSummaryCards(metrics: DashboardMetricsDto?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashboardSummaryCard(
            title = "Pendapatan Hari Ini",
            value = (metrics?.totalRevenueToday ?: 0.0).toReportCurrency(),
            description = "Ringkasan dari dashboard",
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            iconTint = ReportColors.Secondary,
            iconBackground = ReportColors.BlueSoft,
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            title = "Pendapatan Bulan Ini",
            value = (metrics?.totalRevenueThisMonth ?: 0.0).toReportCurrency(),
            description = "Akumulasi bulan berjalan",
            icon = Icons.Outlined.AccountBalanceWallet,
            iconTint = ReportColors.Primary,
            iconBackground = ReportColors.PrimarySoft,
            modifier = Modifier.weight(1f)
        )
        DashboardSummaryCard(
            title = "Total Piutang Aktif",
            value = (metrics?.totalActiveReceivables ?: 0.0).toReportCurrency(),
            description = "${metrics?.activeReceivableCount ?: 0} tagihan aktif",
            icon = Icons.Outlined.CreditScore,
            iconTint = ReportColors.Orange,
            iconBackground = ReportColors.OrangeSoft,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DashboardSummaryCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ReportColors.Surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ReportColors.OutlineSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ReportColors.Outline,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ReportColors.OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = iconTint,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
