package com.tbterminal.app.ui.cashier.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cashier.session.components.CashierBukaShiftView
import com.tbterminal.app.ui.cashier.session.components.LocalCashSessionBadges
import com.tbterminal.app.ui.cashier.session.components.ShiftClosureBottomSheetModal
import com.tbterminal.app.ui.cashier.session.components.ShiftPaymentBreakdownBottomSheet
import com.tbterminal.app.ui.cashier.session.components.ShiftRecentTransactionsSection
import com.tbterminal.app.ui.cashier.session.components.ShiftReprintChooserDialog
import com.tbterminal.app.ui.cashier.session.components.ShiftSingleLineSummaryCard
import com.tbterminal.app.ui.cashier.session.components.ShiftUtilityRow
import com.tbterminal.app.ui.components.SkeletonList
import com.tbterminal.app.ui.components.TopFloatingCapsule
import java.math.BigDecimal

private val CanvasBg = Color(0xFFF8FAF9)

@Composable
fun CashierCashSessionScreen(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onShowExpenseDialog: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onClearMessage: () -> Unit = {},
    onPrintShiftSummary: () -> Unit = {}
) {
    val isClosingError = uiState.hasActiveSession && !uiState.isExpenseDialogOpen && uiState.errorMessage != null
    val closingErrorText = if (isClosingError) uiState.errorMessage else null

    var isPrivacyHidden by rememberSaveable { mutableStateOf(false) }
    var showCloseShiftModal by rememberSaveable { mutableStateOf(false) }
    var showPaymentBreakdownSheet by rememberSaveable { mutableStateOf(false) }
    var showReprintChooserDialog by rememberSaveable { mutableStateOf(false) }

    // Hitung metrik ringkasan shift
    val omzet = remember(uiState.transactions, uiState.cashSales) {
        if (uiState.transactions.isNotEmpty()) {
            uiState.transactions.sumOf { it.total }
        } else {
            uiState.cashSales
        }
    }
    val totalTransactions = remember(uiState.total, uiState.transactions) {
        if (uiState.total > 0) uiState.total else uiState.transactions.size.toLong()
    }
    val expectedCash = uiState.systemCash

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        TopFloatingCapsule(
            message = uiState.message ?: uiState.errorMessage,
            isError = uiState.errorMessage != null,
            onDismiss = onClearMessage
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isDesktop = maxWidth >= 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = if (isDesktop) 720.dp else 512.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sesi lokal offline badge jika ada
                LocalCashSessionBadges(uiState = uiState)

                // ── KONTEN UTAMA ──
                if (uiState.isLoading && !uiState.isSubmitting) {
                    SkeletonList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        itemCount = 3
                    )
                } else if (!uiState.hasActiveSession) {
                    if (uiState.canOpenCashSession) {
                        // FORM BUKA SESI SHIFT BARU (Keputusan 2)
                        CashierBukaShiftView(
                            cashierName = "Kasir",
                            terminalName = "Terminal Barokah Jaya",
                            openingCashInput = uiState.openingCashInput,
                            onOpeningCashChanged = onOpeningCashChanged,
                            onOpenSession = onOpenSession,
                            isSubmitting = uiState.isSubmitting,
                            errorMessage = if (!uiState.hasActiveSession) uiState.errorMessage else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Belum Ada Sesi Kas Aktif",
                                    color = Color(0xFF0F172A),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Hanya kasir yang dapat membuka sesi kas.",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                } else {
                    // ── SHIFT AKTIF (FASE 4) ──

                    // 1. Ringkasan Shift Satu Baris (Keputusan 7: Omzet | Transaksi | Kas Seharusnya)
                    ShiftSingleLineSummaryCard(
                        omzet = omzet,
                        transactionCount = totalTransactions,
                        expectedCash = expectedCash,
                        isPrivacyHidden = isPrivacyHidden,
                        onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                        onClickDetail = { showPaymentBreakdownSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Utility Row: Kas In/Out, Cetak Ulang, Tutup Shift (Touch target >= 48dp)
                    ShiftUtilityRow(
                        onKasMasukKeluarClick = onShowExpenseDialog,
                        onCetakUlangClick = { showReprintChooserDialog = true },
                        onTutupShiftClick = { showCloseShiftModal = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Aktivitas Transaksi Shift Ini (Tanpa kartu bertumpuk)
                    ShiftRecentTransactionsSection(
                        transactions = uiState.transactions,
                        totalTransactions = totalTransactions,
                        onTransactionHistoryClick = onTransactionHistoryClick,
                        onReprintTransaction = { showReprintChooserDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── BOTTOM SHEET 1: Rincian Metode Pembayaran & Arus Kas Laci ──
        if (showPaymentBreakdownSheet && uiState.hasActiveSession) {
            ShiftPaymentBreakdownBottomSheet(
                uiState = uiState,
                onDismiss = { showPaymentBreakdownSheet = false }
            )
        }

        // ── BOTTOM SHEET 2: Rekonsiliasi & Tutup Shift ──
        if (showCloseShiftModal && uiState.hasActiveSession) {
            ShiftClosureBottomSheetModal(
                onDismiss = { showCloseShiftModal = false },
                uiState = uiState,
                cashAmount = uiState.closingCashInput,
                notes = uiState.closingNotesInput,
                onCashChange = onClosingCashChanged,
                onNotesChange = onClosingNotesChanged,
                onSubmit = onCloseSession,
                isSubmitting = uiState.isSubmitting,
                isError = isClosingError,
                supportingText = closingErrorText
            )
        }

        // ── DIALOG 3: Cetak Ulang Dokumen (Slip Shift / Riwayat Struk) ──
        if (showReprintChooserDialog) {
            ShiftReprintChooserDialog(
                hasActiveShift = uiState.hasActiveSession,
                latestTransaction = uiState.transactions.firstOrNull(),
                onPrintShiftSlip = onPrintShiftSummary,
                onOpenTransactionHistory = onTransactionHistoryClick,
                onDismiss = { showReprintChooserDialog = false }
            )
        }
    }
}
