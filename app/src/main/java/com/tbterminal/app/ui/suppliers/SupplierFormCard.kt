package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SupplierFormCard(
    modifier: Modifier,
    uiState: SupplierUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPaymentTermChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SupplierSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SupplierSlate200)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            SupplierCardTitle(Icons.Outlined.AddBusiness, if (uiState.editingSupplier == null) "Tambah Supplier" else "Edit Supplier")
            SupplierField("NAMA SUPPLIER", uiState.nameInput, onNameChanged, "Contoh: PT Sumber Bangunan")
            Spacer(modifier = Modifier.height(16.dp))
            SupplierField("NO. TELEPON", uiState.phoneInput, onPhoneChanged, "Contoh: 081234567890")
            Spacer(modifier = Modifier.height(16.dp))
            SupplierField("ALAMAT", uiState.addressInput, onAddressChanged, "Alamat supplier")
            Spacer(modifier = Modifier.height(16.dp))
            SupplierField("TERMIN PEMBAYARAN (HARI)", uiState.paymentTermInput, onPaymentTermChanged, "30")
            SupplierFormActions(uiState, onSave, onCancelEdit)
            SupplierTips()
        }
    }
}

@Composable
private fun SupplierField(label: String, value: String, onValueChanged: (String) -> Unit, placeholder: String) {
    Text(label, color = SupplierSlate500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        placeholder = { Text(placeholder, color = SupplierSlate400) },
        modifier = Modifier.fillMaxWidth(),
        colors = supplierTextFieldColors(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}

@Composable
private fun SupplierFormActions(uiState: SupplierUiState, onSave: () -> Unit, onCancelEdit: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onSave,
        enabled = !uiState.isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SupplierEmerald600),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (uiState.isSaving) "Menyimpan..." else if (uiState.editingSupplier == null) "Simpan Supplier" else "Simpan Perubahan", fontWeight = FontWeight.Bold)
    }
    if (uiState.editingSupplier != null) {
        TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text("Batal edit") }
    }
}

@Composable
private fun SupplierTips() {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SupplierEmerald50.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, SupplierEmerald100, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = SupplierEmerald600, modifier = Modifier.size(20.dp).offset(y = 2.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Isi termin pembayaran agar jatuh tempo utang supplier tercatat konsisten pada nota pembelian.", color = SupplierEmerald800, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
