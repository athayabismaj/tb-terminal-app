package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SubmitButton(uiState: StockOpnameUiState, onSubmit: () -> Unit) {
    val isReady = uiState.selectedProduct != null && uiState.actualQty != null && !uiState.isSubmitting
    val label = when {
        uiState.selectedProduct == null -> "Pilih produk terlebih dahulu"
        uiState.actualQty == null -> "Isi stok fisik dulu"
        else -> "Simpan Stok Opname"
    }

    Button(
        onClick = onSubmit,
        enabled = isReady,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OpnamePrimary),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        if (uiState.isSubmitting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}
