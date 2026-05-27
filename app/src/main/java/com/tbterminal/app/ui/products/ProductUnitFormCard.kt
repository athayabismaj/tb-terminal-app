package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AddCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductUnitFormCard(
    modifier: Modifier,
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = UnitWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, UnitSlate200)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            ProductUnitCardTitle(
                icon = Icons.Outlined.AddCircle,
                title = if (uiState.editingUnit == null) "Tambah Satuan" else "Edit Satuan"
            )
            if (uiState.message != null) {
                ProductUnitMessage(message = uiState.message)
            }
            ProductUnitTextField(
                label = "NAMA SATUAN",
                value = uiState.nameInput,
                placeholder = "Contoh: Pcs, Dus, Meter",
                onValueChange = onNameChanged
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProductUnitTextField(
                label = "SIMBOL",
                value = uiState.symbolInput,
                placeholder = "Contoh: pcs, dus, m",
                onValueChange = onSymbolChanged
            )
            ProductUnitFormActions(
                uiState = uiState,
                onSave = onSave,
                onCancelEdit = onCancelEdit
            )
            ProductUnitTips()
        }
    }
}

@Composable
private fun ProductUnitTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UnitSlate500)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = UnitSlate400) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = productUnitTextFieldColors(),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ProductUnitFormActions(
    uiState: ProductUnitUiState,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onSave,
        enabled = !uiState.isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = UnitEmerald600)
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (uiState.isSaving) "Menyimpan..." else "Simpan Satuan", fontWeight = FontWeight.Bold)
    }
    if (uiState.editingUnit != null) {
        TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Batal edit")
        }
    }
}

@Composable
private fun ProductUnitTips() {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UnitEmerald50.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, UnitEmerald100, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = UnitEmerald600,
            modifier = Modifier
                .size(20.dp)
                .offset(y = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Gunakan simbol standar yang mudah dikenali tim gudang dan admin untuk mengurangi salah input stok.",
            fontSize = 12.sp,
            color = UnitEmerald800,
            lineHeight = 18.sp
        )
    }
}
