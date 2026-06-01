package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Receivable

@Composable
internal fun ReceivablePaymentDialog(
    receivable: Receivable,
    uiState: ReceivableUiState,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Piutang Pelanggan", color = ReceivableText, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PaymentSummary(receivable)
                PaymentInputField(
                    value = uiState.paymentAmountInput,
                    onValueChange = onAmountChanged,
                    label = "Nominal pembayaran",
                    prefix = "Rp",
                    isNumber = true
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
                colors = ButtonDefaults.buttonColors(containerColor = ReceivablePrimaryDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (uiState.isSubmittingPayment) "Menyimpan..." else "Simpan Pembayaran")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = ReceivableMuted) } },
        containerColor = ReceivableSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun PaymentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String? = null,
    minLines: Int = 1,
    isNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = prefix?.let {
            { Text(it, color = ReceivableMuted, modifier = Modifier.padding(start = 12.dp)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = ReceivableTextFieldColors()
    )
}

@Composable
private fun PaymentSummary(receivable: Receivable) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ReceivableSoft)
            .padding(16.dp)
    ) {
        Text(receivable.customerName, color = ReceivableText, fontWeight = FontWeight.Bold)
        Text("Sisa piutang ${receivable.remainingAmount.currencyText()}", color = ReceivableMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PaymentMethodCards(
    selected: ReceivablePaymentMethod,
    onSelect: (ReceivablePaymentMethod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReceivablePaymentMethod.entries.forEach { method ->
            val isSelected = method == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ReceivablePrimary.copy(alpha = 0.12f) else ReceivableSoft)
                    .clickable { onSelect(method) }
                    .padding(12.dp)
            ) {
                Text(method.label, color = if (isSelected) ReceivablePrimaryDark else ReceivableText, fontWeight = FontWeight.Bold)
                Text(
                    method.description,
                    color = ReceivableMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
