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
                .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
            text = if (isEditMode) "Detail Produk" else "Detail Produk Baru",
            color = ProductText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Lengkapi informasi di bawah ini untuk menyimpan produk ke katalog.",
            color = ProductMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
