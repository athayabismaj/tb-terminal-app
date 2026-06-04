package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductFormContent(
    modifier: Modifier,
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ProductBackground,
        bottomBar = {
            ProductFormBottomBar(
                isSaving = uiState.isSaving,
                isLoading = uiState.isLoading,
                onCancel = onCancel,
                onSave = onSave
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ProductBackground)
                .padding(start = 40.dp, top = 32.dp, end = 40.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ProductFormHeader(isEditMode = uiState.isEditMode)
            ProductFormBody(uiState = uiState, onInputChanged = onInputChanged)
        }
    }
}

@Composable
private fun ProductFormHeader(isEditMode: Boolean) {
    Column {
        Text(
            text = if (isEditMode) "Edit Produk" else "Tambah Produk",
            color = ProductText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isEditMode) {
                "Perbarui informasi produk tanpa mengubah riwayat transaksi."
            } else {
                "Lengkapi informasi produk untuk menyimpan produk ke katalog."
            },
            color = ProductMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
