package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS TOKENS (TB Terminal Design System)
// ═════════════════════════════════════════════════════════════════════════════
private val BrandGreen = Color(0xFF10533C)
private val BrandGreenLight = Color(0xFFF0FDF4)
private val BrandGreenBorder = Color(0xFFBBF7D0)
private val EmeraldText = Color(0xFF047857)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private fun formatRupiah(amount: BigDecimal?): String {
    val safe = amount ?: BigDecimal.ZERO
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp ${formatter.format(safe)}"
}

private fun formatTimestamp(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "-"
    return runCatching {
        val dt = OffsetDateTime.parse(isoDate)
        dt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrElse {
        isoDate.takeLast(8).take(5)
    }
}

private fun formatFullDateTime(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "-"
    return runCatching {
        val dt = OffsetDateTime.parse(isoDate)
        dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrDefault(isoDate)
}

/**
 * 1. RINGKASAN SHIFT SATU BARIS (Keputusan Desain 7)
 * Menampilkan: Omzet | Transaksi | Kas Seharusnya dalam 1 baris terpadu.
 * Dapat diketuk untuk membuka rincian per metode pembayaran.
 */
@Composable
fun ShiftSingleLineSummaryCard(
    omzet: BigDecimal,
    transactionCount: Long,
    expectedCash: BigDecimal,
    isPrivacyHidden: Boolean = false,
    onTogglePrivacy: () -> Unit = {},
    onClickDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClickDetail,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header bar kecil: Shift Berjalan + toggle privacy + hint ketuk
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldText)
                    )
                    Text(
                        text = "SHIFT BERJALAN",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldText,
                        letterSpacing = 0.6.sp
                    )
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Sembunyikan / Tampilkan angka",
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Rincian metode",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Baris Angka Tunggal: OMZET | TRANSAKSI | KAS SEHARUSNYA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Omzet
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "OMZET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••" else formatRupiah(omzet),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .height(30.dp)
                        .background(Slate200)
                )

                // 2. Jumlah Transaksi
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TRANSAKSI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "$transactionCount Trx",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .height(30.dp)
                        .background(Slate200)
                )

                // 3. Kas Seharusnya
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "KAS SEHARUSNYA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••" else formatRupiah(expectedCash),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. UTILITY ROW (Kas Masuk/Keluar, Cetak Ulang Struk, Tutup Shift)
 * 3 tombol ergonomis berdampingan dengan touch target >= 48dp.
 */
@Composable
fun ShiftUtilityRow(
    onKasMasukKeluarClick: () -> Unit,
    onCetakUlangClick: () -> Unit,
    onTutupShiftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Kas Masuk / Keluar
        Surface(
            onClick = onKasMasukKeluarClick,
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 0.5.dp,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kas In/Out",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                    maxLines = 1
                )
            }
        }

        // 2. Cetak Ulang Struk
        Surface(
            onClick = onCetakUlangClick,
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 0.5.dp,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cetak Ulang",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                    maxLines = 1
                )
            }
        }

        // 3. Tutup Shift
        Surface(
            onClick = onTutupShiftClick,
            shape = RoundedCornerShape(14.dp),
            color = BrandGreen,
            border = BorderStroke(1.dp, BrandGreen),
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LockClock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tutup Shift",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 3. BOTTOM SHEET: Rincian Metode Pembayaran & Arus Kas Laci
 * Dibuka saat ringkasan shift satu baris diketuk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftPaymentBreakdownBottomSheet(
    uiState: CashReconciliationUiState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val session = uiState.activeSession

    // Breakdown per metode
    val tunaiTransactions = remember(uiState.transactions) {
        uiState.transactions.filter { tx -> tx.paymentMethods.any { it.contains("TUNAI", ignoreCase = true) } }
    }
    val qrisTransactions = remember(uiState.transactions) {
        uiState.transactions.filter { tx -> tx.paymentMethods.any { it.contains("QRIS", ignoreCase = true) } }
    }
    val transferTransactions = remember(uiState.transactions) {
        uiState.transactions.filter { tx -> tx.paymentMethods.any { it.contains("TRANSFER", ignoreCase = true) } }
    }
    val tempoTransactions = remember(uiState.transactions) {
        uiState.transactions.filter { tx -> tx.paymentMethods.any { it.contains("HUTANG", ignoreCase = true) || it.contains("TEMPO", ignoreCase = true) } }
    }

    val tunaiTotal = remember(tunaiTransactions, uiState.cashSales) {
        if (tunaiTransactions.isNotEmpty()) tunaiTransactions.sumOf { it.paidAmount.min(it.total) } else uiState.cashSales
    }
    val qrisTotal = remember(qrisTransactions) { qrisTransactions.sumOf { it.total } }
    val transferTotal = remember(transferTransactions) { transferTransactions.sumOf { it.total } }
    val tempoTotal = remember(tempoTransactions) { tempoTransactions.sumOf { it.total } }

    val totalOmzet = remember(tunaiTotal, qrisTotal, transferTotal, tempoTotal) {
        tunaiTotal.add(qrisTotal).add(transferTotal).add(tempoTotal)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Slate200)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rincian Shift & Pembayaran",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Mulai: ${formatFullDateTime(session?.openedAt)}",
                        fontSize = 11.5.sp,
                        color = Slate500
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // Section 1: Rincian Omzet per Metode Bayar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "METODE PEMBAYARAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentMethodRow(
                            icon = Icons.Outlined.Payments,
                            title = "Tunai (Cash)",
                            count = tunaiTransactions.size,
                            amount = tunaiTotal,
                            accentColor = EmeraldText
                        )
                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f))
                        PaymentMethodRow(
                            icon = Icons.Outlined.QrCode2,
                            title = "QRIS",
                            count = qrisTransactions.size,
                            amount = qrisTotal,
                            accentColor = Color(0xFF0284C7)
                        )
                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f))
                        PaymentMethodRow(
                            icon = Icons.Outlined.AccountBalance,
                            title = "Transfer Bank",
                            count = transferTransactions.size,
                            amount = transferTotal,
                            accentColor = Color(0xFF7C3AED)
                        )
                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f))
                        PaymentMethodRow(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "Tempo / Hutang",
                            count = tempoTransactions.size,
                            amount = tempoTotal,
                            accentColor = Color(0xFFD97706)
                        )

                        HorizontalDivider(color = Slate200)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Omzet Penjualan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = formatRupiah(totalOmzet),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldText
                            )
                        }
                    }
                }
            }

            // Section 2: Arus Kas Fisik Laci
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ARUS KAS LACI (CASH FLOW)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CashFlowRow("Modal Awal Kas", formatRupiah(session?.openingCash), Slate700)
                        CashFlowRow("Penjualan Tunai", "+ ${formatRupiah(uiState.cashSales)}", EmeraldText)
                        val totalIn = session?.totalCashIn ?: uiState.totalCashIn
                        if (totalIn > BigDecimal.ZERO) {
                            CashFlowRow("Kas Masuk (+)", "+ ${formatRupiah(totalIn)}", EmeraldText)
                        }
                        val totalOut = session?.totalExpenses ?: uiState.totalCashOut
                        if (totalOut > BigDecimal.ZERO) {
                            CashFlowRow("Kas Keluar (-)", "- ${formatRupiah(totalOut)}", Color(0xFFDC2626))
                        }
                        HorizontalDivider(color = Slate200)
                        CashFlowRow(
                            label = "Estimasi Kas Seharusnya di Laci",
                            value = formatRupiah(uiState.systemCash),
                            color = BrandGreen,
                            isBold = true
                        )
                    }
                }
            }

            // Tombol Tutup Sheet
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Tutup", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    icon: ImageVector,
    title: String,
    count: Int,
    amount: BigDecimal,
    accentColor: Color
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800
                )
                Text(
                    text = "$count transaksi",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
        Text(
            text = formatRupiah(amount),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (amount > BigDecimal.ZERO) Slate900 else Slate400
        )
    }
}

