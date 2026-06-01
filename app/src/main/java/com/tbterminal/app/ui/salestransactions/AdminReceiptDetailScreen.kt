package com.tbterminal.app.ui.salestransactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tbterminal.app.ui.cashier.transactions.remainingAmount


import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryViewModel
import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryUiState

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

import java.math.BigDecimal

private val ReceiptLine = Color(0xFFE2E8F0)
private val ReceiptSurfaceSoft = Color(0xFFF1F5F9)

@Composable
fun AdminReceiptDetailScreen(
    name: String,
    role: String,
    transactionId: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onBackClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashierTransactionHistoryViewModel = viewModel(
        factory = CashierTransactionHistoryViewModel.factory(cashReconciliationRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) {
        viewModel.loadReceipt(transactionId)
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SalesTransactions,
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
        ReceiptDetailContent(
            state = uiState,
            transactionId = transactionId,
            role = role,
            onBackClick = onBackClick,
            onShowPayDebt = viewModel::showPayDebtDialog,
            onHidePayDebt = viewModel::hidePayDebtDialog,
            onPayDebtAmountChanged = viewModel::onPayDebtAmountChanged,
            onPayDebtMethodChanged = viewModel::onPayDebtMethodChanged,
            onPayDebt = viewModel::payDebt,
            modifier = contentModifier
        )
    }
}

@Composable
private fun ReceiptDetailContent(
    state: CashierTransactionHistoryUiState,
    transactionId: String,
    role: String,
    onBackClick: () -> Unit,
    onShowPayDebt: () -> Unit,
    onHidePayDebt: () -> Unit,
    onPayDebtAmountChanged: (String) -> Unit,
    onPayDebtMethodChanged: (String) -> Unit,
    onPayDebt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardBackground)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ReceiptDetailHeader(onBackClick)
        when {
            state.isReceiptLoading -> ReceiptLoadingCard()
            state.errorMessage != null -> ReceiptMessageCard(state.errorMessage)
            state.selectedTransaction == null -> ReceiptMessageCard("Struk $transactionId belum dapat dimuat.")
            else -> ReceiptSummaryCard(
                transaction = state.selectedTransaction,
                message = state.receiptMessage.orEmpty(),
                role = role,
                onShowPayDebt = onShowPayDebt
            )
        }
    }
    
    if (state.isPayDebtDialogOpen) {
        CashierPayDebtDialog(
            state = state,
            onDismiss = onHidePayDebt,
            onAmountChanged = onPayDebtAmountChanged,
            onMethodChanged = onPayDebtMethodChanged,
            onConfirm = onPayDebt
        )
    }
}

@Composable
private fun ReceiptDetailHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Detail Struk", color = DashboardTextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = "Ringkasan transaksi kasir dan status pembayaran.",
                color = DashboardTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        OutlinedButton(
            onClick = onBackClick,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ReceiptLine)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembali", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceiptSummaryCard(
    transaction: CashTransactionDetail,
    message: String,
    role: String,
    onShowPayDebt: () -> Unit
) {
    var showPrintDialog by remember { mutableStateOf(false) }

    if (showPrintDialog) {
        CetakStrukPreviewDialog(
            transaction = transaction,
            onDismiss = { showPrintDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ReceiptLine)
    ) {
        Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            ReceiptTopSection(transaction)
            HorizontalDivider(color = ReceiptLine)
            ReceiptTotalsSection(transaction)
            HorizontalDivider(color = ReceiptLine)
            ReceiptItemsList(transaction.items)
            ReceiptActions(
                transaction = transaction,
                role = role,
                onPrintClick = { showPrintDialog = true },
                onShowPayDebt = onShowPayDebt
            )
        }
    }
}

