package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun SupplierTableHeader() {
    Row(Modifier.fillMaxWidth().background(SupplierSlate50).padding(horizontal = 24.dp, vertical = 12.dp)) {
        SupplierTableLabel("SUPPLIER", Modifier.weight(2.4f))
        SupplierTableLabel("KONTAK", Modifier.weight(1.5f))
        SupplierTableLabel("TERMIN", Modifier.weight(0.8f))
        SupplierTableLabel("STATUS", Modifier.weight(0.9f))
        SupplierTableLabel("AKSI", Modifier.weight(0.9f), Alignment.CenterHorizontally)
    }
}

@Composable
internal fun SupplierTableBody(uiState: SupplierUiState, onEdit: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    when {
        uiState.isLoading && uiState.suppliers.isEmpty() -> SupplierLoadingState()
        uiState.suppliers.isEmpty() -> SupplierEmptyState()
        else -> Column(modifier = Modifier.fillMaxWidth()) {
            uiState.suppliers.forEach { SupplierRow(it, onEdit, onDelete) }
        }
    }
}

@Composable
private fun SupplierRow(supplier: Supplier, onEdit: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    Row(Modifier.fillMaxWidth().border(BorderStroke(1.dp, SupplierSlate50)).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(2.4f)) {
            Text(supplier.name, color = SupplierSlate900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(supplier.address ?: "-", color = SupplierSlate500, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(supplier.phone ?: "-", modifier = Modifier.weight(1.5f), color = SupplierSlate500, fontSize = 13.sp)
        Text("${supplier.paymentTermDays} hari", modifier = Modifier.weight(0.8f), color = SupplierSlate900, fontSize = 13.sp)
        Box(modifier = Modifier.weight(0.9f)) {
            Box(Modifier.background(if (supplier.isActive) SupplierEmerald50 else SupplierSlate100, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(if (supplier.isActive) "AKTIF" else "NONAKTIF", color = if (supplier.isActive) SupplierEmerald700 else SupplierSlate500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(modifier = Modifier.weight(0.9f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            IconButton(onClick = { onEdit(supplier) }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = SupplierSlate400) }
            if (supplier.isActive) {
                IconButton(onClick = { onDelete(supplier) }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Nonaktifkan", tint = SupplierDanger) }
            }
        }
    }
}
