package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ReceivableMetrics(uiState: ReceivableUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ReceivableMetricCard(
            modifier = Modifier.weight(1f),
            title = "TOTAL DATA",
            value = "${uiState.total}",
            subtitle = "Piutang pada filter aktif",
            icon = Icons.Outlined.Person,
            tint = ReceivablePrimaryDark
        )
        ReceivableMetricCard(
            modifier = Modifier.weight(1f),
            title = "SISA HALAMAN INI",
            value = uiState.pageRemainingTotal.currencyText(),
            subtitle = "Akumulasi data yang tampil",
            icon = Icons.Outlined.AccountBalanceWallet,
            tint = ReceivableInfo
        )
        ReceivableMetricCard(
            modifier = Modifier.weight(1f),
            title = "BELUM LUNAS",
            value = "${uiState.unpaidCount}",
            subtitle = "Butuh tindak lanjut",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            tint = ReceivableWarning
        )
    }
}

@Composable
private fun ReceivableMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = ReceivableSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ReceivableLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = ReceivableMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = ReceivableText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
