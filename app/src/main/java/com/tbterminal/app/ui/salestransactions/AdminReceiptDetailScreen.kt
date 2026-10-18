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
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.model.remainingAmount
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import com.tbterminal.app.ui.cashier.transactions.CashierPayDebtDialog
import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryUiState
import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryViewModel
import com.tbterminal.app.ui.cashier.transactions.TransactionActionDialogs
import com.tbterminal.app.ui.cashier.transactions.transactionActionAccess
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrandGreen = Color(0xFF256B57)
private val BrandGreenDark = Color(0xFF1D5545)
private val TextPrimary = Color(0xFF0F172A)
private val TextSlate800 = Color(0xFF1E293B)
private val TextSlate700 = Color(0xFF334155)
private val TextSecondary = Color(0xFF64748B)
private val TextMuted = Color(0xFF94A3B8)
private val BorderSlate200 = Color(0xFFE2E8F0)
private val SurfaceSlate100 = Color(0xFFF1F5F9)
private val SurfaceBackground = Color(0xFFF8F9FA)

private fun String.formatReceiptDateTime(): String = runCatching {
    OffsetDateTime.parse(this).format(
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    )
}.getOrElse {
    runCatching {
        LocalDateTime.parse(this).format(
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
        )
    }.getOrDefault(this)
}

private fun BigDecimal?.formatReceiptMoney(): String {
    val value = this ?: BigDecimal.ZERO
    return "Rp" + DecimalFormat("#,###", DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))).format(value)
}

private fun transactionStatusInfo(status: String, remainingAmount: BigDecimal = BigDecimal.ZERO): Pair<String, Color> {
    val lower = status.lowercase()
    if (lower in setOf("voided", "void")) return "Void" to Color(0xFFE11D48)
    if (lower in setOf("refunded", "refund")) return "Refund" to Color(0xFFE11D48)
    if (remainingAmount <= BigDecimal.ZERO) return "Lunas" to BrandGreen
    return when (lower) {
        "completed", "paid", "lunas", "success" -> "Lunas" to BrandGreen
        "dp", "partial" -> "Sebagian" to Color(0xFFD97706)
        "pending" -> "Menunggu" to Color(0xFFD97706)
        "hutang", "unpaid" -> "Hutang" to Color(0xFFE11D48)
        else -> (status.replaceFirstChar { it.uppercase() }) to Color(0xFFD97706)
    }
}

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
        pageTitle = "Detail Struk",
        onBack = onBackClick,
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
private fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = BorderSlate200,
    thickness: Dp = 1.dp,
    dashWidth: Dp = 6.dp,
    gapWidth: Dp = 4.dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val strokeWidthPx = with(density) { thickness.toPx() }
    val dashWidthPx = with(density) { dashWidth.toPx() }
    val gapWidthPx = with(density) { gapWidth.toPx() }
    val pathEffect = remember(dashWidthPx, gapWidthPx) {
        PathEffect.dashPathEffect(floatArrayOf(dashWidthPx, gapWidthPx), 0f)
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidthPx,
            pathEffect = pathEffect
        )
    }
}

