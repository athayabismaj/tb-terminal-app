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
internal fun OpeningReceivableDialog(
    uiState: ReceivableUiState,
    onCustomerChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDebtDateChanged: (String) -> Unit,
    onDueDateChanged: (String) -> Unit,
    onLegacyInvoiceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var customerMenuOpen by remember { mutableStateOf(false) }
    val selectedCustomer = uiState.customers.firstOrNull { it.id == uiState.openingCustomerId }
    AlertDialog(
        onDismissRequest = { if (!uiState.isSubmittingOpeningBalance) onDismiss() },
        title = { Text("Saldo Awal Piutang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Catat piutang lama tanpa membuat transaksi POS palsu.", color = ReceivableMuted)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { customerMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedCustomer?.name ?: "Pilih pelanggan aktif", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = customerMenuOpen,
                        onDismissRequest = { customerMenuOpen = false },
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        uiState.customers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text(customer.name) },
                                onClick = { customerMenuOpen = false; onCustomerChanged(customer.id) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.openingAmountInput,
                    onValueChange = onAmountChanged,
                    label = { Text("Nominal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.openingDebtDateInput,
                    onValueChange = onDebtDateChanged,
                    label = { Text("Tanggal piutang (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.openingDueDateInput,
                    onValueChange = onDueDateChanged,
                    label = { Text("Jatuh tempo (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.openingLegacyInvoiceInput,
                    onValueChange = onLegacyInvoiceChanged,
                    label = { Text("Nomor nota lama") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.openingNotesInput,
                    onValueChange = onNotesChanged,
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                uiState.errorMessage?.let { Text(it, color = ReceivableDanger) }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !uiState.isSubmittingOpeningBalance) {
                if (uiState.isSubmittingOpeningBalance) CircularProgressIndicator()
                else Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isSubmittingOpeningBalance) { Text("Batal") }
        }
    )
}
