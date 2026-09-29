package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProductFormContent(
    modifier: Modifier,
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 720.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ProductBackground,
            bottomBar = {
                ProductFormBottomBar(
                    isSaving = uiState.isSaving,
                    isLoading = uiState.isLoading,
                    compact = compact,
                    onCancel = onCancel,
                    onSave = onSave,
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(ProductBackground)
                    .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 16.dp else 24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 1180.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ProductFormBody(
                        uiState = uiState,
                        onInputChanged = onInputChanged,
                        compact = compact,
                    )
                }
            }
        }
    }
}
