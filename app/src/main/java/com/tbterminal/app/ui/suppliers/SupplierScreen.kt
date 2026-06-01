package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

private val SupplierBackground = Color(0xFFF4FAFD)
private val SupplierBorder = Color(0xFFE2E8F0)
private val SupplierMuted = Color(0xFF64748B)
private val SupplierText = Color(0xFF0F172A)
private val SupplierPrimary = Color(0xFF059669)
private val SupplierPrimaryLight = Color(0xFFECFDF5)

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
        modifier = modifier.fillMaxSize().background(SupplierBackground).padding(32.dp)
    ) {
        val desktop = maxWidth > 900.dp
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SupplierHeader()
            uiState.message?.let { SupplierMessage(it, onDismissMessage) }
            if (desktop) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    SupplierFormCard(Modifier.weight(4f), uiState, onNameChanged, onPhoneChanged, onAddressChanged, onPaymentTermChanged, onSave, onCancelEdit)
                    SupplierListCard(Modifier.weight(7f), uiState, onSearchChanged, onRefresh, onEdit, { deleteTarget = it }, onPreviousPage, onNextPage)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SupplierFormCard(Modifier.fillMaxWidth(), uiState, onNameChanged, onPhoneChanged, onAddressChanged, onPaymentTermChanged, onSave, onCancelEdit)
                    SupplierListCard(Modifier.fillMaxWidth(), uiState, onSearchChanged, onRefresh, onEdit, { deleteTarget = it }, onPreviousPage, onNextPage)
                }
            }
        }
    }
    deleteTarget?.let { supplier ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Nonaktifkan supplier?") },
            text = { Text("${supplier.name} tidak dapat dipakai pada restok baru setelah dinonaktifkan.") },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; onDelete(supplier) }) { Text("Nonaktifkan") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SupplierHeader() {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text("Supplier", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = SupplierText)
        Text("Kelola supplier aktif untuk restok, nota pembelian, dan hutang supplier.", color = SupplierMuted, fontSize = 14.sp)
    }
}

@Composable
private fun SupplierMessage(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).background(SupplierPrimaryLight, RoundedCornerShape(8.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = SupplierPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup") }
    }
}

@Composable
private fun SupplierFormCard(
    modifier: Modifier,
    uiState: SupplierUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPaymentTermChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, SupplierBorder)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AddBusiness, null, tint = SupplierPrimary)
                Spacer(Modifier.width(10.dp))
                Text(if (uiState.editingSupplier == null) "Tambah Supplier" else "Edit Supplier", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SupplierText)
            }
            HorizontalDivider(color = SupplierBorder)
            SupplierField("NAMA SUPPLIER", uiState.nameInput, onNameChanged, "Contoh: PT Sumber Bangunan")
            SupplierField("NO. TELEPON", uiState.phoneInput, onPhoneChanged, "Contoh: 081234567890")
            SupplierField("ALAMAT", uiState.addressInput, onAddressChanged, "Alamat supplier")
            SupplierField("TERMIN PEMBAYARAN (HARI)", uiState.paymentTermInput, onPaymentTermChanged, "30")
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupplierPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.editingSupplier == null) "Simpan Supplier" else "Simpan Perubahan")
            }
            if (uiState.editingSupplier != null) {
                OutlinedButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text("Batal Edit") }
            }
        }
    }
}

@Composable
private fun SupplierField(label: String, value: String, onValueChanged: (String) -> Unit, placeholder: String) {
    Column {
        Text(label, color = SupplierMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value, onValueChanged, modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder) }, singleLine = true)
    }
}

@Composable
private fun SupplierListCard(
    modifier: Modifier,
    uiState: SupplierUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, SupplierBorder)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Business, null, tint = SupplierPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Daftar Supplier", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SupplierText)
                }
                Text("Total ${uiState.totalSuppliers}", color = SupplierPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Cari supplier...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Muat ulang", tint = SupplierPrimary) }
            }
            SupplierTableHeader()
            when {
                uiState.isLoading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.suppliers.isEmpty() -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { Text("Belum ada supplier.", color = SupplierMuted) }
                else -> uiState.suppliers.forEach { SupplierRow(it, onEdit, onDelete) }
            }
            SupplierPagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun SupplierTableHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp)) {
        TableLabel("SUPPLIER", Modifier.weight(2.4f))
        TableLabel("KONTAK", Modifier.weight(1.5f))
        TableLabel("TERMIN", Modifier.weight(0.8f))
        TableLabel("STATUS", Modifier.weight(0.9f))
        TableLabel("AKSI", Modifier.weight(0.8f))
    }
}

@Composable
private fun SupplierRow(supplier: Supplier, onEdit: (Supplier) -> Unit, onDelete: (Supplier) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2.4f)) {
            Text(supplier.name, fontWeight = FontWeight.Bold, color = SupplierText)
            Text(supplier.address ?: "-", color = SupplierMuted, fontSize = 12.sp, maxLines = 1)
        }
        Text(supplier.phone ?: "-", Modifier.weight(1.5f), color = SupplierMuted, fontSize = 13.sp)
        Text("${supplier.paymentTermDays} hari", Modifier.weight(0.8f), color = SupplierText, fontSize = 13.sp)
        Text(if (supplier.isActive) "AKTIF" else "NONAKTIF", Modifier.weight(0.9f), color = if (supplier.isActive) SupplierPrimary else SupplierMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.weight(0.8f)) {
            IconButton(onClick = { onEdit(supplier) }) { Icon(Icons.Outlined.Edit, "Edit", tint = SupplierMuted) }
            if (supplier.isActive) IconButton(onClick = { onDelete(supplier) }) { Icon(Icons.Outlined.DeleteOutline, "Nonaktifkan", tint = Color(0xFFDC2626)) }
        }
    }
    HorizontalDivider(color = SupplierBorder)
}

@Composable
private fun SupplierPagination(uiState: SupplierUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Halaman ${uiState.page} dari ${uiState.totalPages}", color = SupplierMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Text("Sebelumnya") }
            OutlinedButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Text("Berikutnya") }
        }
    }
}

@Composable
private fun TableLabel(text: String, modifier: Modifier) {
    Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
}
