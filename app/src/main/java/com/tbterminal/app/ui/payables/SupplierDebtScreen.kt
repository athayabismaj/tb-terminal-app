package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.SupplierPayable

@Composable
internal fun SupplierDebtScreen(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(DebtBackground)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (compact) 16.dp else 40.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 28.dp)
        ) {
            SupplierDebtMessage(uiState, onDismissMessage)
            SupplierDebtMetrics(uiState, compact)
            SupplierDebtTableCard(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onRefresh = onRefresh,
                onPayClick = onPayClick,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact
            )
        }
    }
}

@Composable
private fun SupplierDebtMessage(
    uiState: SupplierDebtUiState,
    onDismiss: () -> Unit
) {
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val tint = if (isError) DebtDanger else DebtPrimaryDark
    val background = if (isError) DebtDanger.copy(alpha = 0.1f) else DebtPrimary.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Outlined.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = tint, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup", color = tint) }
    }
}

@Composable
private fun SupplierDebtMetrics(uiState: SupplierDebtUiState, compact: Boolean) {
    if (compact) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DebtSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, DebtLine)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(DebtPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = DebtPrimaryDark)
                    }
                    Column {
                        Text("Sisa hutang", color = DebtMuted, fontSize = 12.sp)
                        Text(
                            uiState.pageRemainingTotal.currencyText(),
                            color = DebtText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DebtSummaryValue("Total tagihan", uiState.total.toString(), Modifier.weight(1f))
                    DebtSummaryValue("Belum lunas", uiState.unpaidCount.toString(), Modifier.weight(1f))
                }
            }
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DebtMetricCard(
            modifier = Modifier.weight(1f),
            title = "TOTAL DATA",
            value = "${uiState.total}",
            subtitle = "Tagihan pada filter aktif",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            tint = DebtPrimaryDark
        )
        DebtMetricCard(
            modifier = Modifier.weight(1f),
            title = "SISA HALAMAN INI",
            value = uiState.pageRemainingTotal.currencyText(),
            subtitle = "Akumulasi data yang tampil",
            icon = Icons.Outlined.AccountBalanceWallet,
            tint = DebtInfo
        )
        DebtMetricCard(
            modifier = Modifier.weight(1f),
            title = "BELUM LUNAS",
            value = "${uiState.unpaidCount}",
            subtitle = "Butuh tindak lanjut",
            icon = Icons.Outlined.Payments,
            tint = DebtWarning
        )
    }
}

@Composable
private fun DebtSummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(DebtSoft).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = DebtMuted, fontSize = 11.sp)
        Text(value, color = DebtText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DebtMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = DebtSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DebtLine)
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
                Text(title, color = DebtMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = DebtText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