@Composable
private fun ReceiptTopSection(transaction: CashTransactionDetail) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashboardBrandGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = DashboardBrandGreenDark)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(transaction.receiptNumber(), color = DashboardTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(transaction.createdAt.displayDateTime(), color = DashboardTextSecondary, fontSize = 13.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(transaction.status.statusColor().copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(transaction.status.uppercase(), color = transaction.status.statusColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceiptTotalsSection(transaction: CashTransactionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReceiptInfoRow("ID transaksi", transaction.receiptNumber())
        ReceiptInfoRow("Tipe transaksi", transaction.type.ifBlank { "-" })
        ReceiptInfoRow("Pelanggan", transaction.customerName ?: "Umum")
        ReceiptInfoRow("Subtotal", transaction.total.moneyText())
        ReceiptInfoRow("Dibayar", transaction.paidAmount.moneyText())
        ReceiptInfoRow("Sisa tagihan", transaction.remainingAmount().moneyText(), emphasized = transaction.remainingAmount() > BigDecimal.ZERO)
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DashboardTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = if (emphasized) Color(0xFFEF4444) else DashboardTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReceiptItemsList(items: List<CashTransactionItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daftar Belanja", color = DashboardTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        if (items.isEmpty()) {
            Text("Tidak ada item.", color = DashboardTextSecondary, fontSize = 14.sp)
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.productName.ifBlank { "Produk ${item.productId.take(8)}" }, color = DashboardTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${item.quantity} x ${item.priceAtTransaction.moneyText()}", color = DashboardTextSecondary, fontSize = 12.sp)
                    }
                    Text(item.subtotal.moneyText(), color = DashboardTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ReceiptActions(
    transaction: CashTransactionDetail,
    role: String,
    onPrintClick: () -> Unit,
    onShowPayDebt: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        val canAcceptPayment = role.equals("KASIR", ignoreCase = true) || role.equals("OWNER", ignoreCase = true)
        if (transaction.remainingAmount() > BigDecimal.ZERO && canAcceptPayment) {
            androidx.compose.material3.Button(
                onClick = onShowPayDebt,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terima Pelunasan", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        OutlinedButton(
            onClick = onPrintClick,
            enabled = true,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ReceiptLine)
        ) {
            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cetak struk")
        }
    }
}

@Composable
private fun ReceiptLoadingCard() {
    Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DashboardBrandGreenDark)
    }
}

@Composable
private fun ReceiptMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ReceiptLine)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            Text(message, color = DashboardTextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CetakStrukPreviewDialog(
    transaction: CashTransactionDetail,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TB TERMINAL", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
                Text("Struk Pembelian", fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("No", fontSize = 12.sp, color = Color.Black)
                    Text(transaction.receiptNumber(), fontSize = 12.sp, color = Color.Black)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tgl", fontSize = 12.sp, color = Color.Black)
                    Text(transaction.createdAt.displayDateTime(), fontSize = 12.sp, color = Color.Black)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Plg", fontSize = 12.sp, color = Color.Black)
                    Text(transaction.customerName ?: "Umum", fontSize = 12.sp, color = Color.Black)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                transaction.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.productName.ifBlank { "Barang" }, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("  ${item.quantity} x ${item.priceAtTransaction.moneyText()}", fontSize = 12.sp, color = Color.Black)
                        Text(item.subtotal.moneyText(), fontSize = 12.sp, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(transaction.total.moneyText(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DIBAYAR", fontSize = 12.sp, color = Color.Black)
                    Text(transaction.paidAmount.moneyText(), fontSize = 12.sp, color = Color.Black)
                }
                if (transaction.remainingAmount() > BigDecimal.ZERO) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SISA TAGIHAN", fontSize = 12.sp, color = Color.Black)
                        Text(transaction.remainingAmount().moneyText(), fontSize = 12.sp, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("TERIMA KASIH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup", color = Color.Black)
                }
            }
        }
    }
}

internal fun CashTransactionDetail.receiptNumber(): String {
    return receiptId
}

@Composable
internal fun CashierPayDebtDialog(
    state: CashierTransactionHistoryUiState,
    onDismiss: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Terima Pelunasan",
                    color = DashboardTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Masukkan nominal pembayaran untuk sisa tagihan struk ini.",
                    color = DashboardTextSecondary,
                    fontSize = 14.sp
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("NOMINAL BAYAR", color = DashboardTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    androidx.compose.material3.OutlinedTextField(
                        value = state.payDebtAmountInput,
                        onValueChange = onAmountChanged,
                        placeholder = { Text("Misal: 50000", color = DashboardTextSecondary) },
                        leadingIcon = { Text("Rp", color = DashboardTextSecondary, fontWeight = FontWeight.Bold) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.TextButton(onClick = onDismiss, enabled = !state.isSubmittingDebt) {
                        Text("Batal", color = DashboardTextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        enabled = !state.isSubmittingDebt,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSubmittingDebt) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
