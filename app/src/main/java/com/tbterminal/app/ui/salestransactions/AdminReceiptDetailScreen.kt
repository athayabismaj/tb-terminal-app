package com.tbterminal.app.ui.salestransactions

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.ui.cashier.transactions.TransactionActionDialogs
import com.tbterminal.app.ui.cashier.transactions.transactionActionAccess
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.AppStatusChip

import java.math.BigDecimal
import java.io.FileOutputStream

private val ReceiptLine = Color(0xFFE2E8F0)
private val ReceiptSurfaceSoft = Color(0xFFF1F5F9)

@Composable
fun AdminReceiptDetailScreen(
    name: String,
    role: String,
    transactionId: String,
    cashReconciliationRepository: CashReconciliationRepository,
    managerApprovalRepository: ManagerApprovalRepository,
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
        factory = CashierTransactionHistoryViewModel.factory(
            repository = cashReconciliationRepository,
            actorRole = role,
        )
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
            onShowVoid = viewModel::showVoidDialog,
            onShowRefund = viewModel::showRefundDialog,
            modifier = contentModifier
        )
    }
    TransactionActionDialogs(
        state = uiState,
        managerApprovalRepository = managerApprovalRepository,
        onDismissVoid = viewModel::hideVoidDialog,
        onVoidReasonChanged = viewModel::onVoidReasonChanged,
        onConfirmVoid = viewModel::submitVoid,
        onDismissRefund = viewModel::hideRefundDialog,
        onRefundReasonChanged = viewModel::onRefundReasonChanged,
        onRefundDispositionChanged = viewModel::onRefundDispositionChanged,
        onConfirmRefund = viewModel::submitRefund,
        onApprovalGranted = viewModel::onManagerApprovalGranted,
        onApprovalDismissed = viewModel::cancelManagerApproval,
    )
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
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardBackground)
            .verticalScroll(rememberScrollState())
            .padding(if (compact) 16.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp)
    ) {
        ReceiptDetailHeader(onBackClick, compact)
        when {
            state.isReceiptLoading -> ReceiptLoadingCard()
            state.errorMessage != null -> ReceiptMessageCard(state.errorMessage)
            state.selectedTransaction == null -> ReceiptMessageCard("Struk $transactionId belum dapat dimuat.")
            else -> ReceiptSummaryCard(
                transaction = state.selectedTransaction,
                message = state.receiptMessage.orEmpty(),
                refundResult = state.refundResult,
                role = role,
                onShowPayDebt = onShowPayDebt,
                onShowVoid = onShowVoid,
                onShowRefund = onShowRefund,
                compact = compact
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
private fun ReceiptDetailHeader(onBackClick: () -> Unit, compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Detail Struk", color = DashboardTextPrimary, fontSize = if (compact) 24.sp else 32.sp, fontWeight = FontWeight.ExtraBold)
            if (!compact) Text(
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
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", modifier = Modifier.size(18.dp))
            if (!compact) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kembali", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReceiptSummaryCard(
    transaction: CashTransactionDetail,
    message: String,
    refundResult: TransactionRefundResult?,
    role: String,
    onShowPayDebt: () -> Unit,
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit,
    compact: Boolean
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
        Column(modifier = Modifier.padding(if (compact) 16.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 22.dp)) {
            ReceiptTopSection(transaction, compact)
            HorizontalDivider(color = ReceiptLine)
            ReceiptTotalsSection(transaction)
            HorizontalDivider(color = ReceiptLine)
            ReceiptItemsList(transaction.items)
            refundResult?.takeIf { it.transactionId == transaction.id }?.let { refund ->
                RefundInformation(refund)
            }
            ReceiptActions(
                transaction = transaction,
                role = role,
                onPrintClick = { showPrintDialog = true },
                onShowPayDebt = onShowPayDebt,
                onShowVoid = onShowVoid,
                onShowRefund = onShowRefund,
                compact = compact
            )
        }
    }
}

@Composable
private fun ReceiptTopSection(transaction: CashTransactionDetail, compact: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compact) 44.dp else 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashboardBrandGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = DashboardBrandGreenDark)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(transaction.receiptNumber(), color = DashboardTextPrimary, fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(transaction.createdAt.displayDateTime(), color = DashboardTextSecondary, fontSize = 13.sp)
            }
        }
        AppStatusChip(transaction.status)
    }
}

