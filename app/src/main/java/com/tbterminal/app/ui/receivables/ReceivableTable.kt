package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Receivable

@Composable
internal fun ReceivableTableCard(
    modifier: Modifier,
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit,
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ReceivableToolbar(uiState, onSearchChanged, onStatusFilterChanged, onDueFilterChanged)
        Spacer(modifier = Modifier.height(28.dp))
        ReceivableTableHeader()
        ReceivableRows(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            onPayClick = onPayClick
        )
        ReceivableFooter(uiState, onPreviousPage, onNextPage)
    }
}

@Composable
private fun ReceivableToolbar(
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onDueFilterChanged: (ReceivableDueFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari pelanggan atau ID transaksi...", color = ReceivableMuted) },
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Cari piutang", tint = ReceivableMuted) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = ReceivableToolbarTextFieldColors()
        )
        ReceivableStatusFilterButton(uiState.statusFilter, onStatusFilterChanged)
        ReceivableDueFilterButton(uiState.dueFilter, onDueFilterChanged)
    }
}

@Composable
private fun ReceivableDueFilterButton(
    selected: ReceivableDueFilter,
    onSelect: (ReceivableDueFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(220.dp).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, ReceivableLine)
        ) {
            Text(selected.label, modifier = Modifier.weight(1f), color = ReceivableText)
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = ReceivableMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReceivableDueFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = { expanded = false; onSelect(filter) }
                )
            }
        }
    }
}

@Composable
private fun ReceivableStatusFilterButton(
    selected: ReceivableStatusFilter,
    onSelect: (ReceivableStatusFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(220.dp).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, ReceivableLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Text(selected.label, modifier = Modifier.weight(1f), color = ReceivableText, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = ReceivableMuted)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp).heightIn(max = 280.dp).background(ReceivableSurface)
        ) {
            ReceivableStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        expanded = false
                        onSelect(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReceivableTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReceivableSoft)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReceivableHeaderText("PELANGGAN", Modifier.weight(2.2f))
        ReceivableHeaderText("TRANSAKSI", Modifier.weight(1.45f))
        ReceivableHeaderText("TOTAL", Modifier.weight(1.2f), Alignment.End)
        ReceivableHeaderText("DIBAYAR", Modifier.weight(1.2f), Alignment.End)
        ReceivableHeaderText("SISA", Modifier.weight(1.2f), Alignment.End)
        ReceivableHeaderText("JATUH TEMPO", Modifier.weight(1.35f))
        ReceivableHeaderText("STATUS", Modifier.weight(1.2f), Alignment.CenterHorizontally)
        ReceivableHeaderText("AKSI", Modifier.weight(1f), Alignment.End)
    }
}

@Composable
private fun ReceivableRows(
    modifier: Modifier,
    uiState: ReceivableUiState,
    onPayClick: (Receivable) -> Unit
) {
    when {
        uiState.isLoading -> ReceivableCenteredContent(modifier) {
            CircularProgressIndicator(color = ReceivablePrimaryDark)
        }

        uiState.errorMessage != null && uiState.receivables.isEmpty() -> ReceivableCenteredContent(modifier) {
            Text("Piutang pelanggan gagal dimuat.", color = ReceivableDanger, fontWeight = FontWeight.Bold)
        }

        uiState.filteredReceivables.isEmpty() -> ReceivableCenteredContent(modifier) {
            Text("Belum ada piutang pelanggan yang cocok.", color = ReceivableMuted, fontWeight = FontWeight.SemiBold)
        }

        else -> Column(modifier = modifier.fillMaxWidth()) {
            uiState.filteredReceivables.forEachIndexed { index, receivable ->
                ReceivableRow(receivable, useAlternateBackground = index % 2 != 0, onPayClick = onPayClick)
                    HorizontalDivider(color = ReceivableLine.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun ReceivableCenteredContent(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ReceivableRow(
    receivable: Receivable,
    useAlternateBackground: Boolean,
    onPayClick: (Receivable) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (useAlternateBackground) ReceivableSoft.copy(alpha = 0.76f) else ReceivableSurface)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically) {
            CustomerInitial(receivable.customerName)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(receivable.customerName, color = ReceivableText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(receivable.createdAt.simpleDate(), color = ReceivableMuted, fontSize = 11.sp)
            }
        }
        Text(
            receivable.transactionId?.shortTransactionId()
                ?: receivable.legacyInvoiceNumber?.takeIf(String::isNotBlank)
                ?: receivable.source,
            modifier = Modifier.weight(1.45f),
            color = ReceivableMuted,
            fontSize = 12.sp
        )
        ReceivableAmountText(receivable.amount, Modifier.weight(1.2f))
        ReceivableAmountText(receivable.paidAmount, Modifier.weight(1.2f))
        ReceivableAmountText(receivable.remainingAmount, Modifier.weight(1.2f), strong = true)
        Column(modifier = Modifier.weight(1.35f)) {
            Text(receivable.dueDate.simpleDate(), color = ReceivableText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(receivable.dueDate.dueRelativeText(), color = receivable.dueDate.dueColor(), fontSize = 11.sp)
        }
        Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
            ReceivableStatusBadge(receivable.status)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (receivable.status == ReceivableStatusFilter.Paid.apiValue) {
                Text("Lunas", color = ReceivableMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = { onPayClick(receivable) },
                    colors = ButtonDefaults.buttonColors(containerColor = ReceivablePrimaryDark),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Bayar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerInitial(name: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(ReceivablePrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(name.initial(), color = ReceivablePrimaryDark, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ReceivableFooter(
    uiState: ReceivableUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReceivableSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} piutang",
                color = ReceivableText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Maksimal $RECEIVABLE_PAGE_SIZE piutang per halaman",
                color = ReceivableMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceivablePageIconButton(Icons.Default.ChevronLeft, enabled = uiState.page > 1, onClick = onPreviousPage)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ReceivablePrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.page.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = ReceivableMuted, fontWeight = FontWeight.Bold)
            ReceivablePageIconButton(Icons.Default.ChevronRight, enabled = uiState.page < uiState.totalPages, onClick = onNextPage)
        }
    }
}
