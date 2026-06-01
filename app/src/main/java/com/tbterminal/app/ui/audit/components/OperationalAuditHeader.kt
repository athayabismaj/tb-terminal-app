package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OperationalAuditHeader() {
    Text(
        text = "Audit Operasional",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = "Jejak perubahan produk, stok, pembelian, piutang, utang, kas, dan transaksi.",
        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
        modifier = Modifier.padding(bottom = 24.dp)
    )
}
