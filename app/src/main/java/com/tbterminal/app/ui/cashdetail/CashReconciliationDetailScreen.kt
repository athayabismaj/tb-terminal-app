package com.tbterminal.app.ui.cashdetail

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbPagination
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.ui.cash.ShiftSettlementDialog
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailBackground = Color(0xFFF4FAFD)
private val DetailBorder = Color(0xFFE2E8F0)
private val DetailText = Color(0xFF0F172A)
private val DetailMuted = Color(0xFF64748B)
private val DetailPrimary = Color(0xFF059669)

@Composable
internal fun CashReconciliationDetailScreen(
    modifier: Modifier,
    uiState: CashReconciliationDetailUiState,
    onOpenHistory: () -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenCloseDialog: () -> Unit = {},
    onDismissCloseDialog: () -> Unit = {},
    onConfirmForceClose: (BigDecimal, String?) -> Unit = { _, _ -> }
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    var showSettlementDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().background(DetailBackground)
            .verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 32.dp)
    ) {
        DetailHeader(
            onOpenHistory = onOpenHistory,
            onOpenSettlement = { showSettlementDialog = true },
            hasSession = uiState.session != null,
            compact = compact
        )

        uiState.closeSuccessMessage?.let { successMsg ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = Color(0xFFDCFCE7),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DetailPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text(successMsg, color = Color(0xFF166534), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        when {
            uiState.sessionId.isBlank() -> SelectSessionPrompt(onOpenHistory)
            uiState.isLoading -> CashReconciliationDetailSkeleton(compact)
            uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onRetry)
            uiState.session != null -> DetailContent(
                uiState = uiState,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                onOpenCloseDialog = onOpenCloseDialog,
                compact = compact
            )
        }
    }

    if (uiState.showCloseDialog && uiState.session != null) {
        SupervisorCloseSessionDialog(
            session = uiState.session,
            isClosing = uiState.isClosingSession,
            errorMessage = uiState.closeErrorMessage,
            onDismiss = onDismissCloseDialog,
            onConfirm = onConfirmForceClose
        )
    }

    if (showSettlementDialog && uiState.session != null) {
        ShiftSettlementDialog(
            session = uiState.session,
            onDismiss = { showSettlementDialog = false }
        )
    }
}

@Composable
private fun DetailHeader(
    onOpenHistory: () -> Unit,
    onOpenSettlement: () -> Unit,
    hasSession: Boolean,
    compact: Boolean
) {
    if (compact) Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Detail Kas", color = DetailText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f).height(48.dp)) { Text("Riwayat") }
            if (hasSession) {
                Button(
                    onClick = onOpenSettlement,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                ) {
                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Slip Shift")
                }
            }
        }
    } else Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Detail Rekonsiliasi Kas", color = DetailText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Audit kas sistem, kas fisik, pengeluaran, dan transaksi pada satu shift.", color = DetailMuted, fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasSession) {
                Button(
                    onClick = onOpenSettlement,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                ) {
                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Slip Tutup Shift")
                }
            }
            OutlinedButton(onClick = onOpenHistory) { Text("Riwayat Kas Harian") }
        }
    }
}

@Composable
private fun DetailContent(
    uiState: CashReconciliationDetailUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenCloseDialog: () -> Unit,
    compact: Boolean
) {
    val session = requireNotNull(uiState.session)
    SessionSummary(session, onOpenCloseDialog, compact)
    Spacer(Modifier.height(16.dp))
    TransactionSection(uiState, onPreviousPage, onNextPage, compact)
    Spacer(Modifier.height(16.dp))
    ExpenseSection(uiState.expenses, compact)
}

