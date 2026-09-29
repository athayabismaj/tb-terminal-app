package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ReceivableMetrics(uiState: ReceivableUiState, compact: Boolean) {
    Row(
        modifier = (if (compact) Modifier.fillMaxWidth() else Modifier.width(548.dp))
            .testTag("receivable-overview"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ReceivableMetricItem(
            title = "Sisa di halaman",
            value = uiState.pageRemainingTotal.currencyText(),
            icon = Icons.Outlined.AccountBalanceWallet,
            emphasized = true,
            modifier = if (compact) Modifier.weight(1.55f) else Modifier.width(330.dp),
        )
        ReceivableMetricItem(
            title = "Belum lunas",
            value = "${uiState.unpaidCount} tagihan",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            emphasized = false,
            modifier = if (compact) Modifier.weight(1f) else Modifier.width(208.dp),
        )
    }
}

@Composable
private fun ReceivableMetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    emphasized: Boolean,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(92.dp),
        color = if (emphasized) ReceivablePrimary.copy(alpha = 0.12f) else ReceivableSurface,
        shape = RoundedCornerShape(18.dp),
        border = if (emphasized) null else BorderStroke(1.dp, ReceivableLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (emphasized) ReceivablePrimary.copy(alpha = 0.16f) else ReceivableSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(30.dp),
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = ReceivablePrimaryDark, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = ReceivableMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = if (emphasized) ReceivablePrimaryDark else ReceivableText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
