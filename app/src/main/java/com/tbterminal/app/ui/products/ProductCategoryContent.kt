package com.tbterminal.app.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.components.TopFloatingCapsule

@Composable
internal fun ProductCategoryContent(
    modifier: Modifier,
    uiState: ProductCategoryUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductCategory) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onRetry: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(CategorySurfaceBg)) {
        val compact = maxWidth < 720.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 12.dp else 20.dp,
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1280.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                if (uiState.isFormVisible) {
                    ProductCategoryFormCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .align(Alignment.CenterHorizontally),
                        uiState = uiState,
                        onNameChanged = onNameChanged,
                        onSave = onSave,
                        onCancelEdit = onCancelEdit,
                    )
                } else {
                    ProductCategoryListCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        compact = compact,
                        onSearchChanged = onSearchChanged,
                        onAdd = onAdd,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onRetry = onRetry,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage,
                    )
                }
            }
        }

        val isErrorMessage = uiState.message?.let {
            it.contains("gagal", ignoreCase = true) ||
            it.contains("tidak", ignoreCase = true) ||
            it.contains("kosong", ignoreCase = true)
        } == true

        TopFloatingCapsule(
            message = if (!isErrorMessage) uiState.message else null,
            error = if (isErrorMessage) uiState.message else null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }
}
