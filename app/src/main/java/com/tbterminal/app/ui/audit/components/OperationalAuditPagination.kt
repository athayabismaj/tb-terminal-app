package com.tbterminal.app.ui.audit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuditPagination(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuditPageButton(
            enabled = currentPage > 1,
            onClick = onPrevious
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Halaman sebelumnya", modifier = Modifier.size(18.dp))
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF059669),
            modifier = Modifier.padding(horizontal = 8.dp).size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    currentPage.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = "/ $totalPages",
            color = Color(0xFF64748B),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp)
        )
        AuditPageButton(
            enabled = currentPage < totalPages,
            onClick = onNext
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Halaman berikutnya", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AuditPageButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        contentColor = if (enabled) Color(0xFF64748B) else Color(0xFFCBD5E1),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
