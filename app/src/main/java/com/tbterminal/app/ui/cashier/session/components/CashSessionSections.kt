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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cash.displayDateTime
import com.tbterminal.app.ui.cash.money
import java.math.BigDecimal

private val SurfaceCard = Color.White
private val CardBorder = Color(0xFFE2E8F0).copy(alpha = 0.8f)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val BrandGreen = Color(0xFF1B5E3C)
private val BrandGreenLight = Color(0xFFF4F9F5)
private val BrandGreenTint = Color(0xFFE8F3EC)
private val Emerald500 = Color(0xFF10B981)
private val Emerald600 = Color(0xFF16A34A)
private val Emerald700 = Color(0xFF047857)
private val DangerRed = Color(0xFFDC2626)
private val DangerRedSoft = Color(0xFFFFF1F2)

@Composable
fun LocalCashSessionBadges(
    uiState: CashReconciliationUiState,
    modifier: Modifier = Modifier
) {
    if (!uiState.isUsingLocalActiveSession) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFF7ED))
                .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEA580C))
                )
                Text(
                    text = "Sesi Kas Offline Lokal (Belum Tersinkron Cloud)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC2410C)
                )
            }
        }
    }
}

/**
 * Card 1: Ringkasan Kas Laci
 * Sesuai mockup HTML: CardRingkasanKasLaci dengan toggle privacy, saldo estimasi laci,
 * 4-grid metrics (Modal awal, Penjualan tunai, Pengeluaran kas, Waktu buka),
 * dan tombol aksi cepat Catat Kas Keluar / Kas Masuk.
 */
@Composable
fun SessionSummarySection(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    isPrivacyHidden: Boolean = false,
    onTogglePrivacy: () -> Unit = {},
    onShowExpenseDialog: () -> Unit
) {
    val session = uiState.activeSession

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "RINGKASAN KAS LACI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.6.sp
                    )
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Sembunyikan/Tampilkan nominal",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Status Pill Shift
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Emerald600)
                        )
                        Text(
                            text = "Shift Berjalan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandGreen
                        )
                    }
                }
            }

            // Estimasi Saldo Laci Utama
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Estimasi Kas di Laci",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
                Text(
                    text = if (isPrivacyHidden) "••••••••••••" else (uiState.systemCash.money() ?: "Rp 0,00"),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.6).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tersinkron otomatis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald700
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // 4-Grid Detailed Metrics (2 Kolom)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Modal Awal Shift",
                        value = if (isPrivacyHidden) "••••••••" else (session?.openingCash?.money() ?: "Rp 0,00"),
                        valueColor = Slate800,
                        icon = Icons.Outlined.AccountBalanceWallet,
                        iconTint = Slate400
                    )
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Penjualan Tunai (+)",
                        value = if (isPrivacyHidden) "••••••••" else ("+ " + (uiState.cashSales.money() ?: "Rp 0,00")),
                        valueColor = Emerald700,
                        icon = Icons.Outlined.ArrowUpward,
                        iconTint = Emerald600
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Pengeluaran Kas (-)",
                        value = if (isPrivacyHidden) "••••••••" else ("- " + (session?.totalExpenses?.money() ?: "Rp 0,00")),
                        valueColor = DangerRed,
                        icon = Icons.Outlined.ArrowDownward,
                        iconTint = Color(0xFFF43F5E)
                    )
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Waktu Buka Shift",
                        value = session?.openedAt?.displayDateTime() ?: "-",
                        valueColor = Slate800,
                        icon = Icons.Outlined.Schedule,
                        iconTint = Slate400
                    )
                }
            }

            // Quick Action: Catat Kas Keluar / Kas Masuk
            Surface(
                onClick = onShowExpenseDialog,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catat Kas Keluar / Kas Masuk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                }
            }
        }
    }
}

/**
 * Card 2: Card Tutup Shift Kasir (Halaman Utama)
 * Sesuai mockup HTML: CardTutupShiftKasir yang membuka bottom sheet modal.
 */