@Composable
private fun ReceiptDetailContent(
    state: CashierTransactionHistoryUiState,
    transactionId: String,
    role: String,
    onShowPayDebt: () -> Unit,
    onHidePayDebt: () -> Unit,
    onPayDebtAmountChanged: (String) -> Unit,
    onPayDebtMethodChanged: (String) -> Unit,
    onPayDebt: () -> Unit,
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPrintDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                when {
                    state.isReceiptLoading -> ReceiptLoadingCard()
                    state.errorMessage != null -> ReceiptMessageCard(state.errorMessage)
                    state.selectedTransaction == null -> ReceiptMessageCard("Struk $transactionId belum dapat dimuat.")
                    else -> {
                        ReceiptSummaryCard(
                            transaction = state.selectedTransaction,
                            refundResult = state.refundResult
                        )
                        ReceiptActions(
                            transaction = state.selectedTransaction,
                            role = role,
                            onPrintClick = { showPrintDialog = true },
                            onShowPayDebt = onShowPayDebt,
                            onShowVoid = onShowVoid,
                            onShowRefund = onShowRefund
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

    if (showPrintDialog && state.selectedTransaction != null) {
        CetakStrukPreviewDialog(
            transaction = state.selectedTransaction,
            onDismiss = { showPrintDialog = false }
        )
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
private fun ReceiptSummaryCard(
    transaction: CashTransactionDetail,
    refundResult: TransactionRefundResult?
) {
    val (statusLabel, statusColor) = transactionStatusInfo(transaction.status, transaction.remainingAmount())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderSlate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Icon & No. Transaksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSlate100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = TextSlate800,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NO. TRANSAKSI",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.receiptNumber(),
                        color = TextSlate800,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = SurfaceSlate100, thickness = 1.dp)

            // Metadata Transaksi (Grid 2 Kolom Bernapas Lega & Simetris)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WAKTU TRANSAKSI",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.createdAt.formatReceiptDateTime(),
                            color = TextSlate800,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "KASIR",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.cashierName?.takeIf { it.isNotBlank() } ?: "-",
                            color = TextSlate800,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PELANGGAN",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.customerName?.takeIf { it.isNotBlank() } ?: "Umum",
                            color = TextSlate800,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "TIPE TRANSAKSI",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.type.ifBlank { "Penjualan" },
                            color = TextSlate800,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STATUS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            DashedDivider(color = BorderSlate200)

            // Bagian Daftar Belanja
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DAFTAR BELANJA",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                if (transaction.items.isEmpty()) {
                    Text("Tidak ada item.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    transaction.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = item.productName.ifBlank { "Produk ${item.productId.take(8)}" },
                                    color = TextSlate800,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val qtyText = if (item.quantity.compareTo(BigDecimal.ZERO) == 0) "0" else item.quantity.stripTrailingZeros().toPlainString()
                                Text(
                                    text = "$qtyText × ${item.priceAtTransaction.formatReceiptMoney()}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = item.subtotal.formatReceiptMoney(),
                                color = TextSlate800,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            DashedDivider(color = BorderSlate200)

            // Bagian Rincian Pembayaran
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RINCIAN PEMBAYARAN",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subtotal", color = TextSecondary, fontSize = 13.sp)
                    Text(transaction.total.formatReceiptMoney(), color = TextSlate800, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Diskon", color = TextSecondary, fontSize = 13.sp)
                    Text(BigDecimal.ZERO.formatReceiptMoney(), color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                HorizontalDivider(color = SurfaceSlate100, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(transaction.total.formatReceiptMoney(), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                DashedDivider(color = BorderSlate200)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Metode Pembayaran", color = TextSecondary, fontSize = 13.sp)
                    val isSettled = transaction.remainingAmount() <= BigDecimal.ZERO
                    val effectiveMethods = if (isSettled) {
                        transaction.paymentMethods.filterNot { it.lowercase() in setOf("hutang", "credit", "dp", "down_payment") }
                            .ifEmpty { listOf("tunai") }
                    } else {
                        transaction.paymentMethods.ifEmpty { listOf("tunai") }
                    }
                    val method = effectiveMethods.joinToString(" + ") { m ->
                        when (m.lowercase()) {
                            "tunai", "cash" -> "Tunai"
                            "transfer", "bank_transfer" -> "Transfer"
                            "qris" -> "QRIS"
                            "hutang", "credit" -> "Hutang"
                            "dp", "down_payment" -> "DP"
                            else -> m.replaceFirstChar { it.uppercase() }
                        }
                    }
                    Text(
                        text = method,
                        color = TextSlate800,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dibayar", color = TextSecondary, fontSize = 13.sp)
                    Text(transaction.paidAmount.formatReceiptMoney(), color = TextSlate800, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (transaction.amountTendered > BigDecimal.ZERO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Uang diterima", color = TextSecondary, fontSize = 13.sp)
                        Text(transaction.amountTendered.formatReceiptMoney(), color = TextSlate800, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                if (transaction.changeAmount > BigDecimal.ZERO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kembalian", color = TextSecondary, fontSize = 13.sp)
                        Text(transaction.changeAmount.formatReceiptMoney(), color = Color(0xFF047857), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = SurfaceSlate100, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                val remaining = transaction.remainingAmount()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sisa tagihan", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = remaining.formatReceiptMoney(),
                        color = if (remaining > BigDecimal.ZERO) Color(0xFFEF4444) else BrandGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Informasi Void jika dibatalkan
            if (transaction.status.equals("voided", true) || transaction.voidReason != null) {
                HorizontalDivider(color = SurfaceSlate100, thickness = 1.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("INFORMASI PEMBATALAN", color = Color(0xFFE11D48), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    transaction.voidedAt?.let {
                        ReceiptInfoRow("Dibatalkan", it.formatReceiptDateTime())
                    }
                    transaction.voidReason?.let {
                        ReceiptInfoRow("Alasan void", it, emphasized = true)
                    }
                }
            }

            // Informasi Refund jika ada
            refundResult?.takeIf { it.transactionId == transaction.id }?.let { refund ->
                HorizontalDivider(color = SurfaceSlate100, thickness = 1.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("INFORMASI REFUND", color = Color(0xFFE11D48), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    ReceiptInfoRow("Nomor refund", refund.refundNumber)
                    ReceiptInfoRow("Nominal", refund.refundedAmount.formatReceiptMoney(), emphasized = true)
                    ReceiptInfoRow("Kondisi barang", refund.returnDisposition.displayName)
                    ReceiptInfoRow("Alasan", refund.reason)
                    ReceiptInfoRow("Tanggal", refund.createdAt.formatReceiptDateTime())
                }
            }
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Normal)
        Text(
            value,
            color = if (emphasized) Color(0xFFE11D48) else TextSlate800,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReceiptActions(
    transaction: CashTransactionDetail,
    role: String,
    onPrintClick: () -> Unit,
    onShowPayDebt: () -> Unit,
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tombol Utama Cetak Struk (Sage Green #256B57)
        Button(
            onClick = onPrintClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Print,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (transaction.status.equals("voided", true)) "Cetak struk void" else "Cetak struk",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Tombol Terima Pelunasan jika masih ada sisa tagihan
        val canAcceptPayment = role.equals("KASIR", ignoreCase = true) || role.equals("OWNER", ignoreCase = true)
        if (!transaction.status.equals("voided", true) && transaction.remainingAmount() > BigDecimal.ZERO && canAcceptPayment) {
            Button(
                onClick = onShowPayDebt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terima Pelunasan",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Tombol Sekunder Berdampingan (Void & Refund)
        val access = transactionActionAccess(role, transaction.status, transaction.type)
        if (access.canVoid || access.canRefund) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (access.canVoid) {
                    OutlinedButton(
                        onClick = onShowVoid,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)), // rose-200
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF1F2)) // rose-50
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Void transaksi",
                            color = Color(0xFFE11D48),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (access.canRefund) {
                    OutlinedButton(
                        onClick = onShowRefund,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)), // rose-200
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF1F2)) // rose-50
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Refund transaksi",
                            color = Color(0xFFE11D48),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptLoadingCard() {
    com.tbterminal.app.ui.components.SkeletonCard(
        modifier = Modifier.fillMaxWidth().height(360.dp),
    )
}

@Composable
private fun ReceiptMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate200)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            Text(message, color = TextSecondary, fontWeight = FontWeight.Bold)
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

internal fun printTransactionReceipt(
    context: Context,
    transaction: CashTransactionDetail,
    coroutineScope: CoroutineScope? = null
) {
    val app = context.applicationContext as? com.tbterminal.app.TbTerminalApplication
    val printerManager = app?.appContainer?.thermalPrinterManager

    if (printerManager == null) {
        printTransactionReceiptPdf(context, transaction)
        return
    }

    val scope = coroutineScope ?: CoroutineScope(Dispatchers.Main)
    scope.launch {
        val result = printerManager.printReceipt(context, transaction)
        when (result) {
            is com.tbterminal.app.data.printer.PrinterResult.Success -> {
                Toast.makeText(context, "Struk berhasil dicetak ke printer.", Toast.LENGTH_SHORT).show()
            }
            is com.tbterminal.app.data.printer.PrinterResult.Error -> {
                if (result.message == "USE_SYSTEM_PRINT") {
                    printTransactionReceiptPdf(context, transaction)
                } else {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

internal fun printTransactionReceiptPdf(context: Context, transaction: CashTransactionDetail) {
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
        val isSettled = !isVoided && transaction.remainingAmount() <= BigDecimal.ZERO
        val displayStatus = if (isSettled) "LUNAS" else transaction.status.uppercase()
        canvas.drawText(if (isVoided) "STRUK VOID" else "TB TERMINAL", 32f, y, bold)
        y += 24f
        listOf(
            "Nomor: ${transaction.receiptNumber()}",
            "Tanggal: ${transaction.createdAt.displayDateTime()}",
            "Kasir: ${transaction.cashierName ?: "-"}",
            "Pelanggan: ${transaction.customerName ?: "Umum"}",
            "Status: $displayStatus"
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

