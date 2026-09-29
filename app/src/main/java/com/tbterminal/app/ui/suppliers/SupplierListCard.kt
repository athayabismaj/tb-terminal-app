package com.tbterminal.app.ui.suppliers

import com.tbterminal.app.ui.components.TbPagination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun SupplierListCard(
    modifier: Modifier,
    uiState: SupplierUiState,
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SupplierToolbar(uiState.searchQuery, onSearchChanged, onAdd)
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        when {
            uiState.isLoading && uiState.suppliers.isEmpty() -> SupplierLoadingState()
            uiState.suppliers.isEmpty() -> SupplierEmptyState()
            compact -> SupplierMobileList(uiState.suppliers, onEdit, onDelete)
            else -> SupplierTable(uiState.suppliers, onEdit, onDelete)
        }
        Spacer(Modifier.height(6.dp))
        SupplierPagination(uiState, compact, onPreviousPage, onNextPage)
    }
}

@Composable
private fun SupplierToolbar(searchQuery: String, onSearchChanged: (String) -> Unit, onAdd: () -> Unit) {
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        androidx.compose.material3.Surface(
            modifier = fieldModifier.height(52.dp).testTag("supplier-search"),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Icon(Icons.Outlined.Search, "Cari supplier", tint = SupplierSlate500, modifier = Modifier.size(20.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    singleLine = true,
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = SupplierSlate900),
                    decorationBox = @Composable { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isBlank()) {
                                Text("Cari supplier", color = SupplierSlate500, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().testTag("supplier-toolbar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        search(Modifier.weight(1f))
        SupplierAddButton(onAdd)
    }
}

@Composable
private fun SupplierAddButton(onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.size(48.dp).testTag("supplier-add").clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        contentColor = com.tbterminal.app.ui.theme.TbGreenDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = "Tambah supplier", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SupplierPagination(
    uiState: SupplierUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val start = if (uiState.totalSuppliers == 0L) 0 else (uiState.page - 1L) * uiState.pageSize + 1
    val end = minOf(uiState.page.toLong() * uiState.pageSize, uiState.totalSuppliers)
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = "$start-$end dari ${uiState.totalSuppliers} supplier",
        isLoading = uiState.isLoading,
        testTag = "supplier-pagination",
    )
}
