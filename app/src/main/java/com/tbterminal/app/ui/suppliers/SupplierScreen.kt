package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun SupplierScreen(
    modifier: Modifier,
    uiState: SupplierUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPaymentTermChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (Supplier) -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<Supplier?>(null) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SupplierSurface)
            .padding(40.dp)
    ) {
        val isDesktop = maxWidth > 900.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text("Supplier", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = SupplierSlate900)
            uiState.message?.let { SupplierMessage(it, onDismissMessage) }
            if (isDesktop) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    SupplierPanels(
                        formModifier = Modifier.weight(5f),
                        listModifier = Modifier.weight(7f),
                        uiState = uiState,
                        onNameChanged = onNameChanged,
                        onPhoneChanged = onPhoneChanged,
                        onAddressChanged = onAddressChanged,
                        onPaymentTermChanged = onPaymentTermChanged,
                        onSearchChanged = onSearchChanged,
                        onSave = onSave,
                        onEdit = onEdit,
                        onCancelEdit = onCancelEdit,
                        onDelete = { deleteTarget = it },
                        onRefresh = onRefresh,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    SupplierPanels(
                        formModifier = Modifier.fillMaxWidth(),
                        listModifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onNameChanged = onNameChanged,
                        onPhoneChanged = onPhoneChanged,
                        onAddressChanged = onAddressChanged,
                        onPaymentTermChanged = onPaymentTermChanged,
                        onSearchChanged = onSearchChanged,
                        onSave = onSave,
                        onEdit = onEdit,
                        onCancelEdit = onCancelEdit,
                        onDelete = { deleteTarget = it },
                        onRefresh = onRefresh,
                        onPreviousPage = onPreviousPage,
                        onNextPage = onNextPage
                    )
                }
            }
        }
    }
    deleteTarget?.let { supplier ->
        SupplierDeactivateDialog(
            supplier = supplier,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDelete(supplier)
            }
        )
    }
}

@Composable
private fun SupplierPanels(
    formModifier: Modifier,
    listModifier: Modifier,
    uiState: SupplierUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPaymentTermChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (Supplier) -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    SupplierFormCard(
        modifier = formModifier,
        uiState = uiState,
        onNameChanged = onNameChanged,
        onPhoneChanged = onPhoneChanged,
        onAddressChanged = onAddressChanged,
        onPaymentTermChanged = onPaymentTermChanged,
        onSave = onSave,
        onCancelEdit = onCancelEdit
    )
    SupplierListCard(
        modifier = listModifier,
        uiState = uiState,
        onSearchChanged = onSearchChanged,
        onRefresh = onRefresh,
        onEdit = onEdit,
        onDelete = onDelete,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage
    )
}
