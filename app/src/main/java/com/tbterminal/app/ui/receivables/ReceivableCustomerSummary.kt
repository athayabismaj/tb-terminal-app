package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.CustomerReceivableSummary

@Composable
internal fun ReceivableCustomerSummaries(summaries: List<CustomerReceivableSummary>) {
    if (summaries.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ReceivableLine),
        colors = CardDefaults.cardColors(containerColor = ReceivableSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ringkasan per pelanggan", color = ReceivableText, fontWeight = FontWeight.Bold)
            summaries.take(8).forEachIndexed { index, summary ->
                if (index > 0) HorizontalDivider(color = ReceivableLine)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(summary.customerName, color = ReceivableText, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${summary.unpaidCount} terbuka • ${summary.overdueCount} terlambat • jatuh tempo ${summary.nearestDueDate ?: "-"}",
                            color = ReceivableMuted
                        )
                    }
                    Text(summary.totalRemaining.currencyText(), color = ReceivablePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
