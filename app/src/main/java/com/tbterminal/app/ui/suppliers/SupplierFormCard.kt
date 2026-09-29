package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
        modifier = modifier.testTag("supplier-form"),
        colors = CardDefaults.cardColors(containerColor = SupplierSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SupplierSlate200.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SupplierField("Nama supplier", uiState.nameInput, onNameChanged, "Contoh: PT Sumber Bangunan")
            Spacer(modifier = Modifier.height(16.dp))
            SupplierField("Nomor telepon", uiState.phoneInput, onPhoneChanged, "Contoh: 081234567890")
            Spacer(modifier = Modifier.height(16.dp))
            SupplierField("Alamat", uiState.addressInput, onAddressChanged, "Alamat supplier")
            Spacer(modifier = Modifier.height(16.dp))
            SupplierField("Termin pembayaran (hari)", uiState.paymentTermInput, onPaymentTermChanged, "30")
            SupplierFormActions(uiState, onSave, onCancelEdit)
        }
    }
}

@Composable
private fun SupplierField(label: String, value: String, onValueChanged: (String) -> Unit, placeholder: String) {
    Text(label, color = SupplierSlate500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        placeholder = { Text(placeholder, color = SupplierSlate400) },
        modifier = Modifier.fillMaxWidth(),
        colors = supplierTextFieldColors(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun SupplierFormActions(uiState: SupplierUiState, onSave: () -> Unit, onCancelEdit: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onSave,
        enabled = !uiState.isSaving,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SupplierEmerald600),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (uiState.isSaving) "Menyimpan..." else if (uiState.editingSupplier == null) "Simpan supplier" else "Simpan perubahan", fontWeight = FontWeight.SemiBold)
    }
    TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth().testTag("supplier-form-cancel")) {
        Text(if (uiState.editingSupplier == null) "Batal" else "Batal edit")
    }
}
