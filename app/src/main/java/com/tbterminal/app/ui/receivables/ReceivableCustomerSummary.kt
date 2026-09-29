package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.CustomerReceivableSummary
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivableCustomerSummaries(
    summaries: List<CustomerReceivableSummary>,
    compact: Boolean = false,
) {
    if (summaries.isEmpty()) return

    var showDetail by remember { mutableStateOf(false) }
    val totalRemaining = summaries.fold(BigDecimal.ZERO) { total, summary ->
        total + summary.totalRemaining
    }

    TextButton(
        onClick = { showDetail = true },
        modifier = Modifier.testTag("receivable-customer-summary"),
    ) {
        Icon(Icons.Outlined.Groups, contentDescription = null, tint = ReceivablePrimaryDark, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            if (compact) "Ringkasan" else "Ringkasan pelanggan",
            style = MaterialTheme.typography.labelLarge,
            color = ReceivablePrimaryDark,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(3.dp))
        Icon(Icons.Outlined.ChevronRight, contentDescription = "Buka ringkasan", tint = ReceivablePrimaryDark, modifier = Modifier.size(18.dp))
    }

    if (showDetail) {
        if (compact) {
            ModalBottomSheet(
                onDismissRequest = { showDetail = false },
                containerColor = ReceivableSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                ReceivableSummaryDetail(
                    summaries = summaries,
                    totalRemaining = totalRemaining,
                    onClose = { showDetail = false },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 20.dp).padding(bottom = 24.dp),
                )
            }
        } else {
            Dialog(onDismissRequest = { showDetail = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
                    color = ReceivableSurface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, ReceivableLine),
                ) {
                    ReceivableSummaryDetail(
                        summaries = summaries,
                        totalRemaining = totalRemaining,
                        onClose = { showDetail = false },
                        modifier = Modifier.padding(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivableSummaryDetail(
    summaries: List<CustomerReceivableSummary>,
    totalRemaining: BigDecimal,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.testTag("receivable-customer-summary-detail")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ringkasan pelanggan",
                    style = MaterialTheme.typography.titleLarge,
                    color = ReceivableText,
                    fontWeight = FontWeight.Bold,
                )
                Text("${summaries.size} pelanggan dengan piutang", style = MaterialTheme.typography.bodySmall, color = ReceivableMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total sisa", style = MaterialTheme.typography.labelSmall, color = ReceivableMuted)
                Text(
                    totalRemaining.currencyText(),
                    style = MaterialTheme.typography.titleSmall,
                    color = ReceivablePrimaryDark,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = ReceivableMuted)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = ReceivableLine)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            summaries.forEachIndexed { index, summary ->
                if (index > 0) HorizontalDivider(color = ReceivableLine)
                ReceivableSummaryRow(summary)
            }
        }
    }
}

@Composable
private fun ReceivableSummaryRow(summary: CustomerReceivableSummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = ReceivableSoft, shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(summary.customerName.initial(), color = ReceivablePrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                summary.customerName,
                style = MaterialTheme.typography.bodyLarge,
                color = ReceivableText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${summary.unpaidCount} tagihan", style = MaterialTheme.typography.bodySmall, color = ReceivableMuted)
                if (summary.overdueCount > 0) {
                    Text("  ·  ${summary.overdueCount} terlambat", style = MaterialTheme.typography.bodySmall, color = ReceivableDanger)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("Sisa piutang", style = MaterialTheme.typography.labelSmall, color = ReceivableMuted)
            Text(
                summary.totalRemaining.currencyText(),
                style = MaterialTheme.typography.bodyMedium,
                color = ReceivablePrimaryDark,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}