@Composable
private fun ReceiptTotalsSection(transaction: CashTransactionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReceiptInfoRow("ID transaksi", transaction.receiptNumber())
        ReceiptInfoRow("Tipe transaksi", transaction.type.ifBlank { "-" })
        ReceiptInfoRow("Pelanggan", transaction.customerName ?: "Umum")
        ReceiptInfoRow("Kasir", transaction.cashierName ?: "-")
        ReceiptInfoRow("Metode", transaction.paymentMethods.joinToString(", ").ifBlank { "-" })
        ReceiptInfoRow("Subtotal", transaction.total.moneyText())
        ReceiptInfoRow("Dibayar", transaction.paidAmount.moneyText())
        ReceiptInfoRow("Uang diterima", transaction.amountTendered.moneyText())
        if (transaction.changeAmount > BigDecimal.ZERO) {
            ReceiptInfoRow("Kembalian", transaction.changeAmount.moneyText(), emphasized = true)
        }
        ReceiptInfoRow("Sisa tagihan", transaction.remainingAmount().moneyText(), emphasized = transaction.remainingAmount() > BigDecimal.ZERO)
        transaction.voidedAt?.let { ReceiptInfoRow("Dibatalkan", it.displayDateTime()) }
        transaction.voidReason?.let { ReceiptInfoRow("Alasan void", it, emphasized = true) }
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
    onShowPayDebt: () -> Unit,
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit,
    compact: Boolean
) {
    val actions: @Composable () -> Unit = {
        val canAcceptPayment = role.equals("KASIR", ignoreCase = true) || role.equals("OWNER", ignoreCase = true)
        if (!transaction.status.equals("voided", true) && transaction.remainingAmount() > BigDecimal.ZERO && canAcceptPayment) {
            androidx.compose.material3.Button(
                onClick = onShowPayDebt,
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terima Pelunasan", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = if (compact) Modifier.height(10.dp) else Modifier.width(12.dp))
        }
        val access = transactionActionAccess(role, transaction.status, transaction.type)
        if (access.canVoid) {
            Button(
                onClick = onShowVoid,
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Void transaksi", fontWeight = FontWeight.Bold) }
            Spacer(modifier = if (compact) Modifier.height(10.dp) else Modifier.width(12.dp))
        }
        if (access.canRefund) {
            OutlinedButton(
                onClick = onShowRefund,
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFB91C1C)),
            ) { Text("Refund transaksi", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold) }
            Spacer(modifier = if (compact) Modifier.height(10.dp) else Modifier.width(12.dp))
        }

        OutlinedButton(
            onClick = onPrintClick,
            modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
            enabled = true,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ReceiptLine)
        ) {
            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cetak struk")
        }
    }
    if (compact) Column(modifier = Modifier.fillMaxWidth()) { actions() }
    else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
}

@Composable
private fun RefundInformation(refund: TransactionRefundResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = ReceiptLine)
        Text("Informasi Refund", color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
        ReceiptInfoRow("Nomor refund", refund.refundNumber)
        ReceiptInfoRow("Nominal", refund.refundedAmount.moneyText(), emphasized = true)
        ReceiptInfoRow("Kondisi barang", refund.returnDisposition.displayName)
        ReceiptInfoRow("Alasan", refund.reason)
        ReceiptInfoRow("Tanggal", refund.createdAt.displayDateTime())
    }
}

