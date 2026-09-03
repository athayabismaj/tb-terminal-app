package com.tbterminal.app.ui.cash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashTransaction

@Composable
internal fun CashTransactionsTable(
    uiState: CashReconciliationUiState,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CashSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CashLine)
    ) {
        Column {
            CashTableToolbar()
            HorizontalDivider(color = CashLine)
            CashTableHeader()
            when {
                uiState.isLoading && uiState.transactions.isEmpty() -> CashLoadingState()
                !uiState.hasActiveSession -> CashEmptyState("Belum ada sesi aktif. Buka sesi kas untuk melihat transaksi.")
                uiState.transactions.isEmpty() -> CashEmptyState("Belum ada transaksi pada sesi ini.")
                else -> CashTransactionList(uiState.transactions)
            }
            CashPagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun CashTableToolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Transaksi Sesi", color = CashText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Menampilkan transaksi pada sesi kas yang sedang aktif.", color = CashMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CashTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CashSurfaceSoft)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        CashHeaderText("WAKTU", Modifier.weight(1.2f))
        CashHeaderText("NO TRANSAKSI", Modifier.weight(2f))
        CashHeaderText("STATUS", Modifier.weight(1f))
        CashHeaderText("TOTAL", Modifier.weight(1.3f), Alignment.End)
        CashHeaderText("DIBAYAR", Modifier.weight(1.3f), Alignment.End)
    }
}

@Composable
private fun CashTransactionList(transactions: List<CashTransaction>) {
    LazyColumn(modifier = Modifier.height(320.dp)) {
        items(transactions, key = { it.id }) { transaction ->
            CashTransactionRow(transaction)
            HorizontalDivider(color = CashLine.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun CashTransactionRow(transaction: CashTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            transaction.createdAt.displayTime(),
            color = CashText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            transaction.id.take(8).uppercase(),
            color = CashMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(2f)
        )
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(statusColor(transaction.status).copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    transaction.status.uppercase(),
                    color = statusColor(transaction.status),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            transaction.total.money().orEmpty(),
            color = CashText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.3f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            transaction.paidAmount.money().orEmpty(),
            color = CashMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.3f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CashHeaderText(text: String, modifier: Modifier, align: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(text, color = CashMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CashLoadingState() {
    com.tbterminal.app.ui.components.SkeletonList(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        itemCount = 5,
    )
}

@Composable
private fun CashEmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Text(text, color = CashMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CashPagination(
    uiState: CashReconciliationUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CashSurfaceSoft)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} transaksi",
            color = CashMuted,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onPreviousPage,
                enabled = uiState.page > 1,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CashLine)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CashPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.page.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = CashMuted, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = onNextPage,
                enabled = uiState.page < uiState.totalPages,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CashLine)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}
