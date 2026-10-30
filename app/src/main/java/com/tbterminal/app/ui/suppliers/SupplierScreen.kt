package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.components.TopFloatingCapsule

@Composable
internal fun SupplierScreen(
    modifier: Modifier,
    uiState: SupplierUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierStatusFilter) -> Unit = {},
    onAdd: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSummarySheet by rememberSaveable { mutableStateOf(false) }
    val supplierToDelete = uiState.suppliers.firstOrNull { it.id == deleteTargetId }

    BoxWithConstraints(modifier.fillMaxSize().background(SupplierBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1120.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
        ) {
            SupplierListCard(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                compact = compact,
                onSearchChanged = onSearchChanged,
                onStatusFilterChanged = onStatusFilterChanged,
                onAdd = onAdd,
                onEdit = onEdit,
                onDelete = { deleteTargetId = it.id },
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                onOpenSummary = { showSummarySheet = true },
            )
        }

        val isErrorMessage = uiState.message?.let {
            it.contains("gagal", ignoreCase = true) ||
            it.contains("tidak", ignoreCase = true) ||
            it.contains("kosong", ignoreCase = true)
        } == true

        TopFloatingCapsule(
            message = if (!isErrorMessage) uiState.message else null,
            error = if (isErrorMessage) uiState.message else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }

    if (showSummarySheet) {
        SupplierSummarySheet(
            uiState = uiState,
            onDismiss = { showSummarySheet = false },
        )
    }

    supplierToDelete?.let { supplier ->
        SupplierDeactivateDialog(
            supplier = supplier,
            onDismiss = { deleteTargetId = null },
            onConfirm = {
                deleteTargetId = null
                onDelete(supplier)
            },
        )
    }
}

@Composable
internal fun SupplierFormScreen(
    modifier: Modifier,
    uiState: SupplierUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPaymentTermChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(SupplierBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SupplierFormCard(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                onNameChanged = onNameChanged,
                onPhoneChanged = onPhoneChanged,
                onAddressChanged = onAddressChanged,
                onPaymentTermChanged = onPaymentTermChanged,
                onSave = onSave,
                onCancelEdit = onCancel,
            )
        }

        val isErrorMessage = uiState.message?.let {
            it.contains("gagal", ignoreCase = true) ||
            it.contains("tidak", ignoreCase = true) ||
            it.contains("kosong", ignoreCase = true)
        } == true

        TopFloatingCapsule(
            message = if (!isErrorMessage) uiState.message else null,
            error = if (isErrorMessage) uiState.message else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }
}