@Composable
private fun ReceiptLoadingCard() {
    com.tbterminal.app.ui.components.SkeletonCard(
        modifier = Modifier.fillMaxWidth().height(320.dp),
    )
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
    val context = LocalContext.current
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
                if (transaction.status.equals("voided", true)) {
                    Text("*** DIBATALKAN / VOID ***", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFB91C1C))
                    transaction.voidReason?.let { Text("Alasan: $it", fontSize = 11.sp, color = Color(0xFFB91C1C)) }
                }
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Uang diterima", fontSize = 12.sp, color = Color.Black)
                    Text(transaction.amountTendered.moneyText(), fontSize = 12.sp, color = Color.Black)
                }
                if (transaction.changeAmount > BigDecimal.ZERO) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kembalian", fontSize = 12.sp, color = Color.Black)
                        Text(transaction.changeAmount.moneyText(), fontSize = 12.sp, color = Color.Black)
                    }
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
                
                Button(
                    onClick = { printTransactionReceipt(context, transaction) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (transaction.status.equals("voided", true)) "Cetak struk void" else "Cetak struk")
                }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    }
}

internal fun printTransactionReceipt(context: Context, transaction: CashTransactionDetail) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print(
        "${if (transaction.status.equals("voided", true)) "Void" else "Struk"}-${transaction.receiptNumber()}",
        TransactionReceiptPrintAdapter(context, transaction),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
    )
}

private class TransactionReceiptPrintAdapter(
    private val context: Context,
    private val transaction: CashTransactionDetail
) : PrintDocumentAdapter() {
    private lateinit var attributes: PrintAttributes

    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, signal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
        if (signal.isCanceled) return callback.onLayoutCancelled()
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("struk-${transaction.receiptNumber()}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(),
            true
        )
    }

    override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, signal: CancellationSignal, callback: WriteResultCallback) {
        val document = PrintedPdfDocument(context, attributes)
        try {
            if (signal.isCanceled) return callback.onWriteCancelled()
            val page = document.startPage(0)
            draw(page)
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message ?: "Struk gagal dicetak")
        } finally {
            document.close()
        }
    }

    private fun draw(page: PdfDocument.Page) {
        val normal = Paint().apply { color = android.graphics.Color.BLACK; textSize = 11f }
        val bold = Paint(normal).apply { isFakeBoldText = true; textSize = 16f }
        val canvas = page.canvas
        var y = 42f
        val isVoided = transaction.status.equals("voided", true)
        canvas.drawText(if (isVoided) "STRUK VOID" else "TB TERMINAL", 32f, y, bold)
        y += 24f
        listOf(
            "Nomor: ${transaction.receiptNumber()}",
            "Tanggal: ${transaction.createdAt.displayDateTime()}",
            "Kasir: ${transaction.cashierName ?: "-"}",
            "Pelanggan: ${transaction.customerName ?: "Umum"}",
            "Status: ${transaction.status.uppercase()}"
        ).forEach { canvas.drawText(it.take(82), 32f, y, normal); y += 18f }
        if (isVoided) {
            canvas.drawText("Alasan void: ${transaction.voidReason ?: "-"}".take(82), 32f, y, normal)
            y += 22f
        }
        transaction.items.forEach { item ->
            if (y < page.info.pageHeight - 130) {
                canvas.drawText(item.productName.take(48), 32f, y, normal); y += 16f
                canvas.drawText("  ${item.quantity} x ${item.priceAtTransaction.moneyText()} = ${item.subtotal.moneyText()}".take(82), 32f, y, normal); y += 18f
            }
        }
        y += 6f
        canvas.drawText("TOTAL: ${transaction.total.moneyText()}", 32f, y, bold); y += 22f
        canvas.drawText("Dibayar: ${transaction.paidAmount.moneyText()}", 32f, y, normal); y += 18f
        canvas.drawText("Kembalian: ${transaction.changeAmount.moneyText()}", 32f, y, normal); y += 18f
        canvas.drawText("Sisa: ${transaction.remainingAmount().moneyText()}", 32f, y, normal)
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
