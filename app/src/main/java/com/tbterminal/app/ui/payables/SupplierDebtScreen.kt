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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton

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
    onDismissMessage: () -> Unit,
    onSupplierDetailClick: (String) -> Unit = {}
) {
    var showMobileOverview by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(DebtBackground)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            SupplierDebtMessage(uiState, onDismissMessage)
            if (!compact && (uiState.payables.isNotEmpty() || (!uiState.isLoading && uiState.errorMessage == null))) {
                SupplierDebtMetrics(uiState, compact = false)
            }
            SupplierDebtTableCard(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onRefresh = onRefresh,
                onPayClick = onPayClick,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                compact = compact,
                onOpenMobileOverview = { showMobileOverview = true },
                onSupplierDetailClick = onSupplierDetailClick,
            )
        }
    }

    if (showMobileOverview) {
        TbMobileControlSheet(
            title = "Ringkasan hutang",
            subtitle = "Nilai berdasarkan data pada halaman ini",
            onDismiss = { showMobileOverview = false },
            testTag = "supplier-debt-overview-sheet",
        ) {
            SupplierDebtMetrics(uiState, compact = true)
            TbMobileSheetDoneButton(
                onClick = { showMobileOverview = false },
                label = "Tutup",
                testTag = "supplier-debt-overview-done",
            )
        }
    }
}

@Composable
private fun SupplierDebtMessage(
    uiState: SupplierDebtUiState,
    onDismiss: () -> Unit
) {
    if (uiState.errorMessage != null && uiState.payables.isEmpty()) return
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
    Row(
        modifier = (if (compact) Modifier.fillMaxWidth() else Modifier.width(548.dp))
            .testTag("supplier-debt-overview"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DebtMetricItem(
            title = "Sisa di halaman",
            value = uiState.pageRemainingTotal.currencyText(),
            icon = Icons.Outlined.AccountBalanceWallet,
            emphasized = true,
            modifier = if (compact) Modifier.weight(1.55f) else Modifier.width(330.dp),
        )
        DebtMetricItem(
            title = "Belum lunas",
            value = "${uiState.unpaidCount} tagihan",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            emphasized = false,
            modifier = if (compact) Modifier.weight(1f) else Modifier.width(208.dp),
        )
    }
}

@Composable
private fun DebtMetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    emphasized: Boolean,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(92.dp),
        color = if (emphasized) DebtPrimary.copy(alpha = 0.12f) else DebtSurface,
        shape = RoundedCornerShape(18.dp),
        border = if (emphasized) null else BorderStroke(1.dp, DebtLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (emphasized) DebtPrimary.copy(alpha = 0.16f) else DebtSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = DebtPrimaryDark, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = DebtMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = if (emphasized) DebtPrimaryDark else DebtText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
