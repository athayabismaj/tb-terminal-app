package com.tbterminal.app.ui.receivables

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminReceivableScreen(
    name: String,
    role: String,
    receivableRepository: ReceivableRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ReceivableViewModel = viewModel(
        factory = ReceivableViewModel.factory(receivableRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Receivables,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onSalesTransactionsClick = onSalesTransactionsClick,
        onReportsClick = onReportsClick,
        onPriceManagementClick = onPriceManagementClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        ReceivableContent(
            modifier = contentModifier,
            uiState = uiState,
            onSearchChanged = viewModel::onSearchChanged,
            onStatusFilterChanged = viewModel::onStatusFilterChanged,
            onRefresh = { viewModel.loadReceivables() },
            onPayClick = viewModel::openPayment,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onDismissMessage = viewModel::clearMessage
        )
    }

    uiState.selectedReceivable?.let { receivable ->
        ReceivablePaymentDialog(
            receivable = receivable,
            uiState = uiState,
            onAmountChanged = viewModel::onPaymentAmountChanged,
            onMethodChanged = viewModel::onPaymentMethodChanged,
            onReferenceChanged = viewModel::onReferenceChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onDismiss = viewModel::closePayment,
            onSubmit = viewModel::submitPayment
        )
    }
}

@Composable
private fun ReceivableContent(
    modifier: Modifier,
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReceivableBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ReceivableHeader()
        ReceivableMessage(uiState, onDismissMessage)
        ReceivableMetrics(uiState)
        ReceivableTableCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            uiState = uiState,
            onSearchChanged = onSearchChanged,
            onStatusFilterChanged = onStatusFilterChanged,
            onRefresh = onRefresh,
            onPayClick = onPayClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
    }
}

@Composable
private fun ReceivableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text("Piutang Pelanggan", color = ReceivableText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Pantau hutang pelanggan dari transaksi POS dan catat pembayaran cicilan.",
                color = ReceivableMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(ReceivablePrimary.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Payments, contentDescription = null, tint = ReceivablePrimaryDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kontrol piutang pelanggan", color = ReceivablePrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceivableMessage(
    uiState: ReceivableUiState,
    onDismiss: () -> Unit
) {
    val message = uiState.errorMessage ?: uiState.message ?: return
    val isError = uiState.errorMessage != null
    val tint = if (isError) ReceivableDanger else ReceivablePrimaryDark
    val background = if (isError) ReceivableDanger.copy(alpha = 0.1f) else ReceivablePrimary.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Outlined.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, modifier = Modifier.weight(1f), color = tint, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) { Text("Tutup", color = tint) }
    }
}

@Composable
private fun ReceivableMetrics(uiState: ReceivableUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ReceivableMetricCard(
            modifier = Modifier.weight(1f),
            title = "TOTAL DATA",
            value = "${uiState.total}",
            subtitle = "Piutang pada filter aktif",
            icon = Icons.Outlined.Person,
            tint = ReceivablePrimaryDark
        )
        ReceivableMetricCard(
            modifier = Modifier.weight(1f),
            title = "SISA HALAMAN INI",
            value = uiState.pageRemainingTotal.currencyText(),
            subtitle = "Akumulasi data yang tampil",
            icon = Icons.Outlined.AccountBalanceWallet,
            tint = ReceivableInfo
        )
        ReceivableMetricCard(
            modifier = Modifier.weight(1f),
            title = "BELUM LUNAS",
            value = "${uiState.unpaidCount}",
            subtitle = "Butuh tindak lanjut",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            tint = ReceivableWarning
        )
    }
}

@Composable
private fun ReceivableMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = ReceivableSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ReceivableLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = ReceivableMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = ReceivableText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ReceivableTableCard(
    modifier: Modifier,
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onPayClick: (Receivable) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ReceivableSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ReceivableLine)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReceivableToolbar(uiState, onSearchChanged, onStatusFilterChanged, onRefresh)
            ReceivableTableHeader()
            ReceivableRows(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                onPayClick = onPayClick
            )
            ReceivableFooter(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun ReceivableToolbar(
    uiState: ReceivableUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ReceivableStatusFilter) -> Unit,
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
            placeholder = { Text("Cari pelanggan atau ID transaksi...", color = ReceivableMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = ReceivableMuted) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = ReceivableTextFieldColors()
        )
        ReceivableStatusFilterButton(uiState.statusFilter, onStatusFilterChanged)
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ReceivableLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = ReceivablePrimaryDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat Ulang", color = ReceivableAccentText, fontWeight = FontWeight.Bold)
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
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ReceivableLine),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Text(selected.label, color = ReceivableText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(18.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = ReceivableMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = ReceivablePrimaryDark
            )

            uiState.errorMessage != null && uiState.receivables.isEmpty() -> Text(
                "Piutang pelanggan gagal dimuat.",
                modifier = Modifier.align(Alignment.Center),
                color = ReceivableDanger,
                fontWeight = FontWeight.Bold
            )

            uiState.filteredReceivables.isEmpty() -> Text(
                "Belum ada piutang pelanggan yang cocok.",
                modifier = Modifier.align(Alignment.Center),
                color = ReceivableMuted,
                fontWeight = FontWeight.SemiBold
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.filteredReceivables, key = Receivable::id) { receivable ->
                    ReceivableRow(receivable, onPayClick)
                    HorizontalDivider(color = ReceivableLine.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun ReceivableRow(
    receivable: Receivable,
    onPayClick: (Receivable) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Text(receivable.transactionId.shortTransactionId(), modifier = Modifier.weight(1.45f), color = ReceivableMuted, fontSize = 12.sp)
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.total} piutang",
            color = ReceivableMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceivablePageIconButton(Icons.Default.ChevronLeft, enabled = uiState.page > 1, onClick = onPreviousPage)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
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

@Composable
private fun ReceivablePaymentDialog(
    receivable: Receivable,
    uiState: ReceivableUiState,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Piutang Pelanggan", color = ReceivableText, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PaymentSummary(receivable)
                PaymentInputField(
                    value = uiState.paymentAmountInput,
                    onValueChange = onAmountChanged,
                    label = "Nominal pembayaran",
                    prefix = "Rp",
                    isNumber = true
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
                colors = ButtonDefaults.buttonColors(containerColor = ReceivablePrimaryDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (uiState.isSubmittingPayment) "Menyimpan..." else "Simpan Pembayaran")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = ReceivableMuted) } },
        containerColor = ReceivableSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun PaymentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String? = null,
    minLines: Int = 1,
    isNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = prefix?.let {
            { Text(it, color = ReceivableMuted, modifier = Modifier.padding(start = 12.dp)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = ReceivableTextFieldColors()
    )
}

@Composable
private fun PaymentSummary(receivable: Receivable) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ReceivableSoft)
            .padding(16.dp)
    ) {
        Text(receivable.customerName, color = ReceivableText, fontWeight = FontWeight.Bold)
        Text("Sisa piutang ${receivable.remainingAmount.currencyText()}", color = ReceivableMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PaymentMethodCards(
    selected: ReceivablePaymentMethod,
    onSelect: (ReceivablePaymentMethod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReceivablePaymentMethod.entries.forEach { method ->
            val isSelected = method == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ReceivablePrimary.copy(alpha = 0.12f) else ReceivableSoft)
                    .clickable { onSelect(method) }
                    .padding(12.dp)
            ) {
                Text(method.label, color = if (isSelected) ReceivablePrimaryDark else ReceivableText, fontWeight = FontWeight.Bold)
                Text(
                    method.description,
                    color = ReceivableMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
