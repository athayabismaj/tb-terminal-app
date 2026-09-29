package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductCategoryFormCard(
    modifier: Modifier,
    uiState: ProductCategoryUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Surface(
        modifier = modifier.testTag("category-form"),
        color = CategoryWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CategorySlate200.copy(alpha = 0.85f)),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = CategoryEmerald50, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.Category, null, Modifier.padding(10.dp), tint = CategoryEmerald700)
                }
                Column {
                    Text(if (uiState.editingCategory == null) "Kategori baru" else "Ubah kategori", color = CategorySlate900, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Nama yang singkat memudahkan pencarian produk.", color = CategorySlate500, fontSize = 12.sp)
                }
            }
            uiState.message?.let { ProductCategoryMessage(it) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nama kategori", color = CategorySlate500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth().testTag("category-name"),
                    placeholder = { Text("Contoh: Cat dan pelapis") },
                    singleLine = true,
                    colors = productCategoryTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onCancelEdit, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Batal") }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.height(52.dp).testTag("category-save"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CategoryEmerald600),
                ) {
                    Icon(Icons.Outlined.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isSaving) "Menyimpan…" else "Simpan", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
