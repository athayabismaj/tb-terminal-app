package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.ui.common.toRupiahText
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.ui.common.toRupiahText
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

private val SurfaceBg = Color(0xFFF8FAF8)
private val Brand50 = Color(0xFFF2F7F4)
private val Brand100 = Color(0xFFE4EFEA)
private val Brand600 = Color(0xFF256B57)
private val Brand700 = Color(0xFF1E5E4E)
private val Brand800 = Color(0xFF184C3F)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF9CA3AF)
private val Slate500 = Color(0xFF6B7280)
private val Slate600 = Color(0xFF4B5563)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1F2937)
private val Slate900 = Color(0xFF111827)
private val BorderLight = Color(0xFFF3F4F6)
private val BorderDarker = Color(0xFFE5E7EB)
private val Emerald800 = Color(0xFF065F46)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminReceivableCustomerDetailScreen(
    name: String,
    role: String,
    customerId: String,
    receivableRepository: ReceivableRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReportsClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ReceivableCustomerDetailViewModel = viewModel(
        factory = ReceivableCustomerDetailViewModel.factory(receivableRepository, customerId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaymentHistory by remember { mutableStateOf(false) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Receivables,
        showPageHeader = true,
        pageTitle = "Detail Piutang",
        topBarActions = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .background(Brand50, CircleShape)
                    .border(1.dp, Brand100, CircleShape)
                    .clickable { showPaymentHistory = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "Riwayat Pembayaran",
                    tint = Brand700,
                    modifier = Modifier.size(20.dp)
                )
                if (uiState.payments.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(16.dp)
                            .background(Brand700, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.payments.size.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        onBack = onBack,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onStockOpnameClick = onStockOpnameClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onLogout = onLogout
    ) { contentModifier ->
        when {
            uiState.isLoading -> ReceivableCustomerDetailSkeleton(modifier = contentModifier)
            uiState.errorMessage != null -> {
                Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Column(
                    modifier = contentModifier
                        .fillMaxSize()
                        .background(SurfaceBg)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CustomerSummaryCard(uiState)
                    InvoiceListSection(uiState.receivables)
                }

                if (showPaymentHistory) {
                    ModalBottomSheet(
                        onDismissRequest = { showPaymentHistory = false },
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        PaymentHistoryContent(uiState.payments, onClose = { showPaymentHistory = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableCustomerDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkeletonBox(Modifier.size(40.dp), shape = CircleShape)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SkeletonBox(Modifier.width(130.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                                SkeletonBox(Modifier.width(46.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                            }
                            SkeletonBox(Modifier.width(80.dp).height(11.dp), shape = RoundedCornerShape(3.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(88.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(115.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
                    }
                }

                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(78.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(100.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(78.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(100.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(Modifier.width(90.dp).height(16.dp), shape = RoundedCornerShape(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    repeat(3) { index ->
                        if (index > 0) HorizontalDivider(color = BorderLight)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SkeletonBox(Modifier.width(95.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                                    SkeletonBox(Modifier.width(55.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
                                }
                                SkeletonBox(Modifier.width(80.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SkeletonBox(Modifier.width(70.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                                    SkeletonBox(Modifier.width(110.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                                }
                                SkeletonBox(Modifier.width(76.dp).height(32.dp), shape = RoundedCornerShape(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerSummaryCard(uiState: ReceivableCustomerDetailUiState) {
    val allPaid = uiState.totalRemaining <= java.math.BigDecimal.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Customer Identity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val circleBg = if (allPaid) Brand50 else Slate100
                    val circleBorder = if (allPaid) Brand100 else Slate200
                    val circleText = if (allPaid) Brand700 else Slate700

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(circleBg, CircleShape)
                            .border(1.dp, circleBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.customerName.take(1).uppercase(),
                            color = circleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.customerName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "${uiState.receivables.size} Nota",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = if (allPaid) "Semua tagihan lunas" else "Belum lunas",
                            fontSize = 11.sp,
                            color = if (allPaid) Brand600 else Slate400,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SISA PIUTANG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = uiState.totalRemaining.toRupiahText(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (allPaid) Slate900 else Brand700,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 12.dp))

            // Financial Breakdown
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TOTAL TAGIHAN", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, letterSpacing = 0.5.sp, maxLines = 1, softWrap = false)
                    Text(uiState.totalAmount.toRupiahText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate800, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 2.dp))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("SUDAH DIBAYAR", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate400, letterSpacing = 0.5.sp, maxLines = 1, softWrap = false)
                    Text(uiState.totalPaid.toRupiahText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand700, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun InvoiceListSection(receivables: List<Receivable>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Daftar nota", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                receivables.forEachIndexed { index, receivable ->
                    if (index > 0) HorizontalDivider(color = BorderLight)
                    DetailReceivableCard(receivable)
                }
            }
        }
        
        DetailPaginationRow(receivables.size, receivables.size)
    }
}

@Composable
private fun DetailReceivableCard(receivable: Receivable) {
    val isPaid = receivable.isPaid()
    val refNo = receivable.legacyInvoiceNumber?.takeIf(String::isNotBlank)
        ?: receivable.transactionId?.shortTransactionId()
        ?: receivable.source

    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { }
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(refNo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        HorizontalDivider(color = BorderLight, modifier = Modifier.padding(bottom = 4.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("SISA PIUTANG", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(receivable.remainingAmount.toRupiahText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand700, modifier = Modifier.padding(top = 4.dp))
                }
                Column {
                    Text("TOTAL TAGIHAN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(receivable.amount.toRupiahText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800, modifier = Modifier.padding(top = 2.dp))
                }
                Column {
                    Text("JATUH TEMPO", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(receivable.dueDate.simpleDate(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate600, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(if (isPaid) "LUNAS" else "AKTIF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isPaid) Emerald800 else Brand600, modifier = Modifier.padding(top = 4.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SUDAH DIBAYAR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(receivable.paidAmount.toRupiahText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand700, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailPaginationRow(displayCount: Int, totalCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, BorderDarker),
            modifier = Modifier.height(28.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("$totalCount data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
            }
        }
        
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, BorderDarker),
            modifier = Modifier.height(32.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                Text("1 / 1", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PaymentHistoryContent(payments: List<ReceivablePaymentHistory>, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Riwayat Pembayaran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Surface(color = Brand50, border = BorderStroke(1.dp, Brand100), shape = RoundedCornerShape(50)) {
                        Text("${payments.size} transaksi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand700, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Text("Daftar pembayaran yang telah diterima", fontSize = 12.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp).background(SurfaceBg, CircleShape)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = BorderLight)
        
        // List
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            payments.forEach { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Brand600, CircleShape))
                                Text(payment.paymentNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            Text(
                                "${payment.paymentDate.simpleDate()} • ${payment.method}",
                                fontSize = 11.sp,
                                color = Slate500,
                                modifier = Modifier.padding(top = 4.dp, start = 14.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(payment.amount.toRupiahText(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Brand700, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text(if (payment.isReversed) "DIBATALKAN" else "BERHASIL", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (payment.isReversed) Color.Red else Emerald800, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
        
        // Bottom Action
        HorizontalDivider(color = BorderLight)
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand700)
            ) {
                Text("Tutup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

private fun java.math.BigDecimal?.toRupiahText(): String {
    if (this == null) return "Rp0"
    val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(this).replace("Rp", "Rp")
}

private fun java.time.LocalDateTime?.simpleDate(): String {
    if (this == null) return "-"
    return this.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}


private fun Receivable.isPaid(): Boolean {
    return this.remainingAmount <= java.math.BigDecimal.ZERO
}
