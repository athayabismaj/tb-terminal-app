package com.tbterminal.app.ui.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun SupplierCardTitle(icon: ImageVector, title: String, bottomPadding: Dp = 24.dp) {
    Row(modifier = Modifier.padding(bottom = bottomPadding), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).background(SupplierEmerald50, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = SupplierEmerald600, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = SupplierSlate900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SupplierMessage(message: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(SupplierEmerald50, RoundedCornerShape(8.dp)).border(1.dp, SupplierEmerald100, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(message, color = SupplierEmerald700, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup") }
    }
}

@Composable
internal fun SupplierLoadingState() {
    Box(Modifier.fillMaxWidth().height(SupplierListHeight), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SupplierEmerald600)
    }
}

@Composable
internal fun SupplierEmptyState() {
    Box(Modifier.fillMaxWidth().height(SupplierListHeight), contentAlignment = Alignment.Center) {
        Text("Belum ada supplier yang cocok.", color = SupplierSlate500, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SupplierDeactivateDialog(supplier: Supplier, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nonaktifkan supplier?") },
        text = { Text("${supplier.name} tidak dapat dipakai pada restok baru setelah dinonaktifkan.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Nonaktifkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
internal fun SupplierTableLabel(text: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, color = SupplierSlate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
