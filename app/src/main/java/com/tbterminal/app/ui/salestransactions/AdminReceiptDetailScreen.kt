package com.tbterminal.app.ui.salestransactions

import com.tbterminal.app.ui.components.SkeletonBox
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.tbterminal.app.ui.components.TopFloatingCapsule
import androidx.compose.ui.zIndex
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrandGreen = Color(0xFF10533C)
private val BrandGreenDark = Color(0xFF0D4331)
private val TextPrimary = Color(0xFF0F172A)
private val TextSlate800 = Color(0xFF1E293B)
private val TextSlate700 = Color(0xFF334155)
private val TextSecondary = Color(0xFF64748B)
private val TextMuted = Color(0xFF94A3B8)
private val BorderSlate200 = Color(0xFFE2E8F0)
private val SurfaceSlate100 = Color(0xFFF1F5F9)
private val SurfaceBackground = Color(0xFFF8FAFC)

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

internal fun BigDecimal?.formatReceiptMoney(): String {
    val value = this ?: BigDecimal.ZERO
    return "Rp " + DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))).format(value)
}

internal data class StatusBadgeStyle(
    val label: String,
    val textColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val icon: ImageVector
)

internal fun getStatusBadgeStyle(status: String, remainingAmount: BigDecimal): StatusBadgeStyle {
    val lower = status.lowercase()
    if (lower in setOf("voided", "void")) {
        return StatusBadgeStyle(
            label = "BATAL / VOID",
            textColor = Color(0xFFDC2626),
            backgroundColor = Color(0xFFFEF2F2),
            borderColor = Color(0xFFFECACA),
            icon = Icons.Outlined.Cancel
        )
    }
    if (lower in setOf("refunded", "refund")) {
        return StatusBadgeStyle(
            label = "REFUND",
            textColor = Color(0xFFE11D48),
            backgroundColor = Color(0xFFFFF1F2),
            borderColor = Color(0xFFFECDD3),
            icon = Icons.AutoMirrored.Outlined.Undo
        )
    }
    if (remainingAmount <= BigDecimal.ZERO) {
        return StatusBadgeStyle(
            label = "LUNAS",
            textColor = Color(0xFF047857),
            backgroundColor = Color(0xFFECFDF5),
            borderColor = Color(0xFFA7F3D0),
            icon = Icons.Outlined.CheckCircle
        )
    }
    return when (lower) {
        "dp", "partial" -> StatusBadgeStyle(
            label = "DP / SEBAGIAN",
            textColor = Color(0xFFD97706),
            backgroundColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFDE68A),
            icon = Icons.Outlined.Schedule
        )
        "hutang", "unpaid" -> StatusBadgeStyle(
            label = "HUTANG",
            textColor = Color(0xFFE11D48),
            backgroundColor = Color(0xFFFFF1F2),
            borderColor = Color(0xFFFECDD3),
            icon = Icons.Outlined.ErrorOutline
        )
        else -> StatusBadgeStyle(
            label = status.uppercase(),
            textColor = Color(0xFFD97706),
            backgroundColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFDE68A),
            icon = Icons.Outlined.Info
        )
    }
}

internal fun copyReceiptNumberToClipboard(context: Context, receiptNumber: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Nomor Struk", receiptNumber)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Nomor struk disalin: $receiptNumber", Toast.LENGTH_SHORT).show()
}

internal fun shareReceiptText(context: Context, transaction: CashTransactionDetail, storeName: String = "TB TERMINAL") {
    val text = buildReceiptShareText(transaction, storeName)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Struk Transaksi")
    context.startActivity(shareIntent)
}

