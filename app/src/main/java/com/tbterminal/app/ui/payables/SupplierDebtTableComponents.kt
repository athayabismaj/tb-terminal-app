package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.SupplierPayable

@Composable
internal fun SupplierDebtTableCard(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onPayClick: (SupplierPayable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DebtSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DebtLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SupplierDebtToolbar(uiState, onSearchChanged, onStatusFilterChanged, onRefresh)
            DebtTableHeader()
            SupplierDebtRows(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                onPayClick = onPayClick
            )
            SupplierDebtFooter(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun SupplierDebtToolbar(
    uiState: SupplierDebtUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (SupplierDebtStatusFilter) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari supplier atau ID pembelian...", color = DebtMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = DebtMuted) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = DebtTextFieldColors()
        )
        StatusFilterButton(uiState.statusFilter, onStatusFilterChanged)
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DebtLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = DebtPrimaryDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat Ulang", color = DebtAccentText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusFilterButton(
    selected: SupplierDebtStatusFilter,
    onSelect: (SupplierDebtStatusFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DebtLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Text(selected.label, color = DebtText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(18.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = DebtMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SupplierDebtStatusFilter.entries.forEach { filter ->
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
private fun DebtTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DebtSoft)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebtHeaderText("SUPPLIER", Modifier.weight(2.2f))
        DebtHeaderText("PEMBELIAN", Modifier.weight(1.4f))
        DebtHeaderText("TOTAL", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("DIBAYAR", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("SISA", Modifier.weight(1.25f), Alignment.End)
        DebtHeaderText("JATUH TEMPO", Modifier.weight(1.35f))
        DebtHeaderText("STATUS", Modifier.weight(1.2f), Alignment.CenterHorizontally)
        DebtHeaderText("AKSI", Modifier.weight(1f), Alignment.End)
    }
}

@Composable
private fun SupplierDebtRows(
    modifier: Modifier,
    uiState: SupplierDebtUiState,
    onPayClick: (SupplierPayable) -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = DebtPrimaryDark
            )

            uiState.errorMessage != null && uiState.payables.isEmpty() -> Text(
                "Utang supplier gagal dimuat.",
                modifier = Modifier.align(Alignment.Center),
                color = DebtDanger,
                fontWeight = FontWeight.Bold
            )

            uiState.filteredPayables.isEmpty() -> Text(
                "Belum ada utang supplier yang cocok.",
                modifier = Modifier.align(Alignment.Center),
                color = DebtMuted,
                fontWeight = FontWeight.SemiBold
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.filteredPayables, key = SupplierPayable::id) { payable ->
                    SupplierDebtRow(payable, onPayClick)
                    HorizontalDivider(color = DebtLine.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtRow(
    payable: SupplierPayable,
    onPayClick: (SupplierPayable) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically) {
            SupplierInitial(payable.supplierName)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(payable.supplierName, color = DebtText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(payable.createdAt.simpleDate(), color = DebtMuted, fontSize = 11.sp)
            }
        }
        Text(payable.purchaseId.shortId(), modifier = Modifier.weight(1.4f), color = DebtMuted, fontSize = 12.sp)
        DebtAmountText(payable.amount, Modifier.weight(1.25f))
        DebtAmountText(payable.paidAmount, Modifier.weight(1.25f))
        DebtAmountText(payable.remainingAmount, Modifier.weight(1.25f), strong = true)
        Column(modifier = Modifier.weight(1.35f)) {
            Text(payable.dueDate, color = DebtText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(payable.dueDate.dueRelativeText(), color = payable.dueDate.dueColor(), fontSize = 11.sp)
        }
        Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
            StatusBadge(payable.status)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (payable.status == SupplierDebtStatusFilter.Paid.apiValue) {
                Text("Lunas", color = DebtMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = { onPayClick(payable) },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtPrimaryDark),
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
private fun SupplierInitial(name: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(DebtPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(name.initial(), color = DebtPrimaryDark, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SupplierDebtFooter(
    uiState: SupplierDebtUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DebtSoft.copy(alpha = 0.7f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} utang",
            color = DebtMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PageIconButton(Icons.Default.ChevronLeft, enabled = uiState.page > 1, onClick = onPreviousPage)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
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

@Composable
internal fun SupplierPaymentDialog(
    payable: SupplierPayable,
    uiState: SupplierDebtUiState,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (SupplierPaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Utang Supplier", color = DebtText, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PaymentSummary(payable)
                PaymentInputField(
                    value = uiState.paymentAmountInput,
                    onValueChange = onAmountChanged,
                    label = "Nominal pembayaran",
                    prefix = "Rp"
                )
                PaymentMethodCards(uiState.paymentMethod, onMethodChanged)
                PaymentInputField(
                    value = uiState.referenceInput,
                    onValueChange = onReferenceChanged,
                    label = "Referensi pembayaran"
                )
                PaymentInputField(
                    value = uiState.notesInput,
                    onValueChange = onNotesChanged,
                    label = "Catatan",
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmittingPayment,
                colors = ButtonDefaults.buttonColors(containerColor = DebtPrimaryDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (uiState.isSubmittingPayment) "Menyimpan..." else "Simpan Pembayaran")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = DebtMuted) } },
        containerColor = DebtSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun PaymentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String? = null,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = prefix?.let {
            { Text(it, color = DebtMuted, modifier = Modifier.padding(start = 12.dp)) }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = DebtTextFieldColors()
    )
}

@Composable
private fun PaymentSummary(payable: SupplierPayable) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DebtSoft)
            .padding(16.dp)
    ) {
        Text(payable.supplierName, color = DebtText, fontWeight = FontWeight.Bold)
        Text("Sisa utang ${payable.remainingAmount.currencyText()}", color = DebtMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PaymentMethodCards(
    selected: SupplierPaymentMethod,
    onSelect: (SupplierPaymentMethod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SupplierPaymentMethod.entries.forEach { method ->
            val isSelected = method == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) DebtPrimary.copy(alpha = 0.12f) else DebtSoft)
                    .clickable { onSelect(method) }
                    .padding(12.dp)
            ) {
                Text(method.label, color = if (isSelected) DebtPrimaryDark else DebtText, fontWeight = FontWeight.Bold)
                Text(
                    method.description,
                    color = DebtMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
