package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.components.AppConfirmationSpec
import com.tbterminal.app.ui.components.AppConfirmDialog

@Composable
internal fun SupplierMessage(message: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(SupplierEmerald50, RoundedCornerShape(8.dp)).border(1.dp, SupplierEmerald100, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(message, color = SupplierEmerald700, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup") }
    }
}

@Composable
internal fun SupplierLoadingState() {
    com.tbterminal.app.ui.components.SkeletonList(
        modifier = Modifier.fillMaxWidth().height(SupplierListHeight),
        itemCount = 6,
    )
}

@Composable
internal fun SupplierEmptyState() {
    Box(Modifier.fillMaxWidth().height(SupplierListHeight), contentAlignment = Alignment.Center) {
        Text("Belum ada supplier yang cocok.", color = SupplierSlate500, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SupplierDeactivateDialog(supplier: Supplier, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppConfirmDialog(
        spec = AppConfirmationSpec(
            title = "Nonaktifkan supplier",
            target = supplier.name,
            consequence = "Supplier tidak dapat dipakai pada restok baru setelah dinonaktifkan.",
            confirmLabel = "Nonaktifkan",
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun SupplierTableLabel(text: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = SupplierSlate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