@Composable
private fun SessionSummary(
    session: CashSession,
    onOpenCloseDialog: () -> Unit,
    compact: Boolean
) {
    val isOpen = session.status.equals("OPEN", true)

    if (isOpen) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (compact) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sesi Kasir Masih Terbuka",
                            color = Color(0xFF92400E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Sesi kas ini sedang aktif/belum ditutup oleh ${session.userName ?: "kasir"}. Admin dan Owner dapat menutup sesi secara manual.",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = onOpenCloseDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Tutup Sesi (Force Close)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text(
                                "Sesi Kasir Masih Terbuka",
                                color = Color(0xFF92400E),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Sesi kas ini belum ditutup oleh ${session.userName ?: "kasir"}. Anda dapat menutup sesi ini sebagai supervisor.",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Button(
                        onClick = onOpenCloseDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Tutup Sesi (Force Close)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, DetailBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        session.userName ?: "Kasir",
                        color = DetailText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        "${session.openedAt.asDateTime()} - ${session.closedAt?.asDateTime() ?: "Sesi masih aktif"}",
                        color = DetailMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                StatusBadge(session.status)
            }
            Spacer(Modifier.height(18.dp))
            if (compact) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryValue("MODAL AWAL", session.openingCash.asCurrency(), Modifier.fillMaxWidth())
                SummaryValue("KAS SISTEM", (session.systemCash ?: session.openingCash).asCurrency(), Modifier.fillMaxWidth())
                SummaryValue("KAS FISIK", session.closingCash?.asCurrency() ?: "-", Modifier.fillMaxWidth())
                SummaryValue("PENGELUARAN", session.totalExpenses.asCurrency(), Modifier.fillMaxWidth())
                SummaryValue("SELISIH", session.difference?.asCurrency() ?: "-", Modifier.fillMaxWidth())
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryValue("MODAL AWAL", session.openingCash.asCurrency(), Modifier.weight(1f))
                SummaryValue("KAS SISTEM", (session.systemCash ?: session.openingCash).asCurrency(), Modifier.weight(1f))
                SummaryValue("KAS FISIK", session.closingCash?.asCurrency() ?: "-", Modifier.weight(1f))
                SummaryValue("PENGELUARAN", session.totalExpenses.asCurrency(), Modifier.weight(1f))
                SummaryValue("SELISIH", session.difference?.asCurrency() ?: "-", Modifier.weight(1f))
            }
            if (!session.notes.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text("Catatan: ${session.notes}", color = DetailMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SummaryValue(title: String, value: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(12.dp)) {
        Column {
            Text(title, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(value, color = DetailText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionSection(uiState: CashReconciliationDetailUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit, compact: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, DetailBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            SectionTitle("Transaksi Dalam Sesi", "${uiState.totalTransactions} transaksi")
            if (!compact) TransactionHeader()
            if (uiState.transactions.isEmpty()) {
                EmptyRow("Belum ada transaksi pada sesi ini.")
            } else {
                uiState.transactions.forEach { if (compact) TransactionMobileRow(it) else TransactionRow(it) }
            }
            TbPagination(
                currentPage = uiState.page,
                totalPages = uiState.totalPages,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                modifier = Modifier.padding(14.dp),
                supportingText = "${uiState.transactions.size} dari ${uiState.totalTransactions} transaksi",
                isLoading = uiState.isLoading,
                testTag = "cash-detail-pagination",
            )
        }
    }
}

@Composable
private fun TransactionHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 11.dp)) {
        Label("NO TRANSAKSI", Modifier.weight(1.5f))
        Label("CUSTOMER", Modifier.weight(1.4f))
        Label("TANGGAL", Modifier.weight(1.5f))
        Label("TOTAL", Modifier.weight(1f))
        Label("STATUS", Modifier.weight(0.8f))
    }
}

@Composable
private fun TransactionRow(item: CashTransaction) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(item.receiptId, Modifier.weight(1.5f), color = DetailPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(item.customerName ?: "Umum", Modifier.weight(1.4f), color = DetailText, fontSize = 12.sp)
        Text(item.createdAt.asDateTime(), Modifier.weight(1.5f), color = DetailMuted, fontSize = 12.sp)
        Text(item.total.asCurrency(), Modifier.weight(1f), color = DetailText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.8f)) { StatusBadge(item.status) }
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun TransactionMobileRow(item: CashTransaction) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(item.receiptId, color = DetailPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            StatusBadge(item.status)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.customerName ?: "Pelanggan umum", color = DetailText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(item.createdAt.asDateTime(), color = DetailMuted, fontSize = 11.sp)
            }
            Text(item.total.asCurrency(), color = DetailText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun ExpenseSection(expenses: List<CashExpense>, compact: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, DetailBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            SectionTitle("Pengeluaran Kas", "${expenses.size} catatan")
            if (!compact) Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 11.dp)) {
                Label("DESKRIPSI", Modifier.weight(2f))
                Label("WAKTU", Modifier.weight(1.4f))
                Label("NOMINAL", Modifier.weight(1f))
            }
            if (expenses.isEmpty()) {
                EmptyRow("Tidak ada pengeluaran pada sesi ini.")
            } else {
                expenses.forEach { if (compact) ExpenseMobileRow(it) else ExpenseRow(it) }
            }
        }
    }
}

