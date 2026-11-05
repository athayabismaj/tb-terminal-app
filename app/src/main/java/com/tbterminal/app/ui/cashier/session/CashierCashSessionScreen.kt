package com.tbterminal.app.ui.cashier.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cashier.session.components.CloseShiftSection
import com.tbterminal.app.ui.cashier.session.components.LocalCashSessionBadges
import com.tbterminal.app.ui.cashier.session.components.OpenShiftForm
import com.tbterminal.app.ui.cashier.session.components.SessionSummarySection
import com.tbterminal.app.ui.cashier.session.components.SessionTransactionsCard
import com.tbterminal.app.ui.components.SkeletonList
import com.tbterminal.app.ui.components.TopFloatingCapsule

private val CashSurfaceBg = SurfaceBg

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
    onClearMessage: () -> Unit = {}
) {
    val isClosingError = uiState.hasActiveSession && !uiState.isExpenseDialogOpen && uiState.errorMessage != null
    val closingErrorText = if (isClosingError) uiState.errorMessage else null

    var isPrivacyHidden by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CashSurfaceBg)
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
                    .widthIn(max = if (isDesktop) 960.dp else 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 1. GREETING & PAGE HEADER ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kas Harian & Shift",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = if (uiState.hasActiveSession) {
                                "Shift kasir aktif · Kelola kas laci & rekapitulasi shift"
                            } else {
                                "Shift kasir belum aktif · Buka sesi untuk melayani transaksi"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Status Pill Shift
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (uiState.hasActiveSession) Color(0xFFECFDF5) else Color(0xFFFFFBEB))
                            .border(
                                1.dp,
                                if (uiState.hasActiveSession) Color(0xFFA7F3D0).copy(alpha = 0.6f) else Color(0xFFFDE68A),
                                RoundedCornerShape(999.dp)
                            )
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
                                    .background(if (uiState.hasActiveSession) Emerald500 else Color(0xFFD97706))
                            )
                            Text(
                                text = if (uiState.hasActiveSession) "Shift Aktif" else "Shift Ditutup",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.hasActiveSession) Emerald700 else Color(0xFF92400E)
                            )
                        }
                    }
                }

                // Sesi lokal offline badge jika ada
                LocalCashSessionBadges(uiState = uiState)

                // ── 2. KONTEN UTAMA ──
                if (uiState.isLoading && !uiState.isSubmitting) {
                    SkeletonList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        itemCount = 4
                    )
                } else if (!uiState.hasActiveSession) {
                    // FORM BUKA SESI SHIFT BARU
                    OpenShiftForm(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onOpeningCashChanged = onOpeningCashChanged,
                        onOpenSession = onOpenSession
                    )
                } else {
                    // SHIFT AKTIF: TAMPILKAN RINGKASAN KAS, AKSI TUTUP SHIFT, & TRANSAKSI
                    if (isDesktop) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SessionSummarySection(
                                    modifier = Modifier.fillMaxWidth(),
                                    uiState = uiState,
                                    isPrivacyHidden = isPrivacyHidden,
                                    onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                                    onShowExpenseDialog = onShowExpenseDialog
                                )
                                SessionTransactionsCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    uiState = uiState,
                                    onTransactionHistoryClick = onTransactionHistoryClick
                                )
                            }
                            CloseShiftSection(
                                modifier = Modifier.weight(1f),
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
                    } else {
                        // Mobile Layout: Stacked
                        SessionSummarySection(
                            modifier = Modifier.fillMaxWidth(),
                            uiState = uiState,
                            isPrivacyHidden = isPrivacyHidden,
                            onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                            onShowExpenseDialog = onShowExpenseDialog
                        )
                        CloseShiftSection(
                            modifier = Modifier.fillMaxWidth(),
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
                        SessionTransactionsCard(
                            modifier = Modifier.fillMaxWidth(),
                            uiState = uiState,
                            onTransactionHistoryClick = onTransactionHistoryClick
                        )
                    }
                }
            }
        }
    }
}
