package com.tbterminal.app.ui.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.remote.DailySalesSummaryDto

@Composable
fun DailySalesReportCard(
    startDate: String,
    endDate: String,
    dailySales: List<DailySalesSummaryDto>,
    modifier: Modifier = Modifier
) {
    ReportSurfaceCard(modifier = modifier, contentPadding = 18.dp) {
        ReportSectionHeader(
            title = "Rekap Penjualan Harian",
            subtitle = "$startDate sampai $endDate"
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (dailySales.isEmpty()) {
            Text(
                text = "Belum ada data penjualan pada rentang waktu ini.",
                color = ReportColors.Outline
            )
        } else {
            DailySalesHeader()
            dailySales.forEach { sale ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sale.date,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = ReportColors.OnSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sale.transactionCount.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = ReportColors.Outline,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = sale.totalRevenue.toReportCurrency(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = ReportColors.OnSurface,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ReportDivider()
            }
        }
    }
}

@Composable
private fun DailySalesHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportColors.SurfaceSoft, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderText("Tanggal", Modifier.weight(1f))
        HeaderText("Transaksi", Modifier.weight(1f), TextAlign.Center)
        HeaderText("Total Omzet", Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun HeaderText(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = ReportColors.Outline,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.labelSmall,
        textAlign = textAlign
    )
}
