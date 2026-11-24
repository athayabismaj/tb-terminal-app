package com.tbterminal.app.ui.receivables

import com.tbterminal.app.ui.components.TbPagination

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.R
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.ui.components.SkeletonBox
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// Palette matching HTML mockup & TB Terminal brand design
private val SurfaceBg = Color(0xFFF8FAFC)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)

private val BrandGreen = Color(0xFF10533C)
private val BrandDark = Color(0xFF0C3E2D)
private val BrandLight = Color(0xFFE8F3EF)
private val BrandAccent = Color(0xFF059669)

private val Amber50 = Color(0xFFFFFBEB)
private val Amber200 = Color(0xFFFDE68A)
private val Amber500 = Color(0xFFF59E0B)
private val Amber700 = Color(0xFFB45309)

private val Emerald50 = Color(0xFFECFDF5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)
private val Emerald800 = Color(0xFF065F46)

private val Blue50 = Color(0xFFEFF6FF)
private val Blue100 = Color(0xFFDBEAFE)
private val Blue900 = Color(0xFF1E3A8A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminReceivableCustomerDetailScreen(
    name: String,
    role: String,
    customerId: String,
    receivableRepository: ReceivableRepository,
    customerRepository: CustomerRepository? = null,
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
        factory = ReceivableCustomerDetailViewModel.factory(
            receivableRepository,
            customerId,
            customerRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaymentHistory by remember { mutableStateOf(false) }
    var showQuickBillSheet by remember { mutableStateOf(false) }

    if (uiState.isPayAllOpen) {
        val unpaidReceivables = uiState.receivables.filter { it.remainingAmount > BigDecimal.ZERO }
        ReceivableDedicatedPaymentScreen(
            customerName = uiState.customerName,
            unpaidReceivables = unpaidReceivables,
            selectedInvoiceIds = uiState.selectedPayAllInvoiceIds,
            paymentAmountInput = uiState.paymentAmountInput,
            paymentMethod = uiState.paymentMethod,
            referenceInput = uiState.referenceInput,
            notesInput = uiState.notesInput,
            isSubmittingPayment = uiState.isSubmittingPayment,
            errorMessage = uiState.paymentErrorMessage,
            onToggleInvoice = viewModel::togglePayAllInvoice,
            onSelectAllInvoices = viewModel::selectAllPayAllInvoices,
            onAmountChanged = viewModel::onPaymentAmountChanged,
            onMethodChanged = viewModel::onPaymentMethodChanged,
            onReferenceChanged = viewModel::onReferenceChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onBack = viewModel::closePayment,
            onSubmit = viewModel::submitPayment
        )
        return
    }

    val isAnyPopupOpen = showQuickBillSheet || showPaymentHistory
    val popupBlurRadius by animateDpAsState(
        targetValue = if (isAnyPopupOpen) 16.dp else 0.dp,
        label = "customer_detail_blur"
    )

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        containerColor = Color.White,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (popupBlurRadius > 0.dp) Modifier.blur(popupBlurRadius) else Modifier),
                color = Color.White.copy(alpha = 0.95f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Back Button & Screen Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Slate700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Detail Piutang",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                letterSpacing = (-0.2).sp
                            )
                        }

                        // Right: Sync / History icon with count badge
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { showPaymentHistory = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Riwayat Pembayaran",
                                    tint = Slate700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (uiState.payments.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                        .size(16.dp)
                                        .background(BrandGreen, CircleShape)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = uiState.payments.size.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 9.sp
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> ReceivableCustomerDetailSkeleton(modifier = Modifier.padding(innerPadding))
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Terjadi kesalahan saat memuat data.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Coba Lagi", color = Color.White)
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.White)
                        .then(if (popupBlurRadius > 0.dp) Modifier.blur(popupBlurRadius) else Modifier)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Customer Overview Card
                    CustomerSummaryCard(
                        uiState = uiState,
                        onPayAllClick = { viewModel.openPayAll() },
                        onQuickBillClick = { showQuickBillSheet = true }
                    )

                    // Invoices List Section
                    InvoiceListSection(
                        receivables = uiState.receivables,
                        onPayReceivable = { receivable -> viewModel.openPayment(receivable) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Modal: Quick Bill WhatsApp Bottom Sheet
                if (showQuickBillSheet) {
                    QuickBillCustomerBottomSheet(
                        customerId = customerId,
                        customerName = uiState.customerName,
                        customerPhone = uiState.customerPhone,
                        receivables = uiState.receivables,
                        onDismiss = { showQuickBillSheet = false }
                    )
                }

                // Modal: Payment History Bottom Sheet
                if (showPaymentHistory) {
                    ModalBottomSheet(
                        onDismissRequest = { showPaymentHistory = false },
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        PaymentHistoryContent(
                            payments = uiState.payments,
                            onClose = { showPaymentHistory = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerSummaryCard(
    uiState: ReceivableCustomerDetailUiState,
    onPayAllClick: () -> Unit,
    onQuickBillClick: () -> Unit
) {
    val allPaid = uiState.totalRemaining <= BigDecimal.ZERO

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Customer & Total Balance Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Blue50)
                            .border(1.dp, Blue100.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.customerName.take(1).uppercase(),
                            color = Blue900,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Name & Status
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = uiState.customerName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${uiState.receivables.size} Nota",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Slate500
                            )
                        }
                        Text(
                            text = if (allPaid) "Semua tagihan lunas" else "Belum lunas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Slate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Total Balance Remaining
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SISA PIUTANG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = uiState.totalRemaining.toRupiahText(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Card Divider
            HorizontalDivider(
                color = Slate200.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Summary Billing Details (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL TAGIHAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = uiState.totalAmount.toRupiahText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "SUDAH DIBAYAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = uiState.totalPaid.toRupiahText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Card Divider
            HorizontalDivider(
                color = Slate200.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Buttons: Bayar Semua & Tagih WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bayar Semua Button
                Button(
                    onClick = onPayAllClick,
                    enabled = !allPaid,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Slate200,
                        disabledContentColor = Slate400
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Bayar Semua",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Tagih WhatsApp Button
                OutlinedButton(
                    onClick = onQuickBillClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Emerald200.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Emerald800
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatsapp),
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tagih WhatsApp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald800
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceListSection(
    receivables: List<Receivable>,
    onPayReceivable: (Receivable) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar nota",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (receivables.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada nota piutang",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }
            } else {
                Column {
                    receivables.forEachIndexed { index, receivable ->
                        if (index > 0) {
                            HorizontalDivider(color = Slate100)
                        }
                        DetailReceivableCard(
                            receivable = receivable,
                            onPayClick = { onPayReceivable(receivable) }
                        )
                    }
                }
            }
        }

        DetailPaginationControls(totalCount = receivables.size)
    }
}

@Composable
private fun DetailReceivableCard(
    receivable: Receivable,
    onPayClick: () -> Unit
) {
    val isPaid = receivable.isPaid()
    val refNo = receivable.legacyInvoiceNumber?.takeIf(String::isNotBlank)
        ?: receivable.transactionId?.shortTransactionId()
        ?: receivable.source

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Note Header: Invoice ID and Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isPaid) Emerald500 else Amber500,
                            shape = CircleShape
                        )
                )
                Text(
                    text = refNo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.2).sp
                )
            }

            Text(
                text = if (isPaid) "Lunas" else "Aktif",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPaid) Emerald700 else Amber700
            )
        }

        // Note Breakdown (2x2 Grid)
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Sisa Piutang & Sudah Dibayar
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sisa Piutang",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = if (isPaid) "Rp 0" else receivable.remainingAmount.toRupiahText(),
                        fontSize = 14.sp,
                        fontWeight = if (isPaid) FontWeight.SemiBold else FontWeight.Bold,
                        color = if (isPaid) Slate400 else BrandGreen,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Sudah Dibayar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = receivable.paidAmount.toRupiahText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaid) Slate800 else Slate700,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Row 2: Total Tagihan & Jatuh Tempo
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Tagihan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = receivable.amount.toRupiahText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Jatuh Tempo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = receivable.dueDate.take(10),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Action Footer inside Note (unpaid only)
        if (!isPaid) {
            HorizontalDivider(
                color = Slate100,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum Lunas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Amber700
                )

                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Bayar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPaginationControls(totalCount: Int) {
    TbPagination(
        currentPage = 1,
        totalPages = 1,
        onPreviousPage = {},
        onNextPage = {},
        supportingText = "$totalCount transaksi",
        testTag = "receivable-detail-pagination",
    )
}

@Composable
private fun PaymentHistoryContent(payments: List<ReceivablePaymentHistory>, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Riwayat Pembayaran",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Surface(
                        color = BrandLight,
                        border = BorderStroke(1.dp, Emerald200),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${payments.size} transaksi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Daftar pembayaran yang telah diterima",
                    fontSize = 12.sp,
                    color = Slate500,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .background(Slate100, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(color = Slate100)

        // List
        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat pembayaran",
                    fontSize = 13.sp,
                    color = Slate400
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                payments.forEach { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (payment.isReversed) Color(0xFFEF4444) else Emerald500,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = payment.paymentNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "${payment.paymentDate.simpleDate()} • ${payment.method.uppercase()}",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(top = 4.dp, start = 14.dp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = payment.amount.toRupiahText(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (payment.isReversed) "DIBATALKAN" else "BERHASIL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (payment.isReversed) Color(0xFFEF4444) else Emerald700,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action
        HorizontalDivider(color = Slate100)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("Tutup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
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

                HorizontalDivider(color = Slate100)

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonBox(Modifier.width(78.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(100.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SkeletonBox(Modifier.width(78.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                        SkeletonBox(Modifier.width(100.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
                    }
                }

                HorizontalDivider(color = Slate100)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBox(Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(12.dp))
                    SkeletonBox(Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(12.dp))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(Modifier.width(90.dp).height(16.dp), shape = RoundedCornerShape(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    repeat(3) { index ->
                        if (index > 0) HorizontalDivider(color = Slate100)
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

private fun BigDecimal?.toRupiahText(): String {
    if (this == null) return "Rp 0"
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    format.maximumFractionDigits = 0
    return format.format(this).replace("Rp", "Rp ")
}

private fun Receivable.isPaid(): Boolean {
    return this.remainingAmount <= BigDecimal.ZERO
}
