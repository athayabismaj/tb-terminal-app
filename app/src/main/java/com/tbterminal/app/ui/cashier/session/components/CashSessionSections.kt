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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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

private val BrandGreen = Color(0xFF10533C)       // #10533c
private val BrandGreenDark = Color(0xFF0B3828)   // #0b3828
private val BrandGreenLight = Color(0xFFF0FDF4)  // #f0fdf4
private val BrandGreenTint = Color(0xFFDCFCE7)   // #dcfce7
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald100 = Color(0xFFA7F3D0)
private val Emerald600 = Color(0xFF16A34A)
private val Emerald700 = Color(0xFF15803D)
private val Emerald800 = Color(0xFF166534)
private val DangerRed = Color(0xFFDC2626)
private val DangerRedSoft = Color(0xFFFFF1F2)

private fun BigDecimal?.cleanMoney(): String {
    if (this == null) return "Rp 0"
    val formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(this)
    return "Rp $formatted"
}

private fun String?.formatSessionDate(): String {
    if (isNullOrBlank()) return "-"
    return runCatching {
        val dt = OffsetDateTime.parse(this)
        dt.format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrDefault(this)
}

private fun String?.formatTransactionFullDate(): String {
    if (isNullOrBlank()) return "-"
    return runCatching {
        val dt = OffsetDateTime.parse(this)
        dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrDefault(this)
}

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
 * Sesuai mockup HTML: Kas Laci Saat Ini, toggle privacy, status Shift Berjalan,
 * Estimasi Kas di Laci, "Tersinkron otomatis dengan POS",
 * Clean 4-Grid Metrics (tanpa nested border berat),
 * dan tombol aksi "Catat Kas Keluar / Kas Masuk".
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
                .padding(16.dp)
        ) {
            // Header Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Kas Laci Saat Ini",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(22.dp)
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
                        .background(Emerald50)
                        .border(1.dp, Emerald100.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
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
                                .background(Emerald600)
                        )
                        Text(
                            text = "Shift Berjalan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald800
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Main Balance Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Estimasi Kas di Laci",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
                Text(
                    text = if (isPrivacyHidden) "••••••••••••" else (uiState.systemCash.cleanMoney()),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.6).sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tersinkron otomatis dengan POS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            // Clean 4-Grid Metrics (No heavy nested border cards)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC).copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Modal Awal | Penjualan Tunai
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item 1: Modal Awal
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Modal Awal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = if (isPrivacyHidden) "••••••••" else (session?.openingCash?.cleanMoney() ?: "Rp 0"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Divider vertikal
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color(0xFFE2E8F0).copy(alpha = 0.6f))
                        )

                        // Item 2: Penjualan Tunai (+)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                        ) {
                            Text(
                                text = "Penjualan Tunai",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = if (isPrivacyHidden) "••••••••" else ("+ " + (uiState.cashSales?.cleanMoney() ?: "Rp 0")),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        thickness = 1.dp
                    )

                    // Row 2: Pengeluaran Kas | Mulai Shift
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item 3: Pengeluaran Kas (-)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Pengeluaran Kas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                lineHeight = 14.sp
                            )
                            val expenseVal = session?.totalExpenses ?: BigDecimal.ZERO
                            val expenseText = if (expenseVal.compareTo(BigDecimal.ZERO) == 0) {
                                "Rp 0"
                            } else {
                                "- " + expenseVal.cleanMoney()
                            }
                            Text(
                                text = if (isPrivacyHidden) "••••••••" else expenseText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Divider vertikal
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color(0xFFE2E8F0).copy(alpha = 0.6f))
                        )

                        // Item 4: Mulai Shift
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                        ) {
                            Text(
                                text = "Mulai Shift",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = session?.openedAt.formatSessionDate(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Action: Kas Keluar / Masuk
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                onClick = onShowExpenseDialog,
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SyncAlt,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catat Kas Keluar / Kas Masuk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                }
            }
        }
    }
}

