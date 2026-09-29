package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.components.SkeletonBox

@Composable
internal fun DebtTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(DebtSoft).padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DebtHeaderText("Supplier", Modifier.weight(2f))
        DebtHeaderText("Pembelian", Modifier.weight(1.35f))
        DebtHeaderText("Total / dibayar", Modifier.weight(1.45f), Alignment.End)
        DebtHeaderText("Sisa", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("Jatuh tempo", Modifier.weight(1.5f))
        DebtHeaderText("Status", Modifier.weight(1.1f), Alignment.CenterHorizontally)
        DebtHeaderText("Aksi", Modifier.weight(0.8f), Alignment.End)
    }
}

@Composable
internal fun SupplierDebtRows(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onPayClick: (SupplierPayable) -> Unit,
    compact: Boolean = false,
) {
    when {
        uiState.isLoading && uiState.payables.isEmpty() -> SupplierDebtLoading(modifier, compact)
        uiState.errorMessage != null && uiState.payables.isEmpty() -> SupplierDebtError(modifier)
        uiState.filteredPayables.isEmpty() -> SupplierDebtEmpty(modifier)
        compact -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.filteredPayables.forEach { payable -> SupplierDebtCompactCard(payable, onPayClick) }
        }
        else -> Column(modifier = modifier.fillMaxWidth()) {
            uiState.filteredPayables.forEachIndexed { index, payable ->
                if (index > 0) HorizontalDivider(color = DebtLine)
                SupplierDebtRow(payable, onPayClick)
            }
        }
    }
}

@Composable
private fun SupplierDebtCompactCard(payable: SupplierPayable, onPayClick: (SupplierPayable) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("supplier-debt-${payable.id}"),
        color = DebtSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DebtLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SupplierInitial(payable.supplierName)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        payable.supplierName,
                        style = MaterialTheme.typography.titleSmall,
                        color = DebtText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(payable.purchaseId.shortId(), style = MaterialTheme.typography.bodySmall, color = DebtMuted, maxLines = 1)
                }
                StatusBadge(payable.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Sisa hutang", style = MaterialTheme.typography.labelMedium, color = DebtMuted)
                Text(
                    payable.remainingAmount.currencyText(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = DebtText,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SupplierDebtAmountDetail("Total tagihan", payable.amount.currencyText(), Modifier.weight(1f))
                SupplierDebtAmountDetail("Sudah dibayar", payable.paidAmount.currencyText(), Modifier.weight(1f))
            }

            HorizontalDivider(color = DebtLine.copy(alpha = 0.75f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jatuh tempo", style = MaterialTheme.typography.labelSmall, color = DebtMuted)
                    Text(
                        payable.dueDate.simpleDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = payable.dueDate.dueColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(payable.dueDate.dueRelativeText(), style = MaterialTheme.typography.labelSmall, color = payable.dueDate.dueColor())
                }
                if (!payable.isPaid()) {
                    FilledTonalButton(
                        onClick = { onPayClick(payable) },
                        modifier = Modifier.height(48.dp).testTag("pay-supplier-debt-${payable.id}"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DebtPrimary.copy(alpha = 0.14f),
                            contentColor = DebtPrimaryDark,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                    ) { Text("Bayar", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtAmountDetail(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DebtMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = DebtText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SupplierDebtRow(payable: SupplierPayable, onPayClick: (SupplierPayable) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("supplier-debt-${payable.id}").padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            SupplierInitial(payable.supplierName)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    payable.supplierName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DebtText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(payable.createdAt.simpleDate(), style = MaterialTheme.typography.labelSmall, color = DebtMuted)
            }
        }
        Text(
            payable.purchaseId.shortId(),
            Modifier.weight(1.35f),
            style = MaterialTheme.typography.bodySmall,
            color = DebtMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(modifier = Modifier.weight(1.45f).padding(end = 8.dp), horizontalAlignment = Alignment.End) {
            Text(payable.amount.currencyText(), style = MaterialTheme.typography.bodySmall, color = DebtText, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(payable.paidAmount.currencyText(), style = MaterialTheme.typography.labelSmall, color = DebtMuted, maxLines = 1)
        }
        Text(
            payable.remainingAmount.currencyText(),
            Modifier.weight(1.25f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = DebtText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Column(modifier = Modifier.weight(1.5f).padding(start = 8.dp)) {
            Text(payable.dueDate.simpleDate(), style = MaterialTheme.typography.bodySmall, color = DebtText, fontWeight = FontWeight.Medium)
            Text(payable.dueDate.dueRelativeText(), style = MaterialTheme.typography.labelSmall, color = payable.dueDate.dueColor(), maxLines = 1)
        }
        Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.Center) { StatusBadge(payable.status) }
        Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
            if (payable.isPaid()) {
                Text("Lunas", style = MaterialTheme.typography.labelMedium, color = DebtMuted)
            } else {
                Button(
                    onClick = { onPayClick(payable) },
                    modifier = Modifier.height(48.dp).testTag("pay-supplier-debt-${payable.id}"),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DebtPrimaryDark),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Bayar", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SupplierInitial(name: String) {
    Surface(color = DebtSoft, shape = CircleShape, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.initial(), color = DebtPrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SupplierDebtLoading(modifier: Modifier, compact: Boolean) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("supplier-debt-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(if (compact) 4 else 5) {
            SkeletonBox(Modifier.fillMaxWidth().height(if (compact) 190.dp else 62.dp))
        }
    }
}

@Composable
private fun SupplierDebtEmpty(modifier: Modifier) {
    SupplierDebtFeedback(modifier, "Hutang supplier tidak ditemukan", "Coba ubah pencarian atau filter yang digunakan.", DebtText)
}

@Composable
private fun SupplierDebtError(modifier: Modifier) {
    SupplierDebtFeedback(modifier, "Hutang supplier gagal dimuat", "Tarik layar ke bawah untuk mencoba lagi.", DebtDanger)
}

@Composable
private fun SupplierDebtFeedback(
    modifier: Modifier,
    title: String,
    message: String,
    titleColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DebtSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DebtLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = DebtMuted, textAlign = TextAlign.Center)
        }
    }
}

private fun SupplierPayable.isPaid(): Boolean = status.equals(SupplierDebtStatusFilter.Paid.apiValue, ignoreCase = true)
