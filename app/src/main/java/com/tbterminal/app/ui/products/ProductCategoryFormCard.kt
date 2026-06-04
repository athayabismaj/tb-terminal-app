package com.tbterminal.app.ui.products

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductCategoryFormCard(
    modifier: Modifier,
    uiState: ProductCategoryUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CategoryWhite),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CategorySlate200)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            ProductCategoryCardTitle(
                icon = Icons.Outlined.AddCircle,
                title = if (uiState.editingCategory == null) "Tambah Kategori" else "Edit Kategori"
            )
            if (uiState.message != null) ProductCategoryMessage(uiState.message)
            ProductCategoryTextField(
                value = uiState.nameInput,
                onValueChange = onNameChanged
            )
            CategoryFormActions(uiState, onSave, onCancelEdit)
            CategoryTips()
        }
    }
}

@Composable
private fun ProductCategoryTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Text("NAMA KATEGORI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CategorySlate500)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Contoh: Semen, Cat, Pipa", color = CategorySlate400) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = productCategoryTextFieldColors(),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun CategoryFormActions(
    uiState: ProductCategoryUiState,
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
        colors = ButtonDefaults.buttonColors(containerColor = CategoryEmerald600)
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (uiState.isSaving) "Menyimpan..." else "Simpan Kategori", fontWeight = FontWeight.Bold)
    }
    if (uiState.editingCategory != null) {
        TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Batal edit")
        }
    }
}

@Composable
private fun CategoryTips() {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CategoryEmerald50.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, CategoryEmerald100, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = CategoryEmerald600,
            modifier = Modifier
                .size(20.dp)
                .offset(y = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Gunakan kategori yang mudah dipahami agar kasir cepat menemukan produk saat transaksi.",
            fontSize = 12.sp,
            color = CategoryEmerald800,
            lineHeight = 18.sp
        )
    }
}