@Composable
fun CloseShiftCard(
    modifier: Modifier = Modifier,
    onOpenModal: () -> Unit
) {
    Surface(
        onClick = onOpenModal,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Tutup Shift Kasir",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Selesaikan sesi dan rekonsiliasi kas",
                        fontSize = 12.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Surface(
                onClick = onOpenModal,
                shape = RoundedCornerShape(12.dp),
                color = BrandGreen,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tutup Shift",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet Modal: Tutup Shift Kasir (Pop Up Sesuai Mockup HTML)
 * Sesuai mockup HTML: ShiftClosureBottomSheetModal dengan hitung uang fisik laci,
 * tombol "Isi Sesuai Sistem", banner selisih 3 status (Sesuai, Kurang, Lebih),
 * catatan penutupan, tombol Konfirmasi & Tutup Sesi, Batal, dan banner informasi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftClosureBottomSheetModal(
    onDismiss: () -> Unit,
    uiState: CashReconciliationUiState,
    cashAmount: String,
    notes: String,
    onCashChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val parsedClosingCash = remember(cashAmount) {
        val clean = cashAmount.replace(".", "").replace(",", "").trim()
        clean.toBigDecimalOrNull()
    }

    val cashDifference = remember(parsedClosingCash, uiState.systemCash) {
        parsedClosingCash?.subtract(uiState.systemCash)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        scrimColor = Color(0xFF0F172A).copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Tutup Shift Kasir",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Hitung fisik uang laci & konfirmasi",
                            fontSize = 11.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Modal Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Physical Cash Input Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HITUNG UANG FISIK LACI (IDR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Isi Sesuai Sistem",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onCashChange(uiState.systemCash.toPlainString().substringBefore("."))
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Cash Count Input Field (Border 2.dp brand green, text 20.sp bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(
                            2.dp,
                            if (isError) DangerRed else BrandGreen,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rp",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isError) DangerRed else BrandGreen,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        BasicTextField(
                            value = cashAmount,
                            onValueChange = onCashChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (cashAmount.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate300
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        color = DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Cash Difference Alert Banner
                val diffVal = cashDifference ?: BigDecimal.ZERO
                val isBalanced = cashAmount.isNotBlank() && diffVal.compareTo(BigDecimal.ZERO) == 0
                val isShortage = cashAmount.isNotBlank() && diffVal < BigDecimal.ZERO
                val isExcess = cashAmount.isNotBlank() && diffVal > BigDecimal.ZERO

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isShortage -> Color(0xFFFFF1F2)
                                isExcess -> Color(0xFFEFF6FF)
                                else -> Color(0xFFECFDF5)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isShortage -> Color(0xFFFECDD3)
                                isExcess -> Color(0xFFBFDBFE)
                                else -> Color(0xFFA7F3D0).copy(alpha = 0.8f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isShortage -> Icons.Outlined.WarningAmber
                                    isExcess -> Icons.Outlined.Info
                                    else -> Icons.Outlined.CheckCircle
                                },
                                contentDescription = null,
                                tint = when {
                                    isShortage -> Color(0xFFE11D48)
                                    isExcess -> Color(0xFF2563EB)
                                    else -> Color(0xFF047857)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    isShortage -> "Selisih Kas: Kurang"
                                    isExcess -> "Selisih Kas: Berlebih"
                                    else -> "Saldo Fisik Sesuai"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isShortage -> Color(0xFF9F1239)
                                    isExcess -> Color(0xFF1E40AF)
                                    else -> Color(0xFF065F46)
                                }
                            )
                        }
                        Text(
                            text = when {
                                isShortage -> "- " + (diffVal.abs().money() ?: "Rp 0,00")
                                isExcess -> "+ " + (diffVal.money() ?: "Rp 0,00")
                                else -> "Rp 0,00"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isShortage -> Color(0xFFBE123C)
                                isExcess -> Color(0xFF1D4ED8)
                                else -> Color(0xFF065F46)
                            }
                        )
                    }
                }

                // Optional Closing Notes
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CATATAN PENUTUPAN (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text(
                                "Keterangan serah terima kas atau alasan jika ada selisih...",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            unfocusedBorderColor = Slate200,
                            focusedBorderColor = BrandGreen
                        )
                    )
                }

                // Primary CTA: Konfirmasi & Tutup Sesi
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting && cashAmount.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFA7F3D0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Konfirmasi & Tutup Sesi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // Cancel Button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp)
                    )
                }

                // Informational micro-banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECFDF5).copy(alpha = 0.4f))
                        .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 1.dp)
                        )
                        Text(
                            text = "Menutup sesi akan mengunci transaksi shift ini dan mencetak laporan rekapitulasi kas.",
                            fontSize = 11.sp,
                            color = Color(0xFF064E3B),
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card 3: Transaksi Sesi Ini
 * Sesuai mockup HTML: SectionTransaksiSesiIni dengan dot hijau, judul, tombol "Lihat Semua",
 * dan daftar transaksi format #TRX-... · PENJUALAN · + Rp50.000,00.
 */
@Composable
fun SessionTransactionsCard(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onTransactionHistoryClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Emerald600)
                    )
                    Text(
                        text = "Transaksi Sesi Ini",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onTransactionHistoryClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (uiState.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi pada shift kasir ini.",
                        fontSize = 11.sp,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.transactions.take(4).forEachIndexed { index, tx ->
                        if (index > 0) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                        }

                        val isIncome = tx.type != "EXPENSE"
                        val title = if (isIncome) "#TRX-${tx.receiptId}" else "Kas Keluar"
                        val subtitle = if (isIncome) tx.type.uppercase() else "Pengeluaran Operasional"
                        val timeStr = tx.createdAt.take(16).replace("T", " ")
                        val amountDisplay = (if (isIncome) "+ " else "- ") + (tx.total.money() ?: "Rp 0,00")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onTransactionHistoryClick)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Slate50)
                                        .border(1.dp, Slate200.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.AutoMirrored.Outlined.ReceiptLong else Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$timeStr · $subtitle",
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = amountDisplay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isIncome) Emerald700 else DangerRed
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Form Buka Sesi Kasir Baru saat belum ada shift aktif
 */
