package com.tbterminal.app.ui.reports.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.remote.DashboardMetricsDto

@Composable
fun LowStockWarningCard(
    metrics: DashboardMetricsDto?,
    modifier: Modifier = Modifier
) {
    val lowStockItems = metrics?.lowStockItems ?: emptyList()

    ReportSurfaceCard(modifier = modifier, contentPadding = 18.dp) {
        ReportSectionHeader(
            title = "Peringatan Stok Rendah",
            subtitle = "${metrics?.lowStockCount ?: 0} produk butuh perhatian stok."
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (lowStockItems.isEmpty()) {
            Text(
                text = "Stok semua produk dalam kondisi aman.",
                style = MaterialTheme.typography.bodySmall.copy(color = ReportColors.Outline)
            )
        } else {
            lowStockItems.forEachIndexed { index, item ->
                LowStockItem(
                    name = item.productName,
                    sku = item.sku,
                    stock = item.quantity.toString(),
                    minStock = item.minStock.toString(),
                    danger = index == 0
                )
            }
        }
    }
}

@Composable
private fun LowStockItem(
    name: String,
    sku: String,
    stock: String,
    minStock: String,
    danger: Boolean
) {
    val background = if (danger) ReportColors.ErrorSoft else ReportColors.OrangeSoft
    val accent = if (danger) ReportColors.Error else ReportColors.Orange

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ReportColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = accent,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ReportColors.OnSurface,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sku,
                style = MaterialTheme.typography.bodySmall.copy(color = ReportColors.Outline),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.weight(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StockValue(label = "Sisa", value = stock, color = accent)
            StockValue(label = "Min.", value = minStock, color = ReportColors.OnSurface)
        }
    }
}

@Composable
private fun StockValue(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = ReportColors.Outline,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.End
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = color,
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.End
        )
    }
}
