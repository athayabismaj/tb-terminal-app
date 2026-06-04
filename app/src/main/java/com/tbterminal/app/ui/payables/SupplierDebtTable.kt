package com.tbterminal.app.ui.payables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.SupplierPayable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues

@Composable
internal fun DebtTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DebtSoft)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebtHeaderText("SUPPLIER", Modifier.weight(2.2f))
        DebtHeaderText("PEMBELIAN", Modifier.weight(1.4f))
        DebtHeaderText("TOTAL", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("DIBAYAR", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("SISA", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("JATUH TEMPO", Modifier.weight(1.35f))
        DebtHeaderText("STATUS", Modifier.weight(1.2f), Alignment.CenterHorizontally)
        DebtHeaderText("AKSI", Modifier.weight(1f), Alignment.End)
    }
}

@Composable
internal fun SupplierDebtRows(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onPayClick: (SupplierPayable) -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DebtPrimaryDark)
            }

            uiState.errorMessage != null && uiState.payables.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Utang supplier gagal dimuat.",
                    color = DebtDanger,
                    fontWeight = FontWeight.Bold
                )
            }

            uiState.filteredPayables.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada utang supplier yang cocok.",
                    color = DebtMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }

            else -> Column(modifier = Modifier.fillMaxWidth()) {
                uiState.filteredPayables.forEach { payable ->
                    SupplierDebtRow(payable, onPayClick)
                    HorizontalDivider(color = DebtLine.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtRow(
    payable: SupplierPayable,
    onPayClick: (SupplierPayable) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically) {
            SupplierInitial(payable.supplierName)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(payable.supplierName, color = DebtText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(payable.createdAt.simpleDate(), color = DebtMuted, fontSize = 11.sp)
            }
        }
        Text(payable.purchaseId.shortId(), modifier = Modifier.weight(1.4f), color = DebtMuted, fontSize = 12.sp)
        DebtAmountText(payable.amount, Modifier.weight(1.25f))
        DebtAmountText(payable.paidAmount, Modifier.weight(1.25f))
        DebtAmountText(payable.remainingAmount, Modifier.weight(1.25f), strong = true)
        Column(modifier = Modifier.weight(1.35f)) {
            Text(payable.dueDate, color = DebtText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(payable.dueDate.dueRelativeText(), color = payable.dueDate.dueColor(), fontSize = 11.sp)
        }
        Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
            StatusBadge(payable.status)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (payable.status == SupplierDebtStatusFilter.Paid.apiValue) {
                Text("Lunas", color = DebtMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = { onPayClick(payable) },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtPrimaryDark),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Bayar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SupplierInitial(name: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(DebtPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(name.initial(), color = DebtPrimaryDark, fontWeight = FontWeight.Black)
    }
}
