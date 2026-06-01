package com.tbterminal.app.ui.payables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.SupplierPayable

@Composable
internal fun SupplierPaymentDialog(
    payable: SupplierPayable,
    uiState: SupplierDebtUiState,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (SupplierPaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Utang Supplier", color = DebtText, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PaymentSummary(payable)
                PaymentInputField(
                    value = uiState.paymentAmountInput,
                    onValueChange = onAmountChanged,
                    label = "Nominal pembayaran",
                    prefix = "Rp"
                )
                PaymentMethodCards(uiState.paymentMethod, onMethodChanged)
                PaymentInputField(
                    value = uiState.referenceInput,
                    onValueChange = onReferenceChanged,
                    label = "Referensi pembayaran"
                )
                PaymentInputField(
                    value = uiState.notesInput,
                    onValueChange = onNotesChanged,
                    label = "Catatan",
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmittingPayment,
                colors = ButtonDefaults.buttonColors(containerColor = DebtPrimaryDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (uiState.isSubmittingPayment) "Menyimpan..." else "Simpan Pembayaran")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = DebtMuted) } },
        containerColor = DebtSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun PaymentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String? = null,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = prefix?.let {
            { Text(it, color = DebtMuted, modifier = Modifier.padding(start = 12.dp)) }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = DebtTextFieldColors()
    )
}

@Composable
private fun PaymentSummary(payable: SupplierPayable) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DebtSoft)
            .padding(16.dp)
    ) {
        Text(payable.supplierName, color = DebtText, fontWeight = FontWeight.Bold)
        Text("Sisa utang ${payable.remainingAmount.currencyText()}", color = DebtMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PaymentMethodCards(
    selected: SupplierPaymentMethod,
    onSelect: (SupplierPaymentMethod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SupplierPaymentMethod.entries.forEach { method ->
            val isSelected = method == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) DebtPrimary.copy(alpha = 0.12f) else DebtSoft)
                    .clickable { onSelect(method) }
                    .padding(12.dp)
            ) {
                Text(method.label, color = if (isSelected) DebtPrimaryDark else DebtText, fontWeight = FontWeight.Bold)
                Text(
                    method.description,
                    color = DebtMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