@Composable
fun OpenShiftForm(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGreenLight)
                        .border(1.dp, BrandGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Buka Shift Kasir Baru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Tentukan modal awal di laci kasir sebelum melayani transaksi.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            val isOpeningError = !uiState.hasActiveSession && uiState.errorMessage != null

            // Input Modal Awal
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MODAL AWAL LACI (IDR)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = uiState.openingCashInput,
                    onValueChange = onOpeningCashChanged,
                    placeholder = { Text("0", color = Slate400, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Text(
                            text = "Rp",
                            color = if (isOpeningError) DangerRed else BrandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    isError = isOpeningError,
                    supportingText = if (isOpeningError) {
                        { Text(uiState.errorMessage.orEmpty(), color = DangerRed, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = Slate200,
                        errorBorderColor = DangerRed,
                        errorContainerColor = DangerRedSoft
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                )
            }

            // Quick Chips Modal Awal
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PILIHAN CEPAT MODAL AWAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickOptions = listOf(
                        "100000" to "100 rb",
                        "200000" to "200 rb",
                        "300000" to "300 rb",
                        "500000" to "500 rb"
                    )
                    quickOptions.forEach { (value, label) ->
                        val isSelected = uiState.openingCashInput == value
                        Surface(
                            onClick = { onOpeningCashChanged(value) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandGreenTint else Slate50,
                            border = BorderStroke(1.dp, if (isSelected) BrandGreen else Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandGreen else Slate700,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Tombol Buka Sesi Kasir
            Button(
                onClick = onOpenSession,
                enabled = !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Sesi Kasir", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * CloseShiftSection dipertahankan untuk kompatibilitas jika dipanggil langsung
 */
@Composable
fun CloseShiftSection(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    cashAmount: String,
    notes: String,
    onCashChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    isError: Boolean = false,
    supportingText: String? = null
) {
    CloseShiftCard(
        modifier = modifier,
        onOpenModal = onSubmit
    )
}

@Composable
private fun CashMetricItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector,
    iconTint: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC).copy(alpha = 0.7f))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