@Composable
private fun CashFlowRow(
    label: String,
    value: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isBold) 13.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = Slate700
        )
        Text(
            text = value,
            fontSize = if (isBold) 14.sp else 12.5.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * 4. SECTION AKTIVITAS TRANSAKSI SHIFT
 * Daftar transaksi rapi tanpa kartu bertumpuk tebal.
 */
@Composable
fun ShiftRecentTransactionsSection(
    transactions: List<CashTransaction>,
    totalTransactions: Long,
    onTransactionHistoryClick: () -> Unit,
    onReprintTransaction: (CashTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aktivitas Shift Ini",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "$totalTransactions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onTransactionHistoryClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Lihat Semua",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandGreen
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            // Empty State
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Slate100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Belum Ada Transaksi di Shift Ini",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Text(
                        text = "Transaksi yang selesai dari kasir POS akan otomatis tercatat di sini.",
                        fontSize = 11.5.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // List Transaksi Terbaru
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                transactions.take(5).forEach { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        onReprintClick = { onReprintTransaction(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItemRow(
    transaction: CashTransaction,
    onReprintClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "#${transaction.receiptId.ifBlank { transaction.id.take(8) }}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "• ${formatTimestamp(transaction.createdAt)}",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.customerName ?: "Pelanggan Umum",
                        fontSize = 11.5.sp,
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PaymentMethodBadge(methods = transaction.paymentMethods)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatRupiah(transaction.total),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                IconButton(
                    onClick = onReprintClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Print,
                        contentDescription = "Cetak struk",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodBadge(methods: List<String>) {
    val method = methods.firstOrNull()?.uppercase() ?: "TUNAI"
    val (bgColor, textColor, label) = when {
        method.contains("TUNAI") -> Triple(Color(0xFFECFDF5), Color(0xFF047857), "Tunai")
        method.contains("QRIS") -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "QRIS")
        method.contains("TRANSFER") -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), "Transfer")
        method.contains("HUTANG") || method.contains("TEMPO") -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Tempo")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), method)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * 5. MODAL DIALOG: Cetak Ulang Struk & Slip Shift
 */
@Composable
fun ShiftReprintChooserDialog(
    hasActiveShift: Boolean,
    latestTransaction: CashTransaction?,
    onPrintShiftSlip: () -> Unit,
    onOpenTransactionHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Cetak Ulang Dokumen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilih dokumen yang ingin Anda cetak ulang ke printer thermal:",
                    fontSize = 12.5.sp,
                    color = Slate600
                )

                // Opsi 1: Cetak Slip Rekap Shift Berjalan
                if (hasActiveShift) {
                    Surface(
                        onClick = {
                            onDismiss()
                            onPrintShiftSlip()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Cetak Slip Shift Berjalan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                                Text(
                                    text = "Rekapitulasi omzet dan arus kas shift saat ini",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }

                // Opsi 2: Buka Riwayat Struk Transaksi
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenTransactionHistory()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Paid,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Buka Riwayat Struk",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = "Pilih struk transaksi spesifik untuk dicetak ulang",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Slate600, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}