internal fun buildReceiptShareText(transaction: CashTransactionDetail, storeName: String = "TB TERMINAL"): String {
    val isVoided = transaction.status.equals("voided", true)
    val remaining = transaction.remainingAmount()
    val isSettled = !isVoided && remaining <= BigDecimal.ZERO
    val statusText = when {
        isVoided -> "BATAL / VOID"
        isSettled -> "LUNAS"
        transaction.paymentMethods.any { it.lowercase() in setOf("dp", "partial") } -> "DP / UANG MUKA"
        transaction.paymentMethods.any { it.lowercase() in setOf("hutang", "credit") } -> "HUTANG"
        else -> transaction.status.uppercase()
    }

    val sb = StringBuilder()
    sb.appendLine("*$storeName*")
    sb.appendLine("Bukti Transaksi Pembayaran")
    sb.appendLine("================================")
    sb.appendLine("No. Struk : ${transaction.receiptNumber()}")
    sb.appendLine("Tanggal   : ${transaction.createdAt.formatReceiptDateTime()}")
    sb.appendLine("Kasir     : ${transaction.cashierName ?: "-"}")
    sb.appendLine("Pelanggan : ${transaction.customerName ?: "Pelanggan Umum"}")
    sb.appendLine("Status    : $statusText")
    sb.appendLine("--------------------------------")
    sb.appendLine("RINCIAN BARANG:")
    transaction.items.forEach { item ->
        val qtyText = if (item.quantity.stripTrailingZeros().scale() <= 0) {
            item.quantity.setScale(0).toPlainString()
        } else {
            item.quantity.stripTrailingZeros().toPlainString()
        }
        sb.appendLine("• ${item.productName.ifBlank { "Barang" }}")
        sb.appendLine("  $qtyText x ${item.priceAtTransaction.formatReceiptMoney()} = ${item.subtotal.formatReceiptMoney()}")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("TOTAL     : ${transaction.total.formatReceiptMoney()}")
    val methods = transaction.paymentMethods.joinToString(" + ") { m ->
        when (m.lowercase()) {
            "tunai", "cash" -> "Tunai"
            "qris" -> "QRIS"
            "transfer", "bank_transfer" -> "Transfer Bank"
            "hutang", "credit" -> "Hutang"
            "dp", "down_payment" -> "DP"
            else -> m.replaceFirstChar { it.uppercase() }
        }
    }.ifBlank { "Tunai" }
    sb.appendLine("Metode    : $methods")
    sb.appendLine("Dibayar   : ${transaction.paidAmount.formatReceiptMoney()}")
    if (transaction.changeAmount > BigDecimal.ZERO) {
        sb.appendLine("Kembalian : ${transaction.changeAmount.formatReceiptMoney()}")
    }
    if (remaining > BigDecimal.ZERO) {
        sb.appendLine("Sisa      : ${remaining.formatReceiptMoney()}")
    }
    if (isVoided && !transaction.voidReason.isNullOrBlank()) {
        sb.appendLine("Alasan Void: ${transaction.voidReason}")
    }
    sb.appendLine("================================")
    sb.appendLine("Terima kasih telah berbelanja di $storeName.")
    return sb.toString()
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
        Box(modifier = contentModifier.fillMaxSize()) {
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
                modifier = Modifier.fillMaxSize()
            )
            TopFloatingCapsule(
                message = uiState.receiptMessage,
                error = if (uiState.selectedTransaction != null) uiState.errorMessage else null,
                onDismiss = viewModel::clearFeedback,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
            )
        }
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
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showDeliveryOrderDialog by remember { mutableStateOf(false) }

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                when {
                    state.isReceiptLoading -> ReceiptLoadingCard()
                    state.errorMessage != null -> ReceiptMessageCard(state.errorMessage)
                    state.selectedTransaction == null -> ReceiptMessageCard("Struk $transactionId belum dapat dimuat.")
                    else -> {
                        val context = LocalContext.current
                        ReceiptSummaryCard(
                            transaction = state.selectedTransaction,
                            refundResult = state.refundResult
                        )
                        ReceiptActions(
                            transaction = state.selectedTransaction,
                            role = role,
                            onPrintClick = { showPrintDialog = true },
                            onPrintPdfClick = { printTransactionReceiptPdf(context, state.selectedTransaction) },
                            onShareClick = { shareReceiptText(context, state.selectedTransaction) },
                            onWhatsAppClick = { showWhatsAppDialog = true },
                            onDeliveryOrderClick = { showDeliveryOrderDialog = true },
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

    if (showWhatsAppDialog && state.selectedTransaction != null) {
        val context = LocalContext.current
        WhatsAppReceiptDialog(
            transaction = state.selectedTransaction,
            onDismissRequest = { showWhatsAppDialog = false },
            onSend = { phone ->
                showWhatsAppDialog = false
                WhatsAppReceiptHelper.sendViaWhatsApp(context, state.selectedTransaction, phone)
            }
        )
    }

    if (showDeliveryOrderDialog && state.selectedTransaction != null) {
        DeliveryOrderDialog(
            transaction = state.selectedTransaction,
            onDismiss = { showDeliveryOrderDialog = false }
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
internal fun ReceiptSummaryCard(
    transaction: CashTransactionDetail,
    refundResult: TransactionRefundResult?
) {
    val context = LocalContext.current
    val totalQty = transaction.items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.quantity) }
    val totalQtyText = if (totalQty.stripTrailingZeros().scale() <= 0) {
        totalQty.setScale(0).toPlainString()
    } else {
        totalQty.stripTrailingZeros().toPlainString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Store Branding Emblem & Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Store Branding Emblem
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF2F3FF),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF10533C),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = "TB TERMINAL BAROKAH JAYA",
                    color = Color(0xFF131B2E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Bahan Bangunan & Material",
                    color = Color(0xFF404944),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Jl. Raya Barokah No. 12, Jawa Timur • 0812-3456-7890",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Status Pill
                val badge = getStatusBadgeStyle(transaction.status, transaction.remainingAmount())
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = badge.icon,
                        contentDescription = null,
                        tint = badge.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = badge.label,
                        color = badge.textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Dotted Separator
            DashedDivider(
                color = Color(0xFFBFC9C2).copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // 2. Transaction Meta Data Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Receipt Transaction ID Box with Copy Action
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.35f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF404944),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "NO. TRANSAKSI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = transaction.receiptNumber(),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF131B2E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = { copyReceiptNumberToClipboard(context, transaction.receiptNumber()) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.35f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Salin nomor transaksi",
                                    tint = Color(0xFF404944),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2x2 Metadata Grid: Waktu, Kasir, Pelanggan, Jenis Pembayaran
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Waktu Transaksi & Kasir
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WAKTU TRANSAKSI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = transaction.createdAt.formatReceiptDateTime(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF131B2E),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "KASIR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = transaction.cashierName?.takeIf { it.isNotBlank() } ?: "-",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF131B2E),
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Row 2: Pelanggan & Jenis Pembayaran
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PELANGGAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = transaction.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan Umum",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF131B2E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        val isSettled = transaction.remainingAmount() <= BigDecimal.ZERO
                        val effectiveMethods = if (isSettled) {
                            transaction.paymentMethods.filterNot { it.lowercase() in setOf("hutang", "credit", "dp", "down_payment") }
                                .ifEmpty { listOf("tunai") }
                        } else {
                            transaction.paymentMethods.ifEmpty { listOf("tunai") }
                        }
                        val paymentMethodText = effectiveMethods.joinToString(" + ") { m ->
                            when (m.lowercase()) {
                                "tunai", "cash" -> "Tunai"
                                "transfer", "bank_transfer" -> "Transfer Bank"
                                "qris" -> "QRIS"
                                "hutang", "credit" -> "Hutang"
                                "dp", "down_payment" -> "DP"
                                else -> m.replaceFirstChar { it.uppercase() }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "JENIS PEMBAYARAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = paymentMethodText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF006C4A),
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Dotted Separator
            DashedDivider(
                color = Color(0xFFBFC9C2).copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // 3. Items Section (Daftar Barang)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DAFTAR BARANG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transaction.items.forEachIndexed { index, item ->
                        val qtyText = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                            item.quantity.setScale(0).toPlainString()
                        } else {
                            item.quantity.stripTrailingZeros().toPlainString()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = item.productName.ifBlank { "Barang" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF131B2E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$qtyText × ${item.priceAtTransaction.formatReceiptMoney()}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = item.subtotal.formatReceiptMoney(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF131B2E)
                            )
                        }
                        DashedDivider(color = Color(0xFFBFC9C2).copy(alpha = 0.3f))
                    }

                    // Total Kuantitas Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Kuantitas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${transaction.items.size} Jenis • $totalQtyText Pcs",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF131B2E)
                        )
                    }
                }
            }

            // Dotted Separator
            DashedDivider(
                color = Color(0xFFBFC9C2).copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // 4. Payment Breakdown Section (Rincian Pembayaran)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F3FF).copy(alpha = 0.4f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "RINCIAN PEMBAYARAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                // Subtotal (only if > 1 items)
                if (transaction.items.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtotal", color = Color(0xFF404944), fontSize = 13.sp)
                        Text(
                            transaction.total.formatReceiptMoney(),
                            color = Color(0xFF131B2E),
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                DashedDivider(color = Color(0xFFBFC9C2).copy(alpha = 0.4f))

                // Total Belanja Highlight
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL BELANJA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF131B2E)
                    )
                    Text(
                        text = transaction.total.formatReceiptMoney(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10533C),
                        letterSpacing = (-0.5).sp
                    )
                }

                DashedDivider(color = Color(0xFFBFC9C2).copy(alpha = 0.4f))

                // Payment Method & Tenders
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            "transfer", "bank_transfer" -> "Transfer Bank"
                            "qris" -> "QRIS"
                            "hutang", "credit" -> "Hutang"
                            "dp", "down_payment" -> "DP"
                            else -> m.replaceFirstChar { it.uppercase() }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Metode Pembayaran", color = Color(0xFF404944), fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = Color(0xFF006C4A),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = method,
                                color = Color(0xFF131B2E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (transaction.amountTendered > BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Uang Diterima", color = Color(0xFF404944), fontSize = 13.sp)
                            Text(
                                text = transaction.amountTendered.formatReceiptMoney(),
                                color = Color(0xFF131B2E),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (transaction.changeAmount > BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kembalian", color = Color(0xFF404944), fontSize = 13.sp)
                            Text(
                                text = transaction.changeAmount.formatReceiptMoney(),
                                color = Color(0xFF006C4A),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val remaining = transaction.remainingAmount()
                    if (remaining > BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sudah Dibayar", color = Color(0xFF404944), fontSize = 13.sp)
                            Text(
                                text = transaction.paidAmount.formatReceiptMoney(),
                                color = Color(0xFF131B2E),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        DashedDivider(color = Color(0xFFBFC9C2).copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sisa Tagihan / Hutang", color = Color(0xFFBA1A1A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = remaining.formatReceiptMoney(),
                                color = Color(0xFFBA1A1A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Informasi Void jika dibatalkan
                if (transaction.status.equals("voided", true) || transaction.voidReason != null) {
                    DashedDivider(color = Color(0xFFFECDD3))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("INFORMASI PEMBATALAN", color = Color(0xFFBA1A1A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    DashedDivider(color = Color(0xFFFECDD3))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("INFORMASI REFUND", color = Color(0xFFBA1A1A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        ReceiptInfoRow("Nomor refund", refund.refundNumber)
                        ReceiptInfoRow("Nominal", refund.refundedAmount.formatReceiptMoney(), emphasized = true)
                        ReceiptInfoRow("Kondisi barang", refund.returnDisposition.displayName)
                        ReceiptInfoRow("Alasan", refund.reason)
                        ReceiptInfoRow("Tanggal", refund.createdAt.formatReceiptDateTime())
                    }
                }

                // Catatan Kaki Struk (Receipt Footer with Simulated Barcode)
                DashedDivider(color = Color(0xFFBFC9C2).copy(alpha = 0.4f), modifier = Modifier.padding(top = 4.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Terima kasih telah berbelanja di TB Terminal",
                        fontSize = 12.sp,
                        color = Color(0xFF131B2E),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Simpan struk ini sebagai bukti transaksi yang sah.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    // Simulated Barcode
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BarcodeGraphic()
                        Text(
                            text = "REF: ${transaction.receiptNumber().takeLast(6).uppercase()}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B),
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeGraphic(modifier: Modifier = Modifier) {
    val barWidths = listOf(
        2.dp, 1.dp, 2.dp, 3.dp, 1.dp, 2.dp, 4.dp, 1.dp,
        3.dp, 1.dp, 2.dp, 4.dp, 1.dp, 3.dp, 2.dp, 1.dp, 4.dp, 1.dp, 2.dp
    )
    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barWidths.forEach { w ->
            Box(
                modifier = Modifier
                    .width(w)
                    .fillMaxHeight()
                    .background(Color(0xFF131B2E).copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
internal fun ReceiptInfoRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Normal)
        Text(
            value,
            color = if (emphasized) Color(0xFFBA1A1A) else Color(0xFF131B2E),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun ReceiptActions(
    transaction: CashTransactionDetail,
    role: String,
    onPrintClick: () -> Unit,
    onPrintPdfClick: () -> Unit,
    onShareClick: () -> Unit,
    onWhatsAppClick: () -> Unit = {},
    onDeliveryOrderClick: () -> Unit = {},
    onShowPayDebt: () -> Unit,
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit
) {
    val remaining = transaction.remainingAmount()
    val canAcceptPayment = role.equals("KASIR", ignoreCase = true) || role.equals("OWNER", ignoreCase = true)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Primary CTA: Print Receipt Direct to Thermal Printer
        Button(
            onClick = onPrintClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10533C)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Print,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (transaction.status.equals("voided", true)) "Cetak Struk Void" else "Cetak Struk",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Contextual: Pay Debt Button if remaining amount exists
        if (!transaction.status.equals("voided", true) && remaining > BigDecimal.ZERO && canAcceptPayment) {
            Button(
                onClick = onShowPayDebt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terima Pelunasan (${remaining.formatReceiptMoney()})",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Secondary Operations 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // WhatsApp Button
            OutlinedButton(
                onClick = onWhatsAppClick,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = null,
                    tint = Color(0xFF006C4A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WhatsApp",
                    color = Color(0xFF131B2E),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Cetak PDF Button
            OutlinedButton(
                onClick = onPrintPdfClick,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFF404944),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cetak PDF",
                    color = Color(0xFF131B2E),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Surat Jalan Button
            OutlinedButton(
                onClick = onDeliveryOrderClick,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    tint = Color(0xFF404944),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Surat Jalan",
                    color = Color(0xFF131B2E),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bagikan Button
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBFC9C2).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = Color(0xFF404944),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bagikan",
                    color = Color(0xFF131B2E),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3. Destructive / Reversal Action Controls (2-Column Grid)
        val access = transactionActionAccess(role, transaction.status, transaction.type)
        if (access.canVoid || access.canRefund) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (access.canVoid) {
                    OutlinedButton(
                        onClick = onShowVoid,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFDAD6)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Batal / Void",
                            color = Color(0xFFBA1A1A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
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
                        border = BorderStroke(1.dp, Color(0xFFFFDAD6)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.AssignmentReturn,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Refund Barang",
                            color = Color(0xFFBA1A1A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReceiptLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderSlate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: Icon & No. Transaksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBox(Modifier.size(36.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(Modifier.width(90.dp).height(11.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(Modifier.width(140.dp).height(14.dp))
                }
            }

            HorizontalDivider(color = SurfaceSlate100, thickness = 1.dp)

            // Metadata Transaksi (Grid 2 Kolom)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(Modifier.width(100.dp).height(10.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(130.dp).height(13.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        SkeletonBox(Modifier.width(50.dp).height(10.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(70.dp).height(13.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(Modifier.width(80.dp).height(10.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(100.dp).height(13.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        SkeletonBox(Modifier.width(80.dp).height(10.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(60.dp).height(13.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(Modifier.width(60.dp).height(10.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(70.dp).height(13.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            DashedDivider(color = BorderSlate200)

            // Bagian Daftar Belanja
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(Modifier.width(90.dp).height(11.dp))
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            SkeletonBox(Modifier.width(80.dp).height(12.dp))
                        }
                        SkeletonBox(Modifier.width(70.dp).height(14.dp))
                    }
                }
            }

            DashedDivider(color = BorderSlate200)

            // Ringkasan Pembayaran
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(60.dp).height(12.dp))
                    SkeletonBox(Modifier.width(80.dp).height(12.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(80.dp).height(16.dp))
                    SkeletonBox(Modifier.width(110.dp).height(16.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(90.dp).height(12.dp))
                    SkeletonBox(Modifier.width(80.dp).height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun ReceiptMessageCard(message: String) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pratinjau Struk Kasir",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate800
                    )
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = SurfaceSlate100,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Kertas Struk Fisik Thermal (Paper Roll Style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Name & Info
                        Text("TB TERMINAL", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 0.5.sp)
                        Text("Toko Bahan Bangunan & Material", fontSize = 11.sp, color = Color(0xFF475569))

                        if (transaction.status.equals("voided", true)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("*** TRANSAKSI DIBATALKAN / VOID ***", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            transaction.voidReason?.let { Text("Alasan: $it", fontSize = 10.sp, color = Color(0xFFDC2626)) }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        DashedDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata Struk
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("No. Struk", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(transaction.receiptNumber(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tanggal", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(transaction.createdAt.formatReceiptDateTime(), fontSize = 11.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kasir", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(transaction.cashierName ?: "-", fontSize = 11.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pelanggan", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(transaction.customerName ?: "Pelanggan Umum", fontSize = 11.sp, color = Color.Black)
                        }
                        val isSettled = !transaction.status.equals("voided", true) && transaction.remainingAmount() <= BigDecimal.ZERO
                        val statusLabel = if (isSettled) "LUNAS" else transaction.status.uppercase()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSettled) Color(0xFF047857) else Color(0xFFDC2626))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Rincian Item Belanja
                        transaction.items.forEach { item ->
                            val qtyText = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                                item.quantity.setScale(0).toPlainString()
                            } else {
                                item.quantity.stripTrailingZeros().toPlainString()
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.productName.ifBlank { "Barang" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  $qtyText x ${item.priceAtTransaction.formatReceiptMoney()}", fontSize = 11.sp, color = Color(0xFF475569))
                                Text(item.subtotal.formatReceiptMoney(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        DashedDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Financial Summary
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(transaction.total.formatReceiptMoney(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                        val methods = transaction.paymentMethods.joinToString(" + ") { m ->
                            when (m.lowercase()) {
                                "tunai", "cash" -> "Tunai"
                                "transfer", "bank_transfer" -> "Transfer"
                                "qris" -> "QRIS"
                                "hutang", "credit" -> "Hutang"
                                "dp", "down_payment" -> "DP"
                                else -> m.replaceFirstChar { it.uppercase() }
                            }
                        }.ifBlank { "Tunai" }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Metode Bayar", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(methods, fontSize = 11.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dibayar", fontSize = 11.sp, color = Color(0xFF475569))
                            Text(transaction.paidAmount.formatReceiptMoney(), fontSize = 11.sp, color = Color.Black)
                        }
                        if (transaction.amountTendered > BigDecimal.ZERO && transaction.amountTendered != transaction.paidAmount) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Uang Diterima", fontSize = 11.sp, color = Color(0xFF475569))
                                Text(transaction.amountTendered.formatReceiptMoney(), fontSize = 11.sp, color = Color.Black)
                            }
                        }
                        if (transaction.changeAmount > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembalian", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
                                Text(transaction.changeAmount.formatReceiptMoney(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            }
                        }
                        if (transaction.remainingAmount() > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sisa Tagihan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                Text(transaction.remainingAmount().formatReceiptMoney(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        DashedDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("TERIMA KASIH TELAH BERBELANJA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                        Text("Barang yang sudah dibeli dapat ditukar 1x24 jam", fontSize = 9.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        Text("dengan membawa struk ini.", fontSize = 9.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                    }
                }

                // Tombol Aksi di Bawah Kertas Struk
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol 1: Cetak ke Printer Thermal
                    Button(
                        onClick = { printTransactionReceipt(context, transaction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (transaction.status.equals("voided", true)) "Cetak Struk Void" else "Cetak ke Printer Thermal",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Tombol 2 & 3: Cetak PDF & Bagikan WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { printTransactionReceiptPdf(context, transaction) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSlate200),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = TextSlate700, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cetak PDF", color = TextSlate700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        OutlinedButton(
                            onClick = { shareReceiptText(context, transaction) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSlate200),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, tint = TextSlate700, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bagikan WA", color = TextSlate700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Tutup
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup", color = TextSecondary, fontSize = 13.sp)
                    }
                }
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
        "${if (transaction.status.equals("voided", true)) "Void" else "Faktur"}-${transaction.receiptNumber()}",
        TransactionReceiptPrintAdapter(context, transaction),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
    )
}

private class TransactionReceiptPrintAdapter(
    private val context: Context,
    private val transaction: CashTransactionDetail
) : PrintDocumentAdapter() {
    private lateinit var attributes: PrintAttributes

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        signal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (signal.isCanceled) return callback.onLayoutCancelled()
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("faktur-${transaction.receiptNumber()}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        signal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = PrintedPdfDocument(context, attributes)
        try {
            if (signal.isCanceled) return callback.onWriteCancelled()
            val page = document.startPage(0)
            drawInvoice(page)
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message ?: "Faktur gagal dicetak")
        } finally {
            document.close()
        }
    }

    private fun drawInvoice(page: PdfDocument.Page) {
        val canvas = page.canvas
        val pageWidth = page.info.pageWidth.toFloat()
        val pageHeight = page.info.pageHeight.toFloat()

        val left = 28f
        val right = pageWidth - 28f
        val printableWidth = right - left

        val isVoided = transaction.status.equals("voided", true)
        val isSettled = !isVoided && transaction.remainingAmount() <= BigDecimal.ZERO

        // Color palette
        val colorBrand = android.graphics.Color.rgb(16, 83, 60) // Forest Green #10533C
        val colorDark = android.graphics.Color.rgb(15, 23, 42) // Slate 900
        val colorMuted = android.graphics.Color.rgb(100, 116, 139) // Slate 500
        val colorBorder = android.graphics.Color.rgb(203, 213, 225) // Slate 300
        val colorHeaderBg = android.graphics.Color.rgb(241, 245, 249) // Slate 100
        val colorAltRow = android.graphics.Color.rgb(248, 250, 252) // Slate 50
        val colorLunas = android.graphics.Color.rgb(21, 128, 61) // Green 700
        val colorTempo = android.graphics.Color.rgb(194, 65, 12) // Amber 700
        val colorVoid = android.graphics.Color.rgb(185, 28, 28) // Red 700

        // Paints
        val storeTitlePaint = Paint().apply {
            color = colorBrand
            textSize = 14f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        val storeSubPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
        val docTitlePaint = Paint().apply {
            color = if (isVoided) colorVoid else colorDark
            textSize = 15f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val badgeTextPaint = Paint().apply {
            textSize = 8f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val rightMutedPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = android.graphics.Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = colorBorder
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val thickLinePaint = Paint().apply {
            color = colorBrand
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val metaLabelPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
        val metaValPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        val tableCellPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
        val tableCellBoldPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var y = 28f

        // 1. Header Bagian Atas
        canvas.drawText("TB TERMINAL BAROKAH JAYA", left, y, storeTitlePaint)
        canvas.drawText(if (isVoided) "FAKTUR PENJUALAN (BATAL)" else "FAKTUR PENJUALAN", right, y, docTitlePaint)
        y += 11f

        canvas.drawText("Pusat Perlengkapan & Bahan Bangunan Berkualitas", left, y, storeSubPaint)

        // Status Badge di sebelah kanan
        val statusText: String
        val badgeBgColor: Int
        val badgeBorderColor: Int
        val badgeTextColor: Int

        when {
            isVoided -> {
                statusText = "BATAL / VOID"
                badgeBgColor = android.graphics.Color.rgb(254, 242, 242)
                badgeBorderColor = android.graphics.Color.rgb(254, 202, 202)
                badgeTextColor = colorVoid
            }
            isSettled -> {
                statusText = "LUNAS"
                badgeBgColor = android.graphics.Color.rgb(240, 253, 244)
                badgeBorderColor = android.graphics.Color.rgb(187, 247, 208)
                badgeTextColor = colorLunas
            }
            else -> {
                statusText = "TEMPO / BELUM LUNAS"
                badgeBgColor = android.graphics.Color.rgb(254, 243, 199)
                badgeBorderColor = android.graphics.Color.rgb(253, 230, 138)
                badgeTextColor = colorTempo
            }
        }

        val badgeWidth = 92f
        val badgeHeight = 13f
        val badgeRect = android.graphics.RectF(right - badgeWidth, y - 9f, right, y - 9f + badgeHeight)
        fillPaint.color = badgeBgColor
        canvas.drawRoundRect(badgeRect, 3f, 3f, fillPaint)
        val badgeStroke = Paint().apply {
            color = badgeBorderColor
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 3f, 3f, badgeStroke)
        badgeTextPaint.color = badgeTextColor
        canvas.drawText(statusText, badgeRect.centerX(), y + 1f, badgeTextPaint)
        y += 10f

        canvas.drawText("Jl. Raya Terminal No. 45 | Telp: (031) 8921123 / WA: 0812-3456-7890", left, y, storeSubPaint)
        canvas.drawText("Dicetak: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())}", right, y, rightMutedPaint)
        y += 8f

        // Garis pemisah header
        canvas.drawLine(left, y, right, y, thickLinePaint)
        y += 8f

        // 2. Kotak Metadata Faktur & Pelanggan (2 Kolom)
        val metaBoxHeight = 44f
        fillPaint.color = android.graphics.Color.rgb(250, 250, 250)
        canvas.drawRoundRect(android.graphics.RectF(left, y, right, y + metaBoxHeight), 4f, 4f, fillPaint)
        canvas.drawRoundRect(android.graphics.RectF(left, y, right, y + metaBoxHeight), 4f, 4f, linePaint)

        val splitX = left + (printableWidth * 0.52f)
        canvas.drawLine(splitX, y, splitX, y + metaBoxHeight, linePaint)

        // Kolom Kiri: Info Nota
        val leftPad = left + 8f
        canvas.drawText("No. Faktur", leftPad, y + 12f, metaLabelPaint)
        canvas.drawText(": ${transaction.receiptNumber()}", leftPad + 52f, y + 12f, metaValPaint)
        canvas.drawText("Tanggal", leftPad, y + 24f, metaLabelPaint)
        canvas.drawText(": ${transaction.createdAt.formatReceiptDateTime()}", leftPad + 52f, y + 24f, metaValPaint)
        canvas.drawText("Kasir", leftPad, y + 36f, metaLabelPaint)
        canvas.drawText(": ${transaction.cashierName ?: "Kasir Toko"}", leftPad + 52f, y + 36f, metaValPaint)

        // Kolom Kanan: Pelanggan & Pembayaran
        val rightPad = splitX + 8f
        canvas.drawText("Kepada Yth", rightPad, y + 12f, metaLabelPaint)
        val customerName = transaction.customerName?.ifBlank { null } ?: "Pelanggan Umum"
        canvas.drawText(": $customerName", rightPad + 58f, y + 12f, metaValPaint)
        canvas.drawText("Pembayaran", rightPad, y + 24f, metaLabelPaint)
        val paymentMethodStr = transaction.paymentMethods.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { "Tunai" }
        canvas.drawText(": $paymentMethodStr", rightPad + 58f, y + 24f, metaValPaint)
        canvas.drawText("Keterangan", rightPad, y + 36f, metaLabelPaint)
        val noteDisplay = if (isVoided) {
            "Void: " + (transaction.voidReason ?: "-")
        } else {
            transaction.notes?.replace(Regex("""\[PENGIRIMAN\].*"""), "")?.trim()?.take(30)?.ifBlank { null } ?: "Penjualan Toko"
        }
        canvas.drawText(": $noteDisplay", rightPad + 58f, y + 36f, metaValPaint)

        y += metaBoxHeight + 10f

        // 3. Tabel Rincian Barang (Data Grid Table)
        val colNoW = 20f
        val colQtyW = 34f
        val colUnitW = 38f
        val colPriceW = 68f
        val colTotalW = 74f
        val colNameW = printableWidth - (colNoW + colQtyW + colUnitW + colPriceW + colTotalW)

        val xNo = left
        val xName = xNo + colNoW
        val xQty = xName + colNameW
        val xUnit = xQty + colQtyW
        val xPrice = xUnit + colUnitW
        val xTotal = xPrice + colPriceW

        // Header Tabel
        fillPaint.color = colorHeaderBg
        canvas.drawRect(left, y, right, y + 18f, fillPaint)
        canvas.drawRect(left, y, right, y + 18f, linePaint)

        canvas.drawText("NO", xNo + (colNoW / 2f), y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("NAMA BARANG / MATERIAL", xName + 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("QTY", xQty + colQtyW - 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("SATUAN", xUnit + 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("HARGA", xPrice + colPriceW - 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("SUBTOTAL", right - 4f, y + 12f, tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT })

        y += 18f

        // Baris Item
        val rowHeight = 16f
        transaction.items.forEachIndexed { index, item ->
            if (y < pageHeight - 145f) {
                if (index % 2 == 1) {
                    fillPaint.color = colorAltRow
                    canvas.drawRect(left, y, right, y + rowHeight, fillPaint)
                }
                canvas.drawLine(left, y + rowHeight, right, y + rowHeight, linePaint)

                // Kolom No
                canvas.drawText("${index + 1}", xNo + (colNoW / 2f), y + 11.5f, tableCellPaint.apply { textAlign = Paint.Align.CENTER })

                // Kolom Nama Barang
                canvas.drawText(item.productName.take(36), xName + 4f, y + 11.5f, tableCellBoldPaint.apply { textAlign = Paint.Align.LEFT })

                // Kolom Qty
                val qtyStr = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                    item.quantity.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
                } else {
                    item.quantity.stripTrailingZeros().toPlainString()
                }
                canvas.drawText(qtyStr, xQty + colQtyW - 4f, y + 11.5f, tableCellBoldPaint.apply { textAlign = Paint.Align.RIGHT })

                // Kolom Satuan
                val unitStr = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
                canvas.drawText(unitStr, xUnit + 4f, y + 11.5f, tableCellPaint.apply { textAlign = Paint.Align.LEFT })

                // Kolom Harga
                canvas.drawText(item.priceAtTransaction.formatReceiptMoney(), xPrice + colPriceW - 4f, y + 11.5f, tableCellPaint.apply { textAlign = Paint.Align.RIGHT })

                // Kolom Total
                canvas.drawText(item.subtotal.formatReceiptMoney(), right - 4f, y + 11.5f, tableCellBoldPaint.apply { textAlign = Paint.Align.RIGHT })

                y += rowHeight
            }
        }

        // Garis batas tabel vertikal luar
        canvas.drawLine(left, y, right, y, thickLinePaint)
        y += 8f

        // 4. Ringkasan Keuangan (Kanan) & Terbilang + Syarat (Kiri)
        val summarySplitX = left + (printableWidth * 0.54f)

        // Sisi Kiri: Terbilang & Ketentuan
        var yLeft = y
        val terbilangText = "Terbilang: " + com.tbterminal.app.data.printer.DeliveryOrderFormatter.numberToWordsIndonesian(transaction.total.toLong()) + " Rupiah"
        val terbilangPaint = Paint().apply {
            color = colorBrand
            textSize = 7.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
        canvas.drawText(terbilangText.take(55), left, yLeft + 8f, terbilangPaint)
        yLeft += 16f

        val policyHeaderPaint = Paint().apply {
            color = colorDark
            textSize = 7f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        val policyTextPaint = Paint().apply {
            color = colorMuted
            textSize = 6.5f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText("KETENTUAN TRANSAKSI:", left, yLeft + 4f, policyHeaderPaint)
        yLeft += 10f
        canvas.drawText("1. Barang yang sudah dibeli tidak dapat ditukar/dikembalikan tanpa bukti faktur sah.", left, yLeft + 2f, policyTextPaint)
        yLeft += 9f
        canvas.drawText("2. Klaim kerusakan/kekurangan fisik maksimal 1x24 jam sejak barang diterima.", left, yLeft + 2f, policyTextPaint)
        yLeft += 9f
        canvas.drawText("3. Faktur ini merupakan bukti pembayaran resmi yang sah dari TB Terminal.", left, yLeft + 2f, policyTextPaint)

        // Sisi Kanan: Rincian Keuangan
        var yRight = y
        val totalLabelPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
        val totalValPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        // Subtotal
        canvas.drawText("Subtotal", summarySplitX + 12f, yRight + 8f, totalLabelPaint)
        canvas.drawText(transaction.total.formatReceiptMoney(), right - 4f, yRight + 8f, totalValPaint)
        yRight += 13f

        // Total Akhir (Boxed Shaded Highlight)
        val totalBoxRect = android.graphics.RectF(summarySplitX + 8f, yRight, right, yRight + 16f)
        fillPaint.color = android.graphics.Color.rgb(241, 245, 249)
        canvas.drawRoundRect(totalBoxRect, 3f, 3f, fillPaint)
        canvas.drawRoundRect(totalBoxRect, 3f, 3f, linePaint)

        val grandLabelPaint = Paint().apply {
            color = colorBrand
            textSize = 8.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        val grandValPaint = Paint().apply {
            color = colorBrand
            textSize = 9.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("TOTAL TAGIHAN", summarySplitX + 12f, yRight + 11.5f, grandLabelPaint)
        canvas.drawText(transaction.total.formatReceiptMoney(), right - 4f, yRight + 11.5f, grandValPaint)
        yRight += 21f

        // Dibayar
        canvas.drawText("Jumlah Dibayar", summarySplitX + 12f, yRight + 4f, totalLabelPaint)
        canvas.drawText(transaction.paidAmount.formatReceiptMoney(), right - 4f, yRight + 4f, totalValPaint)
        yRight += 12f

        // Kembalian (jika ada)
        if (transaction.changeAmount > BigDecimal.ZERO) {
            val changePaint = Paint(totalValPaint).apply { color = colorLunas }
            canvas.drawText("Kembalian", summarySplitX + 12f, yRight + 4f, totalLabelPaint)
            canvas.drawText(transaction.changeAmount.formatReceiptMoney(), right - 4f, yRight + 4f, changePaint)
            yRight += 12f
        }

        // Sisa Tagihan (jika tempo)
        if (transaction.remainingAmount() > BigDecimal.ZERO) {
            val tempoPaint = Paint(totalValPaint).apply { color = colorTempo }
            canvas.drawText("Sisa Piutang / Tempo", summarySplitX + 12f, yRight + 4f, totalLabelPaint)
            canvas.drawText(transaction.remainingAmount().formatReceiptMoney(), right - 4f, yRight + 4f, tempoPaint)
            yRight += 12f
        }

        y = maxOf(yLeft + 14f, yRight + 14f)

        // 5. Kotak Tanda Tangan (2 Kolom: Pelanggan & Kasir)
        val sigColW = printableWidth * 0.42f
        val xSigLeft = left + (sigColW * 0.5f)
        val xSigRight = right - (sigColW * 0.5f)

        val sigHeaderPaint = Paint().apply {
            color = colorDark
            textSize = 8f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val sigLinePaint = Paint().apply {
            color = colorDark
            textSize = 7.5f
            typeface = android.graphics.Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawText("Tanda Terima Pelanggan,", xSigLeft, y, sigHeaderPaint)
        canvas.drawText("Hormat Kami / Kasir,", xSigRight, y, sigHeaderPaint)

        y += 32f

        val custSign = if (customerName != "Pelanggan Umum") "( ${customerName.take(22)} )" else "( ................................... )"
        canvas.drawText(custSign, xSigLeft, y, sigLinePaint)
        val cashierSign = "( ${transaction.cashierName ?: "Kasir TB Terminal"} )"
        canvas.drawText(cashierSign, xSigRight, y, sigLinePaint)

        y += 14f

        // 6. Footer Terima Kasih
        val footerPaint = Paint().apply {
            color = colorMuted
            textSize = 7f
            typeface = android.graphics.Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("*** Terima Kasih Atas Kunjungan & Kepercayaan Anda Berbelanja di TB Terminal ***", pageWidth / 2f, y, footerPaint)
    }
}
