package com.tbterminal.app.ui.payables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SupplierDebtFooter(
    uiState: SupplierDebtUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DebtSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (compact) "${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total}"
            else "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} utang",
            color = DebtMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PageIconButton(Icons.Default.ChevronLeft, enabled = uiState.page > 1, onClick = onPreviousPage)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DebtPrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.page.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = DebtMuted, fontWeight = FontWeight.Bold)
            PageIconButton(Icons.Default.ChevronRight, enabled = uiState.page < uiState.totalPages, onClick = onNextPage)
        }
    }
}