@Composable
private fun ExpenseRow(item: CashExpense) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp)) {
        Text(item.description, Modifier.weight(2f), color = DetailText, fontSize = 12.sp)
        Text(item.createdAt.asDateTime(), Modifier.weight(1.4f), color = DetailMuted, fontSize = 12.sp)
        Text(item.amount.asCurrency(), Modifier.weight(1f), color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun ExpenseMobileRow(item: CashExpense) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.description, color = DetailText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(item.createdAt.asDateTime(), color = DetailMuted, fontSize = 11.sp)
        }
        Text(item.amount.asCurrency(), color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = DetailText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = DetailMuted, fontSize = 12.sp)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val isClosedOrPaid = status.equals("CLOSED", true) || status.equals("lunas", true)
    val tint = if (isClosedOrPaid) DetailPrimary else Color(0xFFF59E0B)
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Text(status.replace('_', ' ').uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectSessionPrompt(onOpenHistory: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(240.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Pilih sesi kas dari halaman Riwayat Kas Harian.", color = DetailMuted)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenHistory) { Text("Buka Riwayat Kas") }
    }
}

@Composable
private fun CashReconciliationDetailSkeleton(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("cash-detail-skeleton"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(Color.White),
            border = BorderStroke(1.dp, DetailBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBox(Modifier.width(130.dp).height(18.dp), shape = RoundedCornerShape(4.dp))
                        SkeletonBox(Modifier.width(190.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                    }
                    SkeletonBox(Modifier.width(65.dp).height(24.dp), shape = RoundedCornerShape(6.dp))
                }
                Spacer(Modifier.height(18.dp))
                if (compact) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) {
                            Box(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SkeletonBox(Modifier.width(70.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                                    SkeletonBox(Modifier.width(110.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(5) {
                            Box(Modifier.weight(1f).background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SkeletonBox(Modifier.width(70.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                                    SkeletonBox(Modifier.width(100.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(Color.White),
            border = BorderStroke(1.dp, DetailBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(Modifier.width(140.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.width(70.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                }
                HorizontalDivider(color = DetailBorder)
                if (!compact) {
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 11.dp)) {
                        Label("NO TRANSAKSI", Modifier.weight(1.5f))
                        Label("CUSTOMER", Modifier.weight(1.4f))
                        Label("TANGGAL", Modifier.weight(1.5f))
                        Label("TOTAL", Modifier.weight(1f))
                        Label("STATUS", Modifier.weight(0.8f))
                    }
                }
                repeat(4) { index ->
                    if (index > 0) HorizontalDivider(color = DetailBorder)
                    if (compact) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                SkeletonBox(Modifier.width(90.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                                SkeletonBox(Modifier.width(55.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SkeletonBox(Modifier.width(110.dp).height(12.dp), shape = RoundedCornerShape(3.dp))
                                SkeletonBox(Modifier.width(80.dp).height(13.dp), shape = RoundedCornerShape(4.dp))
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(Modifier.weight(1.5f).height(13.dp), shape = RoundedCornerShape(4.dp))
                            Spacer(Modifier.width(12.dp))
                            SkeletonBox(Modifier.weight(1.4f).height(13.dp), shape = RoundedCornerShape(4.dp))
                            Spacer(Modifier.width(12.dp))
                            SkeletonBox(Modifier.weight(1.5f).height(13.dp), shape = RoundedCornerShape(4.dp))
                            Spacer(Modifier.width(12.dp))
                            SkeletonBox(Modifier.weight(1f).height(13.dp), shape = RoundedCornerShape(4.dp))
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.weight(0.8f)) {
                                SkeletonBox(Modifier.width(55.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    com.tbterminal.app.ui.components.AppErrorState(
        message = message,
        onRetry = onRetry,
        modifier = Modifier.height(240.dp),
    )
}

@Composable
private fun EmptyRow(message: String) = Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text(message, color = DetailMuted) }

@Composable
private fun Label(text: String, modifier: Modifier) = Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.asDateTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)

@Composable
private fun SupervisorCloseSessionDialog(
    session: CashSession,
    isClosing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, String?) -> Unit
) {
    var cashInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val systemCash = session.systemCash ?: session.openingCash

    AlertDialog(
        onDismissRequest = {
            if (!isClosing) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                Spacer(Modifier.width(8.dp))
                Text("Tutup Sesi (Force Close)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Anda akan menutup paksa sesi kasir untuk ${session.userName ?: "Kasir"}. Sesi ini akan ditandai CLOSED.",
                    fontSize = 13.sp,
                    color = DetailText
                )

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("KAS SISTEM SAAT INI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DetailMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(systemCash.asCurrency(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DetailText)
                    }
                }

                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        cashInput = filtered
                        localError = null
                    },
                    label = { Text("Jumlah Kas Fisik (Rp)") },
                    placeholder = { Text("Contoh: 1500000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isClosing,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Catatan Penutupan Supervisor (Opsional)") },
                    placeholder = { Text("Contoh: Ditutup oleh admin karena shift selesai") },
                    maxLines = 3,
                    enabled = !isClosing,
                    modifier = Modifier.fillMaxWidth()
                )

                val displayError = localError ?: errorMessage
                if (!displayError.isNullOrBlank()) {
                    Text(
                        displayError,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val raw = cashInput.trim()
                    if (raw.isBlank()) {
                        localError = "Masukkan jumlah uang kas fisik."
                        return@Button
                    }
                    val amount = raw.toBigDecimalOrNull()
                    if (amount == null || amount < BigDecimal.ZERO) {
                        localError = "Nominal kas fisik tidak valid."
                        return@Button
                    }
                    onConfirm(amount, notesInput.trim().ifBlank { null })
                },
                enabled = !isClosing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White
                )
            ) {
                if (isClosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Menutup...")
                } else {
                    Text("Konfirmasi Tutup Sesi")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClosing
            ) {
                Text("Batal")
            }
        }
    )
}

