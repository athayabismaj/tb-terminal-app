package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AdjustmentReceivableDialog(
    uiState: ReceivableUiState,
    onCustomerChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDebtDateChanged: (String) -> Unit,
    onDueDateChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var customerMenuOpen by remember { mutableStateOf(false) }
    val selectedCustomer = uiState.customers.firstOrNull { it.id == uiState.adjustmentCustomerId && it.isActive }
    AlertDialog(
        onDismissRequest = { if (!uiState.isSubmittingAdjustment) onDismiss() },
        title = { Text("Adjustment Piutang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Adjustment menambah piutang non-POS dan selalu tercatat di audit log.", color = ReceivableMuted)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { customerMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCustomer?.name ?: "Pilih pelanggan aktif", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = customerMenuOpen,
                        onDismissRequest = { customerMenuOpen = false },
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        uiState.customers.filter { it.isActive }.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text(customer.name) },
                                onClick = { customerMenuOpen = false; onCustomerChanged(customer.id) }
                            )
                        }
                    }
                }
                OutlinedTextField(uiState.adjustmentAmountInput, onAmountChanged, label = { Text("Nominal") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(uiState.adjustmentDebtDateInput, onDebtDateChanged, label = { Text("Tanggal adjustment (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(uiState.adjustmentDueDateInput, onDueDateChanged, label = { Text("Jatuh tempo (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(uiState.adjustmentReferenceInput, onReferenceChanged, label = { Text("Referensi *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(uiState.adjustmentReasonInput, onReasonChanged, label = { Text("Alasan *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                uiState.errorMessage?.let { Text(it, color = ReceivableDanger) }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !uiState.isSubmittingAdjustment) {
                if (uiState.isSubmittingAdjustment) CircularProgressIndicator() else Text("Simpan Adjustment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isSubmittingAdjustment) { Text("Batal") }
        }
    )
}
