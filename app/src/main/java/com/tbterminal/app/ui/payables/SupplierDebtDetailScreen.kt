package com.tbterminal.app.ui.payables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

private val SurfaceBg = Color(0xFFF8FAF8)
private val Brand50 = Color(0xFFF2F7F4)
private val Brand100 = Color(0xFFE4EFEA)
private val Brand600 = Color(0xFF256B57)
private val Brand700 = Color(0xFF1E5E4E)
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

@Composable
internal fun AdminSupplierDebtDetailScreen(
    name: String,
    role: String,
    supplierId: String,
    purchasingRepository: PurchasingRepository,
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
    viewModel: SupplierDebtDetailViewModel = viewModel(
        factory = SupplierDebtDetailViewModel.factory(purchasingRepository, supplierId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SupplierDebts,
        showPageHeader = true,
        pageTitle = "Detail Hutang",
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
            uiState.isLoading -> {
                Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand600)
                }
            }
            uiState.errorMessage != null && uiState.payables.isEmpty() -> {
                Box(modifier = contentModifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(uiState.errorMessage ?: "Detail hutang gagal dimuat", color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::load) {
                            Text("Coba Lagi")
                        }
                    }
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
                    SupplierSummaryCard(uiState)
                    InvoiceListSection(
                        payables = uiState.payables,
                        onPayClick = viewModel::openPayment
                    )
                }
            }
        }
    }

    uiState.selectedPayable?.let { payable ->
        SupplierPaymentDialog(
            payable = payable,
            uiState = uiState.toSupplierDebtUiState(),
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
private fun SupplierSummaryCard(uiState: SupplierDebtDetailUiState) {
    val allPaid = uiState.totalRemaining <= java.math.BigDecimal.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Identity Row
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
                            text = uiState.supplierName.take(1).uppercase(),
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
                                text = uiState.supplierName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "${uiState.payables.size} Nota",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = if (allPaid) "Semua hutang lunas" else "Belum lunas",
                            fontSize = 11.sp,
                            color = if (allPaid) Brand600 else Slate400,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SISA HUTANG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = uiState.totalRemaining.currencyText(),
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
                    Text(
                        text = "TOTAL TAGIHAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = uiState.totalAmount.currencyText(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SUDAH DIBAYAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = uiState.totalPaid.currencyText(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand700,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceListSection(
    payables: List<SupplierPayable>,
    onPayClick: (SupplierPayable) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                payables.forEachIndexed { index, payable ->
                    if (index > 0) HorizontalDivider(color = BorderLight)
                    DetailPayableCard(payable = payable, onPayClick = onPayClick)
                }
            }
        }

        DetailPaginationRow(payables.size, payables.size)
    }
}

@Composable
private fun DetailPayableCard(
    payable: SupplierPayable,
    onPayClick: (SupplierPayable) -> Unit
) {
    val isPaid = payable.isPaid()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isPaid) onPayClick(payable) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = payable.purchaseId.shortId(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                fontFamily = FontFamily.Monospace
            )
            Surface(
                color = if (isPaid) Brand50 else Color(0xFFFFF7ED),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = if (isPaid) "LUNAS" else "BELUM LUNAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaid) Brand600 else Color(0xFFC2410C),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
        HorizontalDivider(color = BorderLight, modifier = Modifier.padding(bottom = 4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("SISA HUTANG", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(
                        text = payable.remainingAmount.currencyText(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brand700,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column {
                    Text("TOTAL TAGIHAN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(
                        text = payable.amount.currencyText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column {
                    Text("JATUH TEMPO", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(
                        text = payable.dueDate.simpleDate(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(
                        text = if (isPaid) "LUNAS" else "AKTIF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) Emerald800 else Brand600,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SUDAH DIBAYAR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate400, letterSpacing = 0.5.sp)
                    Text(
                        text = payable.paidAmount.currencyText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
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
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                Text("1 / 1", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun SupplierPayable.isPaid(): Boolean = remainingAmount <= java.math.BigDecimal.ZERO
