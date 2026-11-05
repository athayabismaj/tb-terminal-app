package com.tbterminal.app.ui.cashier.session.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cash.displayDateTime
import com.tbterminal.app.ui.cash.money
import java.math.BigDecimal

private val SurfaceCard = Color.White
private val CardBorder = Color(0xFFE2E8F0)
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
private val BrandGreenDark = Color(0xFF13462C)
private val BrandGreenLight = Color(0xFFF4F9F5)
private val BrandGreenTint = Color(0xFFE8F3EC)
private val Emerald500 = Color(0xFF10B981)
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
            // Header Kartu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "RINGKASAN KAS LACI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.8.sp
                    )
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle privacy",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Emerald500)
                        )
                        Text(
                            text = "Shift Berjalan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald700
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
                    text = if (isPrivacyHidden) "••••••••" else (uiState.systemCash.money() ?: "Rp 0"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Sinkronisasi saldo kas sistem real-time",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald700
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Grid Rincian Kas (2 Kolom Bersih)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Modal Awal Shift",
                        value = if (isPrivacyHidden) "••••••••" else (session?.openingCash?.money() ?: "Rp 0"),
                        valueColor = Slate800,
                        icon = Icons.Outlined.AccountBalanceWallet,
                        iconTint = Slate600
                    )
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Penjualan Tunai (+)",
                        value = if (isPrivacyHidden) "••••••••" else ("+ " + (uiState.cashSales.money() ?: "Rp 0")),
                        valueColor = Emerald700,
                        icon = Icons.Outlined.ArrowUpward,
                        iconTint = Emerald700
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Pengeluaran Kas (-)",
                        value = if (isPrivacyHidden) "••••••••" else ("- " + (session?.totalExpenses?.money() ?: "Rp 0")),
                        valueColor = DangerRed,
                        icon = Icons.Outlined.ArrowDownward,
                        iconTint = DangerRed
                    )
                    CashMetricItem(
                        modifier = Modifier.weight(1f),
                        label = "Waktu Buka Shift",
                        value = session?.openedAt?.displayDateTime() ?: "-",
                        valueColor = Slate800,
                        icon = Icons.Outlined.Schedule,
                        iconTint = Slate600
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Tombol Catat Kas Keluar / Masuk
            Surface(
                onClick = onShowExpenseDialog,
                shape = RoundedCornerShape(12.dp),
                color = Slate50,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
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
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }
            }
        }
    }
}

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
    val parsedClosingCash = remember(cashAmount) {
        val clean = cashAmount.replace(".", "").replace(",", "").trim()
        clean.toBigDecimalOrNull()
    }

    val cashDifference = remember(parsedClosingCash, uiState.systemCash) {
        parsedClosingCash?.subtract(uiState.systemCash)
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandGreenLight)
                        .border(1.dp, BrandGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Tutup Shift Kasir",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Hitung uang fisik di laci dan konfirmasi penutupan sesi.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Input Uang Fisik Laci
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HITUNG UANG FISIK LACI (IDR)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    // Quick Action: Isi Sesuai Saldo Sistem
                    Text(
                        text = "Isi Sesuai Sistem",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onCashChange(uiState.systemCash.toPlainString().substringBefore("."))
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                OutlinedTextField(
                    value = cashAmount,
                    onValueChange = onCashChange,
                    placeholder = {
                        Text("0", color = Slate400, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    leadingIcon = {
                        Text(
                            text = "Rp",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isError) DangerRed else BrandGreen,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    isError = isError,
                    supportingText = supportingText?.let {
                        { Text(it, color = DangerRed, fontSize = 11.sp) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate200,
                        focusedBorderColor = BrandGreen,
                        errorBorderColor = DangerRed,
                        errorContainerColor = DangerRedSoft
                    ),
                    singleLine = true
                )
            }

            // Real-time Selisih Kas Status Badge
            if (cashDifference != null) {
                val diffLong = cashDifference.toLong()
                val isBalanced = diffLong == 0L
                val isShortage = diffLong < 0L

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isBalanced -> Color(0xFFECFDF5)
                                isShortage -> Color(0xFFFFF1F2)
                                else -> Color(0xFFEFF6FF)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isBalanced -> Color(0xFFA7F3D0)
                                isShortage -> Color(0xFFFECDD3)
                                else -> Color(0xFFBFDBFE)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isBalanced -> Icons.Outlined.CheckCircle
                                    isShortage -> Icons.Outlined.WarningAmber
                                    else -> Icons.Outlined.Info
                                },
                                contentDescription = null,
                                tint = when {
                                    isBalanced -> Emerald700
                                    isShortage -> DangerRed
                                    else -> Color(0xFF1D4ED8)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    isBalanced -> "Selisih Kas: Pas / Seimbang"
                                    isShortage -> "Selisih Kas: Kurang"
                                    else -> "Selisih Kas: Lebih"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isBalanced -> Emerald700
                                    isShortage -> DangerRed
                                    else -> Color(0xFF1D4ED8)
                                }
                            )
                        }
                        Text(
                            text = when {
                                isBalanced -> "Rp 0"
                                isShortage -> "- " + (cashDifference.abs().money() ?: "Rp 0")
                                else -> "+ " + (cashDifference.money() ?: "Rp 0")
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isBalanced -> Emerald700
                                isShortage -> DangerRed
                                else -> Color(0xFF1D4ED8)
                            }
                        )
                    }
                }
            }

            // Catatan Penutupan Shift
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATATAN PENUTUPAN (OPSIONAL)",
                    fontSize = 10.sp,
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
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
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

            // Tombol Konfirmasi & Tutup Sesi
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && cashAmount.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Konfirmasi & Tutup Sesi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Menutup sesi akan mengunci transaksi shift ini dan mencetak laporan rekapitulasi kas.",
                    fontSize = 10.sp,
                    color = Emerald700,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Emerald500)
                    )
                    Text(
                        text = "Transaksi Sesi Ini",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onTransactionHistoryClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Lihat Semua",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
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
                    uiState.transactions.take(3).forEachIndexed { index, tx ->
                        if (index > 0) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                        }

                        val isIncome = tx.type != "EXPENSE"
                        val title = if (isIncome) "#${tx.receiptId}" else "Kas Keluar"
                        val subtitle = if (isIncome) tx.type.uppercase() else "Pengeluaran Operasional"
                        val timeStr = tx.createdAt.take(16).replace("T", " ")
                        val amountDisplay = (if (isIncome) "+ " else "- ") + (tx.total.money() ?: "Rp 0")

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
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isIncome) BrandGreenLight else DangerRedSoft)
                                        .border(
                                            1.dp,
                                            if (isIncome) BrandGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.AutoMirrored.Outlined.ReceiptLong else Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = if (isIncome) BrandGreen else DangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$timeStr · $subtitle",
                                        fontSize = 10.sp,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = amountDisplay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) Emerald700 else DangerRed
                            )
                        }
                    }
                }
            }
        }
    }
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
            .clip(RoundedCornerShape(10.dp))
            .background(Slate50)
            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