/**
 * Card 2: Kartu Tutup Shift Kasir (Halaman Utama)
 * Sesuai mockup HTML: CardTutupShiftKasir yang membuka pop-up bottom sheet modal.
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
                shadowElevation = 2.dp
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
 * Pop-up Bottom Sheet Modal: Rekonsiliasi & Tutup Shift
 * Sesuai mockup HTML:
 * 1. Header: "Rekonsiliasi & Tutup Shift" & deskripsi
 * 2. Kartu Ekspektasi Kas Sistem (Kas Awal + Penjualan Tunai)
 * 3. Input Kas Fisik Aktual di Laci dengan aksi "Salin dari sistem"
 * 4. Banner Selisih Kas: Rp 0 (Pas), Kurang, atau Lebih
 * 5. Input Catatan Shift (Opsional)
 * 6. Tombol "Selesaikan & Tutup Shift" & "Kembali"
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

    val session = uiState.activeSession
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
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rekonsiliasi & Tutup Shift",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "Pastikan kas fisik di laci sesuai sebelum mengakhiri sesi kasir.",
                        fontSize = 12.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
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

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Modal Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // System Cash Expectation Card (Ekspektasi Kas Sistem)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ekspektasi Kas Sistem",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500
                            )
                            Text(
                                text = uiState.systemCash.cleanMoney(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kas Awal: " + (session?.openingCash?.cleanMoney() ?: "Rp 0"),
                                fontSize = 11.sp,
                                color = Slate500
                            )
                            Text(
                                text = "Penjualan Tunai: + " + (uiState.cashSales?.cleanMoney() ?: "Rp 0"),
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }

                // Physical Cash Input Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kas Fisik Aktual di Laci",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                        Text(
                            text = "Salin dari sistem",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onCashChange(uiState.systemCash.toPlainString().substringBefore("."))
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(
                                1.dp,
                                if (isError) DangerRed else Color(0xFFCBD5E1),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Rp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                modifier = Modifier.padding(end = 8.dp)
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
                                            text = "Masukkan jumlah uang fisik",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal,
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
                }

                // Difference Banner
                val diffVal = cashDifference ?: BigDecimal.ZERO
                val isBalanced = cashAmount.isNotBlank() && diffVal.compareTo(BigDecimal.ZERO) == 0
                val isShortage = cashAmount.isNotBlank() && diffVal < BigDecimal.ZERO
                val isExcess = cashAmount.isNotBlank() && diffVal > BigDecimal.ZERO

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Selisih Kas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                        Text(
                            text = when {
                                isShortage -> "- " + diffVal.abs().cleanMoney() + " (Kurang)"
                                isExcess -> "+ " + diffVal.cleanMoney() + " (Lebih)"
                                else -> "Rp 0 (Pas)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                isShortage -> Color(0xFFBE123C)
                                isExcess -> Color(0xFF1D4ED8)
                                else -> Emerald700
                            }
                        )
                    }
                }

                // Catatan Shift (Opsional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Catatan Shift (Opsional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text(
                                "Tuliskan keterangan jika terdapat selisih kas atau catatan operasional lainnya...",
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
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            unfocusedBorderColor = Slate200,
                            focusedBorderColor = BrandGreen
                        )
                    )
                }

                // Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Selesaikan & Tutup Shift
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
                            Text(
                                text = "Selesaikan & Tutup Shift",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Tombol Kembali
                    Surface(
                        onClick = onDismiss,
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kembali",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card 3: Transaksi Sesi Ini
 * Sesuai mockup HTML: SectionTransaksiSesiIni dengan dot hijau, judul, tautan "Lihat Semua",
 * dan daftar transaksi tanpa ikon kotak kiri:
 * #TRX-... · Penjualan · 05 Jun 2026 · 15:24  ==>  + Rp 50.000 · Tunai
 */
@Composable
fun SessionTransactionsCard(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onTransactionHistoryClick: () -> Unit
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
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                            .clip(CircleShape)
                            .background(Emerald600)
                    )
                    Text(
                        text = "Transaksi Sesi Ini",
                        fontSize = 12.sp,
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

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            if (uiState.transactions.isEmpty() && (session?.openingCash == null || session.openingCash.compareTo(BigDecimal.ZERO) == 0)) {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Daftar transaksi real-time
                    uiState.transactions.take(4).forEachIndexed { index, tx ->
                        if (index > 0) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                        }

                        val isIncome = tx.type != "EXPENSE"
                        val receiptIdDisplay = "#TRX-" + tx.receiptId.takeLast(8).uppercase()
                        val typeLabel = if (isIncome) "Penjualan" else "Kas Keluar"
                        val timeStr = tx.createdAt.formatTransactionFullDate()
                        val amountDisplay = (if (isIncome) "+ " else "- ") + tx.total.cleanMoney()
                        val paymentMethodBadge = if (isIncome) "Tunai" else "Operasional"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onTransactionHistoryClick)
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = receiptIdDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate800,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = typeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isIncome) Emerald700 else DangerRed,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = timeStr,
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = amountDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isIncome) Emerald700 else DangerRed
                                )
                                Text(
                                    text = paymentMethodBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    // Tampilkan item Modal Awal jika ada
                    if (session?.openingCash != null && session.openingCash > BigDecimal.ZERO) {
                        if (uiState.transactions.isNotEmpty()) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "Buka Kasir & Laci",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                                Text(
                                    text = "Modal Awal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate500,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = session.openedAt.formatTransactionFullDate(),
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = session.openingCash.cleanMoney(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Saldo Awal",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate400
                                )
                            }
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
